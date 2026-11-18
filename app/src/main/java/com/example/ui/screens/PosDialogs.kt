package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessTransaction
import com.example.data.model.CustomerRecord
import com.example.data.model.InventoryItem
import com.example.data.model.NakuruSmeCatalog
import com.example.data.model.PosCartLineItem
import com.example.data.model.PurchaseOrderRecord
import com.example.data.model.SupplierRecord
import com.example.data.model.formatKes
import com.example.data.model.isValidIsoDate
import com.example.data.model.todayIsoDate
import com.example.ui.theme.AlertCoral
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandPurpleSoft
import com.example.ui.theme.BrandTeal
import com.example.ui.theme.BrandTealDark
import com.example.ui.theme.BrandTealLight
import java.io.File
import java.io.FileOutputStream

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    confirmLabel: String = "Delete",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = AlertCoral) },
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = AlertCoral),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * AKNS-inspired Multi-Outlet POS Sale Register modal linking Product SKUs, qty stepper,
 * discount, customer selection, payment channel, and instant receipt generation.
 */
@Composable
fun PosSaleDialog(
    inventory: List<InventoryItem>,
    customers: List<CustomerRecord>,
    businessLocation: String,
    onDismiss: () -> Unit,
    onConfirmPosSale: (
        title: String,
        category: String,
        paymentMethod: String,
        transactionRef: String,
        paymentStatus: String,
        transactionDate: String,
        notes: String,
        customerId: String,
        customerName: String,
        inventoryItemId: String,
        quantityText: String,
        unitPriceText: String,
        discountText: String,
        manualAmountText: String
    ) -> Unit,
    onConfirmPosCart: (
        cartItems: List<PosCartLineItem>,
        defaultCategory: String,
        paymentMethod: String,
        transactionRef: String,
        paymentStatus: String,
        transactionDate: String,
        notes: String,
        customerId: String,
        customerName: String,
        cartDiscountText: String
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> }
) {
    val outlets = remember(businessLocation) {
        listOf(
            businessLocation.ifBlank { "Main Nakuru Shop" },
            "Nakuru CBD Counter",
            "Online / WhatsApp Desk"
        ).distinct()
    }
    var selectedOutlet by remember { mutableStateOf(outlets.first()) }
    var linkInventoryProduct by remember(inventory) { mutableStateOf(inventory.isNotEmpty()) }
    var selectedItem by remember(inventory) { mutableStateOf(inventory.firstOrNull()) }
    var selectedCustomer by remember { mutableStateOf<CustomerRecord?>(null) }

    var title by remember { mutableStateOf(inventory.firstOrNull()?.name ?: "") }
    var category by remember { mutableStateOf(NakuruSmeCatalog.saleCategories.first()) }
    var quantityText by remember { mutableStateOf("1") }
    var unitPriceText by remember {
        mutableStateOf(
            inventory.firstOrNull()?.unitPriceKes?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            } ?: ""
        )
    }
    var discountText by remember { mutableStateOf("0") }
    var manualAmountText by remember { mutableStateOf("") }
    var cartItems by remember { mutableStateOf<List<PosCartLineItem>>(emptyList()) }
    var cartDiscountText by remember { mutableStateOf("0") }
    var paymentMethod by remember { mutableStateOf(BusinessTransaction.METHOD_MPESA) }
    var transactionRef by remember { mutableStateOf("") }
    var paymentStatus by remember { mutableStateOf(BusinessTransaction.STATUS_COMPLETED) }
    var transactionDate by remember { mutableStateOf(todayIsoDate()) }
    var notes by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val qtyNum = quantityText.trim().toLongOrNull() ?: 0L
    val unitPriceNum = unitPriceText.trim().toDoubleOrNull() ?: 0.0
    val rawDiscountNum = discountText.trim().toDoubleOrNull() ?: 0.0
    val discountNum = rawDiscountNum.coerceAtLeast(0.0)
    val rawCartDiscount = cartDiscountText.trim().toDoubleOrNull() ?: 0.0
    val cartDiscountNum = rawCartDiscount.coerceAtLeast(0.0)
    val cartSubtotalBeforeCartDiscount = cartItems.sumOf { it.netBeforeCartDiscountKes }
    val cartNetTotal = (cartSubtotalBeforeCartDiscount - cartDiscountNum).coerceAtLeast(0.0)

    val singleItemComputedTotal = if (linkInventoryProduct && selectedItem != null) {
        (qtyNum * unitPriceNum - discountNum).coerceAtLeast(0.0)
    } else {
        manualAmountText.trim().toDoubleOrNull() ?: 0.0
    }
    val computedTotal = if (cartItems.isNotEmpty()) cartNetTotal else singleItemComputedTotal

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Storefront, contentDescription = null, tint = BrandPurple)
                Text("POS Sale Checkout (KES)", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Store Outlet / Counter", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    outlets.forEach { outlet ->
                        FilterChip(
                            selected = selectedOutlet == outlet,
                            onClick = { selectedOutlet = outlet },
                            label = { Text(outlet) }
                        )
                    }
                }

                if (inventory.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = linkInventoryProduct,
                            onClick = {
                                linkInventoryProduct = true
                                selectedItem?.let { item ->
                                    title = item.name
                                    unitPriceText = if (item.unitPriceKes % 1.0 == 0.0) item.unitPriceKes.toLong().toString() else item.unitPriceKes.toString()
                                }
                            },
                            label = { Text("Inventory SKU") }
                        )
                        FilterChip(
                            selected = !linkInventoryProduct,
                            onClick = { linkInventoryProduct = false },
                            label = { Text("Custom / Service Sale") }
                        )
                    }
                }

                if (linkInventoryProduct && inventory.isNotEmpty()) {
                    Text("Select Product (Stock & Price)", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        inventory.forEach { item ->
                            FilterChip(
                                selected = selectedItem?.id == item.id,
                                onClick = {
                                    selectedItem = item
                                    title = item.name
                                    unitPriceText = if (item.unitPriceKes % 1.0 == 0.0) item.unitPriceKes.toLong().toString() else item.unitPriceKes.toString()
                                    validationError = null
                                },
                                label = { Text("${item.name} (${item.quantity} left)") }
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val next = (qtyNum - 1L).coerceAtLeast(1L)
                                quantityText = next.toString()
                                validationError = null
                            }
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "Decrease qty")
                        }
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = {
                                quantityText = it
                                validationError = null
                            },
                            label = { Text("Qty") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("pos_qty_input")
                        )
                        IconButton(
                            onClick = {
                                val next = qtyNum + 1L
                                quantityText = next.toString()
                                validationError = null
                            }
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Increase qty")
                        }
                        OutlinedTextField(
                            value = unitPriceText,
                            onValueChange = {
                                unitPriceText = it
                                validationError = null
                            },
                            label = { Text("Unit KES") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = discountText,
                            onValueChange = {
                                discountText = it
                                validationError = null
                            },
                            label = { Text("Discount (KES)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("pos_discount_input")
                        )
                        OutlinedTextField(
                            value = transactionDate,
                            onValueChange = {
                                transactionDate = it
                                validationError = null
                            },
                            label = { Text("Date (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            validationError = null
                        },
                        label = { Text("Sale Description *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("dialog_tx_title_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = manualAmountText,
                            onValueChange = {
                                manualAmountText = it
                                validationError = null
                            },
                            label = { Text("Total (KES) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("dialog_tx_amount_input")
                        )
                        OutlinedTextField(
                            value = transactionDate,
                            onValueChange = {
                                transactionDate = it
                                validationError = null
                            },
                            label = { Text("Date *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Add current SKU or Custom/Service line to Multi-Item POS Cart
                OutlinedButton(
                    onClick = {
                        val linked = if (linkInventoryProduct) selectedItem else null
                        if (linked != null) {
                            val alreadyQueued = cartItems
                                .filter { it.inventoryItemId == linked.id }
                                .sumOf { it.quantity }
                            when {
                                qtyNum <= 0L -> validationError = "Sale quantity must be at least 1."
                                alreadyQueued + qtyNum > linked.quantity -> {
                                    validationError = "Insufficient stock for ${linked.name}: ${linked.quantity} total in stock ($alreadyQueued already in cart)."
                                }
                                rawDiscountNum < 0.0 -> validationError = "Discount cannot be negative."
                                unitPriceNum <= 0.0 -> validationError = "Unit price must be greater than 0."
                                qtyNum * unitPriceNum - discountNum <= 0.0 -> {
                                    validationError = "Line total must exceed 0 KES."
                                }
                                else -> {
                                    val newLine = PosCartLineItem(
                                        lineId = "${linked.id}_${System.currentTimeMillis()}_${cartItems.size}",
                                        inventoryItemId = linked.id,
                                        itemName = linked.name,
                                        category = category,
                                        quantity = qtyNum,
                                        unitPriceKes = unitPriceNum,
                                        lineDiscountKes = discountNum
                                    )
                                    cartItems = cartItems + newLine
                                    quantityText = "1"
                                    discountText = "0"
                                    validationError = null
                                }
                            }
                        } else {
                            val customAmt = manualAmountText.trim().toDoubleOrNull() ?: 0.0
                            if (title.isBlank() || customAmt <= 0.0) {
                                validationError = "Enter sale description and positive KES amount to add to cart."
                            } else {
                                val newLine = PosCartLineItem(
                                    lineId = "custom_${System.currentTimeMillis()}_${cartItems.size}",
                                    inventoryItemId = "",
                                    itemName = title.trim(),
                                    category = category,
                                    quantity = 1L,
                                    unitPriceKes = customAmt,
                                    lineDiscountKes = 0.0
                                )
                                cartItems = cartItems + newLine
                                title = ""
                                manualAmountText = ""
                                validationError = null
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BrandTeal.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pos_add_to_cart_button")
                ) {
                    Icon(Icons.Filled.AddShoppingCart, contentDescription = null, tint = BrandTeal, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (cartItems.isEmpty()) "Add Item to Multi-Item Cart" else "Add Another Item (${cartItems.size} in Cart)",
                        color = BrandTealDark,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (cartItems.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, BrandTeal.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pos_cart_list")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = BrandTeal, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "POS Cart (${cartItems.size} item(s))",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            cartItems.forEach { line ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${line.itemName} × ${line.quantity}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = buildString {
                                                append("@ ${formatKes(line.unitPriceKes)}")
                                                if (line.lineDiscountKes > 0.0) {
                                                    append(" (-${formatKes(line.lineDiscountKes)})")
                                                }
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = formatKes(line.netBeforeCartDiscountKes),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandTealDark
                                    )
                                    IconButton(
                                        onClick = { cartItems = cartItems.filterNot { it.lineId == line.lineId } }
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Remove ${line.itemName}",
                                            tint = AlertCoral,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = cartDiscountText,
                                onValueChange = {
                                    cartDiscountText = it
                                    validationError = null
                                },
                                label = { Text("Extra Cart Discount (KES)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pos_cart_discount_input")
                            )
                        }
                    }
                }

                // Live Checkout Summary Bar
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BrandPurpleSoft,
                    border = BorderStroke(1.dp, BrandPurple.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (cartItems.isNotEmpty()) "Cart Payable Total (${cartItems.size})" else "Payable Total",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = BrandNavy
                        )
                        Text(formatKes(computedTotal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = BrandPurple)
                    }
                }

                if (customers.isNotEmpty()) {
                    Text("Customer (Optional Loyalty Link)", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedCustomer == null,
                            onClick = { selectedCustomer = null },
                            label = { Text("Walk-in") }
                        )
                        customers.forEach { cust ->
                            FilterChip(
                                selected = selectedCustomer?.id == cust.id,
                                onClick = { selectedCustomer = cust },
                                label = { Text(cust.name) }
                            )
                        }
                    }
                }

                Text("Payment Method", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BusinessTransaction.allPaymentMethods.forEach { m ->
                        FilterChip(
                            selected = paymentMethod == m,
                            onClick = { paymentMethod = m },
                            label = { Text(m) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = transactionRef,
                        onValueChange = { transactionRef = it },
                        label = { Text("M-Pesa / Ref Code") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = paymentStatus == BusinessTransaction.STATUS_COMPLETED,
                        onClick = {
                            paymentStatus = if (paymentStatus == BusinessTransaction.STATUS_COMPLETED) {
                                BusinessTransaction.STATUS_PENDING
                            } else {
                                BusinessTransaction.STATUS_COMPLETED
                            }
                        },
                        label = { Text(paymentStatus) }
                    )
                }

                Text("Category", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NakuruSmeCatalog.saleCategories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (validationError != null) {
                    Text(validationError!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val combinedNotes = listOfNotNull(
                        "Outlet: $selectedOutlet",
                        notes.trim().takeIf { it.isNotBlank() }
                    ).joinToString(" | ")
                    if (cartItems.isNotEmpty()) {
                        when {
                            !isValidIsoDate(transactionDate) -> validationError = "Use YYYY-MM-DD date format."
                            rawCartDiscount < 0.0 -> validationError = "Cart discount cannot be negative."
                            rawCartDiscount >= cartSubtotalBeforeCartDiscount -> validationError = "Cart discount cannot equal or exceed subtotal."
                            cartNetTotal <= 0.0 -> validationError = "Net payable total must exceed 0 KES."
                            else -> onConfirmPosCart(
                                cartItems,
                                category,
                                paymentMethod,
                                transactionRef.trim(),
                                paymentStatus,
                                transactionDate.trim(),
                                combinedNotes,
                                selectedCustomer?.id ?: "",
                                selectedCustomer?.name ?: "",
                                cartDiscountText.trim()
                            )
                        }
                    } else {
                        val linked = if (linkInventoryProduct) selectedItem else null
                        when {
                            !isValidIsoDate(transactionDate) -> validationError = "Use YYYY-MM-DD date format."
                            linked != null && qtyNum <= 0L -> validationError = "Sale quantity must be at least 1."
                            linked != null && qtyNum > linked.quantity -> validationError = "Insufficient stock: only ${linked.quantity} in stock."
                            linked != null && rawDiscountNum < 0.0 -> validationError = "Discount cannot be negative."
                            linked != null && unitPriceNum <= 0.0 -> validationError = "Unit price must be greater than 0."
                            linked == null && (title.isBlank() || computedTotal <= 0.0) -> validationError = "Enter sale description and positive KES amount."
                            computedTotal <= 0.0 -> validationError = "Net payable total must exceed 0 KES."
                            else -> onConfirmPosSale(
                                title.ifBlank { linked?.name ?: "Counter Sale" },
                                category,
                                paymentMethod,
                                transactionRef.trim(),
                                paymentStatus,
                                transactionDate.trim(),
                                combinedNotes,
                                selectedCustomer?.id ?: "",
                                selectedCustomer?.name ?: "",
                                linked?.id ?: "",
                                quantityText.trim(),
                                unitPriceText.trim(),
                                discountText.trim(),
                                manualAmountText.trim()
                            )
                        }
                    }
                },
                modifier = Modifier.testTag("pos_checkout_confirm_button")
            ) {
                Text(if (cartItems.isNotEmpty()) "Complete Cart (${cartItems.size})" else "Complete Sale")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReceiptPreviewDialog(
    transaction: BusinessTransaction,
    businessName: String,
    businessLocation: String,
    onDismiss: () -> Unit,
    receiptItems: List<BusinessTransaction> = listOf(transaction)
) {
    val context = LocalContext.current
    val effectiveItems = receiptItems.ifEmpty { listOf(transaction) }
    val primary = effectiveItems.first()
    val receiptNo = primary.receiptNumber.ifBlank { "PX-${primary.id.take(6).uppercase()}" }
    val netTotalKes = effectiveItems.sumOf { it.amountKes }
    val totalDiscountKes = effectiveItems.sumOf { it.discountKes }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Receipt, contentDescription = null, tint = BrandTeal)
                Text(
                    text = if (effectiveItems.size > 1) "Digital POS Receipt (${effectiveItems.size} items)" else "Digital POS Receipt",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(businessName.uppercase(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text(businessLocation, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    Text("Receipt #: $receiptNo • ${primary.effectiveDate()}", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                    HorizontalDivider()
                    effectiveItems.forEach { item ->
                        ReceiptRow(
                            label = item.inventoryItemName.ifBlank { item.title },
                            value = formatKes(item.amountKes)
                        )
                        if (item.quantitySold > 0L && item.unitPriceKes > 0.0) {
                            Text(
                                text = "  ${item.quantitySold} × ${formatKes(item.unitPriceKes)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (totalDiscountKes > 0.0) {
                        ReceiptRow("Total Discount", "-${formatKes(totalDiscountKes)}")
                    }
                    HorizontalDivider()
                    ReceiptRow("NET TOTAL (KES)", formatKes(netTotalKes), highlight = true)
                    ReceiptRow("Payment Method", primary.paymentMethod)
                    if (primary.transactionRef.isNotBlank()) {
                        ReceiptRow("Reference", primary.transactionRef)
                    }
                    ReceiptRow("Customer", primary.customerName.ifBlank { "Walk-in Customer" })
                    ReceiptRow("Status", primary.paymentStatus)
                    if (primary.notes.isNotBlank()) {
                        Text(primary.notes, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        sharePosReceiptText(
                            context = context,
                            items = effectiveItems,
                            businessName = businessName,
                            location = businessLocation,
                            receiptNo = receiptNo
                        )
                    },
                    modifier = Modifier.testTag("receipt_share_text_button")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share Text")
                }
                OutlinedButton(
                    onClick = {
                        sharePosReceiptPdf(
                            context = context,
                            items = effectiveItems,
                            businessName = businessName,
                            location = businessLocation,
                            receiptNo = receiptNo
                        )
                    },
                    modifier = Modifier.testTag("receipt_share_pdf_button")
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share PDF")
                }
                Button(
                    onClick = { printPosReceipt(context, effectiveItems, businessName, businessLocation, receiptNo) },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                    modifier = Modifier.testTag("receipt_print_button")
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Print")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun ReceiptRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (highlight) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodySmall,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = if (highlight) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (highlight) BrandTealDark else MaterialTheme.colorScheme.onSurface
        )
    }
}

internal fun formatReceiptShareText(
    items: List<BusinessTransaction>,
    businessName: String,
    location: String,
    receiptNo: String
): String {
    val primary = items.firstOrNull() ?: return ""
    val netTotal = items.sumOf { it.amountKes }
    val totalDisc = items.sumOf { it.discountKes }
    return buildString {
        appendLine("*${businessName.uppercase()}*")
        if (location.isNotBlank()) appendLine(location)
        appendLine("Receipt #: $receiptNo")
        appendLine("Date: ${primary.effectiveDate()}")
        appendLine("Customer: ${primary.customerName.ifBlank { "Walk-in Customer" }}")
        appendLine("--------------------------------")
        items.forEach { item ->
            val label = item.inventoryItemName.ifBlank { item.title }
            if (item.quantitySold > 0L && item.unitPriceKes > 0.0) {
                appendLine("$label (${item.quantitySold} x ${formatKes(item.unitPriceKes)}) — ${formatKes(item.amountKes)}")
            } else {
                appendLine("$label — ${formatKes(item.amountKes)}")
            }
        }
        if (totalDisc > 0.0) {
            appendLine("Discount: -${formatKes(totalDisc)}")
        }
        appendLine("--------------------------------")
        appendLine("*TOTAL PAID (KES): ${formatKes(netTotal)}*")
        append("Method: ${primary.paymentMethod} (${primary.paymentStatus})")
        if (primary.transactionRef.isNotBlank()) {
            append(" | Ref: ${primary.transactionRef}")
        }
        appendLine()
        append("Asante! Powered by Project X SME System")
    }
}

private fun sharePosReceiptText(
    context: Context,
    items: List<BusinessTransaction>,
    businessName: String,
    location: String,
    receiptNo: String
) {
    val body = formatReceiptShareText(items, businessName, location, receiptNo)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Receipt $receiptNo — $businessName")
        putExtra(Intent.EXTRA_TEXT, body)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Share Digital Receipt"))
    }.onFailure {
        Toast.makeText(context, "No sharing app found on this device.", Toast.LENGTH_SHORT).show()
    }
}

private fun buildReceiptPdfDocument(
    items: List<BusinessTransaction>,
    businessName: String,
    location: String,
    receiptNo: String
): PdfDocument {
    val primary = items.first()
    val netTotal = items.sumOf { it.amountKes }
    val totalDisc = items.sumOf { it.discountKes }
    val estimatedHeight = (360 + items.size * 34).coerceIn(460, 1200)
    val doc = PdfDocument()
    val page = doc.startPage(PdfDocument.PageInfo.Builder(320, estimatedHeight, 1).create())
    val c = page.canvas
    val titlePaint = Paint().apply {
        color = AndroidColor.BLACK
        textSize = 13f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    val bodyPaint = Paint().apply {
        color = AndroidColor.DKGRAY
        textSize = 10f
        typeface = Typeface.MONOSPACE
    }
    var y = 32f
    c.drawText(businessName.take(28), 20f, y, titlePaint); y += 16f
    c.drawText(location.take(32), 20f, y, bodyPaint); y += 18f
    c.drawText("Receipt: $receiptNo", 20f, y, bodyPaint); y += 14f
    c.drawText("Date: ${primary.effectiveDate()}", 20f, y, bodyPaint); y += 18f
    c.drawText("--------------------------------", 20f, y, bodyPaint); y += 16f
    items.forEach { tx ->
        val label = tx.inventoryItemName.ifBlank { tx.title }.take(28)
        c.drawText(label, 20f, y, titlePaint); y += 14f
        val lineDetail = if (tx.quantitySold > 0L && tx.unitPriceKes > 0.0) {
            "${tx.quantitySold} x ${formatKes(tx.unitPriceKes)} = ${formatKes(tx.amountKes)}"
        } else {
            formatKes(tx.amountKes)
        }
        c.drawText(lineDetail.take(34), 20f, y, bodyPaint); y += 16f
    }
    if (totalDisc > 0.0) {
        c.drawText("Discount: -${formatKes(totalDisc)}", 20f, y, bodyPaint); y += 15f
    }
    c.drawText("TOTAL: ${formatKes(netTotal)}", 20f, y, titlePaint); y += 18f
    c.drawText("Paid via: ${primary.paymentMethod} (${primary.paymentStatus})", 20f, y, bodyPaint); y += 14f
    if (primary.transactionRef.isNotBlank()) {
        c.drawText("Ref: ${primary.transactionRef}", 20f, y, bodyPaint); y += 14f
    }
    c.drawText("Customer: ${primary.customerName.ifBlank { "Walk-in" }}", 20f, y, bodyPaint); y += 20f
    c.drawText("Asante! Powered by Project X", 20f, y, bodyPaint)
    doc.finishPage(page)
    return doc
}

private fun sharePosReceiptPdf(
    context: Context,
    items: List<BusinessTransaction>,
    businessName: String,
    location: String,
    receiptNo: String
) {
    runCatching {
        val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val safeNo = receiptNo.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val outFile = File(dir, "Receipt_$safeNo.pdf")
        val doc = buildReceiptPdfDocument(items, businessName, location, receiptNo)
        try {
            FileOutputStream(outFile).use { out -> doc.writeTo(out) }
        } finally {
            doc.close()
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Digital POS Receipt $receiptNo — $businessName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share PDF Receipt"))
    }.onFailure { err ->
        Toast.makeText(context, "Could not share PDF receipt: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

private fun printPosReceipt(
    context: Context,
    items: List<BusinessTransaction>,
    businessName: String,
    location: String,
    receiptNo: String
) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
    val adapter = object : PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes?,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }
            val info = PrintDocumentInfo.Builder("Receipt_$receiptNo.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build()
            callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback
        ) {
            val doc = buildReceiptPdfDocument(items, businessName, location, receiptNo)
            try {
                FileOutputStream(destination.fileDescriptor).use { out -> doc.writeTo(out) }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            } finally {
                doc.close()
            }
        }
    }
    printManager.print("ProjectX_Receipt_$receiptNo", adapter, null)
}

@Composable
fun StockPurchaseDialog(
    suppliers: List<SupplierRecord>,
    inventory: List<InventoryItem>,
    initialSupplier: SupplierRecord?,
    initialInventoryItem: InventoryItem?,
    onDismiss: () -> Unit,
    onConfirm: (
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
    ) -> Unit
) {
    var selectedSupplier by remember(initialSupplier) { mutableStateOf(initialSupplier ?: suppliers.firstOrNull()) }
    var selectedItem by remember(initialInventoryItem) { mutableStateOf(initialInventoryItem ?: inventory.firstOrNull()) }
    var customItemName by remember(initialInventoryItem) { mutableStateOf(initialInventoryItem?.name ?: "") }
    var quantityText by remember { mutableStateOf("10") }
    var unitCostText by remember(initialInventoryItem) {
        mutableStateOf(
            (initialInventoryItem ?: inventory.firstOrNull())?.unitCostKes?.takeIf { it > 0.0 }?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            } ?: "150"
        )
    }
    var paymentMethod by remember { mutableStateOf(BusinessTransaction.METHOD_MPESA) }
    var paymentStatus by remember { mutableStateOf(PurchaseOrderRecord.STATUS_PAID) }
    var purchaseDate by remember { mutableStateOf(todayIsoDate()) }
    var notes by remember { mutableStateOf("") }
    var logAsExpense by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val qty = quantityText.trim().toLongOrNull() ?: 0L
    val cost = unitCostText.trim().toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Stock Restock Order", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (suppliers.isNotEmpty()) {
                    Text("Supplier", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        suppliers.forEach { sup ->
                            FilterChip(
                                selected = selectedSupplier?.id == sup.id,
                                onClick = { selectedSupplier = sup },
                                label = { Text(sup.name) }
                            )
                        }
                    }
                }
                if (inventory.isNotEmpty()) {
                    Text("Inventory Product", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        inventory.forEach { item ->
                            FilterChip(
                                selected = selectedItem?.id == item.id,
                                onClick = {
                                    selectedItem = item
                                    customItemName = item.name
                                    if (item.unitCostKes > 0.0) {
                                        unitCostText = if (item.unitCostKes % 1.0 == 0.0) item.unitCostKes.toLong().toString() else item.unitCostKes.toString()
                                    }
                                },
                                label = { Text("${item.name} (${item.quantity})") }
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = customItemName,
                        onValueChange = { customItemName = it },
                        label = { Text("Item Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = {
                            quantityText = it
                            error = null
                        },
                        label = { Text("Restock Qty *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unitCostText,
                        onValueChange = {
                            unitCostText = it
                            error = null
                        },
                        label = { Text("Unit Cost (KES) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Surface(shape = RoundedCornerShape(10.dp), color = BrandTealLight) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Order Cost", style = MaterialTheme.typography.labelMedium, color = BrandTealDark)
                        Text(formatKes(qty * cost), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BrandTealDark)
                    }
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PurchaseOrderRecord.paymentStatuses.forEach { st ->
                        FilterChip(selected = paymentStatus == st, onClick = { paymentStatus = st }, label = { Text(st) })
                    }
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BusinessTransaction.allPaymentMethods.forEach { m ->
                        FilterChip(selected = paymentMethod == m, onClick = { paymentMethod = m }, label = { Text(m) })
                    }
                }

                OutlinedTextField(
                    value = purchaseDate,
                    onValueChange = { purchaseDate = it },
                    label = { Text("Order Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = logAsExpense, onCheckedChange = { logAsExpense = it })
                    Text("Also log as Stock Restock Expense", style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
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
                    val resolvedName = selectedItem?.name ?: customItemName.trim()
                    when {
                        resolvedName.isBlank() -> error = "Select or enter item name."
                        qty <= 0L -> error = "Enter positive restock quantity."
                        cost < 0.0 -> error = "Enter non-negative unit cost."
                        !isValidIsoDate(purchaseDate) -> error = "Use YYYY-MM-DD date format."
                        else -> onConfirm(
                            selectedSupplier?.id ?: "",
                            selectedSupplier?.name ?: "",
                            selectedItem?.id ?: "",
                            resolvedName,
                            quantityText.trim(),
                            unitCostText.trim(),
                            paymentMethod,
                            paymentStatus,
                            purchaseDate.trim(),
                            notes.trim(),
                            logAsExpense
                        )
                    }
                },
                modifier = Modifier.testTag("confirm_restock_button")
            ) {
                Text("Confirm Restock")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
