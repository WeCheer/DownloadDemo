package com.wyc.download.callback

interface DownloadCallBack {

    fun onProgress(progress: Int)

    fun onCompleted()

    fun onError(msg: String)
}
