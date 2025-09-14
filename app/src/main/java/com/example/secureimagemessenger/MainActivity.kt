package com.example.secureimagemessenger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.example.secureimagemessenger.data.ApiInterface
import com.example.secureimagemessenger.data.ApiService
import com.example.secureimagemessenger.screen.AppRoot

class MainActivity : ComponentActivity() {
    private val api: ApiInterface by lazy {
        val baseUrl = getString(R.string.api_base_url)
        ApiService.create(baseUrl)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { AppRoot(api = api) } }
    }
}
