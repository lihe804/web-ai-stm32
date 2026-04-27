# 工业级 IoT 平台 - 快速启动指南

## 🚀 5分钟快速启动

### 1️⃣ 编译项目
```bash
mvn clean package -DskipTests
```

### 2️⃣ 运行应用
```bash
java -jar target/web-ai-stm32-0.0.1-SNAPSHOT.jar
```

或者在IDE中直接运行 `WebAiStm32Application.java`

### 3️⃣ 验证服务
```bash
# 检查HTTP API
curl http://localhost:8080/api/device/list

# 应该返回: {"success":true,"data":[],"count":0}
```

---

## 📱 设备端接入

### STM32 + ESP8266 连接示例

```cpp
#include <ESP8266WiFi.h>

const char* ssid = "your_wifi_ssid";
const char* password = "your_wifi_password";
const char* server = "192.168.1.100";  // 服务器IP
const int port = 9000;

WiFiClient client;

void setup() {
    Serial.begin(115200);
    
    // 连接WiFi
    WiFi.begin(ssid, password);
    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        Serial.print(".");
    }
    Serial.println("\nWiFi connected");
}

void loop() {
    // 连接TCP服务器
    if (!client.connected()) {
        if (client.connect(server, port)) {
            Serial.println("Connected to server");
        } else {
            delay(1000);
            return;
        }
    }
    
    // 读取传感器数据
    float temp = readTemperature();
    float humidity = readHumidity();
    float light = readLight();
    float soil = readSoilMoisture();
    
    // 构建JSON数据
    String jsonData = "{\"deviceId\":\"stm32_001\",";
    jsonData += "\"temp\":" + String(temp) + ",";
    jsonData += "\"humidity\":" + String(humidity) + ",";
    jsonData += "\"light\":" + String(light) + ",";
    jsonData += "\"soil\":" + String(soil) + "}";
    
    // 发送数据
    client.println(jsonData);
    
    // 每10秒发送一次
    delay(10000);
    
    // 发送心跳（每30秒）
    static unsigned long lastHeartbeat = 0;
    if (millis() - lastHeartbeat > 30000) {
        client.println("{\"type\":\"heartbeat\",\"deviceId\":\"stm32_001\"}");
        lastHeartbeat = millis();
    }
}
```

---

## 🌐 前端WebSocket连接

### Vue3 示例代码

```javascript
import { ref, onMounted, onUnmounted } from 'vue'

export default {
    setup() {
        const ws = ref(null)
        const devices = ref([])
        const alarms = ref([])
        
        const connectWebSocket = () => {
            ws.value = new WebSocket('ws://localhost:8080/ws')
            
            ws.value.onopen = () => {
                console.log('WebSocket connected')
            }
            
            ws.value.onmessage = (event) => {
                const data = JSON.parse(event.data)
                
                switch(data.type) {
                    case 'device_data':
                        // 更新设备数据
                        updateDevice(data)
                        break
                    case 'status':
                        // 更新设备状态
                        updateStatus(data)
                        break
                    case 'alarm':
                        // 显示报警
                        showAlarm(data)
                        break
                }
            }
            
            ws.value.onerror = (error) => {
                console.error('WebSocket error:', error)
            }
            
            ws.value.onclose = () => {
                console.log('WebSocket closed')
                // 5秒后重连
                setTimeout(connectWebSocket, 5000)
            }
        }
        
        const updateDevice = (data) => {
            const index = devices.value.findIndex(d => d.deviceId === data.deviceId)
            if (index >= 0) {
                devices.value[index] = data
            } else {
                devices.value.push(data)
            }
        }
        
        const updateStatus = (data) => {
            const device = devices.value.find(d => d.deviceId === data.deviceId)
            if (device) {
                device.online = data.online
            }
        }
        
        const showAlarm = (data) => {
            alarms.value.unshift(data)
            // 显示通知
            alert(`报警: ${data.message}`)
        }
        
        onMounted(() => {
            connectWebSocket()
        })
        
        onUnmounted(() => {
            if (ws.value) {
                ws.value.close()
            }
        })
        
        return {
            devices,
            alarms
        }
    }
}
```

---

## 🔧 REST API 使用示例

### 1. 获取所有设备
```bash
curl http://localhost:8080/api/device/list
```

响应:
```json
{
  "success": true,
  "data": [
    {
      "deviceId": "stm32_001",
      "temp": 28.5,
      "humidity": 66,
      "light": 800,
      "soil": 35,
      "online": true,
      "timestamp": 1234567890
    }
  ],
  "count": 1
}
```

### 2. 获取在线设备
```bash
curl http://localhost:8080/api/device/online
```

### 3. 开启蜂鸣器
```bash
curl -X POST http://localhost:8080/api/device/buzzer/on \
  -H "Content-Type: application/json" \
  -d '{"deviceId":"stm32_001"}'
```

响应:
```json
{
  "success": true,
  "message": "指令已发送: BUZZER_ON"
}
```

### 4. 关闭蜂鸣器
```bash
curl -X POST http://localhost:8080/api/device/buzzer/off \
  -H "Content-Type: application/json" \
  -d '{"deviceId":"stm32_001"}'
```

### 5. 重启设备
```bash
curl -X POST http://localhost:8080/api/device/restart \
  -H "Content-Type: application/json" \
  -d '{"deviceId":"stm32_001"}'
```

### 6. 获取报警列表
```bash
curl http://localhost:8080/api/alarm/list
```

### 7. 处理报警
```bash
curl -X POST http://localhost:8080/api/alarm/handle \
  -H "Content-Type: application/json" \
  -d '{"deviceId":"stm32_001","alarmType":"SOIL_DRY"}'
```

---

## 🧪 测试工具

### 使用 Netcat 测试TCP连接

```bash
# 发送设备数据
echo '{"deviceId":"test_001","temp":25.5,"humidity":60,"light":750,"soil":40}' | nc localhost 9000

# 发送心跳
echo '{"type":"heartbeat","deviceId":"test_001"}' | nc localhost 9000

# 持续发送（模拟设备）
while true; do
  echo '{"deviceId":"test_001","temp":25.5,"humidity":60,"light":750,"soil":40}' | nc localhost 9000
  sleep 10
done
```

### 使用 Python 测试

```python
import socket
import json
import time

def test_device():
    client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    client.connect(('localhost', 9000))
    
    # 发送数据
    data = {
        "deviceId": "test_001",
        "temp": 28.5,
        "humidity": 66,
        "light": 800,
        "soil": 35
    }
    
    client.send((json.dumps(data) + '\n').encode())
    print("Data sent")
    
    # 发送心跳
    time.sleep(5)
    heartbeat = {
        "type": "heartbeat",
        "deviceId": "test_001"
    }
    client.send((json.dumps(heartbeat) + '\n').encode())
    print("Heartbeat sent")
    
    client.close()

if __name__ == '__main__':
    test_device()
```

---

## 📊 监控和调试

### 查看日志

```bash
# 实时查看日志
tail -f logs/spring.log

# 查看设备连接日志
grep "设备上线" logs/spring.log

# 查看报警日志
grep "触发报警" logs/spring.log

# 查看错误日志
grep "ERROR" logs/spring.log
```

### 关键日志示例

```
2026-04-27 19:00:00 INFO  - 设备上线: stm32_001, IP: 192.168.1.100
2026-04-27 19:00:01 INFO  - 设备ID: stm32_001, 温度: 28.5, 湿度: 66.0, 光照: 800.0, 土壤: 35.0
2026-04-27 19:00:01 WARN  - 触发报警: 设备=stm32_001, 类型=SOIL_DRY, 消息=土壤缺水: 35.0% (阈值: 30.0%)
2026-04-27 19:00:05 INFO  - 收到设备心跳: stm32_001
2026-04-27 19:00:10 INFO  - 向设备发送消息: stm32_001, 消息: BUZZER_ON
```

---

## ⚠️ 常见问题

### 1. 端口被占用
**问题**: `BindException: Address already in use`

**解决**:
```bash
# 查找占用端口的进程
netstat -ano | findstr :9000
taskkill /PID <PID> /F

# 或者修改端口
# 在 application.properties 中
netty.port=9001
```

### 2. WebSocket连接失败
**问题**: `WebSocket connection failed`

**解决**:
- 检查服务器是否启动
- 检查防火墙设置
- 确认URL正确: `ws://localhost:8080/ws`
- 浏览器控制台查看详细错误

### 3. 设备无法连接
**问题**: TCP连接超时

**解决**:
- 确认服务器IP和端口正确
- 检查网络连通性
- 确认防火墙开放9000端口
- 查看服务器日志是否有连接记录

### 4. 数据不推送
**问题**: 前端收不到WebSocket消息

**解决**:
- 检查WebSocket连接状态
- 确认设备数据格式正确
- 查看服务器日志是否有推送记录
- 浏览器控制台检查接收到的消息

---

## 🎯 下一步

1. **配置生产环境**
   - 修改数据库配置
   - 配置日志轮转
   - 设置监控告警

2. **前端开发**
   - 创建设备监控页面
   - 实现报警通知
   - 添加历史数据图表

3. **设备扩展**
   - 添加更多传感器
   - 实现OTA升级
   - 增加本地存储

4. **性能优化**
   - 添加Redis缓存
   - 实现消息队列
   - 负载均衡部署

---

## 📞 获取帮助

- 查看完整架构文档: `ARCHITECTURE.md`
- 检查日志文件: `logs/spring.log`
- API文档: 访问 `http://localhost:8080/api`

---

**祝您使用愉快！** 🎉
