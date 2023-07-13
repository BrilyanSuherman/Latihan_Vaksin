package com.example.vaksin_firebase

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.vectordrawable.graphics.drawable.AnimationUtilsCompat

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activiy_splash)

        val backgroundImg : ImageView = findViewById(R.id.logo)
        val fadeAnimation = AnimationUtils.loadAnimation(this, R.anim.fade)
        backgroundImg.startAnimation(fadeAnimation)

        Handler().postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 3000)

    }
}