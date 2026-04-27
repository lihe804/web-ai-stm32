package com.lihe.controller;

import com.lihe.entity.AlarmInfo;
import com.lihe.entity.DeviceData;
import com.lihe.netty.session.DeviceSessionManager;
import com.lihe.service.alarm.AlarmService;
import com.lihe.service.cache.DeviceCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 设备控制器
 * 提供设备查询、控制和报警管理的REST API
 */
@RestController
@RequestMapping("/api")
public class DeviceController {
    
    @Autowired
    private DeviceCacheService deviceCacheService;
    
    @Autowired
    private AlarmService alarmService;
    
    /**
     * 获取所有设备列表
     * GET /device/list
     */
    @GetMapping("/device/list")
    public ResponseEntity<Map<String, Object>> getAllDevices() {
        Map<String, Object> response = new HashMap<>();
        List<DeviceData> devices = deviceCacheService.getAllDevices().stream()
            .collect(Collectors.toList());
        
        response.put("success", true);
        response.put("data", devices);
        response.put("count", devices.size());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取单个设备信息
     * GET /device/{deviceId}
     */
    @GetMapping("/device/{deviceId}")
    public ResponseEntity<Map<String, Object>> getDevice(@PathVariable String deviceId) {
        Map<String, Object> response = new HashMap<>();
        DeviceData data = deviceCacheService.getDevice(deviceId);
        
        if (data != null) {
            response.put("success", true);
            response.put("data", data);
            response.put("online", DeviceSessionManager.isOnline(deviceId));
            response.put("ipAddress", DeviceSessionManager.getIpAddress(deviceId));
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "设备不存在");
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 获取在线设备列表
     * GET /device/online
     */
    @GetMapping("/device/online")
    public ResponseEntity<Map<String, Object>> getOnlineDevices() {
        Map<String, Object> response = new HashMap<>();
        List<DeviceData> onlineDevices = deviceCacheService.getAllDevices().stream()
            .filter(data -> Boolean.TRUE.equals(data.getOnline()))
            .collect(Collectors.toList());
        
        response.put("success", true);
        response.put("data", onlineDevices);
        response.put("count", onlineDevices.size());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取报警列表
     * GET /alarm/list
     */
    @GetMapping("/alarm/list")
    public ResponseEntity<Map<String, Object>> getAlarmList(
            @RequestParam(required = false) String deviceId) {
        Map<String, Object> response = new HashMap<>();
        
        List<AlarmInfo> alarms;
        if (deviceId != null && !deviceId.isEmpty()) {
            alarms = alarmService.getDeviceAlarms(deviceId);
        } else {
            alarms = alarmService.getAllAlarms();
        }
        
        response.put("success", true);
        response.put("data", alarms);
        response.put("count", alarms.size());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 处理报警
     * POST /alarm/handle
     */
    @PostMapping("/alarm/handle")
    public ResponseEntity<Map<String, Object>> handleAlarm(
            @RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        String deviceId = request.get("deviceId");
        String alarmType = request.get("alarmType");
        
        if (deviceId == null || alarmType == null) {
            response.put("success", false);
            response.put("message", "参数不完整");
            return ResponseEntity.badRequest().body(response);
        }
        
        boolean handled = alarmService.handleAlarm(deviceId, alarmType);
        if (handled) {
            response.put("success", true);
            response.put("message", "报警已处理");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "报警不存在或已处理");
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 设备控制 - 发送指令
     * POST /device/control
     */
    @PostMapping("/device/control")
    public ResponseEntity<Map<String, Object>> controlDevice(
            @RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        String deviceId = request.get("deviceId");
        String command = request.get("command");
        
        if (deviceId == null || command == null) {
            response.put("success", false);
            response.put("message", "参数不完整");
            return ResponseEntity.badRequest().body(response);
        }
        
        // 验证命令
        if (!isValidCommand(command)) {
            response.put("success", false);
            response.put("message", "无效的命令");
            return ResponseEntity.badRequest().body(response);
        }
        
        // 检查设备是否在线
        if (!DeviceSessionManager.isOnline(deviceId)) {
            response.put("success", false);
            response.put("message", "设备不在线");
            return ResponseEntity.badRequest().body(response);
        }
        
        // 发送指令到设备
        boolean sent = DeviceSessionManager.sendMessage(deviceId, command);
        if (sent) {
            response.put("success", true);
            response.put("message", "指令已发送: " + command);
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "指令发送失败");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 开启蜂鸣器
     * POST /device/buzzer/on
     */
    @PostMapping("/device/buzzer/on")
    public ResponseEntity<Map<String, Object>> buzzerOn(@RequestBody Map<String, String> request) {
        Map<String, String> cmd = new HashMap<>();
        cmd.put("deviceId", request.get("deviceId"));
        cmd.put("command", "BUZZER_ON");
        return controlDevice(cmd);
    }
    
    /**
     * 关闭蜂鸣器
     * POST /device/buzzer/off
     */
    @PostMapping("/device/buzzer/off")
    public ResponseEntity<Map<String, Object>> buzzerOff(@RequestBody Map<String, String> request) {
        Map<String, String> cmd = new HashMap<>();
        cmd.put("deviceId", request.get("deviceId"));
        cmd.put("command", "BUZZER_OFF");
        return controlDevice(cmd);
    }
    
    /**
     * 重启设备
     * POST /device/restart
     */
    @PostMapping("/device/restart")
    public ResponseEntity<Map<String, Object>> restartDevice(@RequestBody Map<String, String> request) {
        Map<String, String> cmd = new HashMap<>();
        cmd.put("deviceId", request.get("deviceId"));
        cmd.put("command", "RESTART");
        return controlDevice(cmd);
    }
    
    /**
     * 验证命令是否有效
     */
    private boolean isValidCommand(String command) {
        return "BUZZER_ON".equals(command) || 
               "BUZZER_OFF".equals(command) || 
               "RESTART".equals(command);
    }
}
