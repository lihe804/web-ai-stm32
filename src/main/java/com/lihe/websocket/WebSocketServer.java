package com.lihe.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lihe.netty.session.DeviceSessionManager;
import com.lihe.service.alarm.AlarmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class WebSocketServer extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(WebSocketServer.class);
    private static final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private AlarmService alarmService;
    
    @PostConstruct
    public void init() {
        // 初始化时可以加载默认阈值配置
        log.info("WebSocket服务器初始化完成");
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            log.info("收到WebSocket消息: {}", payload);
            
            JsonNode jsonNode = objectMapper.readTree(payload);
            String type = jsonNode.has("type") ? jsonNode.get("type").asText() : null;
            
            if ("threshold".equals(type)) {
                handleThresholdUpdate(jsonNode, session);
            } else {
                log.warn("未知的消息类型: {}", type);
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理阈值更新请求
     */
    private void handleThresholdUpdate(JsonNode jsonNode, WebSocketSession session) {
        try {
            String deviceId = jsonNode.has("deviceId") ? jsonNode.get("deviceId").asText() : null;
            String sensor = jsonNode.has("sensor") ? jsonNode.get("sensor").asText() : null;
            double value = jsonNode.has("value") ? jsonNode.get("value").asDouble() : 0;
            
            if (deviceId == null || sensor == null) {
                log.warn("阈值更新请求参数不完整");
                return;
            }
            
            log.info("收到阈值修改:\ndevice={}\nsensor={}\nvalue={}", deviceId, sensor, value);
            
            // 更新报警服务中的阈值
            alarmService.setThreshold(sensor, value);
            
            // 构造TCP命令发送给STM32
            String tcpCommand = String.format("SET_THRESHOLD %s %.1f", sensor, value);
            log.info("发送TCP命令:\n{}", tcpCommand);
            
            // 通过Netty发送命令到STM32设备
            DeviceSessionManager.sendMessage(deviceId, tcpCommand);
            
            // 向前端发送确认消息
            Map<String, Object> response = new HashMap<>();
            response.put("type", "threshold_ack");
            response.put("deviceId", deviceId);
            response.put("sensor", sensor);
            response.put("value", value);
            response.put("success", true);
            
            String jsonResponse = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(jsonResponse));
            
        } catch (Exception e) {
            log.error("处理阈值更新失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket连接建立，当前连接数: {}", sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket连接关闭，当前连接数: {}, 状态码: {}", sessions.size(), status.getCode());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误: {}", exception.getMessage(), exception);
        sessions.remove(session);
        if (session.isOpen()) {
            session.close();
        }
    }

    /**
     * 发送消息给所有连接的客户端
     */
    public static void sendMessage(String msg) {
        if (sessions.isEmpty()) {
            log.debug("没有WebSocket客户端连接，跳过消息推送");
            return;
        }
        
        int successCount = 0;
        int failCount = 0;
        
        for (WebSocketSession session : sessions) {
            try {
                if (session != null && session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(msg));
                    }
                    successCount++;
                } else {
                    log.warn("会话已关闭，将从列表中移除");
                    sessions.remove(session);
                    failCount++;
                }
            } catch (IOException e) {
                log.error("发送消息失败: {}", e.getMessage(), e);
                sessions.remove(session);
                failCount++;
            }
        }
        
        if (failCount > 0) {
            log.warn("消息推送完成: 成功{}, 失败{}", successCount, failCount);
        } else {
            log.debug("消息推送成功，共{}个客户端", successCount);
        }
    }
    
    /**
     * 获取当前连接数
     */
    public static int getConnectionCount() {
        return sessions.size();
    }
    
    /**
     * 广播消息给所有WebSocket客户端
     */
    public static void broadcast(String message) {
        sendMessage(message);
    }
    
    /**
     * 发送设备数据
     */
    public static void sendDeviceData(com.lihe.entity.DeviceData data) {
        try {
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("type", "device_data");
            dataMap.put("deviceId", data.getDeviceId());
            dataMap.put("temp", data.getTemp());
            dataMap.put("humidity", data.getHumidity());
            dataMap.put("light", data.getLight());
            dataMap.put("soil", data.getSoil());
            dataMap.put("online", data.getOnline());
            dataMap.put("timestamp", data.getTimestamp());
            
            String jsonMessage = objectMapper.writeValueAsString(dataMap);
            sendMessage(jsonMessage);
            log.debug("推送设备数据: {}", data.getDeviceId());
        } catch (Exception e) {
            log.error("发送设备数据失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 发送设备状态信息
     */
    public static void sendDeviceStatus(String deviceId, boolean online) {
        try {
            Map<String, Object> statusMap = new HashMap<>();
            statusMap.put("type", "status");
            statusMap.put("deviceId", deviceId);
            statusMap.put("online", online);
            
            String jsonMessage = objectMapper.writeValueAsString(statusMap);
            sendMessage(jsonMessage);
            log.info("推送设备状态: {}, 在线: {}", deviceId, online);
        } catch (Exception e) {
            log.error("发送设备状态失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 发送报警信息
     */
    public static void sendAlarm(com.lihe.entity.AlarmInfo alarm) {
        try {
            Map<String, Object> alarmMap = new HashMap<>();
            alarmMap.put("type", "alarm");
            alarmMap.put("deviceId", alarm.getDeviceId());
            alarmMap.put("alarmType", alarm.getAlarmType());
            alarmMap.put("message", alarm.getMessage());
            alarmMap.put("alarmTime", alarm.getAlarmTime());
            alarmMap.put("handled", alarm.getHandled());
            
            String jsonMessage = objectMapper.writeValueAsString(alarmMap);
            sendMessage(jsonMessage);
            log.info("推送报警信息: {}, 类型: {}", alarm.getDeviceId(), alarm.getAlarmType());
        } catch (Exception e) {
            log.error("发送报警信息失败: {}", e.getMessage(), e);
        }
    }

}
