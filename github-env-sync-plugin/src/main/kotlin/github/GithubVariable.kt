package github

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class GithubVariable(
    @field:JsonProperty("name")
    val name: String,
    @field:JsonProperty("value")
    val value: String,
)
