package net.perfectdreams.loritta.morenitta.website.routes.dashboard.configure

import io.ktor.server.application.*
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.perfectdreams.i18nhelper.core.I18nContext
import net.perfectdreams.loritta.common.locale.BaseLocale
import net.perfectdreams.loritta.common.utils.LorittaPermission
import net.perfectdreams.loritta.common.utils.UserPremiumPlans
import net.perfectdreams.loritta.morenitta.LorittaBot
import net.perfectdreams.loritta.morenitta.dao.ServerConfig
import net.perfectdreams.loritta.morenitta.utils.LorittaUser
import net.perfectdreams.loritta.morenitta.website.evaluate
import net.perfectdreams.loritta.morenitta.website.routes.dashboard.RequiresGuildAuthLocalizedDashboardRoute
import net.perfectdreams.loritta.temmiewebsession.LorittaJsonWebSession
import net.perfectdreams.loritta.morenitta.website.utils.extensions.legacyVariables
import net.perfectdreams.loritta.morenitta.website.utils.extensions.respondHtml
import net.perfectdreams.loritta.morenitta.website.views.dashboard.guild.LegacyPebbleGuildDashboardRawHtmlView
import net.perfectdreams.loritta.serializable.ColorTheme
import net.perfectdreams.temmiediscordauth.TemmieDiscordAuth
import java.util.*
import kotlin.collections.set

class ConfigurePermissionsRoute(loritta: LorittaBot) : RequiresGuildAuthLocalizedDashboardRoute(loritta, "/configure/permissions") {
	override suspend fun onDashboardGuildAuthenticatedRequest(call: ApplicationCall, locale: BaseLocale, i18nContext: I18nContext, discordAuth: TemmieDiscordAuth, userIdentification: LorittaJsonWebSession.UserIdentification, guild: Guild, serverConfig: ServerConfig, colorTheme: ColorTheme) {	val variables = call.legacyVariables(loritta, locale)

		variables["saveType"] = "permissions"
		val roleConfig = mutableMapOf<Role, MutableMap<String, Boolean>>()
		val rolePermissions = LorittaUser.loadGuildRolesLorittaPermissions(loritta, serverConfig, guild)

		for (role in guild.roles) {
			val permissions = rolePermissions[role.idLong] ?: EnumSet.noneOf(LorittaPermission::class.java)
			val permissionMap = mutableMapOf<String, Boolean>()

			for (permission in LorittaPermission.values()) {
				permissionMap[permission.internalName] = permissions.contains(permission)
			}

			roleConfig[role] = permissionMap
		}

		variables["roleConfigs"] = roleConfig

		call.respondHtml(
			LegacyPebbleGuildDashboardRawHtmlView(
				loritta,
				i18nContext,
				locale,
				getPathWithoutLocale(call),
				loritta.getLegacyLocaleById(locale.id),
				userIdentification,
				UserPremiumPlans.getPlanFromValue(loritta.getActiveMoneyFromDonations(userIdentification.id.toLong())),
				colorTheme,
				guild,
				"Painel de Controle",
				evaluate("permissions.html", variables),
				"permissions"
			).generateHtml()
		)
	}
}