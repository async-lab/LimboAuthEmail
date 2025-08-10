package club.asynclab.limboauthemail

import club.asynclab.limboauthemail.misc.Utils
import club.asynclab.limboauthemail.model.PlayerEmail
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.command.CommandManager
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import net.elytrium.limboapi.api.command.LimboCommandMeta
import java.util.*


class Commands(private val plugin: LimboAuthEmail) {

    fun init() {
        val commandManager: CommandManager = this.plugin.server.commandManager
        val changeEmailNode = BrigadierCommand.literalArgumentBuilder("changeemail")
            .then(
                BrigadierCommand.requiredArgumentBuilder<String?>("newEmail", StringArgumentType.greedyString())
                    .executes(this::execute)
            )
            .build()

        commandManager.register(
            commandManager.metaBuilder("changeemail")
                .plugin(this.plugin)
                .build(), BrigadierCommand(changeEmailNode)
        )

        this.plugin.limboAuth.authServer.registerCommand(LimboCommandMeta(this.plugin.settings.RECOVERY_COMMAND.map { command -> command.trimStart { it == '/' } }))
    }

    private fun execute(context: CommandContext<CommandSource>): Int {
        try {
            val source = context.getSource()
            if (source !is Player) {
                source.sendMessage(this.plugin.getComponent(this.plugin.settings.STRINGS.ONLY_PLAYER))
                return Command.SINGLE_SUCCESS
            }

            val newEmail = context.getArgument("newEmail", String::class.java)

            if (!Utils.isValidEmail(newEmail)) {
                source.sendMessage(this.plugin.getComponent(this.plugin.settings.STRINGS.EMAIL_INVALID))
                return Command.SINGLE_SUCCESS
            }

            try {
                val playerEmail =
                    this.plugin.emailDao.queryForId(source.username.lowercase(Locale.getDefault()))
                        ?: PlayerEmail(source, newEmail)

                if (playerEmail.email == newEmail) {
                    source.sendMessage(this.plugin.getComponent(this.plugin.settings.STRINGS.EMAIL_UNCHANGED))
                    return Command.SINGLE_SUCCESS
                }

                if (this.plugin.emailDao.queryForEq("email", newEmail).isNotEmpty()) {
                    source.sendMessage(this.plugin.getComponent(this.plugin.settings.STRINGS.EMAIL_USED))
                    return Command.SINGLE_SUCCESS
                }

                playerEmail.email = newEmail
                this.plugin.emailDao.update(playerEmail)

                source.sendMessage(this.plugin.getComponent(this.plugin.settings.STRINGS.EMAIL_CHANGED))
            } catch (e: Exception) {
                this.plugin.logger.error("更新邮箱失败", e)
                source.sendMessage(this.plugin.getComponent(this.plugin.settings.STRINGS.INTERNAL_ERROR))
            }
        } catch (e: Exception) {
            this.plugin.logger.error("执行 /changeemail 出错", e)
        }

        return Command.SINGLE_SUCCESS
    }
}