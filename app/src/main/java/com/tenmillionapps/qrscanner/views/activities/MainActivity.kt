package com.tenmillionapps.qrscanner.views.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Animatable
import android.graphics.drawable.AnimatedVectorDrawable
import android.hardware.Camera
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.util.Patterns
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.tenmillionapps.qrscanner.R
import com.tenmillionapps.qrscanner.databinding.ActivityMainBinding
import com.tenmillionapps.qrscanner.views.fragments.FragmentSettings


class MainActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val bottomNavigationView: BottomNavigationView = binding.bottomNavigationView
        var bottomSheet: LinearLayout = findViewById(R.id.bottomSheet)
        var grayShade: View = findViewById(R.id.gray_shade)
        bottomNavigationView.selectedItemId = R.id.menu
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                startQrScanner()
            }
        }
        this@MainActivity.onBackPressedDispatcher.addCallback(this, callback)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        // Configure and start QR code scanner
        startQrScanner()
        bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.menu -> {
                    // Handle scan menu item
                    if (bottomSheet.visibility == View.GONE) {
                        bottomSheet.visibility = View.VISIBLE
                        grayShade.visibility = View.VISIBLE
                    } else {
                        bottomSheet.visibility = View.GONE
                        grayShade.visibility = View.GONE
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun startQrScanner() {
        val intent = Intent(this, CustomScannerActivity::class.java)
        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        binding.bottomNavigationView.visibility = BottomNavigationView.GONE


        if (requestCode == 100 && resultCode == RESULT_OK) {
            binding.qrScannerFrame.visibility = FrameLayout.GONE
            val result = data?.getStringExtra("SCAN_RESULT")
            // Handle the result here
            binding.headerTitle.text = "Result: $result"
            binding.cardView1.setOnClickListener {
                val url = if (!result.isNullOrEmpty() && Patterns.WEB_URL.matcher(result).matches()) {
                    result
                } else {
                    "https://www.google.com/search?q=$result"
                }
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(browserIntent)
            }
            binding.cardView2.setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("label", result)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
            binding.cardView3.setOnClickListener {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, result)
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(shareIntent, "Share QR Code Result"))
            }
        }

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Force orientation to portrait if it changes
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    class CustomScannerActivity : AppCompatActivity() {
        private lateinit var barcodeView: DecoratedBarcodeView
        private lateinit var mediaPlayer: MediaPlayer
        private lateinit var vibrator: Vibrator
        private var isFlashOn = false
        private var isUsingFrontCamera = false
        private lateinit var sharedPreferences: SharedPreferences
        private lateinit var bottomSheet: LinearLayout
        private lateinit var grayShade: View




        @RequiresApi(Build.VERSION_CODES.O)
        @SuppressLint("ResourceType", "MissingInflatedId")
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)


            setContentView(R.layout.activity_main) // The layout for the scanner
            val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
            bottomNavigationView.selectedItemId = R.id.menu
            bottomSheet = findViewById(R.id.bottomSheet)
            grayShade = findViewById(R.id.gray_shade)
            grayShade.setOnClickListener {
                bottomNavigationView.visibility = BottomNavigationView.VISIBLE
                bottomSheet.visibility = View.GONE
                grayShade.visibility = View.GONE
            }
            val cancelbtn = findViewById<ImageView>(R.id.cancel_btn)
            cancelbtn.setOnClickListener {
                bottomNavigationView.visibility = BottomNavigationView.VISIBLE
                bottomSheet.visibility = View.GONE
                grayShade.visibility = View.GONE
            }

            sharedPreferences = getSharedPreferences("scanner_preferences", MODE_PRIVATE)

            barcodeView = findViewById(R.id.zxing_barcode_scanner)
            barcodeView.setStatusText("") // Set custom prompt
            mediaPlayer = MediaPlayer.create(this, R.raw.scan_sound)
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

            barcodeView.decodeContinuous { result ->
                if (sharedPreferences.getBoolean("sound", false)) {
                    mediaPlayer.start()
                }

                if (sharedPreferences.getBoolean("vibrate", false)) {
                    vibrate()
                }
                // Handle the result here
                // Optionally, pass the result back to MainActivity
                val intent = Intent()
                intent.putExtra("SCAN_RESULT", result.text)
                setResult(RESULT_OK, intent)
                finish()
            }
            barcodeView.resume() // Start scanning
            val flashButton: ImageButton = findViewById(R.id.flash_button)
            flashButton.setOnClickListener {
                toggleFlashlight()
            }
            val cameraFlipButton: ImageButton = findViewById(R.id.camera_flip_button)
            cameraFlipButton.setOnClickListener {
                flipCamera()
            }
            val galleryButton: ImageButton = findViewById(R.id.gallery_button)
            galleryButton.setOnClickListener {
                openGallery()
            }


            bottomNavigationView.setOnNavigationItemSelectedListener {
                when (it.itemId) {
                    R.id.menu -> {
                        bottomNavigationView.visibility = BottomNavigationView.GONE
                        // Handle scan menu item
                        if (bottomSheet.visibility == View.GONE) {
                            bottomSheet.visibility = View.VISIBLE
                            grayShade.visibility = View.VISIBLE
                        } else {
                            bottomSheet.visibility = View.GONE
                            grayShade.visibility = View.GONE
                        }
                        true
                    }
                    else -> false
                }
            }
            val animatedIcon = findViewById<ImageView>(R.id.icon3)

            val rotationAnimator = ObjectAnimator.ofFloat(animatedIcon, "rotation", 0f, 180f).apply {
                duration = 1000 // 1 second for the rotation
                interpolator = LinearInterpolator()
            }

            val pauseAnimator = ObjectAnimator.ofFloat(animatedIcon, "rotation", 360f, 360f).apply {
                duration = 1000 // 2 seconds pause
            }

            val animatorSet = AnimatorSet().apply {
                playSequentially(rotationAnimator, pauseAnimator)
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        start() // Restart the animation
                    }
                })
            }


            animatorSet.start()

            findViewById<TextView>(R.id.item1).setOnClickListener {
                bottomNavigationView.visibility = BottomNavigationView.VISIBLE
                bottomSheet.visibility = View.GONE
                grayShade.visibility = View.GONE
                supportFragmentManager.beginTransaction()
                            .replace(R.id.qr_scanner_frame, Fragment()).commit()
            }
            findViewById<TextView>(R.id.item2).setOnClickListener {
                bottomNavigationView.visibility = BottomNavigationView.VISIBLE
                bottomSheet.visibility = View.GONE
                grayShade.visibility = View.GONE
                supportFragmentManager.beginTransaction()
                    .replace(R.id.qr_scanner_frame, FragmentSettings()).commit()
            }
        }

        override fun onBackPressed() {
            super.onBackPressed()
            // Close the app when back button is pressed
            finishAffinity()
        }
        override fun onResume() {
            super.onResume()
            barcodeView.resume()
        }
        override fun onPause() {
            super.onPause()
            barcodeView.pause()
        }
        private fun toggleFlashlight() {
            val flashButton: ImageButton = findViewById(R.id.flash_button)
            isFlashOn = !isFlashOn
            if (isFlashOn) {
                flashButton.setImageResource(R.drawable.icons8_flashlight)
                barcodeView.setTorchOn()
            } else {
                flashButton.setImageResource(R.drawable.icons8_flashlight__off)
                barcodeView.setTorchOff()
            }
        }
        private fun flipCamera() {
            isUsingFrontCamera = !isUsingFrontCamera
            val settings = barcodeView.barcodeView.cameraSettings
            settings.requestedCameraId = if (isUsingFrontCamera) {
                Camera.CameraInfo.CAMERA_FACING_FRONT
            } else {
                Camera.CameraInfo.CAMERA_FACING_BACK
            }
            barcodeView.barcodeView.cameraSettings = settings
            barcodeView.pause()
            barcodeView.resume()
        }
        private fun vibrate() {
            if (vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(500)
                }
            }
        }
        private fun openGallery() {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 200)
        }
        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == 200 && resultCode == RESULT_OK) {
                val selectedImageUri = data?.data
                val inputStream = contentResolver.openInputStream(selectedImageUri!!)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val result = decodeQRCode(bitmap)
                if (result != null) {
                    val intent = Intent()
                    intent.putExtra("SCAN_RESULT", result.text)
                    setResult(RESULT_OK, intent)
                    finish()
                }
                else
                {
                    // Handle the case where no QR code was found
                    Toast.makeText(this, "No QR code found in the image", Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun decodeQRCode(bitmap: Bitmap): com.google.zxing.Result? {
            val intArray = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val source = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            return try {
                MultiFormatReader().decode(binaryBitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    }
}
