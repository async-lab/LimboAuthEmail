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

    private fun execute(ctx: CommandContext<CommandSource>): Int {
        val source = ctx.source
        if (source !is Player) {
            source.sendMessage(plugin.getComponent(plugin.settings.STRINGS.ONLY_PLAYER))
            return Command.SINGLE_SUCCESS
        }

        val newEmail = ctx.getArgument("newEmail", String::class.java).trim()
        if (!Utils.isValidEmail(newEmail)) {
            source.sendMessage(plugin.getComponent(plugin.settings.STRINGS.EMAIL_INVALID))
            return Command.SINGLE_SUCCESS
        }

        val username = source.username.lowercase(Locale.getDefault())

        return try {
            val existing = plugin.emailDao.queryForId(username)

            // 1. 邮箱未改变
            if (existing?.email == newEmail) {
                source.sendMessage(plugin.getComponent(plugin.settings.STRINGS.EMAIL_UNCHANGED))
                return Command.SINGLE_SUCCESS
            }

            // 2. 邮箱已存在别处
            if (plugin.emailDao.queryForEq("email", newEmail).isNotEmpty()) {
                source.sendMessage(plugin.getComponent(plugin.settings.STRINGS.EMAIL_USED))
                return Command.SINGLE_SUCCESS
            }

            // 3. 创建或更新
            val updatedRecord = existing?.apply { email = newEmail } ?: PlayerEmail(source, newEmail)
            plugin.emailDao.createOrUpdate(updatedRecord)

            source.sendMessage(plugin.getComponent(plugin.settings.STRINGS.EMAIL_CHANGED))
            Command.SINGLE_SUCCESS
        } catch (e: Exception) {
            plugin.logger.error("更新邮箱失败", e)
            source.sendMessage(plugin.getComponent(plugin.settings.STRINGS.INTERNAL_ERROR))
            Command.SINGLE_SUCCESS
        }
    }
}