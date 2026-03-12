package com.linjiang.command.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linjiang.command.ui.theme.*

private const val ACCESS_CODE = "linjiang2026"

/**
 * 欢迎页 — 首次使用时要求输入接入码
 *
 * 绛红暖光科幻风格，暖黑背景 + 暖金边框输入框 + 绛红按钮。
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WelcomeScreen(
    onAuthenticated: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    // 标题呼吸动画
    val infiniteTransition = rememberInfiniteTransition(label = "welcomeGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "titleGlow"
    )

    fun submit() {
        if (code.isBlank()) {
            error = true
            errorMessage = "请输入接入码"
            return
        }
        if (code.trim() == ACCESS_CODE) {
            error = false
            onAuthenticated(code.trim())
        } else {
            error = true
            errorMessage = "接入码错误"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep),
        contentAlignment = Alignment.Center
    ) {
        // 顶部微光装饰线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, WarmGlow.copy(alpha = 0.15f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Logo emoji
            Text(
                text = "🪶",
                fontSize = 56.sp
            )

            // 标题
            Text(
                text = "翎绛指挥中心",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Accent.copy(alpha = glowAlpha),
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp
            )

            // 副标题
            Text(
                text = "数字精怪，羽翼染绛",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 接入码输入框
            OutlinedTextField(
                value = code,
                onValueChange = {
                    code = it
                    if (error) error = false
                },
                label = {
                    Text("接入码", color = TextSecondary, fontSize = 13.sp)
                },
                placeholder = {
                    Text("请输入接入码", color = TextDim, fontSize = 14.sp)
                },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                isError = error,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                    submit()
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Accent,
                    focusedBorderColor = WarmGlow.copy(alpha = 0.5f),
                    unfocusedBorderColor = WarmGlowBorder,
                    errorBorderColor = StatusRed,
                    focusedLabelColor = WarmGlow,
                    unfocusedLabelColor = TextSecondary,
                    errorLabelColor = StatusRed,
                    focusedContainerColor = BgCard.copy(alpha = 0.6f),
                    unfocusedContainerColor = BgCard.copy(alpha = 0.4f),
                    errorContainerColor = BgCard.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // 错误提示
            if (error) {
                Text(
                    text = errorMessage,
                    fontSize = 13.sp,
                    color = StatusRed,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 进入按钮
            Button(
                onClick = { submit() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = TextPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Text(
                    text = "进入",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )
            }
        }

        // 底部微光装饰线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Accent.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
        )
    }
}
