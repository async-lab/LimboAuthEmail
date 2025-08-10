package club.asynclab.limboauthemail.misc

object Utils {
    fun generateTempPassword(): String {
        val chars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..12).map { chars.random() }.joinToString("")
    }

    fun isValidEmail(email: String) = email.matches(Regex("^[A-Za-z0-9+_.-]+@(.+)$"))
}