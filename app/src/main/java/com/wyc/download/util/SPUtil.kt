package com.wyc.download.util

import android.content.Context
import android.content.SharedPreferences

import com.wyc.download.App


class SPUtil private constructor() {

    fun clear(): Boolean {
        return mSharedPreferences!!.edit().clear().commit()
    }


    fun save(key: String, value: Long): Boolean {
        return mSharedPreferences!!.edit().putLong(key, value).commit()
    }

    operator fun get(key: String, defValue: Long): Long {
        return mSharedPreferences!!.getLong(key, defValue)
    }

    companion object {
        private var mSharedPreferences: SharedPreferences? = null
        private var instance: SPUtil? = null

        fun getInstance(): SPUtil {
            if (mSharedPreferences == null || instance == null) {
                synchronized(SPUtil::class.java) {
                    if (mSharedPreferences == null || instance == null) {
                        instance = SPUtil()
                        mSharedPreferences = App.context().getSharedPreferences(
                                App.context().packageName + ".downloadSp", Context.MODE_PRIVATE)
                    }
                }
            }
            return instance!!
        }
    }

}

