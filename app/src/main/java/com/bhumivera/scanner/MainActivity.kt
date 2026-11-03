package com.bhumivera.scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.bhumivera.scanner.data.BhumiveraService
import com.bhumivera.scanner.data.SessionStore
import com.bhumivera.scanner.ui.BhumiveraTheme
import com.bhumivera.scanner.ui.ScannerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionStore = SessionStore(applicationContext)
        val service = BhumiveraService.create(sessionStore)

        setContent {
            BhumiveraTheme {
                ScannerScreen(service = service, sessionStore = sessionStore)
            }
        }
    }
}
