package com.wyc.download;

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

class App : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private var mContext: Context? = null
        @SuppressLint("StaticFieldLeak")
        private var mInstance: App? = null

        @JvmStatic
        fun instance() = mInstance!!

        @JvmStatic
        fun context() = mContext!!
    }

    override fun onCreate() {
        super.onCreate()
        mContext = this.applicationContext
        mInstance = this
    }
}

