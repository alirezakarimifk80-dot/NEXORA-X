package com.nexora.x

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexora.x.core.design.theme.NexoraTheme
import com.nexora.x.core.navigation.NexoraAppScaffold
import com.nexora.x.domain.repository.ProfileRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * MainActivity hosts the single-activity Jetpack Compose navigation graph and observes
 * the user's DataStore theme preference (Dark / Light).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var profileRepository: ProfileRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkTheme by profileRepository.isDarkTheme.collectAsStateWithLifecycle(initialValue = true)
            NexoraTheme(darkTheme = isDarkTheme) {
                NexoraAppScaffold(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
