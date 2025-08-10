package club.asynclab.limboauthemail.model

import com.velocitypowered.api.proxy.Player
import net.elytrium.limboauth.thirdparty.com.j256.ormlite.field.DatabaseField
import net.elytrium.limboauth.thirdparty.com.j256.ormlite.table.DatabaseTable

@DatabaseTable(tableName = "player_emails")
class PlayerEmail() {
    @DatabaseField(id = true, columnName = "username")
    var username: String? = null

    @DatabaseField(columnName = "email", unique = true, canBeNull = false)
    var email: String? = null

    constructor(player: Player, email: String) : this() {
        this.username = player.username.lowercase()
        this.email = email
    }
}