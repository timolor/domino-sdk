package com.tagit.btracker.bluetooth

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.os.Message
import android.util.Log
import java.lang.ref.WeakReference
import java.util.*

class BluetoothLeClass constructor(app: Application) {

    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var nTrackNameIndex = 0

    var mLeDevicesMap: HashMap<String, DGTBlePeripheral>? = null
    var mBleDeviceMacArray = ArrayList<String>(50)

    private var mContext: Application = app

    private var mPrefs: SharedPreferenceManager? = null

    // In the outer class, instantiate a WeakReference to the outer class.
    private val outerClass = WeakReference<BluetoothLeClass>(this)

    // Pass the WeakReference object to the outer class to your Handler
    // when you instantiate the Handler
    private val mHandler = MyHandler(outerClass)


    // Declare the Handler as a static class.
    class MyHandler(private val outerClass: WeakReference<BluetoothLeClass>) : Handler() {

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_TRACK_TURNOFF -> {
                    val strMacAddr = msg.obj as String
                    strMacAddr?.let { outerClass.get()?.handleTurnOff(it) }
                }
                MSG_QUICK_RECONN -> {
                    val strMacAddr = msg.obj as String
                    strMacAddr?.let { outerClass.get()?.handleQuickReconn(it) }
                }
                MSG_CONN_SUCCESS -> {
                    // TODO: 21/08/2021 fix this
                    //mContext.sendUpdateListViewMsg()
                    val strMac: String = msg.obj as String

                    if (hasMessages(MSG_CONN_TIMEOUT, strMac)) {
                        removeMessages(MSG_CONN_TIMEOUT)
                    }
                }
                MSG_CONN_TIMEOUT -> {
                    val strMacAddr = msg.obj as String
                    strMacAddr?.let { outerClass.get()?.handleConnTimeout(it) }
                }
                else -> {
                }
            }
        }
    }

//    fun BluetoothLeClass(c: DeviceScanActivity?) {
//        mContext = c
//    }

    fun IsAllConnect(): Boolean {
        val it: Iterator<*> = mLeDevicesMap!!.keys.iterator()
        while (it.hasNext()) {
            val key = it.next() as String
            val blePerp: DGTBlePeripheral? = mLeDevicesMap!![key]
            if (blePerp?.mBleConnStatus != BluetoothProfile.STATE_CONNECTED) {
                return false
            }
        }
        return true
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    fun initialize(): Boolean {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = mContext?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.")
                return false
            }
        }
        mBluetoothAdapter = mBluetoothManager!!.adapter
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.")
            return false
        }
        mLeDevicesMap = HashMap<String, DGTBlePeripheral>()
        mPrefs = SharedPreferenceManager.getInstance(mContext)
        return true
    }

    fun init(device: BluetoothDevice, nRssi: Int): DGTBlePeripheral? {
        //check if is null
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "Conn failed, BluetoothAdapter not initialized or unspecified address.")
            return null
        }

        // Previously connected device.  Try to reconnect.
        val currTick = System.currentTimeMillis()
        var blePerp: DGTBlePeripheral? = mLeDevicesMap!![device.address]
        if (blePerp != null) {
            Log.d(TAG, "Found existing TIBLE instance for connection.")
            blePerp.mLastUpdateTick = currTick
        } else {
            //创建实例
            Log.d(TAG, "Trying to create an new DGTBle instance for connection.")
            blePerp = DGTBlePeripheral(mBluetoothAdapter, mContext, mHandler)
            blePerp.mBleDevName = device.name + nTrackNameIndex++.toString()
            blePerp.mMacAddress = device.address

            //update time
            blePerp.mFindDevTick = currTick
            blePerp.mLastUpdateTick = currTick
            mLeDevicesMap!![blePerp.mMacAddress.toString()] = blePerp
            mBleDeviceMacArray.add(blePerp.mMacAddress.toString())
        }
        blePerp.mRssiResult = nRssi
        return blePerp
    }

    fun getConnectedTrackNum(): Int {
        var nConnectedTrackNum = 0
        val it: Iterator<*> = mLeDevicesMap!!.keys.iterator()
        while (it.hasNext()) {
            val key = it.next() as String
            val blePerp: DGTBlePeripheral? = mLeDevicesMap!![key]
            if (blePerp?.mBleConnStatus == BluetoothProfile.STATE_CONNECTED) {
                nConnectedTrackNum++
            }
        }
        return nConnectedTrackNum
    }

    //处理防丢器关闭
    fun handleTurnOff(strMacAddr: String) {
        val blePerp: DGTBlePeripheral? = mLeDevicesMap!!.remove(strMacAddr)
        for (i in mBleDeviceMacArray.indices) {
            if (mBleDeviceMacArray[i] == strMacAddr) {
                mBleDeviceMacArray.removeAt(i)
                break
            }
        }

        // TODO: 21/08/2021 fix this
        //更新界面
        //mContext.sendUpdateListViewMsg()
    }

    fun startConnect(strMacAddr: String) {
        //check if is null
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "Conn failed, BluetoothAdapter not initialized or unspecified address.")
            return
        }

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        val blePerp: DGTBlePeripheral? = mLeDevicesMap!![strMacAddr]
        if (blePerp != null && blePerp.isDisconnected()) {
            blePerp.connect()
            // TODO: 21/08/2021 fix this
            //mContext.sendUpdateListViewMsg()
            Log.d(TAG, "Start connect to  $strMacAddr")

            //start conn timeout timer
            val msg = mHandler.obtainMessage(MSG_CONN_TIMEOUT)
            msg.obj = blePerp.mMacAddress
            mHandler.sendMessageDelayed(msg, DGTBlePeripheral.MAX_CONNECT_TIME)
        }
    }

    private fun handleConnTimeout(strMacAddr: String) {
        val blePerp: DGTBlePeripheral = mLeDevicesMap!![strMacAddr] ?: return
        Log.i(TAG, "Start close track:$strMacAddr")
        if (blePerp.mBleConnStatus != BluetoothProfile.STATE_CONNECTED) {
            blePerp.closeConnection()
            // TODO: 21/08/2021 fix this
//            mContext.sendUpdateListViewMsg()
//            mContext.toastShow("Connect timeout, please retry later")
        }
    }

    private fun handleQuickReconn(strMacAddr: String) {
        val blePerp: DGTBlePeripheral = mLeDevicesMap!![strMacAddr] ?: return
        if (blePerp.isConnected()) {
            return
        }
        Log.e(TAG, "conn to itrack timeout, now quick conn again")
        mHandler.removeMessages(MSG_CONN_TIMEOUT)
        blePerp.reConnect()
        // TODO: 21/08/2021 fix this
        //mContext.sendUpdateListViewMsg()
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * `BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)`
     * callback.
     */
    fun disconnect() {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        val it: Iterator<*> = mLeDevicesMap!!.keys.iterator()
        while (it.hasNext()) {
            val key = it.next() as String
            val blePerp: DGTBlePeripheral? = mLeDevicesMap!![key]
            blePerp?.mBleGatt?.disconnect()
        }
    }

    fun turnOffAllTrack(): Boolean {
        var bFoundDevice = false
        val iter: Iterator<*> = mLeDevicesMap!!.entries.iterator()
        while (iter.hasNext()) {
            val entry = iter.next() as Map.Entry<*, *>
            val blePerp: DGTBlePeripheral = entry.value as DGTBlePeripheral
            if (blePerp.isConnected()) {
                blePerp.mTurnOffStatus = DGTBlePeripheral.TURNOFF_DEV_START
                // TODO: 21/08/2021 fix this
                //blePerp.turnOffTrack()
                bFoundDevice = true
            }
        }
        return bFoundDevice
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    fun closeAllConnection() {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        for (key in mLeDevicesMap!!.keys) {
            val blePerp: DGTBlePeripheral? = mLeDevicesMap!![key]
            blePerp?.closeConnection()
        }
        mLeDevicesMap!!.clear()
        mBleDeviceMacArray.clear()
    }

    fun getTrackByMac(strMacAddr: String): DGTBlePeripheral? {
        return mLeDevicesMap!![strMacAddr]
    }

    fun getTrackByMac(nIndex: Int): DGTBlePeripheral? {
        val strMacAddr = mBleDeviceMacArray[nIndex]
        return mLeDevicesMap!![strMacAddr]
    }

    fun getPositionByMac(strMmacAddr: String): Int {
        for (i in mBleDeviceMacArray.indices) {
            if (mBleDeviceMacArray[i] == strMmacAddr) {
                return i
            }
        }
        return -1
    }

    fun getTracksCount(): Int {
        return mBleDeviceMacArray.size
    }

    fun updateBleList(blePerp: DGTBlePeripheral) {
        mLeDevicesMap!![blePerp.mMacAddress.toString()] = blePerp
        mBleDeviceMacArray.add(blePerp.mMacAddress.toString())
    }

    fun getBleList(): HashMap<String, DGTBlePeripheral>? {
        return mLeDevicesMap
    }

    companion object {
        const val MAX_CONCURRENT_CONN_NUM = 5

        const val MSG_TRACK_TURNOFF = 2002
        const val MSG_QUICK_RECONN = 2003
        const val MSG_CONN_TIMEOUT = 2004
        const val MSG_CONN_SUCCESS = 2005


        private const val TAG = "DGT:BluetoothLeClass" //BluetoothLeClass.class.getSimpleName();

    }
}