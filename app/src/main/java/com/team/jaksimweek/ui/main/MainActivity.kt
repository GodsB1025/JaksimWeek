package com.team.jaksimweek.ui.main

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.team.jaksimweek.R
import com.team.jaksimweek.databinding.ActivityMainBinding
import com.team.jaksimweek.ui.challenge.AddPostActivity
import com.team.jaksimweek.ui.home.HomeFragment
import com.team.jaksimweek.ui.chat.ChatFragment
import com.team.jaksimweek.ui.profile.ProfileFragment
import com.team.jaksimweek.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.navigation_chat -> {
                    replaceFragment(ChatFragment())
                    true
                }
                R.id.navigation_post -> {
                    startActivity(Intent(this, AddPostActivity::class.java))
                    false
                }
                R.id.navigation_profile -> {
                    replaceFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }

        lifecycleScope.launch {
            viewModel.totalUnreadCount.collect { count ->
                updateChatBadge(count)
            }
        }

        lifecycleScope.launch {
            viewModel.newMessageEvent.collect { (room, message) ->
                if (room.roomId != viewModel.currentChatRoomId.value) {
                    val senderName = if (room.type == "1on1") room.partnerNickname else message.senderNickname
                    val messageText = message.text ?: "사진"
                    showInAppNotification(senderName ?: "알 수 없음", messageText)
                }
            }
        }
    }

    private fun showInAppNotification(sender: String, message: String) {
        val notificationView = LayoutInflater.from(this).inflate(R.layout.in_app_notification, binding.notificationContainer, false)
        val tvSender = notificationView.findViewById<TextView>(R.id.tv_sender)
        val tvMessage = notificationView.findViewById<TextView>(R.id.tv_message)

        tvSender.text = sender
        tvMessage.text = message

        binding.notificationContainer.addView(notificationView)

        val slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down)
        notificationView.startAnimation(slideDown)

        Handler(Looper.getMainLooper()).postDelayed({
            val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
            notificationView.startAnimation(slideUp)
            slideUp.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(animation: android.view.animation.Animation?) {}
                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                    binding.notificationContainer.removeView(notificationView)
                }
                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
            })
        }, 1000)
    }


    override fun onResume() {
        super.onResume()
        val currentFragment = supportFragmentManager.findFragmentById(R.id.main_fragment_container)
        if (currentFragment is HomeFragment) {
            binding.bottomNavigation.selectedItemId = R.id.navigation_home
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_fragment_container, fragment)
            .commit()
    }

    fun updateChatBadge(count: Int) {
        val badge = binding.bottomNavigation.getOrCreateBadge(R.id.navigation_chat)
        badge.isVisible = count > 0
        badge.number = count
    }
}