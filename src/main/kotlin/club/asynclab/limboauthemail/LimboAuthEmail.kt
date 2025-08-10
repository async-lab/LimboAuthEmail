package club.asynclab.limboauthemail;

import club.asynclab.limboauthemail.handler.EmailRegisterSessionHandler
import club.asynclab.limboauthemail.misc.MailManager
import club.asynclab.limboauthemail.model.PlayerEmail
import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyReloadEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import net.elytrium.limboauth.LimboAuth
import net.elytrium.limboauth.event.PreAuthorizationEvent
import net.elytrium.limboauth.event.PreRegisterEvent
import net.elytrium.limboauth.event.TaskEvent
import net.elytrium.limboauth.thirdparty.com.j256.ormlite.dao.Dao
import net.elytrium.limboauth.thirdparty.com.j256.ormlite.dao.DaoManager
import net.elytrium.limboauth.thirdparty.com.j256.ormlite.table.TableUtils
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.slf4j.Logger
import java.io.File
import java.nio.file.Path


@Plugin(
    id = "limboauthemail", name = "LimboAuthEmail", version = BuildConstants.VERSION
)
class LimboAuthEmail @Inject constructor(
    val logger: Logger,
    val server: ProxyServer,
    @DataDirectory val dataDirectory: Path,
) {
    val config = Config(this)

    lateinit var limboAuth: LimboAuth
    lateinit var emailDao: Dao<PlayerEmail, String>
    lateinit var settings: Settings

    lateinit var emailTemplateRegister: File

    lateinit var mailManager: MailManager

    private fun init() {
        this.config.loadConfiguration("config.yml")
        this.emailTemplateRegister = this.config.loadFile("templates/register.html")
        this.settings = Settings(this.config)
        Commands(this).init()
    }

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        this.limboAuth = this.server.pluginManager.getPlugin("limboauth")
            .flatMap(PluginContainer::getInstance)
            .orElseThrow() as LimboAuth
        this.emailDao = DaoManager.createDao(this.limboAuth.connectionSource, PlayerEmail::class.java)
        TableUtils.createTableIfNotExists(this.limboAuth.connectionSource, PlayerEmail::class.java)
        this.mailManager = MailManager(this)
        this.init()
        this.logger.info("Loaded!")
    }

    @Subscribe
    fun onPreRegister(event: PreRegisterEvent) {
        event.result = TaskEvent.Result.WAIT
        limboAuth.authServer.spawnPlayer(event.player, EmailRegisterSessionHandler(this, this.limboAuth, event.player))
    }

    @Subscribe
    fun onAuthorization(event: PreAuthorizationEvent) {
        event.result = TaskEvent.Result.WAIT
        limboAuth.authServer.spawnPlayer(event.player, EmailRegisterSessionHandler(this, this.limboAuth, event.player))
    }

    @Subscribe
    fun onProxyReload(event: ProxyReloadEvent) {
        this.init()
        this.logger.info("Reloaded!")
    }

    fun getComponent(message: String): Component {
        return MiniMessage.miniMessage().deserialize(message.replace("{PRFX}", this.settings.PREFIX))
    }
}
