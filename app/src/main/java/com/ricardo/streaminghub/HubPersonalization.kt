package com.ricardo.streaminghub

import android.content.Context
import android.graphics.Color

/**
 * Persistent personalization settings for the TV experience.
 * Kept separate from the Activity so screens can share one source of truth.
 */
data class HubProfile(
    val displayName: String = "",
    val favoriteGenres: Set<String> = emptySet()
)

data class HubTheme(
    val id: String,
    val label: String,
    val background: Int,
    val surface: Int,
    val accent: Int,
    val text: Int = Color.WHITE,
    val secondaryText: Int = Color.LTGRAY
)

object HubPersonalization {
    private const val PREFS = "streaming_hub"
    private const val KEY_PROFILE_NAME = "profile_name"
    private const val KEY_PROFILE_GENRES = "profile_genres"
    private const val KEY_THEME = "theme"

    val genres = listOf(
        "Ação",
        "Aventura",
        "Ficção científica",
        "Suspense",
        "Terror",
        "Romance",
        "Comédia",
        "Drama",
        "Animação",
        "Anime",
        "Documentário"
    )

    val themes = listOf(
        HubTheme(
            id = "Midnight",
            label = "Midnight",
            background = Color.rgb(7, 9, 15),
            surface = Color.rgb(25, 30, 42),
            accent = Color.rgb(115, 175, 255)
        ),
        HubTheme(
            id = "Ocean",
            label = "Ocean",
            background = Color.rgb(5, 12, 24),
            surface = Color.rgb(18, 34, 54),
            accent = Color.rgb(74, 170, 255)
        ),
        HubTheme(
            id = "Aurora",
            label = "Aurora",
            background = Color.rgb(10, 7, 22),
            surface = Color.rgb(30, 22, 48),
            accent = Color.rgb(153, 116, 255)
        ),
        HubTheme(
            id = "Emerald",
            label = "Emerald",
            background = Color.rgb(5, 15, 13),
            surface = Color.rgb(19, 39, 34),
            accent = Color.rgb(76, 205, 153)
        ),
        HubTheme(
            id = "Crimson",
            label = "Crimson",
            background = Color.rgb(18, 7, 10),
            surface = Color.rgb(43, 20, 26),
            accent = Color.rgb(255, 104, 125)
        )
    )

    fun loadProfile(context: Context): HubProfile {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return HubProfile(
            displayName = prefs.getString(KEY_PROFILE_NAME, "").orEmpty(),
            favoriteGenres = prefs.getStringSet(KEY_PROFILE_GENRES, emptySet())
                ?.toSet()
                .orEmpty()
        )
    }

    fun saveProfile(context: Context, profile: HubProfile) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PROFILE_NAME, profile.displayName.trim())
            .putStringSet(KEY_PROFILE_GENRES, profile.favoriteGenres.toSet())
            .apply()
    }

    fun loadTheme(context: Context): HubTheme {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val selectedId = prefs.getString(KEY_THEME, "Midnight")
        return themes.firstOrNull { it.id == selectedId } ?: themes.first()
    }

    fun saveTheme(context: Context, themeId: String) {
        val validId = themes.firstOrNull { it.id == themeId }?.id ?: themes.first().id
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, validId)
            .apply()
    }
}
