import org.gradle.api.Plugin
import org.gradle.api.Project
import util.EnvSyncState
import java.io.File

private const val GITHUB_ENV_SYNC_GROUP = "github env sync"

@Suppress("UNUSED")
internal class GithubEnvSyncPlugin : Plugin<Project> {

    override fun apply(target: Project) = target.applyGithubEnvSync()

    private fun Project.applyGithubEnvSync() {
        val ext = extensions.create(
            "githubEnvSync",
            GithubEnvSyncExtension::class.java,
        )

        ext.tokenPropertyName.convention("github.env.sync.token")
        ext.usernamePropertyName.convention("github.env.sync.username")
        ext.autoOpenBrowser.convention(true)
        ext.failOnMissingVariables.convention(true)
        ext.runOnIdeSync.convention(true)
        ext.environments.convention(emptyList())
        ext.generatedRootDir.convention(ext.outputDir)

        registerGithubLoginTask(ext)
        registerEnvSyncTask(ext)
        registerLogoutTask(ext)

        afterEvaluate {
            if (!ext.runOnIdeSync.get()) return@afterEvaluate
            if (!isIdeSync(project)) return@afterEvaluate

            val logger = project.logger
            logger.lifecycle("IDE Gradle sync detected")

            val templatesDir = ext.templatesDir.get().asFile
            val targetEnvironments = resolveConfiguredEnvironments(
                environments = ext.environments.orNull.orEmpty(),
                legacyEnvironment = ext.environment.orNull,
            )
            val generatedRootDir = ext.generatedRootDir.get().asFile

            val allUpToDate = targetEnvironments.all { environment ->
                EnvSyncState.isUpToDate(
                    outputDir = File(generatedRootDir, environment),
                    templatesDir = templatesDir,
                    owner = ext.owner.get(),
                    repo = ext.repo.get(),
                    environment = environment,
                )
            }

            if (allUpToDate) {
                logger.lifecycle("Env files already generated for IDE sync. Skipping githubLogin/syncGithubEnv.")
                return@afterEvaluate
            }

            try {
                GithubLoginAction(
                    clientId = ext.clientId.get(),
                    tokenPropertyName = ext.tokenPropertyName.get(),
                    usernamePropertyName = ext.usernamePropertyName.get(),
                    autoOpenBrowser = ext.autoOpenBrowser.get(),
                    logger = { logger.lifecycle(it) },
                ).runLogin()

                GithubEnvSyncAction(
                    owner = ext.owner.get(),
                    repo = ext.repo.get(),
                    environments = ext.environments.orNull.orEmpty(),
                    legacyEnvironment = ext.environment.orNull,
                    templatesDir = templatesDir,
                    outputDir = ext.outputDir.orNull?.asFile,
                    generatedRootDir = generatedRootDir,
                    tokenPropertyName = ext.tokenPropertyName.get(),
                    usernamePropertyName = ext.usernamePropertyName.get(),
                    failOnMissingVariables = ext.failOnMissingVariables.get(),
                    logger = { logger.lifecycle(it) },
                ).runIfNeeded()
            } catch (t: Throwable) {
                logger.warn(
                    "githubEnvSync auto-run was skipped during IDE sync: ${t.message}. " +
                        "Project sync will continue. You can run githubLogin / syncGithubEnv manually.",
                )
            }
        }
    }

    private fun Project.registerGithubLoginTask(ext: GithubEnvSyncExtension) {
        tasks.register("githubLogin", GithubLoginTask::class.java) {
            it.group = GITHUB_ENV_SYNC_GROUP
            it.clientId.set(ext.clientId)
            it.tokenPropertyName.set(ext.tokenPropertyName)
            it.usernamePropertyName.set(ext.usernamePropertyName)
            it.autoOpenBrowser.set(ext.autoOpenBrowser)
            it.owner.set(ext.owner)
            it.repo.set(ext.repo)
            it.environment.set(ext.environment)
            it.environments.set(ext.environments)
            it.templatesDir.set(ext.templatesDir)
            it.outputDir.set(ext.outputDir)
            it.generatedRootDir.set(ext.generatedRootDir)
            it.failOnMissingVariables.set(ext.failOnMissingVariables)
        }
    }

    private fun Project.registerEnvSyncTask(ext: GithubEnvSyncExtension) {
        tasks.register("syncGithubEnv", SyncGithubEnvTask::class.java) {
            it.group = GITHUB_ENV_SYNC_GROUP
            it.owner.set(ext.owner)
            it.repo.set(ext.repo)
            it.environment.set(ext.environment)
            it.environments.set(ext.environments)
            it.templatesDir.set(ext.templatesDir)
            it.outputDir.set(ext.outputDir)
            it.generatedRootDir.set(ext.generatedRootDir)
            it.tokenPropertyName.set(ext.tokenPropertyName)
            it.usernamePropertyName.set(ext.usernamePropertyName)
            it.failOnMissingVariables.set(ext.failOnMissingVariables)
        }
    }

    private fun Project.registerLogoutTask(ext: GithubEnvSyncExtension) {
        tasks.register("githubLogout", GithubLogoutTask::class.java) {
            it.group = GITHUB_ENV_SYNC_GROUP
            it.tokenPropertyName.set(ext.tokenPropertyName)
            it.usernamePropertyName.set(ext.usernamePropertyName)
        }
    }

    private fun resolveConfiguredEnvironments(
        environments: List<String>,
        legacyEnvironment: String?,
    ): List<String> {
        val result = linkedSetOf<String>()
        result.addAll(environments.filter { it.isNotBlank() })
        if (result.isEmpty() && !legacyEnvironment.isNullOrBlank()) {
            result += legacyEnvironment
        }
        return result.toList()
    }

    private fun isIdeSync(project: Project): Boolean {
        val ideaSync = project.providers.systemProperty("idea.sync.active").orNull == "true"
        val androidIde = project.providers.systemProperty("android.injected.invoked.from.ide").orNull == "true"
        return ideaSync || androidIde
    }
}
