package github

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class CollaboratorPermissionResponse(
    @field:JsonProperty("permission")
    val permission: String? = null,
    @field:JsonProperty("user")
    val user: GithubUser? = null,
)
