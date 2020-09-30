package com.gerardbradshaw.library.collageview

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.gerardbradshaw.library.R
import com.gerardbradshaw.library.utils.ImageParams
import com.gerardbradshaw.library.utils.TaskRunner
import com.ortiz.touchview.TouchImageView
import java.util.*
import kotlin.math.roundToInt

/**
 * This class should be inherited in order to reduce code duplication in CollageView classes. Some
 * properties and functions are only used by the inheriting class. Note the inheriting class should
 * also implement [View.OnTouchListener] and call [onTouchHelper]. */
abstract class AbstractCollageView(context: Context,
                                   attrs: AttributeSet?,
                                   private val imageCount: Int,
                                   var layoutWidth: Int = 0,
                                   var layoutHeight: Int = 0,
                                   var isBorderEnabled: Boolean = false,
                                   protected var imageUris: Array<Uri?>? = null) :
  FrameLayout(context, attrs), CollageView {

  // ------------------------ COLLAGE VIEW INSTANCE PROPERTIES ------------------------

  /** True if the frame in the inheriting class has been inflated. */
  protected var isFrameInflated = false

  /** A list of TouchImageViews in the inheriting class. */
  protected val imageViews: List<TouchImageView> = List(imageCount) {
    TouchImageView(context, null)
  }

  /** The height, width, x and y positions of each image in the view. */
  protected val imageParamsCache = Array(imageCount) {
    ImageParams()
  }

  protected val minDimension = 100f


  // ------------------------ ABSTRACT COLLAGE VIEW PROPERTIES ------------------------

  protected var initialLayoutWidth: Int = 0
  protected var initialLayoutHeight: Int = 0

  /** True if an ACTION_DOWN motion event is detected without an ACTION_MOVE or ACTION_UP within the
  time specified by LONG_CLICK_DURATION. */
  private var isLongClickActive = false

  /** The time at which the last ACTION_DOWN motion event was detected. */
  private var touchStartTime: Long? = null
  
  /** The raw x and y coordinates of the last ACTION_DOWN motion event */
  private var initialTouchRawX = 0f
  private var initialTouchRawY = 0f

  /** The raw x and y coordinates of the last motion event. */
  private var touchRawX = 0f
  private var touchRawY = 0f

  /** The index and [Edge] of the image touched during the last ACTION_DOWN touch event. */
  private var touchedImageIndex = -1
  protected var touchedImageEdge: Edge? = null

  /** The class listening for clicks on TouchImageViews */
  private var clickListener: OnClickListener? = null

  /** The TaskRunner used to ensure the previous resize event has finished before the next is
   started. */
  private val resizeTaskRunner = TaskRunner()


  // ------------------------ INITIALIZATION ------------------------

  init {
    initialLayoutWidth = layoutWidth
    initialLayoutHeight = layoutHeight

    initSize(layoutWidth, layoutHeight)
  }


  // ------------------------ PUBLIC INSTANCE METHODS ------------------------

  /** Returns the number if TouchImageViews in the View. */
  override fun imageCount() = imageCount

  /** Sets the image at child [index] to the image at [uri]. If Uri is invalid, the default image is
   loaded. */
  override fun setImageAt(index: Int, uri: Uri?) {
    // TODO use URI
    if (index < imageViews.size) {
      Glide
        .with(context)
        .load(R.drawable.rectangle_background)
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(imageViews[index])
      imageViews[index].scaleType = ImageView.ScaleType.CENTER_CROP
    }
    else Log.d(TAG, "setImageAt: invalid index")
  }

  /** Resize the View to the specified dimensions. */
  fun resize(newWidth: Int, newHeight: Int) {
    if (layoutParams == null) {
      layoutParams = LayoutParams(newWidth, newHeight)
    }
    else {
      layoutParams.width = newWidth
      layoutParams.height = newHeight
    }

    val xScale = newWidth.toFloat() / initialLayoutWidth.toFloat()
    val yScale = newHeight.toFloat() / initialLayoutHeight.toFloat()

    layoutWidth = newWidth
    layoutHeight = newHeight

    scaleViews(xScale, yScale)
  }

  /** Enables the border if [enableBorder] is true, otherwise the border is disabled. */
  @SuppressLint("NewApi") // bug
  fun enableBorder(enableBorder: Boolean) {
    isBorderEnabled = enableBorder

    if (isBorderEnabled) {
      foreground = ContextCompat.getDrawable(context,
        R.drawable.border_frame
      )

      for (image in imageViews) {
        image.foreground = ContextCompat.getDrawable(context,
          R.drawable.border_image
        )
      }
    }
    else {
      foreground = null

      for (image in imageViews) {
        image.foreground = null
      }
    }
  }

  /** Toggles the border on or off depending on its current state. */
  fun toggleBorder() {
    isBorderEnabled = !isBorderEnabled
    enableBorder(isBorderEnabled)
  }

  /** Sets the listener to be notified of image clicks. The View given in the OnClick() interface
   is the TouchImageView clicked and not this (parent) CollageView. */
  fun setImageClickListener(listener: OnClickListener) {
    clickListener = listener
    for (image in imageViews) image.setOnClickListener(listener)
  }


  // ------------------------ COLLAGE VIEW INSTANCE METHODS ------------------------

  protected fun syncLayoutWithParamCache() {
    for (i in imageViews.indices) {
      if (!imageParamsCache[i].synced) {
        syncImageSizeWithCacheAt(i)
        syncImageCoordinatesWithCacheAt(i)
        imageParamsCache[i].synced = true
      }
    }
  }

  /** Adds empty TouchImageViews to the layout. */
  protected fun addViewsToLayout() {
    for (image in imageViews) {
      addView(image)
    }
  }

  /** Adds images from [imageUris] to the TouchImageViews in the layout. */
  protected fun addImagesToViews() {
    for (i in 0 until imageCount) {
      setImageAt(i, imageUris?.get(i))
    }
  }

  /** Helper for the inheriting class' onTouch() method. This reduced the need to duplicate the
  below code in each class inheriting from AbstractCollageView. */
  protected fun onTouchHelper(view: View,
                              event: MotionEvent,
                              resizeImage: (index: Int, deltaX: Float, deltaY: Float) -> Unit): Boolean {
    
    return when (event.action) {
      MotionEvent.ACTION_DOWN -> {
        saveTouchEventProperties(view, event)
        if (touchedImageIndex == -1) touchStartTime = null
        super.onTouchEvent(event)
      }

      MotionEvent.ACTION_MOVE -> {
        if (!isLongClickActive) {
          if (touchStartTime == null) return super.onTouchEvent(event)

          val isLongClick =
            Calendar.getInstance().timeInMillis - touchStartTime!! > LONG_CLICK_DURATION

          if (!isLongClick) {
            val isPseudoMove = event.rawX == initialTouchRawX && event.rawY == initialTouchRawY

            return if (!isPseudoMove) {
              resetTouchEventProperties()
              super.onTouchEvent(event)
            }
            else false
          }
          isLongClickActive = true
        }

        addResizeRunnableToQueueAndRun(event, touchRawX, touchRawY, resizeImage)
        touchRawX = event.rawX
        touchRawY = event.rawY
        true
      }

      MotionEvent.ACTION_UP -> {
        if (!isLongClickActive) {
          resetTouchEventProperties()
          return super.onTouchEvent(event)
        }

        addResizeRunnableToQueueAndRun(event, touchRawX, touchRawY, resizeImage)
        resetTouchEventProperties()
        true
      }

      else -> super.onTouchEvent(event)
    }
  }


  // ------------------------ ABSTRACT COLLAGE VIEW METHODS ------------------------

  private fun initSize(width: Int, height: Int) {
    if (layoutParams == null) {
      layoutParams = LayoutParams(width, height)
    }
    else {
      layoutParams.width = width
      layoutParams.height = height
    }
  }

  private fun syncImageSizeWithCacheAt(index: Int) {
    if (index < 0 || index >= imageCount) return

    val image = imageViews[index]

    val params = image.layoutParams ?: LayoutParams(
      imageParamsCache[index].width.roundToInt(), imageParamsCache[index].height.roundToInt())

    if (imageParamsCache[index].width != 0f) {
      params.width = imageParamsCache[index].width.roundToInt()
    }

    if (imageParamsCache[index].height != 0f) {
      params.height = imageParamsCache[index].height.roundToInt()
    }

    image.layoutParams = params
    image.requestLayout()
  }

  private fun syncImageCoordinatesWithCacheAt(index: Int) {
    if (index < 0 || index >= imageCount) return

    val image = imageViews[index]

    image.x = imageParamsCache[index].x.roundToInt().toFloat()
    image.y = imageParamsCache[index].y.roundToInt().toFloat()
  }

  /** Returns an [Edge] representing the edge or corner at the coordinates (touchX, touchY) in the
   image at index touchedImageIndex in [imageViews]. */
  private fun getTouchedImageEdge(touchX: Float, touchY: Float): Edge? {
    if (touchedImageIndex == -1 || touchedImageIndex >= imageCount) {
      Log.d(TAG, "determineEdge: invalid image index")
      return null
    }

    val percentageAcrossImage = touchX / imageParamsCache[touchedImageIndex].width
    val percentageDownImage = touchY / imageParamsCache[touchedImageIndex].height
    
    return when {
      percentageAcrossImage < 0.25 -> {
        when {
          percentageDownImage < 0.25 -> Edge.TOP_LEFT_CORNER
          percentageDownImage < 0.75 -> Edge.LEFT_SIDE
          else -> Edge.BOTTOM_LEFT_CORNER
        }
      }
      percentageAcrossImage < 0.75 -> {
        when {
          percentageDownImage < 0.25 -> Edge.TOP_SIDE
          percentageDownImage > 0.75 -> Edge.BOTTOM_SIDE
          else -> null
        }
      }
      percentageAcrossImage > 0.75 -> {
        when {
          percentageDownImage < 0.25 -> Edge.TOP_RIGHT_CORNER
          percentageDownImage < 0.75 -> Edge.RIGHT_SIDE
          else -> Edge.BOTTOM_RIGHT_CORNER
        }
      }
      else -> {
        Log.d(TAG, "determineEdge: something went wrong")
        null
      }
    }
  }

  private fun saveTouchEventProperties(view: View, event: MotionEvent) {
    isLongClickActive = false
    touchStartTime = Calendar.getInstance().timeInMillis

    initialTouchRawX = event.rawX
    initialTouchRawY = event.rawY
    touchRawX = initialTouchRawX
    touchRawY = initialTouchRawY

    touchedImageIndex = imageViews.indexOf(view)
    touchedImageEdge = getTouchedImageEdge(event.x, event.y)
  }

  private fun resetTouchEventProperties() {
    isLongClickActive = false
    touchStartTime = null

    initialTouchRawX = 0f
    initialTouchRawY = 0f
    touchRawX = 0f
    touchRawY = 0f

    touchedImageEdge = null
    touchedImageIndex = -1
  }

  private fun scaleViews(xScale: Float, yScale: Float) {
    for (i in imageParamsCache.indices) {
      imageParamsCache[i].width *= xScale
      imageParamsCache[i].height *= yScale
      imageParamsCache[i].x *= xScale
      imageParamsCache[i].y *= yScale
    }
    syncLayoutWithParamCache()
  }

  private fun addResizeRunnableToQueueAndRun(
    event: MotionEvent, touchRawX: Float, touchRawY: Float,
    resizeImage: (index: Int, deltaX: Float, deltaY: Float) -> Unit) {

    resizeTaskRunner.addNewTask(Runnable {
      val deltaX = event.rawX - touchRawX
      val deltaY = event.rawY - touchRawY

      resizeImage(touchedImageIndex, deltaX, deltaY)

      resizeTaskRunner.setTaskFinished()
    })
  }


  // ------------------------ HELPERS ------------------------

  /** Enum class used to determine which [Edge] of the image  was touched */
  protected enum class Edge {
    TOP_LEFT_CORNER,
    TOP_RIGHT_CORNER,
    BOTTOM_LEFT_CORNER,
    BOTTOM_RIGHT_CORNER,
    TOP_SIDE,
    BOTTOM_SIDE,
    LEFT_SIDE,
    RIGHT_SIDE
  }

  companion object {
    private const val TAG = "AbstractCollageView"
    private const val LONG_CLICK_DURATION = 0L
  }
}