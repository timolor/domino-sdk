package com.tagit.btracker.bluetooth

interface DGTCallback {
   fun onDeviceListUpdated(bleDevices: HashMap<String, DGTBlePeripheral>?)
}