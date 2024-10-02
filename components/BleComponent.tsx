import React, {useEffect} from 'react';
import {NativeModules, Button, Alert} from 'react-native';
import {NativeEventEmitter} from 'react-native';

const {BleModule} = NativeModules; // Import the native module

const BleComponent = () => {
  useEffect(() => {
    const bleEventEmitter = new NativeEventEmitter(BleModule); // Listen to native events

    // Listener for BLE Advertising Start
    const startListener = bleEventEmitter.addListener(
      'onAdvertisingStart',
      () => {
        Alert.alert('BLE Advertising Started');
      },
    );

    // Listener for BLE Advertising Failure
    const failureListener = bleEventEmitter.addListener(
      'onAdvertisingFailure',
      errorCode => {
        Alert.alert(`BLE Advertising Failed with error code: ${errorCode}`);
      },
    );

    // Listener for Data Received from GATT Server
    const dataListener = bleEventEmitter.addListener('onDataReceived', data => {
      Alert.alert(`Data Received: ${data}`);
    });

    // Clean up listeners on component unmount
    return () => {
      startListener.remove();
      failureListener.remove();
      dataListener.remove();
    };
  }, []);

  // Function to start BLE Peripheral Mode
  const startPeripheralMode = async () => {
    try {
      await BleModule.startPeripheralMode();
      Alert.alert('Peripheral Mode started');
    } catch (error) {
      Alert.alert('Failed to start Peripheral Mode', (error as Error).message);
    }
  };

  return (
    <>
      <Button title="Start BLE Peripheral Mode" onPress={startPeripheralMode} />
    </>
  );
};

export default BleComponent;
