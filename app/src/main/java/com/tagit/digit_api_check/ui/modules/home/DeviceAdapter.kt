package com.tagit.digit_api_check.ui.modules.home;

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.tagit.btracker.bluetooth.DGTBlePeripheral
import com.tagit.digit_api_check.R

internal class DeviceAdapter(
    private val context: Context,
    private val dataSource: ArrayList<DGTBlePeripheral>

) : BaseAdapter() {

    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return dataSource.size
    }

    override fun getItem(position: Int): Any {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup?): View {
        val rowView = inflater.inflate(R.layout.list_item_device, parent, false)

        // Get title element
        val deviceName = rowView.findViewById(R.id.device_name) as TextView

        // Get subtitle element
        val deviceStatus = rowView.findViewById(R.id.device_state) as TextView

        // Get detail element
        val deviceAddress = rowView.findViewById(R.id.device_address) as TextView

        // Get detail element
        val deviceTestResult = rowView.findViewById(R.id.device_test_result) as TextView

        val device = getItem(position) as DGTBlePeripheral

        deviceName.text = device.mBleDevName
        deviceStatus.text = device.getTrackStatDesc()
        deviceAddress.text = device.mMacAddress

        if (device.isConnected()) {
            deviceStatus.setTextColor(Color.BLUE)
        } else {
            deviceStatus.setTextColor(Color.BLACK)
        }

        val strRssiValue: String = "RSSI: ".plus(java.lang.String.valueOf(device.mRssiResult))
        deviceTestResult.text = strRssiValue

        return rowView
//        val viewHolder: RecyclerView.ViewHolder
//
//        // General ListView optimization code.
//        if (view == null) {
//            view = mInflator.inflate(R.layout.listitem_device, null)
//            viewHolder = com.example.bluetooth.itrackdemo.LeDeviceListAdapter.ViewHolder()
//            viewHolder.deviceAddress = view
//                .findViewById<View>(R.id.device_address) as TextView
//            viewHolder.deviceName = view
//                .findViewById<View>(R.id.device_name) as TextView
//            viewHolder.deviceState = view
//                .findViewById<View>(R.id.device_state) as TextView
//            viewHolder.deviceRssiRslt = view
//                .findViewById<View>(R.id.deviceTestResult) as TextView
//            view.tag = viewHolder
//        } else {
//            viewHolder = view.tag as com.example.bluetooth.itrackdemo.LeDeviceListAdapter.ViewHolder
//        }
//
//        val device: TIBLEPerperal = mContext.getBlePerp(i) ?: return null
//
//        if (device.mBleDevName != null && device.mBleDevName.length() > 0) {
//            viewHolder.deviceName.setText(device.mBleDevName)
//        } else {
//            viewHolder.deviceName.setText(R.string.unknown_device)
//        }
//        viewHolder.deviceAddress.setText(device.mMacAddress)
//
//        //状态
//
//        //状态
//        viewHolder.deviceState.setText(device.getTrackStatDesc())
//        if (device.isConnected()) {
//            viewHolder.deviceState.setTextColor(Color.BLUE)
//        } else {
//            viewHolder.deviceState.setTextColor(Color.BLACK)
//        }
//
//        val strRssiValue: String =
//            mContext.getString(R.string.rssivalue) + java.lang.String.valueOf(device.mRssiResult)
//        viewHolder.deviceRssiRslt.setText(strRssiValue)
//
//        return view
    }


}
