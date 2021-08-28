package com.example.digit_api_check

import android.app.Application

import com.example.btracker.DGTracker

class DominoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        configure()
    }

    private fun configure(){
        DGTracker.initialize(this)
    }
}