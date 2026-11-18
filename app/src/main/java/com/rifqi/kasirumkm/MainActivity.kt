package com.rifqi.kasirumkm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.rifqi.kasirumkm.data.local.AppDatabase
import com.rifqi.kasirumkm.ui.MainPosScreen
import com.rifqi.kasirumkm.ui.PosViewModel
import com.rifqi.kasirumkm.ui.PosViewModelFactory
import com.rifqi.kasirumkm.ui.theme.KasirUMKMTheme

class MainActivity : ComponentActivity() {

    // Memanggil Factory untuk membuat ViewModel yang berisi AppDatabase
    private val viewModel: PosViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        PosViewModelFactory(database)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KasirUMKMTheme {
                MainPosScreen(viewModel = viewModel)
            }
        }
    }
}