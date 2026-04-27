package com.lihe.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceData {

    private String deviceId;

    private Double temp;

    private Double humidity;

    private Double light;

    private Double soil;

    private Boolean online;

    private Long timestamp;
    
    /**
     * 验证数据是否有效
     */
    public boolean isValid() {
        if (deviceId == null || deviceId.isEmpty()) {
            return false;
        }
        if (temp != null && (temp < -50 || temp > 150)) {
            return false;
        }
        if (humidity != null && (humidity < 0 || humidity > 100)) {
            return false;
        }
        if (light != null && (light < 0 || light > 10000)) {
            return false;
        }
        if (soil != null && (soil < 0 || soil > 100)) {
            return false;
        }
        return true;
    }
}
