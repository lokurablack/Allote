package com.example.allote.ui.survey.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.allote.data.AnnotationWithMedia
import com.example.allote.data.FieldSurveyWithAnnotations
import com.example.allote.data.Job
import com.example.allote.data.Lote
import com.example.allote.data.SurveyGeometry
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object FieldSurveyExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 48f
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    fun exportToPdf(
        context: Context,
        snapshot: FieldSurveyWithAnnotations,
        job: Job?,
        lote: Lote?
    ): Uri {
        val pdf = PdfDocument()
        var pageIndex = 1

        fun newPage(): PdfDocument.Page {
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex).create()
            pageIndex += 1
            return pdf.startPage(info)
        }

        var page = newPage()
        var canvas = page.canvas
        var cursorY = MARGIN

        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
            color = Color.BLACK
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            textSize = 12f
            color = Color.DKGRAY
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            textSize = 14f
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        fun finishCurrentPage() {
            pdf.finishPage(page)
            page = newPage()
            canvas = page.canvas
            cursorY = MARGIN
            canvas.drawText("Relevamiento visual de lote (continuaciÃ³n)", MARGIN, cursorY, headerPaint)
            cursorY += 20f
        }

        fun ensureSpace(lines: Int, lineHeight: Float = 16f) {
            if (cursorY + lines * lineHeight > PAGE_HEIGHT - MARGIN) {
                finishCurrentPage()
            }
        }

        fun drawMetadata() {
            canvas.drawText("Relevamiento visual de lote", MARGIN, cursorY, titlePaint)
            cursorY += 22f

            val metadata = mutableListOf<String>().apply {
                job?.let {
                    add("Trabajo: ${it.description?.takeIf(String::isNotBlank) ?: "ID ${it.id}"}")
                    add("Cliente: ${it.clientName}")
                }
                lote?.let {
                    add("Lote: ${it.nombre ?: it.id}")
                    if (it.hectareas != null && it.hectareas > 0) {
                        add("Superficie declarada: ${"%.2f".format(Locale.US, it.hectareas)} ha")
                    }
                }
                add("Fecha relevamiento: ${dateFormatter.format(Date(snapshot.survey.updatedAt))}")
                snapshot.survey.notes?.takeIf { it.isNotBlank() }?.let { add("Notas: $it") }
            }

            metadata.forEach {
                canvas.drawText(it, MARGIN, cursorY, bodyPaint)
                cursorY += 16f
            }
            cursorY += 10f
        }

        fun drawVisualOverview() {
            val mapAnnotations = snapshot.annotations.filter {
                when (it.annotation.geometryType) {
                    SurveyGeometry.TYPE_POINT,
                    SurveyGeometry.TYPE_POLYLINE,
                    SurveyGeometry.TYPE_POLYGON -> true
                    else -> false
                }
            }

            val sketchAnnotations = snapshot.annotations.filterNot { mapAnnotations.contains(it) }

            val overviewHeight = 220f
            ensureSpace(lines = 0, lineHeight = overviewHeight)

            val mapArea = RectF(MARGIN, cursorY, PAGE_WIDTH / 2f - 8f, cursorY + overviewHeight)
            val sketchArea = RectF(PAGE_WIDTH / 2f + 8f, cursorY, PAGE_WIDTH - MARGIN, cursorY + overviewHeight)

            drawMapOverview(canvas, mapArea, mapAnnotations, snapshot)
            drawSketchOverview(canvas, sketchArea, sketchAnnotations)

            cursorY += overviewHeight + 20f
        }

        fun drawAnnotationList() {
            canvas.drawText("Anotaciones registradas", MARGIN, cursorY, headerPaint)
            cursorY += 20f

            val sorted = snapshot.annotations.sortedWith(
                compareByDescending<AnnotationWithMedia> { it.annotation.isCritical }
                    .thenBy { it.annotation.category }
            )

            sorted.forEachIndexed { index, annotation ->
                ensureSpace(4)
                val category = annotation.annotation.category.ifBlank { "Sin categorÃ­a" }
                val title = annotation.annotation.title ?: category
                val geometryLabel = when (annotation.annotation.geometryType) {
                    SurveyGeometry.TYPE_POINT -> "Marcador en mapa"
                    SurveyGeometry.TYPE_POLYLINE -> "Recorrido en mapa"
                    SurveyGeometry.TYPE_POLYGON -> "PolÃ­gono en mapa"
                    SurveyGeometry.TYPE_SKETCH_PATH -> "Trazo en croquis"
                    SurveyGeometry.TYPE_SKETCH_SHAPE -> "Figura en croquis"
                    else -> annotation.annotation.geometryType
                }

                canvas.drawText("${index + 1}. $title [$category]", MARGIN, cursorY, bodyPaint)
                cursorY += 14f

                annotation.annotation.description?.takeIf { it.isNotBlank() }?.let {
                    canvas.drawText("â€¢ $it", MARGIN + 12f, cursorY, bodyPaint)
                    cursorY += 14f
                }

                val extra = buildList {
                    add("Tipo: $geometryLabel")
                    if (annotation.annotation.isCritical) add("Dato crÃ­tico")
                    if (annotation.media.isNotEmpty()) add("Adjuntos: ${annotation.media.size}")
                }

                canvas.drawText(extra.joinToString(" â€¢ "), MARGIN + 12f, cursorY, bodyPaint)
                cursorY += 20f
            }
        }

        drawMetadata()
        drawVisualOverview()
        drawAnnotationList()

        pdf.finishPage(page)

        val exportsDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val outputFile = File(exportsDir, "relevamiento_${snapshot.survey.id}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outputFile).use { stream -> pdf.writeTo(stream) }
        pdf.close()

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outputFile)
    }

    private fun drawMapOverview(
        canvas: android.graphics.Canvas,
        area: RectF,
        annotations: List<AnnotationWithMedia>,
        snapshot: FieldSurveyWithAnnotations
    ) {
        val backgroundPaint = Paint().apply {
            color = Color.parseColor("#F5F5F5")
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val borderPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }

        canvas.drawRect(area, backgroundPaint)
        canvas.drawRect(area, borderPaint)

        val boundaryPoints = parseBoundaryPoints(snapshot.survey.boundaryGeoJson)
        val allPoints = mutableListOf<Pair<Double, Double>>()
        allPoints.addAll(boundaryPoints)
        annotations.forEach { annotation ->
            when (val geometry = SurveyGeometry.fromJson(annotation.annotation.geometryPayload)) {
                is SurveyGeometry.MapPoint -> allPoints.add(geometry.latitude to geometry.longitude)
                is SurveyGeometry.MapPolyline -> allPoints.addAll(geometry.points)
                is SurveyGeometry.MapPolygon -> allPoints.addAll(geometry.points)
                else -> Unit
            }
        }

        if (allPoints.isEmpty()) {
            val hintPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 11f
                isAntiAlias = true
            }
            canvas.drawText("Sin datos georreferenciados", area.left + 12f, area.centerY(), hintPaint)
            return
        }

        val minLat = allPoints.minOf { it.first }
        val maxLat = allPoints.maxOf { it.first }
        val minLng = allPoints.minOf { it.second }
        val maxLng = allPoints.maxOf { it.second }

        val latRange = max(0.0001, maxLat - minLat)
        val lngRange = max(0.0001, maxLng - minLng)

        fun project(lat: Double, lng: Double): android.graphics.PointF {
            val x = (lng - minLng) / lngRange
            val y = 1.0 - (lat - minLat) / latRange
            return android.graphics.PointF(
                area.left + (x * area.width()).toFloat(),
                area.top + (y * area.height()).toFloat()
            )
        }

        if (boundaryPoints.isNotEmpty()) {
            val polygonPath = android.graphics.Path()
            boundaryPoints.map { project(it.first, it.second) }.forEachIndexed { index, point ->
                if (index == 0) polygonPath.moveTo(point.x, point.y) else polygonPath.lineTo(point.x, point.y)
            }
            polygonPath.close()
            val fillPaint = Paint().apply {
                color = Color.parseColor("#BBDEFB")
                style = Paint.Style.FILL
            }
            val outlinePaint = Paint().apply {
                color = Color.parseColor("#1976D2")
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawPath(polygonPath, fillPaint)
            canvas.drawPath(polygonPath, outlinePaint)
        }

        val pointPaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.RED
            isAntiAlias = true
        }

        annotations.forEach { annotation ->
            when (val geometry = SurveyGeometry.fromJson(annotation.annotation.geometryPayload)) {
                is SurveyGeometry.MapPoint -> {
                    val point = project(geometry.latitude, geometry.longitude)
                    pointPaint.color = runCatching { Color.parseColor(annotation.annotation.colorHex) }
                        .getOrElse { Color.RED }
                    canvas.drawCircle(point.x, point.y, 4f, pointPaint)
                }
                is SurveyGeometry.MapPolyline -> {
                    val path = android.graphics.Path()
                    val color = runCatching { Color.parseColor(annotation.annotation.colorHex) }.getOrElse { Color.RED }
                    val paint = Paint().apply {
                        this.color = color
                        style = Paint.Style.STROKE
                        strokeWidth = 2f
                        isAntiAlias = true
                    }
                    geometry.points.map { project(it.first, it.second) }.forEachIndexed { index, point ->
                        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                    }
                    canvas.drawPath(path, paint)
                }
                else -> Unit
            }
        }
    }

    private fun drawSketchOverview(
        canvas: android.graphics.Canvas,
        area: RectF,
        annotations: List<AnnotationWithMedia>
    ) {
        val backgroundPaint = Paint().apply {
            color = Color.parseColor("#F9F9F9")
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        canvas.drawRect(area, backgroundPaint)
        canvas.drawRect(area, borderPaint)

        if (annotations.isEmpty()) {
            val hintPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 11f
                isAntiAlias = true
            }
            canvas.drawText("Sin croquis registrados", area.left + 12f, area.centerY(), hintPaint)
            return
        }

        annotations.forEach { annotation ->
            when (val geometry = SurveyGeometry.fromJson(annotation.annotation.geometryPayload)) {
                is SurveyGeometry.SketchPath -> {
                    val color = runCatching { Color.parseColor(annotation.annotation.colorHex) }.getOrElse { Color.RED }
                    val paint = Paint().apply {
                        this.color = color
                        style = Paint.Style.STROKE
                        strokeWidth = 3f
                        isAntiAlias = true
                    }
                    val path = android.graphics.Path()
                    geometry.points.map { point ->
                        val x = area.left + point.first.coerceIn(0f, 1f) * area.width()
                        val y = area.top + point.second.coerceIn(0f, 1f) * area.height()
                        android.graphics.PointF(x, y)
                    }.forEachIndexed { index, point ->
                        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                    }
                    canvas.drawPath(path, paint)
                }
                is SurveyGeometry.SketchShape -> drawSketchShape(canvas, area, geometry, annotation)
                else -> Unit
            }
        }
    }

    private fun drawSketchShape(
        canvas: android.graphics.Canvas,
        area: RectF,
        geometry: SurveyGeometry.SketchShape,
        annotation: AnnotationWithMedia
    ) {
        val color = runCatching { Color.parseColor(annotation.annotation.colorHex) }.getOrElse { Color.RED }
        val paint = Paint().apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }

        val points = geometry.points.map { point ->
            android.graphics.PointF(
                area.left + point.first.coerceIn(0f, 1f) * area.width(),
                area.top + point.second.coerceIn(0f, 1f) * area.height()
            )
        }

        when (geometry.shape.lowercase(Locale.getDefault())) {
            "line" -> if (points.size >= 2) {
                canvas.drawLine(points.first().x, points.first().y, points.last().x, points.last().y, paint)
            }
            "arrow" -> if (points.size >= 2) {
                val start = points.first()
                val end = points.last()
                canvas.drawLine(start.x, start.y, end.x, end.y, paint)
                val arrow = createArrowHead(start, end)
                canvas.drawPath(arrow, paint)
            }
            "rectangle" -> if (points.size >= 2) {
                val left = min(points.first().x, points.last().x)
                val right = max(points.first().x, points.last().x)
                val top = min(points.first().y, points.last().y)
                val bottom = max(points.first().y, points.last().y)
                canvas.drawRect(left, top, right, bottom, paint)
            }
            "circle" -> if (points.size >= 2) {
                val center = points.first()
                val edge = points.last()
                val radius = kotlin.math.hypot((edge.x - center.x).toDouble(), (edge.y - center.y).toDouble()).toFloat()
                canvas.drawCircle(center.x, center.y, radius, paint)
            }
        }
    }

    private fun createArrowHead(start: android.graphics.PointF, end: android.graphics.PointF): android.graphics.Path {
        val arrow = android.graphics.Path()
        val dx = end.x - start.x
        val dy = end.y - start.y
        val length = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat().takeIf { it > 0f } ?: return arrow
        val ux = dx / length
        val uy = dy / length
        val arrowSize = 14f

        val angle = Math.toRadians(25.0)
        val sin = kotlin.math.sin(angle).toFloat()
        val cos = kotlin.math.cos(angle).toFloat()

        val leftX = end.x - arrowSize * (ux * cos - uy * sin)
        val leftY = end.y - arrowSize * (uy * cos + ux * sin)
        val rightX = end.x - arrowSize * (ux * cos + uy * sin)
        val rightY = end.y - arrowSize * (uy * cos - ux * sin)

        arrow.moveTo(end.x, end.y)
        arrow.lineTo(leftX, leftY)
        arrow.moveTo(end.x, end.y)
        arrow.lineTo(rightX, rightY)

        return arrow
    }

    private fun parseBoundaryPoints(json: String?): List<Pair<Double, Double>> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = org.json.JSONArray(json)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(obj.getDouble("lat") to obj.getDouble("lng"))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}


