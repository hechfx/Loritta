package net.perfectdreams.loritta.legacy.website.routes.api.v1.loritta

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.salomonbrys.kotson.jsonObject
import net.perfectdreams.loritta.legacy.utils.Constants
import net.perfectdreams.loritta.legacy.website.LorittaWebsite
import io.ktor.server.application.*
import net.perfectdreams.loritta.legacy.platform.discord.LorittaDiscord
import net.perfectdreams.loritta.legacy.utils.Emotes
import net.perfectdreams.loritta.legacy.utils.HoconUtils.decodeFromFile
import net.perfectdreams.loritta.legacy.website.routes.api.v1.RequiresAPIAuthenticationRoute
import net.perfectdreams.loritta.legacy.website.utils.extensions.respondJson
import java.io.File

class GetLorittaActionRoute(loritta: LorittaDiscord) : RequiresAPIAuthenticationRoute(loritta, "/api/v1/loritta/action/{actionType}") {
	override suspend fun onAuthenticatedRequest(call: ApplicationCall) {
		val actionType = call.parameters["actionType"]

		when (actionType) {
			"emotes" -> {
				Emotes.emoteManager?.loadEmotes()
			}
			"locales" -> {
				net.perfectdreams.loritta.legacy.utils.loritta.localeManager.loadLocales()
				net.perfectdreams.loritta.legacy.utils.loritta.loadLegacyLocales()
			}
			"website" -> {
				LorittaWebsite.ENGINE.templateCache.invalidateAll()
			}
			"websitekt" -> {
				net.perfectdreams.loritta.legacy.website.LorittaWebsite.INSTANCE.pathCache.clear()
			}
			"config" -> {
				val file = File(System.getProperty("conf") ?: "./loritta.conf")
				net.perfectdreams.loritta.legacy.utils.loritta.config = Constants.HOCON.decodeFromFile(file)
				val file2 = File(System.getProperty("discordConf") ?: "./discord.conf")
				net.perfectdreams.loritta.legacy.utils.loritta.discordConfig = Constants.HOCON.decodeFromFile(file2)
			}
		}

		call.respondJson(jsonObject())
	}
}