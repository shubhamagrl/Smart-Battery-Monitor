package com.example.smartbatterymonitor

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.smartbatterymonitor.databinding.ActivityHomePageBinding
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.io.IOException
import java.util.concurrent.Executors

class HomePage : AppCompatActivity() {

    private lateinit var usbManager: UsbManager
    private var serialPort: UsbSerialPort? = null
    private lateinit var voltageTextView: TextView
    private lateinit var temperatureTextView: TextView
    lateinit var binding: ActivityHomePageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_main)

        // Initialize views
        voltageTextView = findViewById(R.id.voltageTextView)
        temperatureTextView = findViewById(R.id.temperatureTextView)

        // Initialize USB manager
        usbManager = getSystemService(USB_SERVICE) as UsbManager

        // Attempt to connect to Arduino
        connectToArduino()
    }

    private fun connectToArduino() {
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (availableDrivers.isEmpty()) {
            Toast.makeText(this, "No USB device found", Toast.LENGTH_LONG).show()
            return
        }

        val driver = availableDrivers[0]
        val connection = usbManager.openDevice(driver.device)
        if (connection == null) {
            Toast.makeText(this, "Unable to open USB device", Toast.LENGTH_LONG).show()
            return
        }

        // Get the first port from the driver and configure it
        serialPort = driver.ports[0]
        serialPort?.let {
            try {
                it.open(connection)
                it.setParameters(
                    9600,
                    UsbSerialPort.DATABITS_8,
                    UsbSerialPort.STOPBITS_1,
                    UsbSerialPort.PARITY_NONE
                )
                startReadingData()
            } catch (e: IOException) {
                Toast.makeText(this, "Error setting up serial port", Toast.LENGTH_LONG).show()
                e.printStackTrace()
                it.close()
            }
        }
    }

    private fun startReadingData() {
        val usbIoManager = SerialInputOutputManager(serialPort, object : SerialInputOutputManager.Listener {
            override fun onNewData(data: ByteArray?) {
                data?.let {
                    val receivedText = String(it)
                    parseAndDisplayData(receivedText)
                }
            }

            override fun onRunError(e: Exception?) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error reading data", Toast.LENGTH_LONG).show()
                }
            }
        })
        Executors.newSingleThreadExecutor().submit(usbIoManager)
    }

    private fun parseAndDisplayData(data: String) {
        // Match JSON formatted data, for example: {"voltage":3.5,"temperature":25.0}
        val regex = Regex("""\{"voltage":(\d+\.\d+),"temperature":(\d+\.\d+)\}""")
        val matchResult = regex.find(data)

        matchResult?.let {
            val voltage = it.groupValues[1]
            val temperature = it.groupValues[2]

            // Update the UI on the main thread
            runOnUiThread {
                voltageTextView.text = "Voltage: $voltage V"
                temperatureTextView.text = "Temperature: $temperature °C"
            }
        } ?: run {
            // Optionally show a message if data format is incorrect
            runOnUiThread {
                Toast.makeText(this, "Invalid data received", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        serialPort?.close()
        super.onDestroy()
    }
}
