package com.ioristudios.crossdroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.ioristudios.crossdroid.ui.CrossDroidViewModel
import com.ioristudios.crossdroid.ui.Screen
import com.ioristudios.crossdroid.ui.components.AppSidebar
import com.ioristudios.crossdroid.ui.components.BottomNavbar
import com.ioristudios.crossdroid.ui.navigation.NavigationHost
import com.ioristudios.crossdroid.ui.theme.BgMain
import com.ioristudios.crossdroid.ui.theme.CrossDroidTheme

class MainActivity : ComponentActivity() {
    private val viewModel: CrossDroidViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Turn on edge-to-edge system display values
        enableEdgeToEdge()
        
        setContent {
            CrossDroidTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()
                val isSidebarVisible by viewModel.isSidebarVisible.collectAsState()
                
                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        bottomBar = {
                            // Persistent navbar only on the three primary hubs
                            if (currentScreen in listOf(Screen.HOME, Screen.DEVICES, Screen.HISTORY)) {
                                BottomNavbar(viewModel = viewModel)
                            }
                        },
                        containerColor = BgMain,
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(BgMain)
                                .padding(innerPadding)
                        ) {
                            NavigationHost(viewModel = viewModel)
                        }
                    }

                    AppSidebar(
                        isVisible = isSidebarVisible,
                        onDismiss = { viewModel.setSidebarVisible(false) },
                        onAboutClick = {
                            viewModel.setSidebarVisible(false)
                            viewModel.navigateTo(Screen.ABOUT)
                        }
                    )
                }
            }
        }
    }
}
