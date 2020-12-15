package com.mrpowergamerbr.loritta.commands.vanilla.social

import com.mrpowergamerbr.loritta.commands.AbstractCommand
import com.mrpowergamerbr.loritta.commands.CommandContext
import com.mrpowergamerbr.loritta.tables.Profiles
import com.mrpowergamerbr.loritta.utils.Constants
import com.mrpowergamerbr.loritta.utils.extensions.await
import net.perfectdreams.loritta.api.messages.LorittaReply
import com.mrpowergamerbr.loritta.utils.extensions.isEmote
import com.mrpowergamerbr.loritta.utils.locale.BaseLocale
import com.mrpowergamerbr.loritta.utils.loritta
import com.mrpowergamerbr.loritta.utils.lorittaShards
import com.mrpowergamerbr.loritta.utils.onReactionAddByAuthor
import net.dv8tion.jda.api.EmbedBuilder
import net.perfectdreams.loritta.api.commands.CommandCategory
import net.perfectdreams.loritta.profile.ProfileUtils 
import net.perfectdreams.loritta.utils.Emotes
import org.jetbrains.exposed.sql.update

class DivorceCommand : AbstractCommand("divorce", listOf("divorciar"), CommandCategory.SOCIAL) {
	companion object {
	    const val LOCALE_PREFIX = "commands.social.divorce"
		const val DIVORCE_REACTION_EMOJI = "\uD83D\uDC94"
		const val DIVORCE_EMBED_URI = "https://cdn.discordapp.com/emojis/556524143281963008.png?size=2048"
	}

	override fun getDescription(locale: BaseLocale): String {
		return locale["$LOCALE_PREFIX.description"]
	}

	override suspend fun run(context: CommandContext,locale: BaseLocale) {
		val userProfile = loritta.newSuspendedTransaction { context.lorittaUser.profile }
		val marriage = ProfileUtils.getMarriageInfo(userProfile) ?: return
		val userMarriage = loritta.newSuspendedTransaction { context.lorittaUser.profile.marriage }
		val marriagePartner = marriage.partner                            
		val user = lorittaShards.getUserById(marriagePartner.id) ?: return

		if (userMarriage != null) {
			val message = context.reply(
                    LorittaReply(
                            locale["$LOCALE_PREFIX.prepareToDivorce", Emotes.LORI_CRYING],
                            "\uD83D\uDDA4"
                    ),
                    LorittaReply(
                            locale["$LOCALE_PREFIX.pleaseConfirm", DIVORCE_REACTION_EMOJI],
                            mentionUser = false
                    )
			)

			message.onReactionAddByAuthor(context) {
				if (it.reactionEmote.isEmote(DIVORCE_REACTION_EMOJI)) {
					// depois
					loritta.newSuspendedTransaction {
						Profiles.update({ Profiles.marriage eq userMarriage.id }) {
							it[Profiles.marriage] = null
						}
						userMarriage.delete()
					}

					message.delete().queue()

					context.reply(
                            LorittaReply(
                                    locale["$LOCALE_PREFIX.divorced", Emotes.LORI_HUG]
                            )
					)
					
					try {
						val userPrivateChannel = user.openPrivateChannel().await() ?: return@onReactionAddByAuthor

						userPrivateChannel.sendMessage(
							EmbedBuilder()
								.setTitle(locale["$LOCALE_PREFIX.divorcedTitle"])
								.setDescription(locale["$LOCALE_PREFIX.divorcedDescription", context.userHandle.name])
								.setThumbnail(DIVORCE_EMBED_URI)
								.setColor(Constants.LORITTA_AQUA)
								.build()
						).queue()
					} catch (e: Exception) {}
				}
			}

			message.addReaction(DIVORCE_REACTION_EMOJI).queue()
		} else {
			context.reply(
                    LorittaReply(
                            locale["commands.social.youAreNotMarried", "`${context.config.commandPrefix}casar`", Emotes.LORI_HUG],
                            Constants.ERROR
                    )
			)
		}
	}
}
