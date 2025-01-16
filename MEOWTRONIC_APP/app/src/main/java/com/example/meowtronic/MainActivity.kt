package com.example.meowtronic

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val calibrationButton = findViewById<Button>(R.id.btnCalibration)
        calibrationButton.setOnClickListener {
            // Launch DeviceListActivity
            val intent = Intent(this, DeviceListActivity::class.java)
            startActivity(intent)
        }
    }
}
