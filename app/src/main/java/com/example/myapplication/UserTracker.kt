package com.example.myapplication

import android.content.Context

object UserTracker {
    private val listeners = mutableListOf<Context>()

    fun register(context: Context) {
        listeners.add(context)
    }

    fun getListenersCount(): Int = listeners.size
}