package com.example.ui.screens

import android.content.Context
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
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
    ) -> Unit
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
    val computedTotal = if (linkInventoryProduct && selectedItem != null) {
        (qtyNum * unitPriceNum - discountNum).coerceAtLeast(0.0)
    } else {
        manualAmountText.trim().toDoubleOrNull() ?: 0.0
    }

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
                        Text("Payable Total", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = BrandNavy)
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
                    val linked = if (linkInventoryProduct) selectedItem else null
                    when {
                        !isValidIsoDate(transactionDate) -> validationError = "Use YYYY-MM-DD date format."
                        linked != null && qtyNum <= 0L -> validationError = "Sale quantity must be at least 1."
                        linked != null && qtyNum > linked.quantity -> validationError = "Insufficient stock: only ${linked.quantity} in stock."
                        linked != null && rawDiscountNum < 0.0 -> validationError = "Discount cannot be negative."
                        linked != null && unitPriceNum <= 0.0 -> validationError = "Unit price must be greater than 0."
                        linked == null && (title.isBlank() || computedTotal <= 0.0) -> validationError = "Enter sale description and positive KES amount."
                        computedTotal <= 0.0 -> validationError = "Net payable total must exceed 0 KES."
                        else -> {
                            val combinedNotes = listOfNotNull(
                                "Outlet: $selectedOutlet",
                                notes.trim().takeIf { it.isNotBlank() }
                            ).joinToString(" | ")
                            onConfirmPosSale(
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
                Text("Complete Sale")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ReceiptPreviewDialog(
    transaction: BusinessTransaction,
    businessName: String,
    businessLocation: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val receiptNo = transaction.receiptNumber.ifBlank { "PX-${transaction.id.take(6).uppercase()}" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Receipt, contentDescription = null, tint = BrandTeal)
                Text("Digital POS Receipt", fontWeight = FontWeight.Bold)
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
                    Text("Receipt #: $receiptNo • ${transaction.effectiveDate()}", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                    HorizontalDivider()
                    ReceiptRow("Item / Sale", transaction.inventoryItemName.ifBlank { transaction.title })
                    if (transaction.quantitySold > 0L) {
                        ReceiptRow("Qty × Unit Price", "${transaction.quantitySold} × ${formatKes(transaction.unitPriceKes)}")
                    }
                    if (transaction.discountKes > 0.0) {
                        ReceiptRow("Discount", "-${formatKes(transaction.discountKes)}")
                    }
                    HorizontalDivider()
                    ReceiptRow("NET TOTAL (KES)", formatKes(transaction.amountKes), highlight = true)
                    ReceiptRow("Payment Method", transaction.paymentMethod)
                    if (transaction.transactionRef.isNotBlank()) {
                        ReceiptRow("Reference", transaction.transactionRef)
                    }
                    ReceiptRow("Customer", transaction.customerName.ifBlank { "Walk-in Customer" })
                    ReceiptRow("Status", transaction.paymentStatus)
                    if (transaction.notes.isNotBlank()) {
                        Text(transaction.notes, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { printPosReceipt(context, transaction, businessName, businessLocation, receiptNo) },
                colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
            ) {
                Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Print Receipt")
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
            fontFamily = FontFamily.Monospace
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

private fun printPosReceipt(
    context: Context,
    tx: BusinessTransaction,
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
            val doc = PdfDocument()
            try {
                val page = doc.startPage(PdfDocument.PageInfo.Builder(320, 480, 1).create())
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
                c.drawText("Date: ${tx.effectiveDate()}", 20f, y, bodyPaint); y += 18f
                c.drawText("--------------------------------", 20f, y, bodyPaint); y += 16f
                c.drawText(tx.inventoryItemName.ifBlank { tx.title }.take(30), 20f, y, titlePaint); y += 15f
                if (tx.quantitySold > 0L) {
                    c.drawText("${tx.quantitySold} x ${formatKes(tx.unitPriceKes)}", 20f, y, bodyPaint); y += 15f
                }
                if (tx.discountKes > 0.0) {
                    c.drawText("Discount: -${formatKes(tx.discountKes)}", 20f, y, bodyPaint); y += 15f
                }
                c.drawText("TOTAL: ${formatKes(tx.amountKes)}", 20f, y, titlePaint); y += 18f
                c.drawText("Paid via: ${tx.paymentMethod} (${tx.paymentStatus})", 20f, y, bodyPaint); y += 14f
                if (tx.transactionRef.isNotBlank()) {
                    c.drawText("Ref: ${tx.transactionRef}", 20f, y, bodyPaint); y += 14f
                }
                c.drawText("Customer: ${tx.customerName.ifBlank { "Walk-in" }}", 20f, y, bodyPaint); y += 20f
                c.drawText("Asante! Powered by Project X", 20f, y, bodyPaint)
                doc.finishPage(page)
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
