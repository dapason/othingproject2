package com.example.myproject

// Class ini berfungsi sebagai format data yang akan dikirim ke Firebase
data class PredictionHistory(
    val id: String? = null,
    val brand: String? = null,
    val ram: Int = 0,
    val storage: Int = 0,
    val age: Float = 0f,
    val condition: Int = 0,
    val predictedPrice: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis() // Menyimpan waktu saat prediksi
)