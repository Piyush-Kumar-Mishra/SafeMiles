package com.example.safemiles

import ApiClient
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class chat_bot : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat_bot)
            val sendButton: Button= findViewById(R.id.sendMessageButton)
        val userMessageInput: EditText = findViewById(R.id.userMessageInput)
        val messagesTextView: TextView = findViewById(R.id.messagesTextView)

        sendButton.setOnClickListener {
            val userMessage = userMessageInput.text.toString()
            val chatbotRequest = ChatbotRequest(userMessage)


            ApiClient.instance.sendMessage(chatbotRequest).enqueue(object : Callback<ChatbotResponse> {
                override fun onResponse(call: Call<ChatbotResponse>, response: Response<ChatbotResponse>) {
                    if (response.isSuccessful) {
                        val botReply = response.body()?.reply

                        messagesTextView.append("\nYou: $userMessage")
                        messagesTextView.append("\nBot: $botReply")
                    }
                }
                override fun onFailure(call: Call<ChatbotResponse>, t: Throwable) {
                    messagesTextView.append("\nError: Could not connect to server")
                }
            })
        }
    }
}