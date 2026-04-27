package com.lihe.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlarmInfo {
    
    private String deviceId;
    
    private String alarmType;
    
    private String message;
    
    private Long alarmTime;
    
    private Boolean handled;
    
    /**
     * 报警类型常量
     */
    public static class AlarmType {
        public static final String TEMP_HIGH = "TEMP_HIGH";
        public static final String TEMP_LOW = "TEMP_LOW";
        public static final String HUMIDITY_ERROR = "HUMIDITY_ERROR";
        public static final String SOIL_DRY = "SOIL_DRY";
        public static final String DEVICE_OFFLINE = "DEVICE_OFFLINE";
    }
    
    /**
     * 构造函数（自动生成时间）
     */
    public AlarmInfo(String deviceId, String alarmType, String message) {
        this.deviceId = deviceId;
        this.alarmType = alarmType;
        this.message = message;
        this.alarmTime = System.currentTimeMillis();
        this.handled = false;
    }
}
