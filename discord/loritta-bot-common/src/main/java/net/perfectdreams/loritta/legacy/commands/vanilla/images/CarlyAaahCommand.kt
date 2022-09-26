package net.perfectdreams.loritta.legacy.commands.vanilla.images

import net.perfectdreams.loritta.common.commands.CommandCategory
import net.perfectdreams.loritta.legacy.platform.discord.LorittaDiscord
import net.perfectdreams.loritta.legacy.commands.vanilla.images.base.GabrielaImageServerCommandBase

class CarlyAaahCommand(m: LorittaDiscord) : GabrielaImageServerCommandBase(
	m,
	listOf("carlyaaah"),
	1,
	"commands.command.carlyaaah.description",
	"/api/v1/videos/carly-aaah",
	"carly_aaah.mp4",
	category = net.perfectdreams.loritta.common.commands.CommandCategory.VIDEOS,
	slashCommandName = "carlyaaah"
)