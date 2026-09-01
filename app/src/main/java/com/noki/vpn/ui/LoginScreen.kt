package com.noki.vpn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.noki.vpn.AppDestination
import com.noki.vpn.AuthStep
import com.noki.vpn.AppUiState
import com.noki.vpn.MainViewModel
import com.noki.vpn.R
import com.noki.vpn.TelegramLoginStateReducer
import com.noki.vpn.data.VpnConnectionState
import com.noki.vpn.vpn.VpnRuntimeMode
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
@Composable
fun LoginScreen(
    state: AppUiState,
    viewModel: MainViewModel,
    liveGlassEnabled: Boolean = true,
    onGoogleClick: () -> Unit,
    onTelegramClick: () -> Unit,
    onTemporaryVpnClick: () -> Unit,
    onSupportClick: () -> Unit,
) {
    val language = state.personalizationSettings.language

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(NokiBgBase),
    ) {
        val authMetrics = nokiAdaptiveMetrics(maxWidth)
        val loginButtonWidth = authMetrics.contentWidth
        val showEmailLogin = state.authStep == AuthStep.EMAIL_LOGIN
        val loginContentHeight = if (showEmailLogin) 459.dp else 578.dp
        val loginContentTop = ((maxHeight - loginContentHeight) / 2).let { centeredTop ->
            if (centeredTop < 32.dp) 32.dp else centeredTop
        }
        val loginYOffset = loginContentTop - 200.dp
        val loginKeyboardShift = authKeyboardShift(
            maxHeight = maxHeight,
            imeBottom = authImeBottom(),
            contentTop = 200.dp + loginYOffset,
            contentBottom = 659.dp + loginYOffset,
        )
        val forgotPasswordInteractionSource = remember { MutableInteractionSource() }
        val inviteCodeInteractionSource = remember { MutableInteractionSource() }
        val authBackdrop = rememberLayerBackdrop()

        AuthBackground(
            backgroundRes = R.drawable.login_background,
            modifier = Modifier.then(
                if (liveGlassEnabled) Modifier.layerBackdrop(authBackdrop) else Modifier,
            ),
        )
        if (showEmailLogin) {
            AuthLogo(top = 200.dp + loginYOffset - loginKeyboardShift)
        }

        if (!showEmailLogin) {
            val temporaryVpnConnected = state.vpnRuntimeMode == VpnRuntimeMode.AUTH_TEMP &&
                state.connectionState == VpnConnectionState.CONNECTED
            val temporaryVpnConnecting = state.vpnRuntimeMode == VpnRuntimeMode.AUTH_TEMP &&
                state.connectionState == VpnConnectionState.CONNECTING
            WelcomeLoginScreen(
                metrics = authMetrics,
                inlineMessage = welcomeInlineMessageForDisplay(
                    inlineMessage = state.inlineMessage,
                    vpnRuntimeMode = state.vpnRuntimeMode,
                ),
                telegramErrorMessage = TelegramLoginStateReducer.errorMessage(
                    state.telegramLoginState,
                    com.noki.vpn.TelegramAuthPurpose.LOGIN,
                ),
                language = language,
                onLoginClick = viewModel::openEmailLogin,
                onRegistrationClick = viewModel::openRegistrationFlow,
                onTelegramClick = onTelegramClick,
                onTelegramErrorDismiss = viewModel::cancelTelegramLoginFlow,
                telegramLoginInProgress = TelegramLoginStateReducer.isActive(
                    state.telegramLoginState,
                ),
                googleLoginInProgress = state.loginForm.isLoading,
                onGoogleClick = onGoogleClick,
                onInviteCodeClick = { viewModel.openScreen(AppDestination.INVITE_DEVICE) },
                temporaryVpnConnected = temporaryVpnConnected,
                temporaryVpnConnecting = temporaryVpnConnecting,
                temporaryVpnStatusMessage = state.inlineMessage.takeIf {
                    state.vpnRuntimeMode == VpnRuntimeMode.AUTH_TEMP &&
                        state.connectionState == VpnConnectionState.FAILED
                },
                onTemporaryVpnClick = onTemporaryVpnClick,
                onSupportClick = onSupportClick,
                backdrop = authBackdrop,
                liveGlassEnabled = liveGlassEnabled,
            )
            return@BoxWithConstraints
        }

        AuthInputField(
            value = state.loginForm.email,
            onValueChange = viewModel::updateLoginEmail,
            placeholder = tr(language, "E-mail / Логин", "E-mail / Login"),
            keyboardType = KeyboardType.Text,
            modifier = Modifier
                .offset(x = authMetrics.contentStart, y = 338.dp + loginYOffset - loginKeyboardShift)
                .width(authMetrics.contentWidth)
                .height(48.dp),
        )

        AuthInputField(
            value = state.loginForm.password,
            onValueChange = viewModel::updateLoginPassword,
            placeholder = tr(language, "Пароль", "Password"),
            keyboardType = KeyboardType.Password,
            isPassword = true,
            modifier = Modifier
                .offset(x = authMetrics.contentStart, y = 403.dp + loginYOffset - loginKeyboardShift)
                .width(authMetrics.contentWidth)
                .height(48.dp),
        )

        AuthInlineError(
            error = state.loginForm.error,
            modifier = Modifier
                .offset(x = authMetrics.contentStart, y = 309.dp + loginYOffset - loginKeyboardShift)
                .width(authMetrics.contentWidth),
        )

        Box(
            modifier = Modifier
                .offset(x = authMetrics.contentStart, y = 458.dp + loginYOffset - loginKeyboardShift)
                .width(loginButtonWidth),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                text = tr(language, "Восстановить пароль", "Reset password"),
                modifier = Modifier.clickable(
                    interactionSource = forgotPasswordInteractionSource,
                    indication = null,
                    onClick = viewModel::openPasswordRecovery,
                ),
                color = NokiTextSecondary,
                fontSize = authSp(11f),
                lineHeight = authSp(12f),
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Normal,
                letterSpacing = authSp(0.11f),
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Right,
            )
        }

        AuthPrimaryButton(
            text = tr(language, "Войти", "Sign in"),
            loading = state.loginForm.isLoading,
            containerColor = NokiAccentPrimary,
            contentColor = NokiBgBase,
            cornerRadius = 20.dp,
            modifier = Modifier
                .offset(x = authMetrics.contentStart, y = 497.dp + loginYOffset - loginKeyboardShift)
                .width(loginButtonWidth)
                .height(56.dp),
            backdrop = authBackdrop,
            liveGlassEnabled = liveGlassEnabled,
            onClick = viewModel::submitLogin,
        )

        AuthSecondaryButton(
            text = tr(language, "Назад", "Back"),
            containerColor = NokiBgLighter,
            contentColor = NokiTextPrimary,
            cornerRadius = 16.dp,
            modifier = Modifier
                .offset(x = authMetrics.contentStart, y = 567.dp + loginYOffset - loginKeyboardShift)
                .width(loginButtonWidth)
                .height(42.dp),
            backdrop = authBackdrop,
            liveGlassEnabled = liveGlassEnabled,
            onClick = viewModel::goBack,
        )

        Box(
            modifier = Modifier
                .offset(x = 0.dp, y = 623.dp + loginYOffset - loginKeyboardShift)
                .fillMaxWidth()
                .height(36.dp)
                .clickable(
                    interactionSource = inviteCodeInteractionSource,
                    indication = null,
                    onClick = { viewModel.openScreen(AppDestination.INVITE_DEVICE) },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tr(language, "Войти по коду", "Sign in by code"),
                color = NokiTextSecondary,
                fontSize = authSp(14f),
                lineHeight = authSp(14f),
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Normal,
                letterSpacing = authSp(0.14f),
                textDecoration = TextDecoration.Underline,
            )
        }

    }
}
