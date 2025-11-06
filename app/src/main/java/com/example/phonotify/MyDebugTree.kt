package com.example.phonotify

import android.content.Context
import android.util.Log
import com.example.ble_con.fileManager.FileManager
import timber.log.Timber

class MyDebugTree(
    private val context: Context
) : Timber.DebugTree() {

    private val myFileManager = FileManager(context)
    private val TAGprefix = "Timber-"
    override fun createStackElementTag(element: StackTraceElement): String? {
        // By default, Timber uses element.fileName or class name
        // You can include method name, line number, or package
        return "$TAGprefix${element.fileName}"
    }
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {

        val myTag = if(tag == null) "" else tag.replace(TAGprefix,"")
        myFileManager.log(priority,myTag,message)
        // Call parent to write to Logcat
        super.log(priority, tag, message, t)
    }
}