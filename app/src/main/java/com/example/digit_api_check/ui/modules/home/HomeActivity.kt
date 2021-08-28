package com.example.digit_api_check.ui.modules.home

import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.btracker.DGTracker
import com.example.btracker.DGTracker.Companion.mBLESrvMgr
import com.example.btracker.bluetooth.BluetoothLeClass.Companion.MAX_CONCURRENT_CONN_NUM
import com.example.btracker.bluetooth.DGTBlePeripheral
import com.example.digit_api_check.R

class HomeActivity : AppCompatActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var homeContext: Context
    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        setSupportActionBar(findViewById(R.id.topAppBar))
        homeContext = this

        observe()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        return super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.top_app_bar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.scan -> {
                homeViewModel.scan(this)
                true
            }
            R.id.shutdown -> {
                homeViewModel.stop(DGTracker.getBluetoothAdapter())
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun observe() {
        val owner = { lifecycle }

        with(homeViewModel) {
            listView = findViewById(R.id.device_list_view)
            devices.observe(owner) {
                val adapter = DeviceAdapter(homeContext, ArrayList(it.values))
                listView.adapter = adapter

                btnListener()
            }

        }

    }

    private fun btnListener() {
        listView.setOnItemClickListener { parent, view, position, id ->
            val device = parent.getItemAtPosition(position)
            if (device != null && device is DGTBlePeripheral) {
                Toast.makeText(homeContext, device.mBleDevName, Toast.LENGTH_SHORT).show()

                homeViewModel.stop(DGTracker.getBluetoothAdapter())

                if (device.mBleConnStatus == BluetoothProfile.STATE_DISCONNECTED) {
                    val nConnectedTrackNum: Int = mBLESrvMgr.getConnectedTrackNum()
                    if (nConnectedTrackNum < MAX_CONCURRENT_CONN_NUM) {
                        device.mMacAddress?.let { mBLESrvMgr.startConnect(it) }
                    } else {
                        Log.e(TAG, "Exceed max connected ble device number that android supported")
                    }
                }
                Log.e(TAG, "click id:$id")
                //if connected, then send notify to iTrack to make sound
                if (device.mBleConnStatus == BluetoothProfile.STATE_CONNECTED) {
                    device.makeTrackAlert()
                }
            } else {
                Toast.makeText(
                    homeContext, "device is either null or could not be cast",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    companion object {
        private const val TAG = "DGT:HomeActivity"

        fun getContext() = this
    }
}