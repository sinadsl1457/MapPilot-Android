package com.example.simpletracking.presentation.common

import android.Manifest

/** FINE(정밀)과 COARSE(대략)를 함께 요청해야 Android 12+에서 사용자가 "대략적 위치"를 고를 수 있다. */
val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)
