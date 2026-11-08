package fr.sommelle

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.sommelle.core.designsystem.SommelleTheme
import fr.sommelle.core.presentation.AppLinks

class MainActivity : ComponentActivity() {

    /** Écran demandé par une notification ou le widget, consommé une fois la navigation faite. */
    private var pendingLink by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) pendingLink = intent.linkTarget()
        val repository = (application as SommelleApplication).repository
        setContent {
            SommelleTheme {
                SommelleApp(repository, link = pendingLink, onLinkHandled = { pendingLink = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingLink = intent.linkTarget()
    }

    private fun Intent.linkTarget(): String? = getStringExtra(AppLinks.EXTRA_OPEN)
}
