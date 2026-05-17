package com.inhatc.smartwallsaver.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment //
import com.inhatc.smartwallsaver.R

class SafetyScanFragment : Fragment() { //

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_safety_scan, container, false)
    }
}