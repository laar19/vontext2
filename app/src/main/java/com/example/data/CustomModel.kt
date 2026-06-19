package com.example.data

data class CustomModel(
    val id: String,
    val name: String,
    val type: String, // "GEMINI" or "OPENAI_COMPATIBLE"
    val apiKey: String,
    val endpoint: String,
    val modelName: String
)
