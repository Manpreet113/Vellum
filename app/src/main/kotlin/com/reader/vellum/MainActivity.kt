package com.reader.vellum

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import java.net.URLEncoder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.reader.vellum.ui.screens.library.LibraryScreen
import com.reader.vellum.ui.screens.reader.ReaderScreen
import com.reader.vellum.ui.theme.VellumTheme
import com.reader.vellum.util.HardwareEvent
import com.reader.vellum.util.HardwareEventManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var hardwareEventManager: HardwareEventManager
    
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        handleIntent(intent)
        
        setContent {
            VellumTheme {
                VellumApp(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                viewModel.handleIntentUri(uri)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                hardwareEventManager.emitEvent(HardwareEvent.VOLUME_UP)
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                hardwareEventManager.emitEvent(HardwareEvent.VOLUME_DOWN)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
fun VellumApp(mainViewModel: MainViewModel) {
    val navController = rememberNavController()

    LaunchedEffect(mainViewModel) {
        mainViewModel.navigateToReader.collectLatest { bookId ->
            val encodedId = URLEncoder.encode(bookId, "UTF-8")
            navController.navigate("reader/$encodedId") {
                popUpTo("library") { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    NavHost(navController = navController, startDestination = "library") {
        composable("library") {
            LibraryScreen(
                viewModel = hiltViewModel(),
                onBookClick = { bookId ->
                    val encodedId = URLEncoder.encode(bookId, "UTF-8")
                    navController.navigate("reader/$encodedId")
                }
            )
        }
        composable(
            route = "reader/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: return@composable
            ReaderScreen(
                id = bookId,
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() }
            )
        }
    }
}
