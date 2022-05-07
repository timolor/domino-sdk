package com.tagit.btracker.bluetooth

import android.annotation.TargetApi
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.util.Log
import com.tagit.btracker.DGTracker
import com.tagit.btracker.DGTracker.Companion.sendUpdateListViewMsg

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class NPhoneScanCallback : ScanCallback() {

    private var mStartFailedNum = 0
    private var mPrefMgr: SharedPreferenceManager = DGTracker.mPrefMgr

    /**读写BLE终端 */
    private var mBLESrvMgr: BluetoothLeClass = DGTracker.mBLESrvMgr

    private var mLastUpdateViewTick: Long = 0


    override fun onScanResult(callbackType: Int, result: ScanResult) {
        if (callbackType == ScanSettings.CALLBACK_TYPE_MATCH_LOST) {
            return
        }
        mStartFailedNum = 0
        val device = result.device
        val rslt = result.scanRecord
        if (device != null && rslt != null) {
            handleBleScanRslt(device, result.rssi)
        }
    }

    override fun onBatchScanResults(results: List<ScanResult>) {
        mStartFailedNum = 0
        if (results.isNotEmpty()) {
            for (rslt in results) {
                onScanResult(10, rslt)
            }
        } else {
            Log.e(
                TAG,
                "Start N scan found 0 result"
            )
        }
    }

    override fun onScanFailed(errorCode: Int) {
        Log.e(
            TAG,
            "Start N scan failed：$errorCode"
        )
        mStartFailedNum++
        if (mStartFailedNum >= 2) {
            DGTracker.mHandler!!.sendEmptyMessageDelayed(
                MSG_ON_SCAN_FAILED,
                100
            )
        }
    }

    fun handleBleScanRslt(device: BluetoothDevice, rssi: Int) {
        val trackName = device.name ?: return
        val strTrackName: String? = mPrefMgr.getTrackname()
        if (strTrackName?.let { trackName.compareTo(it, ignoreCase = true) } != 0) {
            return
        }
        Log.e(TAG, "Scan success, found new Track " + device.address + device.name + rssi)

        if (rssi < mPrefMgr.getRssiValue()) {
            return
        }
        val strMacAddress = device.address
        val blePerp: DGTBlePeripheral? = mBLESrvMgr.getTrackByMac(strMacAddress)
        if (blePerp == null) {
            mBLESrvMgr.init(device, rssi)
            sendUpdateListViewMsg()
            //mBLESrvMgr.getBleList()?.let { sendUpdatedDeviceList(it) }
            DGTracker.getInstance().setResult(mBLESrvMgr.getBleList())
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

            //refresh track list
            mBLESrvMgr.updateBleList(blePerp)

            //refresh view
            sendUpdateListViewMsg()

            //mBLESrvMgr.getBleList()?.let { sendUpdatedDeviceList(it) }
            DGTracker.getInstance().setResult(mBLESrvMgr.getBleList())
        }
    }

    private fun getPositionByMac(strMmacAddr: String?): Int {
        Log.d(TAG, "getPositionByMac: $strMmacAddr")
        return mBLESrvMgr.getPositionByMac(strMmacAddr!!)
    }

    companion object {
        private const val TAG = "DGT:NPhoneScanCallNBack"
        private const val MSG_ON_SCAN_FAILED = 206
    }
}