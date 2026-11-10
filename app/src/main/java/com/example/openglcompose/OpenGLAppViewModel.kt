package com.example.openglcompose

import android.location.Location
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import projections.EastNorth

class OpenGLAppViewModel : ViewModel() {

    val accelValues = MutableStateFlow(floatArrayOf(0f, 0f, 0f))
    val magFieldValues = MutableStateFlow(floatArrayOf(0f, 0f, 0f))
    val orientMtx = MutableStateFlow(FloatArray(16))
    val pos = MutableStateFlow<EastNorth>(EastNorth(0.0,0.0))
}