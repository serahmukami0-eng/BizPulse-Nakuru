package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppModule
import com.example.data.model.CustomerRecord
import com.example.data.model.DemoSampleData
import com.example.data.model.InventoryItem
import com.example.data.model.PurchaseOrderRecord
import com.example.data.model.SupplierRecord
import com.example.data.model.formatKes
import com.example.ui.components.DemoDataModeBanner
import com.example.ui.components.EmptyModulePlaceholder
import com.example.ui.components.ErrorModulePlaceholder
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.LoadingModulePlaceholder
import com.example.ui.components.ModulePageHeader
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AlertCoral
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandPurpleSoft
import com.example.ui.theme.BrandTeal
import com.example.ui.theme.BrandTealDark
import com.example.ui.theme.BrandTealLight
import com.example.ui.theme.DemoAmber
import com.example.ui.theme.DemoAmberDark
import com.example.ui.theme.DemoAmberLight
import com.example.ui.viewmodel.DashboardSummary
import com.example.ui.viewmodel.UiState

@Composable
fun CustomersScreen(
    summary: DashboardSummary,
    customersState: UiState<List<CustomerRecord>>,
    onBackToDashboard: () -> Unit,
    onOpenAllModules: () -> Unit,
    onToggleDemoMode: (Boolean) -> Unit,
    onAddCustomer: (name: String, phone: String, email: String, location: String, notes: String) -> Unit,
    onUpdateCustomer: (id: String, name: String, phone: String, email: String, location: String, notes: String) -> Unit,
    onDeleteCustomer: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var repeatOnly by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<CustomerRecord?>(null) }
    var deletingCustomer by remember { mutableStateOf<CustomerRecord?>(null) }

    val liveCustomers = (customersState as? UiState.Success)?.data.orEmpty()
    val customers = if (summary.isDemoMode) DemoSampleData.sampleCustomers else liveCustomers
    val filtered = remember(customers, searchQuery, repeatOnly) {
        val q = searchQuery.trim().lowercase()
        customers.filter { c ->
            val matchesQ = q.isEmpty() ||
                c.name.lowercase().contains(q) ||
                c.phone.lowercase().contains(q) ||
                c.location.lowercase().contains(q) ||
                c.email.lowercase().contains(q)
            val matchesRepeat = !repeatOnly || c.isRepeatCustomer
            matchesQ && matchesRepeat
        }
    }
    val totalCustomerSpend = customers.sumOf { it.totalSpentKes }

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
                module = AppModule.CUSTOMERS,
                onBackToDashboard = onBackToDashboard,
                onOpenAllModules = onOpenAllModules
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    DemoDataModeBanner(
                        isDemoMode = summary.isDemoMode,
                        hasLiveRecords = summary.hasAnyLiveRecords,
                        onToggleDemoMode = onToggleDemoMode
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiMetricCard(
                            title = "Registered Customers",
                            valueText = "${customers.size}",
                            subtitle = "${customers.count { it.isRepeatCustomer }} repeat buyer(s) (2+)",
                            icon = Icons.Filled.Groups,
                            accentColor = BrandPurple,
                            isDemoBadge = summary.isDemoMode,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = "Tracked Customer Spend",
                            valueText = formatKes(totalCustomerSpend),
                            subtitle = if (customers.isNotEmpty()) {
                                "Avg ${formatKes(totalCustomerSpend / customers.size)} / buyer"
                            } else {
                                "Link POS sales to track"
                            },
                            icon = Icons.Filled.ShoppingCart,
                            accentColor = BrandTeal,
                            isDemoBadge = summary.isDemoMode,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader(
                            title = "Customer Directory (H)",
                            subtitle = "${filtered.size} of ${customers.size} profiles shown",
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.testTag("add_customer_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Customer")
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search customers by name, phone, or Nakuru estate") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("customers_search_input")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !repeatOnly,
                                onClick = { repeatOnly = false },
                                label = { Text("All Customers (${customers.size})") }
                            )
                            FilterChip(
                                selected = repeatOnly,
                                onClick = { repeatOnly = true },
                                label = { Text("Repeat Buyers 2+ (${customers.count { it.isRepeatCustomer }})") }
                            )
                        }
                    }
                }

                when {
                    customersState is UiState.Loading -> item { LoadingModulePlaceholder("Loading customer directory...") }
                    customersState is UiState.Error -> item { ErrorModulePlaceholder(customersState.message) }
                    customers.isEmpty() -> item {
                        EmptyModulePlaceholder(
                            icon = Icons.Filled.Groups,
                            title = "No customers registered yet",
                            tip = "Add regular Nakuru buyers to track purchase counts, KES spend, and repeat retention."
                        )
                    }
                    filtered.isEmpty() -> item {
                        EmptyModulePlaceholder(
                            icon = Icons.Filled.Search,
                            title = "No customers match your filter",
                            tip = "Clear search or view all customers."
                        )
                    }
                    else -> items(filtered, key = { it.id }) { cust ->
                        CustomerCard(
                            customer = cust,
                            isDemo = summary.isDemoMode,
                            onEdit = if (summary.isDemoMode) null else {
                                { editingCustomer = cust }
                            },
                            onDelete = if (summary.isDemoMode) null else {
                                { deletingCustomer = cust }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CustomerFormDialog(
            initial = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, phone, email, loc, notes ->
                onAddCustomer(name, phone, email, loc, notes)
                showAddDialog = false
            }
        )
    }

    editingCustomer?.let { target ->
        CustomerFormDialog(
            initial = target,
            onDismiss = { editingCustomer = null },
            onConfirm = { name, phone, email, loc, notes ->
                onUpdateCustomer(target.id, name, phone, email, loc, notes)
                editingCustomer = null
            }
        )
    }

    deletingCustomer?.let { target ->
        ConfirmDeleteDialog(
            title = "Delete Customer?",
            message = "Remove ${target.name} from your customer directory? Linked historical sales remain in the ledger.",
            onDismiss = { deletingCustomer = null },
            onConfirm = {
                onDeleteCustomer(target.id)
                deletingCustomer = null
            }
        )
    }
}

@Composable
private fun CustomerCard(
    customer: CustomerRecord,
    isDemo: Boolean,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(customer.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (customer.isRepeatCustomer) {
                            Surface(shape = RoundedCornerShape(6.dp), color = BrandPurpleSoft) {
                                Text(
                                    text = "REPEAT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandPurple,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (isDemo) {
                            Surface(shape = RoundedCornerShape(6.dp), color = DemoAmberLight) {
                                Text(
                                    text = "DEMO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DemoAmberDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = buildString {
                            append(customer.location.ifBlank { "Nakuru Town" })
                            if (customer.phone.isNotBlank()) append(" • ${customer.phone}")
                            if (customer.email.isNotBlank()) append(" • ${customer.email}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    if (onEdit != null) {
                        IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_customer_${customer.id}")) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit customer", tint = BrandNavy)
                        }
                    }
                    if (onDelete != null) {
                        IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_customer_${customer.id}")) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete customer")
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatKes(customer.totalSpentKes), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = BrandTeal)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Purchases", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${customer.purchaseCount}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Last Purchase", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(customer.lastPurchaseDate.ifBlank { "-" }, style = MaterialTheme.typography.labelMedium)
                }
            }
            if (customer.notes.isNotBlank()) {
                Text(customer.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CustomerFormDialog(
    initial: CustomerRecord?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, email: String, location: String, notes: String) -> Unit
) {
    val isEditing = initial != null
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var phone by remember(initial) { mutableStateOf(initial?.phone ?: "") }
    var email by remember(initial) { mutableStateOf(initial?.email ?: "") }
    var location by remember(initial) { mutableStateOf(initial?.location ?: "Nakuru Town") }
    var notes by remember(initial) { mutableStateOf(initial?.notes ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Customer" else "Add Customer") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    label = { Text("Customer Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("customer_name_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone (+254...)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Estate / Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Preferences") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        error = "Enter customer name."
                    } else {
                        onConfirm(name.trim(), phone.trim(), email.trim(), location.trim(), notes.trim())
                    }
                },
                modifier = Modifier.testTag("customer_save_button")
            ) {
                Text(if (isEditing) "Update" else "Save Customer")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliersScreen(
    summary: DashboardSummary,
    suppliersState: UiState<List<SupplierRecord>>,
    purchasesState: UiState<List<PurchaseOrderRecord>>,
    inventoryState: UiState<List<InventoryItem>>,
    onBackToDashboard: () -> Unit,
    onOpenAllModules: () -> Unit,
    onToggleDemoMode: (Boolean) -> Unit,
    onAddSupplier: (
        name: String,
        contactPerson: String,
        phone: String,
        email: String,
        location: String,
        suppliedProductsSummary: String,
        notes: String
    ) -> Unit,
    onUpdateSupplier: (
        id: String,
        name: String,
        contactPerson: String,
        phone: String,
        email: String,
        location: String,
        suppliedProductsSummary: String,
        notes: String
    ) -> Unit,
    onDeleteSupplier: (String) -> Unit,
    onRecordStockPurchase: (
        supplierId: String,
        supplierName: String,
        inventoryItemId: String,
        itemName: String,
        quantityText: String,
        unitCostText: String,
        paymentMethod: String,
        paymentStatus: String,
        purchaseDate: String,
        notes: String,
        logAsExpense: Boolean
    ) -> Unit,
    onMarkPurchasePaid: (String) -> Unit,
    onDeletePurchaseOrder: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showSupplierDialog by remember { mutableStateOf(false) }
    var editingSupplier by remember { mutableStateOf<SupplierRecord?>(null) }
    var deletingSupplier by remember { mutableStateOf<SupplierRecord?>(null) }
    var showRestockDialog by remember { mutableStateOf(false) }
    var preselectedSupplier by remember { mutableStateOf<SupplierRecord?>(null) }
    var deletingPurchase by remember { mutableStateOf<PurchaseOrderRecord?>(null) }

    val liveSuppliers = (suppliersState as? UiState.Success)?.data.orEmpty()
    val livePurchases = (purchasesState as? UiState.Success)?.data.orEmpty()
    val liveInv = (inventoryState as? UiState.Success)?.data.orEmpty()

    val suppliers = if (summary.isDemoMode) DemoSampleData.sampleSuppliers else liveSuppliers
    val purchases = if (summary.isDemoMode) DemoSampleData.samplePurchases else livePurchases
    val inventory = if (summary.isDemoMode) DemoSampleData.sampleInventory else liveInv

    val filteredSuppliers = remember(suppliers, searchQuery) {
        val q = searchQuery.trim().lowercase()
        suppliers.filter { s ->
            q.isEmpty() ||
                s.name.lowercase().contains(q) ||
                s.contactPerson.lowercase().contains(q) ||
                s.suppliedProductsSummary.lowercase().contains(q) ||
                s.location.lowercase().contains(q)
        }
    }

    val pendingPayables = purchases.filter { it.paymentStatus != PurchaseOrderRecord.STATUS_PAID }

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
                module = AppModule.SUPPLIERS,
                onBackToDashboard = onBackToDashboard,
                onOpenAllModules = onOpenAllModules
            )

            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Suppliers (${suppliers.size})", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Purchases & Payables (${purchases.size})", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    DemoDataModeBanner(
                        isDemoMode = summary.isDemoMode,
                        hasLiveRecords = summary.hasAnyLiveRecords,
                        onToggleDemoMode = onToggleDemoMode
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiMetricCard(
                            title = "Active Suppliers",
                            valueText = "${suppliers.size}",
                            subtitle = "${purchases.size} purchase order(s)",
                            icon = Icons.Filled.LocalShipping,
                            accentColor = BrandNavy,
                            isDemoBadge = summary.isDemoMode,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = "Pending Payables",
                            valueText = formatKes(pendingPayables.sumOf { it.totalCostKes }),
                            subtitle = "${pendingPayables.size} unpaid invoice(s)",
                            icon = Icons.Filled.Receipt,
                            accentColor = if (pendingPayables.isNotEmpty()) DemoAmber else BrandTeal,
                            isDemoBadge = summary.isDemoMode,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (selectedTab == 0) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionHeader(
                                title = "Supplier Directory (I)",
                                subtitle = "${filteredSuppliers.size} wholesaler(s) & vendor(s)",
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { showSupplierDialog = true },
                                modifier = Modifier.testTag("add_supplier_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Supplier")
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search suppliers by name, product supplied, or location") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    when {
                        suppliersState is UiState.Loading -> item { LoadingModulePlaceholder("Loading suppliers...") }
                        suppliersState is UiState.Error -> item { ErrorModulePlaceholder(suppliersState.message) }
                        suppliers.isEmpty() -> item {
                            EmptyModulePlaceholder(
                                icon = Icons.Filled.LocalShipping,
                                title = "No suppliers registered yet",
                                tip = "Add Nakuru wholesalers or distributors to link restock orders and pending payables."
                            )
                        }
                        filteredSuppliers.isEmpty() -> item {
                            EmptyModulePlaceholder(
                                icon = Icons.Filled.Search,
                                title = "No suppliers match your search",
                                tip = "Clear search text to see all suppliers."
                            )
                        }
                        else -> items(filteredSuppliers, key = { it.id }) { sup ->
                            SupplierCard(
                                supplier = sup,
                                isDemo = summary.isDemoMode,
                                onRestock = if (summary.isDemoMode) null else {
                                    {
                                        preselectedSupplier = sup
                                        showRestockDialog = true
                                    }
                                },
                                onEdit = if (summary.isDemoMode) null else {
                                    { editingSupplier = sup }
                                },
                                onDelete = if (summary.isDemoMode) null else {
                                    { deletingSupplier = sup }
                                }
                            )
                        }
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionHeader(
                                title = "Stock Purchase Orders",
                                subtitle = "Atomic restock updates inventory counts & optional expense log",
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    preselectedSupplier = suppliers.firstOrNull()
                                    showRestockDialog = true
                                },
                                modifier = Modifier.testTag("record_purchase_order_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
                            ) {
                                Icon(Icons.Filled.Inventory, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restock Order")
                            }
                        }
                    }

                    when {
                        purchasesState is UiState.Loading -> item { LoadingModulePlaceholder("Loading purchase orders...") }
                        purchasesState is UiState.Error -> item { ErrorModulePlaceholder(purchasesState.message) }
                        purchases.isEmpty() -> item {
                            EmptyModulePlaceholder(
                                icon = Icons.Filled.Inventory,
                                title = "No stock purchase orders logged",
                                tip = "Tap 'Restock Order' to replenish inventory and track supplier payables."
                            )
                        }
                        else -> items(purchases, key = { it.id }) { po ->
                            PurchaseOrderCard(
                                order = po,
                                isDemo = summary.isDemoMode,
                                onMarkPaid = if (summary.isDemoMode || po.paymentStatus == PurchaseOrderRecord.STATUS_PAID) null else {
                                    { onMarkPurchasePaid(po.id) }
                                },
                                onDelete = if (summary.isDemoMode) null else {
                                    { deletingPurchase = po }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSupplierDialog) {
        SupplierFormDialog(
            initial = null,
            onDismiss = { showSupplierDialog = false },
            onConfirm = { name, contact, phone, email, loc, products, notes ->
                onAddSupplier(name, contact, phone, email, loc, products, notes)
                showSupplierDialog = false
            }
        )
    }

    editingSupplier?.let { target ->
        SupplierFormDialog(
            initial = target,
            onDismiss = { editingSupplier = null },
            onConfirm = { name, contact, phone, email, loc, products, notes ->
                onUpdateSupplier(target.id, name, contact, phone, email, loc, products, notes)
                editingSupplier = null
            }
        )
    }

    if (showRestockDialog) {
        StockPurchaseDialog(
            suppliers = suppliers,
            inventory = inventory,
            initialSupplier = preselectedSupplier,
            initialInventoryItem = null,
            onDismiss = { showRestockDialog = false },
            onConfirm = { supId, supName, invId, itemName, qtyText, costText, method, status, date, notes, logExp ->
                onRecordStockPurchase(supId, supName, invId, itemName, qtyText, costText, method, status, date, notes, logExp)
                showRestockDialog = false
            }
        )
    }

    deletingSupplier?.let { target ->
        ConfirmDeleteDialog(
            title = "Delete Supplier?",
            message = "Remove ${target.name} from your suppliers directory?",
            onDismiss = { deletingSupplier = null },
            onConfirm = {
                onDeleteSupplier(target.id)
                deletingSupplier = null
            }
        )
    }

    deletingPurchase?.let { target ->
        ConfirmDeleteDialog(
            title = "Delete Purchase Order?",
            message = "Remove purchase order for ${target.itemName} (${formatKes(target.totalCostKes)})? Existing stock count is kept as-is.",
            onDismiss = { deletingPurchase = null },
            onConfirm = {
                onDeletePurchaseOrder(target.id)
                deletingPurchase = null
            }
        )
    }
}

@Composable
private fun SupplierCard(
    supplier: SupplierRecord,
    isDemo: Boolean,
    onRestock: (() -> Unit)?,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(supplier.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (isDemo) {
                            Surface(shape = RoundedCornerShape(6.dp), color = DemoAmberLight) {
                                Text("DEMO", style = MaterialTheme.typography.labelSmall, color = DemoAmberDark, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Text(
                        text = buildString {
                            append(supplier.location.ifBlank { "Nakuru" })
                            if (supplier.contactPerson.isNotBlank()) append(" • Contact: ${supplier.contactPerson}")
                            if (supplier.phone.isNotBlank()) append(" • ${supplier.phone}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    if (onEdit != null) {
                        IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit supplier", tint = BrandNavy) }
                    }
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) { Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete supplier") }
                    }
                }
            }
            if (supplier.suppliedProductsSummary.isNotBlank()) {
                Text(
                    text = "Supplies: ${supplier.suppliedProductsSummary}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
            if (onRestock != null) {
                OutlinedButton(
                    onClick = onRestock,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("supplier_restock_${supplier.id}")
                ) {
                    Icon(Icons.Filled.Inventory, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Record Stock Purchase")
                }
            }
        }
    }
}

@Composable
private fun PurchaseOrderCard(
    order: PurchaseOrderRecord,
    isDemo: Boolean,
    onMarkPaid: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    val isPaid = order.paymentStatus == PurchaseOrderRecord.STATUS_PAID
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (!isPaid) DemoAmber.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${order.itemName} (×${order.quantity})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${order.supplierName.ifBlank { "Direct Purchase" }} • ${order.effectiveDate()} • ${order.paymentMethod}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatKes(order.totalCostKes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BrandNavy)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isPaid) BrandTealLight else DemoAmberLight
                    ) {
                        Text(
                            text = order.paymentStatus,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isPaid) BrandTealDark else DemoAmberDark,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            if (onMarkPaid != null || onDelete != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onMarkPaid != null) {
                        OutlinedButton(onClick = onMarkPaid, shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = BrandTeal)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mark Settled")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    if (onDelete != null && !isDemo) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete purchase")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SupplierFormDialog(
    initial: SupplierRecord?,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        contactPerson: String,
        phone: String,
        email: String,
        location: String,
        suppliedProductsSummary: String,
        notes: String
    ) -> Unit
) {
    val isEditing = initial != null
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var contactPerson by remember(initial) { mutableStateOf(initial?.contactPerson ?: "") }
    var phone by remember(initial) { mutableStateOf(initial?.phone ?: "") }
    var email by remember(initial) { mutableStateOf(initial?.email ?: "") }
    var location by remember(initial) { mutableStateOf(initial?.location ?: "Nakuru Industrial Area") }
    var products by remember(initial) { mutableStateOf(initial?.suppliedProductsSummary ?: "") }
    var notes by remember(initial) { mutableStateOf(initial?.notes ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Supplier" else "Add Supplier") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    label = { Text("Supplier / Wholesaler Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("supplier_name_input")
                )
                OutlinedTextField(
                    value = contactPerson,
                    onValueChange = { contactPerson = it },
                    label = { Text("Contact Person") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone (+254...)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = products,
                    onValueChange = { products = it },
                    label = { Text("Products Supplied") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Terms (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        error = "Enter supplier name."
                    } else {
                        onConfirm(name.trim(), contactPerson.trim(), phone.trim(), email.trim(), location.trim(), products.trim(), notes.trim())
                    }
                },
                modifier = Modifier.testTag("supplier_save_button")
            ) {
                Text(if (isEditing) "Update" else "Save Supplier")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
