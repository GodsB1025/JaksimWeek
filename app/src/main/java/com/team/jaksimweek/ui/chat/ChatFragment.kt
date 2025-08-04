package com.team.jaksimweek.ui.chat

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.team.jaksimweek.R

class ChatFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // fragment_chat.xml 레이아웃을 화면에 표시합니다.
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }
}