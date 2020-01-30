package com.example.camerax_video_test

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Camera
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor

@SuppressLint("RestrictedApi")
class MainActivity : AppCompatActivity(), LifecycleOwner {

    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    private val tag = MainActivity::class.java.simpleName

    private lateinit var videoCapture: VideoCapture
    private lateinit var mExecutor: Executor

    private var preview: Preview? = null

    private lateinit var outputDirectory: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mExecutor = ContextCompat.getMainExecutor(this)
        outputDirectory = getOutputDirectory(this)

        /** 삳태바, 네이게이션바 투명  */

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT


        if (allPermissionsGranted()) {
            view_finder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, Companion.REQUEST_CODE_PERMISSIONS)
        }


        capture_button.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {

//                val file = File(externalMediaDirs.first(), "${System.currentTimeMillis()}.mp4")
                val file = createFile(outputDirectory, FILENAME, ViDEO_EXTENSION)

                capture_button.setBackgroundColor(Color.GREEN)

                val metadata = ImageCapture.Metadata().apply {
                    // Mirror image when using the front camera
//                    isReversedHorizontal = lensFacing == CameraX.LensFacing.FRONT
                }

                videoCapture.startRecording(file, mExecutor, object: VideoCapture.OnVideoSavedListener{

                    override fun onVideoSaved(file: File) {

                        Toast.makeText(this@MainActivity, " saved = " + file, Toast.LENGTH_SHORT).show()
                        // Implicit broadcasts will be ignored for devices running API
                        // level >= 24, so if you only target 24+ you can remove this statement
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
//                            sendBroadcast(Intent(Camera.ACTION_NEW_PICTURE, Uri.fromFile(file)))
                            sendBroadcast(Intent(Camera.ACTION_NEW_VIDEO, Uri.fromFile(file)))
                        }

                        // If the folder selected is an external media directory, this is unnecessary
                        // but otherwise other apps will not be able to access our images unless we
                        // scan them using [MediaScannerConnection]
                        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)

                        MediaScannerConnection.scanFile(
                            this@MainActivity, arrayOf(file.absolutePath), arrayOf(mimeType), null)
                    }

                    override fun onError(
                        videoCaptureError: VideoCapture.VideoCaptureError,
                        message: String,
                        cause: Throwable?
                    ) {
                    }

                })

            } else if (event.action == MotionEvent.ACTION_UP) {
                capture_button.setBackgroundColor(Color.RED)
                videoCapture.stopRecording()
                Log.i(tag, "Video File stopped")
            }
            false
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == Companion.REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                view_finder.post { startCamera() }
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    @SuppressLint("RestrictedApi")
    private fun startCamera() {

        // Create configuration object for the viewfinder use case
        val previewConfig = PreviewConfig.Builder().build()
// Build the viewfinder use case
//         preview = Preview(previewConfig)

        val preview = AutoFitPreviewBuilder.build(previewConfig, view_finder)

//        preview.setOnPreviewOutputUpdateListener {
//            view_finder.surfaceTexture = it.surfaceTexture
//        }

        val videoCaptureConfig = VideoCaptureConfig.Builder().apply {
            setTargetRotation(view_finder.display.rotation)
//            setVideoFrameRate(30)
//            setBitRate(3000000)
        }.build()

        videoCapture = VideoCapture(videoCaptureConfig)

// Bind use cases to lifecycle
        CameraX.bindToLifecycle(this, preview, videoCapture)

    }

    companion object {

        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val ViDEO_EXTENSION = ".mp4"

        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() } }
            return if (mediaDir != null && mediaDir.exists())
                mediaDir else appContext.filesDir
        }

        /** Helper function used to create a timestamped file */
        private fun createFile(baseFolder: File, format: String, extension: String) =
            File(baseFolder, SimpleDateFormat(format, Locale.US)
                .format(System.currentTimeMillis()) + extension)
    }


}
