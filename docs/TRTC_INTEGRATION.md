# 腾讯 TRTC 纯音频接入

MotoLink 小程序现已完成腾讯 TRTC 的代码接线：

- 小程序使用 `trtc-wx-sdk`；
- 页面渲染原生 `live-pusher` / `live-player`；
- 服务端为当前车队成员生成短期 UserSig；
- 用户进房后默认只收听，不上传麦克风；
- 只有收到 MotoLink 服务端的 `FLOOR_GRANTED` 后才打开音频上行；
- 松手、连接中断、页面进入后台、麦权超时或退出房间时立即关闭音频上行；
- 开启腾讯组件提供的 AGC 自动增益和 ANS 基础噪声抑制。

本阶段先验证手机麦克风与扬声器，不包含头盔蓝牙按键、专用蓝牙路由或高速风噪效果承诺。

## 1. 必须具备的账号条件

1. 企业主体微信小程序，测试号和个人主体不能使用实时推拉流组件；
2. 小程序所属服务类目允许使用 `live-pusher`、`live-player`；
3. 在微信公众平台的接口设置中开通实时播放、实时录制音视频流能力；
4. 腾讯云创建 TRTC 应用并开通可用套餐；
5. 使用真实微信小程序 AppID，不能使用仓库中的 `touristappid` 做真机音频测试；
6. 使用真机测试，微信开发者工具不能完整运行原生推拉流组件。

官方资料：

- 小程序快速集成：<https://cloud.tencent.com/document/product/647/116548>
- `trtc-wx` API：<https://cloud.tencent.com/document/product/647/17018>
- 用户鉴权：<https://cloud.tencent.com/document/product/647/17275>

## 2. 安装小程序 SDK

```bash
cd apps/miniprogram
npm install
```

然后在微信开发者工具中：

1. 导入 `apps/miniprogram`；
2. 菜单选择 **工具 → 构建 npm**；
3. 确认生成 `miniprogram_npm/trtc-wx-sdk`；
4. 在项目设置中启用可用的 Live SDK；
5. 使用真机预览或真机调试。

SDK 版本已固定为 `trtc-wx-sdk@1.1.15`。生成的 `node_modules` 和 `miniprogram_npm` 不提交到 Git。

## 3. 配置服务端密钥

SecretKey 只能放 Java API 的运行环境，绝不能写入小程序代码、配置文件或日志。

macOS / Linux：

```bash
export RTC_PROVIDER=trtc
export TRTC_SDK_APP_ID=1400000000
export TRTC_SECRET_KEY='替换为腾讯云控制台密钥'
export TRTC_USER_SIG_TTL=24h

cd services/api
mvn spring-boot:run
```

Windows PowerShell：

```powershell
$env:RTC_PROVIDER = 'trtc'
$env:TRTC_SDK_APP_ID = '1400000000'
$env:TRTC_SECRET_KEY = '替换为腾讯云控制台密钥'
$env:TRTC_USER_SIG_TTL = '24h'

cd services/api
mvn spring-boot:run
```

API 只会在以下条件全部满足时返回 `configured: true`：

- 当前请求用户已经加入目标车队；
- `RTC_PROVIDER=trtc`；
- `TRTC_SDK_APP_ID` 为正数；
- `TRTC_SECRET_KEY` 非空。

UserSig 使用腾讯官方 v2 HMAC-SHA256 格式在服务端动态生成。返回给小程序的是短期签名，不会返回 SecretKey。

## 4. 配置小程序后端地址

模拟器使用：

```js
apiBaseUrl: 'http://127.0.0.1:8080',
wsBaseUrl: 'ws://127.0.0.1:8080',
```

真机联调需要把 `apps/miniprogram/miniprogram/config/index.js` 改为：

- 同一局域网内电脑的 IP；或
- 已备案并配置 HTTPS/WSS 的测试域名。

正式发布时还需要在微信公众平台配置业务 API 的 request/socket 合法域名，以及腾讯 TRTC 文档列出的服务域名。

## 5. 两台手机验证流程

1. 两台手机使用不同昵称做开发态登录；
2. A 创建车队，B 使用房间码加入；
3. 两端进入同一车队房间，页面应显示“腾讯 TRTC 纯音频已连接”；
4. A 按住 PTT，首次操作同意麦克风权限；
5. B 应实时听到 A，A 松手后 B 不应再收到 A 的音频；
6. A、B 同时按下时只能一人获得服务端麦权；
7. 切断网络或将页面退入后台，当前讲话端应立即关麦并释放麦权；
8. 重新进入房间后再次验证收听和抢麦。

## 6. 当前技术边界

已完成：

- 前台纯音频进房、收听、发布与退房；
- 服务端 UserSig；
- 半双工抢麦与真实音频发布联动；
- AGC/ANS 基础处理；
- 网络质量状态回调；
- 异常和后台场景的安全关麦。

尚未包含：

- 锁屏状态下操作 PTT；
- 微信或小程序被系统结束后的持续通话；
- 头盔蓝牙按键和特定耳机兼容性；
- 专用风噪算法或高速骑行降噪验收；
- 录音、云端存档、语音内容审核；
- 正式微信登录和生产级账号体系。

进入 TRTC 房间后会产生腾讯云用量，测试完成应及时退出房间。成本预算应按用户在房时长和腾讯云实际计费口径估算，而不是只计算按键讲话时间。
