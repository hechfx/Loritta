package net.perfectdreams.loritta.legacy.serializable

import kotlinx.serialization.Serializable
import net.perfectdreams.loritta.legacy.api.commands.CommandArguments
import net.perfectdreams.loritta.legacy.common.commands.CommandCategory
import net.perfectdreams.loritta.legacy.common.locale.LocaleKeyData

@Serializable
class CommandInfo(
    val name: String,
    val label: String,
    val aliases: List<String>,
    val category: CommandCategory,
    val description: LocaleKeyData? = null,
    val usage: CommandArguments? = null,
    val examples: LocaleKeyData? = null,
    val cooldown: Int,
    val canUseInPrivateChannel: Boolean,
    val userRequiredPermissions: List<String>,
    val userRequiredLorittaPermissions: List<String>,
    val botRequiredPermissions: List<String>,
    val similarCommands: List<String>
)