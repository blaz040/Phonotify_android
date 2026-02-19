package com.example.phonotify.datastore

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.google.protobuf.ByteString
import java.io.ByteArrayOutputStream

object IconSerializer {

    /**
     * Converts a Drawable to a Protobuf ByteString.
     * Includes a scaling step to ensure the DataStore file stays small.
     */
    fun toByteString(drawable: Drawable?, targetSizePx: Int = 144): ByteString {
        if (drawable == null) return ByteString.EMPTY

        val bitmap = if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            // Handle VectorDrawables by drawing them into a Bitmap
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else targetSizePx
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else targetSizePx
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }

        // Optional: Scale down if the icon is too large to save space
        val scaledBitmap = if (bitmap.width > targetSizePx || bitmap.height > targetSizePx) {
            Bitmap.createScaledBitmap(bitmap, targetSizePx, targetSizePx, true)
        } else {
            bitmap
        }

        val stream = ByteArrayOutputStream()
        // WEBP_LOSSLESS is the best balance of quality and size for Android icons
        scaledBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, stream)

        return ByteString.copyFrom(stream.toByteArray())
    }

    /**
     * Converts a Protobuf ByteString back into a Drawable.
     */
    fun toDrawable(context: Context, byteString: ByteString): Drawable? {
        if (byteString.isEmpty) return null

        val bytes = byteString.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        return BitmapDrawable(context.resources, bitmap)
    }
}