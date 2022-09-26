package net.perfectdreams.loritta.legacy.website.routes

import io.ktor.server.application.*
import net.perfectdreams.loritta.common.locale.BaseLocale
import net.perfectdreams.loritta.legacy.platform.discord.LorittaDiscord
import net.perfectdreams.loritta.legacy.website.utils.extensions.redirect

class FanArtsRoute(loritta: LorittaDiscord) : LocalizedRoute(loritta, "/fanarts") {
    override val isMainClusterOnlyRoute = true

    override suspend fun onLocalizedRequest(call: ApplicationCall, locale: BaseLocale) {
        redirect("https://fanarts.perfectdreams.net/", true)
    }
}