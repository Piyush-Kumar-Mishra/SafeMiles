package com.example.safemiles

data class User(val username: String, val email: String, val password: String)
data class ApiResponse(val message: String)
data class ChatbotRequest(val message: String)
data class ChatbotResponse(val reply: String)