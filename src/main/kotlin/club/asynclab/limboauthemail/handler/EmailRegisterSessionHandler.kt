package club.asynclab.limboauthemail.handler

import club.asynclab.limboauthemail.LimboAuthEmail
import club.asynclab.limboauthemail.misc.HackField
import club.asynclab.limboauthemail.misc.Utils
import club.asynclab.limboauthemail.model.PlayerEmail
import com.velocitypowered.api.proxy.Player
import net.elytrium.limboapi.api.Limbo
import net.elytrium.limboapi.api.player.LimboPlayer
import net.elytrium.limboauth.LimboAuth
import net.elytrium.limboauth.event.PostRegisterEvent
import net.elytrium.limboauth.handler.AuthSessionHandler
import net.elytrium.limboauth.model.RegisteredPlayer
import java.sql.SQLException

class EmailRegisterSessionHandler(
    private val plugin: LimboAuthEmail,
    private val limboAuth: LimboAuth,
    private val proxyPlayer: Player,
) : AuthSessionHandler(
    limboAuth.playerDao,
    proxyPlayer,
    limboAuth,
    limboAuth.playerDao.queryForId(proxyPlayer.username.lowercase())
) {
    private lateinit var limboPlayer: LimboPlayer
    private var playerInfo by HackField<AuthSessionHandler, RegisteredPlayer?>()

    override fun onSpawn(server: Limbo, limboPlayer: LimboPlayer) {
        this.limboPlayer = limboPlayer
        super.onSpawn(server, limboPlayer)
    }

    override fun onChat(message: String) {
        val args = message.split(" ")
        if (!args.isEmpty()) {
            val command = args[0].lowercase()
            when (command) {
                in this.plugin.settings.REGISTER_COMMAND -> {
                    if (this.playerInfo != null) {
                        super.onChat(message)
                        return
                    }

                    // 只接受邮箱
                    if (args.size < 3) {
                        super.onChat("")
                        return
                    }
                    val email = args[1]
                    val repeatedEmail = args[2]
                    if (!listOf(email, repeatedEmail).all(Utils::isValidEmail)) {
                        this.proxyPlayer.sendMessage(this.plugin.getComponent(this.plugin.settings.STRINGS.EMAIL_INVALID))
                        return
                    }

                    if (email != repeatedEmail) {
                        this.proxyPlayer.sendMessage(this.plugin.getComponent(this.plugin.settings.STRINGS.REPEATED_EMAIL_NOT_MATCH))
                        return
                    }

                    if (this.plugin.emailDao.queryForEq("email", email).isNotEmpty()) {
                        this.proxyPlayer.sendMessage(this.plugin.getComponent(this.plugin.settings.STRINGS.EMAIL_USED))
                        return
                    }

                    val tempPassword = Utils.generateTempPassword()
                    this.plugin.mailManager.sendMail(
                        email,
                        this.plugin.emailTemplateRegister,
                        mapOf(
                            "servername" to this.plugin.settings.SERVER_NAME,
                            "playername" to this.proxyPlayer.username,
                            "generatedpass" to tempPassword
                        )
                    )

                    val registeredPlayer = RegisteredPlayer(proxyPlayer).setPassword(tempPassword)
                    val playerEmail = PlayerEmail(proxyPlayer, email)
                    try {
                        this.limboAuth.playerDao.create(registeredPlayer)
                        this.plugin.emailDao.create(playerEmail)
                        this.playerInfo = registeredPlayer
                    } catch (e: SQLException) {
                        this.proxyPlayer.disconnect(this.plugin.getComponent(this.plugin.settings.STRINGS.INTERNAL_ERROR))
                        this.plugin.logger.error(e.message)
                        return
                    }

                    this.proxyPlayer.sendMessage(this.plugin.getComponent(this.plugin.settings.STRINGS.REGISTER_SUCCESSFUL))
                    this.limboAuth.server.eventManager
                        .fire(PostRegisterEvent(null, this.limboPlayer, registeredPlayer, tempPassword))
                }

                in this.plugin.settings.RECOVERY_COMMAND -> {
                    if (this.playerInfo == null) {
                        super.onChat("")
                        return
                    }
                    val args = message.split(" ")

                    if (args.size < 2) {
                        this.proxyPlayer.sendMessage(this.plugin.getComponent(this.plugin.settings.STRINGS.RECOVERY))
                        return
                    }

                    val email = args[1]
                    if (!Utils.isValidEmail(email)) {
                        this.proxyPlayer.sendMessage(this.plugin.getComponent(this.plugin.settings.STRINGS.EMAIL_INVALID))
                        return
                    }

                    val playerEmail: PlayerEmail? = try {
                        this.plugin.emailDao.queryForId(this.proxyPlayer.username.lowercase())
                    } catch (e: SQLException) {
                        this.proxyPlayer.disconnect(this.plugin.getComponent(this.plugin.settings.STRINGS.INTERNAL_ERROR))
                        this.plugin.logger.error(e.message)
                        return
                    }

                    if (playerEmail == null) {
                        this.proxyPlayer.sendMessage(this.plugin.getComponent(this.plugin.settings.STRINGS.NO_EMAIL))
                        return
                    }

                    if (playerEmail.email != email) {
                        this.proxyPlayer.sendMessage(this.plugin.getComponent(this.plugin.settings.STRINGS.EMAIL_NOT_MATCH))
                        return
                    }

                    val tempPassword = Utils.generateTempPassword()
                    this.playerInfo = this.playerInfo!!.setPassword(tempPassword)
                    try {
                        this.limboAuth.playerDao.update(this.playerInfo)
                    } catch (e: SQLException) {
                        this.proxyPlayer.disconnect(this.plugin.getComponent(this.plugin.settings.STRINGS.INTERNAL_ERROR))
                        this.plugin.logger.error(e.message)
                        return
                    }
                    this.plugin.mailManager.sendMail(
                        email,
                        this.plugin.emailTemplateRegister,
                        mapOf(
                            "servername" to this.plugin.settings.SERVER_NAME,
                            "playername" to this.proxyPlayer.username,
                            "generatedpass" to tempPassword
                        )
                    )
                    this.proxyPlayer.sendMessage(this.plugin.getComponent(this.plugin.settings.STRINGS.RECOVERY_SUCCESSFUL))
                }
            }
        }

        super.onChat(message)
    }
}