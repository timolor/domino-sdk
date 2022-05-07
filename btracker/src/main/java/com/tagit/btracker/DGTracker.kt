package com.tagit.btracker

import android.Manifest
import android.app.Activity
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.util.Log
import androidx.core.app.ActivityCompat
import com.tagit.btracker.bluetooth.*
import java.util.HashMap

class DGTracker private constructor(val application: Application) {

    private lateinit var instance: Activity
    private var callback: DGTCallback? = null

    fun scan(activity: Activity, scanCallback: DGTCallback) {
        instance = activity
        callback = scanCallback
        //mLeDeviceListAdapter.clear();
        if (!mBluetoothAdapter.isEnabled) {
            Log.d(TAG, "scan: ${R.string.ble_function_disable}")
        } else {
            if (checkBluetoothPermitDialog()) {
                mBLESrvMgr.closeAllConnection()
                //mLeDeviceListAdapter.notifyDataSetChanged()
                mHandler!!.sendEmptyMessageDelayed(
                    MSG_START_SCAN,
                    10
                )
            }
        }
    }

    internal fun setResult(bleDevices: HashMap<String, DGTBlePeripheral>?){
        resetState(bleDevices)
    }

    private fun resetState(bleDevices: HashMap<String, DGTBlePeripheral>?){

        callback?.apply {
            onDeviceListUpdated(bleDevices)
        }

        //reset callback
        //callback = null
    }

    private fun checkBluetoothPermitDialog(): Boolean {
        return if (!Utils.isLocationBluePermission(instance)) {
            ActivityCompat.requestPermissions(
                instance, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                23
            )

            //判断是否需要 向用户解释，为什么要申请该权限
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    instance,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ) {
                Log.d(TAG, R.string.location_permit_needed_for_ble.toString())
//                toastShow(getString(R.string.location_permit_needed_for_ble))
            }
            false
        } else {
            Log.d(TAG, "checkBluetoothPermitDialog returns true")
            true
        }
    }

    companion object {
        private const val TAG = "DGT_SDK"
        private var isSetup = false
        private lateinit var INSTANCE: DGTracker

        /**搜索BLE终端 */
        private lateinit var mBluetoothAdapter: BluetoothAdapter

        /**读写BLE终端 */
        lateinit var mBLESrvMgr: BluetoothLeClass

        private const val mScanning = false
        var mHandler: Handler? = null
        lateinit var mPrefMgr: SharedPreferenceManager
        lateinit var mLeScanCallback: LeScanCallback
        var mNPhoneCallback: NPhoneScanCallback? = null
        val callback: DGTCallback? = null
//        private lateinit var mLeDeviceListAdapter: LeDeviceListAdapter


        const val MSG_START_SCAN = 201
        const val MSG_SCAN_TIMEOUT = 202
        const val MSG_CHK_TIMER_MY = 203
        const val MSG_ON_SCAN_FAILED = 206
        const val MSG_UPDATE_VIEW = 207
        const val CHK_TIMER_PERIOD = (4 * 1010).toLong()

        @JvmStatic
        fun initialize(app: Application) {
            Log.e(TAG, "Initializing the SDK")
            if (!isSetup) {

                synchronized(this) {
                    INSTANCE = DGTracker(app)
                }

                // Use this check to determine whether BLE is supported on the device.  Then you can
                // selectively disable BLE-related features.
                if (!app.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {

                    Log.e(TAG, R.string.ble_not_supported.toString())
//                    finish()
                    return
                }

                // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
                // BluetoothAdapter through BluetoothManager.
                val bluetoothManager =
                    app.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                mBluetoothAdapter = bluetoothManager.adapter

                // Check if Bluetooth is supported on the device.
                if (!this::mBluetoothAdapter.isInitialized) {
                    Log.e(TAG, R.string.error_bluetooth_not_supported.toString())
//                  finish()
                    return
                }

                //开启蓝牙
                //开启蓝牙
                mBluetoothAdapter.enable()

                mHandler = MsgHandler(mBluetoothAdapter)

                mBLESrvMgr = BluetoothLeClass(app)
                if (!mBLESrvMgr.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth")
                    //finish()
                    return
                }

                mPrefMgr = SharedPreferenceManager.getInstance(app)

//                mLeDeviceListAdapter = LeDeviceListAdapter(this)
//                setListAdapter(mLeDeviceListAdapter)

                mHandler?.sendEmptyMessageDelayed(
                    MSG_CHK_TIMER_MY,
                    CHK_TIMER_PERIOD
                )
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    mLeScanCallback =
                        OldLeScanCallback()
                } else {
                    mNPhoneCallback =
                        NPhoneScanCallback()
                }

                isSetup = true
            }
        }

        fun sendUpdateListViewMsg() {

        }
        
        fun sendUpdatedDeviceList(bleDevices: HashMap<String, DGTBlePeripheral>?){
            Log.d(TAG, "sendUpdatedDeviceList: $bleDevices")

            callback?.onDeviceListUpdated(bleDevices)
        }

        fun getBleDevices(): HashMap<String, DGTBlePeripheral>? {
            return mBLESrvMgr.getBleList()
        }

        @JvmStatic
        fun getInstance(): DGTracker = INSTANCE

        @JvmStatic
        fun getBluetoothAdapter(): BluetoothAdapter = mBluetoothAdapter
    }
}