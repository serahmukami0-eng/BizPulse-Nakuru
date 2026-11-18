package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.firebase.configureAppCheck
import com.example.data.local.ProjectXLocalDatabase
import com.example.data.model.RegistrationDraft
import com.example.data.repository.BusinessRepository
import com.example.data.repository.FirebaseAiAssistantRepository
import com.example.data.repository.LocalOverviewRepository
import com.example.data.service.FirebaseVertexTransactionGrowthService
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProjectXWorkspaceShell
import com.example.ui.screens.PublicDemoWorkspace
import com.example.ui.screens.RegistrationScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.screens.asWorkspaceHost
import com.example.ui.theme.ProjectXTheme
import com.example.ui.viewmodel.DashboardViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_WELCOME = "welcome"
const val ROUTE_ONBOARDING = "onboarding"
const val ROUTE_REGISTER = "register"
const val ROUTE_LOGIN = "login"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            ProjectXTheme {
                ProjectXRootApp()
            }
        }
    }
}

@Composable
fun ProjectXRootApp(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    var pendingDraft by remember { mutableStateOf<RegistrationDraft?>(null) }
    var autoSignInAttempted by remember { mutableStateOf(false) }
    var showPublicDemo by remember { mutableStateOf(false) }
    var preAuthStartRoute by remember { mutableStateOf(ROUTE_WELCOME) }

    LaunchedEffect(Unit) {
        if (!autoSignInAttempted && auth.currentUser == null) {
            autoSignInAttempted = true
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = this
            )
        }
    }

    if (currentUser == null) {
        if (showPublicDemo) {
            PublicDemoWorkspace(
                onSignInClick = {
                    preAuthStartRoute = ROUTE_LOGIN
                    showPublicDemo = false
                },
                onExitToWelcome = {
                    preAuthStartRoute = ROUTE_WELCOME
                    showPublicDemo = false
                }
            )
        } else {
            key(preAuthStartRoute) {
                PreAuthNavigationFlow(
                    startDestination = preAuthStartRoute,
                    initialDraft = pendingDraft ?: RegistrationDraft(),
                    onUpdateDraft = { pendingDraft = it },
                    onExplorePublicDemo = { showPublicDemo = true },
                    onAuthSuccess = { showPublicDemo = false }
                )
            }
        }
    } else {
        val user = currentUser!!
        key(user.uid) {
            AuthenticatedWorkspace(
                currentUserId = user.uid,
                defaultDisplayName = user.displayName.orEmpty(),
                defaultEmail = user.email.orEmpty(),
                initialDraft = pendingDraft,
                onDraftConsumed = { pendingDraft = null }
            )
        }
    }
}

@Composable
private fun PreAuthNavigationFlow(
    startDestination: String = ROUTE_WELCOME,
    initialDraft: RegistrationDraft,
    onUpdateDraft: (RegistrationDraft) -> Unit,
    onExplorePublicDemo: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination) {
        composable(ROUTE_WELCOME) {
            WelcomeScreen(
                onGetStarted = { navController.navigate(ROUTE_ONBOARDING) },
                onCreateAccount = { navController.navigate(ROUTE_REGISTER) },
                onSignIn = { navController.navigate(ROUTE_LOGIN) },
                onExplorePublicDemo = onExplorePublicDemo
            )
        }
        composable(ROUTE_ONBOARDING) {
            OnboardingScreen(
                initialDraft = initialDraft,
                onDraftUpdated = onUpdateDraft,
                onBack = { navController.popBackStack() },
                onFinishToRegister = { navController.navigate(ROUTE_REGISTER) },
                onGoToLogin = { navController.navigate(ROUTE_LOGIN) }
            )
        }
        composable(ROUTE_REGISTER) {
            RegistrationScreen(
                initialDraft = initialDraft,
                onDraftUpdated = onUpdateDraft,
                onBack = { navController.popBackStack() },
                onGoToLogin = { navController.navigate(ROUTE_LOGIN) },
                onAuthSuccess = onAuthSuccess
            )
        }
        composable(ROUTE_LOGIN) {
            LoginScreen(
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(ROUTE_WELCOME) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onGoToRegister = { navController.navigate(ROUTE_REGISTER) },
                onAuthSuccess = onAuthSuccess
            )
        }
    }
}

@Composable
private fun AuthenticatedWorkspace(
    currentUserId: String,
    defaultDisplayName: String,
    defaultEmail: String,
    initialDraft: RegistrationDraft?,
    onDraftConsumed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: DashboardViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val businessRepository = BusinessRepository(db)
                val localOverviewRepository = LocalOverviewRepository(
                    ProjectXLocalDatabase.getInstance(app).businessOverviewDao()
                )
                DashboardViewModel(
                    repository = businessRepository,
                    aiRepository = FirebaseAiAssistantRepository(),
                    growthAnalysisService = FirebaseVertexTransactionGrowthService(businessRepository),
                    currentUserId = currentUserId,
                    initialDraft = initialDraft,
                    defaultDisplayName = defaultDisplayName,
                    onDraftConsumed = onDraftConsumed,
                    localOverviewRepository = localOverviewRepository
                )
            }
        }
    )

    val host = remember(viewModel) { viewModel.asWorkspaceHost() }

    ProjectXWorkspaceShell(
        host = host,
        defaultDisplayName = defaultDisplayName,
        defaultEmail = defaultEmail,
        onSignOutOrSignIn = {
            signOutUser(
                credentialManager = credentialManager,
                scope = scope
            )
        },
        isPublicPreviewMode = false
    )
}
