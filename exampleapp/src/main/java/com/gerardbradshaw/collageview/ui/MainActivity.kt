package com.gerardbradshaw.collageview.ui

import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import com.gerardbradshaw.collageview.R
import com.gerardbradshaw.library.collageview.AbstractCollageView
import com.gerardbradshaw.library.collageview.CollageView4Image4
import com.ortiz.touchview.TouchImageView

class MainActivity : AppCompatActivity(), View.OnClickListener {

  private lateinit var parentFrame: FrameLayout
  private lateinit var collageFrame: FrameLayout
  private lateinit var collage: AbstractCollageView
  private var lastImageClickedIndex: TouchImageView? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    initCollage()
  }

  private fun initCollage() {
    parentFrame = findViewById(R.id.parent_frame)
    collageFrame = findViewById(R.id.collage_frame)

    collageFrame.viewTreeObserver.addOnGlobalLayoutListener(
      object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
          if (collageFrame.height > 0) {
            addDefaultCollageToFrame()
            collageFrame.viewTreeObserver.removeOnGlobalLayoutListener(this)
          }
        }
      })
  }

  private fun addDefaultCollageToFrame() {
    collage = CollageView4Image4(this, null, collageFrame.width, collageFrame.height)
    collageFrame.addView(collage)
    collage.setImageClickListener(this)
  }

  override fun onClick(v: View) {
    if (v is TouchImageView) {
      lastImageClickedIndex = v
      Toast.makeText(this, "You clicked a view!", Toast.LENGTH_SHORT).show()
    }
  }
}