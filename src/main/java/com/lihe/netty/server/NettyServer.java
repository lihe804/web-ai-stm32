package com.lihe.netty.server;

import com.lihe.netty.handler.DeviceHandler;
import com.lihe.service.alarm.AlarmService;
import com.lihe.service.cache.DeviceCacheService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty TCP服务器
 * 支持高并发连接、TCP长连接、自动释放资源
 */
public class NettyServer {
    private static final Logger log = LoggerFactory.getLogger(NettyServer.class);
    private EventLoopGroup boss;
    private EventLoopGroup worker;
    
    private final DeviceCacheService deviceCacheService;
    private final AlarmService alarmService;
    
    /**
     * 构造函数注入依赖
     */
    public NettyServer(DeviceCacheService deviceCacheService, AlarmService alarmService) {
        this.deviceCacheService = deviceCacheService;
        this.alarmService = alarmService;
    }
    
    public void start(int port) {
        boss = new NioEventLoopGroup(1);
        worker = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            // 字符串编码器（用于发送数据给设备）
                            pipeline.addLast(new StringEncoder());
                            // 解决TCP粘包问题，使用换行符作为分隔符（用于接收数据）
                            // 最大帧长度设置为8192字节，支持更长的JSON数据
                            pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Unpooled.wrappedBuffer("\n".getBytes())));
                            // 字符串解码器
                            pipeline.addLast(new StringDecoder());
                            // 自定义业务处理器，注入依赖
                            pipeline.addLast(new DeviceHandler(deviceCacheService, alarmService));
                        }
                    });
            
            log.info("正在启动TCP服务器，端口: {}", port);
            ChannelFuture future = bootstrap.bind(port).sync();
            log.info("TCP服务器启动成功，监听端口: {}", port);
            
            // 等待服务监听端口关闭
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("服务器启动被中断: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("服务器启动失败，端口 {} 可能已被占用: {}", port, e.getMessage(), e);
        } finally {
            shutdown();
        }
    }
    
    public void shutdown() {
        log.info("正在关闭TCP服务器...");
        if (worker != null) {
            worker.shutdownGracefully();
        }
        if (boss != null) {
            boss.shutdownGracefully();
        }
        log.info("TCP服务器已关闭");
    }
}