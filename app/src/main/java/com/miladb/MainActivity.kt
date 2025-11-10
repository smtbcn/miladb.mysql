package com.miladb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.miladb.ui.navigation.MilaDbNavGraph
import com.miladb.ui.theme.MilaDbTheme

/**
 * Ana Activity.
 * 
 * Uygulama giriş noktası.
 * Compose UI'ı başlatır ve navigasyon sistemini kurar.
 * Splash screen desteği ile başlar.
 * System bar renklerini ayarlar.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash screen'den normal theme'e geç
        setTheme(R.style.Theme_MilaDB)
        
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge modu etkinleştir
        enableEdgeToEdge()
        
        setContent {
            MilaDbTheme {
                // System bar ve window background renklerini ayarla
                val darkBlue = Color(0xFF003258) // Koyu mavi (MySQL rengi)
                val backgroundColor = MaterialTheme.colorScheme.background
                val isDarkTheme = isSystemInDarkTheme()
                
                SideEffect {
                    // Status bar rengini koyu mavi yap (hem açık hem karanlık temada)
                    window.statusBarColor = darkBlue.toArgb()
                    
                    // Navigation bar rengini background (tema rengi) yap
                    window.navigationBarColor = backgroundColor.toArgb()
                    
                    // Window background'ı ayarla (klavye açılırken flash önleme)
                    window.setBackgroundDrawable(
                        android.graphics.drawable.ColorDrawable(backgroundColor.toArgb())
                    )
                    
                    // Status bar icon'larını beyaz yap (koyu mavi arka plan için)
                    // Navigation bar icon'larını tema rengine göre ayarla
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = false // Status bar: beyaz icon'lar (her zaman)
                        isAppearanceLightNavigationBars = !isDarkTheme // Navigation bar: tema göre
                    }
                }
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    MilaDbNavGraph(
                        navController = navController,
                        context = this
                    )
                }
            }
        }
    }
}
