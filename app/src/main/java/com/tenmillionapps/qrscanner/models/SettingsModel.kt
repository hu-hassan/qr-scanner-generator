package com.tenmillionapps.qrscanner.models

data class SettingsModel(
    val title:String,
    val desc:String,
    var isChecked: Boolean = false // Add this property
)