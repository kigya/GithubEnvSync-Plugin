package github

import org.gradle.internal.impldep.com.fasterxml.jackson.annotation.JsonProperty

internal data class DeviceCodeResponse(
    @field:JsonProperty("device_code")
    val deviceCode: String,
    @field:JsonProperty("user_code")
    val userCode: String,
    @field:JsonProperty("verification_uri")
    val verificationUri: String,
    @field:JsonProperty("expires_in")
    val expiresIn: Long,
    @field:JsonProperty("interval")
    val interval: Long,
)
