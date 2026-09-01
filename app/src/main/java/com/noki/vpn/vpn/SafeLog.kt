package com.noki.vpn.vpn

import android.util.Log

object SafeLog {
    fun e(tag: String, message: String, error: Throwable? = null) {
        Log.e(tag, safeMessage(message, error))
    }

    fun w(tag: String, message: String, error: Throwable? = null) {
        Log.w(tag, safeMessage(message, error))
    }

    private fun safeMessage(message: String, error: Throwable?): String {
        if (error == null) return message
        return "$message (${error::class.java.simpleName})"
    }
}
