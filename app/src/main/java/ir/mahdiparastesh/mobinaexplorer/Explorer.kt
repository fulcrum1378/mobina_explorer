package ir.mahdiparastesh.mobinaexplorer

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData

@SuppressLint("UnspecifiedImmutableFlag")
class Explorer : Service() {
    private lateinit var c: Context
    lateinit var crawler: Crawler
    lateinit var analyzer: Analyzer
    @Suppress("MemberVisibilityCanBePrivate")
    var handler: Handler? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action != null) when (intent.action) {
            code(Code.RESUME) -> crawler.running = true
            code(Code.PAUSE) -> crawler.running = false
            code(Code.STOP) -> {
                stopForeground(true)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        active.value = true
        c = applicationContext

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(
                NotificationChannel(
                    code(Code.CHANNEL), c.resources.getString(R.string.notif_channel),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = c.resources.getString(R.string.notif_channel_desc) })
        startForeground(CH_ID, NotificationCompat.Builder(c, code(Code.CHANNEL)).apply {
            setSmallIcon(R.mipmap.launcher_round)
            setContentTitle(c.resources.getString(R.string.notif_title))
            setOngoing(true)
            priority = NotificationCompat.PRIORITY_DEFAULT
            setContentIntent(
                PendingIntent.getActivity(
                    c, 0, Intent(c, Panel::class.java), PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            addAction(0, c.resources.getString(R.string.notif_stop), pi(c, Code.STOP))
        }.build())

        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                //when (msg.what) { }
            }
        }
        analyzer = Analyzer()
        crawler = Crawler(this).also { it.start() }
    }

    override fun onDestroy() {
        active.value = false
        handler = null
        crawler.interrupt()
        super.onDestroy()
        System.gc()
    }

    override fun onBind(intent: Intent?): IBinder? = null


    companion object {
        const val CH_ID = 103
        val active = MutableLiveData(false)

        fun code(what: Code) = Explorer::class.java.`package`!!.name + "." + when (what) {
            Code.CHANNEL -> "EXPLORING"
            Code.RESUME -> "ACTION_RESUME"
            Code.PAUSE -> "ACTION_PAUSE"
            Code.STOP -> "ACTION_STOP"
        }

        fun pi(c: Context, code: Code): PendingIntent = PendingIntent.getService(
            c, 0, Intent(c, Explorer::class.java).apply { action = code(code) },
            PendingIntent.FLAG_CANCEL_CURRENT
        )
    }

    enum class Code { CHANNEL, RESUME, PAUSE, STOP }

    //enum class Action { }
}
