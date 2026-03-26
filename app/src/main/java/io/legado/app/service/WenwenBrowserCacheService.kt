package io.legado.app.service

import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.BaseService
import io.legado.app.constant.AppConst
import io.legado.app.constant.EventBus
import io.legado.app.constant.IntentAction
import io.legado.app.constant.NotificationId
import io.legado.app.data.appDb
import io.legado.app.model.WenwenBrowserCache
import io.legado.app.ui.book.cache.CacheActivity
import io.legado.app.utils.activityPendingIntent
import io.legado.app.utils.postEvent
import io.legado.app.utils.servicePendingIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import splitties.init.appCtx
import splitties.systemservices.notificationManager
import java.util.concurrent.ConcurrentHashMap

class WenwenBrowserCacheService : BaseService() {

    companion object {
        var isRun = false
            private set
    }

    private val runningJobs = ConcurrentHashMap<String, Job>()
    private var notificationContent = appCtx.getString(R.string.service_starting)
    private val notificationBuilder by lazy {
        val builder = NotificationCompat.Builder(this, AppConst.channelIdDownload)
            .setSmallIcon(R.drawable.ic_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentTitle(getString(R.string.offline_cache))
            .setContentIntent(activityPendingIntent<CacheActivity>("cacheActivity"))
        builder.addAction(
            R.drawable.ic_stop_black_24dp,
            getString(R.string.cancel),
            servicePendingIntent<WenwenBrowserCacheService>(IntentAction.stop)
        )
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    override fun onCreate() {
        super.onCreate()
        isRun = true
        lifecycleScope.launch {
            while (isActive) {
                delay(1000)
                notificationContent = WenwenBrowserCache.downloadSummary
                upCacheBookNotification()
                postEvent(EventBus.UP_DOWNLOAD, "")
                postEvent(EventBus.UP_DOWNLOAD_STATE, "")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                IntentAction.start -> addDownloadData(
                    intent.getStringExtra("bookUrl"),
                    intent.getIntExtra("start", 0),
                    intent.getIntExtra("end", 0)
                )

                IntentAction.remove -> removeDownload(intent.getStringExtra("bookUrl"))
                IntentAction.stop -> stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        isRun = false
        runningJobs.values.forEach { it.cancel() }
        runningJobs.clear()
        WenwenBrowserCache.close()
        super.onDestroy()
        postEvent(EventBus.UP_DOWNLOAD, "")
        postEvent(EventBus.UP_DOWNLOAD_STATE, "")
    }

    private fun addDownloadData(bookUrl: String?, start: Int, end: Int) {
        bookUrl ?: return
        var shouldStartJob = false
        execute {
            val book = appDb.bookDao.getBook(bookUrl) ?: return@execute
            if (!WenwenBrowserCache.isBrowserBook(book)) return@execute
            val cacheBook = WenwenBrowserCache.getOrCreate(book)
            cacheBook.addDownload(start, end)
            shouldStartJob = true
            notificationContent = WenwenBrowserCache.downloadSummary
            upCacheBookNotification()
            postEvent(EventBus.UP_DOWNLOAD_STATE, "")
        }.onFinally {
            if (shouldStartJob) {
                ensureDownload(bookUrl)
            } else if (runningJobs.isEmpty() && !WenwenBrowserCache.isRun) {
                stopSelf()
            }
        }
    }

    private fun ensureDownload(bookUrl: String) {
        val job = runningJobs[bookUrl]
        if (job?.isActive == true) return
        runningJobs[bookUrl] = lifecycleScope.launch(Dispatchers.IO) {
            try {
                WenwenBrowserCache.processBook(bookUrl)
            } finally {
                runningJobs.remove(bookUrl)
                if (runningJobs.isEmpty() && !WenwenBrowserCache.isRun) {
                    stopSelf()
                }
            }
        }
    }

    private fun removeDownload(bookUrl: String?) {
        bookUrl ?: return
        WenwenBrowserCache.cacheBookMap[bookUrl]?.stop()
        if (runningJobs[bookUrl] == null) {
            WenwenBrowserCache.cacheBookMap.remove(bookUrl)
        }
        postEvent(EventBus.UP_DOWNLOAD, "")
        postEvent(EventBus.UP_DOWNLOAD_STATE, "")
        if (runningJobs.isEmpty() && !WenwenBrowserCache.isRun) {
            stopSelf()
        }
    }

    private fun upCacheBookNotification() {
        notificationBuilder.setContentText(notificationContent)
        val notification = notificationBuilder.build()
        notificationManager.notify(NotificationId.WenwenBrowserCacheService, notification)
    }

    override fun startForegroundNotification() {
        notificationBuilder.setContentText(notificationContent)
        val notification = notificationBuilder.build()
        startForeground(NotificationId.WenwenBrowserCacheService, notification)
    }
}
