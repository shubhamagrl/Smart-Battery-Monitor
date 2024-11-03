package com.example.smartbatterymonitor

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.media.audiofx.BassBoost
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.example.smartbatterymonitor.databinding.ActivityMainBinding
import android.Manifest
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private val REQUEST_CODE_ENABLE_BT = 1
    private val REQUEST_CODE_DISCOVERABLE_BT = 2
    private val REQUEST_BLUETOOTH_PERMISSION = 3

    lateinit var binding: ActivityMainBinding
    lateinit var bluetoothAdapter: BluetoothAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null){
            binding.idStatus.text = "Bluetooth is not available"
        }
        else{
            binding.idStatus.text = "Bluetooth is available"
        }

        if(bluetoothAdapter.isEnabled){
            binding.idStatusImage.setImageResource(R.drawable.bluetooth_enabled)
        }
        else{
            binding.idStatusImage.setImageResource(R.drawable.bluetooth_disabled)
        }

        binding.idButton.setOnClickListener{
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED) {
                handleBluetoothToggle()
            } else {
                // Request BLUETOOTH_CONNECT permission
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    REQUEST_BLUETOOTH_PERMISSION)
            }
        }
        binding.idDiscoverable.setOnClickListener{
            if(!bluetoothAdapter.isEnabled){
                Toast.makeText(this,"Turn on Bluetooth first",Toast.LENGTH_SHORT).show()
            }
            else if(!bluetoothAdapter.isDiscovering()){
                Toast.makeText(this,"Making your device discoverable",Toast.LENGTH_SHORT).show()
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                startActivityForResult(intent,REQUEST_CODE_DISCOVERABLE_BT)
            }
        }
        binding.idPairedText.setOnClickListener{
            if(bluetoothAdapter.isEnabled){
                binding.idPairedText.text = "Paired Devices"
                val devices = bluetoothAdapter.bondedDevices
                for(device in devices) {
                    val deviceName = device.name
                    val deviceAddress = device
                    binding.idPairedText.append("\nDevice: $deviceName, $deviceAddress")
                }
            }
            else{
                Toast.makeText(this,"Turn on Bluetooth first",Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode){
            REQUEST_CODE_ENABLE_BT ->
                if(resultCode == Activity.RESULT_OK){
                    binding.idStatusImage.setImageResource(R.drawable.bluetooth_enabled)
                    Toast.makeText(this, "Bluetooth is turned on", Toast.LENGTH_SHORT).show()
                }
            else{
                Toast.makeText(this,"Could not turn on Bluetooth", Toast.LENGTH_SHORT).show()
                }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
    fun handleBluetoothToggle(){
        if(bluetoothAdapter.isEnabled){
            Toast.makeText(this, "Please disable Bluetooth manually from Settings", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            startActivity(intent)
        }
        else{
            Toast.makeText(this, "Please enable Bluetooth manually from Settings", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            startActivity(intent)
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                handleBluetoothToggle()
            } else {
                Toast.makeText(this, "Permission denied to use Bluetooth", Toast.LENGTH_SHORT).show()
            }
        }
    }
}