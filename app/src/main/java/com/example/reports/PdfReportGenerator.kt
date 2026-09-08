package com.example.reports

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.*
import org.json.JSONArray
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Complete Data Model bundle passed into PDF Generator
 */
data class MonthlyReportData(
    val selectedMonth: String, // e.g. "2025-06" or "Jun-2025"
    val displayMonth: String,  // e.g. "Jun-2025" or "जून 2025"
    val school: SchoolEntity?,
    val agency: CookingAgencyEntity?,
    val pdsShop: PdsShopEntity?,
    val cooks: List<CookEntity>,
    val enrollment: MonthlyEnrollmentEntity?,
    val teachers: MonthlyTeacherEntity?,
    val dailyMeals: List<DailyMealRecordEntity>,
    val receipts: List<RiceReceiptEntity>,
    val configNorms: ConfigNormsEntity?,
    val openingRiceStockKg: Double,
    val closingRiceStockKg: Double,
    val isSignedByHeadMaster: Boolean = true,
    val headMasterName: String? = null,
    val headMasterDesignation: String = "प्रधान पाठक / संस्था प्रमुख",
    val signDate: String = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().time),
    val signatureBitmap: Bitmap? = null
)

object PdfReportGenerator {

    /**
     * Format 1: 2-Page Portrait Official Chhattisgarh Monthly Proforma
     * "मासिक प्रपत्र - मध्यान्ह भोजन योजना - शाला मासिक जानकारी प्रपत्र"
     */
    fun generateFormat1Pdf(context: Context, data: MonthlyReportData): File {
        val pdfDoc = PdfDocument()
        val pageWidth = 595 // A4 standard pt width
        val pageHeight = 842 // A4 standard pt height

        // PAGE 1
        val pageInfo1 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page1 = pdfDoc.startPage(pageInfo1)
        drawFormat1Page1(page1.canvas, pageWidth.toFloat(), pageHeight.toFloat(), data)
        pdfDoc.finishPage(page1)

        // PAGE 2
        val pageInfo2 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
        val page2 = pdfDoc.startPage(pageInfo2)
        drawFormat1Page2(page2.canvas, pageWidth.toFloat(), pageHeight.toFloat(), data)
        pdfDoc.finishPage(page2)

        val outputFile = File(context.cacheDir, "MDM_Monthly_Proforma_${data.selectedMonth}.pdf")
        FileOutputStream(outputFile).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()
        return outputFile
    }

    /**
     * Format 2: 1-Page Landscape Official Master Ledger
     * "मध्यान्ह भोजन योजना प्रपत्र"
     */
    fun generateFormat2Pdf(context: Context, data: MonthlyReportData): File {
        val pdfDoc = PdfDocument()
        val pageWidth = 842 // A4 Landscape pt width
        val pageHeight = 595 // A4 Landscape pt height

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        drawFormat2Landscape(page.canvas, pageWidth.toFloat(), pageHeight.toFloat(), data)
        pdfDoc.finishPage(page)

        val outputFile = File(context.cacheDir, "MDM_Summary_Ledger_${data.selectedMonth}.pdf")
        FileOutputStream(outputFile).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()
        return outputFile
    }

    /**
     * Combined PDF containing both formats (3 Pages: 2 Portrait + 1 Landscape)
     */
    fun generateCombinedPdf(context: Context, data: MonthlyReportData): File {
        val pdfDoc = PdfDocument()

        // Page 1 (Portrait)
        val p1 = pdfDoc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        drawFormat1Page1(p1.canvas, 595f, 842f, data)
        pdfDoc.finishPage(p1)

        // Page 2 (Portrait)
        val p2 = pdfDoc.startPage(PdfDocument.PageInfo.Builder(595, 842, 2).create())
        drawFormat1Page2(p2.canvas, 595f, 842f, data)
        pdfDoc.finishPage(p2)

        // Page 3 (Landscape)
        val p3 = pdfDoc.startPage(PdfDocument.PageInfo.Builder(842, 595, 3).create())
        drawFormat2Landscape(p3.canvas, 842f, 595f, data)
        pdfDoc.finishPage(p3)

        val outputFile = File(context.cacheDir, "MDM_Complete_Report_${data.selectedMonth}.pdf")
        FileOutputStream(outputFile).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()
        return outputFile
    }

    /**
     * Share PDF file via Android Share Sheet
     */
    fun sharePdf(context: Context, file: File, title: String = "मध्यान्ह भोजन मासिक रिपोर्ट") {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "$title ($file.name)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "रिपोर्ट शेयर करें (Share PDF)"))
        } catch (e: Exception) {
            Toast.makeText(context, "शेयर करने में त्रुटि: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Share PDF directly to a specific recipient phone number on WhatsApp / WhatsApp Business.
     * Uses the WhatsApp target package and JID extra ("91XXXXXXXXXX@s.whatsapp.net")
     * to open the chat directly with that phone number and attach the PDF report.
     */
    fun sharePdfToWhatsApp(
        context: Context,
        file: File,
        rawMobileNumber: String,
        caption: String = "मध्यान्ह भोजन मासिक प्रपत्र (PM POSHAN Monthly Report)"
    ): Boolean {
        try {
            val cleanDigits = rawMobileNumber.filter { it.isDigit() }
            val formattedNumber = when {
                cleanDigits.length == 10 -> "91$cleanDigits"
                cleanDigits.startsWith("91") && cleanDigits.length == 12 -> cleanDigits
                cleanDigits.startsWith("0") && cleanDigits.length == 11 -> "91" + cleanDigits.drop(1)
                else -> cleanDigits
            }

            if (formattedNumber.length < 10) {
                Toast.makeText(context, "कृपया वैध 10-अंकीय मोबाइल नंबर दर्ज करें", Toast.LENGTH_SHORT).show()
                return false
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val pm = context.packageManager
            val isStandardWhatsAppInstalled = try {
                pm.getPackageInfo("com.whatsapp", 0)
                true
            } catch (e: Exception) {
                false
            }
            val isBusinessWhatsAppInstalled = try {
                pm.getPackageInfo("com.whatsapp.w4b", 0)
                true
            } catch (e: Exception) {
                false
            }

            val targetPackage = when {
                isStandardWhatsAppInstalled -> "com.whatsapp"
                isBusinessWhatsAppInstalled -> "com.whatsapp.w4b"
                else -> null
            }

            if (targetPackage == null) {
                Toast.makeText(
                    context,
                    "WhatsApp या WhatsApp Business इस डिवाइस में नहीं मिला। सामान्य शेयर मेनू खोला जा रहा है।",
                    Toast.LENGTH_LONG
                ).show()
                sharePdf(context, file, caption)
                return false
            }

            // Direct intent targeting WhatsApp chat with PDF attachment
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, caption)
                putExtra("jid", "$formattedNumber@s.whatsapp.net")
                setPackage(targetPackage)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                // If specific jid parameter is blocked on customized ROM, try general WhatsApp send
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, caption)
                    setPackage(targetPackage)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                return true
            }
        } catch (e: Exception) {
            Toast.makeText(context, "व्हाट्सएप शेयर में त्रुटि: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            sharePdf(context, file, caption)
            return false
        }
    }

    /**
     * Share daily attendance and meal details directly to the SHG President on WhatsApp / WhatsApp Business.
     * Optionally attaches the daily meal photo if available.
     */
    fun shareDailyAttendanceToWhatsApp(
        context: Context,
        rawMobileNumber: String,
        message: String,
        photoUriString: String? = null
    ): Boolean {
        try {
            val cleanDigits = rawMobileNumber.filter { it.isDigit() }
            val formattedNumber = when {
                cleanDigits.length == 10 -> "91$cleanDigits"
                cleanDigits.startsWith("91") && cleanDigits.length == 12 -> cleanDigits
                cleanDigits.startsWith("0") && cleanDigits.length == 11 -> "91" + cleanDigits.drop(1)
                else -> cleanDigits
            }

            if (formattedNumber.length < 10) {
                Toast.makeText(context, "कृपया वैध 10-अंकीय मोबाइल नंबर दर्ज करें", Toast.LENGTH_SHORT).show()
                return false
            }

            val pm = context.packageManager
            val isStandardWhatsAppInstalled = try {
                pm.getPackageInfo("com.whatsapp", 0)
                true
            } catch (e: Exception) {
                false
            }
            val isBusinessWhatsAppInstalled = try {
                pm.getPackageInfo("com.whatsapp.w4b", 0)
                true
            } catch (e: Exception) {
                false
            }

            val targetPackage = when {
                isStandardWhatsAppInstalled -> "com.whatsapp"
                isBusinessWhatsAppInstalled -> "com.whatsapp.w4b"
                else -> null
            }

            // Check if photo is attached and valid
            var photoUri: Uri? = null
            if (!photoUriString.isNullOrBlank()) {
                try {
                    val parsed = Uri.parse(photoUriString)
                    if (photoUriString.startsWith("file://")) {
                        val f = File(parsed.path ?: "")
                        if (f.exists()) {
                            photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
                        }
                    } else if (photoUriString.startsWith("content://")) {
                        photoUri = parsed
                    }
                } catch (_: Exception) {}
            }

            if (photoUri != null && targetPackage != null) {
                try {
                    context.grantUriPermission(targetPackage, photoUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val imgIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, photoUri)
                        putExtra(Intent.EXTRA_TEXT, message)
                        putExtra("jid", "$formattedNumber@s.whatsapp.net")
                        setPackage(targetPackage)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(imgIntent)
                    return true
                } catch (_: Exception) {
                    // Fallback to text intent if image dispatch fails
                }
            }

            // Direct WhatsApp chat URL intent
            val encodedText = java.net.URLEncoder.encode(message, "UTF-8")
            val url = "https://api.whatsapp.com/send?phone=$formattedNumber&text=$encodedText"
            val textIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                if (targetPackage != null) {
                    setPackage(targetPackage)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(textIntent)
                return true
            } catch (e: Exception) {
                // Fallback to general text share
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                }
                val chooser = Intent.createChooser(shareIntent, "दैनिक उपस्थिति विवरण साझा करें")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
                return true
            }
        } catch (e: Exception) {
            Toast.makeText(context, "व्हाट्सएप खोलने में त्रुटि: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            return false
        }
    }

    /**
     * Send monthly reports directly via Gmail app (to Block Education Officer / Administration Office)
     * Supports sending all report PDFs (Format 1, Format 2, and Complete Report) at once directly in Gmail.
     */
     fun shareReportsViaEmail(
         context: Context,
         files: List<File>,
         emailAddress: String,
         subject: String,
         bodyText: String
     ): Boolean {
         try {
             val validFiles = files.filter { it.exists() }
             if (validFiles.isEmpty()) {
                 Toast.makeText(context, "रिपोर्ट फ़ाइल नहीं मिली", Toast.LENGTH_SHORT).show()
                 return false
             }

             val uris = arrayListOf<Uri>()
             for (f in validFiles) {
                 uris.add(
                     FileProvider.getUriForFile(
                         context,
                         "${context.packageName}.fileprovider",
                         f
                     )
                 )
             }

             val gmailPackage = "com.google.android.gm"

             // Explicitly grant read uri permission to Gmail package for each attachment URI
             for (uri in uris) {
                 try {
                     context.grantUriPermission(gmailPackage, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                 } catch (_: Exception) {}
             }

             val gmailIntent = Intent(if (uris.size > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND).apply {
                 setPackage(gmailPackage)
                 type = "message/rfc822"
                 putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
                 putExtra(Intent.EXTRA_SUBJECT, subject)
                 putExtra(Intent.EXTRA_TEXT, bodyText)
                 if (uris.size > 1) {
                     putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                 } else {
                     putExtra(Intent.EXTRA_STREAM, uris[0])
                 }
                 if (uris.isNotEmpty()) {
                     val clipData = ClipData.newRawUri("MDM Reports", uris[0])
                     for (i in 1 until uris.size) {
                         clipData.addItem(ClipData.Item(uris[i]))
                     }
                     this.clipData = clipData
                 }
                 addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                 addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
             }

             try {
                 context.startActivity(gmailIntent)
                 return true
             } catch (e: ActivityNotFoundException) {
                 // If Gmail app is not available, fallback to general email chooser
                 val fallbackIntent = Intent(if (uris.size > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND).apply {
                     type = "message/rfc822"
                     putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
                     putExtra(Intent.EXTRA_SUBJECT, subject)
                     putExtra(Intent.EXTRA_TEXT, bodyText)
                     if (uris.size > 1) {
                         putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                     } else {
                         putExtra(Intent.EXTRA_STREAM, uris[0])
                     }
                     if (uris.isNotEmpty()) {
                         val clipData = ClipData.newRawUri("MDM Reports", uris[0])
                         for (i in 1 until uris.size) {
                             clipData.addItem(ClipData.Item(uris[i]))
                         }
                         this.clipData = clipData
                     }
                     addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                     addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                 }
                 val chooser = Intent.createChooser(fallbackIntent, "ईमेल द्वारा रिपोर्ट भेजें")
                 chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                 context.startActivity(chooser)
                 return true
             }
         } catch (e: Exception) {
             Toast.makeText(context, "ईमेल भेजने में त्रुटि: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
             return false
         }
     }

    /**
     * Share multiple report PDFs via general Android Share sheet (Drive, Nearby, Bluetooth, etc.)
     */
    fun shareMultiplePdfs(context: Context, files: List<File>, title: String = "मध्यान्ह भोजन मासिक रिपोर्ट प्रपत्र") {
        try {
            val validFiles = files.filter { it.exists() }
            if (validFiles.isEmpty()) return

            val uris = arrayListOf<Uri>()
            for (f in validFiles) {
                uris.add(
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        f
                    )
                )
            }

            val intent = if (uris.size > 1) {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "application/pdf"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_TEXT, "$title - समस्त मासिक प्रपत्र")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uris[0])
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_TEXT, "$title (${validFiles[0].name})")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            context.startActivity(Intent.createChooser(intent, "समस्त रिपोर्ट शेयर करें (Share All Reports)"))
        } catch (e: Exception) {
            Toast.makeText(context, "शेयर करने में त्रुटि: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Open PDF directly with any installed PDF viewer
     */
    fun openPdf(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "PDF व्यूअर उपलब्ध नहीं है, कृपया शेयर विकल्प का उपयोग करें", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Native Android Print / Save as PDF
     */
    fun printPdf(context: Context, file: File, jobName: String = "MDM_Monthly_Report") {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(context, "प्रिंट सेवा अनुपलब्ध है", Toast.LENGTH_SHORT).show()
            return
        }

        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(file.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                var input: InputStream? = null
                var output: OutputStream? = null
                try {
                    input = FileInputStream(file)
                    output = FileOutputStream(destination?.fileDescriptor)
                    val buf = ByteArray(16384)
                    var bytesRead: Int
                    while (input.read(buf).also { bytesRead = it } >= 0) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onWriteCancelled()
                            return
                        }
                        output.write(buf, 0, bytesRead)
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                } finally {
                    try { input?.close() } catch (_: Exception) {}
                    try { output?.close() } catch (_: Exception) {}
                }
            }
        }

        printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
    }

    // =========================================================================
    // DRAWING IMPLEMENTATION FOR FORMAT 1 (PAGE 1)
    // =========================================================================
    private fun drawFormat1Page1(canvas: Canvas, width: Float, height: Float, data: MonthlyReportData) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.BLACK
            strokeWidth = 0.8f
        }

        // Draw Outer Page Border
        val margin = 18f
        val right = width - margin
        canvas.drawRect(margin, margin, right, height - margin, stroke)

        var y = margin + 14f

        // Top Logos & Header
        drawEmblemLogo(canvas, margin + 12f, y + 16f, 18f)
        drawMidDayMealLogo(canvas, right - 28f, y + 16f, 18f)

        // Center Title
        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        p.textSize = 11f
        p.color = Color.BLACK
        canvas.drawText("मासिक प्रपत्र", width / 2f, y + 8f, p)
        p.textSize = 10f
        canvas.drawText("मध्यान्ह भोजन योजना", width / 2f, y + 21f, p)
        p.textSize = 9.5f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("शाला मासिक जानकारी प्रपत्र", width / 2f, y + 33f, p)

        y += 42f

        // School UDISE Digit Boxes & Month Row
        p.textAlign = Paint.Align.LEFT
        p.textSize = 8.5f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("स्कुल कोड -", margin + 6f, y + 11f, p)

        val udiseCode = (data.school?.udiseCode ?: "22080100308").padEnd(11, '0').take(11)
        val boxStartX = margin + 65f
        val boxWidth = 14f
        val boxHeight = 15f
        for (i in 0 until 11) {
            val bx = boxStartX + (i * boxWidth)
            canvas.drawRect(bx, y, bx + boxWidth, y + boxHeight, stroke)
            p.textAlign = Paint.Align.CENTER
            p.textSize = 9f
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val char = if (i < udiseCode.length) udiseCode[i].toString() else ""
            canvas.drawText(char, bx + (boxWidth / 2f), y + 11f, p)
        }

        // Horizontal divider below code
        y += 18f
        canvas.drawLine(margin, y, right, y, stroke)

        // Month & Entry Month
        p.textAlign = Paint.Align.LEFT
        p.textSize = 8.5f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("माह-", margin + 6f, y + 12f, p)

        // Highlight Box for Month
        val monthBoxLeft = margin + 45f
        val monthBoxRight = margin + 175f
        val bgPaint = Paint().apply { color = Color.rgb(254, 240, 138) } // Yellow
        canvas.drawRect(monthBoxLeft, y + 2f, monthBoxRight, y + 15f, bgPaint)
        p.textAlign = Paint.Align.CENTER
        canvas.drawText(formatMonthDisplay(data.selectedMonth), (monthBoxLeft + monthBoxRight) / 2f, y + 11f, p)

        p.textAlign = Paint.Align.LEFT
        canvas.drawText("एंटी माह -", margin + 200f, y + 12f, p)
        val entryBoxRight = margin + 340f
        canvas.drawRect(margin + 245f, y + 2f, entryBoxRight, y + 15f, bgPaint)
        p.textAlign = Paint.Align.CENTER
        canvas.drawText(formatMonthDisplay(data.selectedMonth), (margin + 245f + entryBoxRight) / 2f, y + 11f, p)

        y += 18f
        canvas.drawLine(margin, y, right, y, stroke)

        // 12 Items School Master Info Grid (2 columns: left and right)
        val midX = margin + (right - margin) / 2f
        val rowHeight = 15f

        val totalMealsServed = data.dailyMeals.sumOf { it.studentsServed }
        val mdmDaysCount = data.dailyMeals.count { it.studentsServed > 0 }
        val totalStudents = data.enrollment?.totalEnrollment ?: 0

        val infoItems = listOf(
            Pair("1  विकासखंड का नाम -", data.school?.blockName?.ifBlank { "-" } ?: "-"),
            Pair("2  संकुल केंद्र का नाम -", data.school?.clusterName?.ifBlank { "-" } ?: "-"),
            Pair("3  शहर का नाम -", data.school?.villageName?.ifBlank { "-" } ?: "-"),
            Pair("4  शाला का नाम -", data.school?.schoolName?.ifBlank { "-" } ?: "-"),
            Pair("5  शाला का स्तर -", data.school?.schoolType?.ifBlank { "-" } ?: "-"),
            Pair("6  कुल विद्यार्थी -", "$totalStudents"),
            Pair("7  माह में कुल लाभान्वित की संख्या -", "$totalMealsServed"),
            Pair("8  मध्यान्ह भोजन दिवस की संख्या -", "$mdmDaysCount"),
            Pair("9  राशन दुकान का नाम -", data.pdsShop?.shopName?.ifBlank { "-" } ?: "-"),
            Pair("10 मध्यान्ह भोजन संचालन एजेंसी का नाम -", data.agency?.name?.ifBlank { "-" } ?: "-"),
            Pair("11 मध्यान्ह भोजन संचालन एजेंसी का खाता नम्बर -", data.agency?.maskedAccountNo?.ifBlank { "-" } ?: "-"),
            Pair("12 बैंक का नाम -", data.agency?.bankName?.let { if (data.agency.branchName.isNotBlank()) "$it, शाखा ${data.agency.branchName}" else it } ?: "-")
        )

        for (i in 0 until 6) {
            val leftItem = infoItems[i * 2]
            val rightItem = infoItems[i * 2 + 1]
            val rowY = y + (i * rowHeight)

            // Draw horizontal row line
            canvas.drawLine(margin, rowY, right, rowY, stroke)

            // Left Cell
            p.textAlign = Paint.Align.LEFT
            p.textSize = 7.5f
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(leftItem.first, margin + 4f, rowY + 10.5f, p)
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            p.textAlign = Paint.Align.RIGHT
            val leftVal = truncateText(leftItem.second, 28)
            canvas.drawText(leftVal, midX - 6f, rowY + 10.5f, p)

            // Right Cell
            p.textAlign = Paint.Align.LEFT
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(rightItem.first, midX + 4f, rowY + 10.5f, p)
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            p.textAlign = Paint.Align.RIGHT
            val rightVal = truncateText(rightItem.second, 28)
            canvas.drawText(rightVal, right - 6f, rowY + 10.5f, p)
        }

        // Draw vertical center line for info grid
        canvas.drawLine(midX, y, midX, y + (6 * rowHeight), stroke)
        y += (6 * rowHeight)
        canvas.drawLine(margin, y, right, y, stroke)

        // -------------------------------------------------------------
        // SECTION 1: विद्यार्थियों की दैनिक उपस्थिति (दिनांक - 1st TO 31st)
        // -------------------------------------------------------------
        y += 4f
        p.textAlign = Paint.Align.LEFT
        p.textSize = 8f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("1  विद्यार्थियों की दैनिक उपस्थिति (दिनांक - 1st TO 31st)", margin + 4f, y + 8f, p)
        y += 12f

        val dailyAttendanceMap = getDailyAttendanceMap(data.selectedMonth, data.dailyMeals, totalStudents)
        val dailyBeneficiaryMap = getDailyBeneficiaryMap(data.selectedMonth, data.dailyMeals)

        y = drawDailyAttendanceGrid(canvas, margin, right, y, stroke, p, dailyAttendanceMap)

        // -------------------------------------------------------------
        // SECTION 2: लाभान्वित विद्यार्थियों की दैनिक उपस्थिति (दिनांक - 1st To 31st)
        // -------------------------------------------------------------
        y += 4f
        p.textAlign = Paint.Align.LEFT
        p.textSize = 8f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("2  लाभान्वित विद्यार्थियों की दैनिक उपस्थिति (दिनांक - 1st To 31st)", margin + 4f, y + 8f, p)
        y += 12f

        y = drawDailyAttendanceGrid(canvas, margin, right, y, stroke, p, dailyBeneficiaryMap)

        // -------------------------------------------------------------
        // SECTION 3: अनाज का विवरण (किग्रा में) (Rice Ledger)
        // -------------------------------------------------------------
        y += 4f
        p.textAlign = Paint.Align.LEFT
        p.textSize = 8f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("3  अनाज का विवरण (किग्रा में)", margin + 4f, y + 8f, p)
        y += 12f

        val monthReceiptsRice = data.receipts.sumOf { it.quantityKg }
        val riceNormGrams = data.configNorms?.primaryRiceNormGrams ?: 110.0
        val riceNormKg = riceNormGrams / 1000.0
        val riceConsumed = data.dailyMeals.sumOf { it.riceConsumedKg.takeIf { c -> c > 0 } ?: (it.studentsServed * riceNormKg) }
        val openingRice = data.openingRiceStockKg
        val totalRiceStock = openingRice + monthReceiptsRice
        val closingRice = (totalRiceStock - riceConsumed).coerceAtLeast(0.0)

        y = drawStockLedgerTable(
            canvas = canvas,
            margin = margin,
            right = right,
            startY = y,
            stroke = stroke,
            p = p,
            headers = listOf("पूर्व माह का शेष चावल", "माह में प्राप्त चावल", "कुल चावल का स्टॉक", "माह में चावल व्यय", "माह के अंत में चावल का शेष स्टॉक", "रिमार्क"),
            values = listOf(
                formatDecimal(openingRice),
                formatDecimal(monthReceiptsRice),
                formatDecimal(totalRiceStock),
                formatDecimal(riceConsumed),
                formatDecimal(closingRice),
                "NA"
            )
        )

        // -------------------------------------------------------------
        // SECTION 4: सोयाबड़ी का विवरण (किग्रा में)
        // -------------------------------------------------------------
        y += 4f
        p.textAlign = Paint.Align.LEFT
        p.textSize = 8f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("4  सोयाबडी का विवरण (किग्रा में)", margin + 4f, y + 8f, p)
        y += 12f

        val customItemsList = com.example.data.model.CustomFoodItemParser.parse(data.configNorms?.customItemsJson)
        val soyabadiNorm = customItemsList.firstOrNull { it.id.contains("badi", ignoreCase = true) || it.id.contains("soya", ignoreCase = true) }?.quantity ?: 0.025
        val soyabadiConsumed = data.dailyMeals.filter { it.studentsServed > 0 }.sumOf { record ->
            val usedSet = com.example.data.model.CustomFoodItemParser.parseUsedItemIds(record.customItemsUsedJson)
            val isUsed = if (record.customItemsUsedJson.isNotBlank()) usedSet.any { it.contains("badi", ignoreCase = true) || it.contains("soya", ignoreCase = true) } else false
            if (isUsed) record.studentsServed * soyabadiNorm else 0.0
        }

        y = drawStockLedgerTable(
            canvas = canvas,
            margin = margin,
            right = right,
            startY = y,
            stroke = stroke,
            p = p,
            headers = listOf("पूर्व माह का शेष", "माह में प्राप्त सोया बड़ी", "कुल स्टॉक", "माह में सोया बड़ी व्यय", "माह के अंत में सोया बड़ी का शेष स्टॉक", "रिमार्क"),
            values = listOf("0.000", "0.000", "0.000", formatDecimal(soyabadiConsumed), "0.000", "NA")
        )

        // -------------------------------------------------------------
        // SECTION 5: सोयादूध का विवरण (लीटर में)
        // -------------------------------------------------------------
        y += 4f
        p.textAlign = Paint.Align.LEFT
        p.textSize = 8f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("5  सोयादूध (Soya Milk) का विवरण (लीटर में)", margin + 4f, y + 8f, p)
        y += 12f

        val soyadudhNorm = customItemsList.firstOrNull { it.id.contains("dudh", ignoreCase = true) || it.name.contains("milk", ignoreCase = true) }?.quantity ?: 0.200
        val soyadudhConsumed = data.dailyMeals.filter { it.studentsServed > 0 }.sumOf { record ->
            val usedSet = com.example.data.model.CustomFoodItemParser.parseUsedItemIds(record.customItemsUsedJson)
            val isUsed = if (record.customItemsUsedJson.isNotBlank()) usedSet.any { it.contains("dudh", ignoreCase = true) || it.contains("milk", ignoreCase = true) } else false
            if (isUsed) record.studentsServed * soyadudhNorm else 0.0
        }

        y = drawStockLedgerTable(
            canvas = canvas,
            margin = margin,
            right = right,
            startY = y,
            stroke = stroke,
            p = p,
            headers = listOf("पूर्व माह का शेष", "माह में प्राप्त सोया दूध (Soya Milk)", "कुल स्टॉक", "माह में व्यय", "माह के अंत में शेष स्टॉक", "रिमार्क"),
            values = listOf("0.000", "0.000", "0.000", formatDecimal(soyadudhConsumed), "0.000", "NA")
        )

        // -------------------------------------------------------------
        // SECTION 6 (4 in Form): आयरन फोलिक एसिड व कृमिनाशक टेबलेट का विवरण (नग में)
        // -------------------------------------------------------------
        y += 4f
        p.textAlign = Paint.Align.LEFT
        p.textSize = 8f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("4  आयरन फोलिक एसिड व कृमिनाशक टेबलेट का विवरण. (नग में)", margin + 4f, y + 8f, p)
        y += 12f

        drawTabletsLedgerTable(canvas, margin, right, y, stroke, p, data)
    }

    // =========================================================================
    // DRAWING IMPLEMENTATION FOR FORMAT 1 (PAGE 2)
    // =========================================================================
    private fun drawFormat1Page2(canvas: Canvas, width: Float, height: Float, data: MonthlyReportData) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.BLACK
            strokeWidth = 0.8f
        }

        val margin = 18f
        val right = width - margin
        canvas.drawRect(margin, margin, right, height - margin, stroke)

        var y = margin + 14f

        // Top Logos & Header
        drawEmblemLogo(canvas, margin + 12f, y + 16f, 18f)
        drawMidDayMealLogo(canvas, right - 28f, y + 16f, 18f)

        // Center Title
        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        p.textSize = 11f
        p.color = Color.BLACK
        canvas.drawText("मासिक प्रपत्र", width / 2f, y + 8f, p)
        p.textSize = 10f
        canvas.drawText("मध्यान्ह भोजन योजना", width / 2f, y + 21f, p)
        p.textSize = 9.5f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("शाला मासिक जानकारी प्रपत्र", width / 2f, y + 33f, p)

        y += 42f

        // School Code & Month Row
        p.textAlign = Paint.Align.LEFT
        p.textSize = 8.5f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("स्कुल कोड -", margin + 6f, y + 11f, p)

        val udiseCode = (data.school?.udiseCode ?: "22080100308").padEnd(11, '0').take(11)
        val boxStartX = margin + 65f
        val boxWidth = 14f
        val boxHeight = 15f
        for (i in 0 until 11) {
            val bx = boxStartX + (i * boxWidth)
            canvas.drawRect(bx, y, bx + boxWidth, y + boxHeight, stroke)
            p.textAlign = Paint.Align.CENTER
            p.textSize = 9f
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val char = if (i < udiseCode.length) udiseCode[i].toString() else ""
            canvas.drawText(char, bx + (boxWidth / 2f), y + 11f, p)
        }

        y += 18f
        canvas.drawLine(margin, y, right, y, stroke)

        p.textAlign = Paint.Align.LEFT
        p.textSize = 8.5f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("माह-", margin + 6f, y + 12f, p)

        val monthBoxLeft = margin + 45f
        val monthBoxRight = margin + 175f
        val bgPaint = Paint().apply { color = Color.rgb(254, 240, 138) }
        canvas.drawRect(monthBoxLeft, y + 2f, monthBoxRight, y + 15f, bgPaint)
        p.textAlign = Paint.Align.CENTER
        canvas.drawText(formatMonthDisplay(data.selectedMonth), (monthBoxLeft + monthBoxRight) / 2f, y + 11f, p)

        p.textAlign = Paint.Align.LEFT
        canvas.drawText("एंटी माह -", margin + 200f, y + 12f, p)
        val entryBoxRight = margin + 340f
        canvas.drawRect(margin + 245f, y + 2f, entryBoxRight, y + 15f, bgPaint)
        p.textAlign = Paint.Align.CENTER
        canvas.drawText(formatMonthDisplay(data.selectedMonth), (margin + 245f + entryBoxRight) / 2f, y + 11f, p)

        y += 24f

        // -------------------------------------------------------------
        // TEACHER CENSUS & STUDENT CENSUS SIDE-BY-SIDE OR INTEGRATED TABLE
        // -------------------------------------------------------------
        drawPage2CensusTable(canvas, margin, right, y, stroke, p, data)

        // Draw Head Master Signature Stamp at bottom right
        val stampX = right - 110f
        val stampY = y + 260f
        drawOfficialHeadMasterStamp(
            canvas = canvas,
            x = stampX,
            y = stampY,
            school = data.school,
            headMasterName = data.headMasterName ?: data.school?.headTeacherName ?: "",
            headMasterDesignation = data.headMasterDesignation,
            signDate = data.signDate,
            isSigned = data.isSignedByHeadMaster,
            signatureBitmap = data.signatureBitmap
        )
    }

    // =========================================================================
    // DRAWING IMPLEMENTATION FOR FORMAT 2 (LANDSCAPE SUMMARY LEDGER)
    // =========================================================================
    private fun drawFormat2Landscape(canvas: Canvas, width: Float, height: Float, data: MonthlyReportData) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.BLACK
            strokeWidth = 0.8f
        }

        val margin = 16f
        val right = width - margin
        canvas.drawRect(margin, margin, right, height - margin, stroke)

        var y = margin + 12f

        // Header Section
        drawEmblemLogo(canvas, margin + 18f, y + 20f, 20f)
        drawMidDayMealLogo(canvas, right - 32f, y + 20f, 20f)

        // Center Titles
        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        p.textSize = 12f
        p.color = Color.BLACK
        val schoolTitle = "${data.school?.schoolName?.ifBlank { "-" } ?: "-"}, जिला- ${data.school?.districtName?.ifBlank { "-" } ?: "-"}"
        canvas.drawText(schoolTitle, width / 2f, y + 10f, p)

        p.textSize = 9.5f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(data.school?.udiseCode?.ifBlank { "" } ?: "", width / 2f, y + 22f, p)

        p.textSize = 10.5f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("मध्यान्ह भोजन योजना प्रपत्र", width / 2f, y + 36f, p)

        // Left Month Tag
        p.textAlign = Paint.Align.LEFT
        p.textSize = 9f
        canvas.drawText("माह", margin + 150f, y + 36f, p)
        val mBoxL = margin + 172f
        val mBoxR = margin + 240f
        val bgPaint = Paint().apply { color = Color.rgb(254, 240, 138) }
        canvas.drawRect(mBoxL, y + 26f, mBoxR, y + 40f, bgPaint)
        p.textAlign = Paint.Align.CENTER
        canvas.drawText(formatMonthDisplay(data.selectedMonth), (mBoxL + mBoxR) / 2f, y + 36.5f, p)

        // Right Meta stats
        val cookingCostRate = if (data.configNorms != null) {
            if ((data.configNorms.middleReimbursementRate ?: 0.0) > 0.0) data.configNorms.middleReimbursementRate
            else (data.configNorms.primaryReimbursementRate ?: 0.0)
        } else 0.0
        val totalBeneficiaries = data.dailyMeals.sumOf { it.studentsServed }
        val mdmDays = data.dailyMeals.count { it.studentsServed > 0 }
        val boysBeneficiaries = data.dailyMeals.sumOf { it.boysServed }
        val girlsBeneficiaries = data.dailyMeals.sumOf { it.girlsServed }
        val totalStudents = data.enrollment?.totalEnrollment ?: 0
        val boysStudents = data.enrollment?.totalBoys ?: 0
        val girlsStudents = data.enrollment?.totalGirls ?: 0
        val avgAttendance = if (mdmDays > 0) totalBeneficiaries / mdmDays else 0

        p.textAlign = Paint.Align.LEFT
        p.textSize = 8.5f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("कुकिंग कॉस्ट की दर -", right - 240f, y + 14f, p)
        p.textAlign = Paint.Align.RIGHT
        val rateDisplay = if (cookingCostRate > 0.0) String.format(Locale.getDefault(), "%.2f", cookingCostRate) else "0.00"
        canvas.drawText(rateDisplay, right - 130f, y + 14f, p)

        p.textAlign = Paint.Align.LEFT
        canvas.drawText("माह में कुल लाभान्वित -", right - 240f, y + 26f, p)
        p.textAlign = Paint.Align.RIGHT
        canvas.drawText("$totalBeneficiaries", right - 130f, y + 26f, p)

        p.textAlign = Paint.Align.LEFT
        canvas.drawText("मध्यान्ह भोजन दिवस की संख्या -", right - 240f, y + 38f, p)
        p.textAlign = Paint.Align.RIGHT
        canvas.drawText("$mdmDays", right - 130f, y + 38f, p)

        y += 48f

        // -------------------------------------------------------------
        // MAIN 20-COLUMN LEDGER TABLE
        // -------------------------------------------------------------
        val openingRice = data.openingRiceStockKg
        val receivedRice = data.receipts.sumOf { it.quantityKg }
        val totalRiceStock = openingRice + receivedRice
        val riceNormGrams = data.configNorms?.primaryRiceNormGrams ?: 150.0
        val riceNormKg = riceNormGrams / 1000.0
        val consumedRice = data.dailyMeals.sumOf { it.riceConsumedKg.takeIf { c -> c > 0 } ?: (it.studentsServed * riceNormKg) }
        val closingRice = (totalRiceStock - consumedRice).coerceAtLeast(0.0)
        val totalCostAmount = totalBeneficiaries * cookingCostRate

        y = drawMain20ColumnTable(
            canvas = canvas,
            startX = margin,
            endX = right,
            startY = y,
            stroke = stroke,
            p = p,
            school = data.school,
            agency = data.agency,
            totalStudents = totalStudents,
            boysStudents = boysStudents,
            girlsStudents = girlsStudents,
            avgAttendance = avgAttendance,
            mdmDays = mdmDays,
            beneficiariesBoys = boysBeneficiaries,
            beneficiariesGirls = girlsBeneficiaries,
            beneficiariesTotal = totalBeneficiaries,
            openingRice = openingRice,
            receivedRice = receivedRice,
            totalRiceStock = totalRiceStock,
            consumedRice = consumedRice,
            closingRice = closingRice,
            totalCostAmount = totalCostAmount
        )

        y += 18f

        // -------------------------------------------------------------
        // BOTTOM SECTION: COOK DETAILS (LEFT) & CLASS MATRIX (RIGHT)
        // -------------------------------------------------------------
        val bottomWidth = right - margin
        val leftTableWidth = bottomWidth * 0.38f
        val rightTableWidth = bottomWidth * 0.60f

        drawCooksTable(
            canvas = canvas,
            startX = margin + 10f,
            startY = y,
            width = leftTableWidth,
            stroke = stroke,
            p = p,
            cooks = data.cooks
        )

        drawClassMatrixTable(
            canvas = canvas,
            startX = margin + leftTableWidth + 24f,
            startY = y,
            width = rightTableWidth - 10f,
            stroke = stroke,
            p = p,
            enrollment = data.enrollment
        )

        // Draw Head Master Signature Stamp
        drawOfficialHeadMasterStamp(
            canvas = canvas,
            x = right - 115f,
            y = height - 85f,
            school = data.school,
            headMasterName = data.headMasterName ?: data.school?.headTeacherName ?: "",
            headMasterDesignation = data.headMasterDesignation,
            signDate = data.signDate,
            isSigned = data.isSignedByHeadMaster,
            signatureBitmap = data.signatureBitmap
        )
    }

    // =========================================================================
    // HELPER TABLE DRAWING METHODS
    // =========================================================================

    private fun drawDailyAttendanceGrid(
        canvas: Canvas,
        startX: Float,
        endX: Float,
        startY: Float,
        stroke: Paint,
        p: Paint,
        valuesMap: Map<Int, Int>
    ): Float {
        val totalWidth = endX - startX
        val colWidth = totalWidth / 17f // 16 days + 1 extra for row 2 total
        val rowH = 14f

        var curY = startY

        // Row 1 Header: 1 to 16
        canvas.drawRect(startX, curY, endX, curY + (rowH * 2), stroke)
        canvas.drawLine(startX, curY + rowH, endX, curY + rowH, stroke)

        for (i in 0..16) {
            val cx = startX + (i * colWidth)
            canvas.drawLine(cx, curY, cx, curY + (rowH * 2), stroke)

            if (i < 16) {
                val day = i + 1
                p.textAlign = Paint.Align.CENTER
                p.textSize = 7.5f
                p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("$day", cx + (colWidth / 2f), curY + 10f, p)

                p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                val v = valuesMap[day] ?: 0
                canvas.drawText("$v", cx + (colWidth / 2f), curY + rowH + 10f, p)
            }
        }

        curY += (rowH * 2)

        // Row 2 Header: 17 to 31 + Total
        canvas.drawRect(startX, curY, endX, curY + (rowH * 2), stroke)
        canvas.drawLine(startX, curY + rowH, endX, curY + rowH, stroke)

        var totalSum = 0
        for (i in 0..16) {
            val cx = startX + (i * colWidth)
            canvas.drawLine(cx, curY, cx, curY + (rowH * 2), stroke)

            if (i < 15) {
                val day = i + 17
                p.textAlign = Paint.Align.CENTER
                p.textSize = 7.5f
                p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("$day", cx + (colWidth / 2f), curY + 10f, p)

                p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                val v = valuesMap[day] ?: 0
                totalSum += (valuesMap[day] ?: 0)
                canvas.drawText("$v", cx + (colWidth / 2f), curY + rowH + 10f, p)
            } else if (i == 15) {
                // Day 31
                val day = 31
                p.textAlign = Paint.Align.CENTER
                p.textSize = 7.5f
                p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("$day", cx + (colWidth / 2f), curY + 10f, p)

                p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                val v = valuesMap[day] ?: 0
                totalSum += v
                canvas.drawText("$v", cx + (colWidth / 2f), curY + rowH + 10f, p)
            } else {
                // Total (कुल)
                p.textAlign = Paint.Align.CENTER
                p.textSize = 8f
                p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("कुल", cx + (colWidth / 2f), curY + 10f, p)

                val fullTotal = valuesMap.values.sum()
                canvas.drawText("$fullTotal", cx + (colWidth / 2f), curY + rowH + 10f, p)
            }
        }

        return curY + (rowH * 2) + 4f
    }

    private fun drawStockLedgerTable(
        canvas: Canvas,
        margin: Float,
        right: Float,
        startY: Float,
        stroke: Paint,
        p: Paint,
        headers: List<String>,
        values: List<String>
    ): Float {
        val totalWidth = right - margin
        val colWidths = floatArrayOf(
            totalWidth * 0.17f,
            totalWidth * 0.17f,
            totalWidth * 0.17f,
            totalWidth * 0.17f,
            totalWidth * 0.22f,
            totalWidth * 0.10f
        )
        val rowH = 16f
        val headerH = 22f

        // Draw Table Outline & Dividing Horizontal Lines
        canvas.drawRect(margin, startY, right, startY + headerH + rowH, stroke)
        canvas.drawLine(margin, startY + headerH, right, startY + headerH, stroke)

        var curX = margin
        for (i in headers.indices) {
            val w = colWidths[i]
            if (i > 0) {
                canvas.drawLine(curX, startY, curX, startY + headerH + rowH, stroke)
            }

            p.textAlign = Paint.Align.CENTER
            p.textSize = 7f
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            // Header multiline text
            drawCenteredMultiLineText(canvas, headers[i], curX + (w / 2f), startY + 6f, w - 4f, p, 8f)

            // Value text
            p.textSize = 7.5f
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val valY = startY + headerH + 11.5f
            canvas.drawText(values.getOrElse(i) { "" }, curX + (w / 2f), valY, p)

            curX += w
        }

        return startY + headerH + rowH + 4f
    }

    private fun drawTabletsLedgerTable(
        canvas: Canvas,
        margin: Float,
        right: Float,
        startY: Float,
        stroke: Paint,
        p: Paint,
        data: MonthlyReportData
    ): Float {
        val totalWidth = right - margin
        val rowH1 = 14f
        val rowH2 = 14f
        val dataRowH = 14f
        val totalHeaderH = rowH1 + rowH2

        // Main table outline
        canvas.drawRect(margin, startY, right, startY + totalHeaderH + dataRowH, stroke)
        canvas.drawLine(margin, startY + rowH1, right, startY + rowH1, stroke)
        canvas.drawLine(margin, startY + totalHeaderH, right, startY + totalHeaderH, stroke)

        // Column widths for groups
        val g1W = totalWidth * 0.16f // माह में टेबलेट का विवरण (पूर्व माह शेष, प्राप्त, कुल)
        val g2W = totalWidth * 0.18f // विद्यार्थी जिन्होंने टेबलेट खाया (बालक, बालिका, कुल)
        val g3W = totalWidth * 0.18f // शिक्षक जिन्होंने टेबलेट खाया (पुरुष, महिला, कुल)
        val g4W = totalWidth * 0.18f // माह में कुल व्यय (पुरुष, महिला, कुल)
        val g5W = totalWidth * 0.08f // माह में शेष टेबलेट
        val g6W = totalWidth * 0.12f // कृमिनाशक टेबलेट का व्यय (बालक, बालिका)
        val g7W = totalWidth * 0.10f // शिक्षक का विवरण (M+F=T)

        // Draw Main Header Group Labels
        p.textAlign = Paint.Align.CENTER
        p.textSize = 6.5f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        var cx = margin
        canvas.drawText("माह में टेबलेट का विवरण", cx + (g1W / 2f), startY + 10f, p)
        cx += g1W
        canvas.drawLine(cx, startY, cx, startY + totalHeaderH + dataRowH, stroke)

        canvas.drawText("विद्यार्थी जिन्होंने टेबलेट खाया", cx + (g2W / 2f), startY + 10f, p)
        cx += g2W
        canvas.drawLine(cx, startY, cx, startY + totalHeaderH + dataRowH, stroke)

        canvas.drawText("शिक्षक जिन्होंने टेबलेट खाया", cx + (g3W / 2f), startY + 10f, p)
        cx += g3W
        canvas.drawLine(cx, startY, cx, startY + totalHeaderH + dataRowH, stroke)

        canvas.drawText("माह में कुल व्यय", cx + (g4W / 2f), startY + 10f, p)
        cx += g4W
        canvas.drawLine(cx, startY, cx, startY + totalHeaderH + dataRowH, stroke)

        drawCenteredMultiLineText(canvas, "माह में\nशेष\nटेबलेट", cx + (g5W / 2f), startY + 3f, g5W - 2f, p, 7f)
        cx += g5W
        canvas.drawLine(cx, startY, cx, startY + totalHeaderH + dataRowH, stroke)

        canvas.drawText("कृमिनाशक टेबलेट का व्यय", cx + (g6W / 2f), startY + 10f, p)
        cx += g6W
        canvas.drawLine(cx, startY, cx, startY + totalHeaderH + dataRowH, stroke)

        drawCenteredMultiLineText(canvas, "शिक्षक का विवरण\n(M+F=T)", cx + (g7W / 2f), startY + 4f, g7W - 2f, p, 7f)

        // Draw Sub-Headers (Row 2)
        val subH = startY + rowH1
        p.textSize = 6f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        // G1: पूर्व माह शेष, प्राप्त, कुल
        val g1sub = g1W / 3f
        canvas.drawText("पूर्व माह शेष", margin + (g1sub * 0.5f), subH + 10f, p)
        canvas.drawLine(margin + g1sub, subH, margin + g1sub, subH + rowH2 + dataRowH, stroke)
        canvas.drawText("प्राप्त", margin + (g1sub * 1.5f), subH + 10f, p)
        canvas.drawLine(margin + (g1sub * 2), subH, margin + (g1sub * 2), subH + rowH2 + dataRowH, stroke)
        canvas.drawText("कुल", margin + (g1sub * 2.5f), subH + 10f, p)

        // G2: बालक, बालिका, कुल
        var xOffset = margin + g1W
        val g2sub = g2W / 3f
        canvas.drawText("बालक", xOffset + (g2sub * 0.5f), subH + 10f, p)
        canvas.drawLine(xOffset + g2sub, subH, xOffset + g2sub, subH + rowH2 + dataRowH, stroke)
        canvas.drawText("बालिका", xOffset + (g2sub * 1.5f), subH + 10f, p)
        canvas.drawLine(xOffset + (g2sub * 2), subH, xOffset + (g2sub * 2), subH + rowH2 + dataRowH, stroke)
        canvas.drawText("कुल", xOffset + (g2sub * 2.5f), subH + 10f, p)

        // G3: पुरुष, महिला, कुल
        xOffset += g2W
        val g3sub = g3W / 3f
        canvas.drawText("पुरुष", xOffset + (g3sub * 0.5f), subH + 10f, p)
        canvas.drawLine(xOffset + g3sub, subH, xOffset + g3sub, subH + rowH2 + dataRowH, stroke)
        canvas.drawText("महिला", xOffset + (g3sub * 1.5f), subH + 10f, p)
        canvas.drawLine(xOffset + (g3sub * 2), subH, xOffset + (g3sub * 2), subH + rowH2 + dataRowH, stroke)
        canvas.drawText("कुल", xOffset + (g3sub * 2.5f), subH + 10f, p)

        // G4: पुरुष, महिला, कुल
        xOffset += g3W
        val g4sub = g4W / 3f
        canvas.drawText("पुरुष", xOffset + (g4sub * 0.5f), subH + 10f, p)
        canvas.drawLine(xOffset + g4sub, subH, xOffset + g4sub, subH + rowH2 + dataRowH, stroke)
        canvas.drawText("महिला", xOffset + (g4sub * 1.5f), subH + 10f, p)
        canvas.drawLine(xOffset + (g4sub * 2), subH, xOffset + (g4sub * 2), subH + rowH2 + dataRowH, stroke)
        canvas.drawText("कुल", xOffset + (g4sub * 2.5f), subH + 10f, p)

        // G6: बालक, बालिका
        xOffset += g4W + g5W
        val g6sub = g6W / 2f
        canvas.drawText("बालक", xOffset + (g6sub * 0.5f), subH + 10f, p)
        canvas.drawLine(xOffset + g6sub, subH, xOffset + g6sub, subH + rowH2 + dataRowH, stroke)
        canvas.drawText("बालिका", xOffset + (g6sub * 1.5f), subH + 10f, p)

        // Data Row Values
        val dataY = startY + totalHeaderH + 10.5f
        p.textAlign = Paint.Align.CENTER
        p.textSize = 7f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val boys = data.enrollment?.totalBoys ?: 0
        val girls = data.enrollment?.totalGirls ?: 0
        val teachersCount = data.teachers?.totalTeachers ?: 0

        // G1
        canvas.drawText("0", margin + (g1sub * 0.5f), dataY, p)
        canvas.drawText("0", margin + (g1sub * 1.5f), dataY, p)
        canvas.drawText("0", margin + (g1sub * 2.5f), dataY, p)

        // G2
        xOffset = margin + g1W
        canvas.drawText("0", xOffset + (g2sub * 0.5f), dataY, p)
        canvas.drawText("0", xOffset + (g2sub * 1.5f), dataY, p)
        canvas.drawText("0", xOffset + (g2sub * 2.5f), dataY, p)

        // G3
        xOffset += g2W
        canvas.drawText("0", xOffset + (g3sub * 0.5f), dataY, p)
        canvas.drawText("0", xOffset + (g3sub * 1.5f), dataY, p)
        canvas.drawText("0", xOffset + (g3sub * 2.5f), dataY, p)

        // G4
        xOffset += g3W
        canvas.drawText("0", xOffset + (g4sub * 0.5f), dataY, p)
        canvas.drawText("0", xOffset + (g4sub * 1.5f), dataY, p)
        canvas.drawText("0", xOffset + (g4sub * 2.5f), dataY, p)

        // G5 (माह में शेष)
        xOffset += g4W
        canvas.drawText("0", xOffset + (g5W / 2f), dataY, p)

        // G6 (कृमिनाशक व्यय: बालक, बालिका)
        xOffset += g5W
        canvas.drawText("$boys", xOffset + (g6sub * 0.5f), dataY, p)
        canvas.drawText("$girls", xOffset + (g6sub * 1.5f), dataY, p)

        // G7 (शिक्षक M+F=T)
        xOffset += g6W
        canvas.drawText("$teachersCount", xOffset + (g7W / 2f), dataY, p)

        return startY + totalHeaderH + dataRowH + 6f
    }

    private fun drawPage2CensusTable(
        canvas: Canvas,
        margin: Float,
        right: Float,
        startY: Float,
        stroke: Paint,
        p: Paint,
        data: MonthlyReportData
    ) {
        val totalWidth = right - margin
        val leftCensusW = totalWidth * 0.42f
        val rightCensusW = totalWidth * 0.58f

        val rowH = 15f
        val headerH = 28f

        val t = data.teachers
        val e = data.enrollment

        // Draw Table 1: शिक्षक विवरण
        var curY = startY
        p.textAlign = Paint.Align.CENTER
        p.textSize = 9f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawRect(margin, curY, margin + leftCensusW, curY + headerH + (5 * rowH), stroke)
        canvas.drawLine(margin, curY + 14f, margin + leftCensusW, curY + 14f, stroke)
        canvas.drawLine(margin, curY + headerH, margin + leftCensusW, curY + headerH, stroke)

        canvas.drawText("शिक्षक विवरण", margin + (leftCensusW / 2f), curY + 10.5f, p)

        // Teacher Sub Columns
        val tCol0 = leftCensusW * 0.22f // जाति वर्ग
        val tCol1 = leftCensusW * 0.26f // प्रशिक्षित (पुरुष, महिला)
        val tCol2 = leftCensusW * 0.26f // अप्रशिक्षित (पुरुष, महिला)
        val tCol3 = leftCensusW * 0.26f // कुल (पुरुष, महिला)

        var tx = margin + tCol0
        canvas.drawLine(tx, curY + 14f, tx, curY + headerH + (5 * rowH), stroke)

        tx += tCol1
        canvas.drawLine(tx, curY + 14f, tx, curY + headerH + (5 * rowH), stroke)

        tx += tCol2
        canvas.drawLine(tx, curY + 14f, tx, curY + headerH + (5 * rowH), stroke)

        // Subheaders
        p.textSize = 7f
        canvas.drawText("जाति\nवर्ग", margin + (tCol0 / 2f), curY + 22f, p)

        canvas.drawText("प्रशिक्षित", margin + tCol0 + (tCol1 / 2f), curY + 20f, p)
        canvas.drawLine(margin + tCol0, curY + 21f, margin + tCol0 + tCol1, curY + 21f, stroke)
        canvas.drawText("पुरुष", margin + tCol0 + (tCol1 * 0.25f), curY + 27.5f, p)
        canvas.drawLine(margin + tCol0 + (tCol1 * 0.5f), curY + 21f, margin + tCol0 + (tCol1 * 0.5f), curY + headerH + (5 * rowH), stroke)
        canvas.drawText("महिला", margin + tCol0 + (tCol1 * 0.75f), curY + 27.5f, p)

        canvas.drawText("अप्रशिक्षित", margin + tCol0 + tCol1 + (tCol2 / 2f), curY + 20f, p)
        canvas.drawLine(margin + tCol0 + tCol1, curY + 21f, margin + tCol0 + tCol1 + tCol2, curY + 21f, stroke)
        canvas.drawText("पुरुष", margin + tCol0 + tCol1 + (tCol2 * 0.25f), curY + 27.5f, p)
        canvas.drawLine(margin + tCol0 + tCol1 + (tCol2 * 0.5f), curY + 21f, margin + tCol0 + tCol1 + (tCol2 * 0.5f), curY + headerH + (5 * rowH), stroke)
        canvas.drawText("महिला", margin + tCol0 + tCol1 + (tCol2 * 0.75f), curY + 27.5f, p)

        canvas.drawText("कुल", margin + tCol0 + tCol1 + tCol2 + (tCol3 / 2f), curY + 20f, p)
        canvas.drawLine(margin + tCol0 + tCol1 + tCol2, curY + 21f, margin + tCol0 + tCol1 + tCol2 + tCol3, curY + 21f, stroke)
        canvas.drawText("पुरुष", margin + tCol0 + tCol1 + tCol2 + (tCol3 * 0.25f), curY + 27.5f, p)
        canvas.drawLine(margin + tCol0 + tCol1 + tCol2 + (tCol3 * 0.5f), curY + 21f, margin + tCol0 + tCol1 + tCol2 + (tCol3 * 0.5f), curY + headerH + (5 * rowH), stroke)
        canvas.drawText("महिला", margin + tCol0 + tCol1 + tCol2 + (tCol3 * 0.75f), curY + 27.5f, p)

        // Teacher Rows: अजजा, अजा, अपिव, सामा., कुल
        val tRows = if (t != null && t.totalTeachers > 0) {
            listOf(
                listOf("अजजा", "${t.stTrainedMale}", "${t.stTrainedFemale}", "${t.stUntrainedMale}", "${t.stUntrainedFemale}", "${t.stTrainedMale + t.stUntrainedMale}", "${t.stTrainedFemale + t.stUntrainedFemale}"),
                listOf("अजा", "${t.scTrainedMale}", "${t.scTrainedFemale}", "${t.scUntrainedMale}", "${t.scUntrainedFemale}", "${t.scTrainedMale + t.scUntrainedMale}", "${t.scTrainedFemale + t.scUntrainedFemale}"),
                listOf("अपिव", "${t.obcTrainedMale}", "${t.obcTrainedFemale}", "${t.obcUntrainedMale}", "${t.obcUntrainedFemale}", "${t.obcTrainedMale + t.obcUntrainedMale}", "${t.obcTrainedFemale + t.obcUntrainedFemale}"),
                listOf("सामा.", "${t.genTrainedMale}", "${t.genTrainedFemale}", "${t.genUntrainedMale}", "${t.genUntrainedFemale}", "${t.genTrainedMale + t.genUntrainedMale}", "${t.genTrainedFemale + t.genUntrainedFemale}"),
                listOf("कुल", "${t.trainedCount}", "${t.femaleCount}", "${t.untrainedCount}", "0", "${t.maleCount}", "${t.femaleCount}")
            )
        } else {
            listOf(
                listOf("अजजा", "0", "0", "0", "0", "0", "0"),
                listOf("अजा", "0", "0", "0", "0", "0", "0"),
                listOf("अपिव", "0", "0", "0", "0", "0", "0"),
                listOf("सामा.", "0", "0", "0", "0", "0", "0"),
                listOf("कुल", "0", "0", "0", "0", "0", "0")
            )
        }

        for (i in tRows.indices) {
            val rY = curY + headerH + (i * rowH)
            canvas.drawLine(margin, rY, margin + leftCensusW, rY, stroke)
            val row = tRows[i]
            p.typeface = if (i == 4) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            p.textSize = 7.5f

            canvas.drawText(row[0], margin + (tCol0 / 2f), rY + 11f, p)
            canvas.drawText(row[1], margin + tCol0 + (tCol1 * 0.25f), rY + 11f, p)
            canvas.drawText(row[2], margin + tCol0 + (tCol1 * 0.75f), rY + 11f, p)
            canvas.drawText(row[3], margin + tCol0 + tCol1 + (tCol2 * 0.25f), rY + 11f, p)
            canvas.drawText(row[4], margin + tCol0 + tCol1 + (tCol2 * 0.75f), rY + 11f, p)
            canvas.drawText(row[5], margin + tCol0 + tCol1 + tCol2 + (tCol3 * 0.25f), rY + 11f, p)
            canvas.drawText(row[6], margin + tCol0 + tCol1 + tCol2 + (tCol3 * 0.75f), rY + 11f, p)
        }

        // -------------------------------------------------------------
        // Draw Table 2: विद्यार्थी विवरण (RIGHT)
        // -------------------------------------------------------------
        val sX = margin + leftCensusW
        canvas.drawRect(sX, curY, right, curY + headerH + (5 * rowH), stroke)
        canvas.drawLine(sX, curY + 14f, right, curY + 14f, stroke)
        canvas.drawLine(sX, curY + headerH, right, curY + headerH, stroke)

        canvas.drawText("विद्यार्थी विवरण", sX + (rightCensusW / 2f), curY + 10.5f, p)

        val sCol0 = rightCensusW * 0.14f // जाति वर्ग
        val sCol1 = rightCensusW * 0.32f // विद्यार्थी (बालक, बालिका, कुल)
        val sCol2 = rightCensusW * 0.28f // बैगा विद्यार्थी (कक्षा, बालक, बालिका)
        val sCol3 = rightCensusW * 0.13f // दिव्यांगों की संख्या
        val sCol4 = rightCensusW * 0.13f // दिव्यांगता का प्रकार

        var sx = sX + sCol0
        canvas.drawLine(sx, curY + 14f, sx, curY + headerH + (5 * rowH), stroke)
        sx += sCol1
        canvas.drawLine(sx, curY + 14f, sx, curY + headerH + (5 * rowH), stroke)
        sx += sCol2
        canvas.drawLine(sx, curY + 14f, sx, curY + headerH + (5 * rowH), stroke)
        sx += sCol3
        canvas.drawLine(sx, curY + 14f, sx, curY + headerH + (5 * rowH), stroke)

        // Subheaders
        p.textSize = 7f
        canvas.drawText("जाति वर्ग", sX + (sCol0 / 2f), curY + 22f, p)

        canvas.drawText("विद्यार्थी", sX + sCol0 + (sCol1 / 2f), curY + 20f, p)
        canvas.drawLine(sX + sCol0, curY + 21f, sX + sCol0 + sCol1, curY + 21f, stroke)
        val s1Third = sCol1 / 3f
        canvas.drawText("बालक", sX + sCol0 + (s1Third * 0.5f), curY + 27.5f, p)
        canvas.drawLine(sX + sCol0 + s1Third, curY + 21f, sX + sCol0 + s1Third, curY + headerH + (5 * rowH), stroke)
        canvas.drawText("बालिका", sX + sCol0 + (s1Third * 1.5f), curY + 27.5f, p)
        canvas.drawLine(sX + sCol0 + (s1Third * 2), curY + 21f, sX + sCol0 + (s1Third * 2), curY + headerH + (5 * rowH), stroke)
        canvas.drawText("कुल", sX + sCol0 + (s1Third * 2.5f), curY + 27.5f, p)

        canvas.drawText("बैगा विद्यार्थी", sX + sCol0 + sCol1 + (sCol2 / 2f), curY + 20f, p)
        canvas.drawLine(sX + sCol0 + sCol1, curY + 21f, sX + sCol0 + sCol1 + sCol2, curY + 21f, stroke)
        val s2Third = sCol2 / 3f
        canvas.drawText("कक्षा", sX + sCol0 + sCol1 + (s2Third * 0.5f), curY + 27.5f, p)
        canvas.drawLine(sX + sCol0 + sCol1 + s2Third, curY + 21f, sX + sCol0 + sCol1 + s2Third, curY + headerH + (5 * rowH), stroke)
        canvas.drawText("बालक", sX + sCol0 + sCol1 + (s2Third * 1.5f), curY + 27.5f, p)
        canvas.drawLine(sX + sCol0 + sCol1 + (s2Third * 2), curY + 21f, sX + sCol0 + sCol1 + (s2Third * 2), curY + headerH + (5 * rowH), stroke)
        canvas.drawText("बालिका", sX + sCol0 + sCol1 + (s2Third * 2.5f), curY + 27.5f, p)

        drawCenteredMultiLineText(canvas, "दिव्यांगों की\nसंख्या", sX + sCol0 + sCol1 + sCol2 + (sCol3 / 2f), curY + 16f, sCol3 - 2f, p, 6.5f)
        drawCenteredMultiLineText(canvas, "दिव्यांगता\nका प्रकार", sX + sCol0 + sCol1 + sCol2 + sCol3 + (sCol4 / 2f), curY + 16f, sCol4 - 2f, p, 6.5f)

        // Student Data Rows
        val sRows = if (e != null && e.totalEnrollment > 0) {
            listOf(
                listOf("अजजा", "-", "-", "${e.stCount}", "-", "-", "-", "0", "-"),
                listOf("अजा", "-", "-", "${e.scCount}", "-", "-", "-", "0", "-"),
                listOf("अपिव", "-", "-", "${e.obcCount}", "-", "-", "-", "${e.cwsnCount}", if (e.cwsnCount > 0) "दिव्यांग" else "-"),
                listOf("सामा.", "-", "-", "${e.generalCount}", "-", "-", "-", "0", "-"),
                listOf("कुल", "${e.totalBoys}", "${e.totalGirls}", "${e.totalEnrollment}", "कुल", "${e.pvtgCount}", "-", "${e.cwsnCount}", "-")
            )
        } else {
            listOf(
                listOf("अजजा", "0", "0", "0", "-", "-", "-", "0", "-"),
                listOf("अजा", "0", "0", "0", "-", "-", "-", "0", "-"),
                listOf("अपिव", "0", "0", "0", "-", "-", "-", "0", "-"),
                listOf("सामा.", "0", "0", "0", "-", "-", "-", "0", "-"),
                listOf("कुल", "0", "0", "0", "-", "0", "0", "0", "-")
            )
        }

        for (i in sRows.indices) {
            val rY = curY + headerH + (i * rowH)
            canvas.drawLine(sX, rY, right, rY, stroke)
            val row = sRows[i]
            p.typeface = if (i == 4) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            p.textSize = 7.5f

            canvas.drawText(row[0], sX + (sCol0 / 2f), rY + 11f, p)
            canvas.drawText(row[1], sX + sCol0 + (s1Third * 0.5f), rY + 11f, p)
            canvas.drawText(row[2], sX + sCol0 + (s1Third * 1.5f), rY + 11f, p)
            canvas.drawText(row[3], sX + sCol0 + (s1Third * 2.5f), rY + 11f, p)

            canvas.drawText(row[4], sX + sCol0 + sCol1 + (s2Third * 0.5f), rY + 11f, p)
            canvas.drawText(row[5], sX + sCol0 + sCol1 + (s2Third * 1.5f), rY + 11f, p)
            canvas.drawText(row[6], sX + sCol0 + sCol1 + (s2Third * 2.5f), rY + 11f, p)

            canvas.drawText(row[7], sX + sCol0 + sCol1 + sCol2 + (sCol3 / 2f), rY + 11f, p)
            canvas.drawText(row[8], sX + sCol0 + sCol1 + sCol2 + sCol3 + (sCol4 / 2f), rY + 11f, p)
        }
    }

    private fun drawMain20ColumnTable(
        canvas: Canvas,
        startX: Float,
        endX: Float,
        startY: Float,
        stroke: Paint,
        p: Paint,
        school: SchoolEntity?,
        agency: CookingAgencyEntity?,
        totalStudents: Int,
        boysStudents: Int,
        girlsStudents: Int,
        avgAttendance: Int,
        mdmDays: Int,
        beneficiariesBoys: Int,
        beneficiariesGirls: Int,
        beneficiariesTotal: Int,
        openingRice: Double,
        receivedRice: Double,
        totalRiceStock: Double,
        consumedRice: Double,
        closingRice: Double,
        totalCostAmount: Double
    ): Float {
        val totalWidth = endX - startX
        val colWidths = floatArrayOf(
            totalWidth * 0.025f, // 1: क्र
            totalWidth * 0.045f, // 2: शहर
            totalWidth * 0.060f, // 3: शाला का नाम
            totalWidth * 0.030f, // 4: कुल बालक
            totalWidth * 0.030f, // 5: कुल बालिका
            totalWidth * 0.035f, // 6: कुल छात्र
            totalWidth * 0.045f, // 7: औसत उपस्थिति
            totalWidth * 0.045f, // 8: मध्यान्ह दिवस
            totalWidth * 0.040f, // 9: लाभान्वित बालक
            totalWidth * 0.040f, // 10: लाभान्वित बालिका
            totalWidth * 0.045f, // 11: कुल लाभान्वित
            totalWidth * 0.055f, // 12: पूर्व माह चावल
            totalWidth * 0.055f, // 13: प्राप्त चावल
            totalWidth * 0.055f, // 14: कुल चावल स्टॉक
            totalWidth * 0.055f, // 15: खपत चावल
            totalWidth * 0.055f, // 16: शेष चावल
            totalWidth * 0.065f, // 17: व्यय राशि
            totalWidth * 0.090f, // 18: एजेंसी नाम
            totalWidth * 0.065f, // 19: खाता नम्बर
            totalWidth * 0.065f  // 20: बैंक का नाम
        )

        val headerH1 = 28f
        val headerH2 = 14f // for 1 to 20 numbers
        val dataRowH = 50f
        val totalHeaderH = headerH1 + headerH2

        // Draw Table Outline
        canvas.drawRect(startX, startY, endX, startY + totalHeaderH + dataRowH, stroke)
        canvas.drawLine(startX, startY + headerH1, endX, startY + headerH1, stroke)
        canvas.drawLine(startX, startY + totalHeaderH, endX, startY + totalHeaderH, stroke)

        val headers = listOf(
            "क्र", "शहर का\nनाम", "शाला का\nनाम",
            "शाला में कुल विद्यार्थी\n(बालक)", "(बालिका)", "(कुल)",
            "माह में\nऔसत\nउपस्थिति", "मध्यान्ह\nभोजन\nदिवस",
            "कुल लाभान्वित संख्या\n(बालक)", "(बालिका)", "(कुल)",
            "पूर्व माह का\nचावल", "माह में\nप्राप्त चावल", "कुल चावल\nस्टॉक",
            "माह में कुल\nचावल का\nखपत", "माह में शेष\nचावल", "माह में व्यय\nराशि (₹)",
            "मध्यान्ह\nभोजन\nसंचालन\nएजेंसी का नाम", "खाता\nनम्बर", "बैंक का\nनाम"
        )

        var curX = startX
        for (i in 0 until 20) {
            val w = colWidths[i]
            if (i > 0) {
                canvas.drawLine(curX, startY, curX, startY + totalHeaderH + dataRowH, stroke)
            }

            // Header text
            p.textAlign = Paint.Align.CENTER
            p.textSize = 6f
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            drawCenteredMultiLineText(canvas, headers[i], curX + (w / 2f), startY + 4f, w - 1f, p, 6.5f)

            // Number (1..20)
            canvas.drawText("${i + 1}", curX + (w / 2f), startY + headerH1 + 10f, p)

            curX += w
        }

        val schoolNameFormatted = (school?.schoolName ?: "-").replace(" ", "\n")
        val agencyNameFormatted = (agency?.name ?: "-").replace(" ", "\n")
        val bankDetails = if (agency != null && agency.bankName.isNotBlank()) {
            if (agency.branchName.isNotBlank()) "${agency.bankName}\nशाखा ${agency.branchName}" else agency.bankName
        } else "-"
        val accNo = agency?.maskedAccountNo?.ifBlank { "-" } ?: "-"

        // Data Row
        val rowData = listOf(
            "1",
            school?.villageName ?: "-",
            schoolNameFormatted,
            "$boysStudents",
            "$girlsStudents",
            "$totalStudents",
            "$avgAttendance",
            "$mdmDays",
            "$beneficiariesBoys",
            "$beneficiariesGirls",
            "$beneficiariesTotal",
            formatDecimal(openingRice),
            formatDecimal(receivedRice),
            formatDecimal(totalRiceStock),
            formatDecimal(consumedRice),
            formatDecimal(closingRice),
            String.format(Locale.getDefault(), "%.3f", totalCostAmount),
            agencyNameFormatted,
            accNo,
            bankDetails
        )

        curX = startX
        val dataY = startY + totalHeaderH + 12f
        p.textSize = 6.5f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        for (i in 0 until 20) {
            val w = colWidths[i]
            val text = rowData[i]
            if (text.contains("\n")) {
                drawCenteredMultiLineText(canvas, text, curX + (w / 2f), startY + totalHeaderH + 6f, w - 1f, p, 7f)
            } else {
                canvas.drawText(text, curX + (w / 2f), dataY + 12f, p)
            }
            curX += w
        }

        return startY + totalHeaderH + dataRowH
    }

    private fun drawCooksTable(
        canvas: Canvas,
        startX: Float,
        startY: Float,
        width: Float,
        stroke: Paint,
        p: Paint,
        cooks: List<CookEntity>
    ) {
        val rowH = 14f
        val col0 = width * 0.12f // क्र.
        val col1 = width * 0.40f // रसोइया का नाम
        val col2 = width * 0.24f // बैंक का नाम
        val col3 = width * 0.24f // खाता नम्बर

        val list = cooks
        val totalRows = (list.size + 1).coerceAtLeast(4)

        canvas.drawRect(startX, startY, startX + width, startY + (totalRows * rowH), stroke)
        for (i in 1 until totalRows) {
            canvas.drawLine(startX, startY + (i * rowH), startX + width, startY + (i * rowH), stroke)
        }

        var cx = startX + col0
        canvas.drawLine(cx, startY, cx, startY + (totalRows * rowH), stroke)
        cx += col1
        canvas.drawLine(cx, startY, cx, startY + (totalRows * rowH), stroke)
        cx += col2
        canvas.drawLine(cx, startY, cx, startY + (totalRows * rowH), stroke)

        // Header
        p.textAlign = Paint.Align.CENTER
        p.textSize = 7f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("क्र.", startX + (col0 / 2f), startY + 10f, p)
        canvas.drawText("रसोइया का नाम", startX + col0 + (col1 / 2f), startY + 10f, p)
        canvas.drawText("बैंक का नाम", startX + col0 + col1 + (col2 / 2f), startY + 10f, p)
        canvas.drawText("खाता नम्बर", startX + col0 + col1 + col2 + (col3 / 2f), startY + 10f, p)

        // Data Rows
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        if (list.isEmpty()) {
            for (i in 0 until (totalRows - 1)) {
                val rY = startY + ((i + 1) * rowH)
                p.textAlign = Paint.Align.CENTER
                canvas.drawText("${i + 1}", startX + (col0 / 2f), rY + 10f, p)
                canvas.drawText("-", startX + col0 + (col1 / 2f), rY + 10f, p)
                canvas.drawText("-", startX + col0 + col1 + (col2 / 2f), rY + 10f, p)
                canvas.drawText("-", startX + col0 + col1 + col2 + (col3 / 2f), rY + 10f, p)
            }
        } else {
            for (i in list.indices) {
                val rY = startY + ((i + 1) * rowH)
                val c = list[i]
                p.textAlign = Paint.Align.CENTER
                canvas.drawText("${i + 1}", startX + (col0 / 2f), rY + 10f, p)
                p.textAlign = Paint.Align.LEFT
                canvas.drawText(c.name.ifBlank { "-" }, startX + col0 + 4f, rY + 10f, p)
                p.textAlign = Paint.Align.CENTER
                canvas.drawText(c.bankName.ifBlank { "-" }, startX + col0 + col1 + (col2 / 2f), rY + 10f, p)
                canvas.drawText(c.bankAccountNo.ifBlank { "-" }, startX + col0 + col1 + col2 + (col3 / 2f), rY + 10f, p)
            }
        }
    }

    private fun drawClassMatrixTable(
        canvas: Canvas,
        startX: Float,
        startY: Float,
        width: Float,
        stroke: Paint,
        p: Paint,
        enrollment: MonthlyEnrollmentEntity?
    ) {
        val rowH = 13f
        val headerH = 24f
        val totalRows = 6 // अजजा, अजा, अपिव, सामा., कुल, अल्पसंख्यक

        canvas.drawRect(startX, startY, startX + width, startY + headerH + (totalRows * rowH), stroke)
        canvas.drawLine(startX, startY + 12f, startX + width, startY + 12f, stroke)
        canvas.drawLine(startX, startY + headerH, startX + width, startY + headerH, stroke)

        val colSr = width * 0.06f
        val colCat = width * 0.12f
        val colClassesW = width * 0.54f // 3 classes * 2 cols = 6 subcols
        val colSumW = width * 0.18f // योग (बालक, बालिका)
        val colGrandW = width * 0.10f // महायोग

        var cx = startX + colSr
        canvas.drawLine(cx, startY, cx, startY + headerH + (totalRows * rowH), stroke)
        cx += colCat
        canvas.drawLine(cx, startY, cx, startY + headerH + (totalRows * rowH), stroke)
        cx += colClassesW
        canvas.drawLine(cx, startY, cx, startY + headerH + (totalRows * rowH), stroke)
        cx += colSumW
        canvas.drawLine(cx, startY, cx, startY + headerH + (totalRows * rowH), stroke)

        // Class Sub Columns (6th, 7th, 8th -> 6 cols)
        val singleClassW = colClassesW / 3f
        val halfClassW = singleClassW / 2f

        for (i in 0..2) {
            val classStartX = startX + colSr + colCat + (i * singleClassW)
            if (i > 0) {
                canvas.drawLine(classStartX, startY, classStartX, startY + headerH + (totalRows * rowH), stroke)
            }
            canvas.drawLine(classStartX + halfClassW, startY + 12f, classStartX + halfClassW, startY + headerH + (totalRows * rowH), stroke)
        }

        // Sum Sub Columns (बालक, बालिका)
        val halfSumW = colSumW / 2f
        canvas.drawLine(startX + colSr + colCat + colClassesW + halfSumW, startY + 12f, startX + colSr + colCat + colClassesW + halfSumW, startY + headerH + (totalRows * rowH), stroke)

        // Header Top Labels
        p.textAlign = Paint.Align.CENTER
        p.textSize = 6.5f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("स.क्र.", startX + (colSr / 2f), startY + 16f, p)
        canvas.drawText("जाति वर्ग", startX + colSr + (colCat / 2f), startY + 16f, p)

        canvas.drawText("6 वीं", startX + colSr + colCat + (singleClassW * 0.5f), startY + 9f, p)
        canvas.drawText("7 वीं", startX + colSr + colCat + (singleClassW * 1.5f), startY + 9f, p)
        canvas.drawText("8 वीं", startX + colSr + colCat + (singleClassW * 2.5f), startY + 9f, p)

        canvas.drawText("योग", startX + colSr + colCat + colClassesW + (colSumW / 2f), startY + 9f, p)
        canvas.drawText("महायोग", startX + colSr + colCat + colClassesW + colSumW + (colGrandW / 2f), startY + 16f, p)

        // Subheaders (बालक, बालिका)
        p.textSize = 5.5f
        for (i in 0..2) {
            val classStartX = startX + colSr + colCat + (i * singleClassW)
            canvas.drawText("बालक", classStartX + (halfClassW * 0.5f), startY + 20f, p)
            canvas.drawText("बालिका", classStartX + (halfClassW * 1.5f), startY + 20f, p)
        }
        val sumStartX = startX + colSr + colCat + colClassesW
        canvas.drawText("बालक", sumStartX + (halfSumW * 0.5f), startY + 20f, p)
        canvas.drawText("बालिका", sumStartX + (halfSumW * 1.5f), startY + 20f, p)

        // Data Rows
        val matrixRows = if (enrollment != null && enrollment.totalEnrollment > 0) {
            listOf(
                listOf("1", "अजजा", "-", "-", "-", "-", "-", "-", "-", "-", "${enrollment.stCount}"),
                listOf("2", "अजा", "-", "-", "-", "-", "-", "-", "-", "-", "${enrollment.scCount}"),
                listOf("3", "अपिव", "-", "-", "-", "-", "-", "-", "-", "-", "${enrollment.obcCount}"),
                listOf("4", "सामा.", "-", "-", "-", "-", "-", "-", "-", "-", "${enrollment.generalCount}"),
                listOf("5", "कुल", "-", "-", "-", "-", "-", "-", "${enrollment.totalBoys}", "${enrollment.totalGirls}", "${enrollment.totalEnrollment}"),
                listOf("6", "अल्पसंख्यक", "-", "-", "-", "-", "-", "-", "-", "-", "${enrollment.minorityCount}")
            )
        } else {
            listOf(
                listOf("1", "अजजा", "0", "0", "0", "0", "0", "0", "0", "0", "0"),
                listOf("2", "अजा", "0", "0", "0", "0", "0", "0", "0", "0", "0"),
                listOf("3", "अपिव", "0", "0", "0", "0", "0", "0", "0", "0", "0"),
                listOf("4", "सामा.", "0", "0", "0", "0", "0", "0", "0", "0", "0"),
                listOf("5", "कुल", "0", "0", "0", "0", "0", "0", "0", "0", "0"),
                listOf("6", "अल्पसंख्यक", "0", "0", "0", "0", "0", "0", "0", "0", "0")
            )
        }

        for (i in matrixRows.indices) {
            val rY = startY + headerH + (i * rowH)
            canvas.drawLine(startX, rY, startX + width, rY, stroke)
            val r = matrixRows[i]
            p.typeface = if (i == 4) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            p.textSize = 6.5f

            canvas.drawText(r[0], startX + (colSr / 2f), rY + 9.5f, p)
            canvas.drawText(r[1], startX + colSr + (colCat / 2f), rY + 9.5f, p)

            // 6th
            canvas.drawText(r[2], startX + colSr + colCat + (halfClassW * 0.5f), rY + 9.5f, p)
            canvas.drawText(r[3], startX + colSr + colCat + (halfClassW * 1.5f), rY + 9.5f, p)
            // 7th
            canvas.drawText(r[4], startX + colSr + colCat + singleClassW + (halfClassW * 0.5f), rY + 9.5f, p)
            canvas.drawText(r[5], startX + colSr + colCat + singleClassW + (halfClassW * 1.5f), rY + 9.5f, p)
            // 8th
            canvas.drawText(r[6], startX + colSr + colCat + (singleClassW * 2) + (halfClassW * 0.5f), rY + 9.5f, p)
            canvas.drawText(r[7], startX + colSr + colCat + (singleClassW * 2) + (halfClassW * 1.5f), rY + 9.5f, p)

            // योग (बालक, बालिका)
            canvas.drawText(r[8], sumStartX + (halfSumW * 0.5f), rY + 9.5f, p)
            canvas.drawText(r[9], sumStartX + (halfSumW * 1.5f), rY + 9.5f, p)

            // महायोग
            canvas.drawText(r[10], sumStartX + colSumW + (colGrandW / 2f), rY + 9.5f, p)
        }
    }

    // =========================================================================
    // GRAPHICAL ASSETS & DECORATIONS (EMBLEM, PM POSHAN LOGO, SEAL)
    // =========================================================================

    private fun drawEmblemLogo(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.rgb(180, 83, 9) // Golden / Amber Brown
            strokeWidth = 1.2f
        }
        canvas.drawCircle(cx, cy, radius, p)
        canvas.drawCircle(cx, cy, radius - 2.5f, p)

        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(146, 64, 14)
            textSize = 3.5f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("छत्तीसगढ़ शासन", cx, cy - radius + 5f, tp)
        canvas.drawText("सत्यमेव जयते", cx, cy + radius - 3f, tp)

        // Center Pillar placeholder
        val ip = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(180, 83, 9)
            style = Paint.Style.FILL
        }
        canvas.drawRect(cx - 3f, cy - 4f, cx + 3f, cy + 4f, ip)
    }

    private fun drawMidDayMealLogo(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val colors = intArrayOf(
            Color.rgb(239, 68, 68),   // Red
            Color.rgb(249, 115, 22),  // Orange
            Color.rgb(234, 179, 8),   // Yellow
            Color.rgb(34, 197, 94),   // Green
            Color.rgb(59, 130, 246),  // Blue
            Color.rgb(168, 85, 247)   // Purple
        )
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val numDots = 8
        for (i in 0 until numDots) {
            val angle = (i * (360.0 / numDots)) * Math.PI / 180.0
            val dotX = cx + (radius * 0.75f * Math.cos(angle)).toFloat()
            val dotY = cy + (radius * 0.75f * Math.sin(angle)).toFloat()
            p.color = colors[i % colors.size]
            canvas.drawCircle(dotX, dotY, 2.2f, p)
        }
        // Center child / sun icon
        p.color = Color.rgb(234, 88, 12)
        canvas.drawCircle(cx, cy, 3.2f, p)

        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138)
            textSize = 3f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Mid Day Meal Scheme", cx, cy + radius + 4f, tp)
    }

    private fun drawOfficialHeadMasterStamp(
        canvas: Canvas,
        x: Float,
        y: Float,
        school: SchoolEntity?,
        headMasterName: String = "",
        headMasterDesignation: String = "Head Master",
        signDate: String = "",
        isSigned: Boolean = false,
        signatureBitmap: Bitmap? = null
    ) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 27, 75) // Dark Indigo Ink
            textSize = 7.5f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 27, 75)
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }

        if (signatureBitmap != null) {
            // Draw real on-screen captured signature above stamp
            val sigW = 65f
            val sigH = 26f
            val destRect = RectF(x - (sigW / 2f), y - sigH - 2f, x + (sigW / 2f), y - 2f)
            canvas.drawBitmap(signatureBitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))

            // Digitally Signed Badge / Verified indicator
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(22, 101, 52) // Forest Green
                textSize = 5.5f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("✓ DIGITALLY SIGNED", x, y - sigH - 6f, badgePaint)

            val nameToShow = headMasterName.ifBlank { "प्रधान पाठक / Headmaster" }
            canvas.drawText(nameToShow, x, y + 5f, p)
            
            p.textSize = 6.5f
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(headMasterDesignation.ifBlank { "Head Master" }, x, y + 13f, p)
            
            val scName = school?.schoolName?.ifBlank { "शासकीय विद्यालय" } ?: "शासकीय विद्यालय"
            canvas.drawText(scName, x, y + 21f, p)
            
            if (signDate.isNotBlank()) {
                canvas.drawText("दिनांक: $signDate", x, y + 29f, p)
            }
        } else if (isSigned) {
            // Stylized cursive flourish signature
            val sigPath = Path().apply {
                moveTo(x - 28f, y - 6f)
                quadTo(x - 12f, y - 16f, x - 2f, y - 7f)
                quadTo(x + 10f, y + 2f, x + 24f, y - 10f)
                moveTo(x - 20f, y - 4f)
                lineTo(x + 28f, y - 4f)
            }
            canvas.drawPath(sigPath, sp)

            // Digitally Signed Badge / Verified indicator
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(22, 101, 52) // Forest Green
                textSize = 5.5f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("✓ DIGITALLY SIGNED", x, y - 16f, badgePaint)

            val nameToShow = headMasterName.ifBlank { "प्रधान पाठक / Headmaster" }
            canvas.drawText(nameToShow, x, y + 5f, p)
            
            p.textSize = 6.5f
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(headMasterDesignation.ifBlank { "Head Master" }, x, y + 13f, p)
            
            val scName = school?.schoolName?.ifBlank { "शासकीय विद्यालय" } ?: "शासकीय विद्यालय"
            canvas.drawText(scName, x, y + 21f, p)
            
            if (signDate.isNotBlank()) {
                canvas.drawText("दिनांक: $signDate", x, y + 29f, p)
            }
        } else {
            // Signature line and stamp placeholder for manual signing
            canvas.drawLine(x - 30f, y - 6f, x + 30f, y - 6f, sp)
            canvas.drawLine(x - 15f, y - 8f, x + 10f, y - 3f, sp)

            canvas.drawText("हस्ताक्षर प्रधान पाठक / मुहर", x, y + 6f, p)
            p.textSize = 7f
            val scName = school?.schoolName?.ifBlank { "शासकीय विद्यालय" } ?: "शासकीय विद्यालय"
            canvas.drawText(scName, x, y + 15f, p)
            val dist = school?.districtName?.ifBlank { "" } ?: ""
            if (dist.isNotBlank()) {
                canvas.drawText("जिला- $dist", x, y + 24f, p)
            }
            val udise = school?.udiseCode?.ifBlank { "" } ?: ""
            if (udise.isNotBlank()) {
                canvas.drawText("UDISE: $udise", x, y + 33f, p)
            }
        }
    }

    private fun drawCenteredMultiLineText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint,
        lineSpacing: Float
    ) {
        val lines = text.split("\n")
        var curY = startY + lineSpacing
        for (line in lines) {
            canvas.drawText(line, centerX, curY, paint)
            curY += lineSpacing
        }
    }

    private fun formatMonthDisplay(monthStr: String): String {
        return try {
            if (monthStr.contains("-")) {
                val parts = monthStr.split("-")
                if (parts[0].length == 4) {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, parts[0].toInt())
                        set(Calendar.MONTH, parts[1].toInt() - 1)
                    }
                    SimpleDateFormat("MMM-yyyy", Locale.ENGLISH).format(cal.time)
                } else {
                    monthStr
                }
            } else {
                monthStr
            }
        } catch (_: Exception) {
            monthStr
        }
    }

    private fun getDailyAttendanceMap(monthStr: String, records: List<DailyMealRecordEntity>, defaultEnrollment: Int): Map<Int, Int> {
        val map = mutableMapOf<Int, Int>()
        for (d in 1..31) {
            val dateKey = String.format(Locale.getDefault(), "%s-%02d", monthStr, d)
            val r = records.find { it.date == dateKey || it.date.endsWith("-$d") }
            if (r != null) {
                map[d] = r.studentsPresent
            } else {
                map[d] = 0
            }
        }
        return map
    }

    private fun getDailyBeneficiaryMap(monthStr: String, records: List<DailyMealRecordEntity>): Map<Int, Int> {
        val map = mutableMapOf<Int, Int>()
        for (d in 1..31) {
            val dateKey = String.format(Locale.getDefault(), "%s-%02d", monthStr, d)
            val r = records.find { it.date == dateKey || it.date.endsWith("-$d") }
            if (r != null) {
                map[d] = r.studentsServed
            } else {
                map[d] = 0
            }
        }
        return map
    }

    private fun formatDecimal(value: Double): String {
        return String.format(Locale.getDefault(), "%.3f", value)
    }

    private fun truncateText(str: String, maxLen: Int): String {
        return if (str.length > maxLen) str.substring(0, maxLen - 1) + "…" else str
    }
}
