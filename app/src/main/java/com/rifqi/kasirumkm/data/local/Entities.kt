package com.rifqi.kasirumkm.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabel Katalog Menu (Product) yang fully customizable (Bisa Tambah, Edit, Hapus)
 */
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val price: Double,
    val isAvailable: Boolean = true
)

/**
 * Tabel Header Transaksi Nota Kasir
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = false)
    val transactionId: String, // Format: TRX-YYYYMMDDHHMMSS
    val customerName: String,
    val totalQty: Int,
    val totalPrice: Double,
    val timestamp: Long, // Epoch timestamp (milliseconds) untuk filter datetime
    val isSynced: Boolean = false // Marker status sinkronisasi ke Google Sheets
)

/**
 * Tabel Rincian Detail Item dalam Nota Transaksi
 */
@Entity(
    tableName = "transaction_items",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["transactionId"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["transactionId"])]
)
data class TransactionItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: String,
    val productName: String, // Snapshot nama barang agar histori aman meski menu utama dihapus
    val unitPrice: Double,
    val quantity: Int,
    val subtotal: Double
)