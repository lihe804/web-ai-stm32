package com.lihe.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lihe.entity.DeviceData;
import com.lihe.websocket.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DeviceService {
    private static final Logger log = LoggerFactory.getLogger(DeviceService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // 存储最新的设备数据（按设备ID）
    private final Map<String, DeviceData> latestDeviceData = new ConcurrentHashMap<>();
    
    // 存储历史数据（实际项目中应该存入数据库）
    private final List<DeviceDataHistory> historyData = new ArrayList<>();
    
    /**
     * 处理设备数据
     */
    public void processDeviceData(DeviceData data) {
        // 保存最新数据
        latestDeviceData.put(data.getDeviceId(), data);
        
        // 保存历史数据（限制大小，避免内存溢出）
        synchronized (historyData) {
            historyData.add(new DeviceDataHistory(data, LocalDateTime.now()));
            if (historyData.size() > 1000) {
                historyData.remove(0);
            }
        }
        
        // 推送到WebSocket前端
        try {
            String jsonData = objectMapper.writeValueAsString(data);
            WebSocketServer.sendMessage(jsonData);
            log.debug("数据已推送到WebSocket: {}", jsonData);
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 获取指定设备的最新数据
     */
    public DeviceData getLatestData(String deviceId) {
        return latestDeviceData.get(deviceId);
    }
    
    /**
     * 获取所有设备的最新数据
     */
    public Map<String, DeviceData> getAllLatestData() {
        return new ConcurrentHashMap<>(latestDeviceData);
    }
    
    /**
     * 获取在线设备数量
     */
    public int getOnlineDeviceCount() {
        return latestDeviceData.size();
    }
    
    /**
     * 内部类：带时间戳的历史数据
     */
    private static class DeviceDataHistory {
        private DeviceData data;
        private LocalDateTime timestamp;
        
        public DeviceDataHistory(DeviceData data, LocalDateTime timestamp) {
            this.data = data;
            this.timestamp = timestamp;
        }
        
        public DeviceData getData() {
            return data;
        }
        
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }
}
