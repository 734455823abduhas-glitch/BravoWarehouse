fun createPdf(
    context: Context,
    title: String,
    headers: List<String>,
    rows: List<List<String>>
): File {

    require(headers.isNotEmpty()) { "يجب توفير عناوين الأعمدة" }

    val file = File(reportsFolder(context), "${safeName(title)}.pdf")
    val document = PdfDocument()

    val pageWidth = 595
    val pageHeight = 842
    val margin = 28
    val contentWidth = pageWidth - margin * 2

    val navy = Color.rgb(8, 47, 104)
    val red = Color.rgb(229, 36, 42)
    val textColor = Color.rgb(16, 43, 80)
    val borderColor = Color.rgb(183, 200, 223)
    val alternateColor = Color.rgb(243, 246, 252)

    val normalPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = 9.5f
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }

    val headerTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 9f
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = navy
        textSize = 20f
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 9f
        textAlign = Paint.Align.CENTER
    }

    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = borderColor
        strokeWidth = 0.8f
        style = Paint.Style.STROKE
    }

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    fun makeLayout(
        value: String,
        width: Int,
        paint: TextPaint,
        maxLines: Int = 8
    ): StaticLayout {

        return StaticLayout.Builder.obtain(
            value.ifBlank { "—" },
            0,
            value.ifBlank { "—" }.length,
            paint,
            width.coerceAtLeast(20)
        )
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
            .setIncludePad(false)
            .setLineSpacing(2f, 1f)
            .setMaxLines(maxLines)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()
    }

    val columnCount = headers.size

    // حساب عرض كل عمود بناءً على محتواه
    val measurePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10f
        typeface = Typeface.DEFAULT
    }

    val naturalWidths = headers.indices.map { index ->
        val headerWidth = measurePaint.measureText(headers[index])
        val dataWidth = rows.maxOfOrNull { row ->
            measurePaint.measureText(row.getOrElse(index) { "" })
        } ?: 0f

        (maxOf(headerWidth, dataWidth) + 24f)
            .coerceIn(48f, 170f)
    }.toMutableList()

    val minimumWidth = 38f
    var totalWidth = naturalWidths.sum()

    if (totalWidth < contentWidth) {
        val extra = (contentWidth - totalWidth) / columnCount
        for (i in naturalWidths.indices) {
            naturalWidths[i] += extra
        }
    } else {
        var remaining = contentWidth.toFloat()
        var active = naturalWidths.indices.toMutableSet()

        while (active.isNotEmpty()) {
            val weight = active.sumOf { naturalWidths[it].toDouble() }
                .toFloat()
                .coerceAtLeast(1f)

            val tooSmall = active.filter {
                naturalWidths[it] * remaining / weight < minimumWidth
            }

            if (tooSmall.isEmpty()) {
                active.forEach {
                    naturalWidths[it] =
                        naturalWidths[it] * remaining / weight
                }
                break
            }

            tooSmall.forEach {
                naturalWidths[it] = minimumWidth
                remaining -= minimumWidth
                active.remove(it)
            }
        }
    }

    // ضبط آخر عمود لمنع اختلاف مجموع العرض بسبب الكسور
    totalWidth = naturalWidths.sum()
    naturalWidths[naturalWidths.lastIndex] += contentWidth - totalWidth

    val columnWidths = naturalWidths.map { it.toInt() }

    val dateText = DateTimeUtils.formatDateTime(
        context,
        System.currentTimeMillis()
    )

    val tableTop = 132f
    val headerPadding = 7
    val cellPadding = 6
    val footerY = pageHeight - 22f
    val bottomLimit = pageHeight - 48f

    val headerLayouts = headers.mapIndexed { index, value ->
        makeLayout(
            value,
            columnWidths[index] - headerPadding * 2,
            headerTextPaint,
            3
        )
    }

    val tableHeaderHeight =
        (headerLayouts.maxOfOrNull { it.height } ?: 20) +
            headerPadding * 2

    val rowLayouts = rows.map { row ->
        headers.indices.map { index ->
            makeLayout(
                row.getOrElse(index) { "" },
                columnWidths[index] - cellPadding * 2,
                normalPaint,
                8
            )
        }
    }

    val rowHeights = rowLayouts.map { layouts ->
        (layouts.maxOfOrNull { it.height } ?: 16) +
            cellPadding * 2
    }

    // توزيع الصفوف على الصفحات قبل الرسم
    val pages = mutableListOf<List<Int>>()
    var currentPageRows = mutableListOf<Int>()
    var usedHeight = tableTop + tableHeaderHeight

    rowHeights.forEachIndexed { index, height ->
        if (
            currentPageRows.isNotEmpty() &&
            usedHeight + height > bottomLimit
        ) {
            pages.add(currentPageRows.toList())
            currentPageRows = mutableListOf()
            usedHeight = tableTop + tableHeaderHeight
        }

        currentPageRows.add(index)
        usedHeight += height
    }

    if (currentPageRows.isNotEmpty() || pages.isEmpty()) {
        pages.add(currentPageRows.toList())
    }

    fun drawTableHeader(
        canvas: android.graphics.Canvas,
        y: Float
    ) {
        fillPaint.color = navy

        canvas.drawRect(
            margin.toFloat(),
            y,
            (margin + contentWidth).toFloat(),
            y + tableHeaderHeight,
            fillPaint
        )

        var right = (margin + contentWidth).toFloat()

        headers.indices.forEach { index ->
            val width = columnWidths[index].toFloat()
            val left = right - width

            canvas.save()
            canvas.translate(
                left + headerPadding,
                y + headerPadding
            )

            headerLayouts[index].draw(canvas)
            canvas.restore()

            canvas.drawRect(
                left,
                y,
                right,
                y + tableHeaderHeight,
                linePaint
            )

            right = left
        }
    }

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

            // الترويسة النصية
            canvas.drawText(
                "براڤو برو للصناعات البلاستيكية",
                pageWidth / 2f,
                38f,
                titlePaint
            )

            val reportTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = red
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }

            canvas.drawText(
                title,
                pageWidth / 2f,
                61f,
                reportTitlePaint
            )

            canvas.drawText(
                "تاريخ التقرير: $dateText",
                pageWidth / 2f,
                82f,
                smallPaint
            )

            val separatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = red
                strokeWidth = 1.5f
            }

            canvas.drawLine(
                margin.toFloat(),
                99f,
                (pageWidth - margin).toFloat(),
                99f,
                separatorPaint
            )

            // رأس الجدول يتكرر في كل صفحة
            drawTableHeader(canvas, tableTop)

            var y = tableTop + tableHeaderHeight

            pageRows.forEachIndexed { localIndex, rowIndex ->

                val rowHeight = rowHeights[rowIndex].toFloat()

                if (localIndex % 2 == 1) {
                    fillPaint.color = alternateColor

                    canvas.drawRect(
                        margin.toFloat(),
                        y,
                        (margin + contentWidth).toFloat(),
                        y + rowHeight,
                        fillPaint
                    )
                }

                var right = (margin + contentWidth).toFloat()

                headers.indices.forEach { columnIndex ->

                    val width = columnWidths[columnIndex].toFloat()
                    val left = right - width

                    val layout = rowLayouts[rowIndex][columnIndex]

                    canvas.save()
                    canvas.translate(
                        left + cellPadding,
                        y + cellPadding
                    )
                    canvas.clipRect(
                        0f,
                        0f,
                        width - cellPadding * 2,
                        rowHeight - cellPadding * 2
                    )

                    layout.draw(canvas)
                    canvas.restore()

                    canvas.drawRect(
                        left,
                        y,
                        right,
                        y + rowHeight,
                        linePaint
                    )

                    right = left
                }

                y += rowHeight
            }

            // خط أسفل الجدول
            canvas.drawLine(
                margin.toFloat(),
                y,
                (margin + contentWidth).toFloat(),
                y,
                linePaint
            )

            // تذييل الصفحة
            canvas.drawText(
                "صفحة ${pageIndex + 1} من ${pages.size}",
                pageWidth / 2f,
                footerY,
                smallPaint
            )

            document.finishPage(page)
        }

        FileOutputStream(file).use { output ->
            document.writeTo(output)
            output.flush()
        }

    } finally {
        document.close()
    }

    return file
}
