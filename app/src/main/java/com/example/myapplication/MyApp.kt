package com.example.myapplication

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.LifecycleOwner
import timber.log.Timber

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                logLifecycle("onActivityCreated", activity)
            }

            override fun onActivityStarted(activity: Activity) {
                logLifecycle("onActivityStarted", activity)
            }

            override fun onActivityResumed(activity: Activity) {
                logLifecycle("onActivityResumed", activity)
            }

            override fun onActivityPaused(activity: Activity) {
                logLifecycle("onActivityPaused", activity)
            }

            override fun onActivityStopped(activity: Activity) {
                logLifecycle("onActivityStopped", activity)
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                // Обов'язковий метод інтерфейсу, для цього завдання залишаємо порожнім
            }

            override fun onActivityDestroyed(activity: Activity) {
                logLifecycle("onActivityDestroyed", activity)
            }
        })
    }

    private fun logLifecycle(callbackName: String, activity: Activity) {
        val screenName = activity.javaClass.simpleName
        val currentState = (activity as? LifecycleOwner)?.lifecycle?.currentState
        Timber.d("$callbackName -> $screenName | currentState: $currentState")
    }
}