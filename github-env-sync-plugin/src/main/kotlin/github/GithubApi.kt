package github

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import util.GithubApiException

private const val APPLICATION_VND_GITHUB_JSON = "application/vnd.github+json"

private const val AUTHORIZATION_HEADER = "Authorization"

private const val X_GIT_HUB_API_VERSION_HEADER = "X-GitHub-Api-Version"

private const val ACCEPT_HEADER = "Accept"

private const val CONTENT_TYPE_HEADER = "Content-Type"

private const val GITHUB_BASE_URL = "https://github.com"

private const val GITHUB_API_BASE_URL = "https://api.github.com"

private const val GITHUB_API_VERSION = "2022-11-28"

private const val HTTP_STATUS_OK = 200

private const val HTTP_MISCELLANEOUS_PERSISTENT_WARNING = 299

private const val HTTP_TO_MANY_REQUESTS = 429

private const val HTTP_BAD_GATEWAY = 502

private const val HTTP_SERVICE_UNAVAILABLE = 503

private const val HTTP_GATEWAY_TIMEOUT = 504

private const val HTTP_NOT_FOUND = 404

internal class GithubApi(
    private val clientId: String? = null,
) {

    private val mapper = jacksonObjectMapper()
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private val oauthHeaders = mapOf(
        ACCEPT_HEADER to "application/json",
        CONTENT_TYPE_HEADER to "application/x-www-form-urlencoded",
    )

    fun requestDeviceCode(): DeviceCodeResponse {
        val id = clientId ?: error("GitHub clientId is required for login")
        val response = Http.post(
            url = "$GITHUB_BASE_URL/login/device/code",
            headers = oauthHeaders,
            body = "client_id=$id",
        )

        if (response.code !in HTTP_STATUS_OK..HTTP_MISCELLANEOUS_PERSISTENT_WARNING) {
            error("Failed to start GitHub device flow. HTTP ${response.code}: ${response.body}")
        }

        return mapper.readValue(response.body)
    }

    fun pollAccessToken(deviceCode: String): AccessTokenResponse {
        val id = clientId ?: error("GitHub clientId is required for login")

        val response = Http.post(
            url = "$GITHUB_BASE_URL/login/oauth/access_token",
            headers = mapOf(
                ACCEPT_HEADER to "application/json",
                CONTENT_TYPE_HEADER to "application/x-www-form-urlencoded",
            ),
            body = buildString {
                append("client_id=").append(id)
                append("&device_code=").append(deviceCode)
                append("&grant_type=urn:ietf:params:oauth:grant-type:device_code")
            },
        )

        return when (response.code) {
            in HTTP_STATUS_OK..HTTP_MISCELLANEOUS_PERSISTENT_WARNING ->
                mapper.readValue<AccessTokenResponse>(response.body)

            in setOf(
                HTTP_TO_MANY_REQUESTS,
                HTTP_BAD_GATEWAY,
                HTTP_SERVICE_UNAVAILABLE,
                HTTP_GATEWAY_TIMEOUT,
            ),
            -> throw GithubApiException(
                message = "Transient GitHub error during token polling: HTTP ${response.code}",
                statusCode = response.code,
                retryable = true,
            )

            else -> throw GithubApiException(
                message = "Failed to poll GitHub access token. HTTP ${response.code}: ${response.body}",
                statusCode = response.code,
                retryable = false,
            )
        }
    }

    fun getCurrentUser(token: String): GithubUser {
        val response = Http.get(
            url = "$GITHUB_API_BASE_URL/user",
            headers = mapOf(
                AUTHORIZATION_HEADER to token.bearer(),
                ACCEPT_HEADER to APPLICATION_VND_GITHUB_JSON,
                X_GIT_HUB_API_VERSION_HEADER to GITHUB_API_VERSION,
            ),
        )

        if (response.code in setOf(
                HTTP_TO_MANY_REQUESTS, HTTP_BAD_GATEWAY,
                HTTP_SERVICE_UNAVAILABLE, HTTP_GATEWAY_TIMEOUT,
            )
        ) {
            throw GithubApiException(
                message = "Transient GitHub error while loading current user: HTTP ${response.code}",
                statusCode = response.code,
                retryable = true,
            )
        }

        if (response.code !in HTTP_STATUS_OK..HTTP_MISCELLANEOUS_PERSISTENT_WARNING) {
            throw GithubApiException(
                message = "Failed to fetch current GitHub user. HTTP ${response.code}: ${response.body}",
                statusCode = response.code,
                retryable = false,
            )
        }

        return mapper.readValue(response.body)
    }

    fun getCollaboratorPermission(
        token: String,
        owner: String,
        repo: String,
        username: String,
    ): CollaboratorPermissionResponse {
        val response = Http.get(
            url = "$GITHUB_API_BASE_URL/repos/$owner/$repo/collaborators/$username/permission",
            headers = mapOf(
                AUTHORIZATION_HEADER to token.bearer(),
                ACCEPT_HEADER to APPLICATION_VND_GITHUB_JSON,
                X_GIT_HUB_API_VERSION_HEADER to GITHUB_API_VERSION,
            ),
        )

        if (response.code in setOf(
                HTTP_TO_MANY_REQUESTS, HTTP_BAD_GATEWAY,
                HTTP_SERVICE_UNAVAILABLE, HTTP_GATEWAY_TIMEOUT,
            )
        ) {
            throw GithubApiException(
                message = "Transient GitHub error while checking collaborator permission: HTTP ${response.code}",
                statusCode = response.code,
                retryable = true,
            )
        }

        if (response.code !in HTTP_STATUS_OK..HTTP_MISCELLANEOUS_PERSISTENT_WARNING) {
            throw GithubApiException(
                message = "Failed to check collaborator permission. HTTP ${response.code}: ${response.body}",
                statusCode = response.code,
                retryable = false,
            )
        }

        return mapper.readValue(response.body)
    }

    fun getRepositoryVariables(
        token: String,
        owner: String,
        repo: String,
    ): Map<String, String> {
        val response = Http.get(
            url = "$GITHUB_API_BASE_URL/repos/$owner/$repo/actions/variables?per_page=100",
            headers = mapOf(
                AUTHORIZATION_HEADER to token.bearer(),
                ACCEPT_HEADER to APPLICATION_VND_GITHUB_JSON,
                X_GIT_HUB_API_VERSION_HEADER to GITHUB_API_VERSION,
            ),
        )

        if (response.code in setOf(
                HTTP_TO_MANY_REQUESTS, HTTP_BAD_GATEWAY,
                HTTP_SERVICE_UNAVAILABLE, HTTP_GATEWAY_TIMEOUT,
            )
        ) {
            throw GithubApiException(
                message = "Transient GitHub error while loading repository variables: HTTP ${response.code}",
                statusCode = response.code,
                retryable = true,
            )
        }

        if (response.code !in HTTP_STATUS_OK..HTTP_MISCELLANEOUS_PERSISTENT_WARNING) {
            throw GithubApiException(
                message = "Failed to fetch repository variables. HTTP ${response.code}: ${response.body}",
                statusCode = response.code,
                retryable = false,
            )
        }

        val parsed: VariablesListResponse = mapper.readValue(response.body)
        return parsed.variables.associate { it.name to it.value }
    }

    fun getEnvironmentVariables(
        token: String,
        owner: String,
        repo: String,
        environment: String,
    ): Map<String, String> {
        val response = Http.get(
            url = "$GITHUB_API_BASE_URL/repos/$owner/$repo/environments/$environment/variables?per_page=100",
            headers = mapOf(
                AUTHORIZATION_HEADER to token.bearer(),
                ACCEPT_HEADER to APPLICATION_VND_GITHUB_JSON,
                X_GIT_HUB_API_VERSION_HEADER to GITHUB_API_VERSION,
            ),
        )

        if (response.code == HTTP_NOT_FOUND) return emptyMap()

        if (response.code in setOf(
                HTTP_TO_MANY_REQUESTS, HTTP_BAD_GATEWAY,
                HTTP_SERVICE_UNAVAILABLE, HTTP_GATEWAY_TIMEOUT,
            )
        ) {
            throw GithubApiException(
                message = "Transient GitHub error while loading environment variables: HTTP ${response.code}",
                statusCode = response.code,
                retryable = true,
            )
        }

        if (response.code !in HTTP_STATUS_OK..HTTP_MISCELLANEOUS_PERSISTENT_WARNING) {
            throw GithubApiException(
                message = "Failed to fetch environment variables. HTTP ${response.code}: ${response.body}",
                statusCode = response.code,
                retryable = false,
            )
        }

        val parsed: VariablesListResponse = mapper.readValue(response.body)
        return parsed.variables.associate { it.name to it.value }
    }

    private fun String.bearer() = "Bearer $this"
}
