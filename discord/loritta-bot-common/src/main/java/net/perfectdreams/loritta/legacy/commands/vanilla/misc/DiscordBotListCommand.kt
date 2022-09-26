package net.perfectdreams.loritta.legacy.commands.vanilla.misc

import net.perfectdreams.loritta.legacy.utils.Constants
import net.dv8tion.jda.api.EmbedBuilder
import net.perfectdreams.loritta.legacy.common.commands.CommandCategory
import net.perfectdreams.loritta.legacy.platform.discord.LorittaDiscord
import net.perfectdreams.loritta.legacy.platform.discord.legacy.commands.DiscordAbstractCommandBase
import net.perfectdreams.loritta.legacy.utils.Emotes

class DiscordBotListCommand(loritta: LorittaDiscord): DiscordAbstractCommandBase(loritta, listOf("dbl", "upvote"), CommandCategory.MISC) {
    companion object {
        private const val LOCALE_PREFIX = "commands.command.dbl"
    }

    override fun command() = create {
        localizedDescription("$LOCALE_PREFIX.description")

        executesDiscord {
            val context = this
            val embed = EmbedBuilder().apply {
                setColor(Constants.LORITTA_AQUA)
                setThumbnail("${net.perfectdreams.loritta.legacy.utils.loritta.instanceConfig.loritta.website.url}assets/img/loritta_star.png")
                setTitle("✨ Discord Bot List")
                setDescription(
                    locale[
                            "$LOCALE_PREFIX.info",
                            context.serverConfig.commandPrefix,
                            "https://top.gg/bot/${loritta.discordConfig.discord.clientId}",
                            Emotes.DISCORD_BOT_LIST
                    ]
                )
            }

            context.sendMessage(context.getUserMention(true), embed.build())
        }
    }
}