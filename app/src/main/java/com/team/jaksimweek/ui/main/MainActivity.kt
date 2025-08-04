package com.team.jaksimweek.ui.main

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.team.jaksimweek.R
import com.team.jaksimweek.databinding.ActivityMainBinding
import com.team.jaksimweek.ui.auth.LoginActivity
import com.team.jaksimweek.ui.challenge.AddPostActivity
import com.team.jaksimweek.ui.chat.ChatFragment
import com.team.jaksimweek.ui.home.HomeFragment
import com.team.jaksimweek.ui.profile.ProfileFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 초기 화면을 HomeFragment로 설정
        replaceFragment(HomeFragment()) // HomeFragment는 임시로 만들어야 함

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.navigation_chat -> {
                    replaceFragment(ChatFragment()) // ChatFragment는 임시로 만들어야 함
                    true
                }
                R.id.navigation_post -> {
                    // 포스팅은 보통 새 액티비티를 띄움
                     startActivity(Intent(this, AddPostActivity::class.java))
                    true
                }
                R.id.navigation_profile -> {
                    replaceFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_fragment_container, fragment)
            .commit()
    }
}