package com.bnyro.wallpaper.ui.components.prefs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.bnyro.wallpaper.R
import com.bnyro.wallpaper.util.WallpaperApiWrapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.youapps.wallpaper_apis.LoginCredentials
import net.youapps.wallpaper_apis.LoginException
import net.youapps.wallpaper_apis.LoginSession

/**
 * Preference for logging in to a wallpaper API, either with a personal API key (see [WallpaperApiWrapper.supportsApiKey])
 * or with a username and password (see [WallpaperApiWrapper.supportsPasswordLogin]).
 */
@Composable
fun AccountPref(
    api: WallpaperApiWrapper
) {
    val scope = rememberCoroutineScope()

    var apiKey by remember { mutableStateOf(api.apiKey) }
    var loginSession by remember { mutableStateOf(api.loginSession) }
    var showLoginDialog by remember { mutableStateOf(false) }

    val isLoggedIn = apiKey != null || loginSession != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = api.icon, contentDescription = null)
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = api.name, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(2.dp))

            val currentKey = apiKey
            val currentSession = loginSession
            val loggedInText = when {
                currentSession != null -> stringResource(R.string.logged_in_as, currentSession.accountName)
                currentKey != null -> "${stringResource(R.string.logged_in)} · ••••${currentKey.takeLast(4)}"
                else -> null
            }

            if (loggedInText != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = loggedInText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                api.loginDescription?.let { description ->
                    Text(
                        text = stringResource(description),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isLoggedIn) {
            OutlinedButton(
                onClick = {
                    loginSession?.let { session ->
                        // invalidate the session on the server, the local logout happens regardless
                        scope.launch(Dispatchers.IO) { api.logOut(session) }
                    }

                    api.apiKey = null
                    api.loginSession = null
                    apiKey = null
                    loginSession = null
                }
            ) {
                Text(stringResource(R.string.log_out))
            }
        } else {
            Button(
                onClick = { showLoginDialog = true }
            ) {
                Text(stringResource(R.string.log_in))
            }
        }
    }

    if (showLoginDialog) {
        if (api.supportsPasswordLogin) {
            PasswordLoginDialog(
                api = api,
                onLoggedIn = { session ->
                    api.loginSession = session
                    loginSession = api.loginSession
                    showLoginDialog = false
                },
                onDismissRequest = { showLoginDialog = false }
            )
        } else {
            ApiKeyLoginDialog(
                api = api,
                onLoggedIn = { newApiKey ->
                    api.apiKey = newApiKey
                    apiKey = api.apiKey
                    showLoginDialog = false
                },
                onDismissRequest = { showLoginDialog = false }
            )
        }
    }
}

@Composable
private fun ApiKeyLoginDialog(
    api: WallpaperApiWrapper,
    onLoggedIn: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    var input by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val invalidApiKeyMessage = stringResource(R.string.invalid_api_key)

    fun login() {
        val newApiKey = input.trim()
        if (newApiKey.isEmpty() || isLoading) return

        isLoading = true
        error = null
        scope.launch {
            try {
                val isValid = withContext(Dispatchers.IO) {
                    api.validateApiKey(newApiKey)
                }

                if (isValid) onLoggedIn(newApiKey) else error = invalidApiKeyMessage
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.localizedMessage ?: e.toString()
            } finally {
                isLoading = false
            }
        }
    }

    LoginDialog(
        api = api,
        canLogIn = input.isNotBlank(),
        isLoading = isLoading,
        onLogIn = { login() },
        onDismissRequest = onDismissRequest
    ) {
        val currentError = error

        SecretTextField(
            value = input,
            onValueChange = {
                input = it
                error = null
            },
            label = stringResource(R.string.api_key),
            enabled = !isLoading,
            error = currentError,
            imeAction = ImeAction.Done,
            onImeAction = { login() }
        )

        api.apiKeyUrl?.let { url ->
            TextButton(
                onClick = { uriHandler.openUri(url) }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.get_api_key))
            }
        }
    }
}

@Composable
private fun PasswordLoginDialog(
    api: WallpaperApiWrapper,
    onLoggedIn: (LoginSession) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var instance by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var twoFactorToken by remember { mutableStateOf("") }
    var showTwoFactorField by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var instanceError by remember { mutableStateOf<String?>(null) }

    val canLogIn = username.isNotBlank() && password.isNotEmpty() &&
            (!api.requiresInstanceForLogin || instance.isNotBlank()) &&
            (!showTwoFactorField || twoFactorToken.isNotBlank())

    fun login() {
        if (!canLogIn || isLoading) return

        isLoading = true
        error = null
        instanceError = null
        scope.launch {
            try {
                val session = withContext(Dispatchers.IO) {
                    api.logIn(
                        LoginCredentials(
                            username = username.trim(),
                            password = password,
                            instanceUrl = instance.trim().takeIf { api.requiresInstanceForLogin },
                            twoFactorToken = twoFactorToken.trim().takeIf { showTwoFactorField }
                        )
                    )
                }

                onLoggedIn(session)
            } catch (e: CancellationException) {
                throw e
            } catch (e: LoginException) {
                when (e.reason) {
                    LoginException.Reason.TWO_FACTOR_REQUIRED -> {
                        // not an error, the user just has to enter their two factor code now
                        showTwoFactorField = true
                    }

                    LoginException.Reason.INVALID_INSTANCE -> {
                        instanceError = context.getString(R.string.invalid_instance)
                    }

                    else -> error = when (e.reason) {
                        LoginException.Reason.INVALID_CREDENTIALS -> context.getString(R.string.invalid_login)
                        LoginException.Reason.INVALID_TWO_FACTOR_TOKEN -> context.getString(R.string.invalid_two_factor_code)
                        LoginException.Reason.EMAIL_NOT_VERIFIED -> context.getString(R.string.email_not_verified)
                        LoginException.Reason.ACCOUNT_NOT_APPROVED -> context.getString(R.string.account_not_approved)
                        else -> e.localizedMessage ?: e.toString()
                    }
                }
            } catch (e: Exception) {
                error = e.localizedMessage ?: e.toString()
            } finally {
                isLoading = false
            }
        }
    }

    LoginDialog(
        api = api,
        canLogIn = canLogIn,
        isLoading = isLoading,
        onLogIn = { login() },
        onDismissRequest = onDismissRequest
    ) {
        if (api.requiresInstanceForLogin) {
            val currentInstanceError = instanceError

            OutlinedTextField(
                value = instance,
                onValueChange = {
                    instance = it
                    instanceError = null
                },
                label = { Text(stringResource(R.string.instance)) },
                placeholder = { Text(stringResource(R.string.instance_hint)) },
                singleLine = true,
                enabled = !isLoading,
                isError = currentInstanceError != null,
                supportingText = if (currentInstanceError != null) {
                    { Text(currentInstanceError) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                )
            )
        }

        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                error = null
            },
            label = { Text(stringResource(R.string.username_or_email)) },
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )

        SecretTextField(
            value = password,
            onValueChange = {
                password = it
                error = null
            },
            label = stringResource(R.string.password),
            enabled = !isLoading,
            error = error.takeIf { !showTwoFactorField },
            imeAction = if (showTwoFactorField) ImeAction.Next else ImeAction.Done,
            onImeAction = { login() }
        )

        if (showTwoFactorField) {
            val currentError = error

            OutlinedTextField(
                value = twoFactorToken,
                onValueChange = {
                    twoFactorToken = it.filter { char -> char.isDigit() }
                    error = null
                },
                label = { Text(stringResource(R.string.two_factor_code)) },
                singleLine = true,
                enabled = !isLoading,
                isError = currentError != null,
                supportingText = {
                    Text(currentError ?: stringResource(R.string.two_factor_code_desc))
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { login() })
            )
        }
    }
}

/**
 * Dialog with the common layout of all login methods.
 */
@Composable
private fun LoginDialog(
    api: WallpaperApiWrapper,
    canLogIn: Boolean,
    isLoading: Boolean,
    onLogIn: () -> Unit,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(stringResource(R.string.log_in_to, api.name))
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                api.loginDescription?.let { description ->
                    Text(
                        text = stringResource(description),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                content()
            }
        },
        confirmButton = {
            TextButton(
                onClick = onLogIn,
                enabled = canLogIn && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.log_in))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

/**
 * Text field for secrets like passwords or API keys, which are hidden unless the user decides to show them.
 */
@Composable
private fun SecretTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    error: String?,
    imeAction: ImeAction,
    onImeAction: () -> Unit
) {
    var showValue by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        isError = error != null,
        supportingText = if (error != null) {
            { Text(error) }
        } else {
            null
        },
        visualTransformation = if (showValue) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(onDone = { onImeAction() }),
        trailingIcon = {
            IconButton(onClick = { showValue = !showValue }) {
                Icon(
                    imageVector = if (showValue) {
                        Icons.Default.VisibilityOff
                    } else {
                        Icons.Default.Visibility
                    },
                    contentDescription = stringResource(
                        if (showValue) R.string.hide_secret else R.string.show_secret
                    )
                )
            }
        }
    )
}
