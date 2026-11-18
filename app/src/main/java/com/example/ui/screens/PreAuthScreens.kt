package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.onGoogleSignInClicked
import com.example.data.model.NakuruSmeCatalog
import com.example.data.model.RegistrationDraft
import com.example.ui.components.ProjectXBrandMark
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.BrandNavyDeep
import com.example.ui.theme.BrandNavyMid
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandPurpleBright
import com.example.ui.theme.BrandSlateSilver
import com.example.ui.theme.BrandTeal
import com.example.ui.theme.BrandTealBright
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onCreateAccount: () -> Unit,
    onSignIn: () -> Unit,
    onExplorePublicDemo: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 580.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProjectXBrandMark(showTagline = true)
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = "Nakuru Town",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Nakuru, KE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_welcome_hero),
                        contentDescription = "Nakuru SME Storefront Illustration",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(215.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(215.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, BrandNavyDeep.copy(alpha = 0.88f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BrandTeal
                        ) {
                            Text(
                                text = "NAKURU SME BUSINESS SUITE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = NakuruSmeCatalog.APP_TAGLINE,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Text(
                text = "Digital Technology Adoption & SME Performance (KES)",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Project X helps Nakuru Town SMEs track Digital Marketing, Digital Payments, Computerized Record Keeping, and Online Communication alongside Sales, Customer Growth, and Profitability in KES.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Retail Shops", "Boutiques", "Salons", "Restaurants", "Wholesalers", "Supermarkets").forEach { sector ->
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = sector,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("welcome_get_started_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
            ) {
                Text("Get Started", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCreateAccount,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("welcome_register_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Register SME", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onSignIn,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("welcome_login_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandTeal,
                        contentColor = Color.White
                    )
                ) {
                    Text("Login", fontWeight = FontWeight.SemiBold)
                }
            }

            OutlinedButton(
                onClick = onExplorePublicDemo,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("welcome_public_demo_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.2.dp, BrandPurple.copy(alpha = 0.55f))
            ) {
                Icon(
                    imageVector = Icons.Filled.Storefront,
                    contentDescription = null,
                    tint = BrandPurple,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "Explore Public Demo (No Sign-In)",
                        fontWeight = FontWeight.Bold,
                        color = BrandPurple
                    )
                    Text(
                        text = "Preview dashboard charts, multi-item POS cart, receipts & SPSS CSV",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    initialDraft: RegistrationDraft,
    onDraftUpdated: (RegistrationDraft) -> Unit,
    onBack: () -> Unit,
    onFinishToRegister: () -> Unit,
    onGoToLogin: () -> Unit
) {
    BackHandler(onBack = onBack)
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    var selectedType by remember { mutableStateOf(initialDraft.businessType) }
    var selectedLocation by remember { mutableStateOf(initialDraft.locationNakuru) }
    var selectedSize by remember { mutableStateOf(initialDraft.businessSize) }
    var selectedCurrency by remember { mutableStateOf("KES (Kenyan Shilling)") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 580.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                ProjectXBrandMark(compact = true, showTagline = false)
                TextButton(
                    onClick = {
                        onDraftUpdated(
                            initialDraft.copy(
                                businessType = selectedType,
                                locationNakuru = selectedLocation,
                                businessSize = selectedSize,
                                currency = "KES"
                            )
                        )
                        onFinishToRegister()
                    },
                    modifier = Modifier.testTag("onboarding_skip_button")
                ) {
                    Text("Skip")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (page) {
                        0 -> {
                            Image(
                                painter = painterResource(id = R.drawable.img_onboarding_ops),
                                contentDescription = "Daily Operations Illustration",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(22.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(22.dp))
                            Text(
                                text = "Computerized Records & Digital Payments",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Record sales, expenses, inventory, M-Pesa, bank, and card payments in KES while measuring digital payment adoption.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        1 -> {
                            Image(
                                painter = painterResource(id = R.drawable.img_onboarding_insights),
                                contentDescription = "Business Analytics Illustration",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(22.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(22.dp))
                            Text(
                                text = "Digital Marketing & Online Communication",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Track campaign reach, engagement, and customer enquiries across WhatsApp, Instagram, and Facebook alongside SME performance.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        2 -> {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                shape = RoundedCornerShape(22.dp),
                                color = BrandNavy
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(BrandTeal.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AutoAwesome,
                                            contentDescription = "Gemini AI",
                                            tint = BrandTealBright,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "AI Business Assistant + Cloud Isolation",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Multi-business data isolation via Firebase Auth & Cloud Firestore",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(22.dp))
                            Text(
                                text = "Your Numbers, Actionable Growth",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Run Gemini AI analysis over your Firestore ledger or test safely with clearly labeled Sample Demo Data before going live.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        else -> {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Business Profile Onboarding",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Configure your SME type, Nakuru operating area, business size, and currency.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Text("1. Business Type", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        NakuruSmeCatalog.businessTypes.forEach { sector ->
                                            FilterChip(
                                                selected = selectedType == sector,
                                                onClick = { selectedType = sector },
                                                label = { Text(sector) }
                                            )
                                        }
                                    }

                                    Text("2. Business Location", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        NakuruSmeCatalog.nakuruLocations.forEach { loc ->
                                            FilterChip(
                                                selected = selectedLocation == loc,
                                                onClick = { selectedLocation = loc },
                                                label = { Text(loc) }
                                            )
                                        }
                                    }

                                    Text("3. Business Size / Category", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        NakuruSmeCatalog.businessSizes.forEach { sizeOption ->
                                            FilterChip(
                                                selected = selectedSize == sizeOption,
                                                onClick = { selectedSize = sizeOption },
                                                label = { Text(sizeOption) }
                                            )
                                        }
                                    }

                                    Text("4. Operating Currency", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        NakuruSmeCatalog.currencies.forEach { cur ->
                                            FilterChip(
                                                selected = selectedCurrency == cur,
                                                onClick = { selectedCurrency = cur },
                                                label = { Text(cur) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(4) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(if (selected) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (selected) BrandTeal else MaterialTheme.colorScheme.outline)
                    )
                }
            }

            Button(
                onClick = {
                    if (pagerState.currentPage < 3) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        onDraftUpdated(
                            initialDraft.copy(
                                businessType = selectedType,
                                locationNakuru = selectedLocation,
                                businessSize = selectedSize,
                                currency = "KES"
                            )
                        )
                        onFinishToRegister()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("onboarding_next_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
            ) {
                Text(
                    text = if (pagerState.currentPage < 3) "Continue" else "Proceed to Registration",
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            TextButton(
                onClick = onGoToLogin,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Already have an account? Login")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegistrationScreen(
    initialDraft: RegistrationDraft,
    onDraftUpdated: (RegistrationDraft) -> Unit,
    onBack: () -> Unit,
    onGoToLogin: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var ownerName by remember { mutableStateOf(initialDraft.ownerName) }
    var businessName by remember { mutableStateOf(initialDraft.businessName) }
    var contactEmail by remember { mutableStateOf(initialDraft.contactEmail) }
    var businessType by remember { mutableStateOf(initialDraft.businessType) }
    var locationNakuru by remember { mutableStateOf(initialDraft.locationNakuru) }
    var businessSize by remember { mutableStateOf(initialDraft.businessSize) }
    var phone by remember { mutableStateOf(initialDraft.phone) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 580.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Register Business",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = NakuruSmeCatalog.APP_TAGLINE,
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandTeal
                    )
                }
            }

            Text(
                text = "Complete your SME details and authenticate securely with Google to provision an isolated Cloud Firestore workspace.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = ownerName,
                onValueChange = { ownerName = it },
                label = { Text("Full Name *") },
                placeholder = { Text("e.g., Serah Mukami") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_owner_name_input"),
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = businessName,
                onValueChange = { businessName = it },
                label = { Text("Business Name *") },
                placeholder = { Text("e.g., Rift Valley Mart") },
                leadingIcon = { Icon(Icons.Filled.Storefront, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_business_name_input"),
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = contactEmail,
                onValueChange = { contactEmail = it },
                label = { Text("Business Email Address (Optional)") },
                placeholder = { Text("owner@yourbusiness.co.ke") },
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_email_input"),
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Business Contact Phone") },
                placeholder = { Text("+254 7XX XXX XXX") },
                leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_phone_input"),
                shape = RoundedCornerShape(14.dp)
            )

            Text(
                text = "Business Category",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NakuruSmeCatalog.businessTypes.forEach { sector ->
                    FilterChip(
                        selected = businessType == sector,
                        onClick = { businessType = sector },
                        label = { Text(sector) }
                    )
                }
            }

            Text(
                text = "Business Size",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NakuruSmeCatalog.businessSizes.forEach { sizeOption ->
                    FilterChip(
                        selected = businessSize == sizeOption,
                        onClick = { businessSize = sizeOption },
                        label = { Text(sizeOption) },
                        modifier = Modifier.testTag("register_size_${sizeOption.substringBefore(" ").lowercase()}")
                    )
                }
            }

            Text(
                text = "Business Location (Nakuru Town or Custom)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NakuruSmeCatalog.nakuruLocations.forEach { loc ->
                    FilterChip(
                        selected = locationNakuru == loc,
                        onClick = { locationNakuru = loc },
                        label = { Text(loc) }
                    )
                }
            }
            OutlinedTextField(
                value = locationNakuru,
                onValueChange = { locationNakuru = it },
                label = { Text("Location / Street Address *") },
                placeholder = { Text("e.g., Biashara Street, Nakuru or Naivasha") },
                leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_location_input"),
                shape = RoundedCornerShape(14.dp)
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Passwordless security: Project X authenticates via Google Identity & isolates each business with Firestore rules.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Button(
                onClick = {
                    if (businessName.isBlank() || ownerName.isBlank() || locationNakuru.isBlank()) {
                        errorMessage = "Please enter your Full Name, Business Name, and Business Location."
                        return@Button
                    }
                    errorMessage = null
                    onDraftUpdated(
                        RegistrationDraft(
                            ownerName = ownerName.trim(),
                            businessName = businessName.trim(),
                            contactEmail = contactEmail.trim(),
                            businessType = businessType,
                            locationNakuru = locationNakuru.trim(),
                            businessSize = businessSize,
                            currency = "KES",
                            phone = phone.trim()
                        )
                    )
                    isLoading = true
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isLoading = false
                            onAuthSuccess()
                        },
                        onAuthError = { msg ->
                            isLoading = false
                            errorMessage = msg
                        },
                        scope = scope,
                        onAuthCancelled = {
                            isLoading = false
                            errorMessage = "Sign-in was cancelled. Tap below to continue with Google."
                        }
                    )
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("register_google_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.CloudDone, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Workspace with Google", fontWeight = FontWeight.SemiBold)
                }
            }

            TextButton(
                onClick = onGoToLogin,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Already registered? Login")
            }
        }
    }
}

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onGoToRegister: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showRecoveryDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 480.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                ProjectXBrandMark(compact = true, showTagline = true)
            }

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.2.dp, BrandPurple.copy(alpha = 0.65f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(BrandNavyDeep, BrandNavy, BrandNavyMid)
                            )
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BrandPurple.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, BrandSlateSilver.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = "PROJECT X – SME MANAGEMENT SYSTEM",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BrandPurpleBright,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "Nakuru SME Cloud Portal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Real-time Cloud Firestore ledger, M-Pesa payment tracking, stock management, and AI sales forecasting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandSlateSilver
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Sign in to Project X",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Access your isolated Nakuru SME dashboard, KES ledger, inventory, and AI Business Assistant synced to your Google account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Account access & recovery are secured via Google Identity Services.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isLoading = false
                                    errorMessage = "Sign-in was cancelled. Tap Sign in with Google to try again."
                                }
                            )
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("login_google_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign in with Google", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    TextButton(
                        onClick = { showRecoveryDialog = true },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .testTag("login_password_recovery_button")
                    ) {
                        Text("Forgot password or account recovery?")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New SME business?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onGoToRegister,
                    modifier = Modifier.testTag("login_to_register_button")
                ) {
                    Text("Register Business", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showRecoveryDialog) {
        AlertDialog(
            onDismissRequest = { showRecoveryDialog = false },
            title = { Text("Account & Password Recovery") },
            text = {
                Text(
                    "Project X uses Firebase Authentication backed by Google Identity Services (Credential Manager), so your SME account does not store a separate app password.\n\n" +
                        "To reset your password or recover access, visit Google Account Recovery (accounts.google.com/signin/recovery) or manage security in your device's Google Settings, then return and tap 'Sign in with Google'."
                )
            },
            confirmButton = {
                Button(onClick = { showRecoveryDialog = false }) {
                    Text("Got It")
                }
            }
        )
    }
}
