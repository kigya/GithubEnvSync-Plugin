package github

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class AccessTokenResponse(
    @field:JsonProperty("access_token")
    val accessToken: String? = null,
    @field:JsonProperty("expires_in")
    val expiresIn: Long? = null,
    @field:JsonProperty("refresh_token")
    val refreshToken: String? = null,
    @field:JsonProperty("refresh_token_expires_in")
    val refreshTokenExpiresIn: Long? = null,
    @field:JsonProperty("token_type")
    val tokenType: String? = null,
    @field:JsonProperty("scope")
    val scope: String? = null,
    @field:JsonProperty("error")
    val error: String? = null,
    @field:JsonProperty("error_description")
    val errorDescription: String? = null,
    @field:JsonProperty("error_uri")
    val errorUri: String? = null,
)
