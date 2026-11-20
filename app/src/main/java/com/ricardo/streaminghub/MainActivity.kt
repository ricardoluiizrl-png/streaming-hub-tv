package com.ricardo.streaminghub

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val bg = Color.rgb(7, 5, 12)
    private val panel = Color.rgb(27, 21, 36)
    private val purple = Color.rgb(190, 105, 255)
    private val white = Color.WHITE
    private val workers = Executors.newFixedThreadPool(3)
    private val imageCache = object : android.util.LruCache<String, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    private lateinit var pageContent: LinearLayout
    private lateinit var heroImage: ImageView
    private lateinit var heroTitle: TextView
    private lateinit var heroMeta: TextView
    private lateinit var heroOverview: TextView
    private lateinit var statusText: TextView
    private lateinit var catalogRow: LinearLayout
    private var currentItems: List<Media> = emptyList()
    private val favoriteKeys = mutableSetOf<String>()
    private val favoriteItems = mutableListOf<Media>()
    private var showingFavorites = false

    private data class Media(
        val id: Int,
        val title: String,
        val type: String,
        val year: String,
        val rating: String,
        val overview: String,
        val posterPath: String,
        val backdropPath: String
    )

    private data class Service(
        val name: String,
        val packageName: String,
        val fallback: String,
        val imageUrl: String = ""
    )

    private val services = listOf(
        Service("Netflix", "com.netflix.ninja", "N",
            "https://ichef.bbci.co.uk/ace/ws/800/cpsprodpb/0BD6/production/_103403030_gettyimages-912865028.jpg.webp"),
        Service("Prime Video", "com.amazon.amazonvideo.livingroom", "prime",
            "https://t2.tudocdn.net/710772?w=768&h=460"),
        Service("Disney+", "com.disney.disneyplus", "D+"),
        Service("Max", "com.wbd.stream", "max"),
        Service("Globoplay", "com.globo.globotv", "G",
            "https://t2.tudocdn.net/710137?w=1920&h=1080"),
        Service("YouTube", "com.google.android.youtube.tv", "▶"),
        Service("Apple TV+", "com.apple.atve.androidtv", "tv+")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        favoriteItems.addAll(getSharedPreferences("streaming_hub", MODE_PRIVATE).getStringSet("favorites", emptySet())!!.mapNotNull { raw ->
            try {
                val obj = JSONObject(raw)
                Media(obj.getInt("id"), obj.getString("title"), obj.getString("type"), obj.getString("year"), obj.getString("rating"), obj.getString("overview"), obj.getString("poster"), obj.getString("backdrop"))
            } catch (_: Exception) { null }
        })
        favoriteKeys.addAll(favoriteItems.map { mediaKey(it) })
        buildHome()
        loadCatalog("trending/all/week", "TENDÊNCIAS DA SEMANA")
        checkForUpdate(false)
    }

    private fun buildHome() {
        val contentRoot = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(bg)
        }

        val rail = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            setPadding(dp(7), dp(18), dp(7), dp(12))
            setBackgroundColor(Color.rgb(9, 6, 15))
        }
        rail.addView(label("N", 24, purple, true).apply {
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(38, 19, 54), 40)
        }, LinearLayout.LayoutParams(dp(50), dp(50)).apply { bottomMargin = dp(24) })

        val navItems = listOf("⌂" to "Início", "▤" to "Filmes", "▣" to "Séries", "▷" to "Em alta", "◎" to "Aplicativos", "▣" to "Minha lista", "⌕" to "Buscar")
        navItems.forEachIndexed { index, item ->
            val nav = label(item.first, 24, Color.LTGRAY, true).apply {
                gravity = Gravity.CENTER
                isFocusable = true
                setOnFocusChangeListener { view, focused ->
                    view.background = rounded(if (focused) Color.rgb(58, 31, 78) else Color.TRANSPARENT, 14)
                    (view as TextView).setTextColor(if (focused) white else Color.LTGRAY)
                }
                setOnClickListener {
                    when (index) {
                        0 -> loadCatalog("trending/all/week", "TENDÊNCIAS DA SEMANA")
                        1 -> loadCatalog("movie/popular", "FILMES POPULARES")
                        2 -> loadCatalog("tv/popular", "SÉRIES POPULARES")
                        3 -> loadCatalog("trending/all/week", "EM ALTA AGORA")
                        4 -> Toast.makeText(this@MainActivity, "Desça até Seus aplicativos", Toast.LENGTH_SHORT).show()
                        5 -> showFavorites()
                        6 -> showSearch()
                    }
                }
            }
            rail.addView(nav, LinearLayout.LayoutParams(dp(54), dp(54)).apply { bottomMargin = dp(7) })
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        }
        pageContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(28), dp(16), dp(28), dp(30))
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        topBar.addView(label("STREAMING HUB", 21, white, true), LinearLayout.LayoutParams(0, dp(48), 1f))
        listOf("♥", "♟", "⚙", "●").forEachIndexed { index, symbol ->
            topBar.addView(label(symbol, 20, white, true).apply {
                gravity = Gravity.CENTER
                background = rounded(panel, 40)
                isFocusable = true
                isClickable = true
                setOnClickListener { if (index == 2) checkForUpdate(true) }
            }, LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginStart = dp(9) })
        }
        pageContent.addView(topBar)

        val hero = FrameLayout(this).apply { background = rounded(panel, 18) }
        heroImage = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        hero.addView(heroImage, FrameLayout.LayoutParams(-1, -1))
        hero.addView(View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(7, 5, 12), Color.argb(225, 7, 5, 12), Color.TRANSPARENT)
            )
        }, FrameLayout.LayoutParams(-1, -1))

        val heroInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(28), dp(20), dp(20), dp(20))
        }
        heroInfo.addView(label("TENDÊNCIAS DA SEMANA", 12, purple, true))
        heroTitle = label("Descobrindo novidades...", 29, white, true).apply { maxLines = 2 }
        heroInfo.addView(heroTitle)
        heroMeta = label("Filmes e séries • TMDB", 14, Color.LTGRAY, true)
        heroInfo.addView(heroMeta, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        heroOverview = label("Os títulos mais comentados da semana.", 14, Color.LTGRAY, false).apply { maxLines = 3 }
        heroInfo.addView(heroOverview, LinearLayout.LayoutParams(dp(450), -2).apply { topMargin = dp(9) })

        val heroActions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        heroActions.addView(actionButton("▶  Ver agora", true) {
            currentItems.firstOrNull()?.let { showDetails(it) }
        }, LinearLayout.LayoutParams(dp(175), dp(50)).apply { marginEnd = dp(11) })
        heroActions.addView(actionButton("★  Mais info", false) {
            currentItems.firstOrNull()?.let { showDetails(it) }
        }, LinearLayout.LayoutParams(dp(155), dp(50)))
        heroInfo.addView(heroActions, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(16) })
        hero.addView(heroInfo, FrameLayout.LayoutParams(-1, -1))
        pageContent.addView(hero, LinearLayout.LayoutParams(-1, dp(340)).apply {
            topMargin = dp(14)
            bottomMargin = dp(8)
        })

        addSection("SEUS APLICATIVOS", "Acesse seus serviços favoritos")
        val appRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        services.forEach { appRow.addView(serviceCard(it)) }
        pageContent.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(appRow)
        })

        addSection("EM ALTA NO TMDB", "Filmes e séries para descobrir")
        statusText = label("Carregando catálogo...", 13, Color.LTGRAY, false)
        pageContent.addView(statusText)
        catalogRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        pageContent.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(catalogRow)
        })

        addSection("EXPLORAR", "Escolha uma categoria")
        val categories = listOf(
            "Lançamentos" to "movie/upcoming",
            "Mais populares" to "movie/popular",
            "Séries em alta" to "tv/popular",
            "Filmes populares" to "movie/top_rated",
            "Em breve" to "movie/upcoming"
        )
        categories.forEach { category ->
            val button = label("▶    ${category.first}", 16, white, true).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(18), 0, dp(18), 0)
                background = rounded(panel, 12)
                isFocusable = true
                isClickable = true
                setOnFocusChangeListener { view, focused ->
                    view.background = rounded(if (focused) purple else panel, 12)
                    (view as TextView).setTextColor(if (focused) Color.BLACK else white)
                }
                setOnClickListener { loadCatalog(category.second, category.first.uppercase(Locale("pt", "BR"))) }
            }
            pageContent.addView(button, LinearLayout.LayoutParams(-1, dp(56)).apply { bottomMargin = dp(9) })
        }

        pageContent.addView(label("This product uses the TMDB API but is not endorsed or certified by TMDB.", 10, Color.GRAY, false).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, dp(8))
        })

        scroll.addView(pageContent)
        contentRoot.addView(rail, LinearLayout.LayoutParams(dp(72), -1))
        contentRoot.addView(scroll, LinearLayout.LayoutParams(0, -1, 1f))
        val root = FrameLayout(this)
        root.addView(contentRoot, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
        showIntro(root)
    }

    private fun addSection(title: String, subtitle: String) {
        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(21), 0, dp(10))
        }
        heading.addView(label(title, 19, white, true))
        heading.addView(label(subtitle, 12, Color.LTGRAY, false))
        pageContent.addView(heading)
    }

    private fun loadCatalog(endpoint: String, heading: String) {
        showingFavorites = false
        statusText.text = "Carregando $heading..."
        workers.execute {
            var connection: HttpURLConnection? = null
            try {
                val token = BuildConfig.TMDB_READ_ACCESS_TOKEN
                if (token.isBlank()) throw Exception("Token TMDB ausente")
                val url = URL("https://api.themoviedb.org/3/$endpoint?language=pt-BR&region=BR")
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 12000
                connection.readTimeout = 12000
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("accept", "application/json")
                val code = connection.responseCode
                if (code !in 200..299) throw Exception("HTTP $code")
                val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                val array = json.optJSONArray("results")
                val found = mutableListOf<Media>()
                if (array != null) {
                    for (i in 0 until minOf(array.length(), 16)) {
                        val obj = array.optJSONObject(i) ?: continue
                        val title = obj.optString("title").ifBlank { obj.optString("name") }
                        if (title.isBlank()) continue
                        val date = obj.optString("release_date").ifBlank { obj.optString("first_air_date") }
                        val score = obj.optDouble("vote_average", 0.0)
                        found.add(Media(
                            obj.optInt("id"), title,
                            if (obj.has("first_air_date") || endpoint.startsWith("tv/")) "SÉRIE" else "FILME",
                            date.take(4).ifBlank { "—" },
                            if (score > 0) String.format(Locale.US, "★ %.1f", score) else "Sem nota",
                            obj.optString("overview"), obj.optString("poster_path"), obj.optString("backdrop_path")
                        ))
                    }
                }
                runOnUiThread {
                    currentItems = found
                    statusText.text = if (found.isEmpty()) "Nenhum título encontrado." else heading
                    catalogRow.removeAllViews()
                    found.forEach { catalogRow.addView(posterCard(it)) }
                    found.firstOrNull()?.let { updateHero(it) }
                }
            } catch (_: Exception) {
                runOnUiThread { statusText.text = "Não foi possível carregar o catálogo. Confira a internet e o token TMDB." }
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun updateHero(item: Media) {
        heroTitle.text = item.title
        heroMeta.text = "${item.year}   •   ${item.type}   •   ${item.rating}"
        heroOverview.text = item.overview.ifBlank { "Descubra mais sobre este título no catálogo." }
        if (item.backdropPath.isNotBlank()) {
            loadImage("https://image.tmdb.org/t/p/w1280${item.backdropPath}", heroImage)
        } else if (item.posterPath.isNotBlank()) {
            loadImage("https://image.tmdb.org/t/p/w780${item.posterPath}", heroImage)
        }
    }

    private fun posterCard(item: Media): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(panel, 12)
            setPadding(dp(5), dp(5), dp(5), dp(8))
            isFocusable = true
            isClickable = true
            setOnFocusChangeListener { view, focused ->
                view.background = rounded(if (focused) Color.rgb(76, 43, 99) else panel, 12)
                view.scaleX = if (focused) 1.04f else 1f
                view.scaleY = if (focused) 1.04f else 1f
            }
            setOnClickListener { showDetails(item) }
        }
        val poster = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = rounded(Color.rgb(38, 32, 44), 9)
            clipToOutline = true
        }
        card.addView(poster, LinearLayout.LayoutParams(dp(210), dp(118)))
        val imagePath = item.backdropPath.ifBlank { item.posterPath }
        val imageSize = if (item.backdropPath.isNotBlank()) "w780" else "w342"
        if (imagePath.isNotBlank()) loadImage("https://image.tmdb.org/t/p/$imageSize$imagePath", poster)
        card.addView(label(item.title, 13, white, true).apply { maxLines = 2 },
            LinearLayout.LayoutParams(dp(210), -2).apply { topMargin = dp(7) })
        card.addView(label("${item.year}  •  ${item.rating}", 11, Color.LTGRAY, false))
        return card.apply {
            layoutParams = LinearLayout.LayoutParams(dp(222), -2).apply { marginEnd = dp(12) }
        }
    }

    private fun serviceCard(service: Service): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(panel, 14)
            isFocusable = true
            isClickable = true
            setOnFocusChangeListener { view, focused ->
                view.background = rounded(if (focused) Color.rgb(75, 42, 96) else panel, 14)
                view.scaleX = if (focused) 1.06f else 1f
                view.scaleY = if (focused) 1.06f else 1f
            }
            setOnClickListener { openService(service) }
        }
        val icon = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = rounded(Color.rgb(17, 13, 24), 12)
            clipToOutline = true
        }
        if (service.imageUrl.isNotBlank()) {
            icon.scaleType = ImageView.ScaleType.CENTER_CROP
            loadImage(service.imageUrl, icon)
        } else {
            try {
                icon.setImageDrawable(packageManager.getApplicationIcon(service.packageName))
                icon.scaleType = ImageView.ScaleType.FIT_CENTER
            } catch (_: Exception) {
                icon.visibility = View.GONE
            }
        }
        if (icon.visibility == View.VISIBLE) card.addView(icon, LinearLayout.LayoutParams(-1, dp(88)))
        else card.addView(label(service.fallback, 25, white, true).apply { gravity = Gravity.CENTER })
        card.addView(label(service.name, 12, white, true).apply { gravity = Gravity.CENTER })
        return card.apply {
            layoutParams = LinearLayout.LayoutParams(dp(184), dp(132)).apply { marginEnd = dp(14) }
            setPadding(dp(7), dp(7), dp(7), dp(7))
        }
    }

    private fun loadImage(url: String, target: ImageView) {
        val cached = imageCache.get(url)
        if (cached != null) {
            target.setImageBitmap(cached)
            return
        }
        target.tag = url
        workers.execute {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.setRequestProperty("User-Agent", "StreamingHubTV/1.0")
                val bitmap = connection.inputStream.use { BitmapFactory.decodeStream(it) }
                connection.disconnect()
                if (bitmap != null) {
                    imageCache.put(url, bitmap)
                    runOnUiThread {
                        if (target.tag == url) target.setImageBitmap(bitmap)
                    }
                }
            } catch (_: Exception) {
                // Mantém o cartão de fundo caso a imagem não esteja disponível.
            }
        }
    }

    private fun showDetails(item: Media) {
        val isFavorite = favoriteKeys.contains(mediaKey(item))
        AlertDialog.Builder(this)
            .setTitle(item.title)
            .setMessage("${item.type} • ${item.year} • ${item.rating}\n\n${item.overview.ifBlank { "Sinopse não disponível." }}")
            .setPositiveButton(if (isFavorite) "Remover da minha lista" else "Adicionar à minha lista") { _, _ -> toggleFavorite(item) }
            .setNegativeButton("Fechar", null)
            .show()
    }

    private fun mediaKey(item: Media): String {
        val kind = when (item.type.lowercase(Locale.ROOT)) {
            "filme", "movie" -> "movie"
            "série", "serie", "tv" -> "tv"
            else -> item.type.lowercase(Locale.ROOT)
        }
        return "$kind:${item.id}"
    }

    private fun toggleFavorite(item: Media) {
        val key = mediaKey(item)
        if (favoriteKeys.add(key)) favoriteItems.add(item)
        else favoriteItems.removeAll { mediaKey(it) == key }
        val saved = favoriteItems.map { media ->
            JSONObject().put("id", media.id).put("title", media.title).put("type", media.type)
                .put("year", media.year).put("rating", media.rating).put("overview", media.overview)
                .put("poster", media.posterPath).put("backdrop", media.backdropPath).toString()
        }.toSet()
        getSharedPreferences("streaming_hub", MODE_PRIVATE).edit().putStringSet("favorites", saved).apply()
        Toast.makeText(this, if (favoriteKeys.contains(key)) "Adicionado à Minha Lista" else "Removido da Minha Lista", Toast.LENGTH_SHORT).show()
        if (showingFavorites) showFavorites()
    }

    private fun showFavorites() {
        showingFavorites = true
        val favorites = favoriteItems.toList()
        catalogRow.removeAllViews()
        statusText.text = if (favorites.isEmpty()) "Abra os detalhes de um título e escolha Adicionar à minha lista." else "MINHA LISTA • ${favorites.size} TÍTULO(S)"
        favorites.forEach { catalogRow.addView(posterCard(it)) }
        if (favorites.isEmpty()) Toast.makeText(this, "Sua lista está vazia ou os títulos ainda não foram carregados.", Toast.LENGTH_LONG).show()
    }

    private fun showIntro(root: FrameLayout) {
        val overlay = FrameLayout(this).apply { setBackgroundColor(bg) }
        val mark = TextView(this).apply {
            text = "STREAMING HUB"
            textSize = 30f
            setTextColor(white)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            letterSpacing = 0.12f
            alpha = 0f
            scaleX = 0.88f
            scaleY = 0.88f
        }
        overlay.addView(mark, FrameLayout.LayoutParams(-1, -1))
        root.addView(overlay, FrameLayout.LayoutParams(-1, -1))
        mark.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(420).withEndAction {
            mark.animate().alpha(0f).setStartDelay(420).setDuration(260).withEndAction {
                overlay.animate().alpha(0f).setDuration(220).withEndAction { root.removeView(overlay) }.start()
            }.start()
        }.start()
    }

    private fun showSearch() {
        val input = EditText(this).apply { hint = "Nome do filme ou série" }
        AlertDialog.Builder(this)
            .setTitle("Buscar no catálogo")
            .setView(input)
            .setPositiveButton("Buscar") { _, _ ->
                val query = input.text.toString().trim()
                if (query.isNotEmpty()) searchCatalog(query)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun searchCatalog(query: String) {
        workers.execute {
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val url = URL("https://api.themoviedb.org/3/search/multi?query=$encoded&language=pt-BR")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 12000
                connection.readTimeout = 12000
                connection.setRequestProperty("Authorization", "Bearer ${BuildConfig.TMDB_READ_ACCESS_TOKEN}")
                val code = connection.responseCode
                if (code !in 200..299) throw Exception("HTTP $code")
                val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                val array = json.optJSONArray("results")
                val found = mutableListOf<Media>()
                if (array != null) {
                    for (i in 0 until minOf(array.length(), 16)) {
                        val obj = array.optJSONObject(i) ?: continue
                        val mediaType = obj.optString("media_type")
                        if (mediaType != "movie" && mediaType != "tv") continue
                        val title = obj.optString("title").ifBlank { obj.optString("name") }
                        if (title.isNotBlank()) {
                            val date = obj.optString("release_date").ifBlank { obj.optString("first_air_date") }
                            val score = obj.optDouble("vote_average", 0.0)
                            found.add(Media(obj.optInt("id"), title, if (mediaType == "tv") "SÉRIE" else "FILME",
                                date.take(4).ifBlank { "—" }, if (score > 0) String.format(Locale.US, "★ %.1f", score) else "Sem nota",
                                obj.optString("overview"), obj.optString("poster_path"), obj.optString("backdrop_path")))
                        }
                    }
                }
                runOnUiThread {
                    showingFavorites = false
                    currentItems = found
                    catalogRow.removeAllViews()
                    statusText.text = "RESULTADOS PARA: $query"
                    found.forEach { catalogRow.addView(posterCard(it)) }
                    found.firstOrNull()?.let { updateHero(it) }
                }
            } catch (_: Exception) {
                runOnUiThread { statusText.text = "Falha na busca. Tente novamente." }
            }
        }
    }


    private data class HubRelease(val version: String, val apk: String, val checksum: String, val notes: String)

    private fun checkForUpdate(manual: Boolean) {
        if (manual) Toast.makeText(this, "Verificando atualizações...", Toast.LENGTH_SHORT).show()
        workers.execute {
            var connection: HttpURLConnection? = null
            try {
                connection = URL("https://api.github.com/repos/ricardoluiizrl-png/streaming-hub-tv/releases/latest").openConnection() as HttpURLConnection
                connection.connectTimeout = 12000
                connection.readTimeout = 12000
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.setRequestProperty("User-Agent", "StreamingHubTV")
                if (connection.responseCode == 404) {
                    if (manual) runOnUiThread { Toast.makeText(this, "Ainda não há atualização publicada.", Toast.LENGTH_LONG).show() }
                    return@execute
                }
                if (connection.responseCode !in 200..299) throw Exception("GitHub indisponível")
                val release = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                val assets = release.optJSONArray("assets") ?: throw Exception("Assets ausentes")
                var apk = ""
                var checksum = ""
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name")
                    if (name.endsWith(".apk", true)) apk = asset.optString("browser_download_url")
                    if (name.endsWith(".apk.sha256", true)) checksum = asset.optString("browser_download_url")
                }
                val remote = release.optString("tag_name").removePrefix("v")
                val local = packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
                if (remote.isBlank() || apk.isBlank() || checksum.isBlank()) throw Exception("Release sem APK/checksum")
                if (compareHubVersions(remote, local) > 0) {
                    val info = HubRelease(remote, apk, checksum, release.optString("body"))
                    runOnUiThread {
                        AlertDialog.Builder(this)
                            .setTitle("Nova versão disponível: " + info.version)
                            .setMessage((info.notes.ifBlank { "Uma nova versão está pronta." }).take(1000) + "\n\nBaixar e instalar agora? O Android pedirá confirmação.")
                            .setPositiveButton("Atualizar") { _, _ -> downloadHubUpdate(info) }
                            .setNegativeButton("Depois", null)
                            .show()
                    }
                } else if (manual) {
                    runOnUiThread { Toast.makeText(this, "O Streaming Hub já está atualizado.", Toast.LENGTH_LONG).show() }
                }
            } catch (_: Exception) {
                if (manual) runOnUiThread { Toast.makeText(this, "Não foi possível verificar atualizações.", Toast.LENGTH_LONG).show() }
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun compareHubVersions(remote: String, local: String): Int {
        val a = remote.split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
        val b = local.removePrefix("v").split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return 0
    }

    private fun downloadHubUpdate(info: HubRelease) {
        val progress = ProgressBar(this).apply { isIndeterminate = true }
        val dialog = AlertDialog.Builder(this).setTitle("Baixando atualização").setView(progress).setCancelable(false).create()
        dialog.show()
        workers.execute {
            try {
                val apkFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "streaming-hub-update.apk")
                val apkConnection = URL(info.apk).openConnection() as HttpURLConnection
                apkConnection.connectTimeout = 15000
                apkConnection.readTimeout = 30000
                apkConnection.setRequestProperty("User-Agent", "StreamingHubTV")
                if (apkConnection.responseCode !in 200..299) throw Exception("Download do APK falhou")
                apkConnection.inputStream.use { input -> FileOutputStream(apkFile).use { output -> input.copyTo(output) } }
                apkConnection.disconnect()

                val sumConnection = URL(info.checksum).openConnection() as HttpURLConnection
                sumConnection.connectTimeout = 12000
                sumConnection.readTimeout = 12000
                sumConnection.setRequestProperty("User-Agent", "StreamingHubTV")
                val expected = sumConnection.inputStream.bufferedReader().use { it.readText() }.trim().split(Regex("\\s+"))[0]
                sumConnection.disconnect()

                val digest = MessageDigest.getInstance("SHA-256")
                apkFile.inputStream().use { input ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        digest.update(buffer, 0, count)
                    }
                }
                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                if (expected.length != 64 || !actual.equals(expected, true)) throw Exception("Checksum inválido")
                runOnUiThread {
                    dialog.dismiss()
                    getSharedPreferences("streaming_hub", MODE_PRIVATE).edit().putString("pending_update_apk", apkFile.absolutePath).apply()
                    continueHubInstall(apkFile)
                }
            } catch (_: Exception) {
                runOnUiThread {
                    dialog.dismiss()
                    AlertDialog.Builder(this).setTitle("Falha na atualização")
                        .setMessage("Não foi possível baixar ou validar o APK.")
                        .setPositiveButton("OK", null).show()
                }
            }
        }
    }

    private fun continueHubInstall(apk: File) {
        if (android.os.Build.VERSION.SDK_INT >= 26 && !packageManager.canRequestPackageInstalls()) {
            AlertDialog.Builder(this)
                .setTitle("Permitir instalação")
                .setMessage("Autorize o Streaming Hub TV a instalar atualizações. Depois, volte ao aplicativo.")
                .setPositiveButton("Abrir configurações") { _, _ ->
                    startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + packageName)))
                }
                .setNegativeButton("Cancelar", null).show()
        } else {
            installHubApk(apk)
        }
    }

    private fun installHubApk(apk: File) {
        try {
            val uri = Uri.Builder().scheme("content").authority(packageName + ".fileprovider").appendPath("downloads").appendPath(apk.name).build()
            val install = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            getSharedPreferences("streaming_hub", MODE_PRIVATE).edit().remove("pending_update_apk").apply()
            startActivity(install)
        } catch (_: Exception) {
            Toast.makeText(this, "Não foi possível abrir o instalador do Android.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (android.os.Build.VERSION.SDK_INT >= 26 && packageManager.canRequestPackageInstalls()) {
            val pending = getSharedPreferences("streaming_hub", MODE_PRIVATE).getString("pending_update_apk", null)
            if (!pending.isNullOrBlank()) {
                val file = File(pending)
                if (file.exists()) installHubApk(file)
                else getSharedPreferences("streaming_hub", MODE_PRIVATE).edit().remove("pending_update_apk").apply()
            }
        }
    }

    private fun openService(service: Service) {
        val intent = packageManager.getLaunchIntentForPackage(service.packageName)
        if (intent != null) startActivity(intent)
        else Toast.makeText(this, "${service.name} não está instalada nesta TV.", Toast.LENGTH_SHORT).show()
    }

    private fun actionButton(value: String, primary: Boolean, action: () -> Unit): TextView =
        label(value, 15, if (primary) Color.BLACK else white, true).apply {
            gravity = Gravity.CENTER
            background = rounded(if (primary) purple else panel, 28)
            isFocusable = true
            isClickable = true
            setOnFocusChangeListener { view, focused -> view.alpha = if (focused) 0.78f else 1f }
            setOnClickListener { action() }
        }

    private fun label(value: String, size: Int, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
        }

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        workers.shutdownNow()
        super.onDestroy()
    }
}
