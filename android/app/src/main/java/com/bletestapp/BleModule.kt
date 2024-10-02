package com.bletestapp

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import java.util.UUID

class BleModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    private var bluetoothGattServer: BluetoothGattServer? = null
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var connectedDevice: BluetoothDevice? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null

    override fun getName(): String {
        return "BleModule"
    }

    // Method to start BLE advertising
    @ReactMethod
    fun startPeripheralMode(promise: Promise) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT
            )

            if (permissions.all {
                    ContextCompat.checkSelfPermission(reactApplicationContext, it) == PackageManager.PERMISSION_GRANTED
                }) {
                startAdvertising()
                promise.resolve("BLE Peripheral Mode started")
            } else {
                promise.reject("Permissions not granted", "Bluetooth permissions required")
            }
        } else {
            startAdvertising()
            promise.resolve("BLE Peripheral Mode started")
        }
    }

    // Start BLE advertising
    public fun startAdvertising() {
        val bluetoothManager = reactApplicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
        bluetoothAdapter.name = "Wizepass Test"

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .build()

        bluetoothLeAdvertiser?.startAdvertising(settings, data, advertiseCallback)

        setupGattServer(bluetoothManager)
    }

    // Set up GATT server with characteristics for write and notify
    private fun setupGattServer(bluetoothManager: BluetoothManager) {
        bluetoothGattServer = bluetoothManager.openGattServer(reactApplicationContext, gattServerCallback)

        val service = BluetoothGattService(
            UUID.fromString("0000aaa0-0000-1000-8000-aabbccddeeff"),  // Custom service UUID
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        // Create a Write Characteristic for receiving the URL
        val writeCharacteristic = BluetoothGattCharacteristic(
            UUID.fromString("0000aaa2-0000-1000-8000-aabbccddeeff"),  // Write Characteristic UUID
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        // Create a Notify Characteristic to send responses back to the central device
        notifyCharacteristic = BluetoothGattCharacteristic(
            UUID.fromString("0000aaa1-0000-1000-8000-aabbccddeeff"),  // Notify Characteristic UUID
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        service.addCharacteristic(writeCharacteristic)
        service.addCharacteristic(notifyCharacteristic)

        bluetoothGattServer?.addService(service)
    }

    // Callback for BLE advertising
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            sendEvent("onAdvertisingStart", null)
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            sendEvent("onAdvertisingFailure", errorCode)
        }
    }

    // GATT Server Callback handling write and notify characteristics
    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectedDevice = device
                sendEvent("onDeviceConnected", device?.address)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectedDevice = null
                sendEvent("onDeviceDisconnected", device?.address)
            }
        }

        // Handle write requests from central device (when sending URL)
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?, requestId: Int, characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?
        ) {
            if (characteristic?.uuid == UUID.fromString("0000aaa2-0000-1000-8000-aabbccddeeff")) {
                val url = value?.toString(Charsets.UTF_8)  // Convert bytes to string (URL)

                // Open the URL in the phone's browser
                if (url != null) {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    reactApplicationContext.startActivity(browserIntent)

                    // Send success notification back to the central device
                    sendNotification("URL Received and opened!")
                }

                if (responseNeeded) {
                    bluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                }
            }
        }
    }

    // Send notification to central device
    private fun sendNotification(message: String) {
        notifyCharacteristic?.value = message.toByteArray(Charsets.UTF_8)
        connectedDevice?.let {
            bluetoothGattServer?.notifyCharacteristicChanged(it, notifyCharacteristic, false)
        }
    }

    // Utility function to send events from native to React Native
    private fun sendEvent(eventName: String, params: Any?) {
        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }
}
