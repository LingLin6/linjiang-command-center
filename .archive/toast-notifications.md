# Toast 提示功能说明

## 概述

v0.3.1 版本新增了 Toast 提示功能，用户在使用 App 时可以实时看到连接状态和错误信息。

## 提示类型

### 连接状态
1. **正在连接到中继服务器...** - 点击"连接"按钮时显示
2. **连接成功** - WebSocket 连接建立成功
3. **连接失败：[错误信息]** - 连接失败，显示具体错误原因
4. **连接已断开** - WebSocket 连接关闭

### 注册状态
5. **正在注册实例...** - 连接成功后开始注册
6. **注册成功** - 实例注册完成，可以开始通信

## 技术实现

### 架构
```
ViewModel (StateFlow) → UI Layer (LaunchedEffect) → Toast.makeText()
```

### 数据流
1. 网络层（WebSocketManager/RelayClient）触发事件
2. 通过回调函数传递消息到 ViewModel
3. ViewModel 更新 `toastMessage` StateFlow
4. UI 层监听 StateFlow 变化
5. 显示 Toast 并清除消息

### 关键代码

**ViewModel**:
```kotlin
private val _toastMessage = MutableStateFlow<String?>(null)
val toastMessage: StateFlow<String?> = _toastMessage

private fun showToast(message: String) {
    _toastMessage.value = message
}

fun clearToast() {
    _toastMessage.value = null
}
```

**UI Layer**:
```kotlin
val toastMessage by viewModel.toastMessage.collectAsState()

LaunchedEffect(toastMessage) {
    toastMessage?.let {
        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        viewModel.clearToast()
    }
}
```

## 用户体验

- **即时反馈**：操作后立即显示状态
- **错误透明**：失败时显示具体原因
- **非侵入式**：Toast 自动消失，不阻塞操作
- **状态清晰**：每个阶段都有明确提示

## 未来改进

- [ ] 添加成功/失败的音效
- [ ] 支持自定义 Toast 样式（颜色、图标）
- [ ] 添加可点击的 Toast（跳转到详情页）
- [ ] 支持 Snackbar（可撤销操作）

---

**创建时间**: 2026-03-09 01:00  
**版本**: v0.3.1
