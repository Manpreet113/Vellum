package com.reader.vellum

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
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
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var hardwareEventManager: HardwareEventManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VellumTheme {
                VellumApp()
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
fun VellumApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "library") {
        composable("library") {
            LibraryScreen(
                viewModel = hiltViewModel(),
                onBookClick = { bookId ->
                    navController.navigate("reader/$bookId")
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
