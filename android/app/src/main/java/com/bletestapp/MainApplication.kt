package com.bletestapp

import android.app.Application
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactPackage
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.load
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import com.facebook.react.defaults.DefaultReactNativeHost
import com.facebook.soloader.SoLoader

/**
 * MainApplication is the entry point for the Android app.
 * This class extends Application and is responsible for initializing
 * React Native, loading native packages, and managing app-level configurations.
 */
class MainApplication : Application(), ReactApplication {

    /**
     * The reactNativeHost property defines the host that React Native uses to initialize
     * the JavaScript runtime environment and bridge it to the native Android environment.
     */
    override val reactNativeHost: ReactNativeHost =
        object : DefaultReactNativeHost(this) {

            /**
             * Override this method to return a list of all React Native packages.
             * This includes both the default packages and any custom ones you manually register.
             *
             * Here, we add `BlePackage()` to include the Bluetooth module.
             */
            override fun getPackages(): List<ReactPackage> =
                PackageList(this).packages.apply {
                    // Add custom packages that cannot be autolinked yet
                    add(BlePackage()) // Register the BLE package
                }

            /**
             * This method returns the name of the JavaScript entry point for your app.
             * By default, it is usually "index" (as in index.js) where the main React Native code is located.
             */
            override fun getJSMainModuleName(): String = "index"

            /**
             * Returns whether the developer support is enabled.
             * In debug mode (when BuildConfig.DEBUG is true), this is set to true, enabling
             * features like the React Native developer menu and live reloading.
             */
            override fun getUseDeveloperSupport(): Boolean = BuildConfig.DEBUG

            /**
             * This method controls whether the new architecture (like Fabric) is enabled.
             * The value is set from BuildConfig to easily switch between architectures.
             */
            override val isNewArchEnabled: Boolean = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED

            /**
             * This flag determines whether the Hermes JavaScript engine is enabled.
             * Hermes is an optimized JavaScript engine for React Native.
             */
            override val isHermesEnabled: Boolean = BuildConfig.IS_HERMES_ENABLED
        }

    /**
     * The reactHost property provides access to the ReactHost for managing the React Native instance.
     * This uses the default configuration for handling the bridge between the Android and JavaScript sides.
     */
    override val reactHost: ReactHost
        get() = getDefaultReactHost(applicationContext, reactNativeHost)

    /**
     * The onCreate() method is the entry point when the Android application is created.
     * It initializes SoLoader, which is responsible for loading native libraries in React Native.
     * Additionally, if the new architecture is enabled, it loads the native entry point for the new architecture.
     */
    override fun onCreate() {
        super.onCreate()
        // Initialize the SoLoader library for loading native C/C++ libraries
        SoLoader.init(this, false)

        // Load the new architecture entry point if the app is configured to use the new architecture
        if (BuildConfig.IS_NEW_ARCHITECTURE_ENABLED) {
            load()
        }
    }
}
