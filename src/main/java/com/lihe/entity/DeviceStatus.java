package com.lihe.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceStatus {
    
    private String deviceId;
    
    private Boolean online;
    
    private Long lastHeartbeatTime;
    
    private String ipAddress;
    
    /**
     * 构造函数
     */
    public DeviceStatus(String deviceId, Boolean online) {
        this.deviceId = deviceId;
        this.online = online;
        this.lastHeartbeatTime = System.currentTimeMillis();
    }
}
