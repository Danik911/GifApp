package com.example.gifapp.domain.util

import android.os.Build

/**
 * Provide the [Build.VERSION.SDK_INT] in app code. This makes checks on the API simple for
 * Unit test
 */
interface VersionProvider {
    fun provideVersion(): Int
}

class RealVersionProvider
constructor() : VersionProvider {
    override fun provideVersion() = Build.VERSION.SDK_INT

}