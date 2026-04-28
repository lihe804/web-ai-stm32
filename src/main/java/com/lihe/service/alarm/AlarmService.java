package com.lihe.service.alarm;

import com.lihe.entity.AlarmInfo;
import com.lihe.entity.DeviceData;
import com.lihe.websocket.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 报警服务
 * 检测设备数据异常并推送报警信息
 */
@Service
public class AlarmService {
    private static final Logger log = LoggerFactory.getLogger(AlarmService.class);
    
    // 报警历史记录
    private final List<AlarmInfo> alarmHistory = new CopyOnWriteArrayList<>();
    
    // 报警阈值配置（可动态调整）
    private double tempHighThreshold = 35.0;
    private double tempLowThreshold = 0.0;
    private double humidityHighThreshold = 90.0;
    private double soilDryThreshold = 30.0;
    private double lightHighThreshold = 1000.0; // 添加光照阈值
    
    /**
     * 检查设备数据并触发报警
     */
    public void checkAndAlarm(DeviceData data) {
        if (data == null || data.getDeviceId() == null) {
            return;
        }
        
        String deviceId = data.getDeviceId();
        
        // 高温报警
        if (data.getTemp() != null && data.getTemp() > tempHighThreshold) {
            triggerAlarm(deviceId, AlarmInfo.AlarmType.TEMP_HIGH, 
                String.format("温度过高: %.1f°C (阈值: %.1f°C)", data.getTemp(), tempHighThreshold));
        }
        
        // 低温报警
        if (data.getTemp() != null && data.getTemp() < tempLowThreshold) {
            triggerAlarm(deviceId, AlarmInfo.AlarmType.TEMP_LOW, 
                String.format("温度过低: %.1f°C (阈值: %.1f°C)", data.getTemp(), tempLowThreshold));
        }
        
        // 湿度异常报警
        if (data.getHumidity() != null && data.getHumidity() > humidityHighThreshold) {
            triggerAlarm(deviceId, AlarmInfo.AlarmType.HUMIDITY_ERROR, 
                String.format("湿度异常: %.1f%% (阈值: %.1f%%)", data.getHumidity(), humidityHighThreshold));
        }
        
        // 土壤缺水报警
        if (data.getSoil() != null && data.getSoil() < soilDryThreshold) {
            triggerAlarm(deviceId, AlarmInfo.AlarmType.SOIL_DRY, 
                String.format("土壤缺水: %.1f%% (阈值: %.1f%%)", data.getSoil(), soilDryThreshold));
        }
        
        // 光照过高报警
        if (data.getLight() != null && data.getLight() > lightHighThreshold) {
            triggerAlarm(deviceId, "LIGHT_HIGH", 
                String.format("光照过高: %.1flux (阈值: %.1flux)", data.getLight(), lightHighThreshold));
        }
    }
    
    /**
     * 触发报警
     */
    private void triggerAlarm(String deviceId, String alarmType, String message) {
        AlarmInfo alarm = new AlarmInfo(deviceId, alarmType, message);
        
        // 添加到历史记录
        alarmHistory.add(alarm);
        
        // 限制历史记录大小（保留最近100条）
        if (alarmHistory.size() > 100) {
            alarmHistory.remove(0);
        }
        
        log.warn("触发报警: 设备={}, 类型={}, 消息={}", deviceId, alarmType, message);
        
        // 通过WebSocket推送报警信息
        WebSocketServer.sendAlarm(alarm);
    }
    
    /**
     * 获取所有报警记录
     */
    public List<AlarmInfo> getAllAlarms() {
        return alarmHistory;
    }
    
    /**
     * 获取指定设备的报警记录
     */
    public List<AlarmInfo> getDeviceAlarms(String deviceId) {
        return alarmHistory.stream()
            .filter(alarm -> deviceId.equals(alarm.getDeviceId()))
            .toList();
    }
    
    /**
     * 标记报警为已处理
     */
    public boolean handleAlarm(String deviceId, String alarmType) {
        for (AlarmInfo alarm : alarmHistory) {
            if (deviceId.equals(alarm.getDeviceId()) && 
                alarmType.equals(alarm.getAlarmType()) && 
                !Boolean.TRUE.equals(alarm.getHandled())) {
                alarm.setHandled(true);
                log.info("报警已处理: 设备={}, 类型={}", deviceId, alarmType);
                return true;
            }
        }
        return false;
    }
    
    /**
     * 设置传感器阈值
     */
    public void setThreshold(String sensor, double value) {
        switch (sensor.toLowerCase()) {
            case "temp":
                this.tempHighThreshold = value;
                log.info("设置温度阈值为: {}", value);
                break;
            case "hum":
                this.humidityHighThreshold = value;
                log.info("设置湿度阈值为: {}", value);
                break;
            case "light":
                this.lightHighThreshold = value;
                log.info("设置光照阈值为: {}", value);
                break;
            case "soil":
                this.soilDryThreshold = value;
                log.info("设置土壤湿度阈值为: {}", value);
                break;
            default:
                log.warn("未知的传感器类型: {}", sensor);
                break;
        }
    }
    
    /**
     * 获取当前阈值配置
     */
    public Map<String, Double> getCurrentThresholds() {
        Map<String, Double> thresholds = new HashMap<>();
        thresholds.put("temp", tempHighThreshold);
        thresholds.put("hum", humidityHighThreshold);
        thresholds.put("light", lightHighThreshold);
        thresholds.put("soil", soilDryThreshold);
        return thresholds;
    }
    
    /**
     * 清除历史报警记录
     */
    public void clearAlarms() {
        alarmHistory.clear();
        log.info("清除所有报警记录");
    }
}
