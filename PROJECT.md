# 翎绛指挥中心（Linjiang Command Center）

> 不是另一个聊天窗口。是 AI 实例的指挥控制台。

## 状态
- **当前阶段**: M8 触控指挥（核心完成，手势待做）+ UI 全面重设计完成
- **版本**: v0.12.0
- **更新时间**: 2026-03-10 22:04

## 关键文件
| 文件 | 用途 |
|------|------|
| `docs/DESIGN.md` | UI 设计方案（布局/色彩/数据流/实现计划）⭐ |
| `docs/CHANGELOG.md` | 版本变更日志 |
| `ui-prototype/index.html` | 高保真 UI 原型（浏览器打开预览） |
| `src/android/` | Android App 源码（Kotlin + Jetpack Compose） |
| `src/openclaw-relay-client/` | OpenClaw 中继客户端（Node.js） |
| `relay-server-wss.js` | 中继服务器源码（WS+WSS 双端口） |

## 里程碑
- ✅ M1: 中继服务器 MVP（2026-03-08）
- ✅ M2: Android App 基础框架（2026-03-08）
- ✅ M3: 实例管理增强 + 消息双向流转（2026-03-09）
- ✅ M4: 对话功能增强 — 流式输出/独立会话/快捷指令（2026-03-10）
- ✅ M5: Sub-agent 监控（2026-03-08，含在 M2）
- ✅ M6: 基础增强 — 认证/持久化/编辑/通知/WSS 服务端（2026-03-10）
- ✅ M7: 态势感知 — 仪表盘/健康监控/任务看板/时间线（2026-03-10）
- 🟡 **M8: 触控指挥** — 任务派发✅/手势操作⬜/语音指令⬜
- ⬜ **M8.5: 体验基础** — R010 大任务执行反馈⭐/R005 自动连接/R004 设置页重构/R006 接入认证
- ⬜ **M8.6: 消息可靠** — R001 推送与保活/R002 跨实例通知/R003 长任务断线恢复/R008 实例切换+会话持久化
- ⬜ **M8.7: 安全运维** — R009 实例 API 状态监控/R007 翎云自动运维
- ⬜ M9: 跨实例协调 — 多实例并行/任务编排
- ⬜ M10: 智能中枢 — 告警引擎/知识管理/自动化

## 当前任务
**下一步：M8 手势操作 + 语音指令**
- 任务派发链路已打通（v0.9.0：dispatch/update/cancel 三端）
- 记忆页 + 设置页已完成（v0.10.0）
- UI 全面重设计完成（v0.11.0-v0.12.0：JARVIS 毛玻璃风格）
- 待做：手势操作、语音指令

## 部署信息
| 组件 | 地址 | 备注 |
|------|------|------|
| 中继服务器 | 118.25.195.154 WS:8080 + WSS:8443 | PM2 运行，`/root/linjiang-relay/server.js` |
| 中继服务器 SSH | `sshpass -p 'LL17664090912.' ssh root@118.25.195.154` | |
| PM2 路径 | `/root/.nvm/versions/node/v22.22.0/lib/node_modules/pm2/bin/pm2` | |
| OpenClaw 客户端 | 本地 VM，systemd 服务 | `src/openclaw-relay-client/index.js` |
| WSS 域名 | `wss://api.lingjiangapp.online:8443` | 证书有效至 2026-06-03 |
| 认证 Token | `84c348bea7be634216ef5277cf84e4b2bfbbbf2df3d6d2e3` | 三端共用 |
| APK 输出 | `src/android/app/build/outputs/apk/debug/app-debug.apk` | |
| 构建命令 | `cd src/android && ./gradlew assembleDebug` | |
| 翎麟飞书 | `ou_49758be6de6a8443ec1b2e2e1165caba` | 发 APK 用 |

## 架构
```
Android App ←WSS→ 中继服务器（翎云）←WS→ OpenClaw 客户端（VM）
                   118.25.195.154          本地 VM
```

消息类型：
- `register` / `registered` — 注册
- `message` — 对话消息
- `stream_start` / `stream_chunk` / `stream_end` — 流式输出
- `health_report` — 健康数据上报（M7 新增）
- `subagent_status` — Sub-agent 状态（M7 新增）
- `alert` — 告警（M7 新增）
- `timeline_event` — 时间线事件（M7 新增）

## 技术栈
- **Android**: Kotlin + Jetpack Compose + Material3 + OkHttp WebSocket
- **中继服务器**: Node.js + ws + https（WSS）
- **OpenClaw 客户端**: Node.js + ws + child_process

## 教训
| 时间 | 坑 | 解决 |
|------|-----|------|
| 03-09 | ViewModel 之间不自动共享 RelayClient | MainActivity 手动桥接 |
| 03-10 | `optString("key", null)` 返回 Nothing? | 改用 `opt("key") as? String` |
| 03-10 | OutlinedButton 在窄空间挤掉 emoji | 改用 IconButton |
| 03-10 | RelayClient.messages.collect 覆盖 ViewModel 消息 | 改为合并而非覆盖 |
| 03-10 | nginx WSS 反代外部连不上 | Node.js 直接启动 WSS:8443 绕开 |
| 03-10 | HTTPS_PROXY 干扰 WSS 连接测试 | 本机代理问题，不影响手机/服务器 |
| 03-10 | 9 个 md 文件职责重叠 compaction 后找不到 | 统一为 PROJECT.md 单入口 |

## 项目规范
遵循 `docs/PROJECT-STANDARD.md`：
- **PROJECT.md**：唯一入口（本文件）
- **docs/DESIGN.md**：设计方案
- **docs/CHANGELOG.md**：版本日志
- 旧文件已归档到 `.archive/`

---

## 发布规则
- **发 APK 前必须先 git commit + push**，没有例外
- 构建脚本 `scripts/build-and-ship.sh` 强制执行此规则
- 直接 `gradlew assembleDebug` 不会被阻止，但发给翎麟前必须走脚本

**创建时间**: 2026-03-08
**负责人**: 翎绛 🪶
