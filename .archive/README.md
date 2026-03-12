# 翎绛指挥中心（Linjiang Command Center）

**一句话描述**：跨平台、跨网络的 OpenClaw 多实例管理和监控系统

**当前状态**：🟡 规划阶段

**核心价值**：让翎麟在任何地方都能高效指挥多个 OpenClaw 实例（翎绛/翎岚/翎云...），实时监控 sub-agent 工作状态

---

## 核心功能

### MVP（必须有）
- ✅ 多实例管理（发现、连接、切换）
- ✅ Main session 对话（历史记录、流式回复）
- ✅ Sub-agent 监控（实时状态、任务详情）
- ✅ 跨网络通信（VPN 或其他自主可控方案）
- ✅ 实例命名（持久化、可修改）

### 增强功能
- 🟡 进度通知（sub-agent 完成/失败推送）
- 🟡 健康检查（CPU、内存、磁盘、API 额度）
- 🟡 快捷指令（常用命令快捷按钮）
- 🟡 记忆搜索（搜索 MEMORY.md）
- 🟡 每日总结（自动生成工作摘要）

---

## 技术架构

### 客户端
- **Android App**（Kotlin + Jetpack Compose）
- **可能的扩展**：iOS、Web、桌面端

### 服务端
- **OpenClaw 实例**（多个，分布在不同设备/网络）
- **连接方式**：
  - 方案 A：自建 VPN（WireGuard）
  - 方案 B：Skill 插件（OpenClaw 内置服务发现）
  - 方案 C：混合方案

### 通信协议
- HTTP REST API（发送消息、查询状态）
- WebSocket（实时推送、流式回复）
- mDNS/服务发现（自动发现实例）

---

## 项目结构

```
linjiang-command-center/
├── README.md              # 本文件
├── PLAN.md                # 完整开发计划
├── STATUS.md              # 实时状态
├── CHANGELOG.md           # 变更日志
├── TESTING.md             # 测试记录
├── LESSONS.md             # 经验教训
├── docs/                  # 设计文档
│   ├── architecture.md    # 架构设计
│   ├── api-design.md      # API 设计
│   ├── ui-design.md       # 界面设计
│   └── vpn-setup.md       # VPN 配置指南
├── src/                   # 源代码
│   ├── android/           # Android App
│   └── skill/             # OpenClaw Skill（可选）
├── tests/                 # 测试代码
├── scripts/               # 自动化脚本
│   ├── create-checkpoint.sh
│   ├── update-status.sh
│   ├── log-test.sh
│   └── daily-report.sh
└── .checkpoints/          # 检查点快照
```

---

## 开发原则

1. **不靠记忆，靠文件** - 所有状态、进度、问题都记录在文件中
2. **不靠自觉，靠流程** - 自动化检查点、测试、报告
3. **专家思维** - 带入香农、图灵、冯·诺依曼等大师的思维方式
4. **产品思维** - 先想"谁用、为什么持续用"再想"怎么实现"

---

## 快速开始

（开发完成后补充）

---

**创建时间**：2026-03-08 15:10  
**负责人**：翎绛 🪶  
**项目周期**：预计 2-3 周
