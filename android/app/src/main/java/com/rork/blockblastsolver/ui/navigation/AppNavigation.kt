package com.rork.blockblastsolver.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rork.blockblastsolver.ui.components.AppTopBar
import com.rork.blockblastsolver.ui.components.HelpSheetContent
import com.rork.blockblastsolver.ui.components.brandTitle
import com.rork.blockblastsolver.ui.history.HistoryScreen
import com.rork.blockblastsolver.ui.history.HistoryViewModel
import com.rork.blockblastsolver.ui.review.ReviewScreen
import com.rork.blockblastsolver.ui.scan.ScanScreen
import com.rork.blockblastsolver.ui.solution.SolutionScreen
import com.rork.blockblastsolver.ui.solver.SolverEvent
import com.rork.blockblastsolver.ui.solver.SolverViewModel
import com.rork.blockblastsolver.ui.stats.StatsScreen
import com.rork.blockblastsolver.ui.theme.Canvas as CanvasColor
import com.rork.blockblastsolver.ui.theme.NeonTeal
import com.rork.blockblastsolver.ui.theme.SurfaceElevated
import com.rork.blockblastsolver.ui.theme.TextSecondary

private object Routes {
    const val SCAN = "scan"
    const val HISTORY = "history"
    const val STATS = "stats"
    const val REVIEW = "review"
    const val SOLUTION = "solution"
}

private data class TabItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val tabs = listOf(
    TabItem(Routes.SCAN, "Scan", Icons.Rounded.PhotoCamera),
    TabItem(Routes.HISTORY, "Riwayat", Icons.Rounded.History),
    TabItem(Routes.STATS, "Statistik", Icons.Rounded.BarChart)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val solverViewModel: SolverViewModel = viewModel()
    val historyViewModel: HistoryViewModel = viewModel()

    val solverState by solverViewModel.state.collectAsStateWithLifecycle()
    val historyState by historyViewModel.state.collectAsStateWithLifecycle()
    val stats by historyViewModel.stats.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showHelp by remember { mutableStateOf(false) }
    val helpSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTabRoute = currentRoute == null || tabs.any { it.route == currentRoute }

    LaunchedEffect(Unit) {
        solverViewModel.events.collect { event ->
            when (event) {
                SolverEvent.DetectionFinished -> {
                    if (navController.currentBackStackEntry?.destination?.route != Routes.REVIEW) {
                        navController.navigate(Routes.REVIEW)
                    }
                }
                SolverEvent.SolutionReady -> navController.navigate(Routes.SOLUTION)
                is SolverEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    Scaffold(
        containerColor = CanvasColor,
        topBar = {
            if (isTabRoute) {
                AppTopBar(
                    title = brandTitle(),
                    subtitle = "Pindai papan, dapatkan solusi terbaik",
                    onHelp = { showHelp = true }
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isTabRoute,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                NavigationBar(containerColor = SurfaceElevated) {
                    tabs.forEach { tab ->
                        val selected = backStackEntry?.destination?.hierarchy
                            ?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonTeal,
                                selectedTextColor = NeonTeal,
                                indicatorColor = NeonTeal.copy(alpha = 0.16f),
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SCAN,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Routes.SCAN) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    ScanScreen(
                        isAnalyzing = solverState.isAnalyzing,
                        onBitmap = { bitmap, source -> solverViewModel.analyze(bitmap, source) },
                        onManualEntry = {
                            solverViewModel.startManualSession()
                            navController.navigate(Routes.REVIEW)
                        }
                    )
                }
            }

            composable(Routes.HISTORY) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
                ) {
                    HistoryScreen(
                        state = historyState,
                        onQueryChange = historyViewModel::setQuery,
                        onOpen = { record ->
                            solverViewModel.loadRecord(record)
                            navController.navigate(Routes.REVIEW)
                        },
                        onDelete = historyViewModel::delete,
                        contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding())
                    )
                }
            }

            composable(Routes.STATS) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
                ) {
                    StatsScreen(
                        stats = stats,
                        contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding())
                    )
                }
            }

            composable(Routes.REVIEW) {
                ReviewScreen(
                    state = solverState,
                    onBack = { navController.popBackStack() },
                    onToggleCell = solverViewModel::toggleCell,
                    onClearBoard = solverViewModel::clearBoard,
                    onSetPiece = solverViewModel::setPiece,
                    onComboChange = solverViewModel::setStartingCombo,
                    onSolve = solverViewModel::solve,
                    onHelp = { showHelp = true }
                )
            }

            composable(Routes.SOLUTION) {
                SolutionScreen(
                    solutions = solverState.solutions,
                    selectedIndex = solverState.selectedIndex,
                    stepIndex = solverState.stepIndex,
                    bestScore = stats.totalScore,
                    onBack = { navController.popBackStack() },
                    onSelectSolution = solverViewModel::selectSolution,
                    onStep = solverViewModel::setStep,
                    onCommit = {
                        solverViewModel.commitSelected()
                        navController.popBackStack(Routes.SCAN, inclusive = false)
                    },
                    onHelp = { showHelp = true }
                )
            }
        }
    }

    if (showHelp) {
        ModalBottomSheet(
            onDismissRequest = { showHelp = false },
            sheetState = helpSheetState,
            containerColor = SurfaceElevated
        ) {
            HelpSheetContent()
        }
    }
}
