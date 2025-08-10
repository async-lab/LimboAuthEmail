package club.asynclab.limboauthemail.misc

import club.asynclab.limboauthemail.LimboAuthEmail
import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*

class MailManager(private val plugin: LimboAuthEmail) {
    fun sendMail(email: String, template: File, placeholders: Map<String, String>) {
        try {
            val settings = this@MailManager.plugin.settings
            val props = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", settings.SMTP.TLS)
                put("mail.smtp.host", settings.SMTP.HOST)
                put("mail.smtp.port", settings.SMTP.PORT)
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(
                        settings.SMTP.ACCOUNT,
                        settings.SMTP.PASSWORD
                    )
                }
            })

            val html = loadTemplate(template, placeholders)

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(settings.SMTP.ACCOUNT, settings.SMTP.SENDER))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(email))
                subject = settings.SMTP.SUBJECT
                setContent(html, "text/html; charset=UTF-8")
            }

            Transport.send(message)
        } catch (e: Exception) {
            plugin.logger.error("Failed to send email to $email", e)
        }
    }

    fun loadTemplate(file: File, placeholders: Map<String, String>): String =
        placeholders.entries.fold(file.readText(StandardCharsets.UTF_8)) { content, (key, value) ->
            content.replace(Regex("<$key\\s*/>", RegexOption.IGNORE_CASE), value)
        }
}