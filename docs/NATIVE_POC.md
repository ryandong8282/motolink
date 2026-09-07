# 原生 RidingCore POC 计划

## 为什么先做 POC

MotoLink 的商业风险不在普通页面，而在锁屏后台音频、蓝牙输入、定位持续性、弱网重连和真实风噪。RTC 厂商选择必须由同一套真机测试结果决定，不能先把整个业务写完再发现核心不可用。

## 统一 RidingCore 状态机

```text
idle
  └─ joinRoom ─> listening
                    ├─ requestFloor ─> publishing
                    │                    └─ releaseFloor ─> listening
                    ├─ networkLost ─> reconnecting ─> listening
                    └─ leaveRoom ─> idle
```

骑行状态与语音状态分离：

```text
rideIdle -> starting -> riding -> finishing -> rideIdle
```

Flutter 只接收高层事件：

- `roomStateChanged`；
- `floorStateChanged`；
- `audioRouteChanged`；
- `networkQualityChanged`；
- `locationUpdated`；
- `rideStateChanged`；
- `fatalError`。

## iOS POC

### 技术点

- Swift；
- PushToTalk framework；
- AVAudioSession；
- CoreLocation；
- APNs Push-to-Talk 通道；
- 蓝牙 HFP/A2DP 路由观测；
- RTC Native SDK。

### 必测场景

1. 前台入房、按住说话、松开释放；
2. 锁屏后持续收听；
3. 系统允许的后台入口恢复讲话；
4. 有线耳机、AirPods、普通蓝牙、头盔蓝牙切换；
5. 电话、Siri、导航播报打断后恢复；
6. Wi-Fi/4G/5G 切换、飞行模式短暂开启、隧道断网；
7. 后台定位连续 2 小时及轨迹补传；
8. 用户强制结束应用后的明确降级行为。

## Android POC

### 技术点

- Kotlin；
- microphone/location Foreground Service；
- AudioManager 与 AudioFocus；
- BluetoothHeadset / CommunicationDevice；
- Fused Location Provider 或地图厂商定位；
- RTC Native SDK；
- 网络切换监听和 WorkManager 补传。

### 必测机型

华为、小米、OPPO、vivo、荣耀、三星至少各一台，并覆盖 Android 10、较新稳定版和目标商店要求的 targetSdk 行为。

### 必测场景

1. 锁屏 30 分钟、2 小时、4 小时；
2. 系统省电、厂商省电和后台限制；
3. 通知栏停止、最近任务划掉、系统任务管理器停止；
4. 蓝牙设备连接、断开和麦克风路由变化；
5. 电话、导航、音乐抢占音频焦点；
6. 地铁/隧道、蜂窝切换和高丢包；
7. 前台服务异常被杀后的用户可见恢复流程。

## RTC 厂商对比表

每家供应商使用相同设备、路线、房间人数和脚本，记录：

| 指标 | 统计口径 |
|---|---|
| 请求麦权到首帧可听 | P50 / P95 / P99 |
| 入房成功率 | 成功次数 / 总尝试次数 |
| 网络切换恢复时间 | 音频中断到再次可听 |
| 20 人房间下行稳定性 | 丢包、卡顿、CPU、耗电 |
| 风噪可懂度 | 盲听评分与关键字识别率 |
| 蓝牙兼容性 | 每种耳机输入/输出路由结果 |
| SDK 对系统音频会话影响 | 是否擅自重置类别、模式、路由 |
| 后台恢复 | 系统允许场景下的成功率 |
| 成本 | 按用户在房时长和增值能力估算 |

## POC 通过门槛

门槛必须由甲方确认测试设备和环境后写入验收附件。建议至少满足：

- 半双工同一房间不出现双重有效麦权；
- 正常蜂窝网络下 P95 首帧可听延迟达到双方约定值；
- 短时断网后在约定时间内自动恢复；
- 锁屏和后台行为符合系统规则，不使用静音保活等违规手段；
- 轨迹离线保存、去重补传和结束结算可验证；
- 无密钥写入客户端或日志；
- 对无法保证的强制结束、权限撤销、极限省电场景有明确用户提示。

POC 不通过时，先选择更换 RTC、调整产品交互或降低承诺，不能直接进入完整版固定范围开发。
