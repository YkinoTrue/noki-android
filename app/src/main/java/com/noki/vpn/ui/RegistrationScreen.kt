package com.noki.vpn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.noki.vpn.AuthStep
import com.noki.vpn.AppUiState
import com.noki.vpn.MainViewModel
import com.noki.vpn.R
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
@Composable
fun RegistrationScreen(
    state: AppUiState,
    viewModel: MainViewModel,
    liveGlassEnabled: Boolean = true,
) {
    val language = state.personalizationSettings.language

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(NokiBgBase),
    ) {
        val authMetrics = nokiAdaptiveMetrics(maxWidth)
        val form = state.registrationForm
        val registrationContentHeight = when (state.authStep) {
            AuthStep.REGISTRATION_PROFILE -> 380.dp
            AuthStep.REGISTRATION_PASSWORD -> 424.dp
            AuthStep.REGISTRATION_EMAIL -> 385.dp
            else -> 358.dp
        }
        val registrationContentTop = ((maxHeight - registrationContentHeight) / 2).let { centeredTop ->
            if (centeredTop < 32.dp) 32.dp else centeredTop
        }
        val registrationLogoTop = registrationContentTop
        val registrationFieldTop = registrationLogoTop + 78.dp + if (state.authStep == AuthStep.REGISTRATION_PASSWORD) 68.dp else 90.dp
        val registrationFieldLabelTop = registrationFieldTop - 27.dp
        val registrationRequirementHeight = 18.dp
        val registrationFieldBlockHeight = when (state.authStep) {
            AuthStep.REGISTRATION_PROFILE -> 48.dp + 8.dp + registrationRequirementHeight
            AuthStep.REGISTRATION_PASSWORD -> 48.dp + 17.dp + 48.dp + 8.dp + registrationRequirementHeight
            else -> 48.dp
        }
        val registrationErrorReserveHeight = if (form.error == null) 0.dp else 36.dp
        val registrationPrimaryTop = registrationFieldTop + registrationFieldBlockHeight + 30.dp + registrationErrorReserveHeight
        val registrationBackTop = registrationPrimaryTop + 70.dp
        val registrationKeyboardShift = authKeyboardShift(
            maxHeight = maxHeight,
            imeBottom = authImeBottom(),
            contentTop = registrationLogoTop,
            contentBottom = registrationBackTop + 42.dp,
        )
        val authBackdrop = rememberLayerBackdrop()

        AuthBackground(
            backgroundRes = R.drawable.registration_background,
            modifier = Modifier.then(
                if (liveGlassEnabled) Modifier.layerBackdrop(authBackdrop) else Modifier,
            ),
        )
        AuthLogo(top = registrationLogoTop - registrationKeyboardShift)

        when (state.authStep) {
            AuthStep.REGISTRATION_CODE -> {
                AuthVerificationCodeField(
                    value = form.verificationCode,
                    onValueChange = viewModel::updateRegistrationVerificationCode,
                    placeholder = tr(language, "Код подтверждения", "Verification code"),
                    buttonText = when {
                        form.codeCooldownSeconds > 0 -> tr(
                            language,
                            "Повторить (${form.codeCooldownSeconds}с)",
                            "Resend (${form.codeCooldownSeconds}s)",
                        )
                        form.codeSent -> tr(language, "Отправить повторно", "Resend")
                        else -> tr(language, "Отправить код", "Send code")
                    },
                    showButton = true,
                    buttonEnabled = form.codeCooldownSeconds == 0,
                    buttonLoading = form.isCodeSending,
                    modifier = Modifier
                        .offset(x = authMetrics.contentStart, y = registrationFieldTop - registrationKeyboardShift)
                        .width(authMetrics.contentWidth)
                        .height(48.dp),
                    backdrop = authBackdrop,
                    liveGlassEnabled = liveGlassEnabled,
                    onButtonClick = viewModel::requestRegistrationCode,
                )
            }
            AuthStep.REGISTRATION_PROFILE -> {
                AuthFieldLabel(
                    text = tr(language, "Выберите имя пользователя", "Choose a username"),
                    modifier = Modifier
                        .offset(x = authMetrics.contentStart, y = registrationFieldLabelTop - registrationKeyboardShift)
                        .width(authMetrics.contentWidth),
                )

                AuthInputField(
                    value = form.username,
                    onValueChange = viewModel::updateRegistrationUsername,
                    placeholder = "username",
                    keyboardType = KeyboardType.Text,
                    modifier = Modifier
                        .offset(x = authMetrics.contentStart, y = registrationFieldTop - registrationKeyboardShift)
                        .width(authMetrics.contentWidth)
                        .height(48.dp),
                )

                AuthRequirementText(
                    text = tr(language, "Не менее 3 символов, только латиница", "At least 3 characters, Latin letters only"),
                    modifier = Modifier
                        .offset(x = authMetrics.contentStart, y = registrationFieldTop + 56.dp - registrationKeyboardShift)
                        .width(authMetrics.contentWidth)
                        .height(registrationRequirementHeight),
                )
            }
            AuthStep.REGISTRATION_PASSWORD -> {
                AuthFieldLabel(
                    text = tr(language, "Создайте пароль", "Create a password"),
                    modifier = Modifier
                        .offset(x = authMetrics.contentStart, y = registrationFieldLabelTop - registrationKeyboardShift)
                        .width(authMetrics.contentWidth),
                )

                AuthInputField(
                    value = form.password,
                    onValueChange = viewModel::updateRegistrationPassword,
                    placeholder = tr(language, "Пароль", "Password"),
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                    enabled = !form.isLoading,
                    modifier = Modifier
                        .offset(x = authMetrics.contentStart, y = registrationFieldTop - registrationKeyboardShift)
                        .width(authMetrics.contentWidth)
                        .height(48.dp),
                )

                AuthInputField(
                    value = form.passwordRepeat,
                    onValueChange = viewModel::updateRegistrationPasswordRepeat,
                    placeholder = tr(language, "Повторите пароль", "Repeat password"),
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                    enabled = !form.isLoading,
                    modifier = Modifier
                        .offset(x = authMetrics.contentStart, y = registrationFieldTop + 65.dp - registrationKeyboardShift)
                        .width(authMetrics.contentWidth)
                        .height(48.dp),
                )

                AuthRequirementText(
                    text = tr(language, "Не менее 8 символов", "At least 8 characters"),
                    modifier = Modifier
                        .offset(x = authMetrics.contentStart, y = registrationFieldTop + 121.dp - registrationKeyboardShift)
                        .width(authMetrics.contentWidth)
                        .height(registrationRequirementHeight),
                )
            }
            AuthStep.WELCOME,
            AuthStep.EMAIL_LOGIN,
            AuthStep.REGISTRATION_EMAIL -> {
                AuthFieldLabel(
                    text = tr(language, "Введите ваш e-mail", "Enter your email"),
                    modifier = Modifier
                        .offset(x = authMetrics.contentStart, y = registrationFieldLabelTop - registrationKeyboardShift)
                        .width(authMetrics.contentWidth),
                )

                AuthInputField(
                    value = form.email,
                    onValueChange = viewModel::updateRegistrationEmail,
                    placeholder = "E-mail",
                    keyboardType = KeyboardType.Email,
                    modifier = Modifier
                        .offset(x = authMetrics.contentStart, y = registrationFieldTop - registrationKeyboardShift)
                        .width(authMetrics.contentWidth)
                        .height(48.dp),
                )
            }
        }

        AuthInlineError(
            error = form.error,
            modifier = Modifier
                .offset(
                    x = authMetrics.contentStart,
                    y = registrationFieldTop + registrationFieldBlockHeight + 8.dp - registrationKeyboardShift,
                )
                .width(authMetrics.contentWidth),
        )

        AuthPrimaryButton(
            text = when (state.authStep) {
                AuthStep.REGISTRATION_EMAIL -> tr(language, "Получить код", "Get code")
                AuthStep.REGISTRATION_PASSWORD -> tr(language, "Создать аккаунт", "Create account")
                else -> tr(language, "Продолжить", "Continue")
            },
            loading = form.isLoading || form.isCodeSending,
            modifier = Modifier
                .offset(x = authMetrics.contentStart, y = registrationPrimaryTop - registrationKeyboardShift)
                .width(authMetrics.contentWidth)
                .height(56.dp),
            backdrop = authBackdrop,
            liveGlassEnabled = liveGlassEnabled,
            onClick = if (state.authStep == AuthStep.REGISTRATION_EMAIL) {
                viewModel::requestRegistrationCode
            } else {
                viewModel::nextRegistrationStep
            },
        )

        AuthSecondaryButton(
            text = tr(language, "Назад", "Back"),
            containerColor = NokiBgLighter,
            contentColor = NokiTextPrimary,
            cornerRadius = 16.dp,
            modifier = Modifier
                .offset(x = authMetrics.contentStart, y = registrationBackTop - registrationKeyboardShift)
                .width(authMetrics.contentWidth)
                .height(42.dp),
            backdrop = authBackdrop,
            liveGlassEnabled = liveGlassEnabled,
            onClick = viewModel::previousRegistrationStep,
        )

    }
}
