package com.example.openglcompose

import android.opengl.GLSurfaceView
import android.content.Context
import javax.microedition.khronos.egl.EGLConfig
import android.opengl.GLES20
import android.util.Log
import freemap.openglwrapper.Camera
import freemap.openglwrapper.GLMatrix
import javax.microedition.khronos.opengles.GL10
import freemap.openglwrapper.GPUInterface
import freemap.openglwrapper.OpenGLUtils
import java.io.IOException

class OpenGLView(val ctx: Context)  :GLSurfaceView(ctx), GLSurfaceView.Renderer {
    init {
        setEGLContextClientVersion(2) // specify OpenGL ES 2.0
        setRenderer(this) // set the renderer for this GLSurfaceView
    }

    lateinit var gpu: GPUInterface
    var refUColour = -1
    var refUViewMtx = -1
    var refUProjMtx = -1
    val red = floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f)
    val yellow = floatArrayOf(1.0f, 1.0f, 0.0f, 1.0f)
    val blue = floatArrayOf(0.0f, 0.0f, 1.0f, 1.0f)
    val green = floatArrayOf(0.0f, 1.0f, 0.0f, 1.0f)

    val viewMatrix = GLMatrix()
    val projMatrix = GLMatrix()
    val sensorMatrix = GLMatrix()
    val mtxSensorConversion = GLMatrix(
    floatArrayOf(1f, 0f, 0f, 0f,
                        0f, 0f, 1f, 0f,
                        0f, -1f, 0f, 0f,
                        0f, 0f, 0f, 1f)
    )

    val cam = Camera()

    // We initialise the rendering here
    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        // Set the background colour (red=0, green=0, blue=0, alpha=1)
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // Enable depth testing - will cause nearer 3D objects to automatically
        // be drawn over further objects
        GLES20.glClearDepthf(1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        gpu = GPUInterface("mainGpu")
        try {
            if(!gpu.loadShaders(ctx.assets, "vertex.glsl", "fragment.glsl")) {
                Log.e("OpenGL01", gpu.lastShaderError)
            } else {
                // Four triangles, two (red and yellow) towards negative z, two (blue and green) towards positive z
                val coords = floatArrayOf (
                    0f, 0f,-3f,
                    1f, 0f, -3f,
                    0.5f, 1f, -3f,
                    -0.5f, 0f, -6f,
                    0.5f, 0f, -6f,
                    0f, 1f, -6f,
                    0f, 0f,3f,
                    0.5f, 1f, 3f,
                    1f, 0f, 3f,
                    0.5f, 0f, 6f,
                    -0.5f, 0f, 6f,
                    0f, 1f, 6f
                        ).map { it * 50f}.toFloatArray()
                val fbuf = OpenGLUtils.makeFloatBuffer(coords)

                val refAVertex = gpu.getAttribLocation("aVertex")
                gpu.specifyBufferedDataFormat(refAVertex, fbuf, 0)
                refUColour = gpu.getUniformLocation("uColour")
                refUViewMtx = gpu.getUniformLocation("uViewMtx")
                refUProjMtx = gpu.getUniformLocation("uProjMtx")
               
            }
        } catch (e: IOException) {
            Log.e("OpenGL01", e.stackTraceToString())
        }
    }

    // We draw our shapes here
    override fun onDrawFrame(unused: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        viewMatrix.setAsIdentityMatrix()
        viewMatrix.multiply(sensorMatrix)

        viewMatrix.applyTranslationMatrix(-cam.position.x, -cam.position.y, -cam.position.z)

        gpu.sendMatrix(refUViewMtx, viewMatrix)

        if (refUColour >= 0) {
            gpu.setUniform4FloatArray(refUColour, red)
            gpu.drawBufferedTriangles(0, 3)
            gpu.setUniform4FloatArray(refUColour, yellow)
            gpu.drawBufferedTriangles(3, 3)
            gpu.setUniform4FloatArray(refUColour, blue)
            gpu.drawBufferedTriangles(6, 3)
            gpu.setUniform4FloatArray(refUColour, green)
            gpu.drawBufferedTriangles(9, 3)
        }
    }

    // Used if the screen is resized
    override fun onSurfaceChanged(unused: GL10, w: Int, h: Int) {
        GLES20.glViewport(0, 0, w, h)
        val aspect = w.toFloat() / h
        projMatrix.setProjectionMatrix(60f, aspect, 0.001f, 1000f)
        if(refUProjMtx > -1) {
            gpu.sendMatrix(refUProjMtx, projMatrix)
        }
    }
}