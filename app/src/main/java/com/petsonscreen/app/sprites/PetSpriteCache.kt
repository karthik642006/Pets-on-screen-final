package com.petsonscreen.app.sprites

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object PetSpriteCache {
    private val bitmapCache = ConcurrentHashMap<String, Bitmap>()
    private val composeCache = ConcurrentHashMap<String, ImageBitmap>()

    fun clearPet(petId: Int) {
        val prefix = "${petId}_"
        bitmapCache.keys.filter { it.startsWith(prefix) }.forEach { bitmapCache.remove(it) }
        composeCache.keys.filter { it.startsWith(prefix) }.forEach { composeCache.remove(it) }
    }

    fun getComposeFrame(context: Context, petId: Int, frameIndex: Int): ImageBitmap {
        val idx = frameIndex.coerceIn(0, 45)
        val key = "${petId}_$idx"
        return composeCache.getOrPut(key) {
            getBitmapFrame(context, petId, idx).asImageBitmap()
        }
    }

    fun getBitmapFrame(context: Context, petId: Int, frameIndex: Int): Bitmap {
        val idx = frameIndex.coerceIn(0, 45)
        val key = "${petId}_$idx"
        return bitmapCache.getOrPut(key) {
            loadFromDiskOrFallback(context.applicationContext, petId, idx)
        }
    }

    private fun loadFromDiskOrFallback(context: Context, petId: Int, frameIndex: Int): Bitmap {
        val frameNum = frameIndex + 1
        if (petId in 1..7) {
            val path = "pets/$petId/shime$frameNum.png"
            try {
                context.assets.open(path).use { stream ->
                    BitmapFactory.decodeStream(stream)?.let { return it }
                }
            } catch (_: Exception) {
                // Fallback to shime1.png in assets, then canvas generator
                try {
                    context.assets.open("pets/$petId/shime1.png").use { stream ->
                        BitmapFactory.decodeStream(stream)?.let { return it }
                    }
                } catch (_: Exception) {
                }
            }
        } else {
            val dir = File(context.filesDir, "pets/data_$petId")
            val frameFile = File(dir, "shime$frameNum.png")
            if (frameFile.exists()) {
                BitmapFactory.decodeFile(frameFile.absolutePath)?.let { return it }
            }
            val fallbackFile = File(dir, "shime1.png")
            if (fallbackFile.exists()) {
                BitmapFactory.decodeFile(fallbackFile.absolutePath)?.let { return it }
            }
        }
        return renderFallbackBitmap(petId, frameIndex)
    }

    private fun renderFallbackBitmap(petId: Int, frameIndex: Int): Bitmap {
        val size = 128
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val bodyColor = when (petId) {
            1 -> Color.rgb(250, 247, 242)
            2 -> Color.rgb(58, 64, 78)
            3 -> Color.rgb(248, 244, 238)
            4 -> Color.rgb(218, 154, 92)
            5 -> Color.rgb(248, 246, 242)
            6 -> Color.rgb(107, 168, 120)
            7 -> Color.rgb(218, 108, 56)
            else -> Color.rgb(242, 235, 224)
        }
        val outlineColor = Color.rgb(32, 30, 36)
        val goldColor = Color.rgb(182, 141, 64)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bodyColor
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = outlineColor
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = goldColor
            style = Paint.Style.FILL
        }
        val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (petId == 2) goldColor else outlineColor
            style = Paint.Style.FILL
        }

        val bobY = when (frameIndex) {
            1, 2 -> -3f
            3, 21 -> -8f
            10, 25, 30, 31 -> 8f
            else -> 0f
        }

        // Ears
        val earL = Path().apply {
            moveTo(38f, 44f + bobY)
            lineTo(35f, 18f + bobY)
            lineTo(54f, 34f + bobY)
            close()
        }
        val earR = Path().apply {
            moveTo(70f, 34f + bobY)
            lineTo(86f, 18f + bobY)
            lineTo(86f, 44f + bobY)
            close()
        }
        canvas.drawPath(earL, fillPaint)
        canvas.drawPath(earL, strokePaint)
        canvas.drawPath(earR, fillPaint)
        canvas.drawPath(earR, strokePaint)

        // Body
        val bodyRect = RectF(44f, 68f + bobY, 86f, 106f + bobY)
        canvas.drawOval(bodyRect, fillPaint)
        canvas.drawOval(bodyRect, strokePaint)

        // Collar
        canvas.drawRoundRect(RectF(50f, 67f + bobY, 74f, 73f + bobY), 4f, 4f, accentPaint)

        // Head
        val headRect = RectF(34f, 28f + bobY, 88f, 74f + bobY)
        canvas.drawOval(headRect, fillPaint)
        canvas.drawOval(headRect, strokePaint)

        // Eyes facing left
        canvas.drawCircle(48f, 52f + bobY, 4f, eyePaint)
        canvas.drawCircle(68f, 52f + bobY, 4f, eyePaint)

        return bmp
    }
}
