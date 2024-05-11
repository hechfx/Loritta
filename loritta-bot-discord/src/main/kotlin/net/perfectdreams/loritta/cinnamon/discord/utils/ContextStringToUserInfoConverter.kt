package net.perfectdreams.loritta.cinnamon.discord.utils

import dev.kord.common.entity.Snowflake
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.perfectdreams.loritta.cinnamon.discord.interactions.commands.ApplicationCommandContext
import net.perfectdreams.loritta.morenitta.LorittaBot
import net.perfectdreams.loritta.morenitta.interactions.InteractionContext
import net.perfectdreams.loritta.morenitta.interactions.UnleashedContext
import net.perfectdreams.loritta.morenitta.utils.extensions.await
import net.perfectdreams.loritta.morenitta.utils.isValidSnowflake
import net.perfectdreams.loritta.serializable.CachedUserInfo
import net.perfectdreams.loritta.serializable.UserId

/**
 * Converts a String, using a CommandContext, to a CachedUserInfo object
 */
object ContextStringToUserInfoConverter {
    suspend fun convert(context: ApplicationCommandContext, input: String): CachedUserInfo? {
        if (input.startsWith("<@") && input.endsWith(">")) {
            // Is a mention... maybe?
            val userId = input.removePrefix("<@")
                .removePrefix("!")
                .removeSuffix(">")
                .toLongOrNull() ?: return null // If the input is not a long, then return the input

            val user = context.interaKTionsContext.interactionData.resolved?.users?.get(Snowflake(userId)) ?: return null // If there isn't any matching user, then return null
            return CachedUserInfo(
                UserId(user.id.value),
                user.username,
                user.discriminator,
                null,
                user.data.avatar
            )
        }

        val snowflake = try {
            Snowflake(input)
        } catch (e: NumberFormatException) {
            null
        }

        // If the snowflake is not null, then it *may* be a user ID!
        if (snowflake != null) {
            val cachedUserInfo = context.loritta.getCachedUserInfo(UserId(snowflake.value))
            if (cachedUserInfo != null)
                return cachedUserInfo
        }

        return null
    }

    suspend fun convert(context: UnleashedContext, input: String): net.perfectdreams.loritta.morenitta.utils.CachedUserInfo? {
        var userId: Long? = null

        val inputAsLong = input.toLongOrNull()
        if (inputAsLong != null)
            userId = inputAsLong // The input is already a long value! Use it then

        // Is this a mention?
        if (input.startsWith("<@") && input.endsWith(">")) {
            val userIdFromMention = input.removePrefix("<@")
                .removePrefix("!")
                .removeSuffix(">")
                .toLongOrNull()

            if (userIdFromMention != null) {
                userId = userIdFromMention
            }
        }

        // We couldn't find any IDs... :(
        if (userId == null)
            return null

        val userFromMentions = context.mentions.users.firstOrNull { it.idLong == userId }
        if (userFromMentions != null) {
            return net.perfectdreams.loritta.morenitta.utils.CachedUserInfo(
                userFromMentions.idLong,
                userFromMentions.name,
                userFromMentions.discriminator,
                userFromMentions.avatarId
            )
        }

        return context.loritta.lorittaShards.retrieveUserInfoById(userId)
    }
}