package com.upsellbackgroundactions

import android.graphics.Color
import android.os.Bundle
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.ReadableMap


class BackgroundTaskOptions {
  val extras: Bundle?

  constructor(extras: Bundle) {
    this.extras = extras
  }

  constructor(extras: ReadableMap) {
    this.extras=Arguments.toBundle(extras)
  }

  constructor(reactContext: ReactContext, options: ReadableMap) {
    // Create extras
    extras = Arguments.toBundle(options)
    requireNotNull(extras) { "Could not convert arguments to bundle" }
    // Get taskTitle
    try {
      requireNotNull(options.getString("taskTitle"))
    } catch (e: Exception) {
      throw IllegalArgumentException("Task title cannot be null")
    }
    // Get taskDesc
    try {
      requireNotNull(options.getString("taskDesc"))
    } catch (e: Exception) {
      throw IllegalArgumentException("Task description cannot be null")
    }
    // Get iconInt
    try {
      val iconMap = options.getMap("taskIcon")
      requireNotNull(iconMap)
      val iconName = iconMap.getString("name")
      val iconType = iconMap.getString("type")
      var iconPackage: String?
      try {
        iconPackage = iconMap.getString("package")
        requireNotNull(iconPackage)
      } catch (e: Exception) {
        // Get the current package as default
        iconPackage = reactContext.packageName
      }
      val iconInt = reactContext.resources.getIdentifier(iconName, iconType, iconPackage)
      extras.putInt("iconInt", iconInt)
      require(iconInt != 0)
    } catch (e: Exception) {
      throw IllegalArgumentException("Task icon not found")
    }
    // Get color
    try {
      val color = options.getString("color")
      extras.putInt("color", Color.parseColor(color))
    } catch (e: Exception) {
      extras.putInt("color", Color.parseColor("#ffffff"))
    }
  }

  val taskTitle: String
    get() = extras!!.getString("taskTitle", "")

  val taskDesc: String
    get() = extras!!.getString("taskDesc", "")

  @get:IdRes
  val iconInt: Int
    get() = extras!!.getInt("iconInt")

  @get:ColorInt
  val color: Int
    get() = extras!!.getInt("color")

  val linkingURI: String?
    get() = extras!!.getString("linkingURI")

  val progressBar: Bundle?
    get() = extras!!.getBundle("progressBar")
}

