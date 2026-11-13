package com.bubududu.pet.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.LruCache
import com.bubududu.pet.R
import com.bubududu.pet.data.RoutineSnapshot
import com.bubududu.pet.model.PetGlasses
import com.bubududu.pet.model.PetHat
import com.bubududu.pet.model.PetMoodPose
import com.bubududu.pet.model.PetState

private data class CharacterHeadSpec(
    val crownX: Float,
    val crownY: Float,
    val eyeX: Float,
    val eyeY: Float,
    val headSpanW: Float,
    val isBearDudu: Boolean
)

private data class PoseAnchorSpec(
    val resId: Int,
    val left: CharacterHeadSpec,
    val right: CharacterHeadSpec,
    val hideGlassesWhileSleeping: Boolean = false
)

object Pet2DRenderer {

    private val spriteCache = LruCache<Int, Bitmap>(8)
    private val dressedCache = LruCache<String, Bitmap>(16)

    private val lineColor = Color.rgb(68, 43, 35)

    private fun anchorSpecFor(pose: PetMoodPose): PoseAnchorSpec = when (pose) {
        PetMoodPose.BRUSH_TEETH -> PoseAnchorSpec(
            resId = R.drawable.pet_brush_teeth,
            left = CharacterHeadSpec(0.24f, 0.07f, 0.25f, 0.40f, 0.36f, isBearDudu = true),
            right = CharacterHeadSpec(0.75f, 0.08f, 0.74f, 0.40f, 0.36f, isBearDudu = false)
        )
        PetMoodPose.EAT_RICE -> PoseAnchorSpec(
            resId = R.drawable.pet_eat_rice,
            left = CharacterHeadSpec(0.24f, 0.10f, 0.25f, 0.48f, 0.37f, isBearDudu = true),
            right = CharacterHeadSpec(0.75f, 0.14f, 0.74f, 0.48f, 0.36f, isBearDudu = false)
        )
        PetMoodPose.SLEEP_BLANKET -> PoseAnchorSpec(
            resId = R.drawable.pet_sleep_blanket,
            left = CharacterHeadSpec(0.26f, 0.19f, 0.26f, 0.43f, 0.34f, isBearDudu = true),
            right = CharacterHeadSpec(0.73f, 0.19f, 0.73f, 0.43f, 0.34f, isBearDudu = false),
            hideGlassesWhileSleeping = true
        )
        PetMoodPose.HUNGRY -> PoseAnchorSpec(
            resId = R.drawable.pet_hungry,
            left = CharacterHeadSpec(0.25f, 0.12f, 0.25f, 0.45f, 0.36f, isBearDudu = true),
            right = CharacterHeadSpec(0.74f, 0.14f, 0.73f, 0.46f, 0.36f, isBearDudu = false)
        )
        PetMoodPose.CHILL_PLAY -> PoseAnchorSpec(
            resId = R.drawable.pet_chill_play,
            left = CharacterHeadSpec(0.25f, 0.21f, 0.26f, 0.50f, 0.37f, isBearDudu = true),
            right = CharacterHeadSpec(0.74f, 0.25f, 0.73f, 0.52f, 0.36f, isBearDudu = false)
        )
        PetMoodPose.HAPPY_WAVE -> PoseAnchorSpec(
            resId = R.drawable.pet_happy,
            left = CharacterHeadSpec(0.25f, 0.22f, 0.25f, 0.52f, 0.35f, isBearDudu = false),
            right = CharacterHeadSpec(0.75f, 0.20f, 0.75f, 0.50f, 0.35f, isBearDudu = true)
        )
    }

    private fun loadBaseSprite(context: Context, resId: Int): Bitmap {
        spriteCache.get(resId)?.let { return it }
        val bmp = BitmapFactory.decodeResource(context.resources, resId)
        spriteCache.put(resId, bmp)
        return bmp
    }

    /**
     * Composites the base 2D sprite with extra top headroom so tall hats never clip,
     * then paints crisp 2D flat-lineart hats & glasses on both Bubu & Dudu.
     */
    fun getDressedBitmap(
        context: Context,
        pose: PetMoodPose,
        hat: PetHat,
        glasses: PetGlasses
    ): Bitmap {
        val key = "${pose.name}_${hat.name}_${glasses.name}"
        dressedCache.get(key)?.let { if (!it.isRecycled) return it }

        val spec = anchorSpecFor(pose)
        val base = loadBaseSprite(context, spec.resId)
        val topPad = (base.height * 0.20f).toInt()
        val sidePad = (base.width * 0.04f).toInt()
        val outW = base.width + sidePad * 2
        val outH = base.height + topPad

        val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawBitmap(base, sidePad.toFloat(), topPad.toFloat(), null)

        val spriteLeft = sidePad.toFloat()
        val spriteTop = topPad.toFloat()
        val sw = base.width.toFloat()
        val sh = base.height.toFloat()

        listOf(spec.left, spec.right).forEach { head ->
            val unit = sw * head.headSpanW
            val cx = spriteLeft + sw * head.crownX
            val cy = spriteTop + sh * head.crownY
            val ex = spriteLeft + sw * head.eyeX
            val ey = spriteTop + sh * head.eyeY

            if (!spec.hideGlassesWhileSleeping && glasses != PetGlasses.NONE) {
                drawGlasses2D(canvas, glasses, ex, ey, unit, head.isBearDudu)
            }
            if (hat != PetHat.NONE) {
                drawHat2D(canvas, hat, cx, cy, unit, head.isBearDudu)
            }
        }

        dressedCache.put(key, out)
        return out
    }

    /**
     * Renders a miniature 2D room card scene for the Home Screen AppWidget.
     */
    fun renderWidgetSceneBitmap(
        context: Context,
        state: PetState,
        snapshot: RoutineSnapshot
    ): Bitmap {
        val w = 380
        val h = 250
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = lineColor
            strokeWidth = 3f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        // Floor pill
        fill.color = if (snapshot.isNightVisual) Color.rgb(49, 44, 68) else Color.rgb(245, 228, 211)
        canvas.drawRoundRect(10f, h * 0.64f, w - 10f, h - 8f, 22f, 22f, fill)

        // Small rounded sky window in upper middle-left background
        val winRect = RectF(18f, 12f, 126f, 88f)
        fill.color = when {
            snapshot.effectiveHour >= 20 || snapshot.effectiveHour < 6 -> Color.rgb(55, 63, 98)
            snapshot.effectiveHour in 16..19 -> Color.rgb(255, 214, 178)
            else -> Color.rgb(198, 236, 255)
        }
        canvas.drawRoundRect(winRect, 16f, 16f, fill)

        // Window sun or moon
        if (snapshot.effectiveHour >= 20 || snapshot.effectiveHour < 6) {
            fill.color = Color.rgb(255, 235, 160)
            canvas.drawCircle(94f, 36f, 12f, fill)
            fill.color = Color.rgb(55, 63, 98)
            canvas.drawCircle(100f, 32f, 10f, fill)
            fill.color = Color.rgb(255, 245, 200)
            canvas.drawCircle(42f, 30f, 3f, fill)
            canvas.drawCircle(64f, 44f, 2.5f, fill)
        } else {
            fill.color = if (snapshot.effectiveHour in 16..19) Color.rgb(255, 168, 104) else Color.rgb(255, 224, 110)
            canvas.drawCircle(46f, 36f, 14f, fill)
            fill.color = Color.WHITE
            canvas.drawCircle(84f, 52f, 10f, fill)
            canvas.drawCircle(98f, 52f, 12f, fill)
        }
        stroke.strokeWidth = 3f
        canvas.drawRoundRect(winRect, 16f, 16f, stroke)

        // Dressed 2D Bubu & Dudu
        val dressed = getDressedBitmap(context, snapshot.pose, state.equippedHat, state.equippedGlasses)
        val targetH = h * 0.86f
        val scale = minOf((w * 0.90f) / dressed.width, targetH / dressed.height)
        val drawW = dressed.width * scale
        val drawH = dressed.height * scale
        val dx = (w - drawW) / 2f
        val dy = h - 14f - drawH
        canvas.drawBitmap(
            dressed,
            null,
            RectF(dx, dy, dx + drawW, dy + drawH),
            Paint(Paint.FILTER_BITMAP_FLAG)
        )

        // Mess crumbs on the floor
        val spots = listOf(42f to h - 24f, w - 48f to h - 28f, 76f to h - 18f, w - 82f to h - 18f)
        for (i in 0 until state.messCount.coerceAtMost(spots.size)) {
            val (mx, my) = spots[i]
            fill.color = Color.rgb(206, 179, 152)
            canvas.drawCircle(mx, my, 8f, fill)
            canvas.drawCircle(mx + 6f, my + 2f, 6f, fill)
            stroke.strokeWidth = 2.4f
            canvas.drawCircle(mx, my, 8f, stroke)
        }

        return bmp
    }

    fun drawHat2D(
        canvas: Canvas,
        hat: PetHat,
        cx: Float,
        cy: Float,
        unit: Float,
        isBearDudu: Boolean
    ) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = lineColor
            strokeWidth = (unit * 0.032f).coerceAtLeast(3f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        when (hat) {
            PetHat.NONE -> Unit

            PetHat.STRAW_HAT -> {
                val brimRx = unit * 0.42f
                val brimRy = unit * 0.11f
                val hatY = cy + unit * 0.02f

                // Wide straw brim
                fill.color = Color.rgb(248, 224, 146)
                canvas.drawOval(cx - brimRx, hatY - brimRy, cx + brimRx, hatY + brimRy, fill)
                canvas.drawOval(cx - brimRx, hatY - brimRy, cx + brimRx, hatY + brimRy, stroke)

                // Dome crown
                val domeRect = RectF(cx - unit * 0.25f, hatY - unit * 0.24f, cx + unit * 0.25f, hatY + unit * 0.04f)
                canvas.drawArc(domeRect, 180f, 180f, true, fill)
                canvas.drawArc(domeRect, 180f, 180f, false, stroke)

                // Pastel sky-blue / pink ribbon band
                fill.color = if (isBearDudu) Color.rgb(148, 200, 236) else Color.rgb(245, 170, 182)
                val bandRect = RectF(cx - unit * 0.24f, hatY - unit * 0.075f, cx + unit * 0.24f, hatY - unit * 0.01f)
                canvas.drawRoundRect(bandRect, 6f, 6f, fill)
                canvas.drawRoundRect(bandRect, 6f, 6f, stroke)
            }

            PetHat.BERRY_BEANIE -> {
                val beanieY = cy + unit * 0.03f
                val domeRect = RectF(cx - unit * 0.29f, beanieY - unit * 0.29f, cx + unit * 0.29f, beanieY + unit * 0.08f)
                fill.color = if (isBearDudu) Color.rgb(160, 210, 180) else Color.rgb(248, 174, 188)
                canvas.drawArc(domeRect, 180f, 180f, true, fill)
                canvas.drawArc(domeRect, 180f, 180f, false, stroke)

                // Pom-pom on top
                fill.color = Color.rgb(255, 249, 240)
                canvas.drawCircle(cx, beanieY - unit * 0.30f, unit * 0.075f, fill)
                canvas.drawCircle(cx, beanieY - unit * 0.30f, unit * 0.075f, stroke)

                // Knitted folded cuff
                fill.color = if (isBearDudu) Color.rgb(132, 185, 155) else Color.rgb(235, 146, 165)
                val cuff = RectF(cx - unit * 0.31f, beanieY - unit * 0.08f, cx + unit * 0.31f, beanieY + unit * 0.05f)
                canvas.drawRoundRect(cuff, unit * 0.05f, unit * 0.05f, fill)
                canvas.drawRoundRect(cuff, unit * 0.05f, unit * 0.05f, stroke)
            }

            PetHat.DAISY_CLIP -> {
                val fx = cx + if (isBearDudu) -unit * 0.14f else unit * 0.14f
                val fy = cy + unit * 0.03f
                val pr = unit * 0.065f
                fill.color = Color.WHITE
                for (a in 0 until 5) {
                    val rad = Math.toRadians(a * 72.0)
                    val px = fx + kotlin.math.cos(rad).toFloat() * (pr * 1.15f)
                    val py = fy + kotlin.math.sin(rad).toFloat() * (pr * 1.15f)
                    canvas.drawCircle(px, py, pr, fill)
                    canvas.drawCircle(px, py, pr, stroke)
                }
                fill.color = Color.rgb(255, 210, 86)
                canvas.drawCircle(fx, fy, pr * 0.85f, fill)
                canvas.drawCircle(fx, fy, pr * 0.85f, stroke)
            }

            PetHat.MINI_CROWN -> {
                val baseW = unit * 0.30f
                val baseH = unit * 0.21f
                val bottomY = cy + unit * 0.02f
                val path = Path().apply {
                    moveTo(cx - baseW * 0.5f, bottomY)
                    lineTo(cx - baseW * 0.58f, bottomY - baseH * 0.78f)
                    lineTo(cx - baseW * 0.22f, bottomY - baseH * 0.42f)
                    lineTo(cx, bottomY - baseH)
                    lineTo(cx + baseW * 0.22f, bottomY - baseH * 0.42f)
                    lineTo(cx + baseW * 0.58f, bottomY - baseH * 0.78f)
                    lineTo(cx + baseW * 0.5f, bottomY)
                    close()
                }
                fill.color = Color.rgb(255, 214, 92)
                canvas.drawPath(path, fill)
                canvas.drawPath(path, stroke)

                fill.color = Color.rgb(245, 118, 146)
                canvas.drawCircle(cx, bottomY - baseH * 0.34f, unit * 0.035f, fill)
            }
        }
    }

    fun drawGlasses2D(
        canvas: Canvas,
        glasses: PetGlasses,
        ex: Float,
        ey: Float,
        unit: Float,
        isBearDudu: Boolean
    ) {
        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = lineColor
            strokeWidth = (unit * 0.030f).coerceAtLeast(2.8f)
        }
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val lensPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        val lensSpacing = unit * 0.195f
        val lx = ex - lensSpacing
        val rx = ex + lensSpacing

        when (glasses) {
            PetGlasses.NONE -> Unit

            PetGlasses.ROUND_SUN, PetGlasses.PINK_SHADES -> {
                val radX = unit * 0.145f
                val radY = unit * 0.118f
                framePaint.color = when {
                    glasses == PetGlasses.PINK_SHADES -> Color.rgb(248, 174, 200)
                    isBearDudu -> Color.rgb(250, 216, 108)
                    else -> Color.rgb(218, 186, 240)
                }
                lensPaint.color = if (glasses == PetGlasses.PINK_SHADES) {
                    Color.argb(215, 92, 48, 66)
                } else {
                    Color.argb(235, 34, 28, 30)
                }

                // Bridge
                canvas.drawLine(lx + radX * 0.7f, ey, rx - radX * 0.7f, ey, rimPaint)
                for (px in listOf(lx, rx)) {
                    val oval = RectF(px - radX, ey - radY, px + radX, ey + radY)
                    canvas.drawOval(oval, framePaint)
                    val inner = RectF(px - radX * 0.76f, ey - radY * 0.72f, px + radX * 0.76f, ey + radY * 0.72f)
                    canvas.drawOval(inner, lensPaint)
                    canvas.drawOval(oval, rimPaint)
                    // Cute specular highlight
                    val sheen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb(110, 255, 255, 255)
                        strokeWidth = (unit * 0.022f).coerceAtLeast(2f)
                        strokeCap = Paint.Cap.ROUND
                    }
                    canvas.drawLine(px - radX * 0.35f, ey - radY * 0.32f, px - radX * 0.1f, ey - radY * 0.1f, sheen)
                }
            }

            PetGlasses.STUDY_GLASSES -> {
                val rad = unit * 0.135f
                lensPaint.color = Color.argb(48, 190, 232, 255)
                rimPaint.color = Color.rgb(92, 62, 50)
                canvas.drawLine(lx + rad, ey, rx - rad, ey, rimPaint)
                for (px in listOf(lx, rx)) {
                    canvas.drawCircle(px, ey, rad, lensPaint)
                    canvas.drawCircle(px, ey, rad, rimPaint)
                }
            }

            PetGlasses.STAR_SHADES -> {
                val rOuter = unit * 0.165f
                val rInner = unit * 0.078f
                canvas.drawLine(lx + rOuter * 0.55f, ey, rx - rOuter * 0.55f, ey, rimPaint)
                lensPaint.color = Color.rgb(255, 210, 96)
                for (px in listOf(lx, rx)) {
                    val star = Path()
                    for (i in 0 until 10) {
                        val r = if (i % 2 == 0) rOuter else rInner
                        val angle = Math.toRadians(-90.0 + i * 36.0)
                        val sx = px + kotlin.math.cos(angle).toFloat() * r
                        val sy = ey + kotlin.math.sin(angle).toFloat() * r
                        if (i == 0) star.moveTo(sx, sy) else star.lineTo(sx, sy)
                    }
                    star.close()
                    canvas.drawPath(star, lensPaint)
                    canvas.drawPath(star, rimPaint)
                }
            }
        }
    }
}
