package com.nukeru

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nukeru.ui.KiraApp
import com.nukeru.ui.theme.KiraAppTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var useDynamicColor by remember { mutableStateOf(true) }
            KiraAppTheme(dynamicColor = useDynamicColor) {
                KiraApp(
                    isDynamicColor = useDynamicColor,
                    onDynamicColorChange = { useDynamicColor = it }
                )
            }
        }
    }
}
