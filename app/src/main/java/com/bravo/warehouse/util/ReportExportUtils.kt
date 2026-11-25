package com.bravo.warehouse.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.max
import kotlin.math.min

object ReportExportUtils {

    private fun reportsFolder(context: Context): File {
        return File(context.filesDir, "reports").apply {
            if (!exists()) mkdirs()
        }
    }

    private fun safeName(value: String): String {
        return value.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .take(80)
            .ifBlank { "Report" }
    }

    private fun xmlEscape(value: String): String {
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun columnName(number: Int): String {
        var n = number
        var result = ""
        while (n > 0) {
            val remainder = (n - 1) % 26
            result = ('A'.code + remainder).toChar().toString() + result
            n = (n - 1) / 26
        }
        return result
    }

    fun createExcel(
        context: Context,
        title: String,
        headers: List<String>,
        rows: List<List<String>>
    ): File {
        val file = File(reportsFolder(context), "${safeName(title)}.xlsx")
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            fun add(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }

            add(
                "[Content_Types].xml",
                """<?xml version="1.0" encoding="UTF-8"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""
            )
            add(
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
            )
            add(
                "xl/workbook.xml",
                """<?xml version="1.0" encoding="UTF-8"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="التقرير" sheetId="1" r:id="rId1"/></sheets>
</workbook>"""
            )
            add(
                "xl/_rels/workbook.xml.rels",
                """<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>"""
            )

            val allRows = mutableListOf<List<String>>()
            allRows.add(listOf(title))
            allRows.add(
                listOf(
                    "تاريخ التقرير: ${
                        DateTimeUtils.formatDateTime(context, System.currentTimeMillis())
                    }"
                )
            )
            allRows.add(emptyList())
            allRows.add(headers)
            allRows.addAll(rows)

            val sheet = buildString {
                append(
                    """<?xml version="1.0" encoding="UTF-8"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<sheetViews><sheetView workbookViewId="0" rightToLeft="1"/></sheetViews>
<sheetData>"""
                )
                allRows.forEachIndexed { rowIndex, row ->
                    append("""<row r="${rowIndex + 1}">""")
                    row.forEachIndexed { columnIndex, value ->
                        val cell = columnName(columnIndex + 1) + (rowIndex + 1)
                        append(
                            """<c r="$cell" t="inlineStr"><is><t xml:space="preserve">${xmlEscape(value)}</t></is></c>"""
                        )
                    }
                    append("</row>")
                }
                append("</sheetData></worksheet>")
            }
            add("xl/worksheets/sheet1.xml", sheet)
        }
        return file
    }

    private const val PAGE_MARGIN = 28f
    private const val CELL_PADDING = 6f
    private const val HEADER_HEIGHT = 92f
    private const val FOOTER_HEIGHT = 28f

    private val navy = Color.rgb(8, 47, 104)
    private val lightBlue = Color.rgb(235, 241, 250)
    private val borderColor = Color.rgb(165, 184, 210)
    private val textColor = Color.rgb(16, 43, 80)

    private data class PdfCell(val text: String, val layout: StaticLayout)
    private data class PdfRow(val cells: List<PdfCell>, val height: Float)

    private fun makeTextPaint(
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextPaint {
        return TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = Typeface.create(
                "sans-serif",
                if (bold) Typeface.BOLD else Typeface.NORMAL
            )
        }
    }

    private fun makeLayout(
        text: String,
        paint: TextPaint,
        width: Int,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_CENTER
    ): StaticLayout {
        val safeText = text.ifBlank { " " }
        return StaticLayout.Builder.obtain(
            safeText, 0, safeText.length, paint, max(1, width)
        )
            .setAlignment(alignment)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
            .setIncludePad(true)
            .setLineSpacing(2f, 1f)
            .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            .build()
    }

    private fun calculateColumnWidths(
        headers: List<String>,
        rows: List<List<String>>,
        availableWidth: Float,
        paint: TextPaint
    ): List<Float> {
        val count = headers.size
        if (count == 0) return emptyList()

        val weights = MutableList(count) { 1f }
        for (column in 0 until count) {
            var maximum = paint.measureText(headers[column]).coerceAtMost(150f)
            rows.take(100).forEach { row ->
                val value = row.getOrNull(column).orEmpty()
                maximum = max(maximum, paint.measureText(value).coerceAtMost(150f))
            }
            weights[column] = maximum.coerceIn(42f, 150f)
        }

        val minimumWidth = min(48f, availableWidth / count)
        val minimumTotal = minimumWidth * count
        if (minimumTotal >= availableWidth) {
            return List(count) { availableWidth / count }
        }
        val remaining = availableWidth - minimumTotal
        val totalWeight = weights.sum().coerceAtLeast(1f)
        return weights.map { weight ->
            minimumWidth + remaining * (weight / totalWeight)
        }
    }

    private fun buildPdfRow(
        values: List<String>,
        widths: List<Float>,
        paint: TextPaint
    ): PdfRow {
        val cells = widths.mapIndexed { index, width ->
            val text = values.getOrNull(index).orEmpty()
            val layout = makeLayout(text, paint, (width - CELL_PADDING * 2).toInt())
            PdfCell(text, layout)
        }
        val maximumTextHeight = cells.maxOfOrNull {
            it.layout.height.toFloat()
        } ?: 0f
        val height = max(28f, maximumTextHeight + CELL_PADDING * 2)
        return PdfRow(cells, height)
    }

    private fun drawCell(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        cell: PdfCell,
        fillColor: Int,
        paint: Paint
    ) {
        paint.color = fillColor
        paint.style = Paint.Style.FILL
        canvas.drawRect(x, y, x + width, y + height, paint)

        paint.color = borderColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.7f
        canvas.drawRect(x, y, x + width, y + height, paint)

        val textHeight = cell.layout.height.toFloat()
        val textY = y + max(CELL_PADDING, (height - textHeight) / 2f)
        canvas.save()
        canvas.translate(x + CELL_PADDING, textY)
        cell.layout.draw(canvas)
        canvas.restore()
        paint.style = Paint.Style.FILL
    }

    private fun drawTableRow(
        canvas: Canvas,
        row: PdfRow,
        widths: List<Float>,
        startX: Float,
        y: Float,
        header: Boolean,
        paint: Paint
    ) {
        var currentX = startX + widths.sum()
        row.cells.forEachIndexed { index, cell ->
            val width = widths[index]
            currentX -= width
            drawCell(
                canvas = canvas,
                x = currentX,
                y = y,
                width = width,
                height = row.height,
                cell = cell,
                fillColor = if (header) navy else {
                    if (index % 2 == 0) Color.WHITE else lightBlue
                },
                paint = paint
            )
        }
    }

    private fun drawPageHeader(
        canvas: Canvas,
        pageWidth: Int,
        title: String,
        date: String,
        pageNumber: Int,
        paint: Paint
    ) {
        paint.color = navy
        paint.textSize = 19f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textAlign = Paint.Align.CENTER
        paint.style = Paint.Style.FILL
        canvas.drawText(
            "براڤو برو للصناعات البلاستيكية",
            pageWidth / 2f,
            32f,
            paint
        )

        paint.textSize = 15f
        canvas.drawText(title, pageWidth / 2f, 56f, paint)

        paint.color = textColor
        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(
            "تاريخ التقرير: $date",
            pageWidth / 2f,
            75f,
            paint
        )

        paint.color = borderColor
        paint.strokeWidth = 1f
        canvas.drawLine(
            PAGE_MARGIN,
            84f,
            pageWidth - PAGE_MARGIN,
            84f,
            paint
        )
    }

    private fun drawPageFooter(
        canvas: Canvas,
        pageWidth: Int,
        pageHeight: Int,
        pageNumber: Int,
        pageCount: Int,
        paint: Paint
    ) {
        paint.color = Color.GRAY
        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            "صفحة $pageNumber من $pageCount",
            pageWidth / 2f,
            pageHeight - 12f,
            paint
        )
    }

    fun createPdf(
        context: Context,
        title: String,
        headers: List<String>,
        rows: List<List<String>>
    ): File {
        require(headers.isNotEmpty()) {
            "يجب توفير أعمدة للتقرير"
        }

        val file = File(reportsFolder(context), "${safeName(title)}.pdf")
        val document = PdfDocument()
        val columnCount = headers.size
        val landscape = columnCount > 5
        val pageWidth = if (landscape) 842 else 595
        val pageHeight = if (landscape) 595 else 842
        val contentWidth = pageWidth - PAGE_MARGIN * 2
        val tableTop = HEADER_HEIGHT
        val tableBottom = pageHeight - FOOTER_HEIGHT
        val availableTableHeight = tableBottom - tableTop

        val headerPaint = makeTextPaint(
            size = if (columnCount > 6) 9f else 10f,
            color = Color.WHITE,
            bold = true
        )
        val bodyPaint = makeTextPaint(
            size = if (columnCount > 6) 8f else 9.5f,
            color = textColor
        )
        val tablePaint = Paint(Paint.ANTI_ALIAS_FLAG)

        val widths = calculateColumnWidths(
            headers,
            rows,
            contentWidth,
            bodyPaint
        )
        val headerRow = buildPdfRow(headers, widths, headerPaint)
        val bodyRows = rows.map { row ->
            buildPdfRow(row, widths, bodyPaint)
        }

        val pages = mutableListOf<List<PdfRow>>()
        var currentPageRows = mutableListOf<PdfRow>()
        var usedHeight = headerRow.height

        bodyRows.forEach { row ->
            if (
                currentPageRows.isNotEmpty() &&
                usedHeight + row.height > availableTableHeight
            ) {
                pages.add(currentPageRows)
                currentPageRows = mutableListOf()
                usedHeight = headerRow.height
            }
            currentPageRows.add(row)
            usedHeight += row.height
        }

        if (currentPageRows.isNotEmpty() || pages.isEmpty()) {
            pages.add(currentPageRows)
        }

        val date = DateTimeUtils.formatDateTime(
            context,
            System.currentTimeMillis()
        )

        try {
            pages.forEachIndexed { pageIndex, pageRows ->
                val pageInfo = PdfDocument.PageInfo.Builder(
                    pageWidth,
                    pageHeight,
                    pageIndex + 1
                ).create()

                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(Color.WHITE)

                drawPageHeader(
                    canvas,
                    pageWidth,
                    title,
                    date,
                    pageIndex + 1,
                    tablePaint
                )

                var y = tableTop
                drawTableRow(
                    canvas,
                    headerRow,
                    widths,
                    PAGE_MARGIN,
                    y,
                    true,
                    tablePaint
                )
                y += headerRow.height

                pageRows.forEach { row ->
                    drawTableRow(
                        canvas,
                        row,
                        widths,
                        PAGE_MARGIN,
                        y,
                        false,
                        tablePaint
                    )
                    y += row.height
                }

                drawPageFooter(
                    canvas,
                    pageWidth,
                    pageHeight,
                    pageIndex + 1,
                    pages.size,
                    tablePaint
                )
                document.finishPage(page)
            }

            FileOutputStream(file).use { output ->
                document.writeTo(output)
            }
        } finally {
            document.close()
        }

        return file
    }

    fun previewPdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(intent, "معاينة التقرير")
        )
    }

    fun printPdf(context: Context, file: File) {
        require(file.exists() && file.extension.equals("pdf", true)) {
            "ملف PDF غير موجود أو غير صالح"
        }

        val printManager = context.getSystemService(
            Context.PRINT_SERVICE
        ) as PrintManager

        val printAdapter = object : PrintDocumentAdapter() {

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: android.os.Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }

                val info = PrintDocumentInfo.Builder(file.name)
                    .setContentType(
                        PrintDocumentInfo.CONTENT_TYPE_DOCUMENT
                    )
                    .setPageCount(
                        PrintDocumentInfo.PAGE_COUNT_UNKNOWN
                    )
                    .build()

                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (destination == null) {
                    callback?.onWriteFailed("وجهة الطباعة غير متاحة")
                    return
                }

                if (cancellationSignal?.isCanceled == true) {
                    callback?.onWriteCancelled()
                    return
                }

                try {
                    FileInputStream(file).use { input ->
                        FileOutputStream(
                            destination.fileDescriptor
                        ).use { output ->
                            val buffer = ByteArray(8192)

                            while (true) {
                                if (cancellationSignal?.isCanceled == true) {
                                    callback?.onWriteCancelled()
                                    return
                                }

                                val count = input.read(buffer)
                                if (count == -1) break

                                output.write(buffer, 0, count)
                            }

                            output.flush()
                        }
                    }

                    callback?.onWriteFinished(
                        arrayOf(PageRange.ALL_PAGES)
                    )
                } catch (e: Exception) {
                    callback?.onWriteFailed(
                        e.message ?: "تعذرت قراءة ملف PDF"
                    )
                }
            }
        }

        printManager.print(
            safeName(file.nameWithoutExtension),
            printAdapter,
            PrintAttributes.Builder().build()
        )
    }

    fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val mimeType = when (file.extension.lowercase()) {
            "pdf" -> "application/pdf"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            else -> "*/*"
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(intent, "مشاركة التقرير")
        )
    }

    fun saveToUri(
        context: Context,
        sourceFile: File,
        destinationUri: Uri
    ) {
        require(sourceFile.exists()) {
            "الملف المراد حفظه غير موجود"
        }

        context.contentResolver.openOutputStream(destinationUri)?.use { output ->
            sourceFile.inputStream().use { input ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("تعذر فتح مكان الحفظ")
    }
}
