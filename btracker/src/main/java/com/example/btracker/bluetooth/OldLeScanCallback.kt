package com.example.btracker.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.util.Log
import com.example.btracker.DGTracker

class OldLeScanCallback : BluetoothAdapter.LeScanCallback {

    private lateinit var mPrefMgr: SharedPreferenceManager

    /**读写BLE终端 */
    private lateinit var mBLESrvMgr: BluetoothLeClass

    private var mLastUpdateViewTick: Long = 0

    override fun onLeScan(device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray?) {
        handleBleScanRslt(device!!, rssi)
    }

    fun handleBleScanRslt(device: BluetoothDevice, rssi: Int) {
        val trackName = device.name ?: return
        val strTrackName: String? = mPrefMgr.getTrackname()
        if (strTrackName?.let { trackName.compareTo(it, ignoreCase = true) } != 0) {
            return
        }
        Log.e(
            TAG,
            "Scan success, found new Track " + device.address + device.name + rssi
        )
        if (rssi < mPrefMgr.getRssiValue()) {
            return
        }
        val strMacAddress = device.address
        val blePerp: DGTBlePeripheral? = mBLESrvMgr.getTrackByMac(strMacAddress)
        if (blePerp == null) {
            mBLESrvMgr.init(device, rssi)
            DGTracker.sendUpdateListViewMsg()
        } else {
            val currTick = System.currentTimeMillis()
            blePerp.updateRSSI(rssi, currTick)

            //chk need show
            val position: Int = getPositionByMac(blePerp.mMacAddress)
            if (position == -1) {
                return
            }
            if (currTick - mLastUpdateViewTick < 300) {
                return
            }
            mLastUpdateViewTick = currTick

            //refresh view
            DGTracker.sendUpdateListViewMsg()
        }
    }

    private fun getPositionByMac(strMmacAddr: String?): Int {
        return mBLESrvMgr.getPositionByMac(strMmacAddr!!)
    }

    companion object {
        private const val TAG = "DGT:OldLeScanCallNBack"
        private const val MSG_ON_SCAN_FAILED = 206
    }

}