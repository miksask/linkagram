package io.github.miksask.linkagram.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.miksask.linkagram.R
import io.github.miksask.linkagram.data.history.HistoryRepository
import io.github.miksask.linkagram.data.maps.MapUrlParser
import io.github.miksask.linkagram.domain.GeocodeResult
import io.github.miksask.linkagram.domain.HistoryEntry
import io.github.miksask.linkagram.domain.ResolveResult
import io.github.miksask.linkagram.ui.analysis.AnalysisScreen
import io.github.miksask.linkagram.ui.analysis.AnalysisViewModel
import io.github.miksask.linkagram.ui.analysis.HistorySaveNotice
import io.github.miksask.linkagram.ui.history.HistoryDetailsScreen
import io.github.miksask.linkagram.ui.history.HistoryDetailsViewModel
import io.github.miksask.linkagram.ui.history.HistoryScreen
import io.github.miksask.linkagram.ui.history.HistorySnackbar
import io.github.miksask.linkagram.ui.history.HistoryViewModel
import io.github.miksask.linkagram.ui.navigation.LinkagramDestinations
import io.github.miksask.linkagram.ui.settings.SettingsScreen
import io.github.miksask.linkagram.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkagramApp(
    resolveUrl: suspend (String) -> ResolveResult,
    mapUrlParser: MapUrlParser,
    geocode: suspend (String?, String?) -> GeocodeResult,
    historyRepository: HistoryRepository,
    pendingIncomingUrl: String?,
    onIncomingUrlConsumed: () -> Unit,
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute == LinkagramDestinations.ANALYZE ||
        currentRoute == LinkagramDestinations.HISTORY

    val analysisFactory = remember(resolveUrl, mapUrlParser, geocode, historyRepository) {
        AnalysisViewModel.Factory(resolveUrl, mapUrlParser, geocode, historyRepository)
    }
    val analysisViewModel: AnalysisViewModel = viewModel(factory = analysisFactory)
    val historyViewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModel.Factory(historyRepository),
    )

    LaunchedEffect(pendingIncomingUrl) {
        val url = pendingIncomingUrl ?: return@LaunchedEffect
        analysisViewModel.onIncomingUrl(url)
        navController.navigate(LinkagramDestinations.ANALYZE) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
        onIncomingUrlConsumed()
    }

    val historyState by historyViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(historyState.snackbarMessage) {
        when (val message = historyState.snackbarMessage) {
            is HistorySnackbar.DeletedOne -> {
                val result = snackbarHostState.showSnackbar(
                    message = "Analysis deleted",
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    historyViewModel.undoDelete(message.entry)
                }
                historyViewModel.consumeSnackbar()
            }
            is HistorySnackbar.DeletedMany -> {
                snackbarHostState.showSnackbar("Deleted analyses: ${message.count}")
                historyViewModel.consumeSnackbar()
            }
            HistorySnackbar.Restored -> {
                snackbarHostState.showSnackbar("Analysis restored")
                historyViewModel.consumeSnackbar()
            }
            HistorySnackbar.OperationFailed -> {
                snackbarHostState.showSnackbar("History operation failed")
                historyViewModel.consumeSnackbar()
            }
            null -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            currentRoute?.startsWith("history/") == true ->
                                stringResource(R.string.history_details_title)
                            currentRoute == LinkagramDestinations.HISTORY ->
                                stringResource(R.string.history_title)
                            currentRoute == LinkagramDestinations.SETTINGS ->
                                stringResource(R.string.settings_title)
                            else -> stringResource(R.string.app_name)
                        },
                    )
                },
                navigationIcon = {
                    if (!showBottomBar) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    }
                },
                actions = {
                    if (showBottomBar) {
                        IconButton(
                            onClick = {
                                navController.navigate(LinkagramDestinations.SETTINGS)
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.settings),
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == LinkagramDestinations.ANALYZE,
                        onClick = {
                            navController.navigate(LinkagramDestinations.ANALYZE) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = stringResource(R.string.nav_analyze),
                            )
                        },
                        label = { Text(stringResource(R.string.nav_analyze)) },
                    )
                    NavigationBarItem(
                        selected = currentRoute == LinkagramDestinations.HISTORY,
                        onClick = {
                            navController.navigate(LinkagramDestinations.HISTORY) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                Icons.AutoMirrored.Filled.List,
                                contentDescription = stringResource(R.string.nav_history),
                            )
                        },
                        label = { Text(stringResource(R.string.nav_history)) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = LinkagramDestinations.ANALYZE,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(LinkagramDestinations.ANALYZE) {
                AnalysisScreen(
                    viewModel = analysisViewModel,
                    onHistorySaveNotice = { notice ->
                        scope.launch {
                            val text = when (notice) {
                                HistorySaveNotice.Saved -> "Saved to history"
                                HistorySaveNotice.SaveFailed -> "Could not save to history"
                            }
                            snackbarHostState.showSnackbar(text)
                        }
                    },
                )
            }
            composable(LinkagramDestinations.HISTORY) {
                HistoryScreen(
                    viewModel = historyViewModel,
                    onOpenEntry = { id ->
                        navController.navigate(LinkagramDestinations.historyDetails(id))
                    },
                )
            }
            composable(LinkagramDestinations.SETTINGS) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(historyRepository),
                )
                SettingsScreen(viewModel = settingsViewModel)
            }
            composable(
                route = LinkagramDestinations.HISTORY_DETAILS,
                arguments = listOf(
                    navArgument("entryId") { type = NavType.StringType },
                ),
            ) { entry ->
                val entryId = entry.arguments?.getString("entryId").orEmpty()
                val detailsViewModel: HistoryDetailsViewModel = viewModel(
                    factory = HistoryDetailsViewModel.Factory(entryId, historyRepository),
                )
                var pendingUndo by remember { mutableStateOf<HistoryEntry?>(null) }
                LaunchedEffect(pendingUndo) {
                    val deleted = pendingUndo ?: return@LaunchedEffect
                    val result = snackbarHostState.showSnackbar(
                        message = "Analysis deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        historyRepository.restore(deleted)
                    }
                    pendingUndo = null
                }
                HistoryDetailsScreen(
                    viewModel = detailsViewModel,
                    onAnalyzeAgain = { sourceUrl ->
                        analysisViewModel.onIncomingUrl(sourceUrl)
                        analysisViewModel.analyze()
                        navController.navigate(LinkagramDestinations.ANALYZE) {
                            popUpTo(LinkagramDestinations.HISTORY) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onDeleted = { deleted ->
                        pendingUndo = deleted
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
