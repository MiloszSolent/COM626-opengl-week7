package com.example.openglcompose

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.GnssAntennaInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.viewinterop.AndroidView
import com.example.openglcompose.ui.theme.OpenGLComposeTheme
import projections.EastNorth
import projections.LonLat
import projections.SphericalMercatorProjection

class MainActivity : ComponentActivity(), SensorEventListener, LocationListener {
    val viewModel : OpenGLAppViewModel by viewModels()
    val proj = SphericalMercatorProjection()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Set to true on the Lenovo tablets (no magnetometer) or false on devices with a magnetometer
        val useGameSensor = true

        checkPermission()

        setContent {
            OpenGLComposeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // TODO setup state variable for the orientation matrix
                    val orientMatrix = viewModel.orientMtx.collectAsState()
                    val pos = viewModel.pos.collectAsState()

                    // TODO pass the current orientation matrix to OpenGLComposable
                    OpenGLComposable(modifier = Modifier.padding(innerPadding), orientMatrix.value, pos.value)
                }
            }
        }
        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val gameSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

        if(useGameSensor) {
            sensorManager.registerListener(this, gameSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(this, magFieldSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged (ev: SensorEvent) {
        when(ev.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                viewModel.accelValues.value = ev.values.clone()
                val orientMtx = FloatArray(16)
                SensorManager.getRotationMatrix(orientMtx, null, viewModel.accelValues.value,
                    viewModel.magFieldValues.value)
                viewModel.orientMtx.value = orientMtx.clone()
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
                viewModel.magFieldValues.value = ev.values.clone()
            }

            Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                val orientMtx = FloatArray(16)
                SensorManager.getRotationMatrixFromVector(orientMtx, ev.values)
                viewModel.orientMtx.value = orientMtx.clone()
            }
        }
    }

    fun checkPermission() {
        val requiredPermission = Manifest.permission.ACCESS_FINE_LOCATION

        if (checkSelfPermission(requiredPermission) == PackageManager.PERMISSION_GRANTED) {
            startGPS() // a function to start the GPS - see below
        }
        else {
            val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if(isGranted) {
                    startGPS() // A function to start the GPS - see below
                } else {
                    // Permission not granted
                    Toast.makeText(this, "GPS permission not granted", Toast.LENGTH_LONG).show()
                }
            }
            permissionLauncher.launch(requiredPermission)
        }
    }

    @SuppressLint("MissingPermission")
    fun startGPS() {
        val mgr = getSystemService(LOCATION_SERVICE) as LocationManager
        mgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this )
    }

    override fun onAccuracyChanged(sensor: Sensor, acc: Int) {

    }

    override fun onLocationChanged(location: Location) {
        Log.d("POS", location.toString())

        viewModel.pos.value = proj.project(LonLat(location.longitude, location.latitude))
    }
}

@Composable
fun OpenGLComposable(modifier: Modifier, orientMatrix: FloatArray, pos: EastNorth) {
    AndroidView(modifier=modifier,
        factory = { ctx ->
            val glView = OpenGLView(ctx)
            glView
        },
        update = {
            gLView ->
            // TODO
            // This should update the orientation matrix within the OpenGLView when the
            // parameter changes
            gLView.sensorMatrix.values = orientMatrix.clone()
            gLView.sensorMatrix.multiply(gLView.mtxSensorConversion)

            Log.d("pos", "${pos.easting.toString()}, ${pos.northing.toString()}")

            gLView.cam.position.x = pos.easting.toFloat()
            gLView.cam.position.z = -pos.northing.toFloat()
            Log.d("cam", gLView.cam.position.toString())
        }
    )
}