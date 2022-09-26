package net.perfectdreams.loritta.legacy.commands.vanilla.magic

import mu.KotlinLogging
import net.perfectdreams.loritta.common.api.commands.CommandContext
import net.perfectdreams.loritta.common.messages.LorittaReply
import net.perfectdreams.loritta.common.commands.CommandCategory
import net.perfectdreams.loritta.legacy.platform.discord.LorittaDiscord
import net.perfectdreams.loritta.legacy.platform.discord.legacy.commands.DiscordAbstractCommandBase

class LoriToolsCommand(loritta: LorittaDiscord) : DiscordAbstractCommandBase(loritta, listOf("loritools"), net.perfectdreams.loritta.common.commands.CommandCategory.MAGIC) {
	companion object {
		private val logger = KotlinLogging.logger {}
	}

	override fun command() = create {
		description { "Ferramentas de Administração da Loritta" }
		onlyOwner = true

		executesDiscord {
			logger.info { "Executing Lori Tools!" }

			val allExecutors = listOf(
				RegisterYouTubeChannelExecutor,
				PurgeInactiveGuildsExecutor,
				PurgeInactiveUsersExecutor,
				PurgeInactiveGuildUsersExecutor,
				SetSelfBackgroundExecutor,
				SetSelfProfileDesignExecutor,
				GenerateDailyShopExecutor,
				PriceCorrectionExecutor,
				LoriBanIpExecutor,
				LoriUnbanIpExecutor,
				DeleteAccountDataExecutor,
				ChargebackRunExecutor,
				EnableBoostExecutor,
				DisableBoostExecutor
			)

			allExecutors.forEach {
				val result = it.executes().invoke(this)

				if (result) {
					logger.info { "Executed ${it::class.simpleName} Executor!" }
					return@executesDiscord
				}
			}

			val replies = mutableListOf<LorittaReply>()

			allExecutors.forEach {
				replies.add(
					LorittaReply(
						"`${it.args}`",
						mentionUser = false
					)
				)
			}

			reply(replies)
		}
	}

	interface LoriToolsExecutor {
		val args: String

		fun executes(): (suspend CommandContext.() -> (Boolean))
	}
}