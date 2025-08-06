package com.team.jaksimweek.ui.main

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.team.jaksimweek.R
import com.team.jaksimweek.databinding.ActivityMainBinding
import com.team.jaksimweek.ui.challenge.AddPostActivity
import com.team.jaksimweek.ui.home.HomeFragment
import com.team.jaksimweek.ui.chat.ChatFragment
import com.team.jaksimweek.ui.profile.ProfileFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

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
}