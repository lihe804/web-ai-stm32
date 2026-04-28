# 动态阈值调整功能使用说明

## 功能概述

本功能支持前端动态调整以下4个传感器的报警阈值：

1. **温度阈值** (temp) - 超过阈值报警
2. **湿度阈值** (hum) - 超过阈值报警  
3. **光照阈值** (light) - 超过阈值报警
4. **土壤湿度阈值** (soil) - 低于阈值报警

## 工作流程

```
前端拖动滑块 
    ↓ 
发送WebSocket请求 
    ↓ 
后端更新阈值并记录日志 
    ↓ 
通过TCP转发给STM32 
    ↓ 
STM32立即生效
```

## 快速开始

### 1. 启动后端服务

```bash
mvn clean package
java -jar target/web-ai-stm32-0.0.1-SNAPSHOT.jar
```

### 2. 打开测试页面

在浏览器中打开项目根目录下的 `test-threshold.html` 文件。

### 3. 调整阈值

- 拖动滑块实时查看阈值变化
- 松开鼠标后自动发送更新请求
- 观察页面日志和后端控制台输出

## WebSocket 协议

### 请求格式

```json
{
  "type": "threshold",
  "deviceId": "stm32_001",
  "sensor": "temp",
  "value": 38
}
```

### 响应格式

```json
{
  "type": "threshold_ack",
  "deviceId": "stm32_001",
  "sensor": "temp",
  "value": 38,
  "success": true
}
```

## TCP 命令格式

后端会将WebSocket请求转换为TCP命令发送给STM32：

```
SET_THRESHOLD temp 38.0
```

## 后端日志示例

### 收到阈值修改

```
收到阈值修改:
device=stm32_001
sensor=temp
value=38.0
```

### 发送TCP命令

```
发送TCP命令:
SET_THRESHOLD temp 38.0
```

### 设置成功

```
设置温度阈值为: 38.0
```

## STM32 端实现参考

```c
// 解析SET_THRESHOLD命令
if (strcmp(command, "SET_THRESHOLD") == 0) {
    char sensor[10];
    float value;
    sscanf(params, "%s %f", sensor, &value);
    
    if (strcmp(sensor, "temp") == 0) {
        temp_threshold = value;
        printf("温度阈值更新为: %.1f\n", value);
    } else if (strcmp(sensor, "hum") == 0) {
        hum_threshold = value;
        printf("湿度阈值更新为: %.1f\n", value);
    } else if (strcmp(sensor, "light") == 0) {
        light_threshold = value;
        printf("光照阈值更新为: %.1f\n", value);
    } else if (strcmp(sensor, "soil") == 0) {
        soil_threshold = value;
        printf("土壤湿度阈值更新为: %.1f\n", value);
    }
}
```

## 默认阈值配置

| 传感器 | 默认阈值 | 单位 | 报警规则 |
|--------|----------|------|----------|
| 温度 | 35.0 | °C | > 阈值报警 |
| 湿度 | 90.0 | % | > 阈值报警 |
| 光照 | 1000.0 | lux | > 阈值报警 |
| 土壤湿度 | 30.0 | % | < 阈值报警 |

## 注意事项

1. **阈值范围建议**：
   - 温度：-50 ~ 150°C
   - 湿度：0 ~ 100%
   - 光照：0 ~ 10000lux
   - 土壤湿度：0 ~ 100%

2. **实时更新**：阈值修改后立即生效，无需重启设备

3. **持久化**：当前版本阈值存储在内存中，重启后会恢复默认值。如需持久化，可扩展保存至数据库或配置文件

4. **设备在线**：确保设备在线才能成功发送TCP命令

## 故障排查

### WebSocket连接失败
- 检查后端服务是否启动
- 确认端口8080未被占用
- 检查防火墙设置

### 阈值更新失败
- 检查设备ID是否正确
- 确认设备在线状态
- 查看后端日志错误信息

### TCP命令发送失败
- 检查Netty服务器是否正常运行
- 确认TCP端口9000可访问
- 验证设备连接状态

## 技术架构

- **前端**: HTML5 + JavaScript (WebSocket API)
- **后端**: Spring Boot + Netty + WebSocket
- **通信协议**: WebSocket (前端↔后端), TCP (后端↔STM32)
- **数据格式**: JSON

## 扩展建议

1. **阈值持久化**: 将阈值保存到数据库或配置文件
2. **阈值范围限制**: 在后端添加阈值范围验证
3. **批量更新**: 支持一次性更新多个传感器阈值
4. **阈值历史记录**: 记录阈值变更历史
5. **权限控制**: 添加用户认证和授权机制

---

**版本**: v1.0  
**更新日期**: 2026-04-28
