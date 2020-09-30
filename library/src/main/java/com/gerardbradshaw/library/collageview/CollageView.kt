package com.gerardbradshaw.library.collageview

import android.net.Uri

interface CollageView {

  fun imageCount(): Int

  fun setImageAt(index: Int, uri: Uri?)
}