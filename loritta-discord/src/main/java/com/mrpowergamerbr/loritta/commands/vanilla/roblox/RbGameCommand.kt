package com.mrpowergamerbr.loritta.commands.vanilla.roblox

import com.github.kevinsawicki.http.HttpRequest
import com.mrpowergamerbr.loritta.commands.AbstractCommand
import com.mrpowergamerbr.loritta.commands.CommandContext
import com.mrpowergamerbr.loritta.utils.Constants
import com.mrpowergamerbr.loritta.utils.encodeToUrl
import com.mrpowergamerbr.loritta.utils.locale.BaseLocale
import com.mrpowergamerbr.loritta.utils.locale.LocaleKeyData
import com.mrpowergamerbr.loritta.utils.substringIfNeeded
import net.dv8tion.jda.api.EmbedBuilder
import net.perfectdreams.loritta.api.commands.CommandCategory
import net.perfectdreams.loritta.api.messages.LorittaReply
import org.jsoup.Jsoup

class RbGameCommand : AbstractCommand("rbgame", listOf("rbjogo", "rbgameinfo"), CommandCategory.ROBLOX) {
	override fun getDescriptionKey() = LocaleKeyData("commands.roblox.rbgame.description")

	override suspend fun run(context: CommandContext,locale: BaseLocale) {
		if (context.args.isNotEmpty()) {
			val query = context.args.joinToString(" ")

			val url = "https://www.roblox.com/games/moreresultscached?StartRows=0&MaxRows=40&IsUserLoggedIn=false&NumberOfColumns=8&IsInHorizontalScrollMode=false&DeviceTypeId=1&Keyword=${query.encodeToUrl()}&AdSpan=56&AdAlignment=0&v=2&IsSecure=&UseFakeResults=False&SuggestedCorrection=none&SuggestionKeyword=&SuggestionReplacedKeyword="

			val body = HttpRequest.get(url)
					.body()

			val document = Jsoup.parse(body)
			val gameCardLink = document.getElementsByClass("game-card-link").firstOrNull()

			if (gameCardLink == null) {
				context.reply(
                        LorittaReply(
                                message = locale["commands.roblox.rbgame.couldntFind", query],
                                prefix = Constants.ERROR
                        )
				)
				return
			}

			val gameUrl = gameCardLink.attr("href")

			val gameBody = HttpRequest.get(gameUrl)
					.body()

			val embed = EmbedBuilder()
					.setColor(Constants.ROBLOX_RED)

			val gameDocument = Jsoup.parse(gameBody)

			val placeId = gameDocument.getElementById("game-detail-page").attr("data-place-id")
			val gameName = gameDocument.getElementsByClass("game-name").text()
			val gameAuthor = gameDocument.getElementsByClass("game-creator")[0].getElementsByClass("text-name").text()
			val gameDescription = gameDocument.getElementsByClass("game-description")[0].text()
			val favoriteCount = gameDocument.getElementsByClass("game-favorite-count")[0].text()
			val thumbnail = gameDocument.getElementsByClass("carousel-thumb").firstOrNull()

			if (thumbnail != null) {
				embed.setImage(thumbnail.attr("src"))
			}

			val gameStats = gameDocument.getElementsByClass("game-stat")

			val playing = gameStats[0].getElementsByClass("text-lead").text()
			// val favoriteCountFromPage = gameStats[1].getElementsByClass("text-lead").text()
			val visits = gameStats[2].getElementsByClass("text-lead").text()
			val created = gameStats[3].getElementsByClass("text-lead").text()
			val updated = gameStats[4].getElementsByClass("text-lead").text()
			val maxplayers = gameStats[5].getElementsByClass("text-lead").text()
			val genre = gameStats[6].getElementsByClass("text-lead").text()
			// val allowedgear = gameStats[7].getElementsByClass("text-lead").text()

			val voteBody = HttpRequest.get("https://www.roblox.com/games/votingservice/$placeId")
					.body()

			val voteDocument = Jsoup.parse(voteBody)

			val voteSection = voteDocument.getElementById("voting-section")
			val upvotes = voteSection.attr("data-total-up-votes")
			val downvotes = voteSection.attr("data-total-down-votes")

			embed.setTitle("<:roblox_logo:412576693803286528> $gameName", gameUrl)
			embed.addField("\uD83D\uDCBB ${locale["commands.roblox.rbuser.robloxId"]}", placeId, true)
			embed.addField("<:starstruck:540988091117076481> ${locale["commands.roblox.rbgame.favorites"]}", favoriteCount, true)
			embed.addField("\uD83D\uDC4D ${locale["commands.roblox.rbgame.likes"]}", upvotes, true)
			embed.addField("\uD83D\uDC4E ${locale["commands.roblox.rbgame.dislikes"]}", downvotes, true)
			embed.addField("\uD83C\uDFAE ${locale["commands.roblox.rbgame.playing"]}", playing, true)
			embed.addField("\uD83D\uDC3E ${locale["commands.roblox.rbuser.visits"]}", visits, true)
			embed.addField("\uD83C\uDF1F ${locale["commands.roblox.rbgame.createdAt"]}", created, true)
			embed.addField("✨ ${locale["commands.roblox.rbgame.lastUpdated"]}", updated, true)
			embed.addField("⛔ ${locale["commands.roblox.rbgame.maxPlayers"]}", maxplayers, true)
			embed.addField("\uD83C\uDFB2 ${locale["commands.roblox.rbgame.genre"]}", genre, true)

			embed.setAuthor(gameAuthor)
			embed.setDescription(gameDescription.substringIfNeeded(0 until 250))

			context.sendMessage(context.getAsMention(true), embed.build())
		} else {
			context.explain()
		}
	}
}