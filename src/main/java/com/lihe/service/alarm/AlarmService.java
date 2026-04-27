package com.lihe.service.alarm;

import com.lihe.entity.AlarmInfo;
import com.lihe.entity.DeviceData;
import com.lihe.websocket.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
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
    
    // 报警阈值配置
    private static final double TEMP_HIGH_THRESHOLD = 35.0;
    private static final double TEMP_LOW_THRESHOLD = 0.0;
    private static final double HUMIDITY_HIGH_THRESHOLD = 90.0;
    private static final double SOIL_DRY_THRESHOLD = 30.0;
    
    /**
     * 检查设备数据并触发报警
     */
    public void checkAndAlarm(DeviceData data) {
        if (data == null || data.getDeviceId() == null) {
            return;
        }
        
        String deviceId = data.getDeviceId();
        
        // 高温报警
        if (data.getTemp() != null && data.getTemp() > TEMP_HIGH_THRESHOLD) {
            triggerAlarm(deviceId, AlarmInfo.AlarmType.TEMP_HIGH, 
                String.format("温度过高: %.1f°C (阈值: %.1f°C)", data.getTemp(), TEMP_HIGH_THRESHOLD));
        }
        
        // 低温报警
        if (data.getTemp() != null && data.getTemp() < TEMP_LOW_THRESHOLD) {
            triggerAlarm(deviceId, AlarmInfo.AlarmType.TEMP_LOW, 
                String.format("温度过低: %.1f°C (阈值: %.1f°C)", data.getTemp(), TEMP_LOW_THRESHOLD));
        }
        
        // 湿度异常报警
        if (data.getHumidity() != null && data.getHumidity() > HUMIDITY_HIGH_THRESHOLD) {
            triggerAlarm(deviceId, AlarmInfo.AlarmType.HUMIDITY_ERROR, 
                String.format("湿度异常: %.1f%% (阈值: %.1f%%)", data.getHumidity(), HUMIDITY_HIGH_THRESHOLD));
        }
        
        // 土壤缺水报警
        if (data.getSoil() != null && data.getSoil() < SOIL_DRY_THRESHOLD) {
            triggerAlarm(deviceId, AlarmInfo.AlarmType.SOIL_DRY, 
                String.format("土壤缺水: %.1f%% (阈值: %.1f%%)", data.getSoil(), SOIL_DRY_THRESHOLD));
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
     * 清除历史报警记录
     */
    public void clearAlarms() {
        alarmHistory.clear();
        log.info("清除所有报警记录");
    }
}
