package com.bletestapp

import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate

/**
 * MainActivity is the main entry point for the React Native application on Android.
 * It extends ReactActivity, which is a base activity class provided by React Native.
 * It is responsible for loading the React Native app and handling necessary permissions.
 */
class MainActivity : ReactActivity() {

    companion object {
        /**
         * Constant value for the Bluetooth permission request code.
         * This is used to identify the permission request callback in the app.
         */
        const val REQUEST_CODE_BLUETOOTH = 1001
    }

    /**
     * Specifies the name of the main component registered from JavaScript. This name is used
     * by React Native to schedule rendering of the component. In this case, it should correspond
     * to the name of your main JavaScript entry file's component.
     *
     * @return The name of the main component (JavaScript side entry point)
     */
    override fun getMainComponentName(): String = "BLETestApp"

    /**
     * Creates the ReactActivityDelegate responsible for managing the lifecycle of the React Native app.
     * The DefaultReactActivityDelegate is a default implementation provided by React Native,
     * and it takes care of enabling or disabling the new architecture features like Fabric.
     *
     * @return An instance of ReactActivityDelegate, used by React Native.
     */
    override fun createReactActivityDelegate(): ReactActivityDelegate =
        DefaultReactActivityDelegate(this, mainComponentName, fabricEnabled)

    /**
     * This method is called when the user responds to a permission request, such as Bluetooth permissions.
     * It checks if the requested permissions have been granted, and if they have, it starts the BLE operations.
     *
     * @param requestCode The request code passed in when the permissions were requested.
     * @param permissions The array of permissions that were requested.
     * @param grantResults The results for the requested permissions, indicating whether they were granted or denied.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Check if the result is for the Bluetooth permission request
        if (requestCode == REQUEST_CODE_BLUETOOTH) {
            // Check if all permissions were granted
            val allPermissionsGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (allPermissionsGranted) {
                // If all Bluetooth-related permissions were granted, retrieve the BleModule
                val bleModule = (application as MainApplication).reactNativeHost.reactInstanceManager.currentReactContext
                    ?.getNativeModule(BleModule::class.java)

                // Start the BLE advertising operation now that permissions are granted
                bleModule?.startAdvertising()
            } else {
                // Handle permission denial, such as showing a message or disabling BLE features
            }
        }
    }
}
