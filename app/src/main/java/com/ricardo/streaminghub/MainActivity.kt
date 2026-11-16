package com.ricardo.streaminghub

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private val backgroundColor = Color.rgb(16, 17, 22)
    private val accentColor = Color.rgb(85, 194, 255)
    private val textColor = Color.WHITE
    private val cardColor = Color.rgb(39, 43, 54)

    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var trendingContainer: LinearLayout
    private lateinit var statusText: TextView

    private data class Service(
        val name: String,
        val packageName: String
    )

    private data class MovieItem(
        val title: String,
        val type: String,
        val year: String,
        val rating: String
    )

    private val services = listOf(
        Service("Netflix", "com.netflix.ninja"),
        Service("Prime Video", "com.amazon.amazonvideo.livingroom"),
        Service("Disney+", "com.disney.disneyplus"),
        Service("Max", "com.wbd.stream"),
        Service("Globoplay", "com.globo.globotv"),
        Service("YouTube", "com.google.android.youtube.tv"),
        Service("Apple TV+", "com.apple.atve.androidtv")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor

        createHome()
        loadTrending()
    }

    private fun createHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
            setPadding(28, 24, 28, 20)
        }

        val scroll = ScrollView(this)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val title = TextView(this).apply {
            text = "STREAMING HUB"
            textSize = 26f
            setTextColor(accentColor)
            setTypeface(null, Typeface.BOLD)
        }

        val subtitle = TextView(this).apply {
            text = "Seu entretenimento em um só lugar"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, 6, 0, 24)
        }

        content.addView(title)
        content.addView(subtitle)

        addSectionTitle(content, "SEUS APLICATIVOS")

        val serviceRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        services.forEach { service ->
            serviceRow.addView(createServiceCard(service))
        }

        val horizontal = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(serviceRow)
        }

        content.addView(horizontal)

        addSectionTitle(content, "EM ALTA NO TMDB")

        statusText = TextView(this).apply {
            text = "Carregando filmes e séries..."
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, 4, 0, 12)
        }

        content.addView(statusText)

        trendingContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val trendingScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(trendingContainer)
        }

        content.addView(trendingScroll)

        addSectionTitle(content, "EXPLORAR")

        val categories = listOf(
            "Lançamentos",
            "Mais populares",
            "Séries em alta",
            "Filmes populares",
            "Em breve"
        )

        categories.forEach { category ->
            val item = TextView(this).apply {
                text = category
                textSize = 17f
                setTextColor(textColor)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(18, 16, 18, 16)
                background = roundedBackground(cardColor, 12)
                isFocusable = true
                isFocusableInTouchMode = true
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                58
            )
            params.bottomMargin = 10

            content.addView(item, params)
        }

        val attribution = TextView(this).apply {
            text = "This product uses the TMDB API but is not endorsed or certified by TMDB."
            textSize = 10f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 12)
        }

        content.addView(attribution)

        val footer = TextView(this).apply {
            text = "STREAMING HUB • VERSÃO 1.1"
            textSize = 11f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 12)
        }

        content.addView(footer)

        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
    }

    private fun loadTrending() {
        val token = BuildConfig.TMDB_READ_ACCESS_TOKEN

        if (token.isBlank()) {
            statusText.text =
                "Token TMDB não encontrado. Verifique o GitHub Actions."
            return
        }

        executor.execute {
            var connection: HttpURLConnection? = null

            try {
                val url = URL(
                    "https://api.themoviedb.org/3/trending/all/week?language=pt-BR&region=BR"
                )

                connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $token"
                )
                connection.setRequestProperty(
                    "accept",
                    "application/json"
                )

                val responseCode = connection.responseCode

                val responseStream = if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val response = responseStream
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: ""

                if (responseCode !in 200..299) {
                    throw Exception("Erro HTTP $responseCode")
                }

                val json = JSONObject(response)
                val results = json.optJSONArray("results")

                val items = mutableListOf<MovieItem>()

                if (results != null) {
                    for (i in 0 until minOf(results.length(), 15)) {
                        val item = results.optJSONObject(i) ?: continue

                        val title = item.optString("title")
                            .takeIf { it.isNotBlank() }
                            ?: item.optString("name")
                                .takeIf { it.isNotBlank() }
                            ?: continue

                        val mediaType = when (
                            item.optString("media_type")
                        ) {
                            "movie" -> "FILME"
                            "tv" -> "SÉRIE"
                            else -> "CONTEÚDO"
                        }

                        val date = item.optString("release_date")
                            .takeIf { it.isNotBlank() }
                            ?: item.optString("first_air_date")

                        val year = if (date.length >= 4) {
                            date.substring(0, 4)
                        } else {
                            "Ano não informado"
                        }

                        val rating = item.optDouble(
                            "vote_average",
                            0.0
                        )

                        items.add(
                            MovieItem(
                                title = title,
                                type = mediaType,
                                year = year,
                                rating = if (rating > 0) {
                                    "★ %.1f".format(
                                        java.util.Locale.US,
                                        rating
                                    )
                                } else {
                                    "Sem avaliação"
                                }
                            )
                        )
                    }
                }

                runOnUiThread {
                    if (items.isEmpty()) {
                        statusText.text =
                            "Nenhum título encontrado no momento."
                    } else {
                        showTrending(items)
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = when {
                        e.message?.contains("401") == true ->
                            "Token TMDB inválido ou não autorizado."

                        e.message?.contains("403") == true ->
                            "Acesso ao TMDB negado."

                        else ->
                            "Não foi possível carregar o catálogo. Verifique a internet."
                    }
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun showTrending(items: List<MovieItem>) {
        trendingContainer.removeAllViews()

        statusText.text = "Títulos em tendência nesta semana"

        items.forEach { item ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(14, 12, 14, 12)
                background = roundedBackground(cardColor, 14)
                isFocusable = true
                isFocusableInTouchMode = true
            }

            val title = TextView(this).apply {
                text = item.title
                textSize = 16f
                setTextColor(Color.WHITE)
                setTypeface(null, Typeface.BOLD)
                maxLines = 3
            }

            val type = TextView(this).apply {
                text = item.type
                textSize = 11f
                setTextColor(accentColor)
                setPadding(0, 10, 0, 4)
            }

            val details = TextView(this).apply {
                text = "${item.year}  •  ${item.rating}"
                textSize = 12f
                setTextColor(Color.LTGRAY)
            }

            card.addView(title)
            card.addView(type)
            card.addView(details)

            card.setOnFocusChangeListener { view, hasFocus ->
                view.background = roundedBackground(
                    if (hasFocus) accentColor else cardColor,
                    14
                )

                title.setTextColor(
                    if (hasFocus) Color.BLACK else Color.WHITE
                )

                type.setTextColor(
                    if (hasFocus) Color.BLACK else accentColor
                )

                details.setTextColor(
                    if (hasFocus) Color.BLACK else Color.LTGRAY
                )
            }

            card.setOnClickListener {
                Toast.makeText(
                    this,
                    item.title,
                    Toast.LENGTH_SHORT
                ).show()
            }

            val params = LinearLayout.LayoutParams(210, 125)
            params.marginEnd = 12

            trendingContainer.addView(card, params)
        }
    }

    private fun addSectionTitle(
        parent: LinearLayout,
        title: String
    ) {
        val label = TextView(this).apply {
            text = title
            textSize = 15f
            setTextColor(accentColor)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 20, 0, 12)
        }

        parent.addView(label)
    }

    private fun createServiceCard(
        service: Service
    ): TextView {
        return TextView(this).apply {
            text = service.name
            textSize = 15f
            setTextColor(textColor)
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setPadding(16, 12, 16, 12)
            minWidth = 125
            minHeight = 90
            isFocusable = true
            isFocusableInTouchMode = true

            background = roundedBackground(cardColor, 14)

            setOnFocusChangeListener { view, hasFocus ->
                view.background = roundedBackground(
                    if (hasFocus) accentColor else cardColor,
                    14
                )

                setTextColor(
                    if (hasFocus) Color.BLACK else textColor
                )
            }

            setOnClickListener {
                openService(service)
            }

            layoutParams = LinearLayout.LayoutParams(
                145,
                90
            ).apply {
                marginEnd = 12
            }
        }
    }

    private fun roundedBackground(
        color: Int,
        radius: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }
    }

    private fun openService(service: Service) {
        val intent = packageManager.getLaunchIntentForPackage(
            service.packageName
        )

        if (intent != null) {
            startActivity(intent)
        } else {
            Toast.makeText(
                this,
                "${service.name} não encontrado na TV",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}

