package com.talhastudio.callerid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.talhastudio.callerid.ui.AppNav
import com.talhastudio.callerid.ui.theme.TalhaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TalhaTheme { AppNav() } }
    }
}
