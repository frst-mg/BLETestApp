import {StyleSheet, Text, View} from 'react-native';
import BleComponent from './components/BleComponent';
import React from 'react';

const App = () => {
  return (
    <View style={styles.container}>
      <Text>BLE Test App</Text>
      <BleComponent />
    </View>
  );
};

export default App;

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
