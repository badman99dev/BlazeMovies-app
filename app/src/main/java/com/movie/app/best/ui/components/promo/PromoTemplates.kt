package com.movie.app.best.ui.components.promo

/**
 * Template registry — the ids the app can actually draw. Kept in sync with the backend
 * catalog (api/v1/common/promo_templates.php). An unknown id from a newer server always
 * falls back to a default the app knows, so an ad can never render blank.
 */
object PromoTemplates {

    const val DIALOG_IMAGE_ALPHA = "image_alpha"
    const val DIALOG_CENTER_CARD = "center_card"
    const val DIALOG_BOTTOM_SHEET = "bottom_sheet"
    const val DIALOG_FULLSCREEN_HERO = "fullscreen_hero"
    const val DIALOG_TOP_BANNER = "top_banner"

    const val BUTTON_CUSTOM_PNG = "custom_png"
    const val BUTTON_GRADIENT_RED = "gradient_red"
    const val BUTTON_SOLID_LIGHT = "solid_light"
    const val BUTTON_OUTLINE_GLASS = "outline_glass"
    const val BUTTON_GOLD_PREMIUM = "gold_premium"

    val dialogIds = setOf(
        DIALOG_IMAGE_ALPHA, DIALOG_CENTER_CARD, DIALOG_BOTTOM_SHEET,
        DIALOG_FULLSCREEN_HERO, DIALOG_TOP_BANNER
    )

    val buttonIds = setOf(
        BUTTON_CUSTOM_PNG, BUTTON_GRADIENT_RED, BUTTON_SOLID_LIGHT,
        BUTTON_OUTLINE_GLASS, BUTTON_GOLD_PREMIUM
    )

    fun dialog(id: String?): String = if (id != null && dialogIds.contains(id)) id else DIALOG_CENTER_CARD

    fun button(id: String?): String = if (id != null && buttonIds.contains(id)) id else BUTTON_GRADIENT_RED

    /** Image-overlay mode always uses the alpha-aware templates. */
    fun imageOverlayDialog(): String = DIALOG_IMAGE_ALPHA

    fun imageOverlayButton(): String = BUTTON_CUSTOM_PNG
}
