package net.perfectdreams.loritta.morenitta.interactions.vanilla.discord

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Icon
import net.dv8tion.jda.api.entities.sticker.StickerSnowflake
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.exceptions.RateLimitedException
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.requests.ErrorResponse
import net.perfectdreams.loritta.cinnamon.discord.interactions.commands.styled
import net.perfectdreams.loritta.cinnamon.discord.utils.DiscordResourceLimits
import net.perfectdreams.loritta.cinnamon.emotes.Emotes
import net.perfectdreams.loritta.common.commands.CommandCategory
import net.perfectdreams.loritta.i18n.I18nKeysData
import net.perfectdreams.loritta.morenitta.interactions.UnleashedContext
import net.perfectdreams.loritta.morenitta.interactions.commands.*
import net.perfectdreams.loritta.morenitta.interactions.commands.options.ApplicationCommandOptions
import net.perfectdreams.loritta.morenitta.interactions.commands.options.ImageReferenceOrAttachment
import net.perfectdreams.loritta.morenitta.interactions.commands.options.OptionReference
import net.perfectdreams.loritta.morenitta.interactions.newSticker
import net.perfectdreams.loritta.morenitta.utils.LorittaUtils
import java.util.*

class GuildCommand : SlashCommandDeclarationWrapper {
    companion object {
        val I18N_PREFIX = I18nKeysData.Commands.Command.Guild
    }

    override fun command() = slashCommand(I18N_PREFIX.Label, I18N_PREFIX.Description, CommandCategory.DISCORD, UUID.fromString("6392c773-3c42-4b18-92cf-145b3cbaa9b8")) {
        enableLegacyMessageSupport = true
        isGuildOnly = true

        integrationTypes = listOf(
            IntegrationType.GUILD_INSTALL
        )

        subcommandGroup(I18N_PREFIX.Sticker.Label, I18N_PREFIX.Sticker.Description) {
            subcommand(I18N_PREFIX.Sticker.Add.Label, I18N_PREFIX.Sticker.Add.Description, UUID.fromString("a3d3bcbf-17ba-4b35-a46e-288127972d07")) {
                alternativeLegacyAbsoluteCommandPaths.apply {
                    add("addsticker")
                    add("adicionarfigurinha")
                }

                executor = GuildStickerAddExecutor()
            }

            subcommand(I18N_PREFIX.Sticker.Remove.Label, I18N_PREFIX.Sticker.Remove.Description, UUID.fromString("7209c475-9269-4512-8624-3e89f814bf31")) {
                alternativeLegacyAbsoluteCommandPaths.apply {
                    add("removesticker")
                    add("removerfigurinha")
                }

                executor = GuildStickerRemoveExecutor()
            }
        }

        subcommandGroup(I18N_PREFIX.Emoji.Label, I18N_PREFIX.Emoji.Description) {
            subcommand(I18N_PREFIX.Emoji.Add.Label, I18N_PREFIX.Emoji.Add.Description, UUID.fromString("f0e2e530-3057-47b0-b2a9-0fa11b02f75a")) {
                alternativeLegacyAbsoluteCommandPaths.apply {
                    add("addemoji")
                    add("adicionaremoji")
                }

                executor = GuildEmojiAddExecutor()
            }

            subcommand(I18N_PREFIX.Emoji.Remove.Label, I18N_PREFIX.Emoji.Remove.Description, UUID.fromString("906952d3-a7f6-422e-a2c3-e0cb161f8717")) {
                alternativeLegacyAbsoluteCommandPaths.apply {
                    add("removeemoji")
                    add("removeremoji")
                    add("delemoji")
                }

                executor = GuildEmojiRemoveExecutor()
            }
        }
    }

    inner class GuildStickerAddExecutor : LorittaSlashCommandExecutor(), LorittaLegacyMessageCommandExecutor {
        inner class Options : ApplicationCommandOptions() {
            val stickerName = string("sticker_name", I18N_PREFIX.Sticker.Add.Options.Name)
            val stickerTags = string("sticker_tags", I18N_PREFIX.Sticker.Add.Options.Tags)
            val stickerDescription = optionalString("sticker_description", I18N_PREFIX.Sticker.Add.Options.Description)
            val sticker = imageReferenceOrAttachment("sticker", I18N_PREFIX.Sticker.Add.Options.ImageData)
        }

        override val options = Options()

        override suspend fun execute(context: UnleashedContext, args: SlashCommandArguments) {
            if (!context.member.permissions.any { it == Permission.MANAGE_GUILD_EXPRESSIONS }) context.fail(true) {
                styled(
                    context.i18nContext.get(
                        I18nKeysData.Commands.UserDoesntHavePermissionDiscord(Permission.MANAGE_GUILD_EXPRESSIONS)
                    ),
                    Emotes.Error
                )
            }

            val name = args[options.stickerName]
            val description = args[options.stickerDescription] ?: context.i18nContext.get(
                I18N_PREFIX.Sticker.Add.DefaultDescription
            )
            val tags = args[options.stickerTags].split(", ")

            val sticker = try {
                args[options.sticker].get(context, false)
            } catch(e: Exception) {
                null
            }

            if (sticker == null) context.fail(true) {
                styled(
                    context.i18nContext.get(
                        I18nKeysData.Commands.NoValidImageFound
                    ),
                    Emotes.Error
                )
            }

            if (name.length < 2 || name.length > 30) context.fail(true) {
                styled(
                    context.i18nContext.get(I18N_PREFIX.Sticker.Add.OutOfBoundsName)
                )
            }

            if (description.length < 2 || description.length > 100) context.fail(true) {
                styled(
                    context.i18nContext.get(
                        I18N_PREFIX.Sticker.Add.OutOfBoundsDescription
                    ),
                    Emotes.Error
                )
            }

            context.deferChannelMessage(false)

            try {
                context.guild.newSticker(
                    context,
                    name,
                    description,
                    sticker,
                    tags
                )
            } catch (e: Exception) {
                when (e) {
                    is CommandException -> throw e
                    is RateLimitedException -> {
                        context.reply(true) {
                            styled(
                                context.i18nContext.get(I18nKeysData.Commands.Command.Guild.Sticker.Add.RateLimitExceeded),
                                Emotes.LoriHmpf
                            )
                        }
                        return
                    }
                    is ErrorResponseException -> {
                        when (e.errorResponse) {
                            ErrorResponse.FILE_UPLOAD_MAX_SIZE_EXCEEDED -> {
                                context.reply(true) {
                                    styled(
                                        context.i18nContext.get(
                                            I18N_PREFIX.Sticker.Add.FileUploadMaxSizeExceeded
                                        ),
                                        Emotes.Error
                                    )
                                }
                                return
                            }

                            ErrorResponse.MAX_STICKERS -> {
                                context.reply(true) {
                                    styled(
                                        context.i18nContext.get(
                                            I18N_PREFIX.Sticker.Add.MaxStickersReached
                                        ),
                                        Emotes.Error
                                    )
                                }
                                return
                            }

                            ErrorResponse.INVALID_FILE_UPLOADED, ErrorResponse.INVALID_FORM_BODY -> {
                                context.reply(true) {
                                    styled(
                                        context.i18nContext.get(
                                            I18N_PREFIX.Sticker.Add.InvalidUrl
                                        ),
                                        Emotes.Error
                                    )
                                }
                                return
                            }

                            else -> context.reply(true) {
                                styled(
                                    context.i18nContext.get(
                                        I18nKeysData.Commands.ErrorWhileExecutingCommand(
                                            Emotes.LoriRage,
                                            Emotes.LoriSob,
                                            e
                                        )
                                    ),
                                    Emotes.Error
                                )
                            }
                        }
                    }
                }
            }

            context.reply(false) {
                styled(
                    context.i18nContext.get(
                        I18N_PREFIX.Sticker.Add.SuccessfullyAdded
                    ),
                    Emotes.LoriHappyJumping
                )
            }
        }

        override suspend fun convertToInteractionsArguments(
            context: LegacyMessageCommandContext,
            args: List<String>
        ): Map<OptionReference<*>, Any?>? {
            val name = args.getOrNull(0)
            val data = args.getOrNull(1)
            val tags = args.getOrNull(2)


            if (name == null || data == null || tags == null) {
                context.explain()
            } else {
                return mapOf(
                    options.stickerName to name,
                    options.stickerTags to tags,
                    options.sticker to ImageReferenceOrAttachment(
                        dataValue = data,
                        attachment = context.event.message.attachments.firstOrNull()
                    )
                )
            }

            return null
        }
    }

    inner class GuildStickerRemoveExecutor : LorittaSlashCommandExecutor(), LorittaLegacyMessageCommandExecutor {
        inner class Options : ApplicationCommandOptions() {
            val stickerName = string("sticker_name", I18N_PREFIX.Sticker.Remove.Options.Name) {
                autocomplete { context ->
                    val stickerName = context.event.focusedOption.value

                    // There is no way that the guild could be null if the command is guild-only.
                    // So... non-null asserted.
                    val stickers = context.event.guild!!.stickers

                    if (stickerName.isBlank()) {
                        if (stickers.isEmpty()) {
                            return@autocomplete mapOf(
                                context.i18nContext.get(
                                    I18N_PREFIX.Sticker.Remove.NoStickersAvailable
                                ) to "empty"
                            )
                        } else {
                            return@autocomplete stickers.take(DiscordResourceLimits.Command.Options.ChoicesCount).associate { "${it.name} (${it.id})" to it.id }
                        }
                    } else {
                        val filteredStickers = stickers.filter { it.name.contains(stickerName, true) }
                        if (filteredStickers.isEmpty()) {
                            return@autocomplete mapOf(
                                context.i18nContext.get(
                                    I18N_PREFIX.Sticker.Remove.StickerNotFound
                                ) to stickerName
                            )
                        } else {
                            return@autocomplete filteredStickers.associate { "${it.name} (${it.id})" to it.id }
                        }
                    }
                }
            }
        }

        override val options = Options()

        override suspend fun execute(context: UnleashedContext, args: SlashCommandArguments) {
            if (!context.member.permissions.any { it == Permission.MANAGE_GUILD_EXPRESSIONS }) context.fail(true) {
                styled(
                    context.i18nContext.get(
                        I18nKeysData.Commands.UserDoesntHavePermissionDiscord(Permission.MANAGE_GUILD_EXPRESSIONS)
                    ),
                    Emotes.Error
                )
            }

            val stickerId = args[options.stickerName]

            context.guild.deleteSticker(StickerSnowflake.fromId(stickerId)).submit(false).await()

            context.reply(false) {
                styled(
                    context.i18nContext.get(
                        I18N_PREFIX.Sticker.Remove.SuccessfullyRemovedStickerMessage
                    ),
                    Emotes.LoriHappyJumping
                )
            }
        }

        override suspend fun convertToInteractionsArguments(
            context: LegacyMessageCommandContext,
            args: List<String>
        ): Map<OptionReference<*>, Any?>? {
            val name = args.getOrNull(0)

            val stickers = context.guild.stickers

            val stickerId = stickers.firstOrNull { it.name == name }?.id

            if (name == null || stickerId == null) {
                context.explain()
            } else {
                return mapOf(options.stickerName to stickerId)
            }

            return null
        }
    }

    inner class GuildEmojiAddExecutor : LorittaSlashCommandExecutor(), LorittaLegacyMessageCommandExecutor {
        inner class Options : ApplicationCommandOptions() {
            val emojiName = string("emoji_name", I18N_PREFIX.Emoji.Add.Options.Name)
            val emojiData = imageReferenceOrAttachment("emoji", I18N_PREFIX.Emoji.Add.Options.ImageData)
        }

        override val options = Options()

        override suspend fun execute(context: UnleashedContext, args: SlashCommandArguments) {
            if (!context.member.permissions.any { it == Permission.MANAGE_GUILD_EXPRESSIONS }) context.fail(true) {
                styled(
                    context.i18nContext.get(
                        I18nKeysData.Commands.UserDoesntHavePermissionDiscord(Permission.MANAGE_GUILD_EXPRESSIONS)
                    ),
                    Emotes.Error
                )
            }

            // We let the user add multiple emojis in a single message
            val emojiData = args[options.emojiData]
            val name = args[options.emojiName]
            try {
                if (emojiData.dataValue == null && emojiData.attachment == null) {
                    // If the emoji data is null, then we need to parse the emojis from the emoji name!
                    val emojisToBeAdded = LorittaUtils.retrieveEmojis(name)

                    if (emojisToBeAdded.isEmpty()) {
                        // No emojis found!
                        context.fail(true) {
                            styled(
                                context.i18nContext.get(
                                    I18N_PREFIX.Emoji.Add.CouldntFindAnyEmojis
                                ),
                                Emotes.Error
                            )
                        }
                    }

                    context.deferChannelMessage(false)

                    // Add all queried emojis!
                    for (emojiToBeAdded in emojisToBeAdded) {
                        val image = LorittaUtils.downloadFile(context.loritta, emojiToBeAdded.url, 5000) ?: context.fail(false) {
                            styled(
                                context.i18nContext.get(
                                    I18N_PREFIX.Emoji.Add.InvalidUrl
                                ),
                                Emotes.Error
                            )
                        }

                        val addedEmoji = try {
                            context.guild.createEmoji(
                                emojiToBeAdded.name,
                                Icon.from(image)
                            ).submit(false).await()
                        } catch (e: RateLimitedException) {
                            context.fail(true) {
                                styled(
                                    context.i18nContext.get(
                                        I18N_PREFIX.Emoji.Add.RateLimitExceeded
                                    ),
                                    Emotes.Error
                                )
                            }
                        }

                        context.reply(false) {
                            styled(
                                context.i18nContext.get(I18N_PREFIX.Emoji.Add.SuccessfullyAdded),
                                addedEmoji.asMention
                            )
                        }
                    }
                } else {
                    val data = try {
                        args[options.emojiData].get(context, false)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }

                    context.deferChannelMessage(false)

                    val parsedEmoji = LorittaUtils.retrieveEmoji(name)

                    val image = try {
                        LorittaUtils.downloadFile(context.loritta, parsedEmoji!!.url, 5000) ?: context.fail(true) {
                            styled(
                                context.i18nContext.get(
                                    I18N_PREFIX.Emoji.Add.InvalidUrl
                                ),
                                Emotes.Error
                            )
                        }
                    } catch (_: Exception) {
                        data?.let { LorittaUtils.downloadFile(context.loritta, it, 5000) } ?: context.fail(true) {
                            styled(
                                context.i18nContext.get(
                                    I18N_PREFIX.Emoji.Add.InvalidUrl
                                ),
                                Emotes.Error
                            )
                        }
                    }

                    val addedEmoji = try {
                        context.guild.createEmoji(
                            parsedEmoji?.name ?: name,
                            Icon.from(image)
                        ).submit(false).await()
                    } catch (e: RateLimitedException) {
                        context.fail(true) {
                            styled(
                                context.i18nContext.get(
                                    I18N_PREFIX.Emoji.Add.RateLimitExceeded
                                ),
                                Emotes.Error
                            )
                        }
                    }

                    context.reply(false) {
                        styled(
                            context.i18nContext.get(I18N_PREFIX.Emoji.Add.SuccessfullyAdded),
                            addedEmoji.asMention
                        )
                    }
                }
            } catch (e: ErrorResponseException) {
                e.printStackTrace()

                if (e.errorCode == 50138) {
                    context.fail(true) {
                        styled(
                            context.i18nContext.get(
                                I18N_PREFIX.Emoji.Add.FileUploadMaxSizeExceeded
                            ),
                            Emotes.Error
                        )
                    }
                }

                when (e.errorResponse) {
                    ErrorResponse.FILE_UPLOAD_MAX_SIZE_EXCEEDED -> context.fail(true) {
                        styled(
                            context.i18nContext.get(
                                I18N_PREFIX.Emoji.Add.FileUploadMaxSizeExceeded
                            ),
                            Emotes.Error
                        )
                    }

                    ErrorResponse.MAX_EMOJIS -> context.fail(true) {
                        styled(
                            context.i18nContext.get(
                                I18N_PREFIX.Emoji.Add.MaxStaticEmojisLimitReached,
                            ),
                            Emotes.Error
                        )
                    }

                    ErrorResponse.MAX_ANIMATED_EMOJIS -> context.fail(true) {
                        styled(
                            context.i18nContext.get(
                                I18N_PREFIX.Emoji.Add.MaxAnimatedEmojisLimitReached
                            ),
                            Emotes.Error
                        )
                    }

                    else -> context.fail(true) {
                        styled(
                            context.i18nContext.get(
                                I18nKeysData.Commands.ErrorWhileExecutingCommand(
                                    Emotes.LoriRage,
                                    Emotes.LoriSob,
                                    e.message!!
                                )
                            ),
                            Emotes.Error
                        )
                    }
                }
            }
        }

        override suspend fun convertToInteractionsArguments(
            context: LegacyMessageCommandContext,
            args: List<String>
        ): Map<OptionReference<*>, Any?>? {
            val name = args.getOrNull(0)

            if (name != null) {
                if (context.event.message.mentions.customEmojis.find { it.asMention == name } != null) {
                    val emoji = context.getEmoji(0) ?: context.fail(true) {
                        styled(
                            context.i18nContext.get(
                                I18N_PREFIX.Emoji.Add.InvalidEmoji
                            ),
                            Emotes.Error
                        )
                    }

                    return mapOf(
                        options.emojiName to emoji.name,
                        options.emojiData to ImageReferenceOrAttachment(
                            dataValue = emoji.imageUrl,
                            attachment = null
                        )
                    )
                } else {
                    val data = args.getOrNull(1) ?: context.event.message.attachments.firstOrNull()?.url

                    if (data == null) {
                        context.explain()
                        return null
                    }

                    return mapOf(
                        options.emojiName to name,
                        options.emojiData to ImageReferenceOrAttachment(
                            dataValue = data,
                            attachment = context.event.message.attachments.firstOrNull()
                        )
                    )
                }
            } else {
                context.explain()
            }

            return null
        }
    }

    inner class GuildEmojiRemoveExecutor : LorittaSlashCommandExecutor(), LorittaLegacyMessageCommandExecutor {
        inner class Options : ApplicationCommandOptions() {
            val emojiName = string("emoji_name", I18N_PREFIX.Emoji.Remove.Options.Name)
        }

        override val options = Options()

        override suspend fun execute(context: UnleashedContext, args: SlashCommandArguments) {
            if (!context.member.permissions.any { it == Permission.MANAGE_GUILD_EXPRESSIONS }) context.fail(true) {
                styled(
                    context.i18nContext.get(
                        I18nKeysData.Commands.UserDoesntHavePermissionDiscord(Permission.MANAGE_GUILD_EXPRESSIONS)
                    ),
                    Emotes.Error
                )
            }

            context.deferChannelMessage(false)

            val emojis = context.guild.emojis
            val selectedEmojis = args[options.emojiName].removeSurrounding(":").split(" ")
            val fetchedEmojis = emojis.filter { it.asMention in selectedEmojis }
            val removedEmojisSize = fetchedEmojis.size

            if (fetchedEmojis.isEmpty()) {
                context.fail(true) {
                    styled(
                        context.i18nContext.get(
                            I18N_PREFIX.Emoji.Remove.NoEmojisFound
                        ),
                        Emotes.Error
                    )
                }
            } else {
                fetchedEmojis.forEach {
                    if (it.guild == context.guild) {
                        it.delete().queue()
                    }
                }
            }

            context.reply(false) {
                styled(
                    context.i18nContext.get(
                        I18N_PREFIX.Emoji.Remove.SuccessfullyRemovedEmoji(removedEmojisSize)
                    ),
                    Emotes.LoriHappyJumping
                )
            }
        }

        override suspend fun convertToInteractionsArguments(
            context: LegacyMessageCommandContext,
            args: List<String>
        ): Map<OptionReference<*>, Any?>? {
            if (args.isEmpty()) {
                context.explain()
            } else {
                return mapOf(options.emojiName to args.joinToString(" "))
            }

            return null
        }
    }
}