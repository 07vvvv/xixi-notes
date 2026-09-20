package com.xixi.notes.ui.util

import com.xixi.notes.image.ImageManager

/** 相对路径 -> Coil 可加载的绝对路径（只做字符串拼接，无 IO） */
fun imageModel(absolutePathOrRelative: String, imageManager: ImageManager): String =
    imageManager.resolveFile(absolutePathOrRelative).absolutePath

/** 去掉扩展名的文件名，用于展示 */
fun fileNameOf(path: String): String = path.substringAfterLast('/')
