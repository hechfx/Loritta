package net.perfectdreams.loritta.legacy.commands.vanilla.economy

import net.perfectdreams.loritta.common.api.commands.ArgumentType
import net.perfectdreams.loritta.common.commands.CommandCategory
import net.perfectdreams.loritta.common.api.commands.arguments
import net.perfectdreams.loritta.legacy.platform.discord.LorittaDiscord
import net.perfectdreams.loritta.legacy.platform.discord.legacy.commands.DiscordAbstractCommandBase

class EmojiFightCommand(val m: LorittaDiscord) : DiscordAbstractCommandBase(
		m,
		listOf("emojifight", "rinhadeemoji", "emotefight"),
		net.perfectdreams.loritta.common.commands.CommandCategory.ECONOMY
) {
	override fun command() = create {
		localizedDescription("commands.command.emojifight.description")

		usage {
			arguments {
				argument(ArgumentType.NUMBER) {
					optional = true
				}
			}
		}

		this.similarCommands = listOf("EmojiFightBetCommand")
		this.canUseInPrivateChannel = false

		executesDiscord {
			val maxPlayersInEvent = (
					(this.args.getOrNull(0) ?.toIntOrNull() ?: EmojiFight.DEFAULT_MAX_PLAYER_COUNT)
							.coerceIn(2, EmojiFight.DEFAULT_MAX_PLAYER_COUNT)
			)

			val emojiFight = EmojiFight(
					this,
					null,
					maxPlayersInEvent
			)

			emojiFight.start()
		}
	}
}