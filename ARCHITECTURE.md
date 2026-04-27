# 工业级 IoT 智能环境监测平台 - 后端架构说明

## 📋 项目概述

这是一个基于 SpringBoot + Netty + WebSocket 的工业级 IoT 智能环境监测平台后端系统，支持高并发TCP连接、实时数据推送、设备状态管理、报警系统和远程控制功能。

---

## 🏗️ 系统架构

### 技术栈
- **SpringBoot**: 应用框架
- **Netty**: 高性能TCP服务器
- **WebSocket**: 实时双向通信
- **Lombok**: 简化Java代码
- **Jackson**: JSON处理
- **ConcurrentHashMap**: 线程安全缓存

### 核心特性
✅ 高并发TCP连接支持  
✅ 设备在线/离线状态管理  
✅ 心跳检测机制（30秒超时）  
✅ 实时数据推送（WebSocket）  
✅ 自动报警系统  
✅ 远程设备控制  
✅ 线程安全的缓存管理  
✅ 模块化设计  
✅ 完整的REST API  

---

## 📁 项目结构

```
com.lihe
├── config/
│   ├── NettyConfig.java              # Netty服务器配置
│   └── WebSocketConfig.java          # WebSocket配置
├── controller/
│   └── DeviceController.java         # REST API控制器
├── entity/
│   ├── DeviceData.java               # 设备数据实体
│   ├── DeviceStatus.java             # 设备状态实体
│   └── AlarmInfo.java                # 报警信息实体
├── netty/
│   ├── codec/
│   │   └── JsonDecoder.java          # JSON解码器
│   ├── handler/
│   │   └── DeviceHandler.java        # 设备数据处理器
│   ├── server/
│   │   └── NettyServer.java          # Netty TCP服务器
│   └── session/
│       └── DeviceSessionManager.java # 设备会话管理器
├── service/
│   ├── cache/
│   │   └── DeviceCacheService.java   # 设备数据缓存服务
│   ├── alarm/
│   │   └── AlarmService.java         # 报警服务
│   └── task/
│       └── DeviceOnlineCheckTask.java # 在线检测定时任务
├── websocket/
│   └── WebSocketServer.java          # WebSocket服务器
└── WebAiStm32Application.java        # 应用启动类
```

---

## 🔧 核心模块说明

### 1. 实体层 (entity)

#### DeviceData.java
设备上传的传感器数据
```java
- deviceId: String      // 设备ID
- temp: Double          // 温度
- humidity: Double      // 湿度
- light: Double         // 光照强度
- soil: Double          // 土壤湿度
- online: Boolean       // 在线状态
- timestamp: Long       // 时间戳
```

#### DeviceStatus.java
设备连接状态信息
```java
- deviceId: String           // 设备ID
- online: Boolean            // 在线状态
- lastHeartbeatTime: Long    // 最后心跳时间
- ipAddress: String          // IP地址
```

#### AlarmInfo.java
报警信息
```java
- deviceId: String      // 设备ID
- alarmType: String     // 报警类型
- message: String       // 报警消息
- alarmTime: Long       // 报警时间
- handled: Boolean      // 是否已处理

报警类型:
- TEMP_HIGH: 高温报警 (>35°C)
- TEMP_LOW: 低温报警 (<0°C)
- HUMIDITY_ERROR: 湿度异常 (>90%)
- SOIL_DRY: 土壤缺水 (<30%)
- DEVICE_OFFLINE: 设备离线
```

---

### 2. 缓存层 (service/cache)

#### DeviceCacheService.java
线程安全的设备数据缓存服务
- 使用 `ConcurrentHashMap` 存储设备数据
- 提供设备数据的增删改查操作
- 更新设备在线状态
- 统计在线设备数量

**核心方法:**
```java
updateDeviceData(DeviceData data)     // 更新设备数据
getDevice(String deviceId)            // 获取单个设备
getAllDevices()                       // 获取所有设备
removeDevice(String deviceId)         // 移除设备
updateOnlineStatus(String, boolean)   // 更新在线状态
```

---

### 3. 会话管理 (netty/session)

#### DeviceSessionManager.java
设备TCP会话管理器
- 管理设备ID与Channel的映射
- 管理设备心跳时间
- 管理设备IP地址
- 提供消息发送功能
- 自动推送设备上下线状态

**核心方法:**
```java
online(String deviceId, Channel)      // 设备上线
heartbeat(String deviceId)            // 更新心跳
offline(String deviceId)              // 设备离线
sendMessage(String, String)           // 发送消息给设备
isOnline(String deviceId)             // 检查在线状态
getIpAddress(String deviceId)         // 获取IP地址
```

---

### 4. 报警系统 (service/alarm)

#### AlarmService.java
智能报警服务
- 自动检测设备数据异常
- 触发相应类型的报警
- 维护报警历史记录（最多100条）
- 通过WebSocket实时推送报警

**报警规则:**
```java
温度 > 35°C    → TEMP_HIGH 报警
温度 < 0°C     → TEMP_LOW 报警
湿度 > 90%     → HUMIDITY_ERROR 报警
土壤 < 30%     → SOIL_DRY 报警
设备离线30秒   → DEVICE_OFFLINE 报警
```

---

### 5. Netty层 (netty)

#### NettyServer.java
高性能TCP服务器
- 支持高并发连接
- TCP长连接
- 自动资源释放
- 解决TCP粘包问题（使用换行符分隔）

**配置参数:**
```java
SO_BACKLOG: 128              // 连接队列大小
SO_REUSEADDR: true           // 地址重用
SO_KEEPALIVE: true           // 保持连接
TCP_NODELAY: true            // 禁用Nagle算法
```

#### DeviceHandler.java
设备数据处理器
- 解析JSON数据
- 处理设备上线
- 处理心跳消息
- 更新缓存数据
- 触发报警检测
- 推送到WebSocket
- 异常处理和连接管理

**消息类型:**
```json
// 数据上传
{"deviceId":"stm32_001","temp":28.5,"humidity":66,"light":800,"soil":35}

// 心跳
{"type":"heartbeat","deviceId":"stm32_001"}
```

---

### 6. WebSocket层 (websocket)

#### WebSocketServer.java
WebSocket实时推送服务
- 多客户端支持
- 广播机制
- 自动移除断开连接
- 三种推送类型

**推送消息格式:**
```json
// 设备数据
{
  "type": "device_data",
  "deviceId": "stm32_001",
  "temp": 28.5,
  "humidity": 66,
  "light": 800,
  "soil": 35,
  "online": true,
  "timestamp": 1234567890
}

// 设备状态
{
  "type": "status",
  "deviceId": "stm32_001",
  "online": true
}

// 报警信息
{
  "type": "alarm",
  "deviceId": "stm32_001",
  "alarmType": "SOIL_DRY",
  "message": "土壤缺水: 25.0% (阈值: 30.0%)",
  "alarmTime": 1234567890,
  "handled": false
}
```

---

### 7. 定时任务 (service/task)

#### DeviceOnlineCheckTask.java
设备在线状态检测
- 每5秒执行一次
- 检测超过30秒无心跳的设备
- 自动判定为离线
- 触发离线报警

**调度配置:**
```java
@Scheduled(fixedRate = 5000)  // 每5秒
OFFLINE_TIMEOUT = 30000        // 30秒超时
```

---

### 8. REST API (controller)

#### DeviceController.java
完整的REST API接口

**设备查询:**
```
GET /api/device/list           # 获取所有设备
GET /api/device/{deviceId}     # 获取单个设备
GET /api/device/online         # 获取在线设备
```

**报警管理:**
```
GET /api/alarm/list            # 获取报警列表
POST /api/alarm/handle         # 处理报警
```

**设备控制:**
```
POST /api/device/control       # 发送控制指令
POST /api/device/buzzer/on     # 开启蜂鸣器
POST /api/device/buzzer/off    # 关闭蜂鸣器
POST /api/device/restart       # 重启设备
```

**控制指令:**
```
BUZZER_ON      # 开启蜂鸣器
BUZZER_OFF     # 关闭蜂鸣器
RESTART        # 重启设备
```

---

## 🔄 数据流程

### 设备数据上传流程
```
STM32设备 
  ↓ (TCP连接)
Netty Server (9000端口)
  ↓ (JSON解析)
DeviceHandler
  ↓ (更新缓存)
DeviceCacheService
  ↓ (报警检测)
AlarmService
  ↓ (WebSocket推送)
WebSocketServer
  ↓ (广播)
Vue前端
```

### 设备控制流程
```
Vue前端
  ↓ (HTTP请求)
DeviceController
  ↓ (验证指令)
DeviceSessionManager
  ↓ (TCP发送)
Netty Channel
  ↓ (指令下发)
STM32设备
```

### 心跳检测流程
```
DeviceHandler (收到心跳)
  ↓
DeviceSessionManager.heartbeat()
  ↓ (更新时间戳)
heartbeatMap.put(deviceId, currentTime)
  ↓ (定时检测)
DeviceOnlineCheckTask (每5秒)
  ↓ (检查超时)
currentTime - lastHeartbeat > 30000
  ↓ (判定离线)
DeviceSessionManager.offline()
  ↓ (推送状态)
WebSocketServer.sendDeviceStatus()
```

---

## ⚙️ 配置说明

### application.properties
```properties
# Netty TCP服务器端口
netty.port=9000

# Spring Boot端口
server.port=8080
```

### 启动配置
```java
@SpringBootApplication
@EnableScheduling  // 启用定时任务
public class WebAiStm32Application {
    public static void main(String[] args) {
        SpringApplication.run(WebAiStm32Application.class, args);
    }
}
```

---

## 📊 性能特性

### 线程安全
- 所有共享数据使用 `ConcurrentHashMap`
- CopyOnWriteArrayList 用于报警历史
- 无锁设计，高并发性能优异

### 资源管理
- Netty EventLoopGroup 优雅关闭
- Channel 自动清理
- 内存限制（报警历史最多100条）

### 高并发支持
- NIO非阻塞IO
- 事件驱动架构
- 连接池管理
- TCP参数优化

---

## 🧪 测试示例

### 设备模拟发送数据
```bash
# 使用nc命令测试
echo '{"deviceId":"stm32_001","temp":28.5,"humidity":66,"light":800,"soil":35}' | nc localhost 9000

# 发送心跳
echo '{"type":"heartbeat","deviceId":"stm32_001"}' | nc localhost 9000
```

### REST API测试
```bash
# 获取所有设备
curl http://localhost:8080/api/device/list

# 获取在线设备
curl http://localhost:8080/api/device/online

# 开启蜂鸣器
curl -X POST http://localhost:8080/api/device/buzzer/on \
  -H "Content-Type: application/json" \
  -d '{"deviceId":"stm32_001"}'

# 获取报警列表
curl http://localhost:8080/api/alarm/list
```

### WebSocket连接
```javascript
const ws = new WebSocket('ws://localhost:8080/ws');

ws.onmessage = (event) => {
    const data = JSON.parse(event.data);
    console.log('收到消息:', data);
    
    if (data.type === 'device_data') {
        // 处理设备数据
    } else if (data.type === 'status') {
        // 处理设备状态
    } else if (data.type === 'alarm') {
        // 处理报警
    }
};
```

---

## 📝 日志输出

系统输出以下日志：
- ✅ 设备连接/断开
- ✅ 收到数据/心跳
- ✅ 报警触发
- ✅ 指令发送
- ✅ 异常信息
- ✅ 在线状态变化

**日志级别:**
- INFO: 正常业务日志
- WARN: 警告信息（报警、超时）
- ERROR: 错误信息
- DEBUG: 调试信息

---

## 🚀 部署运行

### 编译
```bash
mvn clean package -DskipTests
```

### 运行
```bash
java -jar target/web-ai-stm32-0.0.1-SNAPSHOT.jar
```

### 验证
```bash
# 检查TCP端口
netstat -an | grep 9000

# 检查HTTP端口
netstat -an | grep 8080

# 查看日志
tail -f logs/application.log
```

---

## 🎯 系统目标达成

✅ **多设备实时监控** - 支持任意数量设备同时连接  
✅ **在线离线检测** - 30秒心跳超时自动判定  
✅ **实时报警** - 5种报警类型，WebSocket实时推送  
✅ **远程控制** - TCP指令下发，支持蜂鸣器和重启  
✅ **WebSocket实时推送** - 三种消息类型，毫秒级延迟  
✅ **高并发TCP连接** - Netty NIO架构，支持数千连接  
✅ **工业级稳定性** - 线程安全、异常处理、资源管理  

---

## 📞 技术支持

如有问题，请检查：
1. 端口9000和8080是否被占用
2. 防火墙是否开放相应端口
3. 设备是否正确发送JSON格式数据
4. WebSocket连接地址是否正确

---

**版本**: v2.0  
**更新日期**: 2026-04-27  
**架构**: 工业级 IoT 智能环境监测平台
