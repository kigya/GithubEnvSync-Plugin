package github

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class GithubUser(
    @field:JsonProperty("login")
    val login: String,
    @field:JsonProperty("id")
    val id: Long? = null,
)
