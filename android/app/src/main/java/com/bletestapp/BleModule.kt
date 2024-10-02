package com.bletestapp

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import java.util.UUID
import android.os.ParcelUuid

// The BleModule is a React Native Native Module, written in Kotlin, that enables Bluetooth LE (Low Energy) functionality.
class BleModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    // GATT server that manages connections and handles BLE operations.
    private var bluetoothGattServer: BluetoothGattServer? = null

    // The BLE advertiser used to broadcast BLE data to nearby devices.
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null

    // This defines the name of the module that will be accessible from React Native.
    override fun getName(): String {
        return "BleModule"
    }

    // Method called from the JavaScript side to start BLE Peripheral mode.
    @ReactMethod
    fun startPeripheralMode(promise: Promise) {
        // Check if the device is running Android 12 (S) or higher.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Required permissions for BLE operations.
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT
            )

            // Ensure all permissions are granted before starting BLE operations.
            if (permissions.all {
                    ContextCompat.checkSelfPermission(reactApplicationContext, it) == PackageManager.PERMISSION_GRANTED
                }) {
                // Permissions are granted, so start advertising.
                startAdvertising()
                promise.resolve("BLE Peripheral Mode started")
            } else {
                // If permissions are not granted, return an error through the promise.
                promise.reject("Permissions not granted", "Required Bluetooth permissions are not granted")
            }
        } else {
            // For devices running below Android 12 (S), start BLE advertising directly without requesting these permissions.
            startAdvertising()
            promise.resolve("BLE Peripheral Mode started")
        }
    }

    // Function that sets up and starts BLE advertising.
    fun startAdvertising() {
        // Get the BluetoothManager to access Bluetooth services.
        val bluetoothManager = reactApplicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser // Get the BLE advertiser.


// Enable Bluetooth if it is not already enabled.
    if (!bluetoothAdapter.isEnabled) {
        bluetoothAdapter.enable()
    }
        // Set the Bluetooth adapter's device name (the name visible when other devices scan for this peripheral).
        bluetoothAdapter.name = "Wizepass Phone"

        // Configure advertising settings: low latency, high transmission power, and connectable.
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)  // Low latency mode for faster advertisements.
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)      // High transmission power for better range.
            .setConnectable(true)  // Allow connections from central devices (e.g., smartphones).
            .build()

        // Configure the data to be broadcast, in this case, including the device name.
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)  // Broadcast the device name in advertisements.
            .addServiceUuid(ParcelUuid(UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb")))  // Add the Battery Service UUID.
            .build()

        // Start advertising with the specified settings and data, and register the callback to handle results.
        bluetoothLeAdvertiser?.startAdvertising(settings, data, advertiseCallback)

        // Set up the GATT server to handle connections from central devices (e.g., smartphones).
        setupGattServer(bluetoothManager)
    }

    // Function that sets up the GATT server, allowing central devices to connect and interact with services and characteristics.
    private fun setupGattServer(bluetoothManager: BluetoothManager) {
        // Open a GATT server to allow BLE connections.
        bluetoothGattServer = bluetoothManager.openGattServer(reactApplicationContext, gattServerCallback)

        // Create a new GATT service (for example, a Battery Service using standard UUID).
        val service = BluetoothGattService(
            UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb"),  // Battery Service UUID.
            BluetoothGattService.SERVICE_TYPE_PRIMARY  // Primary service type (as opposed to secondary).
        )

        // Create a new GATT characteristic (for example, Battery Level using standard UUID), supporting read and write operations.
        val characteristic = BluetoothGattCharacteristic(
            UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb"),  // Battery Level Characteristic UUID.
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE,  // Supports read/write.
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE  // Permissions.
        )

        // Add the characteristic to the service.
        service.addCharacteristic(characteristic)

        // Add the service to the GATT server.
        bluetoothGattServer?.addService(service)
    }

    // This callback handles the result of starting the BLE advertising.
    private val advertiseCallback = object : AdvertiseCallback() {
        // Called when BLE advertising starts successfully.
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            // Notify React Native that advertising has started.
            sendEvent("onAdvertisingStart", null)
        }

        // Called when BLE advertising fails to start.
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            // Notify React Native that advertising failed with an error code.
            sendEvent("onAdvertisingFailure", errorCode)
        }
    }

    // This callback handles events related to the GATT server, like incoming write requests from central devices.
    private val gattServerCallback = object : BluetoothGattServerCallback() {
        // Handle write requests from central devices (e.g., when they send data to your device).
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?, requestId: Int, characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?
        ) {
            // Convert the incoming byte array to a string (assuming UTF-8 text).
            val receivedData = value?.toString(Charsets.UTF_8)

            // Send the received data to the React Native side as an event.
            sendEvent("onDataReceived", receivedData)

            // If the central device needs a response, send a success response.
            if (responseNeeded) {
                bluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
            }
        }
    }

    // Utility function to send events from the native side (Kotlin) to the React Native side (JavaScript).
    private fun sendEvent(eventName: String, params: Any?) {
        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)  // Emits the event with its associated parameters to the JS side.
    }
}
