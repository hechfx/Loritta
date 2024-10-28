package net.perfectdreams.loritta.morenitta.profile.badges

import net.perfectdreams.loritta.cinnamon.pudding.Pudding
import net.perfectdreams.loritta.cinnamon.pudding.tables.CollectedCandies
import net.perfectdreams.loritta.morenitta.dao.Profile
import net.perfectdreams.loritta.morenitta.profile.Badge
import net.perfectdreams.loritta.morenitta.profile.ProfileDesignManager
import net.perfectdreams.loritta.morenitta.profile.ProfileUserInfoData
import org.jetbrains.exposed.sql.select
import java.util.*

class Halloween2024Badge(val pudding: Pudding) : Badge.LorittaBadge(
	UUID.fromString("3b74665b-d30c-4cc6-8465-0873ec3dc3b6"),
	ProfileDesignManager.I18N_BADGES_PREFIX.Halloween2019.Title,
	ProfileDesignManager.I18N_BADGES_PREFIX.Halloween2019.Description,
	"halloween2019.png",
	100
) {
	override suspend fun checkIfUserDeservesBadge(user: ProfileUserInfoData, profile: Profile, mutualGuilds: Set<Long>): Boolean {
		return pudding.transaction {
			CollectedCandies.select {
				CollectedCandies.user eq profile.id.value
			}.count() >= 400L
		}
	}
}