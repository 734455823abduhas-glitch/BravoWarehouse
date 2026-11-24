package com.bravo.warehouse.util

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextDirectionHeuristics
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ReportExportUtils {

    private fun reportsFolder(context: Context): File {
        return File(context.filesDir, "reports").apply {
            if (!exists()) mkdirs()
        }
    }

    fun createExcel(
        context: Context,
        title: String,
        headers: List<String>,
        rows: List<List<String>>
    ): File {
        val file = File(
            reportsFolder(context),
            "${safeName(title)}.xlsx"
        )

        ZipOutputStream(FileOutputStream(file)).use { zip ->

            fun addFile(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }

            addFile(
                "[Content_Types].xml",
                """<?xml version="1.0" encoding="UTF-8"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                    <Default Extension="xml" ContentType="application/xml"/>
                    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                    <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                </Types>"""
            )

            addFile(
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1"
                    Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument"
                    Target="xl/workbook.xml"/>
                </Relationships>"""
            )

            addFile(
                "xl/workbook.xml",
                """<?xml version="1.0" encoding="UTF-8"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                    <sheets>
                        <sheet name="التقرير" sheetId="1" r:id="rId1"/>
                    </sheets>
                </workbook>"""
            )

            addFile(
                "xl/_rels/workbook.xml.rels",
                """<?xml version="1.0" encoding="UTF-8"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1"
                    Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"
                    Target="worksheets/sheet1.xml"/>
                </Relationships>"""
            )

            val allRows = mutableListOf<List<String>>()
            allRows.add(listOf(title))
            allRows.add(listOf("تاريخ التقرير: ${DateTimeUtils.formatDateTime(context, System.currentTimeMillis())}"))
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
                        val cell = column
