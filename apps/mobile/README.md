# MotoLink Mobile

## 运行

```bash
./tool/bootstrap_platforms.sh
flutter pub get
flutter run --dart-define=DEMO_MODE=true
```

`DEMO_MODE=true` 不依赖后端，便于先看完整交互。设置为 `false` 后，登录、附近、房间和抢麦会调用 Spring Boot API。

## 原生桥接

Channel 名称：`motolink/riding_core`

当前命令：

- `joinRoom`
- `startTransmit`
- `stopTransmit`
- `leaveRoom`

原生模板只回传成功，下一步由 RTC POC 替换。禁止通过 Channel 传输 PCM 音频帧。
