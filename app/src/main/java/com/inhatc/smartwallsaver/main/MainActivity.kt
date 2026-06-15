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

        // 1. 하단 네비게이션 리스너를 먼저 연결합니다.
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

        // 2. [수정] 앱 실행 시 하단 바의 선택 아이템을 '레벨러'로 강제 지정
        // 이 코드가 실행되면서 위의 리스너가 자동으로 동작해 첫 화면 프래그먼트까지 한 번에 로드
        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.navigation_leveler
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
