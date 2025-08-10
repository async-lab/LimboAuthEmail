package club.asynclab.limboauthemail

import org.spongepowered.configurate.CommentedConfigurationNode

class Settings(private val config: Config) {
    private val rootConfig = this.config.getRootNode("config.yml")!!

    val SERVER_NAME = this.rootConfig.node("server-name").getString("")

    val PREFIX = this.rootConfig.node("prefix").getString("")
    val REGISTER_COMMAND = this.rootConfig.node("register-command").getList(String::class.java) ?: listOf()
    val RECOVERY_COMMAND = this.rootConfig.node("recovery-command").getList(String::class.java) ?: listOf()

    val SMTP = Smtp(this.rootConfig)

    class Smtp(private val rootConfig: CommentedConfigurationNode) {
        val HOST = rootConfig.node("smtp", "host").getString("")
        val PORT = rootConfig.node("smtp", "port").getInt(587)
        val TLS = rootConfig.node("smtp", "tls").getBoolean(true)
        val ACCOUNT = rootConfig.node("smtp", "account").getString("")
        val PASSWORD = rootConfig.node("smtp", "password").getString("")
        val SENDER = rootConfig.node("smtp", "sender").getString("")
        val SUBJECT = rootConfig.node("smtp", "subject").getString("")
    }

    val STRINGS = Strings(this.rootConfig)

    class Strings(private val rootConfig: CommentedConfigurationNode) {
        val EMAIL_INVALID = rootConfig.node("strings", "email-invalid").getString("")
        val REPEATED_EMAIL_NOT_MATCH = rootConfig.node("strings", "repeated-email-not-match").getString("")
        val REGISTER_SUCCESSFUL = rootConfig.node("strings", "register-successful").getString("")
        val INTERNAL_ERROR = rootConfig.node("strings", "internal-error").getString("")
        val RECOVERY = rootConfig.node("strings", "recovery").getString("")
        val RECOVERY_SUCCESSFUL = rootConfig.node("strings", "recovery-successful").getString("")
        val EMAIL_NOT_MATCH = rootConfig.node("strings", "email-not-match").getString("")
        val NO_EMAIL = rootConfig.node("strings", "no-email").getString("")
        val ONLY_PLAYER = rootConfig.node("strings", "only-player").getString("")
        val EMAIL_UNCHANGED = rootConfig.node("strings", "email-unchanged").getString("")
        val EMAIL_CHANGED = rootConfig.node("strings", "email-changed").getString("")
        val EMAIL_USED = rootConfig.node("strings", "email-used").getString("")
    }
}