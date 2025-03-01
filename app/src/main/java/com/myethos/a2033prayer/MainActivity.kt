package com.myethos.a2033prayer

import android.Manifest
import android.app.AlarmManager
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
    private val handler = Handler(Looper.getMainLooper())
    private var countdownRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        testNotifications()
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

        // Register initial countdown trigger receiver
        registerReceiver(
            initialCountdownReceiver,
            IntentFilter("PRAYER_COUNTDOWN_TRIGGER"), Context.RECEIVER_NOT_EXPORTED
        )
    }

    private fun scheduleDailyReminder() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent("PRAYER_COUNTDOWN_TRIGGER")
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

        // FOR TESTING - You can use this to trigger the notification immediately
        // startCountdown()
    }

    // This receiver is triggered exactly at 20:30 to start the countdown
    private val initialCountdownReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Start the countdown process
            startCountdown()
        }
    }

    private fun startCountdown() {
        // Cancel any existing countdown
        stopCountdown()

        // Create and start a new countdown
        countdownRunnable = object : Runnable {
            override fun run() {
                updateCountdown()
                // Schedule next update in 1 second
                handler.postDelayed(this, 1000)
            }
        }

        // Start the countdown immediately
        handler.post(countdownRunnable!!)
    }

    private fun stopCountdown() {
        countdownRunnable?.let {
            handler.removeCallbacks(it)
            countdownRunnable = null
        }
    }

    private fun updateCountdown() {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentSecond = calendar.get(Calendar.SECOND)

        when {
            // At exactly 20:33, show the prayer notification
            currentHour == 20 && currentMinute == 33 && currentSecond == 0 -> {
                notificationHelper.showPrayerNotification()
                // Stop the countdown updates
                stopCountdown()
            }

            // Between 20:30 and 20:32:59, show and update the countdown
            currentHour == 20 && currentMinute in 30..32 -> {
                // Calculate total seconds remaining until 20:33:00
                val targetCalendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 20)
                    set(Calendar.MINUTE, 33)
                    set(Calendar.SECOND, 0)
                }

                val totalSecondsRemaining = (targetCalendar.timeInMillis - calendar.timeInMillis) / 1000
                val minutesRemaining = totalSecondsRemaining / 60
                val secondsRemaining = totalSecondsRemaining % 60

                notificationHelper.showCountdownNotification(minutesRemaining.toInt(), secondsRemaining.toInt())
            }

            // If outside the time window, stop the countdown
            else -> {
                stopCountdown()
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
        stopCountdown()
        unregisterReceiver(initialCountdownReceiver)
    }

    // Add this method for debugging/testing
    fun testNotifications() {
        startCountdown()
    }
}