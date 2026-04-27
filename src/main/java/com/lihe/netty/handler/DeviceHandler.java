package com.lihe.netty.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lihe.entity.DeviceData;
import com.lihe.netty.session.DeviceSessionManager;
import com.lihe.service.alarm.AlarmService;
import com.lihe.service.cache.DeviceCacheService;
import com.lihe.websocket.WebSocketServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 设备数据处理器
 * 处理设备上传的数据、心跳和连接事件
 */
public class DeviceHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger log = LoggerFactory.getLogger(DeviceHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    
    private final DeviceCacheService deviceCacheService;
    private final AlarmService alarmService;
    
    /**
     * 构造函数注入依赖
     */
    public DeviceHandler(DeviceCacheService deviceCacheService, AlarmService alarmService) {
        this.deviceCacheService = deviceCacheService;
        this.alarmService = alarmService;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        try {
            log.info("收到设备数据: {}", msg);
            
            // 解析JSON获取消息类型和设备ID
            JsonNode jsonNode = mapper.readTree(msg);
            String type = jsonNode.has("type") ? jsonNode.get("type").asText() : "data";
            String deviceId = jsonNode.has("deviceId") ? jsonNode.get("deviceId").asText() : null;
            
            // 处理心跳消息
            if ("heartbeat".equals(type)) {
                if (deviceId != null && !deviceId.isEmpty()) {
                    DeviceSessionManager.heartbeat(deviceId);
                    log.info("收到设备心跳: {}", deviceId);
                }
                return;
            }
            
            // 处理设备数据消息
            DeviceData data = mapper.readValue(msg, DeviceData.class);
            
            // 数据验证
            if (!data.isValid()) {
                log.warn("收到无效数据: deviceId={}, temp={}, humidity={}", 
                    data.getDeviceId(), data.getTemp(), data.getHumidity());
                return;
            }
            
            // 设置时间戳
            if (data.getTimestamp() == null) {
                data.setTimestamp(System.currentTimeMillis());
            }
            
            // 设备上线（首次收到数据时注册）
            if (deviceId != null && !deviceId.isEmpty()) {
                DeviceSessionManager.online(deviceId, ctx.channel());
            }
            
            log.info("设备ID: {}, 温度: {}, 湿度: {}, 光照: {}, 土壤: {}", 
                data.getDeviceId(), data.getTemp(), data.getHumidity(), 
                data.getLight(), data.getSoil());
            
            // 更新设备数据缓存
            deviceCacheService.updateDeviceData(data);
            
            // 检查并触发报警
            alarmService.checkAndAlarm(data);
            
            // 推送到WebSocket前端
            WebSocketServer.sendDeviceData(data);
            
        } catch (Exception e) {
            log.error("处理设备数据失败: {}", e.getMessage(), e);
            // 不关闭连接，继续接收其他数据
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("设备连接成功: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("设备断开连接: {}", ctx.channel().remoteAddress());
        // 从会话管理器中移除该通道
        DeviceSessionManager.removeChannel(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("通道异常: {}", cause.getMessage(), cause);
        ctx.close();
    }
}
