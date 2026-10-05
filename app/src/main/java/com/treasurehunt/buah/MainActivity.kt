package com.treasurehunt.buah

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.media.AudioManager
import android.media.ToneGenerator
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private var selected = Basket.GREEN
    private var game: FruitGameView? = null
    private var hud: TextView? = null
    private var tone: ToneGenerator? = null
    private var sound = true
    private var pauseDialog: AlertDialog? = null
    private val cream = Color.rgb(255,245,207)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sound = getPreferences(0).getBoolean("sound", true)
        tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 65) }.getOrNull()
        volumeControlStream = AudioManager.STREAM_MUSIC
        intro()
    }
    private fun page(title: String, subtitle: String): LinearLayout {
        game?.stop(); game = null
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(24),dp(20),dp(24),dp(20)); setBackgroundColor(cream)
            addView(TextView(this@MainActivity).apply { text = title; textSize = 32f; gravity = Gravity.CENTER; setTextColor(Color.rgb(36,92,58)); setTypeface(null, Typeface.BOLD) })
            addView(TextView(this@MainActivity).apply { text = subtitle; textSize = 19f; gravity = Gravity.CENTER; setTextColor(Color.DKGRAY); setPadding(0,dp(20),0,dp(24)) })
            val scroll = ScrollView(this@MainActivity); scroll.isFillViewport = true; scroll.addView(this)
            showContent(scroll)
        }
    }
    private fun button(parent: LinearLayout, label: String, colour: Int = Color.rgb(0,121,107), action: () -> Unit): Button {
        val b = Button(this).apply {
            text = label; textSize = 19f; isAllCaps = false; setTextColor(Color.WHITE)
            background = GradientDrawable().apply { setColor(colour); cornerRadius = dp(20).toFloat() }
            setOnClickListener { action() }
        }
        parent.addView(b, LinearLayout.LayoutParams(-1,dp(64)).apply { topMargin = dp(12) })
        return b
    }
    private fun intro() {
        val p = page("🍎 Treasure Hunt\nBuah-Buahan", "Selamat datang!\nExplore a colourful Malaysian supermarket and collect delicious fruit.")
        button(p,"Let’s explore! →") { menu() }
    }
    private fun menu() {
        val p = page("Pasar Ceria 🛒", "Catch fruit with your basket!\nFruit: +10 points\nMatching colour fruit: +25 points\nAvoid tins: they cost one life.\n3 lives • 60 seconds")
        button(p,"Play • Choose basket") { choose() }
        lateinit var soundButton: Button
        soundButton = button(p, soundLabel()) { toggleSound(); soundButton.text = soundLabel() }
    }
    private fun choose() {
        val p = page("Choose your basket", "Your basket’s matching fruit earns bonus points. Tap a basket to start!")
        Basket.entries.forEach { basket -> button(p,"${basket.title} • ${basket.fruit} +25",basket.colour) { selected = basket; startGame() } }
        button(p,"← Main menu") { menu() }
    }
    private fun startGame() {
        game?.stop()
        val p = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(cream) }
        hud = TextView(this).apply { textSize = 20f; gravity = Gravity.CENTER; setTextColor(Color.rgb(36,92,58)); setPadding(0,dp(12),0,dp(8)); setTypeface(null,Typeface.BOLD); accessibilityLiveRegion = android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE }
        p.addView(hud)
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(8),0,dp(8),dp(8)) }
        fun control(label: String, action: (Button) -> Unit) {
            controls.addView(Button(this).apply { text = label; isAllCaps = false; textSize = 14f; minHeight = dp(48); setOnClickListener { action(this) } },LinearLayout.LayoutParams(0,dp(56),1f))
        }
        control("Pause") { pause() }
        control("Restart") { confirmRestart() }
        control(soundLabel()) { toggleSound(); it.text = soundLabel() }
        p.addView(controls)
        game = FruitGameView(this,selected, { state -> hud?.text = "★ ${state.score}   ⏱ ${kotlin.math.ceil(state.remaining).toInt()}s   ♥ ${state.lives}" }, { good ->
            if(sound) tone?.startTone(if(good) ToneGenerator.TONE_PROP_BEEP else ToneGenerator.TONE_PROP_NACK,100)
        }, { state -> results(state) })
        p.addView(game,LinearLayout.LayoutParams(-1,0,1f)); showContent(p)
    }
    private fun pause() {
        val current = game ?: return
        current.pause()
        if (pauseDialog?.isShowing == true) return
        pauseDialog = AlertDialog.Builder(this).setTitle("Taking a break? 🍊")
            .setMessage("Your timer is paused. Ready to collect more fruit?")
            .setPositiveButton("Resume") { _,_ -> current.resume() }
            .setNegativeButton("Main menu") { _,_ -> menu() }
            .setNeutralButton("Restart") { _,_ -> startGame() }
            .setCancelable(false).show()
    }
    private fun confirmRestart() {
        val current = game ?: return
        current.pause()
        AlertDialog.Builder(this).setTitle("Start a fresh hunt?").setMessage("Your current score will be reset.")
            .setPositiveButton("Restart") { _,_ -> startGame() }
            .setNegativeButton("Keep playing") { _,_ -> current.resume() }
            .setOnCancelListener { current.resume() }.show()
    }
    private fun results(state: GameState) {
        val prefs = getPreferences(0)
        val best = maxOf(state.score,prefs.getInt("best",0)); prefs.edit().putInt("best",best).apply()
        val p = page("Great fruit hunt! 🌟", "${if(state.lives == 0) "All baskets need a little rest!" else "Time’s up!"}\n\nYour score: ${state.score}\nBest score: $best\nBasket: ${selected.title}\n\nTerima kasih for playing!")
        button(p,"Play again") { startGame() }; button(p,"Choose another basket") { choose() }; button(p,"Main menu") { menu() }
    }
    private fun soundLabel() = if(sound) "Sound: On" else "Sound: Off"
    private fun toggleSound() { sound = !sound; getPreferences(0).edit().putBoolean("sound",sound).apply() }
    private fun showContent(view: View) {
        val left = view.paddingLeft; val top = view.paddingTop
        val right = view.paddingRight; val bottom = view.paddingBottom
        view.setOnApplyWindowInsetsListener { v, insets ->
            v.setPadding(left + insets.systemWindowInsetLeft, top + insets.systemWindowInsetTop,
                right + insets.systemWindowInsetRight, bottom + insets.systemWindowInsetBottom)
            insets
        }
        setContentView(view)
        view.requestApplyInsets()
    }
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    override fun onPause() { super.onPause(); if(game != null) pause() }
    override fun onDestroy() { game?.stop(); pauseDialog?.dismiss(); tone?.release(); super.onDestroy() }
    @Deprecated("Legacy back navigation") override fun onBackPressed() { if(game != null) pause() else menu() }
}
