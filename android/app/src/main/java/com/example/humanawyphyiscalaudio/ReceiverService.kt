package com.example.humanawyphyiscalaudio

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.IBinder
import android.os.PowerManager
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class ReceiverService : Service() {
    @Volatile private var running = false
    private var server: ServerSocket? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (running) return START_STICKY
        running = true

        val n = Notification.Builder(this)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("HUmanawyPhyiscalAudio")
            .setContentText("Listening on port 5000")
            .build()
        startForeground(1, n)

        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HUmanawyAudio")
        wakeLock?.acquire()

        Thread { serve() }.start()
        return START_STICKY
    }

    private fun serve() {
        try {
            val ss = ServerSocket()
            ss.reuseAddress = true
            ss.bind(InetSocketAddress(5000))
            server = ss
            while (running) {
                val s = ss.accept()
                try { handle(s) } catch (e: IOException) { }
                try { s.close() } catch (e: IOException) { }
            }
        } catch (e: IOException) { }
    }

    private fun handle(s: Socket) {
        s.tcpNoDelay = true
        val input = DataInputStream(BufferedInputStream(s.getInputStream(), 8192))
        val rate = input.readInt()
        val channels = input.readInt()
        input.readInt() // bits per sample, always 16

        val cfg = if (channels == 1) AudioFormat.CHANNEL_OUT_MONO
        else AudioFormat.CHANNEL_OUT_STEREO
        val minBuf = AudioTrack.getMinBufferSize(rate, cfg, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack(
            AudioManager.STREAM_MUSIC, rate, cfg,
            AudioFormat.ENCODING_PCM_16BIT, minBuf * 3, AudioTrack.MODE_STREAM
        )
        track.play()

        val buf = ByteArray(2048)
        try {
            while (running) {
                val n = input.read(buf)
                if (n < 0) break
                track.write(buf, 0, n)
            }
        } finally {
            track.stop()
            track.release()
        }
    }

    override fun onDestroy() {
        running = false
        try { server?.close() } catch (e: IOException) { }
        wakeLock?.release()
        super.onDestroy()
    }
}