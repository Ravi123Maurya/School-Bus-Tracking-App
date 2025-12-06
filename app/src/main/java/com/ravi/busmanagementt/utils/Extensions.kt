package com.ravi.busmanagementt.utils

import android.content.Context
import android.widget.Toast


var toast: Toast? = null
fun Context.showToast(text: String, duration: Int = Toast.LENGTH_SHORT){
   toast?.cancel()
    toast = Toast.makeText(this, text, duration)
    toast?.show()
}
