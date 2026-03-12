# 翎绛指挥中心 — UI 设计方案 v2.0

> 不是另一个聊天窗口。是 AI 实例的指挥控制台。
> 
> 设计哲学：
> - **老子**：无为而治 — 信息主动找你，不是你找信息
> - **香农**：3 个数字消除 3 个最大的不确定性
> - **深泽直人**：无意识设计 — 交互自然到你不觉得在操作
> - **塔夫特**：每个像素都承载意义，数据墨水比最大化
> - **拉姆斯**：好设计是尽可能少的设计

---

## 1. 信息架构

### 1.1 核心问题（打开 App 需要回答的）
用户打开 App 脑子里有且仅有三个问题：
1. **一切正常吗？**（实例在线数 + 健康状态）
2. **在忙什么？**（执行中的任务数 + 详情）
3. **需要我介入吗？**（待处理告警数）

### 1.2 五个页面
| Tab | 名称 | 图标 | 核心职责 | 飞书能做吗 |
|-----|------|------|---------|-----------|
| 1 | 态势 | 📡 | 全局状态一览 | ❌ |
| 2 | 任务 | 📋 | 任务管理与派发 | ❌ |
| 3 | 对话 | 💬 | 与实例交互（凸起按钮） | ✅ 但我们更专 |
| 4 | 记忆 | 🧠 | 知识与记忆管理 | ❌ |
| 5 | 设置 | ⚙️ | 实例管理与偏好 | — |

### 1.3 导航层级
```
底部导航（5 tabs）
├── 态势（默认首页）
│   ├── 态势总览（3 个数字）
│   ├── 告警横幅（最紧急的浮在顶部）
│   ├── 实例条（横滑卡片）
│   ├── 活跃任务流
│   ├── 快捷指令（4 个按钮）
│   └── 今日时间线
├── 任务
│   ├── 看板视图（待执行/执行中/已完成/失败）
│   ├── 派发任务（选实例→选模板→参数→发）
│   └── 任务详情（日志/输入/输出）
├── 对话（中间凸起绛色按钮）
│   ├── 实例选择器（顶部下拉）
│   ├── 消息列表
│   ├── 输入框 + 快捷指令
│   └── 连接状态指示
├── 记忆
│   ├── 搜索框
│   ├── 最近记忆
│   ├── 分类浏览（项目/人物/教训/基础设施）
│   └── 项目看板（从 PROGRESS.md 自动生成）
└── 设置
    ├── 实例管理（增删改）
    ├── 通知偏好
    ├── 安全（WSS/Token）
    └── 关于
```

---

## 2. 页面设计

### 2.1 态势页（默认首页）

#### 布局（从上到下）

```
┌─────────────────────────────┐
│ [●] 翎绛指挥中心    [全部在线]│ ← 状态栏（sticky）
├─────────────────────────────┤
│  ┌─────┐ ┌─────┐ ┌─────┐   │
│  │  2  │ │  3  │ │  1  │   │ ← 态势总览（3 个数字卡片）
│  │在线  │ │执行中│ │告警  │   │
│  └─────┘ └─────┘ └─────┘   │
├─────────────────────────────┤
│ ⚠️ 翎绛 磁盘使用 90%  5min │ ← 告警横幅（有告警时显示）
├─────────────────────────────┤
│ 实例                  管理→ │
│ ┌────────┐┌────────┐┌────  │ ← 实例条（横滑）
│ │🪶翎绛  ││🦅翎翼  ││☁️翎  │   每个卡片：头像+名字+
│ │CPU 12% ││CPU 8%  ││...  │   CPU/MEM/DISK + 资源条
│ │MEM 4.2G││MEM 2.1G││     │
│ │████░90%││███░ 45%││     │
│ └────────┘└────────┘└────  │
├─────────────────────────────┤
│ 活跃任务              全部→ │
│ ┌───────────────────────┐   │ ← 任务卡片
│ │⚡ FBX武器标注测试      │   │   图标+标题+实例标签
│ │  sub-agent #a7f3 · 4m │   │   进度条+百分比
│ │  ████████░░░░ 65%     │   │
│ └───────────────────────┘   │
│ ┌───────────────────────┐   │
│ │📝 记忆维护            │   │
│ │  sub-agent #b2e1 · 1m │   │
│ │  ░░░░░░░░░░ 进行中    │   │   不定进度=滑动动画
│ └───────────────────────┘   │
├─────────────────────────────┤
│ 快捷指令                    │
│ ┌────┐┌────┐┌────┐┌────┐   │ ← 4 个高频操作
│ │ 🚀 ││ 🔍 ││ 📊 ││ 🛠️ │   │
│ │派发 ││搜索 ││总结 ││自检 │   │
│ └────┘└────┘└────┘└────┘   │
├─────────────────────────────┤
│ 今日动态              全部→ │
│ ✅ 翎绛 完成 v0.7.1   15:35│ ← 时间线
│ │                           │   左侧竖线连接
│ ⚠️ 翎绛 磁盘 90%     15:20│
│ │                           │
│ 🔒 WSS 端口启动      15:06│
│ │                           │
│ 🚀 流式输出部署       14:00│
├─────────────────────────────┤
│ 📡   📋   💬   🧠   ⚙️   │ ← 底部导航（💬 凸起）
│ 态势  任务  对话  记忆  设置 │
└─────────────────────────────┘
```

#### 状态栏组件
| 元素 | 细节 |
|------|------|
| 呼吸绿点 | 所有实例在线=绿色呼吸，有离线=黄色，全离线=红色 |
| 标题 | "翎**绛**指挥中心"，"绛"字用 accent 色 |
| 状态药丸 | "● 全部在线" / "● 1/3 在线" / "● 全部离线" |

#### 态势总览组件（OverviewCards）
| 卡片 | 数字来源 | 颜色逻辑 |
|------|---------|---------|
| 实例在线 | WebSocket 连接中的实例计数 | 全在线=绿，部分=黄，全离线=红 |
| 执行中任务 | 活跃 sub-agent 计数 | >0 蓝色，0 灰色 |
| 待处理告警 | 未读告警计数 | 0=绿色，>0=黄色，紧急=红色 |

点击卡片跳转到对应详情：点实例→实例管理，点任务→任务 Tab，点告警→告警列表。

#### 告警横幅组件（AlertBanner）
- 只显示最紧急的一条
- 背景色跟告警级别走：⚠️黄色/❌红色
- 点击进入告警详情
- 没有告警时不显示（不占空间）

#### 实例条组件（InstanceStrip）
- 横向滑动，一次显示约 2 张卡片
- 每张卡片内容：
  - 头像（emoji + 渐变背景，每个实例不同色）
  - 名称 + 在线状态点（绿/灰）
  - 三个指标：CPU% / MEM(GB) / DISK%
  - 底部资源条（颜色语义：<60%绿，60-85%黄，>85%红）
- 当前选中实例有 accent 色边框发光
- 点击卡片进入对话页（自动选中该实例）
- 离线实例：指标显示 "—"，整体半透明

#### 活跃任务组件（TaskStream）
- 最多显示 3 条（有 "全部→" 入口）
- 每条任务卡片：
  - 左侧图标（执行中=蓝紫渐变，完成=绿底，失败=红底）
  - 标题 + 实例标签（绛色小标签）
  - sub-agent ID + 运行时长
  - 进度条（有百分比的显示百分比，没有的用不定进度动画）
- 完成的任务半透明
- 左滑操作：暂停/终止（M8 实现）
- 点击进入任务详情

#### 快捷指令组件（QuickActions）
| 按钮 | 操作 | 实现方式 |
|------|------|---------|
| 🚀 派发任务 | 打开任务派发 dialog | 跳转到任务 Tab 的派发界面 |
| 🔍 搜记忆 | 打开记忆搜索 | 跳转到记忆 Tab |
| 📊 每日总结 | 生成今日工作摘要 | 调用实例生成，弹 dialog 展示 |
| 🛠️ 全体自检 | 所有实例执行自检 | 批量发指令，结果推到时间线 |

#### 时间线组件（Timeline）
- 垂直时间线，左侧圆点+竖线，右侧内容
- 事件类型：
  - ✅ 任务完成（绿色点）
  - ⚠️ 告警（黄色点）
  - ❌ 错误/失败（红色点）
  - 🚀 部署/更新（蓝色点）
  - 🔒 安全事件（蓝色点）
  - 💤 待命（灰色点）
- 数据来源：从 sub-agent 状态变更 + 告警 + 对话中自动提取
- 默认显示今日前 5 条，点 "全部→" 查看完整时间线

---

### 2.2 任务页

#### 布局

```
┌─────────────────────────────┐
│ 任务                [+ 派发]│ ← 标题 + 新建按钮
├─────────────────────────────┤
│ [全部] [执行中] [已完成] [失败]│ ← 筛选 Tab
├─────────────────────────────┤
│ ┌───────────────────────┐   │
│ │⚡ FBX武器标注测试      │   │ ← 任务卡片（同态势页格式）
│ │  翎绛 · 4m23s         │   │
│ │  ████████░░░░ 65%     │   │
│ └───────────────────────┘   │
│ ┌───────────────────────┐   │
│ │📝 记忆维护            │   │
│ │  翎绛 · 1m12s         │   │
│ │  ░░░░░░░░░░ 进行中    │   │
│ └───────────────────────┘   │
│ ┌───────────────────────┐   │
│ │✅ 安全审查      完成   │   │ ← 已完成（半透明）
│ │  翎绛 · 耗时12m       │   │
│ └───────────────────────┘   │
└─────────────────────────────┘
```

#### 任务派发 Dialog
```
┌─────────────────────────────┐
│ 派发任务               [✕]  │
├─────────────────────────────┤
│ 目标实例                    │
│ [🪶翎绛 ▼]                 │ ← 下拉选择实例
├─────────────────────────────┤
│ 任务模板                    │
│ ┌────┐┌────┐┌────┐┌────┐  │
│ │代码 ││测试 ││文档 ││自检│  │ ← 预设模板
│ └────┘└────┘└────┘└────┘  │
│ ┌────┐┌────┐               │
│ │部署 ││自定义│              │
│ └────┘└────┘               │
├─────────────────────────────┤
│ 任务描述（选"自定义"时显示） │
│ ┌───────────────────────┐  │
│ │                       │  │ ← 文本输入
│ └───────────────────────┘  │
├─────────────────────────────┤
│ 优先级  [普通 ▼]            │
│ 超时    [10 分钟 ▼]         │
├─────────────────────────────┤
│       [取消]    [🚀 派发]   │
└─────────────────────────────┘
```

#### 任务模板定义
| 模板 | 自动生成的 prompt | 超时 |
|------|------------------|------|
| 代码审查 | "审查最近的代码变更，检查质量和安全问题" | 10min |
| 测试 | "执行测试套件，报告结果" | 15min |
| 文档 | "更新项目文档，确保最新" | 10min |
| 自检 | "执行系统自检：磁盘/内存/API余额/服务状态" | 5min |
| 部署 | "构建并部署最新版本" | 20min |
| 自定义 | 用户自己输入 | 自定义 |

#### 任务详情页
```
┌─────────────────────────────┐
│ ← FBX武器标注测试           │
├─────────────────────────────┤
│ 状态：执行中 ⚡              │
│ 实例：🪶 翎绛               │
│ 运行：4m23s                  │
│ 进度：████████░░ 65%        │
├─────────────────────────────┤
│ 实时日志                    │
│ ┌───────────────────────┐  │
│ │> 加载测试用例...       │  │
│ │> 执行 test_fbx_parse  │  │ ← 滚动日志（等宽字体）
│ │> PASS: 12/15          │  │
│ │> 运行 test_sam...     │  │
│ └───────────────────────┘  │
├─────────────────────────────┤
│  [⏸ 暂停]  [⏹ 终止]  [🔄] │
└─────────────────────────────┘
```

---

### 2.3 对话页

和当前类似但增强：

```
┌─────────────────────────────┐
│ ← [🪶 翎绛 ▼]    🟢 已连接 │ ← 实例选择器 + 连接状态
├─────────────────────────────┤
│                             │
│         ┌──────────────┐    │
│         │ 你好翎麟！    │    │ ← AI 消息（左对齐）
│         └──────────────┘    │
│  ┌──────────────┐           │
│  │ 查看今天的任务 │           │ ← 用户消息（右对齐）
│  └──────────────┘           │
│         ┌──────────────┐    │
│         │ 今天完成了... │    │
│         │ 1. v0.7.1... │    │ ← Markdown 渲染
│         │ 2. WSS 加密..│    │
│         └──────────────┘    │
│                             │
├─────────────────────────────┤
│ [状态] [记忆] [待办] [诊断] │ ← 快捷指令 chips
├─────────────────────────────┤
│ ┌───────────────────┐ [➤]  │ ← 输入框 + 发送
└─────────────────────────────┘
```

#### 连接状态指示
| 状态 | 显示 | 颜色 |
|------|------|------|
| 已连接 | 🟢 已连接 | 绿色 |
| 连接中 | 🟡 连接中... | 黄色 |
| 已断开 | 🔴 已断开 (重试 5s) | 红色 |
| 未选择 | — 请选择实例 | 灰色 |

#### 对话页增强（vs 飞书）
- 实例选择器（顶部下拉切换实例，不用切页面）
- 连接状态实时显示 + 自动重连倒计时
- 消息按实例分离存储
- AI 思考状态动画（收到→思考→回复）
- 快捷指令 chips（一键发常用命令）
- 消息长按菜单（复制/转发到其实例/收藏）

---

### 2.4 记忆页

```
┌─────────────────────────────┐
│ 记忆                        │
├─────────────────────────────┤
│ ┌───────────────────────┐   │
│ │ 🔍 搜索记忆...        │   │ ← 搜索框
│ └───────────────────────┘   │
├─────────────────────────────┤
│ 最近记忆                    │
│ ┌───────────────────────┐   │
│ │ FBX 武器标注系统       │   │ ← 记忆卡片
│ │ 92% 准确率，Point-SAM │   │   标题 + 摘要 + 标签
│ │ [项目] [3D] [完成]    │   │
│ └───────────────────────┘   │
│ ┌───────────────────────┐   │
│ │ 百炼 API 配置教训      │   │
│ │ sk-sp-* 是 Coding 专属 │   │
│ │ [教训] [API]          │   │
│ └───────────────────────┘   │
├─────────────────────────────┤
│ 分类                        │
│ ┌────┐┌────┐┌────┐┌────┐  │
│ │📂  ││💡  ││👤  ││🔧  │  │
│ │项目 ││教训 ││人物 ││基建│  │
│ │ 12 ││ 8  ││ 3  ││ 5  │  │
│ └────┘└────┘└────┘└────┘  │
├─────────────────────────────┤
│ 项目看板                全部→│
│ ┌─────────────────────────┐ │
│ │ 指挥中心  M8 ███░ 70%   │ │ ← 自动从 STATUS.md 提取
│ │ FBX标注   ████░ 完成    │ │
│ │ 意识迁移  ████░ 完成    │ │
│ │ 小绛插件  ██░░░ 40%     │ │
│ └─────────────────────────┘ │
└─────────────────────────────┘
```

#### 记忆数据来源
| 分类 | 数据源 | 解析方式 |
|------|-------|---------|
| 项目 | memory/projects.md, PROGRESS.md | 按 ## 标题分割 |
| 教训 | MEMORY.md "教训" 章节 | 按 - 列表项分割 |
| 人物 | MEMORY.md "人物" 章节 | 按 - 列表项分割 |
| 基础设施 | memory/infra.md | 按 ## 标题分割 |

#### 搜索实现
- 调用 OpenClaw 的 memory_search
- 返回结果高亮显示匹配关键词
- 搜索历史本地缓存

---

### 2.5 设置页

```
┌─────────────────────────────┐
│ 设置                        │
├─────────────────────────────┤
│ 实例管理                    │
│  翎绛  wss://api...  ✏️ 🗑️│
│  翎翼  ws://100.75... ✏️ 🗑️│
│  [+ 添加实例]              │
├─────────────────────────────┤
│ 通知                        │
│  消息通知       [开关 ●]    │
│  告警通知       [开关 ●]    │
│  静默时段 23:00-08:00 [>]  │
├─────────────────────────────┤
│ 安全                        │
│  传输加密  WSS    [>]       │
│  认证令牌  ****...4b2 [>]  │
├─────────────────────────────┤
│ 关于                        │
│  版本  v0.7.1               │
│  翎绛指挥中心 🪶            │
└─────────────────────────────┘
```

---

## 3. 视觉规范

### 3.1 色彩系统

| 用途 | 变量名 | 色值 | 语义 |
|------|--------|------|------|
| 深背景 | bg-deep | #0a0b0f | App 底色 |
| 卡片背景 | bg-card | #12141a | 卡片/容器 |
| 卡片悬停 | bg-card-hover | #1a1d26 | 按下反馈 |
| 主文字 | text-primary | #e8eaed | 标题/正文 |
| 次要文字 | text-secondary | #8b8f98 | 标签/说明 |
| 暗文字 | text-dim | #5a5e68 | 时间/辅助 |
| **绛色** | accent | #c9785c | 品牌色/强调 |
| 绛色发光 | accent-glow | #c9785c33 | 选中态发光 |
| 健康 | green | #34d399 | 在线/正常/完成 |
| 警告 | yellow | #fbbf24 | 注意/告警 |
| 危险 | red | #f87171 | 错误/紧急/离线 |
| 进行中 | blue | #60a5fa | 执行中/信息 |

### 3.2 字体规范

- 系统字体栈：SF Pro Display → PingFang SC → sans-serif
- 大数字：28px / Bold / letter-spacing -1px（态势卡片）
- 标题：18px / SemiBold（页面标题）
- 正文：14px / Regular（任务标题、消息正文）
- 辅助：13px / Regular（实例名、描述）
- 标签：11px / Medium（标签、时间戳）
- 微型：9-10px / Medium / uppercase / letter-spacing 0.5px（指标标签）

### 3.3 圆角与间距

- 卡片圆角：16px
- 小组件圆角：10px
- 按钮圆角：10px
- 药丸标签：20px
- 页面内边距：20px（左右）
- 卡片间距：8-10px
- 卡片内边距：14-16px

### 3.4 动画

| 交互 | 动画 | 时长 |
|------|------|------|
| 点击卡片 | scale(0.96) | 150ms |
| 页面进入 | fadeIn + translateY(8px) | 400ms |
| 交错出现 | stagger delay 50ms/item | — |
| 呼吸绿点 | opacity + scale | 2s loop |
| 不定进度 | translateX | 1.5s loop |
| Tab 切换 | crossfade | 200ms |

---

## 4. 数据流设计

### 4.1 数据采集协议

OpenClaw 客户端需要定期上报健康数据，新增消息类型：

```json
// 客户端 → 中继 → App
{
  "type": "health_report",
  "payload": {
    "instanceId": "openclaw-vm-main",
    "cpu": 12.5,         // CPU 使用率 %
    "memory": {
      "used": 4.2,       // GB
      "total": 8.0       // GB  
    },
    "disk": {
      "used": 78,        // GB
      "total": 87,        // GB
      "percent": 90       // %
    },
    "uptime": 86400,      // 秒
    "activeSubAgents": 3,
    "apiBalance": null,   // 如果能获取
    "timestamp": 1741600000
  }
}
```

```json
// 客户端 → 中继 → App
{
  "type": "subagent_status",
  "payload": {
    "instanceId": "openclaw-vm-main",
    "subagents": [
      {
        "id": "sub-a7f3",
        "label": "FBX武器标注测试",
        "status": "running",     // running/completed/failed/pending
        "progress": 65,          // 百分比，null 表示不确定
        "startTime": 1741599000,
        "duration": 263          // 秒
      }
    ]
  }
}
```

```json
// 客户端 → 中继 → App
{
  "type": "alert",
  "payload": {
    "instanceId": "openclaw-vm-main",
    "level": "warning",     // info/warning/critical
    "title": "磁盘使用率高",
    "message": "磁盘使用率达到 90%，建议清理",
    "timestamp": 1741599500
  }
}
```

```json
// 客户端 → 中继 → App
{
  "type": "timeline_event",
  "payload": {
    "instanceId": "openclaw-vm-main",
    "event": "task_completed",  // task_completed/task_failed/deploy/alert/idle
    "title": "完成 v0.7.1 构建",
    "timestamp": 1741599800
  }
}
```

### 4.2 上报频率

| 数据 | 频率 | 理由 |
|------|------|------|
| health_report | 每 60 秒 | 资源变化慢，60s 够了 |
| subagent_status | 每 30 秒 | 任务状态变化快 |
| alert | 实时 | 告警必须即时 |
| timeline_event | 实时 | 事件发生就推 |

### 4.3 Android 端数据模型

```kotlin
// 态势数据
data class InstanceHealth(
    val instanceId: String,
    val cpuPercent: Float,
    val memoryUsedGb: Float,
    val memoryTotalGb: Float,
    val diskPercent: Float,
    val uptime: Long,
    val activeSubAgents: Int,
    val lastUpdate: Long
)

// 告警
data class Alert(
    val id: String,
    val instanceId: String,
    val level: AlertLevel,    // INFO, WARNING, CRITICAL
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false
)

// 时间线事件
data class TimelineEvent(
    val id: String,
    val instanceId: String,
    val eventType: String,    // task_completed, alert, deploy...
    val title: String,
    val timestamp: Long
)

// Sub-agent 任务
data class SubAgentTask(
    val id: String,
    val instanceId: String,
    val label: String,
    val status: TaskStatus,   // PENDING, RUNNING, COMPLETED, FAILED
    val progress: Int?,       // null = indeterminate
    val startTime: Long,
    val duration: Long
)
```

### 4.4 ViewModel 架构

```kotlin
// 新增 ViewModel（态势页专用）
class DashboardViewModel : ViewModel() {
    val instanceHealthMap: StateFlow<Map<String, InstanceHealth>>
    val alerts: StateFlow<List<Alert>>
    val timelineEvents: StateFlow<List<TimelineEvent>>
    val subAgentTasks: StateFlow<List<SubAgentTask>>
    
    // 计算属性
    val onlineCount: Int        // 在线实例数
    val activeTaskCount: Int    // 执行中任务数
    val unreadAlertCount: Int   // 未读告警数
    val overallHealth: Health   // HEALTHY / WARNING / CRITICAL
}

// 原有 ViewModel 保留
class ChatViewModel : AndroidViewModel    // 对话（已有）
class InstanceViewModel : ViewModel       // 实例管理（已有，移到设置页）
```

---

## 5. 交互细节

### 5.1 手势操作（M8 实现）

| 手势 | 位置 | 操作 |
|------|------|------|
| 左滑 | 任务卡片 | 显示"暂停"按钮 |
| 右滑 | 任务卡片 | 显示"终止"按钮 |
| 长按 | 消息气泡 | 弹出菜单（复制/转发/收藏） |
| 下拉 | 态势页顶部 | 刷新所有数据 |
| 点击 | 实例卡片 | 进入与该实例的对话 |
| 点击 | 任务卡片 | 进入任务详情 |
| 点击 | 态势数字 | 跳转到对应详情页 |

### 5.2 通知策略

| 场景 | 系统通知 | 震动 | 声音 |
|------|---------|------|------|
| AI 回复（后台） | ✅ | 短震 | 默认 |
| 任务完成 | ✅ | 短震 | 默认 |
| 任务失败 | ✅ | 长震 | 告警音 |
| 警告告警 | ✅ | 短震 | 默认 |
| 紧急告警 | ✅ 持续 | 连续震 | 紧急铃 |
| 静默时段 | ❌ 累积 | ❌ | ❌ |

### 5.3 推送基础设施（M8.5）

**问题**：当前通知策略依赖 WebSocket 在线，App 关闭/后台被杀后收不到任何通知。

**三层保活方案**：

#### 层1：Android 前台服务（保 WebSocket 不被杀）
- 启动前台 Service，显示常驻通知："翎绛指挥中心 · 已连接 2 个实例"
- WebSocket 连接在 Service 中维持，不受 Activity 生命周期影响
- App 关闭后 Service 继续运行，收到消息弹系统通知
- 用户点击通知进入 App 对应页面

#### 层2：FCM 推送（App 被强杀也能收到）
- 中继服务器集成 Firebase Admin SDK
- 客户端注册 FCM Token，上报给中继服务器
- WebSocket 断开时，中继服务器走 FCM 推送
- 优先级策略：WebSocket 在线→直接推，不在线→FCM
- 推送内容：告警、任务完成/失败、AI 回复摘要

#### 层3：离线消息补拉
- 中继服务器缓存最近 100 条消息（per client）
- 客户端重连时带 `lastMessageId`，服务端返回增量消息
- App 打开时自动补拉错过的消息，按时间排序插入

#### FCM 数据流
```
事件触发 → OpenClaw 客户端 → 中继服务器
                                    ↓
                              WebSocket 在线？
                             ╱              ╲
                          是                  否
                          ↓                   ↓
                    WebSocket 推送       FCM 推送
                          ↓                   ↓
                    App 内处理          系统通知栏
```

#### 中继服务器新增
```json
// 客户端注册 FCM Token
{
  "type": "register_push",
  "payload": {
    "fcmToken": "dL4x...",
    "platform": "android"
  }
}

// 离线消息请求
{
  "type": "sync_messages",
  "payload": {
    "lastMessageId": "msg-xxx",
    "limit": 50
  }
}
```

#### Android 端新增
- `RelayForegroundService.kt` — 前台服务（WebSocket + 通知）
- `FCMService.kt` — Firebase 消息服务
- `NotificationHelper.kt` — 通知渠道管理（消息/告警/任务 三个渠道）

### 5.4 离线行为
- 缓存最近的健康数据、任务列表、时间线
- 断网时显示最后更新时间
- 重新连接后增量同步（带 lastMessageId）

---

## 6. 实现计划

### M7 实现顺序（态势感知）

| 步骤 | 内容 | 依赖 | 估时 |
|------|------|------|------|
| M7.1 | 数据模型（InstanceHealth, Alert, TimelineEvent, SubAgentTask） | 无 | 30min |
| M7.2 | RelayClient 新增消息类型处理（health_report, alert, timeline_event, subagent_status） | M7.1 | 30min |
| M7.3 | DashboardViewModel（聚合所有态势数据） | M7.1 | 30min |
| M7.4 | OpenClaw 客户端健康上报（sys-info 采集 + 定时推送） | 服务端 | 45min |
| M7.5 | 态势页 UI（OverviewCards + AlertBanner + InstanceStrip） | M7.3 | 1h |
| M7.6 | 活跃任务流 UI（TaskStream + 进度条） | M7.3 | 45min |
| M7.7 | 时间线 UI + 快捷指令 | M7.3 | 30min |
| M7.8 | 底部导航改造（5 tabs，对话按钮凸起） | 无 | 30min |
| M7.9 | WSS 切换（客户端 + App 默认地址） | 服务端已完成 | 20min |
| M7.10 | 集成测试 + 编译 | 全部 | 30min |

**预计总耗时：5-6 小时**

### M8.5 实现顺序（体验基础）⭐ 最高优先

> R010 不解决，App 对话功能基本不可用。其余三项消除日常操作摩擦。

| 步骤 | 内容 | 需求 | 依赖 | 估时 |
|------|------|------|------|------|
| M8.5.1 | 消息协议扩展：`process_update` 类型定义 + 中继转发 | R010 | 中继服务器 | 1h |
| M8.5.2 | OpenClaw 客户端适配：拦截 thinking/tool 事件，封装为 `process_update` | R010 | M8.5.1 | 1.5h |
| M8.5.3 | Android 流式过程 UI：思考动画 + 工具调用卡片 + 流式文字 | R010 | M8.5.1 | 2h |
| M8.5.4 | 超时保护机制：三层超时 + 倒计时提示 + 重试入口 | R010 | M8.5.1 | 1h |
| M8.5.5 | 断线恢复：任务结果缓存 + `sync_task_result` 补拉 | R010 | M8.5.1 | 1.5h |
| M8.5.6 | 自动连接：启动即连 + 移除手动连接按钮 | R005 | 无 | 1h |
| M8.5.7 | 设置页重构：移除手动实例管理，改为中继配置 | R004 | M8.5.6 | 45min |
| M8.5.8 | 接入认证：首次接入码输入 + 中继服务端验证 | R006 | M8.5.6 | 1.5h |
| M8.5.9 | 集成测试 + 编译 | — | 全部 | 30min |

**预计总耗时：10-11 小时**

### M8.6 实现顺序（消息可靠）

> 解决"消息丢失"和"切换麻烦"两大痛点。依赖 §5.3 推送基础设施。

| 步骤 | 内容 | 需求 | 依赖 | 估时 |
|------|------|------|------|------|
| M8.6.1 | Android 前台服务：WebSocket 保活 + 常驻通知 | R001 | 无 | 2h |
| M8.6.2 | FCM 集成：中继服务端 Firebase Admin + 客户端 Token 注册 | R001 | M8.6.1 | 2h |
| M8.6.3 | 离线消息补拉：中继缓存 + `sync_messages` 增量拉取 | R001 | M8.6.1 | 1.5h |
| M8.6.4 | 跨实例通知：未读 badge + 前台横幅 + 后台系统通知 | R002 | M8.6.1 | 2h |
| M8.6.5 | 长任务断线恢复：服务端结果缓存 + 重连自动补拉 | R003 | M8.6.3, M8.5.5 | 1.5h |
| M8.6.6 | 对话页实例选择器：顶部下拉/横滑切换 | R008 | 无 | 1h |
| M8.6.7 | 会话持久化：SQLite 本地存储 + 实例隔离 | R008 | M8.6.6 | 2h |
| M8.6.8 | 集成测试 + 编译 | — | 全部 | 30min |

**预计总耗时：12-13 小时**

### M8.7 实现顺序（安全运维）

> 从"手动运维"进化到"主动感知 + 自动修复"。

| 步骤 | 内容 | 需求 | 依赖 | 估时 |
|------|------|------|------|------|
| M8.7.1 | API 状态上报：`health_report` 扩展 `api` 字段 | R009 | 无 | 1h |
| M8.7.2 | OpenClaw 客户端 API 检测：各提供商余额/到期查询 | R009 | M8.7.1 | 2h |
| M8.7.3 | App 端 API 状态展示：实例卡片 + 详情页 + 到期告警 | R009 | M8.7.1 | 1.5h |
| M8.7.4 | 翎云运维 Agent：离线检测 + SSH 诊断 + 自动修复 | R007 | M8.6.1 | 3h |
| M8.7.5 | 运维权限边界：白名单命令 + 审批机制 | R007 | M8.7.4 | 1h |
| M8.7.6 | 集成测试 + 编译 | — | 全部 | 30min |

**预计总耗时：9-10 小时**

---

## 7. M8.5 详细设计：体验基础

### 7.1 R010 大任务执行反馈（⭐ 最高优先级）

#### 7.1.1 问题分析

当前对话链路：用户发消息 → 中继转发 → OpenClaw 处理 → 返回完整回复。大任务（sub-agent 执行、多工具调用）期间，用户看到的是：

1. 发送后界面卡住，无任何视觉反馈
2. 不知道 AI 是在思考、调工具、还是已挂掉
3. WebSocket 超时后回复静默消失，用户一脸懵

Web 端（ChatGPT/Claude）已有的体验：实时看到 thinking → tool calls → 中间结果 → 流式文字。App 端需要对齐。

#### 7.1.2 消息协议扩展

新增 `process_update` 消息类型，覆盖 AI 回复的全生命周期：

```json
// OpenClaw 客户端 → 中继 → App
{
  "type": "process_update",
  "payload": {
    "instanceId": "openclaw-vm-main",
    "requestId": "req-a1b2c3",       // 关联用户的原始消息
    "phase": "thinking_start",        // 阶段标识（见下表）
    "data": {},                       // 阶段特定数据
    "timestamp": 1741600000
  }
}
```

##### 阶段定义

| phase | 含义 | data 结构 | 触发时机 |
|-------|------|-----------|----------|
| `thinking_start` | AI 开始思考 | `{}` | 收到请求，模型开始推理 |
| `thinking_content` | 思考过程文字（流式） | `{ "text": "让我想想..." }` | 模型输出 thinking token（如有） |
| `tool_call` | 调用工具 | `{ "tool": "exec", "args": "ls -la", "callId": "tc-001" }` | 模型发起工具调用 |
| `tool_result` | 工具返回结果 | `{ "callId": "tc-001", "summary": "12 files listed", "success": true }` | 工具执行完成 |
| `text_chunk` | 回复文字片段（流式） | `{ "text": "好的，我来..." }` | 模型输出回复 token |
| `subagent_spawn` | 派生子任务 | `{ "agentId": "sub-a7f3", "label": "代码审查" }` | sub-agent 被创建 |
| `subagent_update` | 子任务进度 | `{ "agentId": "sub-a7f3", "status": "running", "log": "编译中..." }` | sub-agent 状态变化 |
| `complete` | 处理完成 | `{ "finalText": "完整回复..." }` | 全部处理结束 |
| `error` | 处理出错 | `{ "error": "timeout", "message": "任务超时" }` | 异常终止 |
| `timeout_warning` | 即将超时 | `{ "elapsed": 120, "limit": 180, "message": "任务较大，预计还需 1 分钟" }` | 接近超时阈值 |

##### 中继服务器转发逻辑

```javascript
// relay-server-wss.js 新增
case 'process_update':
  // 直接转发给目标 App 客户端，不缓存（实时性优先）
  forwardToApp(ws, message);
  
  // 仅缓存 complete/error 阶段用于断线补拉
  if (['complete', 'error'].includes(payload.phase)) {
    cacheTaskResult(payload.requestId, payload);
  }
  break;
```

##### OpenClaw 客户端拦截点

```
用户消息到达
  ↓
发送 process_update { phase: "thinking_start" }
  ↓
模型开始推理
  ↓ (如有 thinking 输出)
发送 process_update { phase: "thinking_content", data: { text } }
  ↓ (如有工具调用)
发送 process_update { phase: "tool_call", data: { tool, args } }
  ↓
工具执行...
  ↓
发送 process_update { phase: "tool_result", data: { summary, success } }
  ↓ (可能多轮工具调用)
回复文字生成
  ↓
发送 process_update { phase: "text_chunk", data: { text } } × N
  ↓
发送 process_update { phase: "complete", data: { finalText } }
```

#### 7.1.3 Android 端 UI 设计

##### 消息气泡状态机

```
IDLE → THINKING → [TOOL_CALLING ↔ TOOL_RESULT]* → STREAMING → COMPLETE
  ↓                                                              ↓
ERROR ←────────────────── TIMEOUT ←──────────── TIMEOUT_WARNING ─┘
```

##### 思考状态 UI

```
┌──────────────────────────────┐
│ 🪶 翎绛                     │
│ ┌──────────────────────┐     │
│ │ 💭 正在思考...        │     │  ← 绛色呼吸动画
│ │ ·  ·  ·              │     │  ← 三点跳动（150ms 交错）
│ └──────────────────────┘     │
└──────────────────────────────┘
```

- 背景：`bg-card` 半透明 + 绛色 glow 边框（1px `accent-glow`）
- 动画：三个圆点 Y 轴弹跳，交错 150ms，cycle 1.2s
- 思考内容（如有）：灰色斜体文字，折叠在"思考中"下方，点击可展开

##### 工具调用 UI

```
┌──────────────────────────────┐
│ 🪶 翎绛                     │
│ ┌──────────────────────────┐ │
│ │ 🔧 正在执行: exec        │ │  ← 工具名称 + 旋转图标
│ │ └ ls -la /tmp/           │ │  ← 参数（等宽字体，折叠）
│ │   ⏳ 执行中...            │ │  ← 状态（旋转 spinner）
│ └──────────────────────────┘ │
│ ┌──────────────────────────┐ │
│ │ ✅ exec 完成              │ │  ← 工具返回
│ │ └ 12 files listed        │ │  ← 摘要
│ └──────────────────────────┘ │
└──────────────────────────────┘
```

- 工具调用卡片：深色背景 + 左侧绛色竖线（3px）
- 执行中：spinner 旋转 + 脉冲动画
- 完成：绿色 ✅ + 摘要文字
- 失败：红色 ❌ + 错误摘要
- 多个工具调用：垂直堆叠，最新的在底部

##### 流式文字 UI

```
┌──────────────────────────────┐
│ 🪶 翎绛                     │
│ ┌──────────────────────────┐ │
│ │ 好的，我来帮你检查一下文件│ │  ← 文字逐字出现
│ │ 结构。▌                   │ │  ← 闪烁光标
│ └──────────────────────────┘ │
└──────────────────────────────┘
```

- 文字追加渲染（不重绘整个气泡）
- 末尾闪烁竖线光标 `▌`（opacity 0↔1, 500ms）
- 滚动跟随：新内容出现时自动滚到底部（用户手动上滑则暂停跟随）

##### 超时提示 UI

```
┌──────────────────────────────┐
│ ⏰ 任务执行中 (2:15)         │  ← 黄色横幅
│ 预计还需约 1 分钟             │
│                    [取消任务] │
└──────────────────────────────┘
```

超时后：

```
┌──────────────────────────────┐
│ ⚠️ 任务超时                  │  ← 红色横幅
│ 已运行 3:00，未收到完整回复   │
│            [重试]  [查看部分] │
└──────────────────────────────┘
```

#### 7.1.4 Android 数据模型

```kotlin
// 消息处理状态
enum class ProcessPhase {
    IDLE,
    THINKING,
    TOOL_CALLING,
    STREAMING,
    COMPLETE,
    ERROR,
    TIMEOUT
}

// 工具调用记录
data class ToolCallRecord(
    val callId: String,
    val tool: String,
    val args: String,
    val status: ToolStatus,      // RUNNING, SUCCESS, FAILED
    val summary: String? = null,
    val startTime: Long = System.currentTimeMillis()
)

// 消息处理上下文（绑定到一条用户消息）
data class MessageProcessContext(
    val requestId: String,
    val phase: ProcessPhase = ProcessPhase.IDLE,
    val thinkingText: String = "",
    val toolCalls: List<ToolCallRecord> = emptyList(),
    val streamedText: StringBuilder = StringBuilder(),
    val elapsedSeconds: Int = 0,
    val timeoutLimit: Int = 180,  // 默认 3 分钟
    val subAgents: List<SubAgentInfo> = emptyList()
)

// ViewModel 扩展
class ChatViewModel : AndroidViewModel {
    // 现有字段...
    
    // 新增：当前活跃的消息处理上下文
    val activeProcess: StateFlow<MessageProcessContext?>
    
    // 处理 process_update 消息
    fun handleProcessUpdate(payload: JSONObject) {
        when (payload.getString("phase")) {
            "thinking_start" -> updatePhase(ProcessPhase.THINKING)
            "thinking_content" -> appendThinkingText(payload)
            "tool_call" -> addToolCall(payload)
            "tool_result" -> updateToolResult(payload)
            "text_chunk" -> appendStreamText(payload)
            "timeout_warning" -> showTimeoutWarning(payload)
            "complete" -> completeProcess(payload)
            "error" -> handleError(payload)
        }
    }
}
```

#### 7.1.5 超时保护机制

三层超时协同工作：

```
┌─────────────────────────────────────────────────────────────┐
│                        超时保护架构                          │
│                                                             │
│  OpenClaw 服务端            中继服务器           Android App  │
│  ┌───────────────┐    ┌───────────────┐    ┌──────────────┐ │
│  │ L1: 任务超时   │    │ L2: 会话超时   │    │ L3: UI 超时   │ │
│  │ 默认 10min     │    │ 默认 15min     │    │ 默认 3min    │ │
│  │ 可配置         │    │ 固定           │    │ 可延长       │ │
│  │               │    │               │    │              │ │
│  │ 超时→返回错误  │    │ 超时→断开连接  │    │ 超时→提示用户│ │
│  │ 缓存部分结果   │    │ 缓存完整结果   │    │ 提供重试按钮 │ │
│  └───────────────┘    └───────────────┘    └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

| 层级 | 位置 | 默认值 | 触发行为 |
|------|------|--------|----------|
| L1 | OpenClaw 服务端 | 10 分钟 | 向中继发送 `process_update { phase: "error", data: { error: "timeout" } }`，缓存已有输出 |
| L2 | 中继服务器 | 15 分钟 | 强制断开该请求的等待，向 App 发超时通知 |
| L3 | Android App | 3 分钟（无 process_update 时） | 弹出黄色提示横幅 → 继续等待或取消 |

##### L3 客户端超时特殊逻辑

```
发送消息 → 启动 3 分钟倒计时
  收到 thinking_start → 重置为 5 分钟
  收到 tool_call → 重置为 5 分钟
  收到 text_chunk → 重置为 2 分钟
  收到 timeout_warning → 显示服务端预估 + 延长倒计时
  
倒计时结束且未收到 complete：
  → 显示黄色提示："任务仍在执行中，继续等待？"
  → 按钮：[继续等待 +3min] [取消任务]
```

#### 7.1.6 断线恢复

```
WebSocket 断线
  ↓
自动重连（已有机制）
  ↓
重连成功 → 发送 sync_task_result
  ↓
┌──────────────────────────────────────────────┐
│  {                                            │
│    "type": "sync_task_result",                │
│    "payload": {                               │
│      "requestIds": ["req-a1b2c3"],            │ ← 断线前未完成的请求
│      "lastProcessUpdateId": "pu-xyz"          │ ← 最后收到的 process_update
│    }                                          │
│  }                                            │
└──────────────────────────────────────────────┘
  ↓
中继服务器返回（两种情况）：
  A) 任务已完成 → 返回缓存的 complete 结果 → App 显示最终回复
  B) 任务仍执行 → 返回当前阶段 → App 恢复实时展示
```

##### 中继服务器缓存策略

```javascript
// 任务结果缓存（内存，重启丢失可接受）
const taskResultCache = new Map(); // requestId → { result, timestamp }
const CACHE_TTL = 30 * 60 * 1000; // 30 分钟

// 缓存 complete/error 结果
function cacheTaskResult(requestId, payload) {
  taskResultCache.set(requestId, {
    result: payload,
    timestamp: Date.now()
  });
  // 定期清理过期缓存
  setTimeout(() => taskResultCache.delete(requestId), CACHE_TTL);
}
```

---

### 7.2 R005 自动连接

#### 行为变更

| 原行为 | 新行为 |
|--------|--------|
| 打开 App → 手动选实例 → 手动点连接 | 打开 App → 自动连接中继服务器 → 自动获取实例列表 |
| 设置页：添加/连接/断开按钮 | 设置页：仅显示连接状态（自动管理） |
| 断线后手动重连 | 断线自动重连（已有，保留） |

#### 启动流程

```
App 启动
  ↓
读取本地缓存的中继地址 + Token
  ↓ (首次使用无缓存 → 跳转 R006 认证流程)
自动连接中继 WebSocket
  ↓
连接成功 → 发送 register → 获取实例列表
  ↓
实例列表更新 → 自动选中上次使用的实例
  ↓
状态栏显示 "🟢 已连接"
```

#### 中继地址配置
- 默认内置：`wss://api.lingjiangapp.online:8443`
- 设置页可修改（高级选项，折叠在底部）
- 本地持久化到 SharedPreferences

---

### 7.3 R004 设置页重构

#### 新设置页布局

```
┌─────────────────────────────┐
│ 设置                        │
├─────────────────────────────┤
│ 连接状态                    │
│  🟢 已连接 中继服务器        │
│  在线实例：翎绛、翎翼、翎云  │
├─────────────────────────────┤
│ 通知                        │
│  消息通知       [开关 ●]    │
│  告警通知       [开关 ●]    │
│  静默时段 23:00-08:00 [>]   │
├─────────────────────────────┤
│ 安全                        │
│  传输加密  WSS    [>]       │
│  接入状态  已认证 ✅         │
│  重新认证         [>]       │  ← 清除本地 Token 重走 R006
├─────────────────────────────┤
│ 高级                        │  ← 默认折叠
│  中继地址  wss://api...  [>]│
│  连接超时  10s           [>]│
│  调试日志       [开关 ○]    │
├─────────────────────────────┤
│ 关于                        │
│  版本  v0.13.0              │
│  翎绛指挥中心 🪶            │
└─────────────────────────────┘
```

**移除的 UI 元素**：
- ❌ "添加实例"按钮
- ❌ 每个实例的"连接"/"断开"按钮
- ❌ 手动输入实例名称和 URL 的表单
- ❌ 实例编辑功能（名称/地址）

**保留/新增的 UI 元素**：
- ✅ 连接状态总览（自动管理）
- ✅ 在线实例列表（只读，从中继获取）
- ✅ 实例别名编辑（可选，长按弹出）
- ✅ 接入状态 + 重新认证入口

---

### 7.4 R006 App 接入认证

#### 方案选择：设备绑定 + 接入码（方案 A+C 组合）

首次使用需要接入码，通过后绑定设备 ID，后续自动认证。

#### 认证流程

```
首次打开 App
  ↓
显示欢迎页 + 接入码输入框
  ↓
用户输入接入码
  ↓
App 发送认证请求到中继服务器
┌──────────────────────────────────┐
│  {                                │
│    "type": "auth_request",        │
│    "payload": {                   │
│      "accessCode": "xxxxxx",      │
│      "deviceId": "android-uuid",  │  ← Android Settings.Secure.ANDROID_ID
│      "deviceName": "Pixel 8",     │
│      "appVersion": "0.13.0"       │
│    }                              │
│  }                                │
└──────────────────────────────────┘
  ↓
中继服务器验证
  ↓ (成功)
返回 auth_token（长期有效）
┌──────────────────────────────────┐
│  {                                │
│    "type": "auth_response",       │
│    "payload": {                   │
│      "success": true,             │
│      "authToken": "tok-xxxxxx",   │
│      "expiresAt": null            │  ← null = 永不过期
│    }                              │
│  }                                │
└──────────────────────────────────┘
  ↓
App 本地保存 authToken
  ↓
后续连接 WebSocket 时带 authToken 头
  ws.connect(url, headers: { "X-Auth-Token": authToken })
```

#### 中继服务器接入码管理

```javascript
// 环境变量配置
ACCESS_CODES=["code1", "code2"]  // 有效接入码列表
BOUND_DEVICES={}                  // deviceId → authToken 映射

// 吊销设备
POST /admin/revoke-device { deviceId }
// 更换接入码
POST /admin/rotate-access-code { oldCode, newCode }
```

#### 欢迎页 UI

```
┌─────────────────────────────┐
│                             │
│         🪶                  │
│    翎绛指挥中心              │
│                             │
│    输入接入码以继续           │
│                             │
│  ┌───────────────────────┐  │
│  │  ● ● ● ● ● ●         │  │  ← 6 位接入码输入
│  └───────────────────────┘  │
│                             │
│       [ 🔑 认证 ]           │  ← 绛色按钮
│                             │
│  接入码由管理员提供           │  ← 灰色辅助文字
│                             │
└─────────────────────────────┘
```

---

## 8. M8.6 详细设计：消息可靠

### 8.1 R001 推送与保活

> 技术方案已在 §5.3 给出（前台服务 + FCM + 离线补拉），此处补充实现细节。

#### 前台服务通知样式

```
┌───────────────────────────────────────┐
│ 🪶 翎绛指挥中心 · 已连接 2 个实例     │  ← 常驻通知（低优先级）
│    翎绛 🟢  翎翼 🟢  翎云 ⚫          │
└───────────────────────────────────────┘
```

- 通知渠道：`relay_service`（低重要性，无声音/震动）
- 点击：打开 App 态势页
- 不可滑动清除（前台服务要求）

#### FCM 推送消息格式

```json
{
  "to": "<fcm_token>",
  "priority": "high",
  "data": {
    "type": "chat_message",           // chat_message / alert / task_complete
    "instanceId": "openclaw-vm-main",
    "instanceName": "翎绛",
    "title": "翎绛",
    "body": "任务已完成：代码审查",
    "requestId": "req-xxx"            // 用于点击跳转定位
  }
}
```

#### 离线消息存储（中继服务器端）

```javascript
// 每个客户端独立的消息队列
const offlineQueues = new Map(); // clientId → Message[]
const MAX_OFFLINE_MESSAGES = 100;

function enqueueOfflineMessage(clientId, message) {
  if (!offlineQueues.has(clientId)) {
    offlineQueues.set(clientId, []);
  }
  const queue = offlineQueues.get(clientId);
  queue.push({ ...message, id: generateId(), timestamp: Date.now() });
  if (queue.length > MAX_OFFLINE_MESSAGES) queue.shift(); // FIFO 淘汰
}
```

---

### 8.2 R002 跨实例消息通知

#### 已读/未读状态管理

```kotlin
// Android 端未读计数
data class UnreadState(
    val instanceId: String,
    val count: Int,
    val lastMessage: String?,        // 最新消息摘要
    val lastTimestamp: Long
)

class NotificationViewModel : ViewModel() {
    val unreadMap: StateFlow<Map<String, UnreadState>>  // instanceId → UnreadState
    val totalUnread: StateFlow<Int>                     // 所有实例未读总数

    fun markAsRead(instanceId: String)                  // 进入对话页时调用
    fun onNewMessage(instanceId: String, message: String) // 收到新消息时调用
}
```

#### 通知展示策略

| 场景 | App 状态 | 通知方式 |
|------|----------|----------|
| 当前正在看翎绛对话，翎翼来消息 | 前台，不同实例 | 顶部横幅 Snackbar（3 秒自动消失，点击切换实例） |
| 当前正在看翎绛对话，翎绛来消息 | 前台，同实例 | 直接追加到对话列表，无额外通知 |
| App 在后台 | 后台 | 系统通知栏，点击进入对应实例对话 |
| App 被杀 | 未运行 | FCM 推送 → 系统通知栏 |

#### 底部导航 Badge

```
┌─────────────────────────────────────┐
│ 📡   📋   💬③  🧠   ⚙️            │  ← 对话 Tab 显示未读总数
│ 态势  任务  对话  记忆  设置         │
└─────────────────────────────────────┘
```

Badge 样式：红色圆点，数字白色，≤99 显示数字，>99 显示 "99+"。

---

### 8.3 R003 长时间任务断线恢复

#### 与 R010 的关系

R010 解决"看得到过程"，R003 解决"断线后拿得回结果"。两者共用 `sync_task_result` 协议。

#### 补充机制

在 R010 §7.1.6 的基础上，增加：

1. **服务端结果持久化**：长任务结果写入文件而非仅内存缓存
   ```javascript
   // 超过 5 分钟的任务，结果写入文件
   const PERSIST_THRESHOLD = 5 * 60 * 1000;
   function persistTaskResult(requestId, result) {
     fs.writeFileSync(`/tmp/relay-cache/${requestId}.json`, JSON.stringify(result));
   }
   ```

2. **App 端任务追踪列表**：本地维护"已发送但未收到完整回复"的请求列表
   ```kotlin
   // SharedPreferences 存储
   val pendingRequests: MutableSet<String>  // requestId 集合
   // 发送消息时 add，收到 complete/error 时 remove
   // 重连时用这个列表调 sync_task_result
   ```

---

### 8.4 R008 对话页实例切换 + 会话持久化

#### 实例选择器 UI

```
┌─────────────────────────────┐
│ ← [🪶 翎绛 ▼]    🟢 已连接 │  ← 点击下拉
├─────────────────────────────┤
│ ┌─────────────────────────┐ │
│ │ 🪶 翎绛        🟢 ●    │ │  ← 当前选中（绛色高亮）
│ │ 🦅 翎翼        🟢 ②   │ │  ← ② = 未读数
│ │ ☁️ 翎云         ⚫      │ │  ← 灰色 = 离线
│ └─────────────────────────┘ │
```

#### 会话持久化 — SQLite Schema

```sql
CREATE TABLE conversations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    instance_id TEXT NOT NULL,
    message_id TEXT UNIQUE,          -- 消息唯一 ID
    role TEXT NOT NULL,               -- "user" / "assistant" / "system"
    content TEXT NOT NULL,
    content_type TEXT DEFAULT 'text', -- "text" / "markdown" / "image"
    timestamp INTEGER NOT NULL,
    is_read INTEGER DEFAULT 1,
    INDEX idx_instance_time (instance_id, timestamp DESC)
);

-- 会话元数据
CREATE TABLE conversation_meta (
    instance_id TEXT PRIMARY KEY,
    last_scroll_position INTEGER DEFAULT 0,  -- 保留滚动位置
    last_active_time INTEGER,
    draft_text TEXT DEFAULT ''                -- 未发送的草稿
);
```

#### 数据流

```
用户切换实例
  ↓
保存当前实例的 scroll_position + draft_text
  ↓
从 SQLite 加载目标实例的消息列表
  ↓
恢复 scroll_position
  ↓
更新 WebSocket 订阅（告诉中继当前关注的实例）
```

---

## 9. M8.7 详细设计：安全运维

### 9.1 R009 实例 API 状态监控

#### health_report 协议扩展

在现有 `health_report` 消息（§4.1）中新增 `apis` 字段：

```json
{
  "type": "health_report",
  "payload": {
    "instanceId": "openclaw-vm-main",
    "cpu": 12.5,
    "memory": { "used": 4.2, "total": 8.0 },
    "disk": { "used": 78, "total": 87, "percent": 90 },
    "uptime": 86400,
    "activeSubAgents": 3,
    "apis": [
      {
        "provider": "yunyi",
        "status": "active",          // active / expiring / expired / unknown
        "expiresAt": 1742000000,     // Unix 时间戳，null = 未知
        "balance": 45.20,            // 余额（元），null = 无额度概念
        "quota": { "used": 1200, "total": 5000 },  // 请求配额，null = 无限
        "lastChecked": 1741599000
      },
      {
        "provider": "bailian",
        "status": "expiring",
        "expiresAt": 1741800000,
        "balance": null,
        "quota": { "used": 4800, "total": 5000 },
        "lastChecked": 1741599000
      }
    ],
    "timestamp": 1741600000
  }
}
```

#### 告警规则

| 条件 | 告警级别 | 触发 |
|------|----------|------|
| 到期时间 ≤ 3 天 | warning | `alert { level: "warning", title: "yunyi API 即将到期" }` |
| 已过期 | critical | `alert { level: "critical", title: "yunyi API 已过期" }` |
| 余额 ≤ ¥10 | warning | `alert { level: "warning", title: "yunyi 余额不足 ¥10" }` |
| 配额使用 ≥ 90% | warning | 同上 |

#### 实例卡片 API 状态指示

```
┌────────────────┐
│ 🪶 翎绛   🟢   │
│ CPU 12%        │
│ MEM 4.2G       │
│ ████░ 90%      │
│ API: 🟢 2  ⚠️ 1│  ← 新增：API 状态汇总
└────────────────┘
```

---

### 9.2 R007 翎云自动运维

#### 运维 Agent 架构

```
翎云（中继服务器 + OpenClaw 实例）
  ↓ 检测到实例离线
  ↓ 等待 2 分钟（排除短暂重启）
  ↓ 仍离线
  ↓
触发自动运维流程
  ↓
SSH 连接到故障机器（Tailscale 网络）
  ↓
诊断序列：
  1. ping 检测 → 机器是否可达
  2. systemctl status openclaw → 服务状态
  3. journalctl -u openclaw --since "5min ago" → 最近日志
  4. df -h / free -h → 资源状况
  ↓
判断问题类型 → 执行修复
  ↓
修复结果 → 通知用户（推送 + 时间线）
```

#### 权限白名单

| 允许的操作 | 命令 | 说明 |
|-----------|------|------|
| 重启 OpenClaw 服务 | `systemctl restart openclaw` | 最常用修复手段 |
| 清理临时文件 | `rm -rf /tmp/openclaw-*` | 磁盘满时 |
| 清理日志 | `journalctl --vacuum-size=100M` | 磁盘满时 |
| 查看系统状态 | `systemctl status`, `df -h`, `free -h`, `top -bn1` | 只读诊断 |
| 查看网络 | `ip addr`, `ss -tlnp`, `tailscale status` | 只读诊断 |

| 禁止的操作 | 说明 |
|-----------|------|
| `rm -rf /` 或任何非 /tmp 目录删除 | 防止误删 |
| 修改配置文件 | 需要人工确认 |
| 安装/卸载软件包 | 需要人工确认 |
| 修改用户/权限 | 安全敏感 |
| reboot / shutdown | 需要人工确认 |

#### 通知消息

修复成功：
```
┌───────────────────────────────────────┐
│ 🔧 翎云自动运维                        │
│                                       │
│ 检测到翎绛离线（17:35），已自动修复：    │
│ • 原因：OpenClaw 服务异常退出          │
│ • 操作：已重启服务                     │
│ • 状态：翎绛已恢复在线 ✅              │
│                                       │
│ 耗时：45 秒                           │
└───────────────────────────────────────┘
```

修复失败：
```
┌───────────────────────────────────────┐
│ ⚠️ 翎云运维告警                        │
│                                       │
│ 检测到翎绛离线（17:35），自动修复失败：  │
│ • 原因：SSH 连接超时，机器不可达        │
│ • 建议：检查 VM 是否已关机              │
│                                       │
│ 诊断日志已保存                         │
│                    [查看详情]           │
└───────────────────────────────────────┘
```

---

## 10. 原型文件

高保真 HTML 原型：`ui-prototype/index.html`
用手机浏览器打开即可预览态势页效果。

---

**创建时间**: 2026-03-10 15:55
**最后更新**: 2026-03-11（整合需求池 R001-R010，新增 M8.5/M8.6/M8.7 里程碑设计）
**设计者**: 翎绛 🪶
**版本**: v3.0
