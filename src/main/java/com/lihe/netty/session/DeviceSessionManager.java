package com.lihe.netty.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lihe.entity.DeviceStatus;
import com.lihe.service.cache.DeviceCacheService;
import com.lihe.websocket.WebSocketServer;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备会话管理器
 * 管理设备TCP连接和心跳状态
 */
public class DeviceSessionManager {
    private static final Logger log = LoggerFactory.getLogger(DeviceSessionManager.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // 存储所有设备的channel
    private static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    
    // 存储设备ID与channel的映射关系
    private static final Map<String, Channel> deviceChannelMap = new ConcurrentHashMap<>();
    
    // 存储设备ID与最后心跳时间的映射关系
    private static final Map<String, Long> heartbeatMap = new ConcurrentHashMap<>();
    
    // 存储设备ID与IP地址的映射关系
    private static final Map<String, String> deviceIpMap = new ConcurrentHashMap<>();
    
    // 离线超时时间（30秒）
    private static final long OFFLINE_TIMEOUT = 30000;
    
    // 设备缓存服务
    private static DeviceCacheService deviceCacheService;
    
    /**
     * 设置设备缓存服务（由Spring注入）
     */
    public static void setDeviceCacheService(DeviceCacheService cacheService) {
        deviceCacheService = cacheService;
    }
    
    /**
     * 设备上线
     */
    public static void online(String deviceId, Channel channel) {
        if (deviceId == null || deviceId.isEmpty()) {
            log.warn("设备ID为空，无法上线");
            return;
        }
        
        channels.add(channel);
        deviceChannelMap.put(deviceId, channel);
        heartbeatMap.put(deviceId, System.currentTimeMillis());
        
        // 获取IP地址
        String ipAddress = getIpAddress(channel);
        deviceIpMap.put(deviceId, ipAddress);
        
        log.info("设备上线: {}, IP: {}", deviceId, ipAddress);
        
        // 更新缓存中的在线状态
        if (deviceCacheService != null) {
            deviceCacheService.updateOnlineStatus(deviceId, true);
        }
        
        // 推送设备上线状态到WebSocket
        WebSocketServer.sendDeviceStatus(deviceId, true);
    }
    
    /**
     * 更新设备心跳时间
     */
    public static void heartbeat(String deviceId) {
        if (deviceId != null && !deviceId.isEmpty()) {
            heartbeatMap.put(deviceId, System.currentTimeMillis());
            log.debug("设备心跳更新: {}", deviceId);
        }
    }
    
    /**
     * 设备离线
     */
    public static void offline(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) {
            return;
        }
        
        Channel channel = deviceChannelMap.remove(deviceId);
        if (channel != null) {
            channels.remove(channel);
        }
        heartbeatMap.remove(deviceId);
        String ipAddress = deviceIpMap.remove(deviceId);
        
        log.info("设备离线: {}, IP: {}", deviceId, ipAddress);
        
        // 更新缓存中的在线状态
        if (deviceCacheService != null) {
            deviceCacheService.updateOnlineStatus(deviceId, false);
        }
        
        // 推送设备离线状态到WebSocket
        WebSocketServer.sendDeviceStatus(deviceId, false);
    }
    
    /**
     * 向指定设备发送消息
     */
    public static boolean sendMessage(String deviceId, String message) {
        if (deviceId == null || deviceId.isEmpty()) {
            log.warn("❌ 设备ID为空，无法发送消息");
            return false;
        }
        
        Channel channel = deviceChannelMap.get(deviceId);
        
        // 诊断：检查通道是否存在
        if (channel == null) {
            log.warn("❌ 设备通道不存在: {}", deviceId);
            log.warn("📋 当前在线设备列表: {}", deviceChannelMap.keySet());
            return false;
        }
        
        // 诊断：检查通道是否活跃
        if (!channel.isActive()) {
            log.warn("❌ 设备通道不活跃: {}", deviceId);
            log.warn("📊 通道状态 - isOpen: {}, isRegistered: {}, isWritable: {}", 
                channel.isOpen(), channel.isRegistered(), channel.isWritable());
            
            // 自动清理无效通道
            log.info("🧹 清理无效通道: {}", deviceId);
            offline(deviceId);
            return false;
        }
        
        // 发送消息
        try {
            channel.writeAndFlush(message + "\n");
            log.info("✅ 向设备发送消息成功: {}, 消息: {}", deviceId, message);
            return true;
        } catch (Exception e) {
            log.error("❌ 向设备发送消息异常: {}, 错误: {}", deviceId, e.getMessage(), e);
            // 发送失败，判定设备离线
            offline(deviceId);
            return false;
        }
    }
    
    /**
     * 检查设备是否在线
     */
    public static boolean isOnline(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) {
            return false;
        }
        
        Long lastHeartbeat = heartbeatMap.get(deviceId);
        if (lastHeartbeat == null) {
            return false;
        }
        
        // 判断是否超过离线超时时间
        return (System.currentTimeMillis() - lastHeartbeat) < OFFLINE_TIMEOUT;
    }
    
    /**
     * 获取设备IP地址
     */
    public static String getIpAddress(String deviceId) {
        return deviceIpMap.get(deviceId);
    }
    
    /**
     * 获取心跳映射表（用于定时任务检测）
     */
    public static Map<String, Long> getHeartbeatMap() {
        return heartbeatMap;
    }
    
    /**
     * 获取设备通道映射表
     */
    public static Map<String, Channel> getDeviceChannelMap() {
        return deviceChannelMap;
    }
    
    /**
     * 移除设备通道
     */
    public static void removeChannel(Channel channel) {
        channels.remove(channel);
        // 从映射中移除该通道对应的设备ID
        deviceChannelMap.entrySet().removeIf(entry -> {
            if (entry.getValue().equals(channel)) {
                String deviceId = entry.getKey();
                heartbeatMap.remove(deviceId);
                String ipAddress = deviceIpMap.remove(deviceId);
                log.info("设备断开连接: {}, IP: {}", deviceId, ipAddress);
                
                // 更新缓存中的在线状态
                if (deviceCacheService != null) {
                    deviceCacheService.updateOnlineStatus(deviceId, false);
                }
                
                // 推送设备离线状态到WebSocket
                WebSocketServer.sendDeviceStatus(deviceId, false);
                return true;
            }
            return false;
        });
    }
    
    /**
     * 获取通道的IP地址
     */
    private static String getIpAddress(Channel channel) {
        if (channel != null && channel.remoteAddress() instanceof InetSocketAddress) {
            InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
            return address.getAddress().getHostAddress();
        }
        return "unknown";
    }
    
    /**
     * 根据设备ID获取通道
     */
    public static Channel getChannelByDeviceId(String deviceId) {
        return deviceChannelMap.get(deviceId);
    }
    
    /**
     * 广播消息给所有设备
     */
    public static void broadcast(String message) {
        channels.writeAndFlush(message + "\n");
    }
    
    /**
     * 发送消息给指定设备
     */
    public static void sendToDevice(String deviceId, String message) {
        Channel channel = deviceChannelMap.get(deviceId);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(message + "\n");
        }
    }
    
    /**
     * 获取当前连接的设备数量
     */
    public static int getConnectedDeviceCount() {
        return channels.size();
    }
}
