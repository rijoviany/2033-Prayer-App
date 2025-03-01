package com.myethos.a2033prayer

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import com.myethos.a2033prayer.databinding.ActivityMainBinding
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var notificationHelper: NotificationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }

        notificationHelper = NotificationHelper(this)

        // Request notification permission for Android 13+
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        scheduleDailyReminder()

        // Register countdown receiver
        registerReceiver(
            countdownReceiver,
            IntentFilter("PRAYER_COUNTDOWN"), RECEIVER_NOT_EXPORTED
        )
    }

    private fun scheduleDailyReminder() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent("PRAYER_COUNTDOWN")
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set alarm for 20:30 every day
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)

            // If time has already passed today, set for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private val countdownReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            val currentSecond = calendar.get(Calendar.SECOND)

            when {
                // At exactly 20:33, show the prayer notification
                currentHour == 20 && currentMinute == 33 && currentSecond == 0 -> {
                    notificationHelper.showPrayerNotification()

                    // Schedule a handler to remove the notification after a reasonable time if not clicked
                    Handler(Looper.getMainLooper()).postDelayed({
                        // This would just cancel if already dismissed
                        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                            .cancel(1002) // Using the PRAYER_NOTIFICATION_ID from NotificationHelper
                    }, 15 * 60 * 1000) // 15 minutes
                }

                // Between 20:30 and 20:33, show and update the countdown
                currentHour == 20 && currentMinute in 30..32 -> {
                    val remainingMinutes = 32 - currentMinute
                    val remainingSeconds = 59 - currentSecond
                    notificationHelper.showCountdownNotification(remainingMinutes, remainingSeconds)

                    // Schedule next update in 1 second
                    Handler(Looper.getMainLooper()).postDelayed({
                        sendBroadcast(Intent("PRAYER_COUNTDOWN"))
                    }, 1000)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(countdownReceiver)
    }
}