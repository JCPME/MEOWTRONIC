package com.example.meowtronic

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class FinishedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_finished)
        // That’s it. Just show "FINISHED."
    }
    override fun onDestroy() {
        super.onDestroy()

        // Or delete the entire folder if it's empty
        val tempDir = File(cacheDir, "tempCaptures")
        tempDir.deleteRecursively()
    }
}