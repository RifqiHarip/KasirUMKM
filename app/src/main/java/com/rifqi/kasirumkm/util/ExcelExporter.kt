package com.rifqi.kasirumkm.util

import android.content.Context
import android.net.Uri
import com.rifqi.kasirumkm.data.local.PosDao
import com.rifqi.kasirumkm.data.local.TransactionEntity
import com.rifqi.kasirumkm.data.local.TransactionItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Workbook
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelExporter {

    suspend fun exportTransactionsToExcel(
        context: Context,
        posDao: PosDao,
        transactions: List<TransactionEntity>,
        targetUri: Uri
    ): Boolean = withContext(Dispatchers.IO) {
        val displayDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val workbook: Workbook = HSSFWorkbook()

        try {
            // SHEET 1: RINGKASAN NOTA
            val sheetHeader = workbook.createSheet("Ringkasan Nota")
            val headersSheet1 = listOf("ID Transaksi", "Waktu", "Pelanggan", "Qty", "Total Bayar", "Status Sync")
            val rowHeader1 = sheetHeader.createRow(0)

            headersSheet1.forEachIndexed { index, title ->
                rowHeader1.createCell(index).setCellValue(title)
            }

            var rowIndex1 = 1
            for (trx in transactions) {
                val row = sheetHeader.createRow(rowIndex1++)
                row.createCell(0).setCellValue(trx.transactionId)
                row.createCell(1).setCellValue(displayDateFormat.format(Date(trx.timestamp)))
                row.createCell(2).setCellValue(trx.customerName)
                row.createCell(3).setCellValue(trx.totalQty.toDouble())
                row.createCell(4).setCellValue(trx.totalPrice)
                row.createCell(5).setCellValue(if (trx.isSynced) "Ter-sync" else "Pending")
            }
            // autoSizeColumn dihapus agar tidak memanggil java.awt di Android

            // SHEET 2: RINCIAN ITEM
            val sheetDetail = workbook.createSheet("Rincian Item")
            val headersSheet2 = listOf("ID Transaksi", "Nama Produk", "Harga Satuan", "Qty", "Subtotal")
            val rowHeader2 = sheetDetail.createRow(0)

            headersSheet2.forEachIndexed { index, title ->
                rowHeader2.createCell(index).setCellValue(title)
            }

            var rowIndex2 = 1
            for (trx in transactions) {
                val items: List<TransactionItemEntity> = posDao.getItemsForTransaction(trx.transactionId)
                for (item in items) {
                    val row = sheetDetail.createRow(rowIndex2++)
                    row.createCell(0).setCellValue(trx.transactionId)
                    row.createCell(1).setCellValue(item.productName)
                    row.createCell(2).setCellValue(item.unitPrice)
                    row.createCell(3).setCellValue(item.quantity.toDouble())
                    row.createCell(4).setCellValue(item.subtotal)
                }
            }
            // autoSizeColumn dihapus

            // Tulis Workbook langsung ke URI (File Picker SAF)
            context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                workbook.write(outputStream)
            }
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            android.util.Log.e("ExcelExporter", "Gagal export Excel: ${e.localizedMessage}", e)
            false
        } finally {
            workbook.close()
        }
    }
}