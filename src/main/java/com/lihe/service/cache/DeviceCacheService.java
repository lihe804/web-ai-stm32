package com.lihe.service.cache;

import com.lihe.entity.DeviceData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备数据缓存服务
 * 线程安全的设备数据管理
 */
@Service
public class DeviceCacheService {
    private static final Logger log = LoggerFactory.getLogger(DeviceCacheService.class);
    
    // 设备数据缓存: deviceId -> DeviceData
    private final Map<String, DeviceData> deviceDataCache = new ConcurrentHashMap<>();
    
    /**
     * 更新设备数据
     */
    public void updateDeviceData(DeviceData data) {
        if (data == null || data.getDeviceId() == null) {
            log.warn("无效的设备数据");
            return;
        }
        
        // 设置时间戳
        if (data.getTimestamp() == null) {
            data.setTimestamp(System.currentTimeMillis());
        }
        
        deviceDataCache.put(data.getDeviceId(), data);
        log.debug("更新设备数据: {}, 温度: {}, 湿度: {}, 光照: {}, 土壤: {}", 
            data.getDeviceId(), data.getTemp(), data.getHumidity(), 
            data.getLight(), data.getSoil());
    }
    
    /**
     * 获取单个设备数据
     */
    public DeviceData getDevice(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) {
            return null;
        }
        return deviceDataCache.get(deviceId);
    }
    
    /**
     * 获取所有设备数据
     */
    public Collection<DeviceData> getAllDevices() {
        return deviceDataCache.values();
    }
    
    /**
     * 移除设备
     */
    public void removeDevice(String deviceId) {
        if (deviceId != null && !deviceId.isEmpty()) {
            DeviceData removed = deviceDataCache.remove(deviceId);
            if (removed != null) {
                log.info("移除设备: {}", deviceId);
            }
        }
    }
    
    /**
     * 更新设备在线状态
     */
    public void updateOnlineStatus(String deviceId, boolean online) {
        DeviceData data = deviceDataCache.get(deviceId);
        if (data != null) {
            data.setOnline(online);
            log.info("更新设备在线状态: {}, 在线: {}", deviceId, online);
        }
    }
    
    /**
     * 获取在线设备数量
     */
    public int getOnlineDeviceCount() {
        return (int) deviceDataCache.values().stream()
            .filter(data -> Boolean.TRUE.equals(data.getOnline()))
            .count();
    }
    
    /**
     * 清空所有设备数据
     */
    public void clearAll() {
        deviceDataCache.clear();
        log.info("清空所有设备数据");
    }
}
