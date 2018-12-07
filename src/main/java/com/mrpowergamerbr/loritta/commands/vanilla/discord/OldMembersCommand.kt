package com.mrpowergamerbr.loritta.commands.vanilla.discord

import com.mrpowergamerbr.loritta.commands.AbstractCommand
import com.mrpowergamerbr.loritta.commands.CommandCategory
import com.mrpowergamerbr.loritta.commands.CommandContext
import com.mrpowergamerbr.loritta.utils.Constants
import com.mrpowergamerbr.loritta.utils.Emotes
import com.mrpowergamerbr.loritta.utils.extensions.await
import com.mrpowergamerbr.loritta.utils.extensions.edit
import com.mrpowergamerbr.loritta.utils.locale.BaseLocale
import com.mrpowergamerbr.loritta.utils.onReactionAddByAuthor
import com.mrpowergamerbr.loritta.utils.stripCodeMarks
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.Message

class OldMembersCommand : AbstractCommand("oldmembers", listOf("membrosantigos", "oldusers", "usuáriosantigos", "usuariosantigos"), CommandCategory.DISCORD) {
	override fun getDescription(locale: BaseLocale): String {
		return locale.get("USERINFO_DESCRIPTION")
	}

	override fun canUseInPrivateChannel(): Boolean {
		return false
	}

	override suspend fun run(context: CommandContext, locale: BaseLocale) {
		showOldMembers(null, context, 0)
	}

	suspend fun showOldMembers(message: Message?, context: CommandContext, page: Int) {
		val guild = context.guild

		val sortedMembers = guild.members.sortedBy { it.joinDate }

		val sortedMembersInCurrentPage = sortedMembers.subList(page * 10, Math.min((page + 1) * 10, guild.members.size))

		val maxPage = guild.members.size / 10
		val userCurrentPage = sortedMembers.indexOf(context.handle) / 10

		val embed = EmbedBuilder().apply {
			setColor(Constants.DISCORD_BLURPLE)
			setTitle("\uD83C\uDF1F As pessoas mais antigas no ${guild.name}")

			for ((index, member) in sortedMembersInCurrentPage.withIndex()) {
				val ownerEmote = when {
					member?.isOwner == true -> "\uD83D\uDC51"
					else -> ""
				}

				val userEmote = when {
					member == context.handle -> "\uD83D\uDC81"
					else -> ""
				}

				val typeEmote = when {
					member.user.isBot -> Emotes.DISCORD_BOT_TAG
					else -> Emotes.DISCORD_WUMPUS_BASIC
				}

				val statusEmote = when (member?.onlineStatus) {
					OnlineStatus.ONLINE -> Emotes.DISCORD_ONLINE
					OnlineStatus.IDLE -> Emotes.DISCORD_IDLE
					OnlineStatus.DO_NOT_DISTURB -> Emotes.DISCORD_DO_NOT_DISTURB
					else -> Emotes.DISCORD_OFFLINE
				}

				appendDescription("`${1 + index + (page * 20)}º` $ownerEmote$userEmote$typeEmote$statusEmote `${member.user.name.stripCodeMarks()}#${member.user.discriminator}`\n")
				setFooter("Página ${page + 1} de ${maxPage + 1} | Você está na página ${userCurrentPage + 1}!", null)
			}
		}

		val _message = message?.edit(context.getAsMention(true), embed.build()) ?: context.sendMessage(context.getAsMention(true), embed.build()) // phew, agora finalmente poderemos enviar o embed!
		_message.onReactionAddByAuthor(context) {
			if (it.reactionEmote.name == "⏪") {
				showOldMembers(_message, context, 0)
				return@onReactionAddByAuthor
			}
			if (it.reactionEmote.name == "◀") {
				showOldMembers(_message, context, page - 1)
				return@onReactionAddByAuthor
			}
			if (it.reactionEmote.name == "▶") {
				showOldMembers(_message, context, page + 1)
				return@onReactionAddByAuthor
			}
			if (it.reactionEmote.name == "⏩") {
				showOldMembers(_message, context, maxPage)
				return@onReactionAddByAuthor
			}
			if (it.reactionEmote.name == "\uD83D\uDC81") {
				showOldMembers(_message, context, userCurrentPage)
				return@onReactionAddByAuthor
			}
		}

		if (page != 0) {
			_message.addReaction("⏪").await()
			_message.addReaction("◀").await()
		}
		if (maxPage != page) {
			_message.addReaction("▶").await()
			_message.addReaction("⏩").await()
		}
		_message.addReaction("\uD83D\uDC81").await()
	}
}