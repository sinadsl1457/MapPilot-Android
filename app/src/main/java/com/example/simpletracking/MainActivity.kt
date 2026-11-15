package com.example.simpletracking

import androidx.activity.SystemBarStyle
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.simpletracking.presentation.map.MapRoute
import com.example.simpletracking.presentation.theme.MapPilotTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * @AndroidEntryPoint: 이 Activity(와 그 안의 hiltViewModel())가 Hilt 그래프에서 의존성을 주입받을 수 있게 한다.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 앱이 항상 라이트라 상태 표시줄·내비게이션 바 아이콘도 폰 다크모드와 상관없이 어둡게(밝은 지도 위에서 보이게).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            MapPilotTheme {
                MapRoute()
            }
        }
    }
}
