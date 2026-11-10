package freemap.openglwrapper

import android.opengl.Matrix

class GLMatrix(var values: FloatArray = floatArrayOf(1f,0f,0f,0f,0f,1f,0f,0f,0f,0f,1f,0f,0f,0f,0f,1f)) {

    val axes = mapOf('x' to floatArrayOf(1.0f, 0.0f, 0.0f), 'y' to floatArrayOf(0.0f, 1.0f, 0.0f), 'z' to floatArrayOf(0.0f, 0.0f, 1.0f))


    fun setAsIdentityMatrix() {
        Matrix.setIdentityM(values, 0)
    }

    fun setProjectionMatrix(hFov: Float, aspect: Float, near: Float, far: Float) {
        Matrix.perspectiveM(values, 0, hFov/aspect, aspect, near, far)
    }

    fun applyTranslationMatrix(dx: Float, dy: Float, dz: Float) {
        Matrix.translateM(values, 0, dx, dy, dz)
    }

    fun applyRotationMatrix(angle: Float, x: Float, y: Float, z: Float) {
        Matrix.rotateM(values, 0, angle, x, y, z)
    }

    fun rotateAboutAxis(angle: Float, axis: Char) : Boolean {
        axes[axis]?.apply {
            applyRotationMatrix(angle, this[0], this[1], this[2])
            return true
        }
        return false
    }

    fun rotateAboutY(angle: Float) {
        applyRotationMatrix(angle, 1.0f, 0.0f, 0.0f)
    }

    fun multiply(right: GLMatrix) {
        Matrix.multiplyMM(values, 0, values.clone(), 0, right.values, 0)
    }

    fun clone() : GLMatrix {
        return GLMatrix(values.clone())
    }

    fun multiply(vec: FloatArray) : FloatArray {
        val result = vec.clone()
        Matrix.multiplyMV(result, 0, values, 0, vec, 0)
        return result
    }

    override fun toString(): String {
        return values.contentToString()
    }

    fun correctSensorMatrix() {
        val correctionMatrix = GLMatrix(floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, -1f, 0f, 0f,
            0f, 0f, 0f, 1f
        ))

        multiply(correctionMatrix)
    }
}