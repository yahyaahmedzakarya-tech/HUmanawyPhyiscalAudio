package com.example.humanawyphyiscalaudio

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val svc = Intent(this, ReceiverService::class.java)

        val start = Button(this)
        start.text = "Start receiver"
        start.setOnClickListener { startService(svc) }

        val stop = Button(this)
        stop.text = "Stop receiver"
        stop.setOnClickListener { stopService(svc) }

        layout.addView(start)
        layout.addView(stop)
        setContentView(layout)
    }
}