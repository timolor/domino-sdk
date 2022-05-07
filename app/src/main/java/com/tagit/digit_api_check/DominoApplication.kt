package com.tagit.digit_api_check

import android.app.Application

import com.tagit.btracker.DGTracker

class DominoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        configure()
    }

    private fun configure(){
        DGTracker.initialize(this)
    }
}