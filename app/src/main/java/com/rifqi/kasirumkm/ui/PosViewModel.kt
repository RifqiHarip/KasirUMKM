package com.rifqi.kasirumkm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rifqi.kasirumkm.data.local.PosDao
import com.rifqi.kasirumkm.data.local.ProductEntity
import com.rifqi.kasirumkm.data.local.TransactionEntity
import com.rifqi.kasirumkm.data.local.TransactionItemEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.room.withTransaction
import com.rifqi.kasirumkm.data.local.AppDatabase

import com.rifqi.kasirumkm.data.remote.ApiClient
import com.rifqi.kasirumkm.data.remote.SyncItem
import com.rifqi.kasirumkm.data.remote.SyncRequest
import com.rifqi.kasirumkm.data.remote.SyncTransaction
import kotlinx.coroutines.flow.first

data class CartItem(val product: ProductEntity, var quantity: Int)

class PosViewModel(private val database: AppDatabase) : ViewModel() {
    private val posDao = database.posDao()
    val productList: StateFlow<List<ProductEntity>> = posDao.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Riwayat Transaksi untuk Tab History
    val historyList: StateFlow<List<TransactionEntity>> = posDao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart State
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    val totalItemsCount: StateFlow<Int> = _cartItems.map { it.sumOf { item -> item.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalPrice: StateFlow<Double> = _cartItems.map { it.sumOf { item -> item.product.price * item.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // State untuk menampilkan Popup Detail Transaksi (Struk)
    private val _showDetailDialog = MutableStateFlow<Pair<TransactionEntity, List<TransactionItemEntity>>?>(null)
    val showDetailDialog: StateFlow<Pair<TransactionEntity, List<TransactionItemEntity>>?> = _showDetailDialog.asStateFlow()

    // --- CRUD MENU ---
    fun addMenuProduct(name: String, price: Double) {
        if (name.isBlank() || price <= 0.0) return
        viewModelScope.launch { posDao.insertProduct(ProductEntity(name = name.trim(), price = price)) }
    }

    fun deleteMenuProduct(product: ProductEntity) {
        viewModelScope.launch {
            posDao.deleteProduct(product)
            removeFromCartCompletely(product)
        }
    }

    // --- CART OPS ---
    fun addToCart(product: ProductEntity) {
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == product.id }
        if (index != -1) {
            currentList[index] = currentList[index].copy(quantity = currentList[index].quantity + 1)
        } else {
            currentList.add(CartItem(product, 1))
        }
        _cartItems.value = currentList
    }

    fun removeFromCart(product: ProductEntity) {
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == product.id }
        if (index != -1) {
            if (currentList[index].quantity > 1) {
                currentList[index] = currentList[index].copy(quantity = currentList[index].quantity - 1)
            } else {
                currentList.removeAt(index)
            }
            _cartItems.value = currentList
        }
    }

    private fun removeFromCartCompletely(product: ProductEntity) {
        _cartItems.value = _cartItems.value.filter { it.product.id != product.id }
    }

    fun clearCart() { _cartItems.value = emptyList() }

    // --- CHECKOUT & TRANSACTION DETAILS ---
    fun checkout(customerName: String, onComplete: () -> Unit) {
        val items = _cartItems.value
        if (items.isEmpty()) return

        val trxId = "TRX-${System.currentTimeMillis()}"
        val totalPrice = items.sumOf { it.product.price * it.quantity }
        val totalQty = items.sumOf { it.quantity }

        val transactionHeader = TransactionEntity(
            transactionId = trxId,
            timestamp = System.currentTimeMillis(),
            totalPrice = totalPrice,
            totalQty = totalQty,
            customerName = customerName,
            isSynced = false
        )

        val transactionItems = items.map { cartItem ->
            TransactionItemEntity(
                transactionId = trxId,
                productName = cartItem.product.name,
                unitPrice = cartItem.product.price,
                quantity = cartItem.quantity,
                subtotal = cartItem.product.price * cartItem.quantity
            )
        }

        viewModelScope.launch {
            // 3. Eksekusi 100% AMAN di dalam 1 transaksi Room
            database.withTransaction {
                posDao.insertTransaction(transactionHeader)
                posDao.insertTransactionItems(transactionItems)
            }

            clearCart()
            onComplete()
            _showDetailDialog.value = Pair(transactionHeader, transactionItems)
        }
    }
    // ==========================================
    // TAMBAHKAN FUNGSI INI DI DALAM PosViewModel
    // ==========================================
    fun exportToExcel(
        context: android.content.Context,
        startTimestamp: Long,
        endTimestamp: Long,
        targetUri: android.net.Uri, // Menerima URI dari Storage Access Framework
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val transactions = posDao.getTransactionsByDateRange(startTimestamp, endTimestamp)
                if (transactions.isEmpty()) {
                    onResult(false, "Tidak ada data transaksi pada rentang tanggal tersebut.")
                    return@launch
                }

                // Panggil ExcelExporter dengan targetUri
                val success = com.rifqi.kasirumkm.util.ExcelExporter.exportTransactionsToExcel(
                    context, posDao, transactions, targetUri
                )

                if (success) {
                    onResult(true, "File Excel berhasil disimpan!")
                } else {
                    onResult(false, "Gagal menulis file Excel.")
                }
            } catch (e: Exception) {
                onResult(false, "Terjadi kesalahan: ${e.localizedMessage}")
            }
        }
    }

    // Fungsi untuk memuat rincian transaksi jika diklik dari Riwayat
    fun loadTransactionDetails(transaction: TransactionEntity) {
        viewModelScope.launch {
            val items = posDao.getItemsForTransaction(transaction.transactionId)
            _showDetailDialog.value = Pair(transaction, items)
        }
    }

    fun closeDetailDialog() {
        _showDetailDialog.value = null
    }
    fun syncToGoogleSheets(webhookUrl: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                // Ambil semua transaksi (bisa difilter yang isSynced == false saja lewat Dao)
                // Untuk contoh ini, kita filter dari getAllTransactions yang sudah ada
                val allTransactions = posDao.getAllTransactions().first()
                val unsyncedTransactions = allTransactions.filter { !it.isSynced }

                if (unsyncedTransactions.isEmpty()) {
                    onResult(true, "Tidak ada data baru untuk disinkronkan.")
                    return@launch
                }

                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                val syncTransactions = mutableListOf<SyncTransaction>()

                // Ambil rincian item untuk setiap transaksi
                for (trx in unsyncedTransactions) {
                    val items = posDao.getItemsForTransaction(trx.transactionId)
                    val syncItems = items.map {
                        SyncItem(it.productName, it.unitPrice, it.quantity, it.subtotal)
                    }

                    syncTransactions.add(
                        SyncTransaction(
                            transactionId = trx.transactionId,
                            timestamp = dateFormat.format(Date(trx.timestamp)),
                            customerName = trx.customerName,
                            items = syncItems
                        )
                    )
                }

                val requestPayload = SyncRequest(syncTransactions)

                // Tembak ke API Google Apps Script
                val response = ApiClient.api.syncData(webhookUrl, requestPayload)

                if (response.isSuccessful && response.body()?.status == "success") {
                    // Update status di Room Database
                    val syncedIds = unsyncedTransactions.map { it.transactionId }
                    posDao.markAsSynced(syncedIds)
                    onResult(true, "Berhasil sinkronisasi ${syncedIds.size} transaksi ke Google Sheets!")
                } else {
                    onResult(false, "Gagal sinkronisasi: ${response.body()?.message ?: response.code()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Error: ${e.localizedMessage}")
            }
        }
    }

}

