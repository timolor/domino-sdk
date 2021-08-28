package com.example.btracker.bluetooth

interface DGTCallback {
   fun onDeviceListUpdated(bleDevices: HashMap<String, DGTBlePeripheral>?)
}