package com.example.btracker.bluetooth

import android.app.Application
import android.bluetooth.*
import android.os.Handler
import android.util.Log
import com.example.btracker.DGTracker
import com.example.btracker.R
import java.util.*

class DGTBlePeripheral(adapter: BluetoothAdapter?, context: Application?, parentHandler: Handler?) {
    var mBleGatt: BluetoothGatt? = null
    var mBleDevName: String? = null
    var mMacAddress //mac address
            : String? = null
    var mBleConnStatus //BLE connection state
            = 0
    var isAlerting //is app calling to iTrack
            = false
    var mKeyNotify = 0 //key press notify:  0: not notify;  1: press notify;   2: long press notify

    var mRssiResult = 0
    var mTurnOffStatus = TURNOFF_DEV_INVALID
    var mStartConnNum = 0

    var mFindDevTick: Long = 0 //添加的时间

    var mLastUpdateTick: Long = 0 //最后一次更新时间


    private var mParentHandler: Handler? = parentHandler

    private var mBluetoothAdapter: BluetoothAdapter? = adapter

    private var mContext: Application? = context
    var mPref: SharedPreferenceManager? = null

    private val DEFAULT_PASSWORD = byteArrayOf(
        0xA1.toByte(),
        0xA2.toByte(), 0xA3.toByte(), 0xA4.toByte()
    )

    private var mStartConnectTick: Long = 0


    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    @ExperimentalUnsignedTypes
    private val mGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val nOldConnState = mBleConnStatus
            val tmpBlePerp = gatt.device
            if (mMacAddress != tmpBlePerp.address) {
                return
            }
            if (mBleGatt != gatt) {
                Log.e(TAG, "update gatt in BluetoothGattCallback")
                mBleGatt = gatt //更新
            }
            Log.i(
                TAG, String.format(
                    "Mac:%s onConnectionStateChange for connection,old state:%d new state:%d",
                    mMacAddress,
                    mBleConnStatus, newState
                )
            )

            //check if result is success
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "onConnectionStateChange failed:$mMacAddress")
                handleConnDiscon()
            } else {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    // Attempts to discover services after successful connection.
                    Log.i(TAG, "$mMacAddress: connect success, now start discover.")

                    //发现服务
                    if (!gatt.discoverServices()) {
                        Log.d(TAG, "start service discovery failed")
                    }
                } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                    Log.i(TAG, "Connecting to GATT server.")
                } else {
                    Log.i(TAG, "Disconnected from GATT server.$mMacAddress")

                    //reconnect
                    handleConnDiscon()
                }
            }
        }

        //处理断开连接
        fun handleConnDiscon() {
            val nLastTime = System.currentTimeMillis() - mStartConnectTick
            if (mBleConnStatus == BluetoothProfile.STATE_CONNECTING
                && nLastTime < MAX_CONNECT_TIME / 2
            ) {
                closeConnection()
                mStartConnNum++
                if (mStartConnNum < 2) {
                    mParentHandler!!.sendEmptyMessage(BluetoothLeClass.MSG_QUICK_RECONN)
                }
            } else {
                closeConnection()
                if (mTurnOffStatus == TURNOFF_DEV_SUCC) {
                    val msg = mParentHandler!!.obtainMessage(BluetoothLeClass.MSG_TRACK_TURNOFF)
                    msg.obj = mMacAddress
                    mParentHandler!!.sendMessage(msg)
                }
            }
        }

        fun handleConnSucc() {
            mBleConnStatus = BluetoothProfile.STATE_CONNECTED

            //notify conn success
            val msg = mParentHandler!!.obtainMessage(BluetoothLeClass.MSG_CONN_SUCCESS)
            msg.obj = mMacAddress
            mParentHandler!!.sendMessage(msg)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val tmpBlePerp = gatt.device
            if (mMacAddress != tmpBlePerp.address) {
                return
            }

            //检查是否更新
            if (mBleGatt != gatt) {
                //Log.i(TAG, "update gatt in onServicesDiscovered");
                mBleGatt = gatt //更新
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //wirte password to iTrack
                writePassword2Itrack(gatt)
            } else {
                handleConnDiscon()
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic, status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) Log.e(
                TAG, "onCharRead " + gatt.device.name
                        + " read "
                        + characteristic.uuid.toString()
                        + " -> "
//                        + characteristic.value.bytesToHexString()
                        + Utils.bytesToHexString(characteristic.value)
            )
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            Log.e(
                TAG, "onCharWrite " + gatt.device.name
                        + " write "
                        + characteristic.uuid.toString()
                        + " -> "
                        + String(characteristic.value)
            )

            //enable key notifycation
            val serviceUuid = characteristic.service.uuid
            val charUuid = characteristic.uuid
            if (serviceUuid == Utils.CUSTOM_SERVICE && charUuid == Utils.CHARACTERISTIC_CUSTOM_VERIFIED) {

                //通知连接建立成功
                handleConnSucc()
                enableKeyPressNotification(gatt)
            }
        }

        //write password
        private fun writePassword2Itrack(gatt: BluetoothGatt) {
            val service = gatt.getService(Utils.CUSTOM_SERVICE)
            if (service == null) {
                Log.e(TAG, ":writePassword2Itrack failed.")
                return
            }
            val characteristic = service.getCharacteristic(Utils.CHARACTERISTIC_CUSTOM_VERIFIED)
            if (characteristic == null) {
                Log.e(TAG, ":writePassword2Itrack failed.")
                return
            }
            gatt.setCharacteristicNotification(characteristic, true)
            //set password
            characteristic.value = DEFAULT_PASSWORD
            //write password
            if (gatt.writeCharacteristic(characteristic)) {
                Log.e(TAG, "start write password success")
            }
        }

        //enable key notification
        private fun enableKeyPressNotification(gatt: BluetoothGatt) {
            val service = gatt.getService(Utils.CHARACTERISTIC_KEY_PRESS_SRV_UUID)
            if (service == null) {
                Log.e(TAG, ":setCharacteristicNotification set enable failed.")
                return
            }
            val characteristic = service.getCharacteristic(Utils.CHARACTERISTIC_KEY_PRESS_UUID)
            if (characteristic == null) {
                Log.e(TAG, ":setCharacteristicNotification set enable failed.")
                return
            }

            //set enable
            if (!gatt.setCharacteristicNotification(characteristic, true)) {
                Log.e(TAG, ":setCharacteristicNotification set enable failed.")
                return
            }
            val descriptor =
                characteristic.getDescriptor(Utils.CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID)
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            if (!gatt.writeDescriptor(descriptor)) {
                //descriptor write operation successfully started?
                Log.e(TAG, ":writeDescriptor set enable failed.")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val strMac = gatt.device.address
            if (strMac != mMacAddress) {
                Log.e(TAG, ":get iTrack failed.")
                return
            }
            val charUuid = characteristic.uuid
            if (charUuid == Utils.CHARACTERISTIC_KEY_PRESS_UUID) {
                val keyPressValue =
                    characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                mKeyNotify = if (keyPressValue == 2) {
                    KEY_HOLD_NOTIFY
                } else {
                    KEY_PRESS_NOTIFY
                }
                // TODO: 21/08/2021 find a way to implement this method 
                //mContext.sendUpdateListViewMsg()
                Log.e(TAG, String.format("Receive an key press ntf, value:%d", keyPressValue))
            }
        }
    }

    fun connect() {
        val device = mBluetoothAdapter!!.getRemoteDevice(mMacAddress)
        mBleConnStatus = BluetoothProfile.STATE_CONNECTING
        mBleGatt = device.connectGatt(mContext, false, mGattCallback)
        mTurnOffStatus = TURNOFF_DEV_INVALID
        mStartConnNum = 0
        mStartConnectTick = System.currentTimeMillis()
    }

    fun reConnect() {
        val device = mBluetoothAdapter!!.getRemoteDevice(mMacAddress)
        mBleConnStatus = BluetoothProfile.STATE_CONNECTING
        mBleGatt = device.connectGatt(mContext, false, mGattCallback)
        mStartConnNum++
    }


    fun updateRSSI(nNewRssi: Int, currTick: Long) {
        mRssiResult = if (mRssiResult == 0) {
            nNewRssi
        } else {
            (mRssiResult + nNewRssi) / 2
        }
        mLastUpdateTick = currTick
    }


    fun isConnected(): Boolean {
        return mBleConnStatus == BluetoothProfile.STATE_CONNECTED
    }

    fun isDisconnected(): Boolean {
        return mBleConnStatus == BluetoothProfile.STATE_DISCONNECTED
    }

    //get track state
    fun getTrackStatDesc(): String? {
        var currStateRslt = ""
        if (isAlerting) {
            currStateRslt += mContext?.getString(R.string.alert_desc).toString() + "|"
        }
        if (mKeyNotify == KEY_PRESS_NOTIFY) {
            currStateRslt += mContext?.getString(R.string.click_desc).toString() + "|"
        } else if (mKeyNotify == KEY_HOLD_NOTIFY) {
            currStateRslt += mContext?.getString(R.string.hold_desc).toString() + "|"
        }
        currStateRslt = when (mBleConnStatus) {
            BluetoothProfile.STATE_DISCONNECTED -> if (mTurnOffStatus == TURNOFF_DEV_SUCC) {
                currStateRslt + mContext?.getString(R.string.turnoff_desc).toString() + "|"
            } else {
                currStateRslt + mContext?.getString(R.string.disconnect_desc).toString() + "|"
            }
            BluetoothProfile.STATE_CONNECTED -> currStateRslt + mContext?.getString(R.string.connected_desc)
                .toString() + "|"
            BluetoothProfile.STATE_CONNECTING -> currStateRslt + mContext?.getString(R.string.connecting_desc)
                .toString() + "|"
            else -> currStateRslt + mContext?.getString(R.string.unknown_desc).toString() + "|"
        }
        return currStateRslt
    }

    //获取对应的characetristic
    private fun getCharacteristicByID(
        srvUUID: UUID,
        charaID: UUID
    ): BluetoothGattCharacteristic? {
        if (mBleGatt == null) {
            return null
        }
        val service = mBleGatt!!.getService(srvUUID) ?: return null

        //检查防丢器是否正确
        return service.getCharacteristic(charaID) ?: return null
    }

    fun closeConnection() {
        if (mBleGatt != null) {
            mBleGatt!!.close()
            mBleGatt = null
        }
        mBleConnStatus = BluetoothProfile.STATE_DISCONNECTED
        isAlerting = false
        mKeyNotify = 0
    }

//    fun turnOffTrack() {
//        if (!isConnected()) {
//            return
//        }
//        val byCallData = byteArrayOf(0x4)
//        val callgatt = getCharacteristicByID(
//            Utils.TI_KEYFOB_DISCONN_ALERT_SRV_UUID,
//            UUID.fromString(Utils.TI_KEYFOB_PROXIMITY_ALERT_PROPERTY_UUID)
//        )
//        if (callgatt != null) {
//            mBleGatt!!.setCharacteristicNotification(callgatt, true)
//
//            //设置数据内容
//            callgatt.value = byCallData
//
//            //往蓝牙模块写入数据
//            if (mBleGatt!!.writeCharacteristic(callgatt)) {
//                mTurnOffStatus = TURNOFF_DEV_SUCC
//                Log.i(TAG, "turnoff success, and connect next")
//            } else {
//                mContext.toastShow(mContext?.getString(R.string.ble_error_write))
//            }
//        } else {
//            mContext.toastShow(mContext?.getString(R.string.ble_error_write))
//        }
//    }
//
    fun makeTrackAlert() {
        try {
            if (!isConnected()) {
                return
            }
            val byCallData = byteArrayOf(0x2)
            if (isAlerting) {
                //turn off alert
                byCallData[0] = 0x0
            }
            val callgatt = getCharacteristicByID(
                UUID.fromString(Utils.TI_KEYFOB_PROXIMITY_ALERT_UUID),
                UUID.fromString(Utils.TI_KEYFOB_PROXIMITY_ALERT_PROPERTY_UUID)
            )
            if (callgatt != null) {
                //接受Characteristic被写的通知,收到蓝牙模块的数据后会触发mOnDataAvailable.onCharacteristicWrite()
                mBleGatt!!.setCharacteristicNotification(callgatt, true)

                //设置数据内容
                callgatt.value = byCallData

                //往蓝牙模块写入数据
                if (mBleGatt!!.writeCharacteristic(callgatt)) {
                    if (!isAlerting) {
                        //start alert
                        isAlerting = true
                      DGTracker.mHandler?.postDelayed(Runnable {
                            if (isAlerting) {
                                isAlerting = false
                                Log.e(TAG, "alert complete")
                                //mContext.sendUpdateListViewMsg()
                            }
                        }, DEFAULT_ALERT_TIME)
                        Log.e(TAG, "start call success")
                    } else {
                        //stop alert
                        isAlerting = false
                    }
                    //mContext.sendUpdateListViewMsg()
                }
            }
        } catch (excp: RuntimeException) {
            Log.e(TAG, "catch exception: Find gatt failed" + excp.message)
        }
    }

    companion object {

        val DEFAULT_ALERT_TIME : Long = 20 * 1000
        val KEY_PRESS_NOTIFY = 1
        val KEY_HOLD_NOTIFY = 2
        val MAX_RMV_TICK = 15 * 1000

        val TURNOFF_DEV_INVALID = 0
        val TURNOFF_DEV_START = 1
        val TURNOFF_DEV_TIMEOUT = 2
        val TURNOFF_DEV_SUCC = 3

        private const val TAG = "DGT:DGTBlePeripheral" //DeviceScanActivity.class.getSimpleName();

        val MAX_CONNECT_TIME: Long = 30 * 1000

        @ExperimentalUnsignedTypes // just to make it clear that the experimental unsigned types are used
        fun ByteArray.bytesToHexString() =
            asUByteArray().joinToString("") { it.toString(16).padStart(2, '0') }

    }

    init {
        mPref = SharedPreferenceManager.getInstance(context)
    }
}