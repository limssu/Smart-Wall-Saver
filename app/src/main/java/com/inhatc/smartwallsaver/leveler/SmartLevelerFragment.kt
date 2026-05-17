package com.inhatc.smartwallsaver.leveler

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.inhatc.smartwallsaver.R

class SmartLevelerFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // fragment_smart_leveler.xml 레이아웃과 연결합니다.
        return inflater.inflate(R.layout.fragment_smart_leveler, container, false)
    }
}