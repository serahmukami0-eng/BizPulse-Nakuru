package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.analytics.Prompt4AnalyticsBundle
import com.example.data.analytics.SmeFullAnalyticsReport
import com.example.data.backup.BackupSummary
import com.example.data.export.BusinessReportType
import com.example.data.export.ReportExportManager
import com.example.data.model.AppModule
import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.CustomerRecord
import com.example.data.model.DemoSampleData
import com.example.data.model.DigitalMarketingCampaign
import com.example.data.model.InventoryItem
import com.example.data.model.NakuruSmeCatalog
import com.example.data.model.OnlineCommunicationLog
import com.example.data.model.PurchaseOrderRecord
import com.example.data.model.todayIsoDate
import com.example.ui.components.DemoDataModeBanner
import com.example.ui.components.ModulePageHeader
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AlertCoral
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.BrandNavyDeep
import com.example.ui.theme.BrandNavyMid
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandPurpleBright
import com.example.ui.theme.BrandPurpleSoft
import com.example.ui.theme.BrandSlateSilver
import com.example.ui.theme.BrandTeal
import com.example.ui.theme.BrandTealBright
import com.example.ui.theme.DemoAmberDark
import com.example.ui.theme.DemoAmberLight
import com.example.ui.viewmodel.BackupRestoreUiState
import com.example.ui.viewmodel.DashboardSummary
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportsScreen(
    currentUserDisplayName: String,
    currentUserEmail: String,
    profileState: UiState<BusinessProfile?>,
    summary: DashboardSummary,
    transactionsState: UiState<List<BusinessTransaction>>,
    campaignsState: UiState<List<DigitalMarketingCampaign>>,
    communicationsState: UiState<List<OnlineCommunicationLog>>,
    inventoryState: UiState<List<InventoryItem>>,
    customersState: UiState<List<CustomerRecord>>,
    purchasesState: UiState<List<PurchaseOrderRecord>>,
    analyticsReport: SmeFullAnalyticsReport,
    prompt4Analytics: Prompt4AnalyticsBundle,
    isSaving: Boolean,
    backupRestoreState: BackupRestoreUiState = BackupRestoreUiState.Idle,
    onRequestBackupJson: (onResult: (Result<Pair<String, BackupSummary>>) -> Unit) -> Unit = {},
    onReportBackupExportOutcome: (Boolean, String) -> Unit = { _, _ -> },
    onInspectBackupJson: (rawJson: String, sourceLabel: String) -> Unit = { _, _ -> },
    onConfirmRestoreBackup: (replaceExisting: Boolean) -> Unit = {},
    onDismissRestoreConfirmation: () -> Unit = {},
    onBackToDashboard: () -> Unit,
    onOpenAllModules: () -> Unit,
    onToggleDemoMode: (Boolean) -> Unit,
    onSaveProfile: (
        ownerName: String,
        businessName: String,
        businessType: String,
        locationNakuru: String,
        businessSize: String,
        phone: String,
        demoModeEnabled: Boolean
    ) -> Unit,
    onSignOut: () -> Unit,
    isPublicPreviewMode: Boolean = false
) {
    val context = LocalContext.current
    var selectedReportType by remember { mutableStateOf(BusinessReportType.OVERALL_PERFORMANCE) }
    var exportStatusMessage by remember { mutableStateOf<String?>(null) }

    val existing = (profileState as? UiState.Success)?.data
    var ownerName by remember(existing) {
        mutableStateOf(existing?.ownerName?.ifBlank { currentUserDisplayName } ?: currentUserDisplayName)
    }
    var businessName by remember(existing) { mutableStateOf(existing?.businessName ?: "") }
    var businessType by remember(existing) {
        mutableStateOf(existing?.businessType ?: NakuruSmeCatalog.businessTypes.first())
    }
    var locationNakuru by remember(existing) {
        mutableStateOf(existing?.locationNakuru ?: NakuruSmeCatalog.nakuruLocations.first())
    }
    var businessSize by remember(existing) {
        mutableStateOf(existing?.businessSize ?: NakuruSmeCatalog.businessSizes[1])
    }
    var phone by remember(existing) { mutableStateOf(existing?.phone ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val createdDateLabel = remember(existing?.createdAt) {
        existing?.createdAt?.toDate()?.let { date ->
            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(date)
        } ?: "Created upon first profile save"
    }

    val liveTx = (transactionsState as? UiState.Success)?.data.orEmpty()
    val liveInv = (inventoryState as? UiState.Success)?.data.orEmpty()
    val liveCust = (customersState as? UiState.Success)?.data.orEmpty()
    val livePo = (purchasesState as? UiState.Success)?.data.orEmpty()
    val liveCamps = (campaignsState as? UiState.Success)?.data.orEmpty()
    val liveComms = (communicationsState as? UiState.Success)?.data.orEmpty()

    val activeTx = if (summary.isDemoMode) DemoSampleData.sampleTransactions else liveTx
    val activeInv = if (summary.isDemoMode) DemoSampleData.sampleInventory else liveInv
    val activeCust = if (summary.isDemoMode) DemoSampleData.sampleCustomers else liveCust
    val activePo = if (summary.isDemoMode) DemoSampleData.samplePurchases else livePo
    val activeCamps = if (summary.isDemoMode) DemoSampleData.sampleCampaigns else liveCamps
    val activeComms = if (summary.isDemoMode) DemoSampleData.sampleCommunications else liveComms

    val generatedReport = remember(
        selectedReportType,
        summary,
        activeTx,
        activeInv,
        activeCust,
        activePo,
        activeCamps,
        activeComms,
        analyticsReport,
        prompt4Analytics
    ) {
        ReportExportManager.generateReport(
            type = selectedReportType,
            businessName = summary.businessName,
            location = summary.locationNakuru,
            isDemoData = summary.isDemoMode,
            transactions = activeTx,
            inventory = activeInv,
            customers = activeCust,
            purchases = activePo,
            campaigns = activeCamps,
            communications = activeComms,
            analytics = analyticsReport,
            prompt4 = prompt4Analytics,
            periodLabel = analyticsReport.activeWindow.label
        )
    }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    ReportExportManager.writeCsvToStream(generatedReport, out)
                } ?: error("Unable to open destination file stream.")
            }.onSuccess {
                exportStatusMessage = "Exported ${generatedReport.title} CSV successfully."
                Toast.makeText(context, "CSV saved!", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                exportStatusMessage = "CSV export failed: ${err.localizedMessage}"
            }
        }
    }

    val researchSpssReport = remember(
        summary,
        activeTx,
        activeInv,
        activeCust,
        activePo,
        activeCamps,
        activeComms,
        analyticsReport,
        prompt4Analytics
    ) {
        ReportExportManager.generateReport(
            type = BusinessReportType.RESEARCH_DATASET_SPSS,
            businessName = summary.businessName,
            location = summary.locationNakuru,
            isDemoData = summary.isDemoMode,
            transactions = activeTx,
            inventory = activeInv,
            customers = activeCust,
            purchases = activePo,
            campaigns = activeCamps,
            communications = activeComms,
            analytics = analyticsReport,
            prompt4 = prompt4Analytics,
            periodLabel = analyticsReport.activeWindow.label
        )
    }

    val researchCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    ReportExportManager.writeCsvToStream(researchSpssReport, out)
                } ?: error("Unable to open destination file stream.")
            }.onSuccess {
                exportStatusMessage = "Exported Chapter 4 SPSS / Excel Research CSV (${researchSpssReport.rows.size} observation row(s))."
                Toast.makeText(context, "Research CSV saved!", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                exportStatusMessage = "Research CSV export failed: ${err.localizedMessage}"
            }
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    ReportExportManager.writePdfToStream(generatedReport, out)
                } ?: error("Unable to open destination file stream.")
            }.onSuccess {
                exportStatusMessage = "Exported ${generatedReport.title} PDF successfully."
                Toast.makeText(context, "PDF saved!", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                exportStatusMessage = "PDF export failed: ${err.localizedMessage}"
            }
        }
    }

    val backupExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            onRequestBackupJson { res ->
                res.onSuccess { (json, backupSummary) ->
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(json.toByteArray(Charsets.UTF_8))
                            out.flush()
                        } ?: error("Unable to open backup destination stream.")
                    }.onSuccess {
                        onReportBackupExportOutcome(
                            true,
                            "Saved backup (${backupSummary.totalRecords} records) successfully."
                        )
                    }.onFailure { err ->
                        onReportBackupExportOutcome(
                            false,
                            "Backup export failed: ${err.localizedMessage}"
                        )
                    }
                }.onFailure { err ->
                    onReportBackupExportOutcome(
                        false,
                        err.localizedMessage ?: "Failed to compile backup."
                    )
                }
            }
        }
    }

    val backupRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                    it.readText()
                } ?: error("Unable to read selected backup file.")
            }.onSuccess { rawJson ->
                onInspectBackupJson(rawJson, uri.lastPathSegment ?: "Selected backup")
            }.onFailure { err ->
                onReportBackupExportOutcome(
                    false,
                    "Could not read backup file: ${err.localizedMessage}"
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1040.dp)
        ) {
            ModulePageHeader(
                module = AppModule.REPORTS,
                onBackToDashboard = onBackToDashboard,
                onOpenAllModules = onOpenAllModules
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("reports_lazy_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SectionHeader(
                        title = "Business Reports, CSV/PDF Exports & Printing (G)",
                        subtitle = "Generate all ${BusinessReportType.entries.size} SME & SPSS research reports from Cloud Firestore records"
                    )
                }

                item {
                    DemoDataModeBanner(
                        isDemoMode = summary.isDemoMode,
                        hasLiveRecords = summary.hasAnyLiveRecords,
                        onToggleDemoMode = onToggleDemoMode
                    )
                }

                // 11-Report Suite Card + In-App Preview + CSV/PDF/Print Exports
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Filled.Assessment, contentDescription = null, tint = BrandPurple)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = generatedReport.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${generatedReport.periodLabel} • ${generatedReport.rows.size} row(s)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (summary.isDemoMode) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = DemoAmberLight) {
                                        Text(
                                            text = "DEMO DATA",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = DemoAmberDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Text("Select Report Type (${BusinessReportType.entries.size} Available)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                BusinessReportType.entries.forEach { reportType ->
                                    FilterChip(
                                        selected = selectedReportType == reportType,
                                        onClick = {
                                            selectedReportType = reportType
                                            exportStatusMessage = null
                                        },
                                        label = { Text(reportType.title) },
                                        modifier = Modifier.testTag("report_type_${reportType.name.lowercase()}")
                                    )
                                }
                            }

                            Text(
                                text = selectedReportType.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Export Actions Row (Zero storage permissions needed)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val filename = "ProjectX_${selectedReportType.name.lowercase()}_${todayIsoDate()}.csv"
                                        csvLauncher.launch(filename)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("export_csv_button")
                                ) {
                                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export CSV")
                                }
                                Button(
                                    onClick = {
                                        val filename = "ProjectX_${selectedReportType.name.lowercase()}_${todayIsoDate()}.pdf"
                                        pdfLauncher.launch(filename)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("export_pdf_button")
                                ) {
                                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export PDF")
                                }
                                OutlinedButton(
                                    onClick = {
                                        ReportExportManager.printReport(context, generatedReport)
                                        exportStatusMessage = "Sent ${generatedReport.title} to Android Print spooler."
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("print_report_button")
                                ) {
                                    Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Print Report")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val filename = "ProjectX_Chapter4_SPSS_Dataset_${todayIsoDate()}.csv"
                                        researchCsvLauncher.launch(filename)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, BrandTeal.copy(alpha = 0.7f)),
                                    modifier = Modifier.testTag("export_spss_research_csv_button")
                                ) {
                                    Icon(Icons.Filled.Download, contentDescription = null, tint = BrandTeal, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("SPSS / Excel Research CSV", color = BrandTeal, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            exportStatusMessage?.let { statusMsg ->
                                Surface(shape = RoundedCornerShape(10.dp), color = BrandPurpleSoft) {
                                    Text(
                                        text = statusMsg,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = BrandNavy,
                                        modifier = Modifier.fillMaxWidth().padding(10.dp)
                                    )
                                }
                            }

                            HorizontalDivider()

                            Text("Executive Summary", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            generatedReport.summaryPairs.forEach { (label, value) ->
                                ReportMetricLine(label, value)
                            }

                            HorizontalDivider()

                            Text("Formatted Report Table Preview", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            if (generatedReport.rows.isEmpty()) {
                                Text(
                                    text = "No matching records in this period to populate rows.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        generatedReport.columns.forEach { col ->
                                            Text(
                                                text = col,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandPurple,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.width(104.dp)
                                            )
                                        }
                                    }
                                    HorizontalDivider()
                                    generatedReport.rows.take(10).forEach { row ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            row.forEach { cell ->
                                                Text(
                                                    text = cell,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.width(104.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (generatedReport.rows.size > 10) {
                                        Text(
                                            text = "Showing first 10 of ${generatedReport.rows.size} rows — Export CSV or PDF for full statement.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Text(
                                text = generatedReport.footerNote,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Authenticated Account Card
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = BrandNavy)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Authenticated User Account",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.VerifiedUser,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Google Auth",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                            ReportMetricLine(
                                "Account Name",
                                currentUserDisplayName.ifBlank { existing?.ownerName?.ifBlank { "Signed-in Owner" } ?: "Signed-in Owner" }
                            )
                            ReportMetricLine(
                                "Email",
                                currentUserEmail.ifBlank { "Google Workspace Account" }
                            )
                            ReportMetricLine("Profile Created", createdDateLabel)
                            Text(
                                text = "Password changes and 2-step recovery are protected via your Google Account security settings. Each SME workspace is strictly isolated in Cloud Firestore by authenticated UID.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Business Profile Editor
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Business Profile (Cloud Firestore)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedTextField(
                                value = businessName,
                                onValueChange = {
                                    businessName = it
                                    validationError = null
                                },
                                label = { Text("Business Name *") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_business_name_input")
                            )
                            OutlinedTextField(
                                value = ownerName,
                                onValueChange = {
                                    ownerName = it
                                    validationError = null
                                },
                                label = { Text("Owner / Manager Name *") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_owner_name_input")
                            )
                            Text("Business Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                NakuruSmeCatalog.businessTypes.forEach { sector ->
                                    FilterChip(
                                        selected = businessType == sector,
                                        onClick = { businessType = sector },
                                        label = { Text(sector) }
                                    )
                                }
                            }
                            Text("Business Size", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                NakuruSmeCatalog.businessSizes.forEach { sizeOption ->
                                    FilterChip(
                                        selected = businessSize == sizeOption,
                                        onClick = { businessSize = sizeOption },
                                        label = { Text(sizeOption) },
                                        modifier = Modifier.testTag("settings_size_${sizeOption.substringBefore(" ").lowercase()}")
                                    )
                                }
                            }
                            Text("Business Location (Nakuru Town or Other)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                NakuruSmeCatalog.nakuruLocations.forEach { loc ->
                                    FilterChip(
                                        selected = locationNakuru == loc,
                                        onClick = {
                                            locationNakuru = loc
                                            validationError = null
                                        },
                                        label = { Text(loc) }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = locationNakuru,
                                onValueChange = {
                                    locationNakuru = it
                                    validationError = null
                                },
                                label = { Text("Location / Street Address *") },
                                placeholder = { Text("e.g., Kenyatta Avenue, Nakuru or Eldoret") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_location_input")
                            )
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Business Contact / M-Pesa Phone") },
                                placeholder = { Text("+254 7XX XXX XXX") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_phone_input")
                            )

                            if (validationError != null) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = validationError!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (businessName.isBlank() || ownerName.isBlank() || locationNakuru.isBlank()) {
                                        validationError = "Business Name, Owner Name, and Location are required."
                                        return@Button
                                    }
                                    validationError = null
                                    onSaveProfile(
                                        ownerName,
                                        businessName,
                                        businessType,
                                        locationNakuru,
                                        businessSize,
                                        phone,
                                        summary.isDemoMode
                                    )
                                },
                                enabled = !isSaving,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("save_profile_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isSaving) "Saving..." else "Save Business Profile")
                            }
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, BrandPurple.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("backup_restore_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Backup,
                                    contentDescription = "Database Backup and Restore",
                                    tint = BrandPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Database Backup & Restore (JSON)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Export all saved live records (profile, sales, expenses, inventory, stock audit movements, customers, suppliers, purchases, campaigns, and inquiries) to a portable JSON backup, or restore a validated backup file.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (backupRestoreState is BackupRestoreUiState.Exporting ||
                                backupRestoreState is BackupRestoreUiState.Restoring
                            ) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = BrandPurple
                                )
                            }
                            when (backupRestoreState) {
                                is BackupRestoreUiState.Success -> {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = BrandTeal.copy(alpha = 0.14f),
                                        modifier = Modifier.testTag("backup_status_banner")
                                    ) {
                                        Text(
                                            text = backupRestoreState.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = BrandTeal,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                                is BackupRestoreUiState.Error -> {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        modifier = Modifier.testTag("backup_status_banner")
                                    ) {
                                        Text(
                                            text = backupRestoreState.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                                else -> Unit
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        backupExportLauncher.launch("projectx_backup_${todayIsoDate()}.json")
                                    },
                                    enabled = !isSaving && backupRestoreState !is BackupRestoreUiState.Exporting &&
                                        backupRestoreState !is BackupRestoreUiState.Restoring,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("export_backup_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
                                ) {
                                    Icon(Icons.Filled.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export Backup", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                OutlinedButton(
                                    onClick = {
                                        backupRestoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                    },
                                    enabled = !isSaving && backupRestoreState !is BackupRestoreUiState.Exporting &&
                                        backupRestoreState !is BackupRestoreUiState.Restoring,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("restore_backup_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, BrandPurple.copy(alpha = 0.7f))
                                ) {
                                    Icon(
                                        Icons.Filled.Restore,
                                        contentDescription = null,
                                        tint = BrandPurple,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Restore File",
                                        color = BrandPurple,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.2.dp, BrandPurple.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("about_project_x_card")
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
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = "About Project X",
                                    tint = BrandPurpleBright,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "About PROJECT X – SME MANAGEMENT SYSTEM",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "Version ${BuildConfig.VERSION_NAME} • Research-aligned SME performance, POS, inventory, M-Pesa ledger, and AI forecasting platform for Nakuru Town, Kenya.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandSlateSilver
                            )
                            HorizontalDivider(color = BrandSlateSilver.copy(alpha = 0.2f))
                            Text(
                                text = "Research Framework: The Effect of Digital Technology Adoption on the Performance of Small and Medium-Sized Enterprises in Nakuru Town, Kenya (Digital Marketing, Digital Payments, Computerized Record Keeping & Online Communication).",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandSlateSilver
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = BrandPurple.copy(alpha = 0.22f),
                                border = BorderStroke(1.dp, BrandSlateSilver.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "Cloud Stack: Google Sign-In • Cloud Firestore • App Check • Firebase AI Logic",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandPurpleBright,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("support_contact_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VerifiedUser,
                                    contentDescription = "Support and System Diagnostics",
                                    tint = BrandPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Support & System Diagnostics",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            ReportMetricLine(
                                label = "Data Mode",
                                value = if (summary.isDemoMode) "Sample Demo Preview" else "Live Cloud Firestore"
                            )
                            ReportMetricLine(
                                label = "Account Email",
                                value = currentUserEmail.ifBlank { "Google Workspace User" }
                            )
                            ReportMetricLine(
                                label = "App Release",
                                value = "v${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})"
                            )
                            Text(
                                text = "Digital payment references (M-Pesa, Bank Transfer, Card) are recorded securely in your encrypted Firestore ledger. For account or workspace support, copy the diagnostic summary below and include it with your administrator or project inquiry.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = {
                                    val diagnostics = buildString {
                                        appendLine("PROJECT X – SME MANAGEMENT SYSTEM Diagnostics")
                                        appendLine("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                                        appendLine("Package: ${BuildConfig.APPLICATION_ID}")
                                        appendLine("Workspace Mode: ${if (summary.isDemoMode) "Sample Demo Preview" else "Live Cloud Firestore"}")
                                        appendLine("Account: ${currentUserEmail.ifBlank { "Signed-in User" }}")
                                        appendLine("Business: ${summary.businessName} (${summary.locationNakuru})")
                                    }
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Project X Diagnostics", diagnostics))
                                    Toast.makeText(
                                        context,
                                        "Support diagnostics copied to clipboard.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("copy_support_summary_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BrandPurple.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = null,
                                    tint = BrandPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Copy Support Summary",
                                    color = BrandPurple,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("sign_out_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, AlertCoral.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = if (isPublicPreviewMode) BrandPurple else AlertCoral)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPublicPreviewMode) "Sign In to Cloud Workspace" else "Sign Out",
                            color = if (isPublicPreviewMode) BrandPurple else AlertCoral,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    if (backupRestoreState is BackupRestoreUiState.ReadyToConfirmRestore) {
        val bs = backupRestoreState.backup.summary
        AlertDialog(
            onDismissRequest = onDismissRestoreConfirmation,
            modifier = Modifier.testTag("confirm_restore_dialog"),
            icon = {
                Icon(
                    imageVector = Icons.Filled.WarningAmber,
                    contentDescription = "Restore Warning",
                    tint = AlertCoral
                )
            },
            title = {
                Text(
                    text = "Confirm Database Restore",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Validated backup for '${bs.businessName}' (exported ${bs.exportedAtIso.take(10)}):",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "• ${bs.transactionsCount} transaction(s)\n" +
                            "• ${bs.inventoryCount} inventory SKU(s), ${bs.stockMovementsCount} stock audit log(s)\n" +
                            "• ${bs.customersCount} customer(s), ${bs.suppliersCount} supplier(s), ${bs.purchasesCount} purchase(s)\n" +
                            "• ${bs.campaignsCount} campaign(s), ${bs.communicationsCount} inquiry log(s)",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "Warning: 'Replace All' permanently overwrites your current Cloud Firestore business records with this backup. Use 'Merge' to add or update matching IDs without deleting other records.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onConfirmRestoreBackup(true) },
                    modifier = Modifier.testTag("confirm_restore_replace_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = AlertCoral)
                ) {
                    Text("Replace All")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    androidx.compose.material3.TextButton(
                        onClick = onDismissRestoreConfirmation
                    ) {
                        Text("Cancel")
                    }
                    androidx.compose.material3.TextButton(
                        onClick = { onConfirmRestoreBackup(false) },
                        modifier = Modifier.testTag("confirm_restore_merge_button")
                    ) {
                        Text("Merge", color = BrandPurple, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }
}

@Composable
private fun ReportMetricLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
