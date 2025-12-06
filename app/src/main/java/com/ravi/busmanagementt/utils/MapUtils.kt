package com.ravi.busmanagementt.utils

// In a new file like utils/IntentManager.kt or an existing helper file

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri

object MapUtils {
    val permissionToRequest = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

}
