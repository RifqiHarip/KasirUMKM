package com.rifqi.kasirumkm.data.remote

data class SyncRequest(
    val transactions: List<SyncTransaction>
)

data class SyncTransaction(
    val transactionId: String,
    val timestamp: String,
    val customerName: String,
    val items: List<SyncItem>
)

data class SyncItem(
    val productName: String,
    val unitPrice: Double,
    val quantity: Int,
    val subtotal: Double
)

data class SyncResponse(
    val status: String,
    val message: String?
)