package com.example.btracker.bluetooth

import android.Manifest
import android.annotation.TargetApi
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.text.TextUtils
import androidx.core.content.ContextCompat
import java.util.*

class Utils {

    companion object {

        val IMMEDIATE_ALERT_SERVICE_UUID_STRING = "00001802-0000-1000-8000-00805f9b34fb"
        val CUSTOM_SERVICE = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
        val PARCE_UUID_IMMEDIATE_ALERT_SERVICE =
            ParcelUuid.fromString("00001802-0000-1000-8000-00805f9b34fb")
        val PARCE_UUID_BEACON_SERVICE =
            ParcelUuid.fromString("EE0C2080-8786-40BA-AB96-99B91AC981D8")
        val PARCE_UUID_ESL_SERVICE = ParcelUuid.fromString("0000fea0-0000-1000-8000-00805f9b34fb")
        val CHARACTERISTIC_ANDROID_SYSTEM_FLAG =
            UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")
        val PARCE_CFG_UUID_EDDYSTONE = ParcelUuid.fromString("EE0C2080-8786-40BA-AB96-99B91AC981D8")
        val CHARACTERISTIC_CUSTOM_VERIFIED = UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb")

        val IMMEDIATE_ALERT_SERVICE_UUID = UUID.fromString(IMMEDIATE_ALERT_SERVICE_UUID_STRING)
        val CHARACTERISTIC_KEY_PRESS_SRV_UUID =
            UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        val UUID_KEY_DATA = "0000fff4-0000-1000-8000-00805f9b34fb"
        val TI_KEYFOB_PROXIMITY_ALERT_PROPERTY_UUID = "00002a06-0000-1000-8000-00805f9b34fb"
        val CHARACTERISTIC_KEY_PRESS_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        val CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        val TI_KEYFOB_PROXIMITY_ALERT_UUID = "00001802-0000-1000-8000-00805f9b34fb"
        val TI_KEYFOB_BATT_SERVICE_UUID = "0000180F-0000-1000-8000-00805f9b34fb"
        val TI_KEYFOB_DISCONN_ALERT_SRV_UUID_STR = "00001803-0000-1000-8000-00805f9b34fb"
        val TI_KEYFOB_DISCONN_ALERT_SRV_UUID = UUID.fromString(TI_KEYFOB_DISCONN_ALERT_SRV_UUID_STR)

        val TI_KEYFOB_ITRACK_SRV_UUID = "0000FFF0-0000-1000-8000-00805f9b34fb"
        val TI_KEYFOB_KEYS_SERVICE_UUID = "0000FFE0-0000-1000-8000-00805f9b34fb"


        private val serviceTypes: HashMap<Int?, String?> = hashMapOf(
            BluetoothGattService.SERVICE_TYPE_PRIMARY to "PRIMARY",
            BluetoothGattService.SERVICE_TYPE_SECONDARY to "SECONDARY"
        )

        fun getServiceType(type: Int): String? {
            return serviceTypes[type]
        }


        //-------------------------------------------
        private val charPermissions: HashMap<Int?, String?> = hashMapOf(
            0 to "UNKNOW",
            BluetoothGattCharacteristic.PERMISSION_READ to "READ",
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED to "READ_ENCRYPTED",
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM to "READ_ENCRYPTED_MITM",
            BluetoothGattCharacteristic.PERMISSION_WRITE to "WRITE",
            BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED to "WRITE_ENCRYPTED",
            BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM to "WRITE_ENCRYPTED_MITM",
            BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED to "WRITE_SIGNED",
            BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM to "WRITE_SIGNED_MITM"
        )

        fun getCharPermission(permission: Int): String? {
            return getHashMapValue(charPermissions, permission)
        }

        //-------------------------------------------
        private val charProperties: HashMap<Int?, String?> = hashMapOf(
            BluetoothGattCharacteristic.PROPERTY_BROADCAST to "BROADCAST",
            BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS to "EXTENDED_PROPS",
            BluetoothGattCharacteristic.PROPERTY_INDICATE to "INDICATE",
            BluetoothGattCharacteristic.PROPERTY_NOTIFY to "NOTIFY",
            BluetoothGattCharacteristic.PROPERTY_READ to "READ",
            BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE to "SIGNED_WRITE",
            BluetoothGattCharacteristic.PROPERTY_WRITE to "WRITE",
            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE to "WRITE_NO_RESPONSE"
        )

        fun getCharPropertie(property: Int): String? {
            return getHashMapValue(charProperties, property)
        }

        //--------------------------------------------------------------------------
        private val descPermissions: HashMap<Int?, String?> = hashMapOf(
            0 to "UNKNOW",
            BluetoothGattDescriptor.PERMISSION_READ to "READ",
            BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED to "READ_ENCRYPTED",
            BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM to "READ_ENCRYPTED_MITM",
            BluetoothGattDescriptor.PERMISSION_WRITE to "WRITE",
            BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED to "WRITE_ENCRYPTED",
            BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM to "WRITE_ENCRYPTED_MITM",
            BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED to "WRITE_SIGNED",
            BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM to "WRITE_SIGNED_MITM",
        )

        fun getDescPermission(property: Int): String? {
            return getHashMapValue(descPermissions, property)
        }


        private fun getHashMapValue(hashMap: HashMap<Int?, String?>, number: Int): String? {
            var result = hashMap[number]
            if (TextUtils.isEmpty(result)) {
                val numbers = getElement(number)
                result = ""
                for (i in numbers.indices) {
                    result += hashMap[numbers[i]].toString() + "|"
                }
            }
            return result
        }

        @TargetApi(Build.VERSION_CODES.M)
        fun isLocationBluePermission(context: Context): Boolean {
            return if (!isMPhone()) {
                true
            } else {
                var result = true
                if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    result = false
                }
                result
            }
        }

        fun isMPhone(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        }

        /**
         * 位运算结果的反推函数10 -> 2 | 8;
         */
        private fun getElement(number: Int): List<Int> {
            val result: MutableList<Int> = ArrayList()
            for (i in 0..31) {
                val b = 1 shl i
                if (number and b > 0) result.add(b)
            }
            return result
        }


//    fun bytesToHexString(src: ByteArray?): String? {
//        val stringBuilder = StringBuilder("")
//        if (src == null || src.size <= 0) {
//            return null
//        }
//        for (i in src.indices) {
//            val v: Int = src[i] and 0xFF
//            val hv = Integer.toHexString(v)
//            if (hv.length < 2) {
//                stringBuilder.append(0)
//            }
//            stringBuilder.append(hv)
//        }
//        return stringBuilder.toString()
//    }

        @ExperimentalUnsignedTypes // just to make it clear that the experimental unsigned types are used
        fun bytesToHexString(src: ByteArray?): String? {
            return src?.joinToString("") { it.toString(16).padStart(2, '0') }
        }
    }
}