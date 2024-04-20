package com.example.myapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.ImageView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.example.myapplication.loginActivity.LoginActivity
import com.example.myapplication.userPreferences.SoundManager

class IntroductoryActivity : AppCompatActivity() {

    private lateinit var logo: ImageView
    private lateinit var splashImg: ImageView
    private lateinit var lottieAnimationView: LottieAnimationView
    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_introductory)
        soundManager = SoundManager(this)
        logo = findViewById(R.id.logo)
        splashImg = findViewById(R.id.img)
        lottieAnimationView = findViewById(R.id.lottie)
        soundManager.playIntro()
        lottieAnimationView.repeatCount = LottieDrawable.INFINITE
        lottieAnimationView.speed = 0.6f

        splashImg.animate().translationY(-3000f).setDuration(1000).setStartDelay(3000)
        logo.animate().translationY(-1650f).setDuration(1000).setStartDelay(2000)
        lottieAnimationView.animate().translationY(3000f).setDuration(2000).setStartDelay(2200)

        // Schedule a task to start LoginActivity after a delay
        Handler(mainLooper).postDelayed({
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()  // Optional: finish the current activity to prevent going back
        }, 3100)  // Delay in milliseconds (3000ms = 3s)
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}