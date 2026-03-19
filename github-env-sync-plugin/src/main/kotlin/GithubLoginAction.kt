import github.AccessTokenResponse
import github.DeviceCodeResponse
import github.GithubApi
import gradle.GradleUserProperties
import util.GithubApiException
import java.awt.Desktop
import java.awt.GraphicsEnvironment
import java.net.URI
import kotlin.math.min

private const val SECOND_IN_MILLIS = 1000

private const val INTERVAL_INCREASE_STEP = 5

private const val MAX_REQUEST_INTERVAL = 30L

internal class GithubLoginAction(
    private val clientId: String,
    private val tokenPropertyName: String,
    private val usernamePropertyName: String,
    private val autoOpenBrowser: Boolean,
    private val logger: (String) -> Unit,
) {

    fun runLogin(): String {
        val api = GithubApi(clientId = clientId)
        val deviceCode = api.requestDeviceCode()

        logger("GitHub authorization required.")
        logger("Open: ${deviceCode.verificationUri}")
        logger("Code: ${deviceCode.userCode}")

        tryOpenBrowser(deviceCode.verificationUri)

        return pollForToken(api, deviceCode)
    }

    private fun tryOpenBrowser(url: String) {
        val isSupported = !GraphicsEnvironment.isHeadless() && Desktop.isDesktopSupported()
        if (autoOpenBrowser && isSupported) {
            runCatching {
                val desktop = Desktop.getDesktop()
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(URI(url))
                    return
                }
            }.onFailure {
                logger("Could not open browser automatically. Open the URL manually: $url")
                return
            }
        }
        logger("Open the URL manually: $url")
    }

    private fun pollForToken(
        api: GithubApi,
        deviceCode: DeviceCodeResponse,
    ): String {
        val deadlineMillis = System.currentTimeMillis() + deviceCode.expiresIn * SECOND_IN_MILLIS
        val state = PollingState(intervalSeconds = deviceCode.interval.coerceAtLeast(1))

        while (System.currentTimeMillis() < deadlineMillis) {
            Thread.sleep(state.intervalSeconds * SECOND_IN_MILLIS)

            val tokenResponse = tryFetchToken(api, deviceCode.deviceCode, state) ?: continue

            if (tokenResponse.error == null) {
                return handleSuccessfulLogin(api, tokenResponse)
            }

            handleErrorResponse(tokenResponse, state)
        }
        error("Timed out while waiting for GitHub authorization")
    }

    private class PollingState(
        var intervalSeconds: Long,
        var transientFailures: Int = 0,
    ) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PollingState

            if (intervalSeconds != other.intervalSeconds) return false
            if (transientFailures != other.transientFailures) return false

            return true
        }

        override fun hashCode(): Int {
            var result = intervalSeconds.hashCode()
            result = 31 * result + transientFailures
            return result
        }
    }

    private fun tryFetchToken(
        api: GithubApi,
        deviceCode: String,
        state: PollingState,
    ): AccessTokenResponse? = try {
        api.pollAccessToken(deviceCode)
    } catch (e: GithubApiException) {
        if (e.retryable) {
            state.transientFailures++
            state.intervalSeconds = min(MAX_REQUEST_INTERVAL, state.intervalSeconds + state.transientFailures * 2)
            logger("GitHub temporary error (${e.statusCode}). Retrying in ${state.intervalSeconds}s...")
            null
        } else {
            throw e
        }
    }

    private fun handleErrorResponse(
        response: AccessTokenResponse,
        state: PollingState,
    ) {
        val error = response.error
        when (error) {
            "authorization_pending" -> state.transientFailures = 0
            "slow_down" -> {
                state.intervalSeconds += INTERVAL_INCREASE_STEP
                state.transientFailures = 0
            }
            "expired_token" -> error("Device code expired before authorization completed")
            "access_denied" -> error("Authorization was denied by the user")
            else -> {
                val description = response.errorDescription?.let { " - $it" } ?: ""
                error("GitHub authorization failed: $error$description")
            }
        }
    }

    private fun handleSuccessfulLogin(
        api: GithubApi,
        response: AccessTokenResponse,
    ): String {
        val token = response.accessToken ?: error("GitHub returned success without access_token")
        val user = api.getCurrentUser(token)

        GradleUserProperties.write(tokenPropertyName, token)
        GradleUserProperties.write(usernamePropertyName, user.login)

        logger("Authorized as: ${user.login}")
        logger("Saved token to ~/.gradle/gradle.properties")
        return token
    }
}
