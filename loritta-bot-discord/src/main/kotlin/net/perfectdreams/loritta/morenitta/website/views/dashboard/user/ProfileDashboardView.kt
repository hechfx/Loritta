package net.perfectdreams.loritta.morenitta.website.views.dashboard.user

import kotlinx.html.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.perfectdreams.i18nhelper.core.I18nContext
import net.perfectdreams.loritta.common.locale.BaseLocale
import net.perfectdreams.loritta.common.utils.UserPremiumPlans
import net.perfectdreams.loritta.i18n.I18nKeysData
import net.perfectdreams.loritta.morenitta.utils.locale.LegacyBaseLocale
import net.perfectdreams.loritta.morenitta.website.LorittaWebsite
import net.perfectdreams.loritta.morenitta.website.components.LoadingSectionComponents
import net.perfectdreams.loritta.morenitta.website.utils.EmbeddedSpicyModalUtils.closeModalOnClick
import net.perfectdreams.loritta.morenitta.website.utils.EmbeddedSpicyModalUtils.openEmbeddedModalOnClick
import net.perfectdreams.loritta.morenitta.website.views.dashboard.DashboardView
import net.perfectdreams.loritta.serializable.ColorTheme
import net.perfectdreams.loritta.temmiewebsession.LorittaJsonWebSession

abstract class ProfileDashboardView(
    lorittaWebsite: LorittaWebsite,
    i18nContext: I18nContext,
    locale: BaseLocale,
    path: String,
    legacyBaseLocale: LegacyBaseLocale,
    userIdentification: LorittaJsonWebSession.UserIdentification,
    userPremiumPlan: UserPremiumPlans,
    colorTheme: ColorTheme,
    private val selectedType: String,
) : DashboardView(
    lorittaWebsite,
    i18nContext,
    locale,
    path,
    legacyBaseLocale,
    userIdentification,
    userPremiumPlan,
    colorTheme
) {
    override fun NAV.generateLeftSidebarContents() {
        div(classes = "entries") {
            a(classes = "entry loritta-logo") {
                text("Loritta")
            }

            hr(classes = "divider") {}

            appendEntry("/dashboard", true, locale["website.dashboard.profile.sectionNames.yourServers"], "fa fa-cogs", "main")

            hr(classes = "divider") {}
            div(classes = "category") {
                + "Configurações do Usuário"
            }

            appendEntry("/dashboard/profiles", false, locale["website.dashboard.profile.sectionNames.profileLayout"], "far fa-id-card", "profile_list")
            appendEntry("/dashboard/backgrounds", false, "Backgrounds", "far fa-images", "background_list")
            appendEntry("/dashboard/ship-effects", true, locale["website.dashboard.profile.sectionNames.shipEffects"], "fas fa-heart", "ship_effects")

            hr(classes = "divider") {}
            div(classes = "category") {
                + "Miscelânea"
            }

            appendEntry("/daily", false, "Daily", "fas fa-money-bill-wave", "daily")
            appendEntry("/dashboard/daily-shop", true, locale["website.dailyShop.title"], "fas fa-store", "daily_shop")
            appendEntry("/dashboard/sonhos-shop", true, locale["website.dashboard.profile.sectionNames.sonhosShop"], "fas fa-shopping-cart", "bundles")
            appendEntry("/guidelines", false, locale["website.guidelines.communityGuidelines"], "fas fa-asterisk", "guidelines")
            a(classes = "entry") {
                openEmbeddedModalOnClick(
                    i18nContext.get(I18nKeysData.Website.Dashboard.LorittaSpawner.PocketLoritta),
                    canBeClosedByClickingOutsideTheWindow = false,
                    {
                        div {
                            div {
                                style = "text-align: center;"

                                for (text in i18nContext.get(I18nKeysData.Website.Dashboard.LorittaSpawner.DoYouWantSomeCompany)) {
                                    p {
                                        text(text)
                                    }
                                }
                            }

                            div(classes = "loritta-spawner-wrapper") {
                                div(classes = "loritta-spawners") {
                                    fun FlowContent.creatureSpawner(
                                        fancyName: String,
                                        internalName: String,
                                        spritesFolder: String,
                                        loadingButtonGif: String
                                    ) {
                                        div(classes = "loritta-spawner") {
                                            img(src = "https://stuff.loritta.website/pocket-loritta/$spritesFolder/repouso.png") {
                                                width = "128"
                                            }
                                            button(classes = "discord-button success") {
                                                attributes["hx-post"] =
                                                    "/${i18nContext.get(I18nKeysData.Website.LocalePathId)}/dashboard/pocket-loritta/spawn"
                                                attributes["hx-vals"] = buildJsonObject {
                                                    put("type", internalName)
                                                }.toString()
                                                attributes["hx-indicator"] =
                                                    "find .htmx-discord-like-loading-button"
                                                attributes["hx-disabled-elt"] = "this"
                                                // Makes it a feel a bit better with the button not jumping all over the place when the text is replaced
                                                attributes["hx-on::before-request"] =
                                                    "this.style.minWidth = this.getBoundingClientRect().width + \"px\""
                                                attributes["hx-on::after-request"] =
                                                    "this.style.minWidth = null"

                                                div(classes = "htmx-discord-like-loading-button") {
                                                    div {
                                                        text(
                                                            i18nContext.get(
                                                                I18nKeysData.Website.Dashboard.LorittaSpawner.SpawnPlayer(
                                                                    fancyName
                                                                )
                                                            )
                                                        )
                                                    }

                                                    div(classes = "loading-text-wrapper") {
                                                        img(src = loadingButtonGif)

                                                        text(i18nContext.get(I18nKeysData.Website.Dashboard.Loading))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    creatureSpawner("Loritta", "LORITTA", "lori-sprites", LoadingSectionComponents.LORITTA_LOADING_GIF)
                                    creatureSpawner("Pantufa", "PANTUFA", "pantufa-sprites", LoadingSectionComponents.PANTUFA_LOADING_GIF)
                                    creatureSpawner("Gabriela", "GABRIELA", "gabriela-sprites", LoadingSectionComponents.GABRIELA_LOADING_GIF)
                                }

                                button(classes = "discord-button danger") {
                                    attributes["hx-post"] = "/${i18nContext.get(I18nKeysData.Website.LocalePathId)}/dashboard/pocket-loritta/clear"
                                    attributes["hx-indicator"] = "find .htmx-discord-like-loading-button"
                                    attributes["hx-disabled-elt"] = "this"
                                    // Makes it a feel a bit better with the button not jumping all over the place when the text is replaced
                                    attributes["hx-on::before-request"] = "this.style.minWidth = this.getBoundingClientRect().width + \"px\""
                                    attributes["hx-on::after-request"] = "this.style.minWidth = null"

                                    div(classes = "htmx-discord-like-loading-button") {
                                        div {
                                            text(i18nContext.get(I18nKeysData.Website.Dashboard.LorittaSpawner.CleanUp))
                                        }

                                        div(classes = "loading-text-wrapper") {
                                            img(src = LoadingSectionComponents.list.random())

                                            text(i18nContext.get(I18nKeysData.Website.Dashboard.Loading))
                                        }
                                    }
                                }
                            }
                        }
                    },
                    listOf {
                        classes += "no-background-theme-dependent-dark-text"

                        closeModalOnClick()
                        text(i18nContext.get(I18nKeysData.Website.Dashboard.Modal.Close))
                    }
                )
                i(classes = "fas fa-star") {
                    attributes["aria-hidden"] = "true"
                }

                text(" ")
                text(i18nContext.get(I18nKeysData.Website.Dashboard.LorittaSpawner.PocketLoritta))
            }

            hr(classes = "divider") {}

            a {
                id = "logout-button"

                div(classes = "entry") {
                    i(classes = "fas fa-sign-out-alt") {
                        attributes["aria-hidden"] = "true"
                    }

                    + " "
                    + locale["website.dashboard.profile.sectionNames.logout"]
                }
            }
        }
    }
}