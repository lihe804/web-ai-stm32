package com.lihe.service.task;

import com.lihe.entity.AlarmInfo;
import com.lihe.netty.session.DeviceSessionManager;
import com.lihe.service.alarm.AlarmService;
import com.lihe.websocket.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * 设备在线状态检测定时任务
 * 每5秒检测一次，超过30秒无心跳自动判定离线
 */
@Service
public class DeviceOnlineCheckTask {
    private static final Logger log = LoggerFactory.getLogger(DeviceOnlineCheckTask.class);
    
    // 离线超时时间（30秒）
    private static final long OFFLINE_TIMEOUT = 30000;
    
    @Autowired
    private AlarmService alarmService;
    
    /**
     * 每5秒检测一次设备在线状态
     */
    @Scheduled(fixedRate = 5000)
    public void checkDeviceOnlineStatus() {
        log.debug("开始检测设备在线状态...");
        
        Map<String, Long> heartbeatMap = DeviceSessionManager.getHeartbeatMap();
        
        if (heartbeatMap.isEmpty()) {
            log.debug("当前没有设备连接");
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        Set<String> deviceIds = heartbeatMap.keySet();
        
        for (String deviceId : deviceIds) {
            Long lastHeartbeat = heartbeatMap.get(deviceId);
            if (lastHeartbeat != null) {
                long timeSinceLastHeartbeat = currentTime - lastHeartbeat;
                
                // 如果超过30秒未收到心跳，判定为离线
                if (timeSinceLastHeartbeat > OFFLINE_TIMEOUT) {
                    log.warn("设备超时未心跳，判定为离线: {}, 最后心跳时间: {}毫秒前", 
                        deviceId, timeSinceLastHeartbeat);
                    
                    // 执行离线操作
                    DeviceSessionManager.offline(deviceId);
                    
                    // 触发设备离线报警
                    triggerOfflineAlarm(deviceId);
                }
            }
        }
        
        log.debug("设备在线状态检测完成，当前设备数: {}", heartbeatMap.size());
    }
    
    /**
     * 触发设备离线报警
     */
    private void triggerOfflineAlarm(String deviceId) {
        String message = String.format("设备离线: %s (超过30秒未心跳)", deviceId);
        
        // 创建报警信息
        AlarmInfo alarm = new AlarmInfo(deviceId, AlarmInfo.AlarmType.DEVICE_OFFLINE, message);
        
        // 推送报警到WebSocket
        WebSocketServer.sendAlarm(alarm);
        
        log.warn("设备离线报警: {}", message);
    }
}
