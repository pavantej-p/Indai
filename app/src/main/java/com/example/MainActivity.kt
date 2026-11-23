package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ai.configureAppCheck
import com.example.data.model.UserMode
import com.example.data.repository.authStateFlow
import com.example.ui.screens.AppShell
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.IndiAiGuildTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            IndiAiGuildTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    var preferredInitialMode by remember { mutableStateOf(UserMode.CREATOR) }

    if (user == null) {
        AuthScreen(
            selectedInitialMode = preferredInitialMode,
            onSelectInitialMode = { preferredInitialMode = it },
            onAuthSuccess = {}
        )
    } else {
        key(user.uid) {
            AppShell(
                currentUserId = user.uid,
                initialMode = preferredInitialMode
            )
        }
    }
}
