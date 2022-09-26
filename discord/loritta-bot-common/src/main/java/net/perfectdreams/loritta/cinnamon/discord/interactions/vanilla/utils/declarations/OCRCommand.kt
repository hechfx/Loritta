package net.perfectdreams.loritta.cinnamon.discord.interactions.vanilla.utils.declarations

import net.perfectdreams.loritta.common.locale.LanguageManager
import net.perfectdreams.loritta.i18n.I18nKeysData
import net.perfectdreams.loritta.cinnamon.discord.interactions.commands.CommandCategory
import net.perfectdreams.loritta.cinnamon.discord.interactions.commands.CinnamonSlashCommandDeclarationWrapper
import net.perfectdreams.loritta.cinnamon.discord.interactions.vanilla.utils.ocr.OCRSlashExecutor

class OCRCommand(languageManager: LanguageManager) : CinnamonSlashCommandDeclarationWrapper(languageManager) {
    companion object {
        val I18N_PREFIX = I18nKeysData.Commands.Command.Ocr
    }

    override fun declaration() = slashCommand(I18N_PREFIX.Label, CommandCategory.UTILS, I18N_PREFIX.Description) {
        executor = { OCRSlashExecutor(it) }
    }
}