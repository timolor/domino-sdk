package com.tagit.btracker.bluetooth

import android.content.Context

class SharedPreferenceManager private constructor() {
    private var mContext: Context? = null

    private var mMinRssiValue = DEF_MIN_RSSI_VALUE
    private val DEF_TRACK_NAME = "iTrack"
    private var mOtherTrackName: String? = DEF_TRACK_NAME

    private val mMusicIndex: Long = 0

    //初始化配置
    fun initSetting(ctx: Context?) {
        mContext = ctx
        val shareReference = mContext!!.getSharedPreferences(SETTING_INFO, Context.MODE_PRIVATE)
        mMinRssiValue = shareReference.getInt(FLD_MIN_RSSI_VALUE, DEF_MIN_RSSI_VALUE)
        mOtherTrackName = shareReference.getString(FLD_OTH_TRACK, DEF_TRACK_NAME)
    }

    fun setRssiValue(nMinRssi: Int) {
        if (nMinRssi == mMinRssiValue) {
            return
        }
        val shareReference = mContext!!.getSharedPreferences(SETTING_INFO, Context.MODE_PRIVATE)
        val edit = shareReference.edit()
        edit.putInt(FLD_MIN_RSSI_VALUE, nMinRssi)
        edit.apply()
        mMinRssiValue = nMinRssi
    }


    fun setFilterTrackName(strTrackName: String) {
        if (mOtherTrackName == strTrackName) {
            return
        }
        val shareReference = mContext!!.getSharedPreferences(SETTING_INFO, Context.MODE_PRIVATE)
        val edit = shareReference.edit()
        edit.putString(FLD_OTH_TRACK, strTrackName)
        edit.apply()
        mOtherTrackName = strTrackName
    }

    fun getTrackname(): String? {
        return mOtherTrackName
    }


    fun getRssiValue(): Int {
        return mMinRssiValue
    }

    companion object {
        private val SETTING_INFO = "SETTING_INFO"
        private val FLD_MIN_RSSI_VALUE = "MIN_RSSI_VALUE"
        private val FLD_OTH_TRACK = "DEF_TRACK_FIELD"
        private val DEF_MIN_RSSI_VALUE = -75

        private lateinit var sPrefMgr: SharedPreferenceManager

        fun getInstance(ctx: Context?): SharedPreferenceManager {
            if (!this::sPrefMgr.isInitialized) {
                synchronized(this) {
                    sPrefMgr = SharedPreferenceManager()
                }
                sPrefMgr.initSetting(ctx)
            }
            return sPrefMgr
        }
    }
}