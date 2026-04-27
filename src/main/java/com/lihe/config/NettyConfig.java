package com.lihe.config;

import com.lihe.netty.server.NettyServer;
import com.lihe.netty.session.DeviceSessionManager;
import com.lihe.service.alarm.AlarmService;
import com.lihe.service.cache.DeviceCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Configuration
public class NettyConfig {
    
    @Value("${netty.port:9000}")
    private int port;
    
    @Autowired
    private DeviceCacheService deviceCacheService;
    
    @Autowired
    private AlarmService alarmService;
    
    private NettyServer nettyServer;
    
    @PostConstruct
    public void startNettyServer() {
        // 设置设备缓存服务到会话管理器
        DeviceSessionManager.setDeviceCacheService(deviceCacheService);
        
        // 创建 NettyServer 时注入依赖
        nettyServer = new NettyServer(deviceCacheService, alarmService);
        
        // 在新线程中启动Netty服务器，避免阻塞Spring Boot主线程
        Thread nettyThread = new Thread(() -> {
            try {
                System.out.println("准备启动Netty TCP服务器，端口: " + port);
                nettyServer.start(port);
            } catch (Exception e) {
                System.err.println("Netty服务器启动异常: " + e.getMessage());
                System.err.println("请检查端口 " + port + " 是否被占用，或修改 application.properties 中的 netty.port 配置");
            }
        }, "netty-server-thread");
        nettyThread.setDaemon(true);
        nettyThread.start();
        System.out.println("Netty服务器启动线程已创建");
    }
    
    @PreDestroy
    public void stopNettyServer() {
        if (nettyServer != null) {
            nettyServer.shutdown();
        }
    }
}
