package com.xixi.notes.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 本地图片管理。
 *
 * 目录约定：
 * - 临时：`filesDir/images/temp/{uuid}.jpg`（选择图片、拍照后压缩产物）
 * - 正式：`filesDir/images/{taskId}/{uuid}.jpg`
 *
 * 所有方法都是 suspend + Dispatchers.IO，返回 Result，不抛异常。
 * 只保存相对路径，绝不保存绝对路径。
 */
class ImageManager(private val context: Context) {

    companion object {
        /** 压缩后最大边长 */
        const val MAX_EDGE = 1920

        /** JPEG 压缩质量 */
        const val JPEG_QUALITY = 85

        /** 单张图片压缩超时时间 */
        const val COMPRESS_TIMEOUT_MS = 30_000L

        const val DIR_IMAGES = "images"
        const val DIR_TEMP = "temp"
        const val PREFIX_TEMP = "tmp_"
        const val PREFIX_CAMERA = "camera_"
    }

    // ---------------------------------------------------------------- 目录

    fun imagesRoot(): File = File(context.filesDir, DIR_IMAGES)

    fun tempDir(): File = File(imagesRoot(), DIR_TEMP)

    fun taskDir(taskId: Long): File = File(imagesRoot(), taskId.toString())

    // ---------------------------------------------------------------- 路径

    /**
     * 相对路径 -> File。只拼路径，不做 IO。
     * 兼容三种形式："images/12/x.jpg"、"12/x.jpg"、"x.jpg"（裸文件名会在 images/ 下查找）。
     */
    fun resolveFile(path: String): File {
        if (path.isBlank()) return File(imagesRoot(), path)
        return when {
            path.startsWith("$DIR_IMAGES/") -> File(context.filesDir, path)
            path.contains('/') -> File(imagesRoot(), path)
            else -> File(imagesRoot(), path)
        }
    }

    /** 生成正式图片的相对路径（仅字符串拼接，不做 IO） */
    fun relativePathFor(taskId: Long, fileName: String): String = "$DIR_IMAGES/$taskId/$fileName"

    // ---------------------------------------------------------------- 压缩入库

    /**
     * 相册/图片 URI -> 压缩到临时目录。
     *
     * 流程：URI 复制到临时文件 -> 从 File 读 EXIF -> 按方向旋转 -> 压缩 -> 写入 temp/{uuid}.jpg。
     * 复制到临时文件是必须的：ExifInterface 需要可随机访问的文件，URI 流不可靠。
     */
    suspend fun compressAndSaveToTemp(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        var copied: File? = null
        try {
            val copyResult = copyUriToTemp(uri)
            val source = copyResult.getOrElse { return@withContext Result.failure(it) }
            copied = source
            val output = nextTempOutput()
            val relative = compressFile(source, output)
            Result.success(relative)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            // 中间拷贝文件用完即删（外部传入的相机文件不在此处删除）
            copied?.let { if (it.name.startsWith(PREFIX_TEMP)) it.delete() }
        }
    }

    /**
     * 文件 -> 压缩到临时目录（相机拍照后调用，源文件由调用方负责清理）。
     */
    suspend fun compressAndSaveToTemp(sourceFile: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists() || sourceFile.length() == 0L) {
                return@withContext Result.failure(IllegalStateException("源文件不存在或为空"))
            }
            val output = nextTempOutput()
            val relative = compressFile(sourceFile, output)
            Result.success(relative)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 在 temp/ 下创建拍照用的空文件，返回 File（配合 FileProvider 生成 content:// URI） */
    fun createCameraTempFile(): File {
        val dir = tempDir()
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "$PREFIX_CAMERA${UUID.randomUUID()}.jpg")
    }

    /**
     * 把临时图片移动到任务目录，返回正式相对路径。
     * 已移动过的文件会抛异常，由调用方记录用于回滚。
     */
    suspend fun moveToTask(tempPath: String, taskId: Long): String = withContext(Dispatchers.IO) {
        val source = resolveFile(tempPath)
        if (!source.exists()) {
            throw IllegalStateException("临时图片不存在：$tempPath")
        }
        val dir = taskDir(taskId)
        if (!dir.exists()) dir.mkdirs()
        val target = File(dir, source.name)
        val moved = source.renameTo(target) || run {
            // renameTo 跨挂载点可能失败，退化为复制 + 删除
            source.copyTo(target, overwrite = true)
            source.delete()
            target.exists()
        }
        if (!moved) throw IllegalStateException("图片移动失败：$tempPath")
        relativePathFor(taskId, target.name)
    }

    /**
     * 按给定顺序把多张临时图片移动到任务目录，返回正式相对路径列表。
     * 任意一张失败时抛出异常，调用方负责回滚已移动的文件与数据库记录。
     */
    suspend fun moveTempImages(tempPaths: List<String>, taskId: Long): List<String> =
        withContext(Dispatchers.IO) {
            tempPaths.map { moveToTask(it, taskId) }
        }

    /**
     * 清空临时目录（首次启动时调用）。
     * 会先尝试删除 images/ 下遗留的空任务目录。
     */
    suspend fun cleanTempDir(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            tempDir().listFiles()?.forEach { it.delete() }
            imagesRoot().listFiles()?.forEach { file ->
                if (file.isDirectory && file.name != DIR_TEMP && file.listFiles().isNullOrEmpty()) {
                    file.delete()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------------------------------------------------------------- 删除

    /** 删除单张图片（相对路径或绝对路径均可） */
    suspend fun deleteImage(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val direct = resolveFile(path)
            if (direct.exists() && direct.isFile) {
                direct.delete()
            } else {
                // 裸文件名或路径不完整时，在 images/ 下查找同名文件
                val name = path.substringAfterLast('/')
                findByName(name)?.delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 删除某个任务的全部图片（整个文件夹） */
    suspend fun deleteTaskImages(taskId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dir = taskDir(taskId)
            if (dir.exists()) dir.deleteRecursively()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 删除一个临时文件（取消编辑、压缩失败、拍照失败时使用） */
    suspend fun deleteTempFile(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = resolveFile(path)
            if (file.exists()) file.delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun findByName(name: String): File? {
        val root = imagesRoot()
        if (!root.exists()) return null
        return root.walkTopDown().firstOrNull { it.isFile && it.name == name }
    }

    // ---------------------------------------------------------------- 私有

    private fun nextTempOutput(): File {
        val dir = tempDir()
        if (!dir.exists()) dir.mkdirs()
        // 压缩产物统一为 JPEG
        return File(dir, "${UUID.randomUUID()}.jpg")
    }

    /** 复制 URI 到临时文件（保留扩展名提示格式） */
    private fun copyUriToTemp(uri: Uri): Result<File> {
        return try {
            val dir = tempDir()
            if (!dir.exists()) dir.mkdirs()
            val nameHint = uri.lastPathSegment ?: ""
            val ext = nameHint.substringAfterLast('.', "jpg").ifBlank { "jpg" }
            val temp = File(dir, "$PREFIX_TEMP${UUID.randomUUID()}.$ext")
            val input = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(IllegalStateException("无法打开图片：$uri"))
            input.use { source ->
                FileOutputStream(temp).use { output ->
                    source.copyTo(output)
                }
            }
            if (temp.length() == 0L) {
                temp.delete()
                return Result.failure(IllegalStateException("图片内容为空"))
            }
            Result.success(temp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 压缩 core：
     * 1. 两阶段解码（先 inJustDecodeBounds 读尺寸，再算 inSampleSize 解码），防 OOM
     * 2. 读 EXIF 方向并旋转，旋转 90/270 度后宽高互换
     * 3. 目标最大边 = min(旋转后最大边, 1920)，不放大原图
     * 4. JPEG 85% 写入目标文件，压缩后立即 recycle
     * 5. 把目标文件 EXIF 方向写为 NORMAL
     */
    private fun compressFile(source: File, destination: File): String {
        val orientation = readOrientation(source)

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalStateException("无法解析图片尺寸")
        }

        // 旋转 90/270 度后宽高互换，按互换后的尺寸做采样与缩放计算
        val swapped = orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
            orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
            orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
            orientation == ExifInterface.ORIENTATION_TRANSVERSE
        val sourceWidth = if (swapped) bounds.outHeight else bounds.outWidth
        val sourceHeight = if (swapped) bounds.outWidth else bounds.outHeight
        val sourceMaxEdge = max(sourceWidth, sourceHeight)

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(sourceMaxEdge, MAX_EDGE)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeFile(source.absolutePath, decodeOptions)
            ?: throw IllegalStateException("图片解码失败")

        var bitmap: Bitmap = decoded
        try {
            // 按 EXIF 方向旋转（可能回收 decoded，返回值才是有效位图）
            bitmap = applyOrientation(decoded, orientation)

            // 压缩不放大原图：目标最大边 = min(当前最大边, 1920)
            val currentMax = max(bitmap.width, bitmap.height)
            val targetMax = if (currentMax <= MAX_EDGE) currentMax else MAX_EDGE
            if (targetMax != currentMax) {
                val scale = targetMax.toFloat() / currentMax.toFloat()
                val targetWidth = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
                val targetHeight = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
                if (scaled !== bitmap) {
                    bitmap.recycle()
                    bitmap = scaled
                }
            }

            val parent = destination.parentFile
            if (parent != null && !parent.exists()) parent.mkdirs()
            FileOutputStream(destination).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
                output.flush()
            }
        } finally {
            // 压缩后立即回收，防 OOM
            if (!bitmap.isRecycled) bitmap.recycle()
            if (decoded !== bitmap && !decoded.isRecycled) decoded.recycle()
        }

        // 像素已按方向旋转，重置 EXIF 方向避免二次旋转
        try {
            val exif = ExifInterface(destination.absolutePath)
            exif.setAttribute(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL.toString()
            )
            exif.saveAttributes()
        } catch (e: Exception) {
            // EXIF 写入失败不影响图片可用性
        }

        return "$DIR_TEMP/${destination.name}"
    }

    private fun readOrientation(file: File): Int = try {
        val exif = ExifInterface(file.absolutePath)
        exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    } catch (e: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }

    /** 计算采样率：保证解码后的最大边不小于目标最大边的一半 */
    private fun calculateInSampleSize(sourceMaxEdge: Int, targetMaxEdge: Int): Int {
        var sampleSize = 1
        if (targetMaxEdge <= 0) return sampleSize
        while (sourceMaxEdge / (sampleSize * 2) >= targetMaxEdge) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return try {
            val transformed = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
            if (transformed !== bitmap) {
                bitmap.recycle()
            }
            transformed
        } catch (e: Exception) {
            bitmap
        }
    }
}
