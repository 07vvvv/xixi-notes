package com.xixi.notes.image

/** 图片查看页参数：路径列表 + 初始页 */
data class ImageViewerArgs(
    val paths: List<String>,
    val initialIndex: Int
)

/**
 * 编辑页与图片查看页之间的传参桥。
 *
 * 路径列表含 "/" 不适合放进导航路由，因此由桥持有，路由只带初始页下标。
 */
class ImageViewerBridge {

    var args: ImageViewerArgs? = null
        private set

    /** 打开查看页：写入参数并返回初始页下标（可直接用于路由） */
    fun open(paths: List<String>, initialIndex: Int): Int {
        val safeIndex = initialIndex.coerceIn(0, (paths.size - 1).coerceAtLeast(0))
        args = ImageViewerArgs(paths = paths, initialIndex = safeIndex)
        return safeIndex
    }

    /** 读取参数并消费（避免下次进入复用旧图片） */
    fun consume(): ImageViewerArgs? {
        val current = args
        args = null
        return current
    }

    fun clear() {
        args = null
    }
}
