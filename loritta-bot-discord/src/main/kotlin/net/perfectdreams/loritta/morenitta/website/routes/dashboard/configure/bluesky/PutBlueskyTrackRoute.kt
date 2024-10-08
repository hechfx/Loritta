package net.perfectdreams.loritta.morenitta.website.routes.dashboard.configure.bluesky

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.util.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.dv8tion.jda.api.entities.Guild
import net.perfectdreams.i18nhelper.core.I18nContext
import net.perfectdreams.loritta.cinnamon.pudding.tables.servers.moduleconfigs.TrackedBlueskyAccounts
import net.perfectdreams.loritta.common.locale.BaseLocale
import net.perfectdreams.loritta.common.utils.UserPremiumPlans
import net.perfectdreams.loritta.i18n.I18nKeysData
import net.perfectdreams.loritta.morenitta.LorittaBot
import net.perfectdreams.loritta.morenitta.dao.ServerConfig
import net.perfectdreams.loritta.morenitta.website.routes.dashboard.RequiresGuildAuthLocalizedDashboardRoute
import net.perfectdreams.loritta.morenitta.website.utils.EmbeddedSpicyModalUtils
import net.perfectdreams.loritta.morenitta.website.utils.EmbeddedSpicyModalUtils.headerHXPushURL
import net.perfectdreams.loritta.morenitta.website.utils.EmbeddedSpicyModalUtils.headerHXTrigger
import net.perfectdreams.loritta.morenitta.website.utils.EmbeddedSpicyModalUtils.respondBodyAsHXTrigger
import net.perfectdreams.loritta.morenitta.website.utils.extensions.respondHtml
import net.perfectdreams.loritta.morenitta.website.utils.extensions.respondJson
import net.perfectdreams.loritta.morenitta.website.views.dashboard.guild.bluesky.GuildBlueskyView
import net.perfectdreams.loritta.morenitta.website.views.dashboard.guild.bluesky.GuildConfigureBlueskyProfileView
import net.perfectdreams.loritta.serializable.ColorTheme
import net.perfectdreams.loritta.serializable.EmbeddedSpicyToast
import net.perfectdreams.loritta.temmiewebsession.LorittaJsonWebSession
import net.perfectdreams.temmiediscordauth.TemmieDiscordAuth
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.time.Instant

class PutBlueskyTrackRoute(loritta: LorittaBot) : RequiresGuildAuthLocalizedDashboardRoute(loritta, "/configure/bluesky/tracks") {
	override suspend fun onDashboardGuildAuthenticatedRequest(call: ApplicationCall, locale: BaseLocale, i18nContext: I18nContext, discordAuth: TemmieDiscordAuth, userIdentification: LorittaJsonWebSession.UserIdentification, guild: Guild, serverConfig: ServerConfig, colorTheme: ColorTheme) {
		// This is the route that adds a NEW instance to the configuration
		val postParams = call.receiveParameters()

		// Revalidate again just to be sure that the user isn't adding an invalid did
		val http = loritta.http.get("https://public.api.bsky.app/xrpc/app.bsky.actor.getProfile") {
			parameter("actor", postParams.getOrFail("did"))
		}

		if (http.status == HttpStatusCode.BadRequest) {
			call.respondBodyAsHXTrigger(HttpStatusCode.BadRequest) {
				playSoundEffect = "config-error"
				showSpicyToast(
					EmbeddedSpicyToast.Type.WARN,
					"Conta não existe!"
				)
			}
			return
		}

		val json = Json {
			ignoreUnknownKeys = true
		}

		val textStuff = http.bodyAsText(Charsets.UTF_8)
		val profile = json.decodeFromString<BlueskyProfile>(textStuff)

		val insertedRow = loritta.transaction {
			val count = TrackedBlueskyAccounts.selectAll()
				.where {
					TrackedBlueskyAccounts.guildId eq guild.idLong
				}.count()
			if (count >= GuildBlueskyView.MAX_TRACKED_BLUESKY_ACCOUNTS) {
				return@transaction null
			}

			val now = Instant.now()

			TrackedBlueskyAccounts.insert {
				it[TrackedBlueskyAccounts.repo] = profile.did
				it[TrackedBlueskyAccounts.guildId] = guild.idLong
				it[TrackedBlueskyAccounts.channelId] = postParams.getOrFail("channelId").toLong()
				it[TrackedBlueskyAccounts.message] = postParams.getOrFail("message")
				it[TrackedBlueskyAccounts.addedAt] = now
				it[TrackedBlueskyAccounts.editedAt] = now
			}
		}

		if (insertedRow == null) {
			call.response.header("SpicyMorenitta-Use-Response-As-HXTrigger", "true")
			call.respondJson(
				buildJsonObject {
					put("playSoundEffect", "config-error")
					put(
						"showSpicyToast",
						EmbeddedSpicyModalUtils.encodeURIComponent(
							Json.encodeToString(
								EmbeddedSpicyToast(
									EmbeddedSpicyToast.Type.WARN,
									"Você já tem muitas contas conectadas!",
									null
								)
							)
						)
					)
				}.toString(),
				status = HttpStatusCode.BadRequest
			)
			return
		}

		call.response.headerHXPushURL("/${i18nContext.get(I18nKeysData.Website.LocalePathId)}/guild/${guild.idLong}/configure/bluesky/tracks/${insertedRow[TrackedBlueskyAccounts.id].value}")
		call.response.headerHXTrigger {
			playSoundEffect = "config-saved"
		}

		call.respondHtml(
			GuildConfigureBlueskyProfileView(
				loritta.newWebsite!!,
				i18nContext,
				locale,
				getPathWithoutLocale(call),
				loritta.getLegacyLocaleById(locale.id),
				userIdentification,
				UserPremiumPlans.getPlanFromValue(loritta.getActiveMoneyFromDonations(userIdentification.id.toLong())),
				colorTheme,
				guild,
				"bluesky",
				insertedRow[TrackedBlueskyAccounts.id].value,
				profile,
				GuildConfigureBlueskyProfileView.BlueskyTrackSettings(
					insertedRow[TrackedBlueskyAccounts.channelId],
					insertedRow[TrackedBlueskyAccounts.message]
				)
			).generateHtml()
		)
	}
}