package com.example.safemiles

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class for_emergency : AppCompatActivity() {

    private lateinit var emergencyNumber: String
    private val client = OkHttpClient()

    companion object {
        const val BASE_URL = "http://139.84.143.97:5000/"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_for_emergency)

        val btnSubmit: Button = findViewById(R.id.btnSubmit)
        val inputNumber: EditText = findViewById(R.id.inputNumber)
        val btnSendMessage: Button = findViewById(R.id.btnSendMessage)

        btnSubmit.setOnClickListener {
            val number = inputNumber.text.toString()
            if (number.isNotEmpty()) {
                saveEmergencyContact(number)
            } else {
                Toast.makeText(this, "Please enter a number", Toast.LENGTH_SHORT).show()
            }
        }
        btnSendMessage.setOnClickListener {
            fetchEmergencyContactAndSendMessage()
        }
    }

    private fun sendEmergencyMessage(emergencyNumber: String) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), 1)
        } else {
            try {
                val smsManager = SmsManager.getDefault()
                val emergencyMessage =
                    "This is an emergency message. Please check on the delivery partner's status."
                smsManager.sendTextMessage(emergencyNumber, null, emergencyMessage, null, null)
                Toast.makeText(
                    applicationContext,
                    "Emergency message sent to $emergencyNumber",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    applicationContext,
                    "Failed to send message: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    private fun saveEmergencyContact(emergencyNumber: String) {
        val url = BASE_URL + "save_emergency_contact"
        val json = JSONObject()
        json.put("user_id", "123")
        json.put("emergency_number", emergencyNumber)

        val requestBody =
            json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(applicationContext, "Failed to save contact", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            applicationContext,
                            "Emergency contact saved successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            applicationContext,
                            "Error: ${response.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }

    private fun fetchEmergencyContactAndSendMessage() {
        val url = BASE_URL + "get_emergency_contact/123"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        applicationContext,
                        "Failed to fetch contact",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse = JSONObject(responseBody ?: "")
                    val emergencyNumber = jsonResponse.optString("emergency_number")

                    runOnUiThread {
                        if (emergencyNumber.isNotEmpty()) {
                            sendEmergencyMessage(emergencyNumber)
                        } else {
                            Toast.makeText(
                                applicationContext,
                                "Emergency contact not found",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            applicationContext,
                            "Error: ${response.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }
}
