package com.inhatc.smartwallsaver.scan

import android.opengl.GLES11Ext
import android.opengl.GLES20
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

// ARCore - 카메라 영상을 화면 배경에 강제로 렌더링해주는 OpenGL 헬퍼 클래스
class CameraBackgroundRenderer {

    private var programId = -1
    private var positionHandle = -1
    private var texCoordHandle = -1
    var textureId = -1
        private set

    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var quadTexCoordBuffer: FloatBuffer
    private lateinit var transTexCoordBuffer: FloatBuffer

    private val quadCoordinates = floatArrayOf(
        -1.0f, -1.0f, 0.0f,
        -1.0f, +1.0f, 0.0f,
        +1.0f, -1.0f, 0.0f,
        +1.0f, +1.0f, 0.0f
    )

    private val quadTexCoordinates = floatArrayOf(
        0.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 1.0f,
        1.0f, 0.0f
    )

    fun init() {
        // 카메라 외부 OES 텍스처 생성
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)

        vertexBuffer = ByteBuffer.allocateDirect(quadCoordinates.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(quadCoordinates)
                position(0)
            }

        quadTexCoordBuffer = ByteBuffer.allocateDirect(quadTexCoordinates.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(quadTexCoordinates)
                position(0)
            }

        transTexCoordBuffer = ByteBuffer.allocateDirect(quadTexCoordinates.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()

        val vertexShaderSource = """
            attribute vec4 a_Position;
            attribute vec2 a_TexCoord;
            varying vec2 v_TexCoord;
            void main() {
                gl_Position = a_Position;
                v_TexCoord = a_TexCoord;
            }
        """.trimIndent()

        val fragmentShaderSource = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 v_TexCoord;
            uniform samplerExternalOES s_Texture;
            void main() {
                gl_FragColor = texture2D(s_Texture, v_TexCoord);
            }
        """.trimIndent()

        val vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER).apply {
            GLES20.glShaderSource(this, vertexShaderSource)
            GLES20.glCompileShader(this)
        }
        val fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER).apply {
            GLES20.glShaderSource(this, fragmentShaderSource)
            GLES20.glCompileShader(this)
        }

        programId = GLES20.glCreateProgram().apply {
            GLES20.glAttachShader(this, vertexShader)
            GLES20.glAttachShader(this, fragmentShader)
            GLES20.glLinkProgram(this)
        }

        positionHandle = GLES20.glGetAttribLocation(programId, "a_Position")
        texCoordHandle = GLES20.glGetAttribLocation(programId, "a_TexCoord")
    }

    fun draw(frame: Frame) {
        if (frame.hasDisplayGeometryChanged()) {
            // 디바이스 스크린 기준 표준 2D 뷰 좌표를 카메라 텍스처 규격으로 타겟팅 변환
            frame.transformCoordinates2d(
                Coordinates2d.VIEW_NORMALIZED,
                quadTexCoordBuffer,
                Coordinates2d.TEXTURE_NORMALIZED,
                transTexCoordBuffer
            )
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)

        GLES20.glUseProgram(programId)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(positionHandle)

        transTexCoordBuffer.position(0)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, transTexCoordBuffer)
        GLES20.glEnableVertexAttribArray(texCoordHandle)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
        GLES20.glDepthMask(true)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }
}