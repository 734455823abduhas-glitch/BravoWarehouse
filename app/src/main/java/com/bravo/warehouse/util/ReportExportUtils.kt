package com.bravo.warehouse.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
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
                        DateTimeUtils.formatDateTime(
                            context,
                            System.currentTimeMillis()
                        )
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

    fun createPdf(
        context: Context,
        title: String,
        headers: List<String>,
        rows: List<List<String>>
    ): File {

        val file = File(reportsFolder(context), "${safeName(title)}.pdf")
        val document = PdfDocument()

        val pageWidth = 595
        val pageHeight = 842
        val margin = 35
        val contentWidth = pageWidth - margin * 2
        val topOffset = 100
        val availableHeight = pageHeight - topOffset - margin

        val reportText = buildString {
            append("براڤو برو للصناعات البلاستيكية\n")
            append("$title\n")
            append("تاريخ التقرير: ")
            append(
                DateTimeUtils.formatDateTime(
                    context,
                    System.currentTimeMillis()
                )
            )
            append("\n\n")
            append(headers.joinToString("     |     "))
            append("\n----------------------------------------\n")

            rows.forEach { row ->
                append(row.joinToString("     |     "))
                append("\n")
            }
        }

        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(16, 43, 80)
            textSize = 12f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }

        val layout = StaticLayout.Builder.obtain(
            reportText,
            0,
            reportText.length,
            paint,
            contentWidth
        )
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setIncludePad(true)
            .setLineSpacing(5f, 1f)
            .build()

        val pageCount = maxOf(
            1,
            (layout.height + availableHeight - 1) / availableHeight
        )

        try {
            for (pageNumber in 0 until pageCount) {

                val pageInfo = PdfDocument.PageInfo.Builder(
                    pageWidth,
                    pageHeight,
                    pageNumber + 1
                ).create()

                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                canvas.drawColor(Color.WHITE)

                val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(8, 47, 104)
                    textSize = 19f
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }

                canvas.drawText(
                    "براڤو برو",
                    pageWidth / 2f,
                    55f,
                    titlePaint
                )

                canvas.save()
                canvas.translate(margin.toFloat(), topOffset.toFloat())
                canvas.clipRect(
                    0,
                    0,
                    contentWidth,
                    availableHeight
                )
                canvas.translate(
                    0f,
                    -(pageNumber * availableHeight).toFloat()
                )
                layout.draw(canvas)
                canvas.restore()

                val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.GRAY
                    textSize = 10f
                    textAlign = Paint.Align.CENTER
                }

                canvas.drawText(
                    "صفحة ${pageNumber + 1} من $pageCount",
                    pageWidth / 2f,
                    pageHeight - 18f,
                    footerPaint
                )

                document.finishPage(page)
            }

            FileOutputStream(file).use {
                document.writeTo(it)
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
}
