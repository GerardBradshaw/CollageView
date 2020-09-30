package com.gerardbradshaw.library.collageview

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import com.gerardbradshaw.library.utils.ImageParams

class CollageView3Image0(context: Context, attrs: AttributeSet?,
                         totalWidth: Int, totalHeight: Int,
                         isBorderEnabled: Boolean = false,
                         imageUris: Array<Uri?>? = null) :
  AbstractCollageView(context, attrs, 3,
    totalWidth, totalHeight, isBorderEnabled, imageUris),
  View.OnTouchListener {


  // ------------------------ INITIALIZATION ------------------------

  // Do not change
  init {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
      override fun onGlobalLayout() {
        if (height > 0) {
          addViewsToLayout()
          initImageLayout()
          addImagesToViews()
          prepareTouchListeners()
          enableBorder(isBorderEnabled)

          isFrameInflated = true
          viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
      }
    })
  }

  private fun initImageLayout() {
    imageParamsCache[0] = ImageParams(
      width = layoutWidth.toFloat(),
      height = layoutHeight / 2f,
      x = 0f,
      y = 0f)

    imageParamsCache[1] = ImageParams(
      width = layoutWidth / 2f,
      height = layoutHeight / 2f,
      x = 0f,
      y = layoutHeight / 2f)

    imageParamsCache[2] = ImageParams(
      width = layoutWidth / 2f,
      height = layoutHeight / 2f,
      x = layoutWidth / 2f,
      y = layoutHeight / 2f)

    syncLayoutWithParamCache()
  }

  // Do not change
  private fun prepareTouchListeners() {
    for (view in imageViews) view.setOnTouchListener(this)
  }


  // ------------------------ RESIZING HELPERS ------------------------

  private fun resizeImageAt(index: Int, deltaWidth: Float, deltaHeight: Float) {
    when (index) {
      0 -> resizeImage0(deltaHeight)
      1 -> resizeImage1(deltaWidth, deltaHeight)
      2 -> resizeImage2(deltaWidth, deltaHeight)
      else -> {
        Log.d(TAG, "resizeImage: invalid index")
        return
      }
    }
  }

  private fun resizeImage0(deltaHeight: Float) {
    val okToAdjustHeight = imageParamsCache[0].height + deltaHeight in minDimension..(layoutHeight - minDimension)

    when (touchedImageEdge) {
      null -> {
        Log.d(TAG, "resizeImage0: invalid edge")
      }
      Edge.BOTTOM_LEFT_CORNER, Edge.BOTTOM_SIDE, Edge.BOTTOM_RIGHT_CORNER -> {
        if (okToAdjustHeight) imageParamsCache[0].height += deltaHeight
      }
      else -> return
    }

    imageParamsCache[1].height = layoutHeight - imageParamsCache[0].height
    imageParamsCache[1].y = imageParamsCache[0].height

    imageParamsCache[2].height = imageParamsCache[1].height
    imageParamsCache[2].y = imageParamsCache[0].height

    syncLayoutWithParamCache()
  }

  private fun resizeImage1(deltaWidth: Float, deltaHeight: Float) {
    val okToAdjustWidth = imageParamsCache[1].width + deltaWidth in minDimension..(layoutWidth - minDimension)
    val okToAdjustHeight = imageParamsCache[1].height - deltaHeight in minDimension..(layoutHeight - minDimension)

    when (touchedImageEdge) {
      null -> {
        Log.d(TAG, "resizeImage1: invalid edge")
        return
      }
      Edge.TOP_RIGHT_CORNER -> {
        if (okToAdjustWidth) imageParamsCache[1].width += deltaWidth
        if (okToAdjustHeight) imageParamsCache[1].height -= deltaHeight
      }
      Edge.TOP_SIDE, Edge.TOP_LEFT_CORNER -> {
        if (okToAdjustHeight) imageParamsCache[1].height -= deltaHeight

      }
      Edge.RIGHT_SIDE, Edge.BOTTOM_RIGHT_CORNER -> {
        if (okToAdjustWidth) imageParamsCache[1].width += deltaWidth
      }
      else -> return
    }

    imageParamsCache[1].y = layoutHeight - imageParamsCache[1].height

    imageParamsCache[0].height = layoutHeight - imageParamsCache[1].height

    imageParamsCache[2].width = layoutWidth - imageParamsCache[1].width
    imageParamsCache[2].height = imageParamsCache[1].height
    imageParamsCache[2].x = imageParamsCache[1].width
    imageParamsCache[2].y = imageParamsCache[1].y

    syncLayoutWithParamCache()
  }

  private fun resizeImage2(deltaWidth: Float, deltaHeight: Float) {
    val okToAdjustWidth = imageParamsCache[2].width - deltaWidth in minDimension..(layoutWidth - minDimension)
    val okToAdjustHeight = imageParamsCache[2].height - deltaHeight in minDimension..(layoutHeight - minDimension)

    when (touchedImageEdge) {
      null -> {
        Log.d(TAG, "resizeImage2: invalid edge")
        return
      }
      Edge.TOP_SIDE, Edge.TOP_RIGHT_CORNER -> {
        if (okToAdjustHeight) imageParamsCache[2].height -= deltaHeight
      }
      Edge.TOP_LEFT_CORNER -> {
        if (okToAdjustWidth) imageParamsCache[2].width -= deltaWidth
        if (okToAdjustHeight) imageParamsCache[2].height -= deltaHeight
      }
      Edge.LEFT_SIDE, Edge.BOTTOM_LEFT_CORNER -> {
        if (okToAdjustWidth) imageParamsCache[2].width -= deltaWidth
      }
      else -> return
    }

    imageParamsCache[2].x = layoutWidth - imageParamsCache[2].width
    imageParamsCache[2].y = layoutHeight - imageParamsCache[2].height

    imageParamsCache[0].height = layoutHeight - imageParamsCache[2].height

    imageParamsCache[1].width = layoutWidth - imageParamsCache[2].width
    imageParamsCache[1].height = imageParamsCache[2].height
    imageParamsCache[1].y = imageParamsCache[2].y

    syncLayoutWithParamCache()
  }


  // ------------------------ TOUCH HELPER ------------------------

  // Do not change
  override fun onTouch(v: View, event: MotionEvent): Boolean {
    return onTouchHelper(v, event, ::resizeImageAt)
  }


  // ------------------------ HELPERS ------------------------

  companion object {
    private const val TAG = "CollageView3Image0"
  }
}