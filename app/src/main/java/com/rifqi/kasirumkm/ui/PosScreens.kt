package com.rifqi.kasirumkm.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.rifqi.kasirumkm.data.local.ProductEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
 import android.widget.Toast
 import androidx.compose.ui.platform.LocalContext
 import androidx.compose.material.icons.filled.DateRange
 import androidx.compose.material3.DateRangePicker
 import androidx.compose.material3.rememberDateRangePickerState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
fun Double.toRupiah(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return formatter.format(this).replace(",00", "")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPosScreen(viewModel: PosViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    // Tambahkan Tab "Riwayat"
    val tabs = listOf("Kasir POS", "Riwayat", "Kelola Menu")

    val totalItemsCount by viewModel.totalItemsCount.collectAsState()
    val totalPrice by viewModel.totalPrice.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()

    val detailDialogData by viewModel.showDetailDialog.collectAsState()
    var showCheckoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Kasir UMKM", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (selectedTab == 0 && cartItems.isNotEmpty()) {
                Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total ($totalItemsCount item)", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = totalPrice.toRupiah(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Button(onClick = { showCheckoutDialog = true }, shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bayar")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> PosKasirTabScreen(viewModel = viewModel)
                1 -> RiwayatTabScreen(viewModel = viewModel) // Layar Riwayat Baru
                2 -> KelolaMenuTabScreen(viewModel = viewModel)
            }
        }
    }

    if (showCheckoutDialog) {
        CheckoutDialog(
            totalPrice = totalPrice,
            onDismiss = { showCheckoutDialog = false },
            onConfirm = { customerName ->
                viewModel.checkout(customerName) { showCheckoutDialog = false }
            }
        )
    }

    // Popup Struk Transaksi (Digunakan saat checkout sukses ATAU saat melihat riwayat)
    detailDialogData?.let { (header, items) ->
        TransactionDetailDialog(
            header = header,
            items = items,
            onDismiss = { viewModel.closeDetailDialog() }
        )
    }
}

// ==========================================
// LAYAR RIWAYAT TRANSAKSI
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiwayatTabScreen(viewModel: PosViewModel) {
    val historyList by viewModel.historyList.collectAsState()
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val sharedPref = context.getSharedPreferences("KasirPrefs", android.content.Context.MODE_PRIVATE)

    var showSyncDialog by remember { mutableStateOf(false) }
    var webhookUrl by remember { mutableStateOf(sharedPref.getString("webhook_url", "") ?: "") }
    var isSyncing by remember { mutableStateOf(false) }
    // State untuk menampilkan dialog filter tanggal
    var showExportDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {

        // --- HEADER: TOMBOL EXPORT EXCEL ---
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Riwayat Penjualan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { showExportDialog = true },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Export Excel")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Excel")
                }
                // Tombol Export Excel (yang sudah ada)
                // ... [kode tombol excel Anda sebelumnya] ...

                Spacer(modifier = Modifier.height(8.dp))

                // Tombol Baru: Sync ke Google Sheets
                Button(
                    onClick = { showSyncDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary, // Warna Gold
                        contentColor = androidx.compose.ui.graphics.Color.Black
                    )
                ) {
                    Text("Sync ke Google Sheets", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }
        }

        // --- DAFTAR TRANSAKSI ---
        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Belum ada riwayat transaksi.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(historyList, key = { it.transactionId }) { trx ->
                    Card(
                        onClick = { viewModel.loadTransactionDetails(trx) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(trx.transactionId, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(dateFormat.format(Date(trx.timestamp)), style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Pelanggan: ${trx.customerName}", style = MaterialTheme.typography.bodyMedium)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(trx.totalPrice.toRupiah(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("${trx.totalQty} Item", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG DATE RANGE PICKER (EXPORT) ---
    if (showExportDialog) {
        val dateRangePickerState = rememberDateRangePickerState()

        // 1. Inisialisasi Launcher Storage Access Framework
        val createDocumentLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/vnd.ms-excel")
        ) { uri ->
            if (uri != null) {
                try {
                    val flag = android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, flag)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val start = dateRangePickerState.selectedStartDateMillis
                val end = dateRangePickerState.selectedEndDateMillis

                if (start != null && end != null) {
                    val endAdjusted = end + (24 * 60 * 60 * 1000) - 1
                    viewModel.exportToExcel(context, start, endAdjusted, uri) { success, message ->
                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                        if (success) showExportDialog = false
                    }
                }
            } else {
                android.widget.Toast.makeText(context, "Penyimpanan dibatalkan", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        DatePickerDialog(
            onDismissRequest = { showExportDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        // 3. Munculkan System File Picker (User bebas pilih folder)
                        val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        val defaultFileName = "Rekap_UMKM_${fileDateFormat.format(Date())}.xls"
                        createDocumentLauncher.launch(defaultFileName)
                              },
                    enabled = dateRangePickerState.selectedStartDateMillis != null && dateRangePickerState.selectedEndDateMillis != null
                ) {
                    Text("Pilih Folder & Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Batal")
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = { Text(text = "Export Rekap Penjualan", modifier = Modifier.padding(start = 16.dp, top = 16.dp)) },
                headline = { Text(text = "Pilih Tanggal Mulai - Selesai", modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)) },
                showModeToggle = false,
                modifier = Modifier.weight(1f)
            )
        }
    }

    // --- POPUP DIALOG SYNC GOOGLE SHEETS ---
    if (showSyncDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSyncing) showSyncDialog = false },
            title = {
                Text("Sinkronisasi Google Sheets", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column {
                    Text(
                        text = "Masukkan URL Webhook dari Google Apps Script untuk mengirim data offline:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = webhookUrl,
                        onValueChange = { webhookUrl = it },
                        label = { Text("Webhook URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSyncing
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (webhookUrl.isNotBlank()) {
                            // Simpan URL agar tidak perlu ketik ulang besok
                            sharedPref.edit().putString("webhook_url", webhookUrl).apply()

                            isSyncing = true
                            viewModel.syncToGoogleSheets(webhookUrl) { success, message ->
                                isSyncing = false
                                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                                if (success) {
                                    showSyncDialog = false
                                }
                            }
                        } else {
                            android.widget.Toast.makeText(context, "URL Webhook tidak boleh kosong", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isSyncing
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Mulai Sync")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSyncDialog = false },
                    enabled = !isSyncing
                ) {
                    Text("Batal")
                }
            }
        )
    }
}
// ==========================================
// DIALOG POPUP DETAIL TRANSAKSI / STRUK
// ==========================================
@Composable
fun TransactionDetailDialog(
    header: com.rifqi.kasirumkm.data.local.TransactionEntity,
    items: List<com.rifqi.kasirumkm.data.local.TransactionItemEntity>,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp)
            ) {
                Icon(
                    Icons.Default.Receipt,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.CenterHorizontally).size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Detail Transaksi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Text("ID: ${header.transactionId}", style = MaterialTheme.typography.bodySmall)
                Text("Waktu: ${dateFormat.format(Date(header.timestamp))}", style = MaterialTheme.typography.bodySmall)
                Text("Pelanggan: ${header.customerName}", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(16.dp))
                Text("Rincian Item:", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(items) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${item.quantity}x ${item.productName}", modifier = Modifier.weight(1f))
                            Text(item.subtotal.toRupiah())
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Bayar", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        header.totalPrice.toRupiah(),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Tutup")
                }
            }
        }
    }
}

// ... KODE PosKasirTabScreen, KelolaMenuTabScreen, & CheckoutDialog TETAP SAMA SEPERTI SEBELUMNYA ...
// ==========================================
// 1. LAYAR KASIR POS (KATALOG & KERANJANG)
// ==========================================
@Composable
fun PosKasirTabScreen(viewModel: PosViewModel) {
    val products by viewModel.productList.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()

    if (products.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Belum ada menu produk.\nSilakan tambah produk di menu 'Kelola Menu'.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(products, key = { it.id }) { product ->
                val cartItem = cartItems.find { it.product.id == product.id }
                val qtyInCart = cartItem?.quantity ?: 0

                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (qtyInCart > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = product.price.toRupiah(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (qtyInCart == 0) {
                            Button(
                                onClick = { viewModel.addToCart(product) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Tambah")
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.removeFromCart(product) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Kurang")
                                }
                                Text(
                                    text = "$qtyInCart",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { viewModel.addToCart(product) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Tambah")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. LAYAR KELOLA MENU (DYNAMIC PRODUCTS CRUD)
// ==========================================
@Composable
fun KelolaMenuTabScreen(viewModel: PosViewModel) {
    val products by viewModel.productList.collectAsState()

    var nameInput by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Form Input Tambah Menu Baru
        Text(
            text = "Tambah Menu Baru",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            label = { Text("Nama Produk / Menu") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = priceInput,
            onValueChange = { priceInput = it },
            label = { Text("Harga Satuan (IDR)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                val priceDouble = priceInput.toDoubleOrNull() ?: 0.0
                if (nameInput.isNotBlank() && priceDouble > 0) {
                    viewModel.addMenuProduct(nameInput, priceDouble)
                    nameInput = ""
                    priceInput = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Simpan ke Katalog Menu")
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Daftar Menu Yang Ada
        Text(
            text = "Daftar Menu Aktif (${products.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(products, key = { it.id }) { product ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = product.price.toRupiah(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        IconButton(onClick = { viewModel.deleteMenuProduct(product) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Hapus Produk",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. DIALOG CHECKOUT (NAMA PELANGGAN & SUMMARY)
// ==========================================
@Composable
fun CheckoutDialog(
    totalPrice: Double,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var customerName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Konfirmasi Transaksi", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Total Bayar: ${totalPrice.toRupiah()}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Nama Pelanggan (Opsional)") },
                    placeholder = { Text("misal: Budi / Pelanggan 1") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(customerName) }) {
                Text("Selesaikan Transaksi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}