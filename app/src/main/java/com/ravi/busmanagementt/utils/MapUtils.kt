package com.ravi.busmanagementt.utils

// In a new file like utils/IntentManager.kt or an existing helper file

import android.Manifest

object MapUtils {
    val permissionToRequest = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

}
