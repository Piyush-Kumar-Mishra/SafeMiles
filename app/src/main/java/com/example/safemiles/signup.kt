package com.example.safemiles

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class signup : AppCompatActivity() {

    private lateinit var loginForm: CardView
    private lateinit var signupForm: CardView
    private lateinit var signupCard: CardView
    private lateinit var loginCard: CardView

    private val BASE_URL = "http://139.84.143.97:5000/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        loginForm = findViewById(R.id.loginFormCard)
        signupForm = findViewById(R.id.signupFormCard)
        signupCard = findViewById(R.id.signupCard)
        loginCard = findViewById(R.id.loginCard)

        val userUsername = findViewById<TextInputEditText>(R.id.signupUsername)
        val userEmail = findViewById<TextInputEditText>(R.id.signupEmail)
        val userPassword = findViewById<TextInputEditText>(R.id.signupPassword)
        val btnSignup = findViewById<Button>(R.id.signupButton)

        val loginUsername = findViewById<TextInputEditText>(R.id.loginUsername)
        val loginPassword = findViewById<TextInputEditText>(R.id.loginPassword)
        val btnLogin = findViewById<Button>(R.id.loginButton)

        val textLogin = findViewById<TextView>(R.id.login_text)


        animateLoginText(textLogin)

        btnSignup.setOnClickListener {
            val username = userUsername.text.toString()
            val email = userEmail.text.toString()
            val password = userPassword.text.toString()

            if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                registerUser(username, email, password)
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        // Login
        btnLogin.setOnClickListener {
            val loginUser = loginUsername.text.toString()
            val loginPass = loginPassword.text.toString()

            if (loginUser.isNotEmpty() && loginPass.isNotEmpty()) {
                loginUser(loginUser, loginPass)
            } else {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
            }
        }

        showLogin()

        loginCard.setOnClickListener {
            Log.d("SignupActivity", "Login Card Clicked")
            showLogin()
        }

        signupCard.setOnClickListener {
            Log.d("SignupActivity", "Signup Card Clicked")
            showSignup()
        }
    }

    private fun animateLoginText(textView: TextView) {
        val colorAnimator = ObjectAnimator.ofArgb(
            textView, "textColor",
            Color.parseColor("#8b00ff"), Color.parseColor("#00e571"), Color.WHITE
        )
        colorAnimator.duration = 2000
        colorAnimator.repeatCount = ValueAnimator.INFINITE
        colorAnimator.repeatMode = ValueAnimator.REVERSE
        colorAnimator.start()

        val translationAnimator = ObjectAnimator.ofFloat(textView, "translationX", -10f, 10f)
        translationAnimator.duration = 1000
        translationAnimator.repeatCount = ValueAnimator.INFINITE
        translationAnimator.repeatMode = ValueAnimator.REVERSE
        translationAnimator.start()

        val skewAnimator = ObjectAnimator.ofFloat(textView, "rotationX", 0f, 10f, -10f, 0f)
        skewAnimator.duration = 1000
        skewAnimator.repeatCount = ValueAnimator.INFINITE
        skewAnimator.start()
    }

    private fun registerUser(username: String, email: String, password: String) {
        val client = OkHttpClient()
        val url = "http://139.84.143.97:5000//add_user"
        val json = JSONObject()
        json.put("username", username)
        json.put("email", email)
        json.put("password", password)

        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())
        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@signup, "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@signup, "Registration successful!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@signup, Dashboard::class.java)
                        startActivity(intent)
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@signup, "Registration failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun loginUser(username: String, password: String) {
        val client = OkHttpClient()
        val url = "http://139.84.143.97:5000//login"
        val json = JSONObject()
        json.put("username", username)
        json.put("password", password)

        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())
        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@signup, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@signup, "Login successful!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@signup, Dashboard::class.java)
                        startActivity(intent)
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@signup, "Invalid username or password", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun showLogin() {
        loginForm.visibility = View.VISIBLE
        signupForm.visibility = View.GONE
        loginCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.black))
        signupCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white))
    }

    private fun showSignup() {
        loginForm.visibility = View.GONE
        signupForm.visibility = View.VISIBLE
        loginCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white))
        signupCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.black))
    }
}
