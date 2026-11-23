package com.lockcanvas.app.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import android.util.LruCache
import java.util.Locale
import com.lockcanvas.app.model.AnimationStyle
import com.lockcanvas.app.model.BatteryGaugeElement
import com.lockcanvas.app.model.BatteryGaugeStyle
import com.lockcanvas.app.model.CanvasDesign
import com.lockcanvas.app.model.CanvasElement
import com.lockcanvas.app.model.DynamicDataSnapshot
import com.lockcanvas.app.model.ImageElement
import com.lockcanvas.app.model.ShapeElement
import com.lockcanvas.app.model.ShapeType
import com.lockcanvas.app.model.TextElement
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class CanvasSceneRenderer(
    private val context: Context? = null,
) {
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fxPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#64D2FF")
        pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
    }
    private val guideFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(46, 255, 255, 255)
    }
    private val guideLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(140, 148, 163, 184)
        pathEffect = DashPathEffect(floatArrayOf(10f, 8f), 0f)
    }
    private val guideTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(190, 226, 232, 240)
        textAlign = Paint.Align.CENTER
    }

    private val workRect = RectF()
    private val workClipPath = Path()
    private val textBounds = Rect()

    private val bitmapCache = object : LruCache<String, Bitmap>(8) {}

    fun drawScene(
        canvas: Canvas,
        width: Int,
        height: Int,
        design: CanvasDesign,
        snapshot: DynamicDataSnapshot,
        animationTimeSec: Float = 0f,
        selectedElementId: String? = null,
        showSafeAreaGuides: Boolean = false,
    ) {
        if (width <= 0 || height <= 0) return

        val unitScale = (width / 390f).coerceAtLeast(0.25f)
        val effective = RuleEvaluator.evaluate(design, snapshot).design

        drawBackground(canvas, width, height, effective)
        drawAnimationOverlay(canvas, width, height, effective, animationTimeSec)

        val sortedElements = effective.elements
            .filter { it.visible }
            .sortedBy { it.zIndex }

        for (element in sortedElements) {
            canvas.save()
            val cx = element.xFraction * width
            val cy = element.yFraction * height
            canvas.translate(cx, cy)
            if (element.rotationDegrees != 0f) {
                canvas.rotate(element.rotationDegrees)
            }
            if (element.scale != 1f) {
                canvas.scale(element.scale, element.scale)
            }

            val elementBounds = when (element) {
                is ShapeElement -> drawShape(canvas, width, height, unitScale, element)
                is TextElement -> drawText(canvas, unitScale, element, snapshot)
                is BatteryGaugeElement -> drawBatteryGauge(canvas, width, height, unitScale, element, snapshot)
                is ImageElement -> drawImage(canvas, width, height, unitScale, element)
            }

            if (selectedElementId != null && element.id == selectedElementId) {
                drawSelectionBox(canvas, elementBounds, unitScale)
            }
            canvas.restore()
        }

        if (showSafeAreaGuides) {
            drawSafeAreaGuides(canvas, width, height, unitScale)
        }
    }

    fun hitTestElement(
        touchX: Float,
        touchY: Float,
        width: Int,
        height: Int,
        design: CanvasDesign,
        snapshot: DynamicDataSnapshot,
    ): CanvasElement? {
        if (width <= 0 || height <= 0) return null
        val unitScale = (width / 390f).coerceAtLeast(0.25f)
        val effective = RuleEvaluator.evaluate(design, snapshot).design
        val sortedDesc = effective.elements
            .filter { it.visible }
            .sortedByDescending { it.zIndex }

        val minHitHalf = 22f * unitScale
        for (element in sortedDesc) {
            val cx = element.xFraction * width
            val cy = element.yFraction * height
            val dx = touchX - cx
            val dy = touchY - cy
            val rad = -element.rotationDegrees * (PI / 180.0)
            val cosR = cos(rad).toFloat()
            val sinR = sin(rad).toFloat()
            val scale = element.scale.coerceAtLeast(0.1f)
            val localX = (dx * cosR - dy * sinR) / scale
            val localY = (dx * sinR + dy * cosR) / scale

            val bounds = measureElementLocalBounds(element, width, height, unitScale, snapshot)
            val halfW = maxOf(bounds.width() / 2f, minHitHalf)
            val halfH = maxOf(bounds.height() / 2f, minHitHalf)
            if (localX in -halfW..halfW && localY in -halfH..halfH) {
                return element
            }
        }
        return null
    }

    fun clearImageCache() {
        bitmapCache.evictAll()
    }

    private fun drawBackground(
        canvas: Canvas,
        width: Int,
        height: Int,
        design: CanvasDesign,
    ) {
        val primary = design.backgroundColorArgb.toInt()
        if (design.backgroundGradientEnabled) {
            val secondary = design.secondaryBackgroundColorArgb.toInt()
            bgPaint.shader = LinearGradient(
                0f,
                0f,
                0f,
                height.toFloat(),
                primary,
                secondary,
                Shader.TileMode.CLAMP,
            )
        } else {
            bgPaint.shader = null
            bgPaint.color = primary
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
    }

    private fun drawAnimationOverlay(
        canvas: Canvas,
        width: Int,
        height: Int,
        design: CanvasDesign,
        animationTimeSec: Float,
    ) {
        when (design.animationStyle) {
            AnimationStyle.NONE -> return
            AnimationStyle.PULSE_GLOW -> {
                val pulse = 0.5f + 0.5f * sin(animationTimeSec * 1.15f)
                val radius = min(width, height) * (0.42f + 0.08f * pulse)
                val alpha = (26 + (28 * pulse).toInt()).coerceIn(0, 255)
                val centerColor = Color.argb(alpha, 100, 210, 255)
                val edgeColor = Color.argb(0, 100, 210, 255)
                fxPaint.shader = RadialGradient(
                    width * 0.5f,
                    height * 0.52f,
                    radius.coerceAtLeast(1f),
                    centerColor,
                    edgeColor,
                    Shader.TileMode.CLAMP,
                )
                canvas.drawCircle(width * 0.5f, height * 0.52f, radius, fxPaint)
                fxPaint.shader = null
            }
            AnimationStyle.FLOATING_PARTICLES -> {
                fxPaint.shader = null
                val particleCount = 18
                for (i in 0 until particleCount) {
                    val seed = (i + 1) * 1.6180339f
                    val baseX = ((seed * 97.3f) % 1f)
                    val baseY = ((seed * 53.7f) % 1f)
                    val speed = 0.015f + (i % 5) * 0.006f
                    val driftX = sin(animationTimeSec * 0.6f + seed * 6.28f) * 0.025f
                    val y = (baseY - animationTimeSec * speed) % 1f
                    val normalizedY = if (y < 0f) y + 1f else y
                    val normalizedX = (baseX + driftX).coerceIn(0.02f, 0.98f)
                    val radius = (2.2f + (i % 4) * 1.4f) * (width / 390f).coerceAtLeast(0.5f)
                    val twinkle = 0.45f + 0.55f * sin(animationTimeSec * 1.4f + i * 0.9f)
                    val alpha = (28 + (56 * twinkle).toInt()).coerceIn(10, 120)
                    fxPaint.color = if (i % 2 == 0) {
                        Color.argb(alpha, 100, 210, 255)
                    } else {
                        Color.argb(alpha, 192, 132, 252)
                    }
                    canvas.drawCircle(normalizedX * width, normalizedY * height, radius, fxPaint)
                }
            }
        }
    }

    private fun drawShape(
        canvas: Canvas,
        width: Int,
        height: Int,
        unitScale: Float,
        element: ShapeElement,
    ): RectF {
        val w = (element.widthFraction * width).coerceAtLeast(8f)
        val h = (element.heightFraction * height).coerceAtLeast(8f)
        val alphaFactor = element.alpha.coerceIn(0f, 1f)

        fillPaint.color = applyAlpha(element.fillColorArgb.toInt(), alphaFactor)
        strokePaint.color = applyAlpha(element.strokeColorArgb.toInt(), alphaFactor)
        strokePaint.strokeWidth = (element.strokeWidthDp * unitScale).coerceAtLeast(0f)

        return when (element.shapeType) {
            ShapeType.CIRCLE -> {
                val radius = min(w, h) / 2f
                canvas.drawCircle(0f, 0f, radius, fillPaint)
                if (strokePaint.strokeWidth > 0f && Color.alpha(strokePaint.color) > 0) {
                    canvas.drawCircle(0f, 0f, radius, strokePaint)
                }
                RectF(-radius, -radius, radius, radius)
            }
            ShapeType.PILL -> {
                workRect.set(-w / 2f, -h / 2f, w / 2f, h / 2f)
                val radius = min(w, h) / 2f
                canvas.drawRoundRect(workRect, radius, radius, fillPaint)
                if (strokePaint.strokeWidth > 0f && Color.alpha(strokePaint.color) > 0) {
                    canvas.drawRoundRect(workRect, radius, radius, strokePaint)
                }
                RectF(workRect)
            }
            ShapeType.RECTANGLE -> {
                workRect.set(-w / 2f, -h / 2f, w / 2f, h / 2f)
                val radius = (element.cornerRadiusDp * unitScale).coerceAtLeast(0f)
                canvas.drawRoundRect(workRect, radius, radius, fillPaint)
                if (strokePaint.strokeWidth > 0f && Color.alpha(strokePaint.color) > 0) {
                    canvas.drawRoundRect(workRect, radius, radius, strokePaint)
                }
                RectF(workRect)
            }
        }
    }

    private fun drawText(
        canvas: Canvas,
        unitScale: Float,
        element: TextElement,
        snapshot: DynamicDataSnapshot,
    ): RectF {
        val resolvedText = if (element.dynamicTemplate) {
            TemplateFormatter.format(element.text, snapshot)
        } else {
            element.text
        }
        val lines = resolvedText.split('\n')

        textPaint.color = applyAlpha(element.colorArgb.toInt(), element.alpha.coerceIn(0f, 1f))
        textPaint.textSize = (element.fontSizeSp * unitScale).coerceAtLeast(6f)
        textPaint.typeface = if (element.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        textPaint.setShadowLayer(
            4f * unitScale,
            0f,
            1.5f * unitScale,
            Color.argb((110 * element.alpha.coerceIn(0f, 1f)).toInt(), 0, 0, 0),
        )

        val fm = textPaint.fontMetrics
        val lineHeight = (fm.descent - fm.ascent) * 1.15f
        val totalHeight = lineHeight * lines.size
        var maxLineWidth = 0f

        var baselineY = -totalHeight / 2f - fm.ascent
        for (line in lines) {
            val lw = textPaint.measureText(line)
            if (lw > maxLineWidth) maxLineWidth = lw
            canvas.drawText(line, 0f, baselineY, textPaint)
            baselineY += lineHeight
        }
        textPaint.clearShadowLayer()

        val halfW = (maxLineWidth / 2f).coerceAtLeast(16f * unitScale)
        val halfH = (totalHeight / 2f).coerceAtLeast(12f * unitScale)
        return RectF(-halfW, -halfH, halfW, halfH)
    }

    private fun drawBatteryGauge(
        canvas: Canvas,
        width: Int,
        height: Int,
        unitScale: Float,
        element: BatteryGaugeElement,
        snapshot: DynamicDataSnapshot,
    ): RectF {
        val w = (element.widthFraction * width).coerceAtLeast(24f)
        val h = (element.heightFraction * height).coerceAtLeast(12f)
        val alphaFactor = element.alpha.coerceIn(0f, 1f)
        val fraction = (snapshot.batteryPercent.coerceIn(0, 100) / 100f)

        return when (element.style) {
            BatteryGaugeStyle.BAR -> {
                workRect.set(-w / 2f, -h / 2f, w / 2f, h / 2f)
                val radius = h / 2f
                fillPaint.color = applyAlpha(element.trackColorArgb.toInt(), alphaFactor)
                canvas.drawRoundRect(workRect, radius, radius, fillPaint)

                val activeW = (w * fraction).coerceAtLeast(if (fraction > 0f) h else 0f)
                if (activeW > 0f) {
                    val activeRect = RectF(-w / 2f, -h / 2f, -w / 2f + activeW, h / 2f)
                    fillPaint.color = applyAlpha(element.activeColorArgb.toInt(), alphaFactor)
                    canvas.drawRoundRect(activeRect, radius, radius, fillPaint)
                }

                if (element.showLabel) {
                    val label = "${snapshot.batteryPercent}%"
                    textPaint.color = applyAlpha(element.textColorArgb.toInt(), alphaFactor)
                    textPaint.textSize = (h * 0.62f).coerceIn(10f * unitScale, 18f * unitScale)
                    textPaint.typeface = Typeface.DEFAULT_BOLD
                    textPaint.getTextBounds(label, 0, label.length, textBounds)
                    canvas.drawText(label, 0f, textBounds.height() / 2f, textPaint)
                }
                RectF(-w / 2f, -h / 2f, w / 2f, h / 2f)
            }
            BatteryGaugeStyle.CIRCULAR -> {
                val diameter = min(w, h)
                val radius = diameter / 2f
                val ringWidth = (diameter * 0.10f).coerceIn(4f * unitScale, 14f * unitScale)
                val arcRadius = (radius - ringWidth / 2f).coerceAtLeast(4f)
                workRect.set(-arcRadius, -arcRadius, arcRadius, arcRadius)

                strokePaint.strokeWidth = ringWidth
                strokePaint.strokeCap = Paint.Cap.ROUND
                strokePaint.color = applyAlpha(element.trackColorArgb.toInt(), alphaFactor)
                canvas.drawArc(workRect, 0f, 360f, false, strokePaint)

                strokePaint.color = applyAlpha(element.activeColorArgb.toInt(), alphaFactor)
                canvas.drawArc(workRect, -90f, 360f * fraction, false, strokePaint)

                if (element.showLabel) {
                    val label = "${snapshot.batteryPercent}%"
                    textPaint.color = applyAlpha(element.textColorArgb.toInt(), alphaFactor)
                    textPaint.textSize = (diameter * 0.24f).coerceAtLeast(12f * unitScale)
                    textPaint.typeface = Typeface.DEFAULT_BOLD
                    textPaint.getTextBounds(label, 0, label.length, textBounds)
                    canvas.drawText(label, 0f, textBounds.height() / 2f, textPaint)
                }
                RectF(-radius, -radius, radius, radius)
            }
        }
    }

    private fun drawImage(
        canvas: Canvas,
        width: Int,
        height: Int,
        unitScale: Float,
        element: ImageElement,
    ): RectF {
        val w = (element.widthFraction * width).coerceAtLeast(24f)
        val h = (element.heightFraction * height).coerceAtLeast(24f)
        val radius = (element.cornerRadiusDp * unitScale).coerceAtLeast(0f)
        val bounds = RectF(-w / 2f, -h / 2f, w / 2f, h / 2f)
        val alphaFactor = element.alpha.coerceIn(0f, 1f)

        val bitmap = loadBitmapSafely(element.uriString)
        if (bitmap != null && !bitmap.isRecycled) {
            canvas.save()
            workClipPath.reset()
            workClipPath.addRoundRect(bounds, radius, radius, Path.Direction.CW)
            canvas.clipPath(workClipPath)

            // Center-crop scaling into bounds
            val scale = maxOf(w / bitmap.width.toFloat(), h / bitmap.height.toFloat())
            val drawW = bitmap.width * scale
            val drawH = bitmap.height * scale
            val dest = RectF(-drawW / 2f, -drawH / 2f, drawW / 2f, drawH / 2f)
            fillPaint.alpha = (255 * alphaFactor).toInt().coerceIn(0, 255)
            canvas.drawBitmap(bitmap, null, dest, fillPaint)
            fillPaint.alpha = 255
            canvas.restore()
        } else {
            fillPaint.color = applyAlpha(Color.argb(60, 148, 163, 184), alphaFactor)
            strokePaint.color = applyAlpha(Color.argb(140, 203, 213, 225), alphaFactor)
            strokePaint.strokeWidth = 1.5f * unitScale
            canvas.drawRoundRect(bounds, radius, radius, fillPaint)
            canvas.drawRoundRect(bounds, radius, radius, strokePaint)

            textPaint.color = applyAlpha(Color.argb(200, 226, 232, 240), alphaFactor)
            textPaint.textSize = 12f * unitScale
            textPaint.typeface = Typeface.DEFAULT
            canvas.drawText("Image", 0f, 4f * unitScale, textPaint)
        }
        return bounds
    }

    private fun loadBitmapSafely(uriString: String): Bitmap? {
        val ctx = context ?: return null
        if (uriString.isBlank()) return null
        bitmapCache.get(uriString)?.let { cached ->
            if (!cached.isRecycled) return cached
        }
        return runCatching {
            val uri = Uri.parse(uriString)
            val resolver = ctx.contentResolver
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            val maxDim = maxOf(opts.outWidth, opts.outHeight)
            val sampleSize = if (maxDim > 512) {
                var s = 1
                while (maxDim / (s * 2) >= 512) s *= 2
                s
            } else {
                1
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val rawBmp = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            }
            val bmp = rawBmp?.let { applyExifOrientation(resolver, uri, it) }
            if (bmp != null) {
                bitmapCache.put(uriString, bmp)
            }
            bmp
        }.getOrNull()
    }

    private fun applyExifOrientation(
        resolver: android.content.ContentResolver,
        uri: Uri,
        source: Bitmap,
    ): Bitmap {
        val orientation = runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

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
            else -> return source
        }
        return runCatching {
            val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
            if (rotated != source) source.recycle()
            rotated
        }.getOrElse { source }
    }

    private fun drawSelectionBox(canvas: Canvas, bounds: RectF, unitScale: Float) {
        val pad = 8f * unitScale
        workRect.set(bounds.left - pad, bounds.top - pad, bounds.right + pad, bounds.bottom + pad)
        selectionPaint.strokeWidth = 2f * unitScale
        canvas.drawRoundRect(workRect, 8f * unitScale, 8f * unitScale, selectionPaint)

        val handleR = 4.5f * unitScale
        fillPaint.color = Color.parseColor("#64D2FF")
        canvas.drawCircle(workRect.left, workRect.top, handleR, fillPaint)
        canvas.drawCircle(workRect.right, workRect.top, handleR, fillPaint)
        canvas.drawCircle(workRect.left, workRect.bottom, handleR, fillPaint)
        canvas.drawCircle(workRect.right, workRect.bottom, handleR, fillPaint)
    }

    private fun drawSafeAreaGuides(
        canvas: Canvas,
        width: Int,
        height: Int,
        unitScale: Float,
    ) {
        val topUnsafeBottom = height * 0.28f
        val bottomUnsafeTop = height * 0.80f

        canvas.drawRect(0f, 0f, width.toFloat(), topUnsafeBottom, guideFillPaint)
        canvas.drawRect(0f, bottomUnsafeTop, width.toFloat(), height.toFloat(), guideFillPaint)

        guideLinePaint.strokeWidth = 1.5f * unitScale
        canvas.drawLine(0f, topUnsafeBottom, width.toFloat(), topUnsafeBottom, guideLinePaint)
        canvas.drawLine(0f, bottomUnsafeTop, width.toFloat(), bottomUnsafeTop, guideLinePaint)

        val isJapanese = Locale.getDefault().language == Locale.JAPANESE.language
        val topLabel = if (isJapanese) "OS 時計・通知エリア" else "OS Lock Clock Area"
        val bottomLabel = if (isJapanese) "指紋認証・ショートカット" else "Fingerprint & Shortcuts"
        guideTextPaint.textSize = 11f * unitScale
        canvas.drawText(topLabel, width / 2f, topUnsafeBottom - 10f * unitScale, guideTextPaint)
        canvas.drawText(bottomLabel, width / 2f, bottomUnsafeTop + 18f * unitScale, guideTextPaint)
    }

    private fun measureElementLocalBounds(
        element: CanvasElement,
        width: Int,
        height: Int,
        unitScale: Float,
        snapshot: DynamicDataSnapshot,
    ): RectF = when (element) {
        is ShapeElement -> {
            val w = (element.widthFraction * width).coerceAtLeast(8f)
            val h = (element.heightFraction * height).coerceAtLeast(8f)
            if (element.shapeType == ShapeType.CIRCLE) {
                val r = min(w, h) / 2f
                RectF(-r, -r, r, r)
            } else {
                RectF(-w / 2f, -h / 2f, w / 2f, h / 2f)
            }
        }
        is BatteryGaugeElement -> {
            val w = (element.widthFraction * width).coerceAtLeast(24f)
            val h = (element.heightFraction * height).coerceAtLeast(12f)
            if (element.style == BatteryGaugeStyle.CIRCULAR) {
                val r = min(w, h) / 2f
                RectF(-r, -r, r, r)
            } else {
                RectF(-w / 2f, -h / 2f, w / 2f, h / 2f)
            }
        }
        is ImageElement -> {
            val w = (element.widthFraction * width).coerceAtLeast(24f)
            val h = (element.heightFraction * height).coerceAtLeast(24f)
            RectF(-w / 2f, -h / 2f, w / 2f, h / 2f)
        }
        is TextElement -> {
            val resolved = if (element.dynamicTemplate) {
                TemplateFormatter.format(element.text, snapshot)
            } else {
                element.text
            }
            textPaint.textSize = (element.fontSizeSp * unitScale).coerceAtLeast(6f)
            textPaint.typeface = if (element.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            val lines = resolved.split('\n')
            val maxW = lines.maxOfOrNull { textPaint.measureText(it) } ?: 0f
            val fm = textPaint.fontMetrics
            val totalH = (fm.descent - fm.ascent) * 1.15f * lines.size
            RectF(
                -(maxW / 2f).coerceAtLeast(16f * unitScale),
                -(totalH / 2f).coerceAtLeast(12f * unitScale),
                (maxW / 2f).coerceAtLeast(16f * unitScale),
                (totalH / 2f).coerceAtLeast(12f * unitScale),
            )
        }
    }

    private fun applyAlpha(argb: Int, alphaFactor: Float): Int {
        val baseAlpha = Color.alpha(argb)
        val outAlpha = (baseAlpha * alphaFactor).toInt().coerceIn(0, 255)
        return Color.argb(outAlpha, Color.red(argb), Color.green(argb), Color.blue(argb))
    }
}
