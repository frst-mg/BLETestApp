package com.bletestapp

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

/**
 * BlePackage is used to register the BleModule with React Native.
 * It implements the ReactPackage interface to expose native modules
 * and custom view managers (if any) to the JavaScript side of the app.
 */
class BlePackage : ReactPackage {

    /**
     * This method is responsible for creating and returning the list of native modules
     * that the package should expose to the JavaScript side.
     *
     * @param reactContext - The ReactApplicationContext, which is a context that gives
     * access to the application's resources and modules in the React Native environment.
     *
     * @return List of NativeModules - In this case, it contains only the BleModule,
     * which implements the BLE functionality.
     */
    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
        // Register the BleModule so it can be accessed from the JavaScript side using NativeModules.BleModule
        return listOf(BleModule(reactContext))
    }

    /**
     * This method is responsible for creating and returning the list of custom view managers.
     * View managers are used to expose custom native UI components (like buttons or layouts)
     * to React Native. Since we're not creating any custom UI components in this project,
     * this method returns an empty list.
     *
     * @param reactContext - The ReactApplicationContext, providing access to the application context.
     *
     * @return List of ViewManagers - Since no view managers are needed for BLE functionality, return an empty list.
     */
    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        // No custom UI components are being created, so we return an empty list.
        return emptyList()
    }
}
