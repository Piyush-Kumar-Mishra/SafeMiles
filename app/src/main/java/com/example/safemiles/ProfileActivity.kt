package com.example.safemiles

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Retrieve data passed from Dashboard
        val name = intent.getStringExtra("name") ?: "N/A"
        val age = intent.getStringExtra("age") ?: "N/A"
        val height = intent.getStringExtra("height") ?: "N/A"
        val weight = intent.getStringExtra("weight") ?: "N/A"

        // Find TextViews and set the data
        findViewById<TextView>(R.id.name_text).text = "Name: $name"
        findViewById<TextView>(R.id.age_text).text = "Age: $age"
        findViewById<TextView>(R.id.height_text).text = "Height: $height cm"
        findViewById<TextView>(R.id.weight_text).text = "Weight: $weight kg"
    }
}
