package net.perfectdreams.loritta.cinnamon.showtime.backend.routes

import io.ktor.server.application.*
import io.ktor.server.html.*
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put
import net.perfectdreams.dokyo.RoutePath
import net.perfectdreams.i18nhelper.core.I18nContext
import net.perfectdreams.loritta.cinnamon.pudding.tables.DonationKeys
import net.perfectdreams.loritta.cinnamon.showtime.backend.ShowtimeBackend
import net.perfectdreams.loritta.cinnamon.showtime.backend.utils.userTheme
import net.perfectdreams.loritta.cinnamon.showtime.backend.views.DonateView
import net.perfectdreams.loritta.common.locale.BaseLocale
import net.perfectdreams.loritta.temmiewebsession.lorittaSession
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select

class DonateRoute(val showtime: ShowtimeBackend) : LocalizedRoute(showtime, RoutePath.PREMIUM) {
    override suspend fun onLocalizedRequest(call: ApplicationCall, locale: BaseLocale, i18nContext: I18nContext) {
        val userIdentification = call.lorittaSession.getUserIdentification(showtime.rootConfig.discord.applicationId, showtime.rootConfig.discord.clientSecret, call)

        val keys = buildJsonArray {
            if (userIdentification != null) {
                val donationKeys = loritta.pudding.transaction {
                    // Pegar keys ativas
                    DonationKeys.select {
                        (DonationKeys.expiresAt greaterEq System.currentTimeMillis()) and (DonationKeys.userId eq userIdentification.id.toLong())
                    }.toMutableList()
                }

                for (donationKey in donationKeys) {
                    addJsonObject {
                        put("id", donationKey[DonationKeys.id].value)
                        put("value", donationKey[DonationKeys.value])
                        put("expiresAt", donationKey[DonationKeys.expiresAt])
                    }
                }
            }
        }

        call.respondHtml(
            block = DonateView(
                loritta,
                call.request.userTheme,
                locale,
                i18nContext,
                "/donate",
                userIdentification,
                keys
            ).generateHtml()
        )
    }
}