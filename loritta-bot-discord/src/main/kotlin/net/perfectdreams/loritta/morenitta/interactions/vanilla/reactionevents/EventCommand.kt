package net.perfectdreams.loritta.morenitta.interactions.vanilla.reactionevents

import dev.minn.jda.ktx.messages.InlineMessage
import dev.minn.jda.ktx.messages.MessageEdit
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.TimeFormat
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder
import net.perfectdreams.loritta.cinnamon.discord.interactions.commands.styled
import net.perfectdreams.loritta.cinnamon.discord.interactions.vanilla.social.declarations.XpCommand
import net.perfectdreams.loritta.cinnamon.discord.utils.images.ImageFormatType
import net.perfectdreams.loritta.cinnamon.discord.utils.images.ImageUtils.toByteArray
import net.perfectdreams.loritta.cinnamon.emotes.Emotes
import net.perfectdreams.loritta.cinnamon.pudding.tables.Profiles
import net.perfectdreams.loritta.cinnamon.pudding.tables.reactionevents.CollectedReactionEventPoints
import net.perfectdreams.loritta.cinnamon.pudding.tables.reactionevents.CraftedReactionEventItems
import net.perfectdreams.loritta.cinnamon.pudding.tables.reactionevents.ReactionEventDrops
import net.perfectdreams.loritta.cinnamon.pudding.tables.reactionevents.ReactionEventPlayers
import net.perfectdreams.loritta.cinnamon.pudding.utils.SimpleSonhosTransactionsLogUtils
import net.perfectdreams.loritta.common.commands.CommandCategory
import net.perfectdreams.loritta.common.utils.LorittaColors
import net.perfectdreams.loritta.common.utils.TransactionType
import net.perfectdreams.loritta.i18n.I18nKeysData
import net.perfectdreams.loritta.morenitta.LorittaBot
import net.perfectdreams.loritta.morenitta.interactions.UnleashedButton
import net.perfectdreams.loritta.morenitta.interactions.UnleashedContext
import net.perfectdreams.loritta.morenitta.interactions.commands.LorittaSlashCommandExecutor
import net.perfectdreams.loritta.morenitta.interactions.commands.SlashCommandArguments
import net.perfectdreams.loritta.morenitta.interactions.commands.SlashCommandDeclarationWrapper
import net.perfectdreams.loritta.morenitta.interactions.commands.options.ApplicationCommandOptions
import net.perfectdreams.loritta.morenitta.interactions.commands.slashCommand
import net.perfectdreams.loritta.morenitta.interactions.vanilla.economy.SonhosCommand
import net.perfectdreams.loritta.morenitta.reactionevents.ReactionEvent
import net.perfectdreams.loritta.morenitta.reactionevents.ReactionEventReward
import net.perfectdreams.loritta.morenitta.reactionevents.ReactionEventsAttributes
import net.perfectdreams.loritta.morenitta.utils.RankingGenerator
import net.perfectdreams.loritta.morenitta.utils.extensions.await
import net.perfectdreams.loritta.morenitta.utils.extensions.toJDA
import net.perfectdreams.loritta.serializable.StoredReactionEventSonhosTransaction
import org.jetbrains.exposed.sql.*
import java.time.Instant
import java.util.*
import kotlin.math.ceil

class EventCommand(val loritta: LorittaBot) : SlashCommandDeclarationWrapper {
    private val I18N_PREFIX = I18nKeysData.Commands.Command.Reactionevents

    override fun command() = slashCommand(I18N_PREFIX.Label, I18N_PREFIX.Description, CommandCategory.FUN, UUID.fromString("a1a03b03-b23d-43b8-93f5-0c714307220d")) {
        subcommand(I18N_PREFIX.Join.Label, I18N_PREFIX.Description, UUID.fromString("a9df2f98-5e35-4716-a8bc-906e566cb5ed")) {
            executor = JoinEventExecutor(loritta)
        }

        subcommand(I18N_PREFIX.Stats.Label, I18N_PREFIX.Stats.Description, UUID.fromString("011500c4-9955-49c2-aa72-925f576d6b0c")) {
            executor = StatsEventExecutor(loritta)
        }

        subcommand(I18N_PREFIX.Inventory.Label, I18N_PREFIX.Inventory.Description, UUID.fromString("b3766a24-5b2d-4fea-8c9d-7c02b047601b")) {
            executor = InventoryEventExecutor(loritta)
        }

        subcommand(I18N_PREFIX.Rank.Label, I18N_PREFIX.Rank.Description, UUID.fromString("39a1ba04-ad37-45cb-a3ba-7497cb1d28e7")) {
            executor = StatsRankExecutor()
        }
    }

    class JoinEventExecutor(private val loritta: LorittaBot) : LorittaSlashCommandExecutor() {
        override suspend fun execute(context: UnleashedContext, args: SlashCommandArguments) {
            context.deferChannelMessage(false)

            val now = Instant.now()

            // Get the current active event
            val activeEvent = ReactionEventsAttributes.getActiveEvent(now)

            if (activeEvent == null) {
                context.reply(true) {
                    styled(
                        "Nenhum evento ativo...",
                        Emotes.LoriSob
                    )
                }
                return
            }

            // Attempt to join the active event
            val result = loritta.transaction {
                val alreadyJoined = ReactionEventPlayers.selectAll()
                    .where {
                        ReactionEventPlayers.userId eq context.user.idLong and (ReactionEventPlayers.event eq activeEvent.internalId)
                    }.count() != 0L

                if (!alreadyJoined) {
                    ReactionEventPlayers.insert {
                        it[ReactionEventPlayers.event] = activeEvent.internalId
                        it[ReactionEventPlayers.userId] = context.user.idLong
                        it[ReactionEventPlayers.joinedAt] = now
                    }
                    return@transaction JoinEventResult.JoinedEvent
                }

                return@transaction JoinEventResult.YouHaveAlreadyJoinedTheEvent
            }

            when (result) {
                JoinEventResult.EventUnavailable -> {
                    context.reply(false) {
                        styled(
                            "Nenhum evento ativo...",
                            Emotes.LoriSob
                        )
                    }
                }

                JoinEventResult.YouHaveAlreadyJoinedTheEvent -> {
                    context.reply(false) {
                        styled(
                            "Você já está participando do evento!",
                            Emotes.LoriShrug
                        )
                    }
                }

                JoinEventResult.JoinedEvent -> {
                    context.reply(false) {
                        activeEvent.createJoinMessage(context).invoke(this)
                    }
                }
            }
        }

        sealed class JoinEventResult {
            data object JoinedEvent : JoinEventResult()
            data object YouHaveAlreadyJoinedTheEvent : JoinEventResult()
            data object EventUnavailable : JoinEventResult()
        }
    }

    class StatsEventExecutor(val loritta: LorittaBot) : LorittaSlashCommandExecutor() {
        override suspend fun execute(context: UnleashedContext, args: SlashCommandArguments) {
            context.deferChannelMessage(false)

            val now = Instant.now()
            val activeEvent = ReactionEventsAttributes.getActiveEvent(now)
            if (activeEvent == null) {
                context.reply(false) {
                    styled(
                        "Nenhum evento ativo...",
                        Emotes.LoriSob
                    )
                }
                return
            }

            val result = loritta.newSuspendedTransaction {
                val playerData = ReactionEventPlayers.selectAll()
                    .where {
                        ReactionEventPlayers.userId eq context.user.idLong and (ReactionEventPlayers.event eq activeEvent.internalId)
                    }.firstOrNull()

                if (playerData == null)
                    return@newSuspendedTransaction Result.HasNotJoinedTheEvent

                val count = CraftedReactionEventItems.selectAll()
                    .where {
                        CraftedReactionEventItems.user eq playerData[ReactionEventPlayers.id] and (CraftedReactionEventItems.event eq activeEvent.internalId)
                    }.count()

                return@newSuspendedTransaction Result.Success(count)
            }

            when (result) {
                Result.HasNotJoinedTheEvent -> {
                    context.reply(false) {
                        styled("Você precisa entrar no evento antes de poder ver as suas estatísticas!")
                    }
                }
                is Result.Success -> {
                    context.reply(false) {
                        styled(activeEvent.createCraftedXItemsMessage(loritta, context.i18nContext, result.collectedPoints, loritta.commandMentions.eventInventory))

                        styled("O evento irá acabar às ${TimeFormat.DATE_TIME_LONG.format(activeEvent.endsAt)}!")
                        styled("Lembre-se que os itens apenas aparecem em servidores que possuem mais de mil membros!")
                        styled("**Itens a serem coletados:** ${activeEvent.reactionSets.joinToString(" ") { loritta.emojiManager.get(it.reaction).toJDA().formatted }}")

                        for (reward in activeEvent.rewards.sortedBy { it.requiredPoints }) {
                            when (reward) {
                                is ReactionEventReward.SonhosReward -> {
                                    styled(
                                        buildString {
                                            append("**[${activeEvent.createShortCraftedItemMessage(context.i18nContext, reward.requiredPoints)}]")
                                            if (reward.prestige) {
                                                append(" (Prestígio \uD83D\uDD25)")
                                            }
                                            append("** ")
                                            append("${reward.sonhos} sonhos")
                                        },
                                        prefix = if (result.collectedPoints >= reward.requiredPoints) "✅" else "❌"
                                    )
                                }
                                is ReactionEventReward.BadgeReward -> {
                                    styled(
                                        buildString {
                                            append("**[${activeEvent.createShortCraftedItemMessage(context.i18nContext, reward.requiredPoints)}]")
                                            if (reward.prestige) {
                                                append(" (Prestígio \uD83D\uDD25)")
                                            }
                                            append("** ")
                                            append("Badge para o seu ${loritta.commandMentions.profileView}")
                                        },
                                        prefix = if (result.collectedPoints >= reward.requiredPoints) "✅" else "❌"
                                    )
                                }

                                /*
                                is LorittaEaster2023Event.EventReward.PremiumKeyReward -> {
                                    styled(
                                        buildString {
                                            append("**[${reward.requiredPoints} cestas]")
                                            if (reward.prestige) {
                                                append(" (Prestígio \uD83D\uDD25)")
                                            }
                                            append("** ")
                                            append("Todas as vantagens premiums (R\$ 99,99) por três meses")
                                        },
                                        prefix = if (collectedPoints >= reward.requiredPoints) "✅" else "❌"
                                    )
                                }

                                is LorittaEaster2023Event.EventReward.ProfileDesignReward -> {
                                    styled(
                                        buildString {
                                            append("**[${reward.requiredPoints} cestas]")
                                            if (reward.prestige) {
                                                append(" (Prestígio \uD83D\uDD25)")
                                            }
                                            append("** ")
                                            append("Design de Perfil ${context.locale["profileDesigns.${reward.profileName}.title"]}")
                                        },
                                        prefix = if (collectedPoints >= reward.requiredPoints) "✅" else "❌"
                                    )
                                } */
                            }
                        }

                        styled("*Itens apenas aparecem em mensagens de membros que estão participando do evento. Você precisa de um servidor com membros participando? Então entre na Comunidade da Loritta! https://discord.gg/loritta *")
                    }
                }
            }
        }

        sealed class Result {
            data class Success(val collectedPoints: Long) : Result()
            data object HasNotJoinedTheEvent : Result()
        }
    }

    class InventoryEventExecutor(private val loritta: LorittaBot) : LorittaSlashCommandExecutor() {
        private suspend fun response(context: UnleashedContext, target: suspend (InlineMessage<*>.() -> (Unit)) -> (Unit)) {
            val now = Instant.now()
            val activeEvent = ReactionEventsAttributes.getActiveEvent(now)

            if (activeEvent == null) {
                context.reply(false) {
                    styled(
                        "Nenhum evento ativo...",
                        Emotes.LoriSob
                    )
                }
                return
            }

            val reactionSetIdCount = ReactionEventDrops.reactionSetId.count()

            val result = loritta.transaction {
                val playerData = ReactionEventPlayers.selectAll()
                    .where {
                        ReactionEventPlayers.userId eq context.user.idLong and (ReactionEventPlayers.event eq activeEvent.internalId)
                    }.firstOrNull()

                if (playerData == null)
                    return@transaction Result.HasNotJoinedEvent

                val pointCounts = CollectedReactionEventPoints.innerJoin(ReactionEventDrops)
                    .select(ReactionEventDrops.reactionSetId, reactionSetIdCount)
                    .where {
                        CollectedReactionEventPoints.user eq playerData[ReactionEventPlayers.id] and (CollectedReactionEventPoints.associatedWithCraft.isNull())
                    }
                    .groupBy(ReactionEventDrops.reactionSetId)
                    .associate { Pair(it[ReactionEventDrops.reactionSetId], it[reactionSetIdCount]) }

                val basketCount = CraftedReactionEventItems.selectAll()
                    .where {
                        CraftedReactionEventItems.user eq playerData[ReactionEventPlayers.id]
                    }.count()

                return@transaction Result.Success(
                    pointCounts,
                    basketCount
                )
            }

            when (result) {
                Result.HasNotJoinedEvent -> {
                    context.reply(false) {
                        styled("Você precisa entrar no evento antes de poder ver as suas estatísticas!")
                    }
                    return
                }
                is Result.Success -> {
                    val activeCraft = activeEvent.getCurrentActiveCraft(context.user, result.craftedThings)
                    val button = activeEvent.createCraftItemButtonMessage(context.i18nContext)

                    val craftButton = UnleashedButton.of(
                        ButtonStyle.PRIMARY,
                        button.text,
                        loritta.emojiManager.get(button.emoji)
                    )

                    var canCraft = true
                    for (eventReactionSet in activeEvent.reactionSets) {
                        val howMuchDoWeHave = result.counts[eventReactionSet.reactionSetId] ?: 0
                        val howMuchIsRequired = activeCraft[eventReactionSet.reactionSetId] ?: 0
                        val missing = howMuchIsRequired - howMuchDoWeHave

                        if (missing > 0) {
                            canCraft = false
                            break
                        }
                    }

                    val btn = if (!canCraft) {
                        craftButton.asDisabled()
                    } else {
                        loritta.interactivityManager.buttonForUser(
                            context.user,
                            ButtonStyle.PRIMARY,
                            button.text,
                            {
                                loriEmoji = loritta.emojiManager.get(button.emoji)
                            }
                        ) { context ->
                            context.deferChannelMessage(false)

                            // Attempt to create a basket
                            val basketCreationResult = loritta.newSuspendedTransaction {
                                val playerData = ReactionEventPlayers.selectAll()
                                    .where {
                                        ReactionEventPlayers.userId eq context.user.idLong and (ReactionEventPlayers.event eq activeEvent.internalId)
                                    }.firstOrNull()

                                if (playerData == null)
                                    error("playerData is null but should never be null!")

                                val basketCount = CraftedReactionEventItems.selectAll()
                                    .where {
                                        CraftedReactionEventItems.user eq playerData[ReactionEventPlayers.id] and (CraftedReactionEventItems.event eq activeEvent.internalId)
                                    }.count()

                                val activeCraft = activeEvent.getCurrentActiveCraft(context.user, result.craftedThings)

                                val points = CollectedReactionEventPoints.innerJoin(ReactionEventDrops)
                                    .select(CollectedReactionEventPoints.id, ReactionEventDrops.reactionSetId)
                                    .where {
                                        CollectedReactionEventPoints.user eq playerData[ReactionEventPlayers.id] and (CollectedReactionEventPoints.associatedWithCraft.isNull()) and (ReactionEventDrops.event eq activeEvent.internalId)
                                    }
                                    .toList()
                                //         .groupBy(ReactionEventDrops.reactionSetId)
                                //         .associate { Pair(it[ReactionEventDrops.reactionSetId], it[reactionSetIdCount]) }

                                val itemsToBeUpdated = mutableListOf<ResultRow>()

                                for (requiredItem in activeCraft) {
                                    val itemsOfThisType = points.filter { it[ReactionEventDrops.reactionSetId] == requiredItem.key }
                                    val howMuchDoWeHave = itemsOfThisType.size

                                    if (requiredItem.value > howMuchDoWeHave)
                                        return@newSuspendedTransaction CraftCreationResult.InsufficientItems
                                    else
                                        itemsToBeUpdated.addAll(itemsOfThisType.take(requiredItem.value))
                                }

                                // Okay, we do have enough ITEMS! Let's create a :sparkles: basket :sparkles:
                                val basket = CraftedReactionEventItems.insertAndGetId {
                                    it[CraftedReactionEventItems.user] = playerData[ReactionEventPlayers.id]
                                    it[CraftedReactionEventItems.event] = activeEvent.internalId
                                    it[CraftedReactionEventItems.createdAt] = Instant.now()
                                }

                                // And then attempt to update every item ID to reference the newly created basket
                                CollectedReactionEventPoints.update({
                                    CollectedReactionEventPoints.id inList itemsToBeUpdated.map { it[CollectedReactionEventPoints.id] }
                                }) {
                                    it[CollectedReactionEventPoints.associatedWithCraft] = basket
                                }

                                // How many baskets do they now have?
                                val newBasketCount = (basketCount + 1).toInt()
                                for (reward in activeEvent.rewards) {
                                    if (reward.requiredPoints != newBasketCount)
                                        continue

                                    when (reward) {
                                        is ReactionEventReward.SonhosReward -> {
                                            // Cinnamon transactions log
                                            SimpleSonhosTransactionsLogUtils.insert(
                                                context.user.idLong,
                                                Instant.now(),
                                                TransactionType.EVENTS,
                                                reward.sonhos,
                                                StoredReactionEventSonhosTransaction(
                                                    activeEvent.internalId,
                                                    reward.requiredPoints
                                                )
                                            )

                                            Profiles.update({ Profiles.id eq context.user.idLong }) {
                                                with(SqlExpressionBuilder) {
                                                    it[money] = money + reward.sonhos
                                                }
                                            }
                                        }

                                        // No need to do anything for badges :3
                                        is ReactionEventReward.BadgeReward -> {}
                                    }
                                }

                                return@newSuspendedTransaction CraftCreationResult.Success
                            }

                            when (basketCreationResult) {
                                CraftCreationResult.InsufficientItems -> {
                                    context.reply(false) {
                                        styled(
                                            activeEvent.createYouDontHaveEnoughItemsMessage(context.i18nContext),
                                            Emotes.LoriSob
                                        )
                                    }
                                }

                                CraftCreationResult.Success -> {
                                    val message = activeEvent.createYouCraftedAItemMessage(context.i18nContext)

                                    context.reply(false) {
                                        styled(
                                            message.text,
                                            loritta.emojiManager.get(message.emoji)
                                        )
                                    }

                                    response(context) {
                                        context.event.message.editMessage(
                                            MessageEdit {
                                                it.invoke(this)
                                            }
                                        ).await()
                                    }
                                }
                            }
                        }
                    }

                    target.invoke {
                        embed {
                            description = buildString {
                                appendLine(activeEvent.createHowManyCraftedItemsYouHaveMessage(context.i18nContext, result.craftedThings, loritta.commandMentions.eventStats))
                                appendLine()
                                appendLine("**${activeEvent.createItemsInYourInventoryMessage(context.i18nContext)}**")
                                for (eventReactionSet in activeEvent.reactionSets) {
                                    val howMuch = result.counts[eventReactionSet.reactionSetId] ?: 0
                                    appendLine("${loritta.emojiManager.get(eventReactionSet.reaction).asMention} ${howMuch}x")
                                }

                                appendLine()
                                appendLine("**${activeEvent.createYourNextCraftIngredientsAreMessage(context.i18nContext)}**")
                                for (eventReactionSet in activeEvent.reactionSets) {
                                    val howMuchDoWeHave = result.counts[eventReactionSet.reactionSetId] ?: 0
                                    val howMuchIsRequired = activeCraft[eventReactionSet.reactionSetId] ?: 0

                                    if (howMuchIsRequired != 0) {
                                        val missing = howMuchIsRequired - howMuchDoWeHave
                                        append("${loritta.emojiManager.get(eventReactionSet.reaction).asMention} ${howMuchIsRequired}x")
                                        if (missing > 0) {
                                            append(" (falta ${missing}x)")
                                        }
                                        appendLine()
                                    }
                                }

                                color = LorittaColors.LorittaAqua.rgb

                                // Discord, for some reason, isn't rendering the non @1280w version, so let's scale the image down to help poor discord
                                // image = "https://stuff.loritta.website/loritta-easter-2023@1280w.png"
                            }
                        }

                        actionRow(btn)
                    }
                }
            }
        }

        override suspend fun execute(context: UnleashedContext, args: SlashCommandArguments) {
            context.deferChannelMessage(false)

            response(context) {
                context.reply(false) {
                    it.invoke(this)
                }
            }
        }

        sealed class Result {
            data class Success(
                val counts: Map<UUID, Long>,
                val craftedThings: Long
            ) : Result()
            data object HasNotJoinedEvent : Result()
        }

        sealed class CraftCreationResult {
            object Success : CraftCreationResult()
            object InsufficientItems : CraftCreationResult()
        }
    }

    inner class StatsRankExecutor : LorittaSlashCommandExecutor() {
        inner class Options : ApplicationCommandOptions() {
            val page = optionalLong("page", XpCommand.XP_RANK_I18N_PREFIX.Options.Page.Text) /* {
                // range = RankingGenerator.VALID_RANKING_PAGES
            } */
        }

        override val options = Options()

        override suspend fun execute(context: UnleashedContext, args: SlashCommandArguments) {
            val page = (args[options.page]?.minus(1)) ?: 0

            context.deferChannelMessage(false)
            val now = Instant.now()

            val activeEvent = ReactionEventsAttributes.getActiveEvent(now)

            if (activeEvent == null) {
                context.reply(true) {
                    styled(
                        "Nenhum evento ativo...",
                        Emotes.LoriSob
                    )
                }
                return
            }

            context.reply(false) {
                createRankMessage(context, page, activeEvent)()
            }
        }

        private suspend fun createRankMessage(context: UnleashedContext, page: Long, event: ReactionEvent): suspend InlineMessage<*>.() -> (Unit) = {
            styled(
                context.i18nContext.get(SonhosCommand.TRANSACTIONS_I18N_PREFIX.Page(page + 1)),
                Emotes.LoriReading
            )

            val countColumn = CraftedReactionEventItems.user.count()

            val (totalCount, profiles) = loritta.pudding.transaction {
                val totalCount = CraftedReactionEventItems
                    .select(CraftedReactionEventItems.user)
                    .where {
                        CraftedReactionEventItems.event eq event.internalId
                    }
                    .groupBy(CraftedReactionEventItems.user)
                    .count()

                val profilesInTheQuery =
                    CraftedReactionEventItems
                        .innerJoin(ReactionEventPlayers)
                        .select(ReactionEventPlayers.userId, countColumn)
                        .where {
                            CraftedReactionEventItems.event eq event.internalId
                        }
                        .groupBy(ReactionEventPlayers.userId)
                        .orderBy(countColumn to SortOrder.DESC)
                        .limit(5, page * 5)
                        .toList()

                Pair(totalCount, profilesInTheQuery)
            }

            // Calculates the max page
            val maxPage = ceil(totalCount / 5.0)
            val maxPageZeroIndexed = maxPage - 1

            files += FileUpload.fromData(
                RankingGenerator.generateRanking(
                    loritta,
                    page * 5,
                    "Evento",
                    null,
                    profiles.map {
                        val presentesCount = it[countColumn]

                        RankingGenerator.UserRankInformation(
                            it[ReactionEventPlayers.userId]
                            ,
                            event.createShortCraftedItemMessage(context.i18nContext, presentesCount.toInt())
                        )
                    }
                ) {
                    null
                }.toByteArray(ImageFormatType.PNG).inputStream(),
                "rank.png"
            )

            actionRow(
                loritta.interactivityManager.buttonForUser(
                    context.user,
                    ButtonStyle.PRIMARY,
                    builder = {
                        loriEmoji = Emotes.ChevronLeft
                        disabled = page !in RankingGenerator.VALID_RANKING_PAGES
                    }
                ) {
                    it.deferEdit()
                        .editOriginal(
                            InlineMessage(MessageEditBuilder())
                                .apply {
                                    createRankMessage(
                                        context,
                                        page - 1,
                                        event
                                    )()
                                }.build()
                        )
                        .await()
                },
                loritta.interactivityManager.buttonForUser(
                    context.user,
                    ButtonStyle.PRIMARY,
                    builder = {
                        loriEmoji = Emotes.ChevronRight
                        disabled = page + 2 !in RankingGenerator.VALID_RANKING_PAGES || page >= maxPageZeroIndexed
                    }
                ) {
                    it.deferEdit()
                        .editOriginal(
                            InlineMessage(MessageEditBuilder())
                                .apply {
                                    createRankMessage(
                                        context,
                                        page + 1,
                                        event
                                    )()
                                }.build()
                        )
                        .await()
                },
            )
        }
    }
}