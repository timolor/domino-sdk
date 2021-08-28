package com.example.btracker.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.os.Handler
import android.os.Message
import android.util.Log
import com.example.btracker.DGTracker.Companion.CHK_TIMER_PERIOD
import com.example.btracker.DGTracker.Companion.MSG_CHK_TIMER_MY
import com.example.btracker.DGTracker.Companion.MSG_ON_SCAN_FAILED
import com.example.btracker.DGTracker.Companion.MSG_SCAN_TIMEOUT
import com.example.btracker.DGTracker.Companion.MSG_START_SCAN
import com.example.btracker.DGTracker.Companion.MSG_UPDATE_VIEW
import com.example.btracker.DGTracker.Companion.mHandler
import com.example.btracker.DGTracker.Companion.mLeScanCallback
import com.example.btracker.DGTracker.Companion.mNPhoneCallback
import java.util.*

class MsgHandler(private val mBluetoothAdapter: BluetoothAdapter) : Handler() {
    var mScanning = false

    override fun handleMessage(msg: Message) {
        when (msg.what) {
            MSG_START_SCAN -> {
                mHandler?.removeMessages(MSG_START_SCAN)
                handleStartScan(mBluetoothAdapter)
            }
            MSG_SCAN_TIMEOUT -> {
                handleStopScan(mBluetoothAdapter)
            }
            MSG_CHK_TIMER_MY -> {
                handlePeriodChk()
                mHandler?.sendEmptyMessageDelayed(
                    MSG_CHK_TIMER_MY,
                    CHK_TIMER_PERIOD
                )
            }
            MSG_UPDATE_VIEW -> {
                mHandler?.removeMessages(MSG_UPDATE_VIEW)
//                updateListView()
            }
            MSG_ON_SCAN_FAILED -> {
                mHandler?.removeMessages(MSG_ON_SCAN_FAILED)
                Log.d(TAG, "Start scan faileed, please reboot bluetooth")
                //toastShow("Start scan faileed, please reboot bluetooth")
                stopScan(mBluetoothAdapter)
            }
            else -> {
            }
        }
    }

    private fun handleStartScan(mBluetoothAdapter: BluetoothAdapter) {
        try {
            if (mScanning) {
                Log.e(
                    TAG,
                    "current is scan, now start scan again"
                )
                stopScan(mBluetoothAdapter)
            }

            //create filter
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback)
                val trackUUID = arrayOf(Utils.IMMEDIATE_ALERT_SERVICE_UUID)
                mBluetoothAdapter.startLeScan(trackUUID, mLeScanCallback)
            } else {
                val filters: MutableList<ScanFilter> = ArrayList(2)
                val builder = ScanFilter.Builder()
                builder.setServiceUuid(Utils.PARCE_UUID_IMMEDIATE_ALERT_SERVICE)
                filters.add(builder.build())

                //start scan
                val setsBuild: ScanSettings.Builder
                setsBuild = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                val scaner: BluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner()
                scaner.startScan(filters, setsBuild.build(), mNPhoneCallback)
            }
            mScanning = true
            Log.e(
                TAG,
                "now start scan success fully"
            )
            //invalidateOptionsMenu()
        } catch (excp: RuntimeException) {
            Log.e(
                TAG,
                "start scan error" + excp.cause
            )
        }
    }

    fun stopScan(mBluetoothAdapter: BluetoothAdapter) {
        mHandler!!.removeMessages(MSG_SCAN_TIMEOUT)
        try {
            if (mScanning) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback)
                } else {
                    val scaner: BluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner()
                    scaner.stopScan(mNPhoneCallback)
                }
                mScanning = false
                Log.e(
                    TAG,
                    "now stop scan success fully"
                )
            }
        } catch (excp: RuntimeException) {
            Log.e(
                TAG,
                "start scan error" + excp.cause
            )
        }
//        invalidateOptionsMenu()
    }

    fun handleStopScan(mBluetoothAdapter: BluetoothAdapter) {
        stopScan(mBluetoothAdapter)
    }

    private fun handlePeriodChk() {}

    companion object {
        private const val TAG = "DGT"
    }
}