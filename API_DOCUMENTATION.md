# 工业级 IoT 平台 - 前后端交互接口文档

## 📋 文档说明

本文档描述了前端与后端的所有交互接口，包括：
- **REST API** - HTTP请求接口
- **WebSocket** - 实时通信接口
- **TCP协议** - 设备通信协议（供参考）

**基础信息：**
- 服务器地址: `http://localhost:8080`
- WebSocket地址: `ws://localhost:8080/ws`
- TCP端口: `9000`

---

## 🌐 REST API 接口

### 1. 设备管理接口

#### 1.1 获取所有设备列表

**接口地址:** `GET /api/device/list`

**请求参数:** 无

**响应示例:**
```json
{
  "success": true,
  "data": [
    {
      "deviceId": "stm32_001",
      "temp": 28.5,
      "humidity": 66.0,
      "light": 800.0,
      "soil": 35.0,
      "online": true,
      "timestamp": 1714234567890
    },
    {
      "deviceId": "stm32_002",
      "temp": 25.0,
      "humidity": 70.0,
      "light": 750.0,
      "soil": 40.0,
      "online": true,
      "timestamp": 1714234567890
    }
  ],
  "count": 2
}
```

**字段说明:**
| 字段 | 类型 | 说明 |
|------|------|------|
| deviceId | String | 设备唯一标识 |
| temp | Double | 温度（°C） |
| humidity | Double | 湿度（%） |
| light | Double | 光照强度（Lux） |
| soil | Double | 土壤湿度（%） |
| online | Boolean | 在线状态 |
| timestamp | Long | 数据时间戳（毫秒） |

---

#### 1.2 获取单个设备信息

**接口地址:** `GET /api/device/{deviceId}`

**路径参数:**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| deviceId | String | 是 | 设备ID |

**请求示例:**
```
GET /api/device/stm32_001
```

**响应示例:**
```json
{
  "success": true,
  "data": {
    "deviceId": "stm32_001",
    "temp": 28.5,
    "humidity": 66.0,
    "light": 800.0,
    "soil": 35.0,
    "online": true,
    "timestamp": 1714234567890
  },
  "online": true,
  "ipAddress": "192.168.1.100"
}
```

**错误响应:**
```json
{
  "success": false,
  "message": "设备不存在"
}
```

---

#### 1.3 获取在线设备列表

**接口地址:** `GET /api/device/online`

**请求参数:** 无

**响应示例:**
```json
{
  "success": true,
  "data": [
    {
      "deviceId": "stm32_001",
      "temp": 28.5,
      "humidity": 66.0,
      "light": 800.0,
      "soil": 35.0,
      "online": true,
      "timestamp": 1714234567890
    }
  ],
  "count": 1
}
```

---

### 2. 报警管理接口

#### 2.1 获取报警列表

**接口地址:** `GET /api/alarm/list`

**查询参数:**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| deviceId | String | 否 | 设备ID（不传则返回所有报警） |

**请求示例:**
```
GET /api/alarm/list?deviceId=stm32_001
```

**响应示例:**
```json
{
  "success": true,
  "data": [
    {
      "deviceId": "stm32_001",
      "alarmType": "SOIL_DRY",
      "message": "土壤缺水: 25.0% (阈值: 30.0%)",
      "alarmTime": 1714234567890,
      "handled": false
    },
    {
      "deviceId": "stm32_001",
      "alarmType": "TEMP_HIGH",
      "message": "温度过高: 38.5°C (阈值: 35.0°C)",
      "alarmTime": 1714234500000,
      "handled": true
    }
  ],
  "count": 2
}
```

**字段说明:**
| 字段 | 类型 | 说明 |
|------|------|------|
| deviceId | String | 设备ID |
| alarmType | String | 报警类型（见下方说明） |
| message | String | 报警详细描述 |
| alarmTime | Long | 报警时间戳（毫秒） |
| handled | Boolean | 是否已处理 |

**报警类型说明:**
| 类型 | 说明 | 触发条件 |
|------|------|----------|
| TEMP_HIGH | 高温报警 | 温度 > 35°C |
| TEMP_LOW | 低温报警 | 温度 < 0°C |
| HUMIDITY_ERROR | 湿度异常 | 湿度 > 90% |
| SOIL_DRY | 土壤缺水 | 土壤湿度 < 30% |
| DEVICE_OFFLINE | 设备离线 | 超过30秒无心跳 |

---

#### 2.2 处理报警

**接口地址:** `POST /api/alarm/handle`

**请求头:**
```
Content-Type: application/json
```

**请求体:**
```json
{
  "deviceId": "stm32_001",
  "alarmType": "SOIL_DRY"
}
```

**响应示例（成功）:**
```json
{
  "success": true,
  "message": "报警已处理"
}
```

**响应示例（失败）:**
```json
{
  "success": false,
  "message": "报警不存在或已处理"
}
```

---

### 3. 设备控制接口

#### 3.1 发送控制指令（通用）

**接口地址:** `POST /api/device/control`

**请求头:**
```
Content-Type: application/json
```

**请求体:**
```json
{
  "deviceId": "stm32_001",
  "command": "BUZZER_ON"
}
```

**支持的命令:**
| 命令 | 说明 |
|------|------|
| BUZZER_ON | 开启蜂鸣器 |
| BUZZER_OFF | 关闭蜂鸣器 |
| RESTART | 重启设备 |

**响应示例（成功）:**
```json
{
  "success": true,
  "message": "指令已发送: BUZZER_ON"
}
```

**响应示例（设备离线）:**
```json
{
  "success": false,
  "message": "设备不在线"
}
```

**响应示例（无效命令）:**
```json
{
  "success": false,
  "message": "无效的命令"
}
```

---

#### 3.2 开启蜂鸣器（快捷接口）

**接口地址:** `POST /api/device/buzzer/on`

**请求体:**
```json
{
  "deviceId": "stm32_001"
}
```

**响应示例:**
```json
{
  "success": true,
  "message": "指令已发送: BUZZER_ON"
}
```

---

#### 3.3 关闭蜂鸣器（快捷接口）

**接口地址:** `POST /api/device/buzzer/off`

**请求体:**
```json
{
  "deviceId": "stm32_001"
}
```

**响应示例:**
```json
{
  "success": true,
  "message": "指令已发送: BUZZER_OFF"
}
```

---

#### 3.4 重启设备

**接口地址:** `POST /api/device/restart`

**请求体:**
```json
{
  "deviceId": "stm32_001"
}
```

**响应示例:**
```json
{
  "success": true,
  "message": "指令已发送: RESTART"
}
```

---

## 🔌 WebSocket 实时通信

### 连接地址

```
ws://localhost:8080/ws
```

### 连接示例（JavaScript）

```javascript
const ws = new WebSocket('ws://localhost:8080/ws');

ws.onopen = () => {
    console.log('WebSocket 连接成功');
};

ws.onmessage = (event) => {
    const data = JSON.parse(event.data);
    handleMessage(data);
};

ws.onerror = (error) => {
    console.error('WebSocket 错误:', error);
};

ws.onclose = () => {
    console.log('WebSocket 连接关闭');
    // 可选：5秒后重连
    setTimeout(() => connectWebSocket(), 5000);
};
```

---

### 消息类型

WebSocket 推送三种类型的消息，通过 `type` 字段区分：

#### 1. 设备数据消息 (`device_data`)

当设备上传新的传感器数据时推送。

**消息格式:**
```json
{
  "type": "device_data",
  "deviceId": "stm32_001",
  "temp": 28.5,
  "humidity": 66.0,
  "light": 800.0,
  "soil": 35.0,
  "online": true,
  "timestamp": 1714234567890
}
```

**字段说明:**
| 字段 | 类型 | 说明 |
|------|------|------|
| type | String | 消息类型，固定为 "device_data" |
| deviceId | String | 设备ID |
| temp | Double | 温度（°C） |
| humidity | Double | 湿度（%） |
| light | Double | 光照强度（Lux） |
| soil | Double | 土壤湿度（%） |
| online | Boolean | 在线状态 |
| timestamp | Long | 数据时间戳 |

**前端处理示例:**
```javascript
function handleMessage(data) {
    if (data.type === 'device_data') {
        // 更新设备数据显示
        updateDeviceDisplay(data);
        
        // 更新图表
        updateChart(data.deviceId, data);
    }
}
```

---

#### 2. 设备状态消息 (`status`)

当设备上线或离线时推送。

**消息格式:**
```json
{
  "type": "status",
  "deviceId": "stm32_001",
  "online": true
}
```

**字段说明:**
| 字段 | 类型 | 说明 |
|------|------|------|
| type | String | 消息类型，固定为 "status" |
| deviceId | String | 设备ID |
| online | Boolean | 在线状态（true=上线，false=离线） |

**触发场景:**
- 设备首次连接并发送数据
- 设备断开TCP连接
- 设备心跳超时（30秒无心跳）

**前端处理示例:**
```javascript
function handleMessage(data) {
    if (data.type === 'status') {
        // 更新设备在线状态
        updateDeviceStatus(data.deviceId, data.online);
        
        // 显示通知
        if (data.online) {
            showNotification('设备上线', `${data.deviceId} 已连接`);
        } else {
            showNotification('设备离线', `${data.deviceId} 已断开`);
        }
    }
}
```

---

#### 3. 报警消息 (`alarm`)

当检测到异常情况时推送报警信息。

**消息格式:**
```json
{
  "type": "alarm",
  "deviceId": "stm32_001",
  "alarmType": "SOIL_DRY",
  "message": "土壤缺水: 25.0% (阈值: 30.0%)",
  "alarmTime": 1714234567890,
  "handled": false
}
```

**字段说明:**
| 字段 | 类型 | 说明 |
|------|------|------|
| type | String | 消息类型，固定为 "alarm" |
| deviceId | String | 设备ID |
| alarmType | String | 报警类型 |
| message | String | 报警详细描述 |
| alarmTime | Long | 报警时间戳 |
| handled | Boolean | 是否已处理（通常为false） |

**报警类型:**
| 类型 | 说明 |
|------|------|
| TEMP_HIGH | 高温报警 |
| TEMP_LOW | 低温报警 |
| HUMIDITY_ERROR | 湿度异常 |
| SOIL_DRY | 土壤缺水 |
| DEVICE_OFFLINE | 设备离线 |

**前端处理示例:**
```javascript
function handleMessage(data) {
    if (data.type === 'alarm') {
        // 添加到报警列表
        addAlarmToList(data);
        
        // 显示弹窗通知
        showAlarmNotification(data);
        
        // 播放警报声音
        playAlarmSound();
        
        // 高亮显示对应设备
        highlightDevice(data.deviceId);
    }
}
```

---

### 完整的 WebSocket 处理示例

```javascript
class WebSocketManager {
    constructor() {
        this.ws = null;
        this.reconnectInterval = 5000;
        this.devices = new Map();
        this.alarms = [];
    }
    
    connect() {
        this.ws = new WebSocket('ws://localhost:8080/ws');
        
        this.ws.onopen = () => {
            console.log('✅ WebSocket 连接成功');
        };
        
        this.ws.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                this.handleMessage(data);
            } catch (error) {
                console.error('消息解析失败:', error);
            }
        };
        
        this.ws.onerror = (error) => {
            console.error('❌ WebSocket 错误:', error);
        };
        
        this.ws.onclose = () => {
            console.log('⚠️ WebSocket 连接关闭');
            // 自动重连
            setTimeout(() => this.connect(), this.reconnectInterval);
        };
    }
    
    handleMessage(data) {
        switch (data.type) {
            case 'device_data':
                this.handleDeviceData(data);
                break;
            case 'status':
                this.handleDeviceStatus(data);
                break;
            case 'alarm':
                this.handleAlarm(data);
                break;
            default:
                console.warn('未知消息类型:', data.type);
        }
    }
    
    handleDeviceData(data) {
        // 更新设备数据
        this.devices.set(data.deviceId, data);
        
        // 触发更新事件
        this.emit('deviceUpdate', data);
        
        console.log(`📊 设备数据更新: ${data.deviceId}`);
    }
    
    handleDeviceStatus(data) {
        // 更新设备在线状态
        const device = this.devices.get(data.deviceId);
        if (device) {
            device.online = data.online;
        }
        
        // 触发状态更新事件
        this.emit('statusUpdate', data);
        
        console.log(`🔌 设备状态变化: ${data.deviceId} - ${data.online ? '上线' : '离线'}`);
    }
    
    handleAlarm(data) {
        // 添加到报警列表
        this.alarms.unshift(data);
        
        // 限制报警列表大小
        if (this.alarms.length > 100) {
            this.alarms.pop();
        }
        
        // 触发报警事件
        this.emit('alarmReceived', data);
        
        console.log(`🚨 收到报警: ${data.alarmType} - ${data.message}`);
    }
    
    emit(eventName, data) {
        // 自定义事件分发逻辑
        document.dispatchEvent(new CustomEvent(eventName, { detail: data }));
    }
    
    disconnect() {
        if (this.ws) {
            this.ws.close();
        }
    }
}

// 使用示例
const wsManager = new WebSocketManager();
wsManager.connect();

// 监听设备数据更新
document.addEventListener('deviceUpdate', (event) => {
    const deviceData = event.detail;
    updateUI(deviceData);
});

// 监听报警
document.addEventListener('alarmReceived', (event) => {
    const alarm = event.detail;
    showAlarm(alarm);
});
```

---

## 📱 Vue3 集成示例

### 1. 创建 WebSocket composable

```javascript
// composables/useWebSocket.js
import { ref, onMounted, onUnmounted } from 'vue'

export function useWebSocket() {
    const ws = ref(null)
    const devices = ref(new Map())
    const alarms = ref([])
    const connected = ref(false)
    
    const connect = () => {
        ws.value = new WebSocket('ws://localhost:8080/ws')
        
        ws.value.onopen = () => {
            connected.value = true
            console.log('WebSocket connected')
        }
        
        ws.value.onmessage = (event) => {
            const data = JSON.parse(event.data)
            
            switch (data.type) {
                case 'device_data':
                    devices.value.set(data.deviceId, data)
                    break
                case 'status':
                    const device = devices.value.get(data.deviceId)
                    if (device) {
                        device.online = data.online
                    }
                    break
                case 'alarm':
                    alarms.value.unshift(data)
                    if (alarms.value.length > 100) {
                        alarms.value.pop()
                    }
                    break
            }
        }
        
        ws.value.onclose = () => {
            connected.value = false
            setTimeout(connect, 5000)
        }
        
        ws.value.onerror = (error) => {
            console.error('WebSocket error:', error)
        }
    }
    
    onMounted(() => {
        connect()
    })
    
    onUnmounted(() => {
        if (ws.value) {
            ws.value.close()
        }
    })
    
    return {
        devices,
        alarms,
        connected
    }
}
```

### 2. 在组件中使用

```vue
<template>
    <div class="dashboard">
        <!-- 设备列表 -->
        <div v-for="[deviceId, device] in devices" :key="deviceId" class="device-card">
            <h3>{{ device.deviceId }}</h3>
            <div class="status" :class="{ online: device.online }">
                {{ device.online ? '在线' : '离线' }}
            </div>
            <div class="data">
                <p>温度: {{ device.temp }}°C</p>
                <p>湿度: {{ device.humidity }}%</p>
                <p>光照: {{ device.light }} Lux</p>
                <p>土壤: {{ device.soil }}%</p>
            </div>
        </div>
        
        <!-- 报警列表 -->
        <div class="alarms">
            <h2>报警信息</h2>
            <div v-for="alarm in alarms" :key="alarm.alarmTime" class="alarm-item">
                <span class="alarm-type">{{ alarm.alarmType }}</span>
                <span class="alarm-message">{{ alarm.message }}</span>
            </div>
        </div>
    </div>
</template>

<script setup>
import { useWebSocket } from '@/composables/useWebSocket'

const { devices, alarms, connected } = useWebSocket()
</script>
```

---

## 📡 设备端 TCP 协议（参考）

### 连接信息
- **服务器地址:** `localhost` 或服务器IP
- **端口:** `9000`
- **协议:** TCP
- **编码:** UTF-8
- **分隔符:** `\n` (换行符)

### 消息格式

所有消息均为 JSON 格式，以换行符结尾。

#### 1. 设备数据上传

**格式:**
```json
{
  "deviceId": "stm32_001",
  "temp": 28.5,
  "humidity": 66.0,
  "light": 800.0,
  "soil": 35.0
}
```

**发送频率建议:** 每10秒一次

---

#### 2. 心跳消息

**格式:**
```json
{
  "type": "heartbeat",
  "deviceId": "stm32_001"
}
```

**发送频率建议:** 每30秒一次（防止被判定离线）

---

#### 3. 接收控制指令

设备会收到以下指令（以换行符结尾）：

| 指令 | 说明 |
|------|------|
| `BUZZER_ON\n` | 开启蜂鸣器 |
| `BUZZER_OFF\n` | 关闭蜂鸣器 |
| `RESTART\n` | 重启设备 |

---

### STM32 + ESP8266 示例代码

```cpp
#include <ESP8266WiFi.h>
#include <ArduinoJson.h>

const char* ssid = "YOUR_WIFI_SSID";
const char* password = "YOUR_WIFI_PASSWORD";
const char* server = "192.168.1.100";
const int port = 9000;

WiFiClient client;
unsigned long lastDataTime = 0;
unsigned long lastHeartbeatTime = 0;

void setup() {
    Serial.begin(115200);
    
    WiFi.begin(ssid, password);
    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        Serial.print(".");
    }
    Serial.println("\nWiFi connected");
}

void loop() {
    // 保持TCP连接
    if (!client.connected()) {
        if (client.connect(server, port)) {
            Serial.println("Connected to server");
        } else {
            delay(1000);
            return;
        }
    }
    
    // 读取服务器下发的指令
    if (client.available()) {
        String command = client.readStringUntil('\n');
        command.trim();
        
        if (command == "BUZZER_ON") {
            digitalWrite(BUZZER_PIN, HIGH);
        } else if (command == "BUZZER_OFF") {
            digitalWrite(BUZZER_PIN, LOW);
        } else if (command == "RESTART") {
            ESP.restart();
        }
    }
    
    unsigned long now = millis();
    
    // 每10秒发送一次数据
    if (now - lastDataTime > 10000) {
        sendData();
        lastDataTime = now;
    }
    
    // 每30秒发送一次心跳
    if (now - lastHeartbeatTime > 30000) {
        sendHeartbeat();
        lastHeartbeatTime = now;
    }
}

void sendData() {
    float temp = readTemperature();
    float humidity = readHumidity();
    float light = readLight();
    float soil = readSoilMoisture();
    
    StaticJsonDocument<200> doc;
    doc["deviceId"] = "stm32_001";
    doc["temp"] = temp;
    doc["humidity"] = humidity;
    doc["light"] = light;
    doc["soil"] = soil;
    
    String json;
    serializeJson(doc, json);
    client.println(json);
    
    Serial.println("Data sent: " + json);
}

void sendHeartbeat() {
    StaticJsonDocument<100> doc;
    doc["type"] = "heartbeat";
    doc["deviceId"] = "stm32_001";
    
    String json;
    serializeJson(doc, json);
    client.println(json);
    
    Serial.println("Heartbeat sent");
}
```

---

## 🔒 错误处理

### HTTP 错误码

| 状态码 | 说明 | 处理方式 |
|--------|------|----------|
| 200 | 成功 | 正常处理响应数据 |
| 400 | 请求参数错误 | 检查请求参数 |
| 404 | 资源不存在 | 检查设备ID是否正确 |
| 500 | 服务器内部错误 | 联系后端开发 |

### WebSocket 错误处理

```javascript
ws.onerror = (error) => {
    console.error('WebSocket error:', error);
    // 显示错误提示
    showError('连接失败，请检查网络');
};

ws.onclose = () => {
    // 自动重连
    setTimeout(() => {
        console.log('尝试重新连接...');
        connectWebSocket();
    }, 5000);
};
```

---

## 📊 数据流程图

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│   STM32设备  │ ─TCP→   │  Netty Server │ ─JSON→  │ DeviceHandler│
└─────────────┘  9000    └──────────────┘         └──────┬──────┘
                                                          │
                                                          ↓
                                                  ┌──────────────┐
                                                  │Cache Service │
                                                  └──────┬───────┘
                                                         │
                                                         ↓
                                                 ┌──────────────┐
                                                 │ Alarm Service│
                                                 └──────┬───────┘
                                                        │
                                                        ↓
                                                ┌───────────────┐
                                                │WebSocket Server│
                                                └───────┬───────┘
                                                        │
                                                        ↓ WS
                                               ┌────────────────┐
                                               │   Vue 前端      │
                                               └────────────────┘
                                                        │
                                                        ↓ HTTP
                                               ┌────────────────┐
                                               │ REST API       │
                                               └────────────────┘
```

---

## 🧪 测试工具推荐

### 1. Postman - REST API 测试

导入以下集合进行测试：

**获取设备列表:**
```
GET http://localhost:8080/api/device/list
```

**控制设备:**
```
POST http://localhost:8080/api/device/buzzer/on
Content-Type: application/json

{
  "deviceId": "stm32_001"
}
```

### 2. WebSocket King Client - Chrome插件

用于测试 WebSocket 连接和消息接收。

### 3. Netcat - TCP 测试

```bash
# 模拟设备发送数据
echo '{"deviceId":"test_001","temp":25.5,"humidity":60}' | nc localhost 9000
```

---

## 🎯 动态阈值调整接口

### WebSocket 请求协议

前端通过WebSocket发送阈值修改请求：

**请求格式:**
```json
{
  "type": "threshold",
  "deviceId": "stm32_001",
  "sensor": "temp",
  "value": 38
}
```

**字段说明:**
| 字段 | 类型 | 说明 |
|------|------|------|
| type | string | 固定为 "threshold" |
| deviceId | string | 设备ID |
| sensor | string | 传感器类型: temp/hum/light/soil |
| value | number | 阈值数值 |

**支持的传感器类型:**
| sensor | 含义 | 默认阈值 | 报警规则 |
|--------|------|----------|----------|
| temp | 温度 | 35.0°C | 超过阈值报警 |
| hum | 湿度 | 90.0% | 超过阈值报警 |
| light | 光照 | 1000.0lux | 超过阈值报警 |
| soil | 土壤湿度 | 30.0% | 低于阈值报警 |

**响应格式:**
```json
{
  "type": "threshold_ack",
  "deviceId": "stm32_001",
  "sensor": "temp",
  "value": 38,
  "success": true
}
```

### TCP转发协议

后端收到WebSocket请求后，会转换为TCP命令发送给STM32：

**TCP命令格式:**
```
SET_THRESHOLD temp 38.0
```

**命令说明:**
- `SET_THRESHOLD` - 固定命令头
- `temp` - 传感器类型 (temp/hum/light/soil)
- `38.0` - 阈值数值（保留一位小数）

### STM32行为

STM32收到命令后实时更新阈值：

```c
// 示例：收到 SET_THRESHOLD temp 38.0
if (strcmp(command, "SET_THRESHOLD") == 0) {
    char sensor[10];
    float value;
    sscanf(params, "%s %f", sensor, &value);
    
    if (strcmp(sensor, "temp") == 0) {
        temp_threshold = value;
    } else if (strcmp(sensor, "hum") == 0) {
        hum_threshold = value;
    } else if (strcmp(sensor, "light") == 0) {
        light_threshold = value;
    } else if (strcmp(sensor, "soil") == 0) {
        soil_threshold = value;
    }
}
```

### 后端日志示例

**收到阈值修改时:**
```
收到阈值修改:
device=stm32_001
sensor=temp
value=38.0
```

**发送TCP命令时:**
```
发送TCP命令:
SET_THRESHOLD temp 38.0
```

**设置成功时:**
```
设置温度阈值为: 38.0
```

### 前端交互要求

每个传感器卡片底部添加滑块：

**拖动时:**
- 实时更新阈值显示数值
- 不发送请求

**松开鼠标后:**
- 发送WebSocket阈值更新请求
- 等待后端确认
- 显示更新结果

### UI显示示例

**温度卡片:**
```
🌡️ 温度传感器
报警阈值: >35°C
[====|=====] 滑块
当前值: 35°C
```

**湿度卡片:**
```
💧 湿度传感器
报警阈值: >90%
[====|=====] 滑块
当前值: 90%
```

**光照卡片:**
```
☀️ 光照传感器
报警阈值: >1000lux
[====|=====] 滑块
当前值: 1000lux
```

**土壤湿度卡片:**
```
🌱 土壤湿度传感器
报警阈值: <30%
[====|=====] 滑块
当前值: 30%
```

### 测试页面

项目根目录提供了测试页面：`test-threshold.html`

**使用方法:**
1. 启动后端服务
2. 在浏览器中打开 `test-threshold.html`
3. 拖动滑块调整阈值
4. 观察日志和控制台输出

---

## 📞 技术支持

如有问题，请检查：
1. 服务器是否正常启动
2. 端口是否正确（HTTP: 8080, TCP: 9000, WS: 8080）
3. 防火墙是否开放相应端口
4. 查看浏览器控制台错误信息
5. 查看服务器日志

**日志位置:** `logs/spring.log`

---

**文档版本:** v1.1  
**更新日期:** 2026-04-28  
**适用版本:** web-ai-stm32 v2.1
