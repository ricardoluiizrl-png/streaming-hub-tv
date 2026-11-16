
package com.ricardo.streaminghub

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val backgroundColor = Color.rgb(16, 17, 22)
    private val accentColor = Color.rgb(85, 194, 255)
    private val textColor = Color.WHITE

    private data class Service(
        val name: String,
        val packageName: String
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
                background = roundedBackground(
                    Color.rgb(34, 37, 46), 12
                )
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

        val footer = TextView(this).apply {
            text = "STREAMING HUB • VERSÃO 1.0"
            textSize = 11f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 12)
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

    private fun addSectionTitle(
        parent: LinearLayout,
        title: String
    ) {
        val label = TextView(this).apply {
            text = title
            textSize = 15f
            setTextColor(accentColor)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 16, 0, 12)
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

            background = roundedBackground(
                Color.rgb(39, 43, 54), 14
            )

            setOnFocusChangeListener { view, hasFocus ->
                view.background = roundedBackground(
                    if (hasFocus) accentColor
                    else Color.rgb(39, 43, 54),
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
}
