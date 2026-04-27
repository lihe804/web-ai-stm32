# Web AI STM32 物联网数据接收服务

## 项目简介
这是一个基于Spring Boot + Netty的物联网设备数据接收服务，用于接收STM32设备通过TCP发送的数据，并通过WebSocket实时推送到前端。

## 技术栈
- **Spring Boot 4.0.6** - 主框架
- **Netty 4.1.108** - TCP服务器
- **WebSocket** - 实时数据推送
- **Jackson** - JSON处理
- **Lombok** - 简化代码
- **SLF4J + Logback** - 日志框架

## 功能特性
✅ TCP服务器接收设备数据（端口9000）  
✅ WebSocket实时推送数据到前端（端口8080）  
✅ REST API查询设备状态  
✅ 数据验证和异常处理  
✅ 日志记录  
✅ 设备会话管理  

## 快速开始

### 1. 环境要求
- JDK 17+
- Maven 3.6+

### 2. 编译运行
```bash
mvn clean package
java -jar target/web-ai-stm32-0.0.1-SNAPSHOT.jar
```

或使用IDE直接运行 `WebAiStm32Application.java`

### 3. 配置说明
主要配置文件：`src/main/resources/application.properties`

```properties
# HTTP服务端口
server.port=8080

# TCP服务器端口
netty.port=9000

# 日志级别
logging.level.com.lihe=DEBUG
```

## API接口

### 1. 获取所有设备数据
```
GET http://localhost:8080/api/devices
```

响应示例：
```json
{
  "success": true,
  "data": {
    "device001": {
      "deviceId": "device001",
      "temp": 25.5,
      "humidity": 60.0
    }
  },
  "count": 1
}
```

### 2. 获取指定设备数据
```
GET http://localhost:8080/api/devices/{deviceId}
```

### 3. 获取系统状态
```
GET http://localhost:8080/api/devices/status
```

响应示例：
```json
{
  "success": true,
  "onlineDevices": 5
}
```

## 数据格式

### TCP设备数据格式
设备通过TCP发送JSON数据，以换行符(`\n`)结尾：

```json
{"deviceId":"device001","temp":25.5,"humidity":60.0}\n
```

### WebSocket推送格式
前端连接 `ws://localhost:8080/ws` 接收实时数据推送，格式与TCP接收的相同。

## 测试

### 使用Telnet测试TCP连接
```bash
telnet localhost 9000
{"deviceId":"test001","temp":25.5,"humidity":60.0}
```

### 使用WebSocket测试工具
连接 `ws://localhost:8080/ws` 查看实时数据推送。

## 项目结构
```
src/main/java/com/lihe/
├── config/              # 配置类
│   ├── NettyConfig.java
│   ├── WebSocketConfig.java
│   └── GlobalExceptionHandler.java
├── controller/          # REST控制器
│   └── DeviceController.java
├── entity/              # 实体类
│   └── DeviceData.java
├── netty/               # Netty相关
│   ├── server/
│   ├── handler/
│   ├── codec/
│   └── session/
├── service/             # 业务逻辑
│   └── DeviceService.java
├── websocket/           # WebSocket处理
│   └── WebSocketServer.java
└── WebAiStm32Application.java
```

## 注意事项

1. **端口占用**：确保8080和9000端口未被占用
2. **数据验证**：温度范围 -50~150℃，湿度范围 0~100%
3. **内存限制**：历史数据最多保存1000条，避免内存溢出
4. **线程安全**：已处理并发访问问题

## 后续改进建议

- [ ] 添加数据库持久化（MySQL/MongoDB）
- [ ] 添加用户认证和授权
- [ ] 添加数据可视化界面
- [ ] 支持更多通信协议（MQTT等）
- [ ] 添加监控告警功能
- [ ] 单元测试覆盖

## 许可证
MIT License
