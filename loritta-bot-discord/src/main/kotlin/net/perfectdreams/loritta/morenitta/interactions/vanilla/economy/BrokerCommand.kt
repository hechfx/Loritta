package net.perfectdreams.loritta.morenitta.interactions.vanilla.economy

import dev.minn.jda.ktx.messages.InlineEmbed
import dev.minn.jda.ktx.messages.InlineMessage
import dev.minn.jda.ktx.messages.MessageEdit
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.perfectdreams.discordinteraktions.common.utils.field
import net.perfectdreams.loritta.cinnamon.discord.interactions.commands.styled
import net.perfectdreams.loritta.cinnamon.discord.utils.DiscordResourceLimits
import net.perfectdreams.loritta.cinnamon.discord.utils.NumberUtils
import net.perfectdreams.loritta.cinnamon.discord.utils.SonhosUtils
import net.perfectdreams.loritta.cinnamon.discord.utils.SonhosUtils.appendUserHaventGotDailyTodayOrUpsellSonhosBundles
import net.perfectdreams.loritta.cinnamon.discord.utils.toLong
import net.perfectdreams.loritta.cinnamon.emotes.Emotes
import net.perfectdreams.loritta.serializable.BrokerTickerInformation
import net.perfectdreams.loritta.serializable.UserId
import net.perfectdreams.loritta.cinnamon.pudding.services.BovespaBrokerService
import net.perfectdreams.loritta.common.achievements.AchievementType
import net.perfectdreams.loritta.common.commands.CommandCategory
import net.perfectdreams.loritta.common.utils.LorittaBovespaBrokerUtils
import net.perfectdreams.loritta.common.utils.text.TextUtils.shortenAndStripCodeBackticks
import net.perfectdreams.loritta.common.utils.text.TextUtils.shortenWithEllipsis
import net.perfectdreams.loritta.i18n.I18nKeysData
import net.perfectdreams.loritta.morenitta.LorittaBot
import net.perfectdreams.loritta.morenitta.interactions.UnleashedContext
import net.perfectdreams.loritta.morenitta.interactions.commands.*
import net.perfectdreams.loritta.morenitta.interactions.commands.options.ApplicationCommandOptions
import net.perfectdreams.loritta.morenitta.interactions.commands.options.OptionReference
import java.awt.Color
import kotlin.math.abs

class BrokerCommand(val loritta: LorittaBot) : SlashCommandDeclarationWrapper {
    companion object {
        val I18N_PREFIX = I18nKeysData.Commands.Command.Broker

        fun InlineMessage<*>.brokerEmbed(context: UnleashedContext, block: InlineEmbed.() -> Unit) {
            embed {
                author("Loritta's Home Broker")
                // TODO: Move this to an object
                color = Color(23, 62, 163).rgb
                thumbnail = "${context.loritta.config.loritta.website.url}assets/img/loritta_stonks.png"
                footer(context.i18nContext.get(I18N_PREFIX.FooterDataInfo))
                apply(block)
            }
        }

        fun getEmojiStatusForTicker(brokerTickerInformation: BrokerTickerInformation) = if (!LorittaBovespaBrokerUtils.checkIfTickerIsActive(brokerTickerInformation.status))
            Emotes.DoNotDisturb
        else if (LorittaBovespaBrokerUtils.checkIfTickerDataIsStale(brokerTickerInformation.lastUpdatedAt))
            Emotes.Idle
        else Emotes.Online
    }

    override fun command() = slashCommand(I18N_PREFIX.Label, I18N_PREFIX.Description, CommandCategory.ECONOMY) {
        enableLegacyMessageSupport = true
        this.integrationTypes = listOf(Command.IntegrationType.GUILD_INSTALL, Command.IntegrationType.USER_INSTALL)

        val infoExecutor = BrokerInfoExecutor()
        executor = infoExecutor

        subcommand(I18N_PREFIX.Info.Label, I18N_PREFIX.Info.Description) {
            executor = infoExecutor
        }

        subcommand(I18N_PREFIX.Portfolio.Label, I18N_PREFIX.Portfolio.Description) {
            executor = BrokerPortfolioExecutor()
        }

        subcommand(I18N_PREFIX.Stock.Label, I18N_PREFIX.Stock.Description) {
            executor = BrokerStockInfoExecutor()
        }

        subcommand(I18N_PREFIX.Buy.Label, I18N_PREFIX.Buy.Description) {
            executor = BrokerBuyStockExecutor()
        }

        subcommand(I18N_PREFIX.Sell.Label, I18N_PREFIX.Sell.Description) {
            executor = BrokerSellStockExecutor()
        }
    }

    inner class BrokerInfoExecutor : LorittaSlashCommandExecutor(), LorittaLegacyMessageCommandExecutor {
        override suspend fun execute(context: UnleashedContext, args: SlashCommandArguments) {
            context.deferChannelMessage(false) // Defer because this sometimes takes too long

            context.reply(false) {
                brokerEmbed(context) {
                    title = "${Emotes.LoriStonks} ${context.i18nContext.get(I18N_PREFIX.Info.Embed.Title)}"
                    description = context.i18nContext.get(
                        I18N_PREFIX.Info.Embed.Explanation(
                            loriSob = Emotes.LoriSob,
                            tickerOutOfMarket = Emotes.DoNotDisturb,
                            openTime = LorittaBovespaBrokerUtils.TIME_OPEN_DISCORD_TIMESTAMP,
                            closingTime = LorittaBovespaBrokerUtils.TIME_CLOSING_DISCORD_TIMESTAMP,
                            brokerBuyCommandMention = loritta.commandMentions.brokerBuy,
                            brokerSellCommandMention = loritta.commandMentions.brokerSell,
                            brokerPortfolioCommandMention = loritta.commandMentions.brokerPortfolio,
                        )
                    ).joinToString("\n")
                }

                actionRow(selectCompanyCategoryMenu(context, null))
            }
        }

        override suspend fun convertToInteractionsArguments(context: LegacyMessageCommandContext, args: List<String>): Map<OptionReference<*>, Any?> = LorittaLegacyMessageCommandExecutor.NO_ARGS

        private fun selectCompanyCategoryMenu(context: UnleashedContext, selectedCategory: LorittaBovespaBrokerUtils.CompanyCategory?): StringSelectMenu {
            return loritta.interactivityManager.stringSelectMenuForUser(
                context.user,
                {
                    for (category in LorittaBovespaBrokerUtils.CompanyCategory.values()) {
                        addOption(
                            context.i18nContext.get(category.i18nName),
                            category.name,
                            Emoji.fromFormatted(category.emoji.asMention)
                        )
                    }

                    if (selectedCategory != null)
                        setDefaultValues(selectedCategory.name)
                }
            ) { context, values ->
                context.deferAndEditOriginal {
                    val categories = values.map { LorittaBovespaBrokerUtils.CompanyCategory.valueOf(it) }
                    val stockInformations = context.loritta.pudding.bovespaBroker.getAllTickers()

                    MessageEdit {
                        brokerEmbed(context) {
                            title = "${Emotes.LoriStonks} ${context.i18nContext.get(I18N_PREFIX.Info.Embed.Title)}"
                            description = context.i18nContext.get(
                                I18N_PREFIX.Info.Embed.Explanation(
                                    loriSob = Emotes.LoriSob,
                                    tickerOutOfMarket = Emotes.DoNotDisturb,
                                    openTime = LorittaBovespaBrokerUtils.TIME_OPEN_DISCORD_TIMESTAMP,
                                    closingTime = LorittaBovespaBrokerUtils.TIME_CLOSING_DISCORD_TIMESTAMP,
                                    brokerBuyCommandMention = loritta.commandMentions.brokerBuy,
                                    brokerSellCommandMention = loritta.commandMentions.brokerSell,
                                    brokerPortfolioCommandMention = loritta.commandMentions.brokerPortfolio,
                                )
                            ).joinToString("\n")

                            for (stockInformation in stockInformations.sortedBy(BrokerTickerInformation::ticker)) {
                                val stockData =
                                    LorittaBovespaBrokerUtils.trackedTickerCodes.first { it.ticker == stockInformation.ticker }
                                if (stockData.category !in categories)
                                    continue

                                val tickerId = stockInformation.ticker
                                val tickerName = stockData.name
                                val currentPrice = LorittaBovespaBrokerUtils.convertReaisToSonhos(stockInformation.value)
                                val buyingPrice = LorittaBovespaBrokerUtils.convertToBuyingPrice(currentPrice) // Buying price
                                val sellingPrice = LorittaBovespaBrokerUtils.convertToSellingPrice(currentPrice) // Selling price
                                val changePercentage = stockInformation.dailyPriceVariation

                                val fieldTitle =
                                    "`$tickerId` ($tickerName) | ${"%.2f".format(changePercentage)}%"
                                val emojiStatus =
                                    getEmojiStatusForTicker(stockInformation)

                                if (!LorittaBovespaBrokerUtils.checkIfTickerIsActive(stockInformation.status)) {
                                    field {
                                        name = "$emojiStatus $fieldTitle"
                                        value = context.i18nContext.get(
                                            I18N_PREFIX.Info.Embed.PriceBeforeMarketClose(
                                                currentPrice
                                            )
                                        )
                                        inline = true
                                    }
                                } else if (LorittaBovespaBrokerUtils.checkIfTickerDataIsStale(
                                        stockInformation.lastUpdatedAt
                                    )
                                ) {
                                    field {
                                        name = "$emojiStatus $fieldTitle"
                                        value =
                                            """${
                                                context.i18nContext.get(
                                                    I18N_PREFIX.Info.Embed.BuyPrice(
                                                        buyingPrice
                                                    )
                                                )
                                            }
                                |${context.i18nContext.get(I18N_PREFIX.Info.Embed.SellPrice(sellingPrice))}
                            """.trimMargin()
                                        inline = true
                                    }
                                } else {
                                    field {
                                        name = "$emojiStatus $fieldTitle"
                                        value =
                                            """${
                                                context.i18nContext.get(
                                                    I18N_PREFIX.Info.Embed.BuyPrice(
                                                        buyingPrice
                                                    )
                                                )
                                            }
                                |${context.i18nContext.get(I18N_PREFIX.Info.Embed.SellPrice(sellingPrice))}
                            """.trimMargin()
                                        inline = true
                                    }
                                }
                            }
                        }

                        actionRow(selectCompanyCategoryMenu(context, categories.first()))
                    }
                }
            }
        }
    }

    inner class BrokerPortfolioExecutor : LorittaSlashCommandExecutor(), LorittaLegacyMessageCommandExecutor {
        override suspend fun execute(context: UnleashedContext, args: SlashCommandArguments) {
            context.deferChannelMessage(false) // Defer because this sometimes takes too long

            val stockInformations = context.loritta.pudding.bovespaBroker.getAllTickers()
            val userStockAssets = context.loritta.pudding.bovespaBroker.getUserBoughtStocks(context.user.idLong)

            if (userStockAssets.isEmpty())
                context.fail(false) {
                    styled(
                        context.i18nContext.get(I18N_PREFIX.Portfolio.YouDontHaveAnyShares(loritta.commandMentions.brokerInfo, loritta.commandMentions.brokerBuy)),
                        Emotes.LoriSob
                    )
                }

            context.reply(false) {
                brokerEmbed(context) {
                    title = "${Emotes.LoriStonks} ${context.i18nContext.get(I18N_PREFIX.Portfolio.Title)}"

                    val totalStockCount = userStockAssets.sumOf { it.count }
                    val totalStockSum = userStockAssets.sumOf { it.sum }
                    val totalGainsIfSoldEverythingNow = userStockAssets.sumOf { stockAsset ->
                        val tickerInformation = stockInformations.first { it.ticker == stockAsset.ticker }

                        LorittaBovespaBrokerUtils.convertToSellingPrice(
                            LorittaBovespaBrokerUtils.convertReaisToSonhos(
                                tickerInformation.value
                            )
                        ) * stockAsset.count
                    }
                    val diff = totalGainsIfSoldEverythingNow - totalStockSum
                    val totalProfitPercentage = ((totalGainsIfSoldEverythingNow - totalStockSum.toDouble()) / totalStockSum)

                    description = context.i18nContext.get(
                        I18N_PREFIX.Portfolio.YouHaveSharesInYourPortfolio(
                            totalStockCount,
                            totalStockSum,
                            totalGainsIfSoldEverythingNow,
                            diff.let { if (it > 0) "+$it" else it.toString() },
                            totalProfitPercentage
                        )
                    )

                    for (stockAsset in userStockAssets.sortedByDescending {
                        // Sort the portfolio by the stock's profit percentage
                        val stockTicker = it.ticker
                        val stockCount = it.count
                        val stockSum = it.sum
                        val tickerInformation = stockInformations.first { it.ticker == stockTicker }

                        val totalGainsIfSoldNow = LorittaBovespaBrokerUtils.convertToSellingPrice(
                            LorittaBovespaBrokerUtils.convertReaisToSonhos(
                                tickerInformation.value
                            )
                        ) * stockCount

                        ((totalGainsIfSoldNow - stockSum.toDouble()) / stockSum)
                    }) {
                        val (tickerId, stockCount, stockSum, stockAverage) = stockAsset
                        val tickerName = LorittaBovespaBrokerUtils.trackedTickerCodes.first { it.ticker == tickerId }.name
                        val tickerInformation = stockInformations.first { it.ticker == stockAsset.ticker }
                        val currentPrice = LorittaBovespaBrokerUtils.convertReaisToSonhos(tickerInformation.value)
                        val buyingPrice = LorittaBovespaBrokerUtils.convertToBuyingPrice(currentPrice) // Buying price
                        val sellingPrice = LorittaBovespaBrokerUtils.convertToSellingPrice(currentPrice) // Selling price
                        val emojiStatus = getEmojiStatusForTicker(tickerInformation)

                        val totalGainsIfSoldNow = LorittaBovespaBrokerUtils.convertToSellingPrice(
                            LorittaBovespaBrokerUtils.convertReaisToSonhos(
                                tickerInformation.value
                            )
                        ) * stockCount

                        val diff = totalGainsIfSoldNow - stockSum
                        val emojiProfit = when {
                            diff > 0 -> "\uD83D\uDD3C"
                            0 > diff -> "\uD83D\uDD3D"
                            else -> "⏹️"
                        }

                        val changePercentage = tickerInformation.dailyPriceVariation

                        // https://percentage-change-calculator.com/
                        val profitPercentage = ((totalGainsIfSoldNow - stockSum.toDouble()) / stockSum)

                        val youHaveSharesInThisTickerMessage = context.i18nContext.get(
                            I18N_PREFIX.Portfolio.YouHaveSharesInThisTicker(
                                stockCount,
                                stockSum,
                                totalGainsIfSoldNow,
                                diff.let { if (it > 0) "+$it" else it.toString() },
                                profitPercentage
                            )
                        )

                        if (!LorittaBovespaBrokerUtils.checkIfTickerIsActive(tickerInformation.status)) {
                            field(
                                "$emojiStatus$emojiProfit `${tickerId}` ($tickerName) | ${"%.2f".format(changePercentage)}%",
                                """${context.i18nContext.get(I18N_PREFIX.Info.Embed.PriceBeforeMarketClose(currentPrice))}
                                |$youHaveSharesInThisTickerMessage
                            """.trimMargin(),
                                true
                            )
                        } else {
                            field(
                                "$emojiStatus$emojiProfit `${tickerId}` ($tickerName) | ${"%.2f".format(changePercentage)}%",
                                """${context.i18nContext.get(I18N_PREFIX.Info.Embed.BuyPrice(buyingPrice))}
                                |${context.i18nContext.get(I18N_PREFIX.Info.Embed.SellPrice(sellingPrice))}
                                |$youHaveSharesInThisTickerMessage""".trimMargin(),
                                true
                            )
                        }
                    }
                }
            }
        }

        override suspend fun convertToInteractionsArguments(context: LegacyMessageCommandContext, args: List<String>): Map<OptionReference<*>, Any?> = LorittaLegacyMessageCommandExecutor.NO_ARGS
    }

    inner class BrokerStockInfoExecutor : LorittaSlashCommandExecutor(), LorittaLegacyMessageCommandExecutor {
        inner class Options : ApplicationCommandOptions() {
            val ticker = string("ticker", I18N_PREFIX.Stock.Options.Ticker.Text) {
                LorittaBovespaBrokerUtils.trackedTickerCodes.map { Pair(it.ticker, it.name) }.forEach { (tickerId, tickerTitle) ->
                    choice("$tickerTitle ($tickerId)", tickerId.lowercase())
                }
            }
        }

        override val options = Options()

        override suspend fun execute(context: UnleashedContext, args: SlashCommandArguments) {
            context.deferChannelMessage(true) // Defer because this sometimes takes too long

            val tickerId = args[options.ticker].uppercase()

            // This should *never* happen because the values are validated on Discord side BUT who knows
            if (tickerId !in LorittaBovespaBrokerUtils.validStocksCodes)
                context.fail(true, context.i18nContext.get(I18N_PREFIX.ThatIsNotAnValidStockTicker(loritta.commandMentions.brokerInfo)))

            val stockInformation = context.loritta.pudding.bovespaBroker.getTicker(tickerId)

            val stockAsset = context.loritta.pudding.bovespaBroker.getUserBoughtStocks(context.user.id.toLong())
                .firstOrNull { it.ticker == tickerId }

            context.reply(true) {
                brokerEmbed(context) {
                    title = "${Emotes.LoriStonks} ${context.i18nContext.get(I18N_PREFIX.Stock.Embed.Title)}"

                    // This is just like the "/broker portfolio" command
                    // There is two alternatives however: If the user has stock, the output will be the same as the "/broker portfolio" command
                    // If not, it will be just the buy/sell price
                    val tickerInformation = stockInformation
                    val tickerName = LorittaBovespaBrokerUtils.trackedTickerCodes.first { it.ticker == tickerInformation.ticker }.name
                    val currentPrice = LorittaBovespaBrokerUtils.convertReaisToSonhos(tickerInformation.value)
                    val buyingPrice = LorittaBovespaBrokerUtils.convertToBuyingPrice(currentPrice) // Buying price
                    val sellingPrice = LorittaBovespaBrokerUtils.convertToSellingPrice(currentPrice) // Selling price
                    val changePercentage = tickerInformation.dailyPriceVariation
                    val emojiStatus = getEmojiStatusForTicker(tickerInformation)

                    if (stockAsset != null) {
                        val (tickerId, stockCount, stockSum, stockAverage) = stockAsset

                        val totalGainsIfSoldNow = LorittaBovespaBrokerUtils.convertToSellingPrice(
                            LorittaBovespaBrokerUtils.convertReaisToSonhos(
                                tickerInformation.value
                            )
                        ) * stockCount

                        val diff = totalGainsIfSoldNow - stockSum
                        val emojiProfit = when {
                            diff > 0 -> "\uD83D\uDD3C"
                            0 > diff -> "\uD83D\uDD3D"
                            else -> "⏹️"
                        }

                        // https://percentage-change-calculator.com/
                        val profitPercentage = ((totalGainsIfSoldNow - stockSum.toDouble()) / stockSum)

                        val youHaveSharesInThisTickerMessage = context.i18nContext.get(
                            I18N_PREFIX.Portfolio.YouHaveSharesInThisTicker(
                                stockCount,
                                stockSum,
                                totalGainsIfSoldNow,
                                diff.let { if (it > 0) "+$it" else it.toString() },
                                profitPercentage
                            )
                        )

                        if (!LorittaBovespaBrokerUtils.checkIfTickerIsActive(tickerInformation.status)) {
                            field(
                                "$emojiStatus$emojiProfit `${tickerId}` ($tickerName) | ${"%.2f".format(changePercentage)}%",
                                """${context.i18nContext.get(I18N_PREFIX.Info.Embed.PriceBeforeMarketClose(currentPrice))}
                                |$youHaveSharesInThisTickerMessage
                            """.trimMargin()
                            )
                        } else {
                            field(
                                "$emojiStatus$emojiProfit `${tickerId}` ($tickerName) | ${"%.2f".format(changePercentage)}%",
                                """${context.i18nContext.get(I18N_PREFIX.Info.Embed.BuyPrice(buyingPrice))}
                                |${context.i18nContext.get(I18N_PREFIX.Info.Embed.SellPrice(sellingPrice))}
                                |$youHaveSharesInThisTickerMessage""".trimMargin()
                            )
                        }
                    } else {
                        if (!LorittaBovespaBrokerUtils.checkIfTickerIsActive(tickerInformation.status)) {
                            field(
                                "$emojiStatus `${tickerId}` ($tickerName) | ${"%.2f".format(changePercentage)}%",
                                context.i18nContext.get(I18N_PREFIX.Info.Embed.PriceBeforeMarketClose(currentPrice))
                                    .trimMargin()
                            )
                        } else {
                            field(
                                "$emojiStatus `${tickerId}` ($tickerName) | ${"%.2f".format(changePercentage)}%",
                                """${context.i18nContext.get(I18N_PREFIX.Info.Embed.BuyPrice(buyingPrice))}
                                |${context.i18nContext.get(I18N_PREFIX.Info.Embed.SellPrice(sellingPrice))}""".trimMargin()
                            )
                        }
                    }
                }
            }
        }

        override suspend fun convertToInteractionsArguments(context: LegacyMessageCommandContext, args: List<String>): Map<OptionReference<*>, Any?>? {
            if (args.isEmpty()) {
                context.explain()
                return null
            }

            return mapOf(options.ticker to args[0])
        }
    }

    inner class BrokerBuyStockExecutor : LorittaSlashCommandExecutor(), LorittaLegacyMessageCommandExecutor {
        inner class Options : ApplicationCommandOptions() {
            val ticker = string("ticker", I18N_PREFIX.Stock.Options.Ticker.Text) {
                autocomplete {
                    val focusedOptionValue = it.event.focusedOption.value

                    val results = LorittaBovespaBrokerUtils.trackedTickerCodes.filter {
                        it.ticker.startsWith(focusedOptionValue, true)
                    }

                    return@autocomplete results.map {
                        "${it.name} (${it.ticker})" to it.ticker.uppercase()
                    }.take(25).toMap()
                }
            }

            val quantity = optionalString("quantity", I18N_PREFIX.Buy.Options.Quantity.Text) {
                autocomplete {
                    val currentInput = it.event.focusedOption.value

                    val ticker = it.event.getOption("ticker")?.asString?.uppercase() ?: return@autocomplete mapOf()
                    // Not a valid ticker, bye!
                    if (ticker !in LorittaBovespaBrokerUtils.validStocksCodes)
                        return@autocomplete mapOf()

                    val tickerInfo = loritta.pudding.bovespaBroker.getTicker(ticker)

                    val quantity = NumberUtils.convertShortenedNumberToLong(it.i18nContext, currentInput) ?: return@autocomplete mapOf(
                        it.i18nContext.get(
                            I18nKeysData.Commands.InvalidNumber(currentInput)
                        ).shortenAndStripCodeBackticks(DiscordResourceLimits.Command.Options.Description.Length) to "invalid_number"
                    )

                    return@autocomplete mapOf(
                        it.i18nContext.get(
                            I18N_PREFIX.SharesCountWithPrice(
                                quantity,
                                quantity * tickerInfo.value
                            )
                        ).shortenWithEllipsis(DiscordResourceLimits.Command.Options.Description.Length) to quantity.toString()
                    )
                }
            }
        }

        override val options = Options()

        override suspend fun execute(context: UnleashedContext, args: SlashCommandArguments) {
            if (SonhosUtils.checkIfEconomyIsDisabled(context))
                return

            context.deferChannelMessage(true) // Defer because this sometimes takes too long

            val tickerId = args[options.ticker].uppercase()
            val quantityAsString = args[options.quantity] ?: "1"

            // This should *never* happen because the values are validated on Discord side BUT who knows
            if (tickerId !in LorittaBovespaBrokerUtils.validStocksCodes)
                context.fail(true, context.i18nContext.get(I18N_PREFIX.ThatIsNotAnValidStockTicker(loritta.commandMentions.brokerInfo)))

            val quantity = NumberUtils.convertShortenedNumberToLong(context.i18nContext, quantityAsString) ?: context.fail(
                true,
                context.i18nContext.get(
                    I18nKeysData.Commands.InvalidNumber(quantityAsString)
                )
            )

            val (_, boughtQuantity, value) = try {
                context.loritta.pudding.bovespaBroker.buyStockShares(
                    context.user.idLong,
                    tickerId,
                    quantity
                )
            } catch (e: BovespaBrokerService.TransactionActionWithLessThanOneShareException) {
                context.fail(
                    true,
                    context.i18nContext.get(
                        when (quantity) {
                            0L -> I18N_PREFIX.Buy.TryingToBuyZeroShares
                            else -> I18N_PREFIX.Buy.TryingToBuyLessThanZeroShares
                        }
                    )
                )
            } catch (e: BovespaBrokerService.StaleTickerDataException) {
                context.fail(true, context.i18nContext.get(I18N_PREFIX.StaleTickerData))
            } catch (e: BovespaBrokerService.OutOfSessionException) {
                context.fail(
                    true,
                    context.i18nContext.get(
                        I18N_PREFIX.StockMarketClosed(
                            LorittaBovespaBrokerUtils.TIME_OPEN_DISCORD_TIMESTAMP,
                            LorittaBovespaBrokerUtils.TIME_CLOSING_DISCORD_TIMESTAMP
                        )
                    )
                )
            } catch (e: BovespaBrokerService.NotEnoughSonhosException) {
                context.reply(true) {
                    styled(
                        context.i18nContext.get(SonhosUtils.insufficientSonhos(e.userSonhos, e.howMuch)),
                        Emotes.LoriSob
                    )

                    appendUserHaventGotDailyTodayOrUpsellSonhosBundles(
                        context.loritta,
                        context.i18nContext,
                        UserId(context.user.idLong),
                        "lori-broker",
                        "buy-shares-not-enough-sonhos"
                    )
                }
                return
            } catch (e: BovespaBrokerService.TooManySharesException) {
                context.fail(
                    true,
                    context.i18nContext.get(
                        I18N_PREFIX.Buy.TooManyShares(
                            LorittaBovespaBrokerUtils.MAX_STOCK_SHARES_PER_USER
                        )
                    )
                )
            }

            context.reply(true) {
                styled(
                    context.i18nContext.get(
                        I18N_PREFIX.Buy.SuccessfullyBought(
                            sharesCount = boughtQuantity,
                            ticker = tickerId,
                            price = value,
                            brokerPortfolioCommandMention = loritta.commandMentions.brokerPortfolio
                        )
                    ),
                    Emotes.LoriRich
                )
            }
        }

        override suspend fun convertToInteractionsArguments(context: LegacyMessageCommandContext, args: List<String>): Map<OptionReference<*>, Any?>? {
            if (args.isEmpty()) {
                context.explain()
                return null
            }

            return mapOf(options.ticker to args[0])
        }
    }

    inner class BrokerSellStockExecutor : LorittaSlashCommandExecutor(), LorittaLegacyMessageCommandExecutor {
        inner class Options : ApplicationCommandOptions() {
            val ticker = string("ticker", I18N_PREFIX.Stock.Options.Ticker.Text) {
                autocomplete {
                    val focusedOptionValue = it.event.focusedOption.value

                    val userBoughtStocks = loritta.pudding.bovespaBroker.getUserBoughtStocks(it.event.user.idLong)
                        .map { it.ticker }
                        .toSet()

                    val results = LorittaBovespaBrokerUtils.trackedTickerCodes.filter {
                        it.ticker in userBoughtStocks && it.ticker.startsWith(focusedOptionValue, true)
                    }

                    return@autocomplete results.map {
                        "${it.name} (${it.ticker})" to it.ticker
                    }.take(25).toMap()
                }
            }

            val quantity = optionalString("quantity", I18N_PREFIX.Buy.Options.Quantity.Text) {
                autocomplete {
                    val currentInput = it.event.focusedOption.value

                    val ticker = it.event.getOption("ticker")?.asString?.uppercase() ?: return@autocomplete mapOf()
                    // Not a valid ticker, bye!
                    if (ticker !in LorittaBovespaBrokerUtils.validStocksCodes)
                        return@autocomplete mapOf()

                    val tickerInfo = loritta.pudding.bovespaBroker.getTicker(ticker)

                    val quantity = NumberUtils.convertShortenedNumberToLong(it.i18nContext, currentInput) ?: return@autocomplete mapOf(
                        it.i18nContext.get(
                            I18nKeysData.Commands.InvalidNumber(currentInput)
                        ).shortenAndStripCodeBackticks(DiscordResourceLimits.Command.Options.Description.Length) to "invalid_number"
                    )

                    return@autocomplete mapOf(
                        it.i18nContext.get(
                            I18N_PREFIX.SharesCountWithPrice(
                                quantity,
                                quantity * tickerInfo.value
                            )
                        ).shortenWithEllipsis(DiscordResourceLimits.Command.Options.Description.Length) to quantity.toString()
                    )
                }
            }
        }

        override val options = Options()

        override suspend fun execute(context: UnleashedContext, args: SlashCommandArguments) {
            if (SonhosUtils.checkIfEconomyIsDisabled(context))
                return

            context.deferChannelMessage(true) // Defer because this sometimes takes too long

            val tickerId = args[options.ticker].uppercase()
            val quantityAsString = args[options.quantity] ?: "1"

            // This should *never* happen because the values are validated on Discord side BUT who knows
            if (tickerId !in LorittaBovespaBrokerUtils.validStocksCodes)
                context.fail(true, context.i18nContext.get(I18N_PREFIX.ThatIsNotAnValidStockTicker(loritta.commandMentions.brokerInfo)))

            val quantity = if (quantityAsString == "all") {
                context.loritta.pudding.bovespaBroker.getUserBoughtStocks(context.user.idLong)
                    .firstOrNull { it.ticker == tickerId }
                    ?.count ?: context.fail(
                    true,
                    context.i18nContext.get(
                        I18N_PREFIX.Sell.YouDontHaveAnySharesInThatTicker(
                            tickerId
                        )
                    )
                )
            } else {
                NumberUtils.convertShortenedNumberToLong(context.i18nContext, quantityAsString) ?: context.fail(
                    true,
                    context.i18nContext.get(
                        I18nKeysData.Commands.InvalidNumber(quantityAsString)
                    )
                )
            }

            val (_, soldQuantity, earnings, profit) = try {
                context.loritta.pudding.bovespaBroker.sellStockShares(
                    context.user.idLong,
                    tickerId,
                    quantity
                )
            } catch (e: BovespaBrokerService.TransactionActionWithLessThanOneShareException) {
                context.fail(
                    true,
                    context.i18nContext.get(
                        when (quantity) {
                            0L -> I18N_PREFIX.Sell.TryingToSellZeroShares
                            else -> I18N_PREFIX.Sell.TryingToSellLessThanZeroShares
                        }
                    )
                )
            } catch (e: BovespaBrokerService.StaleTickerDataException) {
                context.fail(true, context.i18nContext.get(I18N_PREFIX.StaleTickerData))
            } catch (e: BovespaBrokerService.OutOfSessionException) {
                context.fail(
                    true,
                    context.i18nContext.get(
                        I18N_PREFIX.StockMarketClosed(
                            LorittaBovespaBrokerUtils.TIME_OPEN_DISCORD_TIMESTAMP,
                            LorittaBovespaBrokerUtils.TIME_CLOSING_DISCORD_TIMESTAMP
                        )
                    )
                )
            } catch (e: BovespaBrokerService.NotEnoughSharesException) {
                context.fail(
                    true,
                    context.i18nContext.get(
                        I18N_PREFIX.Sell.YouDontHaveEnoughStocks(
                            e.currentBoughtSharesCount,
                            tickerId
                        )
                    )
                )
            }

            val isNeutralProfit = profit == 0L
            val isPositiveProfit = profit > 0L
            val isNegativeProfit = !isNeutralProfit && !isPositiveProfit

            context.reply(true) {
                styled(
                    context.i18nContext.get(
                        I18N_PREFIX.Sell.SuccessfullySold(
                            soldQuantity,
                            tickerId,
                            when {
                                isNeutralProfit -> {
                                    context.i18nContext.get(
                                        I18N_PREFIX.Sell.SuccessfullySoldNeutral
                                    )
                                }

                                isPositiveProfit -> {
                                    context.i18nContext.get(
                                        I18N_PREFIX.Sell.SuccessfullySoldProfit(
                                            abs(earnings),
                                            abs(profit)
                                        )
                                    )
                                }

                                else -> {
                                    context.i18nContext.get(
                                        I18N_PREFIX.Sell.SuccessfullySoldLoss(
                                            abs(earnings),
                                            abs(profit),
                                            (loritta.commandMentions.brokerPortfolio)
                                        )
                                    )
                                }
                            }
                        )
                    ),
                    when {
                        profit == 0L -> Emotes.LoriShrug
                        profit > 0L -> Emotes.LoriRich
                        else -> Emotes.LoriSob
                    }
                )
            }

            if (isPositiveProfit)
                context.giveAchievementAndNotify(AchievementType.STONKS, ephemeral = true)
            if (isNegativeProfit)
                context.giveAchievementAndNotify(AchievementType.NOT_STONKS, ephemeral = true)
        }

        override suspend fun convertToInteractionsArguments(context: LegacyMessageCommandContext, args: List<String>): Map<OptionReference<*>, Any?>? {
            if (args.isEmpty()) {
                context.explain()
                return null
            }

            return mapOf(options.ticker to args[0])
        }
    }
}