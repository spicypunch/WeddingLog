package kr.jm.weddingledger.feature.auth.data.platform

import androidx.activity.ComponentActivity
import java.lang.ref.WeakReference

object AndroidAuthActivityHolder {
    private var currentRef: WeakReference<ComponentActivity>? = null

    fun set(activity: ComponentActivity) {
        currentRef = WeakReference(activity)
    }

    fun clear(activity: ComponentActivity) {
        if (currentRef?.get() === activity) {
            currentRef = null
        }
    }

    fun get(): ComponentActivity? = currentRef?.get()
}
