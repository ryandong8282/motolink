# ADR 0001：Flutter 业务层 + 双端原生 Riding Core

- 状态：Accepted
- 日期：2026-09-07

## 决策

普通业务页面和业务状态使用 Flutter；后台音频、系统 PTT、蓝牙、音频焦点、后台定位与 RTC 引擎生命周期放入 Swift/Kotlin 原生模块。

## 原因

纯双原生会重复大量社交与管理页面；纯 Flutter 又无法可靠掌控骑行核心生命周期。混合架构在开发效率和系统能力之间更平衡。

## 后果

团队仍需具备 Swift 和 Kotlin 能力；原生接口必须保持小而稳定，禁止把音频帧通过 MethodChannel 往返传输。
