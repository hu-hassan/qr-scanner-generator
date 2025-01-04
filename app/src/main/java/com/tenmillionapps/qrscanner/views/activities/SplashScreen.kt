package com.tenmillionapps.qrscanner.views.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tenmillionapps.qrscanner.databinding.ActivitySplashBinding

class SplashScreen : AppCompatActivity() {
    private val activity = this
    private val binding by lazy {
        ActivitySplashBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Initialize the progress bar
        val progressBar = binding.progressBar
        progressBar.progress = 0

        // Simulate loading process
        Thread {
            for (i in 1..100) {
                Thread.sleep(10) // Reduce sleep time for rapid progress
                progressBar.progress = i
            }
            // Navigate to MainActivity when progress reaches 100
            val intent = Intent(activity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }.start()
    }
}