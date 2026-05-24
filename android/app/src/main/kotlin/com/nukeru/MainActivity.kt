package com.nukeru

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nukeru.ui.NukeruApp
import com.nukeru.ui.theme.NukeruAppTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var useDynamicColor by remember { mutableStateOf(true) }
            var selectedColorIndex by remember { mutableIntStateOf(0) }
            
            NukeruAppTheme(
                dynamicColor = useDynamicColor,
                colorIndex = selectedColorIndex
            ) {
                NukeruApp(
                    isDynamicColor = useDynamicColor,
                    onDynamicColorChange = { useDynamicColor = it },
                    selectedColorIndex = selectedColorIndex,
                    onColorIndexChange = { selectedColorIndex = it }
                )
            }
        }
    }
}
