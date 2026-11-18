# Kasir UMKM — Offline-First Mobile Point of Sale (POS)

**Kasir UMKM** is a native Android Point of Sale (POS) application built for local micro, small, and medium enterprises (MSMEs / UMKM). Designed with an offline-first architecture, it ensures uninterrupted daily sales operations without requiring a constant internet connection, while offering one-tap cloud backup to Google Sheets and local Excel reporting.

---

## Key Features

* **Offline-First POS & Product Catalog**: Manage menu items and process customer checkouts smoothly using local SQLite persistence via Room Database.
* **Atomic Transaction Recording**: Safeguards data integrity by recording both transaction headers (`transactions`) and granular item breakdowns (`transaction_items`) atomically via `RoomDatabase.withTransaction`.
* **Receipt Detail Preview**: View itemized digital receipts immediately after checkout or from the transaction history list.
* **Custom Date-Range Excel Export**: Generate multi-sheet `.xls` sales reports (*Ringkasan Nota* & *Rincian Item*) using Apache POI, saved directly to user-selected directories via Android's Storage Access Framework (SAF) with timestamped filenames (`Rekap_UMKM_YYYYMMDD_HHMMSS.xls`).
* **Dynamic Google Sheets Cloud Sync**: Synchronize unsynced offline transactions (`isSynced = false`) directly to a Google Spreadsheet via a customizable Google Apps Script Web App URL stored locally in `SharedPreferences`.
* **Dark Emerald & Gold UI**: Built with Jetpack Compose and Material 3 featuring a high-contrast Deep Black, Dark Emerald Green, and Gold theme optimized for AMOLED displays and fast cashier readability.

---

## Tech Stack & Architecture

* **Language**: Kotlin
* **UI Framework**: Jetpack Compose (Material 3)
* **Architecture**: MVVM (Model-View-ViewModel) with Kotlin Coroutines & StateFlow
* **Local Persistence**: Room Database (SQLite) + KSP
* **Networking**: Retrofit 2, Gson Converter & OkHttp Logging Interceptor (Dynamic `@Url` webhook routing)
* **Spreadsheet Generation**: Apache POI (`3.17`) + Storage Access Framework (SAF)

---

## Database Schema

The local Room database (`AppDatabase`) consists of three core entities:
1. **`products`**: Stores available menu items, pricing, and availability status.
2. **`transactions`**: Stores order headers (`transactionId`, `timestamp`, `totalPrice`, `totalQty`, `customerName`, `isSynced`).
3. **`transaction_items`**: Linked via `ForeignKey` (`CASCADE`) to `transactions`, storing itemized product names, unit prices, quantities, and subtotals per order.

---

## Google Sheets Webhook Setup

To enable cloud synchronization, deploy a Google Apps Script bound to your target Google Sheet:

1. Open your Google Sheet and navigate to **Extensions > Apps Script**.
2. Paste the following webhook handler:
   ```javascript
   function doPost(e) {
     try {
       var sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName("Transactions");
       if (!sheet) {
         sheet = SpreadsheetApp.getActiveSpreadsheet().insertSheet("Transactions");
         sheet.appendRow(["ID Transaksi", "Waktu", "Pelanggan", "Nama Produk", "Harga", "Qty", "Subtotal"]);
       }
       var data = JSON.parse(e.postData.contents);
       data.transactions.forEach(function(trx) {
         trx.items.forEach(function(item) {
           sheet.appendRow([
             trx.transactionId,
             trx.timestamp,
             trx.customerName,
             item.productName,
             item.unitPrice,
             item.quantity,
             item.subtotal
           ]);
         });
       });
       return ContentService.createTextOutput(JSON.stringify({"status": "success"}))
         .setMimeType(ContentService.MimeType.JSON);
     } catch(error) {
       return ContentService.createTextOutput(JSON.stringify({"status": "error", "message": error.toString()}))
         .setMimeType(ContentService.MimeType.JSON);
     }
   }
