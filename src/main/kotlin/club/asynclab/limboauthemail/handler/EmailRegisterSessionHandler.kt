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
        val args = message.trim().split(" ")
        if (args.isEmpty()) {
            super.onChat(message)
            return
        }

        when (args[0].lowercase()) {
            in plugin.settings.REGISTER_COMMAND -> handleRegister(args)
            in plugin.settings.RECOVERY_COMMAND -> handleRecovery(args)
            else -> super.onChat(message)
        }
    }

    /** 注册命令逻辑 */
    private fun handleRegister(args: List<String>) {
        // 已经注册过
        if (playerInfo != null) return super.onChat("")

        if (args.size < 3) return super.onChat("")

        val email = args[1]
        val repeatEmail = args[2]

        if (!Utils.isValidEmail(email) || !Utils.isValidEmail(repeatEmail)) {
            proxyPlayer.sendMessage(plugin.getComponent(plugin.settings.STRINGS.EMAIL_INVALID))
            return
        }
        if (email != repeatEmail) {
            proxyPlayer.sendMessage(plugin.getComponent(plugin.settings.STRINGS.REPEATED_EMAIL_NOT_MATCH))
            return
        }
        if (plugin.emailDao.queryForEq("email", email).isNotEmpty()) {
            proxyPlayer.sendMessage(plugin.getComponent(plugin.settings.STRINGS.EMAIL_USED))
            return
        }

        // 生成密码 & 发邮件
        val tempPassword = Utils.generateTempPassword()
        plugin.mailManager.sendMail(
            email, plugin.emailTemplateRegister, mapOf(
                "servername" to plugin.settings.SERVER_NAME,
                "playername" to proxyPlayer.username,
                "generatedpass" to tempPassword
            )
        )

        val registeredPlayer = RegisteredPlayer(proxyPlayer).setPassword(tempPassword)
        val playerEmail = PlayerEmail(proxyPlayer, email)

        try {
            limboAuth.playerDao.create(registeredPlayer)
            plugin.emailDao.create(playerEmail)
            playerInfo = registeredPlayer
        } catch (e: SQLException) {
            proxyPlayer.disconnect(plugin.getComponent(plugin.settings.STRINGS.INTERNAL_ERROR))
            plugin.logger.error(e.message)
            return
        }

        proxyPlayer.sendMessage(plugin.getComponent(plugin.settings.STRINGS.REGISTER_SUCCESSFUL))
        limboAuth.server.eventManager.fire(PostRegisterEvent(null, limboPlayer, registeredPlayer, tempPassword))
    }

    /** 密码找回逻辑 */
    private fun handleRecovery(args: List<String>) {
        if (playerInfo == null) return super.onChat("")

        if (args.size < 2) {
            proxyPlayer.sendMessage(plugin.getComponent(plugin.settings.STRINGS.RECOVERY))
            return
        }

        val email = args[1]
        if (!Utils.isValidEmail(email)) {
            proxyPlayer.sendMessage(plugin.getComponent(plugin.settings.STRINGS.EMAIL_INVALID))
            return
        }

        val storedEmail = try {
            plugin.emailDao.queryForId(proxyPlayer.username.lowercase())
        } catch (e: SQLException) {
            proxyPlayer.disconnect(plugin.getComponent(plugin.settings.STRINGS.INTERNAL_ERROR))
            plugin.logger.error(e.message)
            return
        }

        if (storedEmail == null) {
            proxyPlayer.sendMessage(plugin.getComponent(plugin.settings.STRINGS.NO_EMAIL))
            return
        }
        if (storedEmail.email != email) {
            proxyPlayer.sendMessage(plugin.getComponent(plugin.settings.STRINGS.EMAIL_NOT_MATCH))
            return
        }

        val tempPassword = Utils.generateTempPassword()
        playerInfo = playerInfo!!.setPassword(tempPassword)

        try {
            limboAuth.playerDao.update(playerInfo)
        } catch (e: SQLException) {
            proxyPlayer.disconnect(plugin.getComponent(plugin.settings.STRINGS.INTERNAL_ERROR))
            plugin.logger.error(e.message)
            return
        }

        plugin.mailManager.sendMail(
            email, plugin.emailTemplateRegister, mapOf(
                "servername" to plugin.settings.SERVER_NAME,
                "playername" to proxyPlayer.username,
                "generatedpass" to tempPassword
            )
        )
        proxyPlayer.sendMessage(plugin.getComponent(plugin.settings.STRINGS.RECOVERY_SUCCESSFUL))
    }
}