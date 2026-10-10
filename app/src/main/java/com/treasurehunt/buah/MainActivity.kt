package com.treasurehunt.buah

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.*
import kotlin.math.ceil

class MainActivity : Activity() {
    private enum class Screen { SPLASH, MENU, VIDEO, STORY, INSTRUCTIONS, BASKETS, COUNTDOWN, GAME, RESULT, ENDING }
    private data class Frame(val asset: String, val title: String, val caption: String, val scene: Scene)
    private var screen = Screen.SPLASH
    private var selected = Basket.GREEN
    private var game: FruitGameView? = null
    private var hud: TextView? = null
    private var lastHud = ""
    private var tone: ToneGenerator? = null
    private var sound = true
    private var pauseDialog: AlertDialog? = null
    private var storyVideo: VideoView? = null
    private val cream = Color.rgb(255, 248, 226)
    private val handler = Handler(Looper.getMainLooper())
    private var countdownStep = 3
    private var countdownLabel: TextView? = null
    private val countdown = object : Runnable {
        override fun run() {
            if (screen != Screen.COUNTDOWN) return
            if (countdownStep < 0) { startGame(); return }
            countdownLabel?.text = if (countdownStep == 0) "MULA!" else "$countdownStep..."
            countdownLabel?.apply { scaleX = .75f; scaleY = .75f; animate().scaleX(1f).scaleY(1f).setDuration(300).start() }
            playTone(Feedback.COUNTDOWN)
            countdownStep--
            handler.postDelayed(this, if (countdownStep < 0) 350 else 1000)
        }
    }
    private val introFrames = listOf(
        Frame("story_supermarket", "Selamat datang!", "Hari ini, kita meneroka pasar raya bersama Cikgu.", Scene.TITLE),
        Frame("story_enter", "Jom masuk!", "Ikut Cikgu dan kawan-kawan ke bahagian buah-buahan.", Scene.BRIEFING),
        Frame("story_briefing", "Taklimat Cikgu", "Kita akan mencari buah-buahan. Pilih bakul warna kegemaran kamu!", Scene.BRIEFING),
        Frame("story_instruction", "Dengar arahan", "Tangkap buah-buahan sahaja. Elak tangkap selain buah.", Scene.BRIEFING),
        Frame("story_teamwork", "Kita satu pasukan!", "Bekerjasama, pilih buah yang betul dan berseronok!", Scene.BRIEFING),
        Frame("story_ready", "Sudah bersedia?", "Bakul hijau, merah, ungu atau oren? Jom mulakan misi!", Scene.BASKETS)
    )
    private val collectionFrames = listOf(
        Frame("story_start", "Mula memburu!", "Semua pasukan bergerak ke bahagian buah-buahan.", Scene.BRIEFING),
        Frame("story_fruit_hunt", "Cari buah segar", "Lihat rak buah-buahan dengan teliti.", Scene.INSTRUCTIONS),
        Frame("story_orange_team", "Pasukan oren", "Cari oren yang segar dan masukkan ke dalam bakul.", Scene.BASKETS),
        Frame("story_orange_basket", "Bakul oren", "Buah oren memberi bonus kepada bakul oren.", Scene.BASKETS),
        Frame("story_grape_team", "Pasukan ungu", "Anggur ungu yang sedap untuk bakul ungu.", Scene.BASKETS),
        Frame("story_purple_basket", "Bakul ungu", "Kumpul buah-buahan yang betul.", Scene.BASKETS),
        Frame("story_green_team", "Pasukan hijau", "Epal hijau memberi bonus kepada bakul hijau.", Scene.BASKETS),
        Frame("story_green_basket", "Bakul hijau", "Jangan ambil barang selain buah!", Scene.BASKETS),
        Frame("story_red_team", "Pasukan merah", "Epal merah memberi bonus kepada bakul merah.", Scene.BASKETS),
        Frame("story_red_basket", "Bakul merah", "Epal merah dan strawberi dikumpulkan dalam bakul.", Scene.BASKETS),
        Frame("story_montage", "Bakul semakin penuh", "Oren, epal dan anggur — semua buah dikumpulkan.", Scene.INSTRUCTIONS),
        Frame("story_progress", "Syabas, semua!", "Pasukan berkumpul dengan hasil buruan mereka.", Scene.CHECKOUT)
    )
    private val endingFrames = listOf(
        Frame("story_finish", "Semak buah kita", "Susun dan semak buah-buahan sebelum ke kaunter.", Scene.CHECKOUT),
        Frame("story_checkout", "Di kaunter", "Cikgu dan kawan-kawan membawa buah ke kaunter bayaran.", Scene.CHECKOUT),
        Frame("story_celebration", "TAHNIAH!", "ANDA TELAH MENYELESAIKAN\nTREASURE HUNT BUAH-BUAHAN!", Scene.CHECKOUT)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sound = getPreferences(0).getBoolean("sound", true)
        tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 65) }.getOrNull()
        volumeControlStream = AudioManager.STREAM_MUSIC
        menu()
    }

    private fun page(destination: Screen, title: String, subtitle: String, scene: Scene, asset: String): LinearLayout {
        releaseStoryVideo()
        handler.removeCallbacks(countdown); countdownLabel = null
        game?.stop(); game = null; hud = null; screen = destination
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setBackgroundColor(cream)
        }
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(8))
        }
        hero.addView(TextView(this).apply {
            text = title; textSize = 25f; gravity = Gravity.CENTER
            setTextColor(Color.rgb(36, 92, 58)); setTypeface(null, Typeface.BOLD); setPadding(0, 0, 0, dp(8))
            if (Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
        })
        hero.addView(StoryboardView(this, asset, scene), LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(hero, LinearLayout.LayoutParams(-1, 0, 1.15f))
        val p = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(2), dp(4), dp(8))
        }
        if (subtitle.isNotEmpty()) p.addView(TextView(this).apply {
            text = subtitle; textSize = 16f; gravity = Gravity.CENTER
            setTextColor(Color.rgb(49, 70, 61)); setPadding(0, dp(4), 0, dp(8)); setLineSpacing(dp(2).toFloat(), 1f)
        })
        root.addView(ScrollView(this).apply {
            isFillViewport = true
            addView(p)
        }, LinearLayout.LayoutParams(-1, 0, 1f))
        showContent(root)
        return p
    }

    private fun button(parent: LinearLayout, label: String, colour: Int = Color.rgb(0, 121, 107), action: () -> Unit): Button {
        val b = Button(this).apply {
            text = label; textSize = 16f; isAllCaps = false; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD)
            background = GradientDrawable().apply { setColor(colour); cornerRadius = dp(16).toFloat() }
            setOnClickListener { action() }
        }
        parent.addView(b, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(8) })
        return b
    }

    private fun splash() {
        val p = page(Screen.SPLASH, "TREASURE HUNT\nBUAH-BUAHAN", "Selamat datang!\nMisi buah-buahan bersama Cikgu dan kawan-kawan.", Scene.TITLE, "story_intro")
        button(p, "SETERUSNYA") { story(introFrames, 0, true) }
        button(p, "LANGKAU INTRO", Color.rgb(92, 83, 157)) { completeIntro(); menu() }
    }
    private fun completeIntro() { getPreferences(0).edit().putBoolean("intro_seen", true).apply() }

    private fun menu() {
        val p = page(Screen.MENU, "TREASURE HUNT\nBUAH-BUAHAN",
            "Pasar Ceria • Tangkap buah-buahan sahaja!", Scene.TITLE, "story_fruit_hunt")
        button(p, "CERITA", Color.rgb(92, 83, 157)) { playStoryVideo() }
        button(p, "CARA BERMAIN") { instructions() }
        button(p, "MULA BERMAIN") { choose() }
        button(p, "SKOR TERTINGGI", Color.rgb(92, 83, 157)) {
            AlertDialog.Builder(this).setTitle("SKOR TERTINGGI")
                .setMessage("${getPreferences(0).getInt("best", 0)} mata")
                .setPositiveButton("OK", null).show()
        }
        lateinit var soundButton: Button
        soundButton = button(p, soundLabel()) { toggleSound(); soundButton.text = soundLabel() }
    }

    /** Plays a bundled MP4 from app/src/main/res/raw/cerita.mp4.
     *  No network access or extra permissions are required.
     */
    private fun playStoryVideo() {
        releaseStoryVideo()
        val videoId = resources.getIdentifier("cerita", "raw", packageName)
        if (videoId == 0) {
            AlertDialog.Builder(this)
                .setTitle("VIDEO CERITA")
                .setMessage("Video cerita belum dimasukkan ke dalam aplikasi.")
                .setPositiveButton("KEMBALI", null)
                .show()
            return
        }
        handler.removeCallbacks(countdown)
        game?.stop(); game = null; hud = null
        screen = Screen.VIDEO

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        root.addView(TextView(this).apply {
            text = "CERITA"
            gravity = Gravity.CENTER
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(-1, dp(48)))

        val player = VideoView(this).apply {
            setMediaController(MediaController(this@MainActivity).also { it.setAnchorView(this) })
            setOnPreparedListener { media ->
                media.isLooping = false
                media.setVolume(if (sound) 1f else 0f, if (sound) 1f else 0f)
            }
            setOnCompletionListener { menu() }
            setOnErrorListener { _, _, _ ->
                Toast.makeText(this@MainActivity, "Video tidak dapat dimainkan.", Toast.LENGTH_LONG).show()
                menu()
                true
            }
        }
        storyVideo = player
        root.addView(player, LinearLayout.LayoutParams(-1, 0, 1f))
        button(root, "KEMBALI KE MENU", Color.rgb(92, 83, 157)) { menu() }
        showContent(root)
        player.setVideoURI(Uri.parse("android.resource://$packageName/$videoId"))
        player.start()
    }

    private fun releaseStoryVideo() {
        storyVideo?.stopPlayback()
        storyVideo = null
    }

    private fun story(frames: List<Frame>, index: Int, intro: Boolean, ending: Boolean = false) {
        val frame = frames[index]
        val p = page(if (ending) Screen.ENDING else Screen.STORY, frame.title,
            "${index + 1} / ${frames.size}\n\n${frame.caption}", frame.scene, frame.asset)
        if (index < frames.lastIndex) button(p, "SETERUSNYA") { story(frames, index + 1, intro, ending) }
        else if (ending) button(p, "MAIN LAGI") { choose() }
        else button(p, if (intro) "CARA BERMAIN" else "MENU UTAMA") {
            if (intro) { completeIntro(); instructions() } else menu()
        }
        button(p, "KEMBALI", Color.rgb(92, 83, 157)) { if (index > 0) story(frames, index - 1, intro, ending) else menu() }
        if (!ending) button(p, "LANGKAU INTRO") { if (intro) completeIntro(); menu() }
        else button(p, "MENU UTAMA") { menu() }
    }

    private fun instructions() {
        val p = page(Screen.INSTRUCTIONS, "CARA BERMAIN", "Tangkap buah-buahan sahaja.\nElak tangkap selain buah.\n\nSeret bakul ke kiri dan kanan.\nBuah biasa: +10 • Buah bonus: +20\n60 saat • 3 hati\nBarang bukan buah: −1 hati\nHilang semua hati = PERMAINAN TAMAT.", Scene.INSTRUCTIONS, "story_instruction")
        button(p, "MULA") { choose() }
        button(p, "MENU UTAMA", Color.rgb(92, 83, 157)) { menu() }
    }

    private fun choose() {
        val p = page(Screen.BASKETS, "PILIH BAKUL", "Pilih warna bakul.\nBuah sepadan mendapat +20 mata.", Scene.BASKETS, "story_baskets")
        val buttons = mutableMapOf<Basket, Button>()
        Basket.entries.forEach { basket ->
            buttons[basket] = button(p, "${basket.title} • ${basket.fruit}", basket.colour) {
                selected = basket
                buttons.forEach { (colour, b) ->
                    b.text = "${if (colour == selected) "✓ " else ""}${colour.title} • ${colour.fruit}"
                    b.background = GradientDrawable().apply {
                        setColor(colour.colour); cornerRadius = dp(16).toFloat()
                        if (colour == selected) setStroke(dp(3), Color.rgb(255, 207, 64))
                    }
                }
                buttons[basket]?.apply { scaleX = .95f; scaleY = .95f; animate().scaleX(1f).scaleY(1f).setDuration(180).start() }
            }
        }
        buttons[selected]?.performClick()
        button(p, "MULA") { beginCountdown() }
        button(p, "MENU UTAMA", Color.rgb(92, 83, 157)) { menu() }
    }

    private fun beginCountdown() {
        val p = page(Screen.COUNTDOWN, "SEDIA, ${selected.title}!", "Sediakan bakul kamu...", Scene.BRIEFING, "story_countdown")
        countdownLabel = TextView(this).apply { textSize = 58f; gravity = Gravity.CENTER; setTypeface(null, Typeface.BOLD); setTextColor(selected.colour) }
        p.addView(countdownLabel)
        button(p, "MENU UTAMA") { menu() }
        countdownStep = 3; handler.post(countdown)
    }

    private fun startGame() {
        game?.stop(); handler.removeCallbacks(countdown); screen = Screen.GAME; lastHud = ""
        val p = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(cream) }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), 0, dp(12), 0) }
        hud = TextView(this).apply { textSize = 15f; gravity = Gravity.CENTER; setTextColor(Color.rgb(36, 92, 58)); setTypeface(null, Typeface.BOLD) }
        top.addView(hud, LinearLayout.LayoutParams(0, dp(56), 1f))
        top.addView(Button(this).apply { text = "HENTI SEBENTAR"; textSize = 11f; isAllCaps = false; setOnClickListener { pause() } }, LinearLayout.LayoutParams(dp(114), dp(52)))
        p.addView(top)
        game = FruitGameView(this, selected, { state ->
            val label = getString(R.string.game_hud, ceil(state.remaining).toInt(), state.score, "♥".repeat(state.lives) + "♡".repeat(3 - state.lives))
            if (label != lastHud) { hud?.text = label; lastHud = label; hud?.setTextColor(if (state.remaining <= 10) Color.rgb(185, 28, 28) else Color.rgb(36, 92, 58)) }
        }, { feedback -> playTone(feedback) }, { state -> results(state) })
        p.addView(game, LinearLayout.LayoutParams(-1, 0, 1f)); showContent(p)
    }

    private fun pause() {
        val current = game ?: return
        if (current.hasEnded) return
        current.pause()
        if (pauseDialog?.isShowing == true) return
        val controls = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(4), dp(20), dp(20)) }
        val dialog = AlertDialog.Builder(this).setTitle("PERMAINAN DIHENTIKAN SEMENTARA").setView(controls).setCancelable(false).create()
        pauseDialog = dialog
        button(controls, "SAMBUNG") { dialog.dismiss(); current.resume() }
        button(controls, "MULA SEMULA") { dialog.dismiss(); beginCountdown() }
        button(controls, "MENU UTAMA") { dialog.dismiss(); menu() }
        lateinit var soundButton: Button
        soundButton = button(controls, soundLabel()) { toggleSound(); soundButton.text = soundLabel() }
        dialog.show()
    }

    internal fun results(state: GameState) {
        val prefs = getPreferences(0)
        val best = maxOf(state.score, prefs.getInt("best", 0)); prefs.edit().putInt("best", best).apply()
        val failed = state.lives == 0
        playTone(if (failed) Feedback.GAME_OVER else Feedback.SUCCESS)
        val p = page(Screen.RESULT, if (failed) "PERMAINAN TAMAT" else "TAHNIAH!",
            "Skor akhir: ${state.score}\nBuah ditangkap: ${state.fruitsCaught}\nBuah bonus: ${state.bonusCaught}\nKesalahan: ${state.mistakes}\nSkor tertinggi: $best\n\n${if (failed) "BUKAN BUAH! Cuba lagi, kamu boleh!" else "Masa tamat! Jumpa Cikgu di kaunter."}",
            if (failed) Scene.RETRY else Scene.CHECKOUT, if (failed) "story_briefing" else "story_progress")
        button(p, "MAIN LAGI") { beginCountdown() }
        button(p, "MENU UTAMA", Color.rgb(92, 83, 157)) { menu() }
        if (!failed) button(p, "LIHAT PENAMAT") { story(endingFrames, 0, false, true) }
    }

    private fun soundLabel() = if (sound) "BUNYI ON" else "BUNYI OFF"
    private fun toggleSound() { sound = !sound; getPreferences(0).edit().putBoolean("sound", sound).apply(); if (!sound) tone?.stopTone() }
    private fun playTone(feedback: Feedback) {
        if (!sound) return
        val kind = when (feedback) {
            Feedback.FRUIT -> ToneGenerator.TONE_PROP_BEEP
            Feedback.BONUS -> ToneGenerator.TONE_PROP_ACK
            Feedback.WRONG -> ToneGenerator.TONE_PROP_NACK
            Feedback.COUNTDOWN, Feedback.WARNING -> ToneGenerator.TONE_DTMF_1
            Feedback.SUCCESS -> ToneGenerator.TONE_PROP_BEEP2
            Feedback.GAME_OVER -> ToneGenerator.TONE_SUP_ERROR
        }
        tone?.startTone(kind, if (feedback == Feedback.SUCCESS || feedback == Feedback.GAME_OVER) 350 else 120)
    }

    @Suppress("DEPRECATION")
    private fun showContent(view: View) {
        val left = view.paddingLeft; val top = view.paddingTop; val right = view.paddingRight; val bottom = view.paddingBottom
        view.setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val safe = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                v.setPadding(left + safe.left, top + safe.top, right + safe.right, bottom + safe.bottom)
            } else v.setPadding(left + insets.systemWindowInsetLeft, top + insets.systemWindowInsetTop, right + insets.systemWindowInsetRight, bottom + insets.systemWindowInsetBottom)
            insets
        }
        setContentView(view); view.requestApplyInsets()
    }
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    override fun onPause() { game?.pause(); storyVideo?.pause(); handler.removeCallbacks(countdown); super.onPause() }
    override fun onResume() {
        super.onResume()
        if (screen == Screen.COUNTDOWN) { handler.removeCallbacks(countdown); handler.postDelayed(countdown, 1000) }
        else if (game?.isRunning == false) pause()
    }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); game?.stop(); releaseStoryVideo(); pauseDialog?.dismiss(); tone?.release(); super.onDestroy() }
    @Suppress("DEPRECATION") @Deprecated("Legacy back navigation")
    override fun onBackPressed() { when (screen) { Screen.GAME -> pause(); Screen.MENU -> super.onBackPressed(); else -> menu() } }
}
