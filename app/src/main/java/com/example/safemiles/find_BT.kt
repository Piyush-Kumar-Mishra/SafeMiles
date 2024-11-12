package com.example.safemiles

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class find_BT : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var deviceListAdapter: ArrayAdapter<String>
    private val deviceList = ArrayList<String>()
    private val discoveredDevices = ArrayList<BluetoothDevice>()

    companion object {
        const val REQUEST_PERMISSIONS_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_bt)

        val deviceListView = findViewById<ListView>(R.id.device_list)
        deviceListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceList)
        deviceListView.adapter = deviceListAdapter

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (!bluetoothAdapter.isDiscovering) {
            checkPermissionsAndDiscoverDevices()
        }

        deviceListView.setOnItemClickListener { _, _, position, _ ->
            val device = discoveredDevices[position]
            device.createBond()

            val resultIntent = Intent().apply {
                putExtra(Dashboard.EXTRA_DEVICE_NAME, device.name)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun checkPermissionsAndDiscoverDevices() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSIONS_CODE
            )
        } else {
            discoverDevices()
        }
    }

    private fun discoverDevices() {
        bluetoothAdapter.startDiscovery()

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)

        Toast.makeText(this, "Discovering devices...", Toast.LENGTH_SHORT).show()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action: String? = intent?.action

            if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice? =
                    intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                device?.let {
                    val deviceName = it.name ?: "Unknown Device"
                    if (!deviceList.contains(deviceName)) {
                        deviceList.add(deviceName)
                        discoveredDevices.add(it)
                        deviceListAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }
}