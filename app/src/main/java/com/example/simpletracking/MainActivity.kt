package com.example.simpletracking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.simpletracking.presentation.map.MapRoute
import com.example.simpletracking.presentation.theme.SimpleTrackingTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * @AndroidEntryPoint: 이 Activity(와 그 안의 hiltViewModel())가 Hilt 그래프에서 의존성을 주입받을 수 있게 한다.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleTrackingTheme {
                MapRoute()
            }
        }
    }
}
