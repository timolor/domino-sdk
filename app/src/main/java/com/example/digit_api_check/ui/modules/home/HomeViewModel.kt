package com.example.digit_api_check.ui.modules.home

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.os.Build
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.btracker.DGTracker
import com.example.btracker.DGTracker.Companion.mHandler
import com.example.btracker.DGTracker.Companion.mLeScanCallback
import com.example.btracker.DGTracker.Companion.mNPhoneCallback
import com.example.btracker.bluetooth.DGTBlePeripheral
import com.example.btracker.bluetooth.DGTCallback
import java.lang.RuntimeException
import java.util.*
import kotlin.collections.HashMap

class HomeViewModel : ViewModel(), DGTCallback {
    private val _devices = MutableLiveData<HashMap<String, DGTBlePeripheral>>()
    val devices: LiveData<HashMap<String, DGTBlePeripheral>> get() = _devices

    private var mScanning = false

    fun scan(homeActivity: HomeActivity) {
        mScanning = true
        DGTracker.getInstance().scan(homeActivity, this)
    }

    fun stop(mBluetoothAdapter: BluetoothAdapter): Boolean {
            mHandler?.removeMessages(DGTracker.MSG_SCAN_TIMEOUT)
            try {
                if (mScanning) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback)
                    } else {
                        val scaner: BluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner()
                        scaner.stopScan(mNPhoneCallback)
                    }
                    mScanning = false
                    Log.e(TAG, "now stop scan success fully")
                    return true
                }
            } catch (excp: RuntimeException) {
                Log.e(TAG, "start scan error" + excp.cause)
            }
//            invalidateOptionsMenu()
        return false
    }

    fun getScannedDevices(): HashMap<String, DGTBlePeripheral>? {
       return DGTracker.getBleDevices()
    }
    private fun loadUsers() {
        // Do an asynchronous operation to fetch users.
    }


    override fun onDeviceListUpdated(bleDevices: HashMap<String, DGTBlePeripheral>?) {
        Log.e(TAG, "called onDeviceListUpdated")
        _devices.value = bleDevices
    }

    companion object {
        private const val TAG = "DGT:HomeViewModel"
    }
}