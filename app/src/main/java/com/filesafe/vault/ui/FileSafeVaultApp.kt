package com.filesafe.vault.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.filesafe.vault.ui.screens.AccessKeyScreen
import com.filesafe.vault.ui.screens.AddFileScreen
import com.filesafe.vault.ui.screens.ImportFsfScreen
import com.filesafe.vault.ui.screens.ItemDetailsScreen
import com.filesafe.vault.ui.screens.OnboardingAdminScreen
import com.filesafe.vault.ui.screens.VaultListScreen
import com.filesafe.vault.ui.screens.ViewerScreen
import com.filesafe.vault.viewmodel.AppViewModelFactory
import com.filesafe.vault.viewmodel.OnboardingViewModel

object Routes {
    const val ONBOARDING = "onboarding"
    const val VAULT = "vault"
    const val ADD = "add"
    const val IMPORT = "import"
    const val DETAILS = "details/{id}"
    const val ACCESS = "access/{id}"
    const val VIEWER = "viewer/{id}"
}

@Composable
fun FileSafeVaultApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val factory = AppViewModelFactory(context.applicationContext as Application)
    val onboardingViewModel: OnboardingViewModel = viewModel(factory = factory)
    val hasAdmin by onboardingViewModel.hasAdmin

    NavHost(
        navController = navController,
        startDestination = if (hasAdmin) Routes.VAULT else Routes.ONBOARDING
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingAdminScreen(
                viewModel = onboardingViewModel,
                onComplete = { navController.navigate(Routes.VAULT) { popUpTo(Routes.ONBOARDING) { inclusive = true } } }
            )
        }
        composable(Routes.VAULT) {
            VaultListScreen(
                factory = factory,
                onAdd = { navController.navigate(Routes.ADD) },
                onImport = { navController.navigate(Routes.IMPORT) },
                onOpen = { id -> navController.navigate("details/$id") }
            )
        }
        composable(Routes.ADD) {
            AddFileScreen(factory = factory, onDone = { navController.popBackStack() })
        }
        composable(Routes.IMPORT) {
            ImportFsfScreen(factory = factory, onDone = { navController.popBackStack() })
        }
        composable(
            Routes.DETAILS,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) {
            val id = it.arguments?.getString("id") ?: return@composable
            ItemDetailsScreen(
                factory = factory,
                itemId = id,
                onBack = { navController.popBackStack() },
                onView = { navController.navigate("viewer/$id") },
                onAccessKey = { navController.navigate("access/$id") }
            )
        }
        composable(
            Routes.ACCESS,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) {
            val id = it.arguments?.getString("id") ?: return@composable
            AccessKeyScreen(factory = factory, itemId = id, onBack = { navController.popBackStack() })
        }
        composable(
            Routes.VIEWER,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) {
            val id = it.arguments?.getString("id") ?: return@composable
            ViewerScreen(factory = factory, itemId = id, onBack = { navController.popBackStack() })
        }
    }
}
