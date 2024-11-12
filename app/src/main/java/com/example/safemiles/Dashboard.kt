package com.example.safemiles

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.view.Menu
import android.view.MenuItem
import android.app.AlertDialog
import android.bluetooth.BluetoothClass
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.widget.Toolbar
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.location.LocationListener

class Dashboard : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var connectedWatchText: TextView
    private var connectedDevice: BluetoothDevice? = null
    private lateinit var mMap: GoogleMap
    private lateinit var profileTextView: TextView
    private val client = OkHttpClient()
    private lateinit var locationManager: LocationManager
    private var currentLocation: LatLng? = null
    private lateinit var emergencyNumber: String
    private lateinit var currentLocationTextView: TextView

    companion object {
        const val REQUEST_BT_DEVICE = 1
        const val REQUEST_LOCATION_PERMISSION = 2
        const val REQUEST_BLUETOOTH_PERMISSIONS = 3
        const val EXTRA_DEVICE_NAME = "DEVICE_NAME"
        const val BASE_URL = "http://139.84.143.97:5000/"
    }

    private val drinkWaterHandler = Handler()
    private val restHandler = Handler()
    private val drinkWaterInterval: Long = 1 * 60 * 1000
    private val restInterval: Long = 1 * 60 * 1000

private val locationListener = LocationListener { location ->
    currentLocation = LatLng(location.latitude, location.longitude)
    currentLocation?.let { loc ->
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 15f))
        mMap.clear()
        mMap.addMarker(MarkerOptions().position(loc).title("Your Location"))

        currentLocationTextView.text = "Location: ${loc.latitude}, ${loc.longitude}"
    }
}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        connectedWatchText = findViewById(R.id.connected_watch_text)
        profileTextView = findViewById(R.id.profile_text_view)
          currentLocationTextView = findViewById(R.id.current_location_text)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val btn_to_chatbot=findViewById<ImageButton>(R.id.btn_chatbot)
        val emergency_activity=findViewById<Button>(R.id.btn_to_emergency)
        val report_user=findViewById<ImageButton>(R.id.report)

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        registerReceiver(btConnectionReceiver, filter)

        checkForAlreadyConnectedDevice()

        checkBluetoothPermissions()

        val addPersonButton: ImageButton = findViewById(R.id.add_person)
        addPersonButton.setOnClickListener {
            showInputDialog()
        }

        btn_to_chatbot.setOnClickListener {
            val intent=Intent(this,chat_bot::class.java)
            startActivity(intent)
        }

        fetchEmergencyContactAndStartReminders()

        emergency_activity.setOnClickListener {
            val intent=Intent(this, for_emergency::class.java)
            startActivity(intent)
        }

        report_user.setOnClickListener{
            val intent=Intent(this, Daily_Report::class.java)
            startActivity(intent)
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }



    private fun getCurrentLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, locationListener)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (::mMap.isInitialized) {
                mMap.isMyLocationEnabled = true
                getCurrentLocation()
            }
        }
    }

    private fun fetchEmergencyContactAndStartReminders() {
        val url = BASE_URL + "get_emergency_contact/123"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    val locationMessage = if (currentLocation != null) {
                        "Your current location is ${currentLocation!!.latitude}, ${currentLocation!!.longitude}"
                    } else {
                        "Location unavailable"
                    }
                    Toast.makeText(
                        applicationContext,
                        "Failed to fetch contact. $locationMessage",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse = JSONObject(responseBody)
                    emergencyNumber = jsonResponse.optString("emergency_number")

                    runOnUiThread {
                        if (emergencyNumber.isNotEmpty()) {
                            startDrinkWaterReminder()
                            startRestReminder()
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
                        val locationMessage = if (currentLocation != null) {
                            "Your current location is ${currentLocation!!.latitude}, ${currentLocation!!.longitude}"
                        } else {
                            "Location unavailable"
                        }
                        Toast.makeText(
                            applicationContext,
                            "Error: ${response.message}. $locationMessage",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        })
    }

    private val drinkWaterRunnable = object : Runnable {
        override fun run() {
            sendReminderMessage(emergencyNumber, "Reminder: Please drink water to stay hydrated.")
            drinkWaterHandler.postDelayed(this, drinkWaterInterval)
        }
    }

    private val restRunnable = object : Runnable {
        override fun run() {
            sendReminderMessage(
                emergencyNumber,
                "Reminder: Please take a short rest to avoid exhaustion."
            )
            restHandler.postDelayed(this, restInterval)
        }
    }

    private fun startDrinkWaterReminder() {
        drinkWaterHandler.postDelayed(drinkWaterRunnable, drinkWaterInterval)
    }

    private fun startRestReminder() {
        restHandler.postDelayed(restRunnable, restInterval)
    }

    private fun sendReminderMessage(emergencyNumber: String, message: String) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), 1)
        } else {
            try {
                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(emergencyNumber, null, message, null, null)
                Toast.makeText(
                    applicationContext,
                    "Reminder sent to $emergencyNumber",
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

 override fun onMapReady(googleMap: GoogleMap) {
    mMap = googleMap
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        mMap.isMyLocationEnabled = true
        getCurrentLocation()
    } else {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_LOCATION_PERMISSION
        )
    }
}

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.bottom_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.profile -> {
                openProfileScreen()
                true
            }

            R.id.location_btn -> {
                checkLocationPermissionAndEnable()
                true
            }

            R.id.connected_watch_text -> {
                if (!bluetoothAdapter.isEnabled) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(enableBtIntent, BluetoothAdapter.STATE_ON)
                } else {
                    val intent = Intent(this, find_BT::class.java)
                    startActivityForResult(intent, REQUEST_BT_DEVICE)
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
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
                    val jsonResponse = JSONObject(responseBody)
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

    private fun showInputDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.input_dialog, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.input_name)
        val ageInput = dialogView.findViewById<EditText>(R.id.input_age)
        val heightInput = dialogView.findViewById<EditText>(R.id.input_height)
        val weightInput = dialogView.findViewById<EditText>(R.id.input_weight)

        AlertDialog.Builder(this)
            .setTitle("Enter Your Profile")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = nameInput.text.toString()
                val age = ageInput.text.toString()
                val height = heightInput.text.toString()
                val weight = weightInput.text.toString()
                saveProfileData(name, age, height, weight)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveProfileData(name: String, age: String, height: String, weight: String) {
        val jsonObject = JSONObject()
        jsonObject.put("name", name)
        jsonObject.put("age", age)
        jsonObject.put("height", height)
        jsonObject.put("weight", weight)

        val requestBody =
            jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("http://139.84.143.97:5000/add_profile")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@Dashboard, "Failed to save profile", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(
                            this@Dashboard,
                            "Profile saved successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@Dashboard, "Error saving profile", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        })
    }

    private fun openProfileScreen() {
        val request = Request.Builder()
            .url("http://139.84.143.97:5000/get_profile/1")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@Dashboard, "Failed to load profile", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.let { responseBody ->
                        val json = JSONObject(responseBody.string())
                        val name = json.getString("name")
                        val age = json.getString("age")
                        val height = json.getString("height")
                        val weight = json.getString("weight")

                        val intent = Intent(this@Dashboard, ProfileActivity::class.java)
                        intent.putExtra("name", name)
                        intent.putExtra("age", age)
                        intent.putExtra("height", height)
                        intent.putExtra("weight", weight)
                        startActivity(intent)
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@Dashboard, "Error loading profile", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        })
    }

    private fun checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ), REQUEST_BLUETOOTH_PERMISSIONS
                )
            }
        }
    }

    private fun checkLocationPermissionAndEnable() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder(this)
                .setTitle("Location Permission")
                .setMessage("Location is required for Bluetooth scanning. Enable location?")
                .setPositiveButton("Yes") { _, _ ->
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .setNegativeButton("No", null)
                .show()
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    private fun checkForAlreadyConnectedDevice() {
        val connectedDevices = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET)
        if (connectedDevices == BluetoothProfile.STATE_CONNECTED) {
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
            if (pairedDevices != null) {
                for (device in pairedDevices) {
                    if (device.bluetoothClass.deviceClass == BluetoothClass.Device.WEARABLE_WRIST_WATCH) {
                        connectedDevice = device
                        connectedWatchText.text = "Connected to ${device.name}"
                        break
                    }
                }
            }
        }
    }

    private val btConnectionReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            val device = intent?.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

            when (action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    if (device != null && device.bluetoothClass.deviceClass == BluetoothClass.Device.WEARABLE_WRIST_WATCH) {
                        connectedWatchText.text = "Connected to ${device.name}"
                    }
                }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    connectedWatchText.text = "Not Connected"
                }
            }
        }
    }

    override fun onDestroy() {
        drinkWaterHandler.removeCallbacks(drinkWaterRunnable)
        restHandler.removeCallbacks(restRunnable)
    }
}