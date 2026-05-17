package com.inhatc.smartwallsaver.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.inhatc.smartwallsaver.R
import com.inhatc.smartwallsaver.leveler.SmartLevelerFragment
import com.inhatc.smartwallsaver.scan.SafetyScanFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        // 앱 실행 시 바로 수평계 화면이 첫화면으로 (test진행중)
        if (savedInstanceState == null) {
            loadFragment(SmartLevelerFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_scan -> {
                    loadFragment(SafetyScanFragment())
                    true
                }
                R.id.navigation_leveler -> {
                    loadFragment(SmartLevelerFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}