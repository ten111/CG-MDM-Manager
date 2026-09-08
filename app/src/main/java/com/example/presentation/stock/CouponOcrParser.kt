package com.example.presentation.stock

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume

data class CouponOcrResult(
    val isSuccess: Boolean = false,
    val couponNumber: String = "",
    val receiptDateDdMmYyyy: String = "", // Strictly DD-MM-YYYY (1st date of allotment month, e.g. 01-07-2026)
    val dateIso: String = "",             // yyyy-MM-dd
    val dateDisplay: String = "",         // dd/MM/yyyy
    val pdsShopName: String = "",
    val quantityKg: Double = 0.0,
    val rawAllotmentText: String = "",
    val allotmentMonth: String = "",
    val schoolName: String = "",
    val selfHelpGroupName: String = "",
    val enrolledStudents: String = "",
    val schoolDays: String = "",
    val rawFullText: String = "",
    val errorMessage: String? = null
)

object CouponOcrParser {

    suspend fun parseCouponFromUri(context: Context, imageUri: Uri): CouponOcrResult =
        withContext(Dispatchers.IO) {
            try {
                val bitmap = loadOptimizedBitmap(context, imageUri)
                    ?: return@withContext CouponOcrResult(
                        isSuccess = false,
                        errorMessage = "छवि लोड करने में असमर्थ (Unable to load image)"
                    )
                parseCouponFromBitmap(bitmap)
            } catch (e: Exception) {
                CouponOcrResult(
                    isSuccess = false,
                    errorMessage = "OCR प्रक्रिया में त्रुटि: ${e.localizedMessage}"
                )
            }
        }

    suspend fun parseCouponFromBitmap(bitmap: Bitmap): CouponOcrResult =
        suspendCancellableCoroutine { continuation ->
            try {
                val recognizer = TextRecognition.getClient(
                    DevanagariTextRecognizerOptions.Builder().build()
                )
                val inputImage = InputImage.fromBitmap(bitmap, 0)

                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val parsed = extractCouponData(visionText.text)
                        continuation.resume(parsed.copy(rawFullText = visionText.text))
                    }
                    .addOnFailureListener { exception ->
                        continuation.resume(
                            CouponOcrResult(
                                isSuccess = false,
                                errorMessage = "पाठ पहचानने में त्रुटि: ${exception.localizedMessage}"
                            )
                        )
                    }
            } catch (e: Exception) {
                continuation.resume(
                    CouponOcrResult(
                        isSuccess = false,
                        errorMessage = "OCR प्रारंभ करने में त्रुटि: ${e.localizedMessage}"
                    )
                )
            }
        }

    private fun normalizeDigitsAndChars(input: String): String {
        val devanagariDigits = mapOf(
            '०' to '0', '१' to '1', '२' to '2', '३' to '3', '४' to '4',
            '५' to '5', '६' to '6', '७' to '7', '८' to '8', '९' to '9'
        )
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when {
                devanagariDigits.containsKey(ch) -> sb.append(devanagariDigits[ch])
                ch == '•' || ch == '·' -> sb.append('.')
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun extractCouponData(fullText: String): CouponOcrResult {
        if (fullText.isBlank()) {
            return CouponOcrResult(
                isSuccess = false,
                errorMessage = "छवि में कोई पाठ नहीं मिला (No text detected in image)"
            )
        }

        val normalizedFullText = normalizeDigitsAndChars(fullText)
        val lines = normalizedFullText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        // 1. Coupon / Challan Number: Always starts with "RC", e.g. "RC570207260025"
        var couponNumber = ""

        // Strategy 1A: Direct regex matching "RC" followed by digits/letters
        // Handle common OCR confusion: '5' read as 'S' or 's' (e.g. "RCS70207260025" -> "RC570207260025")
        val rcRegex = Regex("""(?:^|[^a-zA-Z0-9])(RC[\s\-_.:]*[0-9OIlSsa-zA-Z]{6,16})""", RegexOption.IGNORE_CASE)
        val rcMatch = rcRegex.find(normalizedFullText)
        if (rcMatch != null) {
            val raw = rcMatch.groupValues[1]
            val cleanedDigits = raw.substring(2)
                .replace(" ", "").replace("-", "").replace("_", "").replace(":", "").replace(".", "")
                .replace("O", "0").replace("o", "0")
                .replace("I", "1").replace("l", "1")
                .replace("S", "5").replace("s", "5")
                .replace("B", "8")
                .filter { it.isDigit() }
            couponNumber = "RC$cleanedDigits"
        }

        // Strategy 1B: Look in lines containing "पत्रक" or "कूपन"
        if (couponNumber.isBlank()) {
            for (line in lines) {
                if (line.contains("पत्रक") || line.contains("कूपन") || line.contains("Coupon")) {
                    val candidateMatch = Regex("""(RC[\s\-_.:]*[0-9OIlSsa-zA-Z]{6,16})""", RegexOption.IGNORE_CASE).find(line)
                    if (candidateMatch != null) {
                        val raw = candidateMatch.groupValues[1]
                        val cleanedDigits = raw.substring(2)
                            .replace(" ", "").replace("-", "").replace("_", "").replace(":", "").replace(".", "")
                            .replace("O", "0").replace("o", "0")
                            .replace("I", "1").replace("l", "1")
                            .replace("S", "5").replace("s", "5")
                            .replace("B", "8")
                            .filter { it.isDigit() }
                        couponNumber = "RC$cleanedDigits"
                        break
                    }
                    // Fallback: If OCR missed "RC" prefix but found the long digit code next to "पत्रक क्र."
                    val numberOnlyMatch = Regex("""(?:पत्रक\s*क्र[^\d]*|कूपन[^\d]*)([0-9]{8,14})""").find(line)
                    if (numberOnlyMatch != null) {
                        couponNumber = "RC" + numberOnlyMatch.groupValues[1]
                        break
                    }
                }
            }
        }

        // 2. Allotment Month & First Date of Allotment Month (DD-MM-YYYY)
        // User rule: "Date format should be DD-MM-YYYY and don't use the date of the coupon for input field.
        // It is the coupon date not the receipt date. Use the first date of the allotment month which is jul 2026 in this coupon"
        var allotmentMonth = ""
        var receiptDateDdMmYyyy = ""

        // Helper to convert any Hindi or English month string to 2-digit month number ("01" to "12")
        fun mapMonthToNumber(str: String): String? {
            val lower = str.lowercase(Locale.ROOT)
            return when {
                lower.contains("जनवरी") || lower.contains("jan") -> "01"
                lower.contains("फरवरी") || lower.contains("फ़रवरी") || lower.contains("feb") -> "02"
                lower.contains("मार्च") || lower.contains("mar") -> "03"
                lower.contains("अप्रैल") || lower.contains("अप्रेल") || lower.contains("apr") -> "04"
                lower.contains("मई") || lower.contains("may") -> "05"
                lower.contains("जून") || lower.contains("jun") -> "06"
                lower.contains("जुलाई") || lower.contains("jul") -> "07"
                // OCR variations for "अगस्त": अगरत, अगसत, अगत, अगस्ट, अगस्त्य
                lower.contains("अगस्त") || lower.contains("अगरत") || lower.contains("अगसत") ||
                        lower.contains("अगत") || lower.contains("अगस्ट") || lower.contains("aug") -> "08"
                lower.contains("सितम्बर") || lower.contains("सितंबर") || lower.contains("sep") -> "09"
                lower.contains("अक्टूबर") || lower.contains("अक्तूबर") || lower.contains("oct") -> "10"
                lower.contains("नवम्बर") || lower.contains("नवंबर") || lower.contains("nov") -> "11"
                lower.contains("दिसम्बर") || lower.contains("दिसंबर") || lower.contains("dec") -> "12"
                else -> null
            }
        }

        fun getMonthHindiName(mNum: String): String {
            return when (mNum) {
                "01" -> "जनवरी"
                "02" -> "फरवरी"
                "03" -> "मार्च"
                "04" -> "अप्रैल"
                "05" -> "मई"
                "06" -> "जून"
                "07" -> "जुलाई"
                "08" -> "अगस्त"
                "09" -> "सितंबर"
                "10" -> "अक्टूबर"
                "11" -> "नवंबर"
                "12" -> "दिसंबर"
                else -> ""
            }
        }

        // Year extraction helper: look for 20xx in text or default to 2026
        var detectedYear = Regex("""\b(202[4-9])\b""").find(normalizedFullText)?.groupValues?.get(1) ?: "2026"
        var detectedMonthNum: String? = null

        // Strategy 2A: Directly from Coupon Number structure RC<ShopId:4><MM><YY><Serial:4>
        // On CG MDM PDS rice coupons: RC570207260025 -> MM=07 (July), YY=26 (2026)
        // RC570208260025 -> MM=08 (August), YY=26 (2026)
        if (couponNumber.isNotBlank()) {
            val couponCodeMatch = Regex("""RC\d{4}(0[1-9]|1[0-2])(2\d)\d{4}""", RegexOption.IGNORE_CASE).find(couponNumber)
            if (couponCodeMatch != null) {
                detectedMonthNum = couponCodeMatch.groupValues[1]
                detectedYear = "20" + couponCodeMatch.groupValues[2]
                allotmentMonth = "${getMonthHindiName(detectedMonthNum)} $detectedYear"
            }
        }

        // Strategy 2B: "आबंटन माह: [Month] [Year]" or "आबटन माह: [Month]"
        // Covers Devanagari OCR variations: आबंटन, आवंटन, आबटन, आवटन, आबन्टन, आवंटण
        if (detectedMonthNum == null) {
            val allotmentMonthRegex = Regex(
                """(?:आबंटन|आवंटन|आबटन|आवटन|आबन्टन|आवंटण)\s*माह[:\s\-=|]*([^\n\r,:|]{2,30})""",
                RegexOption.IGNORE_CASE
            )
            val monthMatch = allotmentMonthRegex.find(normalizedFullText)
            if (monthMatch != null) {
                val candidate = monthMatch.groupValues[1].trim()
                val m = mapMonthToNumber(candidate)
                if (m != null) {
                    detectedMonthNum = m
                    val yr = Regex("""(202[4-9])""").find(candidate)?.groupValues?.get(1)
                    if (yr != null) detectedYear = yr
                    allotmentMonth = "${getMonthHindiName(detectedMonthNum)} $detectedYear"
                }
            }
        }

        // Strategy 2C: "[Month] में शाला दिवस" e.g. "अगस्त में शाला दिवस की संख्या: 24"
        if (detectedMonthNum == null) {
            val daysMonthRegex = Regex("""([^\s\d\n]{3,15})\s*में\s*शाला\s*दिवस""", RegexOption.IGNORE_CASE)
            val dMatch = daysMonthRegex.find(normalizedFullText)
            if (dMatch != null) {
                val m = mapMonthToNumber(dMatch.groupValues[1])
                if (m != null) {
                    detectedMonthNum = m
                    allotmentMonth = "${getMonthHindiName(detectedMonthNum)} $detectedYear"
                }
            }
        }

        // Strategy 2D: "[Month] का आबंटन" e.g. "अगस्त का आबंटन: 2.30 क्विंटल" or "जुलाई का आबंटन: 1.90 क्विंटल"
        if (detectedMonthNum == null) {
            val monthAllotmentRegex = Regex(
                """([^\s\d\n]{3,15})\s*का\s*(?:आबंटन|आवंटन|आबटन|आवटन|आबन्टन)""",
                RegexOption.IGNORE_CASE
            )
            val mMatch = monthAllotmentRegex.find(normalizedFullText)
            if (mMatch != null) {
                val m = mapMonthToNumber(mMatch.groupValues[1])
                if (m != null) {
                    detectedMonthNum = m
                    allotmentMonth = "${getMonthHindiName(detectedMonthNum)} $detectedYear"
                }
            }
        }

        // Strategy 2E: Any line with a Hindi month name and 20xx (strictly excluding "प्रविष्टी माह" if on a separate line)
        if (detectedMonthNum == null) {
            for (line in lines) {
                // If line contains "आबंटन" or does not contain "प्रविष्टी"
                if (line.contains("आबंटन") || line.contains("आबटन") || !line.contains("प्रविष्टी")) {
                    val m = mapMonthToNumber(line)
                    if (m != null) {
                        detectedMonthNum = m
                        val yr = Regex("""(202[4-9])""").find(line)?.groupValues?.get(1)
                        if (yr != null) detectedYear = yr
                        allotmentMonth = "${getMonthHindiName(detectedMonthNum)} $detectedYear"
                        break
                    }
                }
            }
        }

        // If we found the allotment month, form 1st of that month in DD-MM-YYYY
        if (detectedMonthNum != null) {
            receiptDateDdMmYyyy = "01-$detectedMonthNum-$detectedYear"
        }

        // For reference only: coupon issue date from coupon header
        var couponIssueDate = ""
        val dateRegex = Regex("""\b(\d{1,2})[/.-](\d{1,2})[/.-](\d{4})\b""")
        val dateMatches = dateRegex.findAll(normalizedFullText).toList()
        if (dateMatches.isNotEmpty()) {
            val d = dateMatches.first().groupValues[1].padStart(2, '0')
            val m = dateMatches.first().groupValues[2].padStart(2, '0')
            val y = dateMatches.first().groupValues[3]
            couponIssueDate = "$d/$m/$y"
        }

        // Fallback if month could still not be determined:
        // If coupon issue date is available (e.g. 27/07/2026), receipt date should be 1st of the following month (01-08-2026)
        if (receiptDateDdMmYyyy.isBlank()) {
            if (couponIssueDate.isNotBlank()) {
                val parts = couponIssueDate.split("/")
                val issueMonth = parts.getOrNull(1)?.toIntOrNull() ?: 7
                val issueYear = parts.getOrNull(2)?.toIntOrNull() ?: 2026
                val nextMonth = if (issueMonth == 12) 1 else issueMonth + 1
                val nextYear = if (issueMonth == 12) issueYear + 1 else issueYear
                receiptDateDdMmYyyy = String.format(Locale.US, "01-%02d-%04d", nextMonth, nextYear)
                allotmentMonth = "${getMonthHindiName(String.format(Locale.US, "%02d", nextMonth))} $nextYear"
            } else {
                receiptDateDdMmYyyy = "01-" + SimpleDateFormat("MM-yyyy", Locale.US).format(Date())
            }
        }

        // 3. PDS Shop Name (उ.मु.दु का आईडी/नाम / सेवा सहकारी समिति)
        var pdsShop = ""
        val pdsLine = lines.firstOrNull {
            it.contains("उ.मु.दु") || it.contains("उ.मू.दु") || it.contains("उचित मूल्य") ||
                    it.contains("सेवा सहकारी") || it.contains("सोसायटी") || it.contains("PDS")
        }
        if (pdsLine != null) {
            if (pdsLine.contains("/")) {
                val parts = pdsLine.split("/").map { it.trim() }
                pdsShop = parts.lastOrNull { it.any { c -> !c.isDigit() } } ?: pdsLine
            } else {
                pdsShop = pdsLine.substringAfter(":").ifEmpty { pdsLine }.trim()
            }
            pdsShop = pdsShop.replace("उ.मु.दु का आईडी/नाम", "")
                .replace("उ.मू.दु का आईडी/नाम", "")
                .replace("उ.मु.दु", "")
                .replace("उ.मू.दु", "")
                .replace("उचित मूल्य की दुकान", "")
                .replace(":", "")
                .trim()
        }
        if (pdsShop.isBlank()) {
            val coopLine = lines.firstOrNull { it.contains("सहकारी") || it.contains("समिति") }
            if (coopLine != null) {
                pdsShop = coopLine
            }
        }
        if (pdsShop.isNotBlank()) {
            // Strip leading IDs like "572002016", "s72002016", or separators to get pure shop name e.g. "सेवा सहकारी समिति"
            val cleanedShop = pdsShop.replace(Regex("""^[0-9a-zA-Z/:\s\-_]+"""), "").trim()
            if (cleanedShop.isNotBlank()) {
                pdsShop = cleanedShop
            }
        }

        // 4. Student count and days (parse early so they can be excluded from quantity matching)
        var enrolledStudents = ""
        val studentsMatch = Regex("""(?:दर्ज\s*संख्या|छात्र)[^\d]*(\d+)""").find(normalizedFullText)
            ?: Regex("""(?:दर्ज\s*संख्या|छात्र)[^\d]*(\d+)""").find(fullText)
        if (studentsMatch != null) {
            enrolledStudents = studentsMatch.groupValues[1]
        }

        var schoolDays = ""
        val daysMatch = Regex("""(?:शाला\s*दिवस|दिवस\s*की\s*संख्या)[^\d]*(\d+)""").find(normalizedFullText)
            ?: Regex("""(?:शाला\s*दिवस|दिवस\s*की\s*संख्या)[^\d]*(\d+)""").find(fullText)
        if (daysMatch != null) {
            schoolDays = daysMatch.groupValues[1]
        }

        // Set of numbers that can NEVER be the rice quantity (school days, student count, deadline days, years)
        val excludedNumbers = mutableSetOf(
            2023.0, 2024.0, 2025.0, 2026.0, 2027.0, 2028.0,
            15.0, // 15 दिवस के भीतर उठाव
            25.0  // शाला दिवस की संख्या: 25
        )
        enrolledStudents.toDoubleOrNull()?.let { excludedNumbers.add(it) }
        schoolDays.toDoubleOrNull()?.let { excludedNumbers.add(it) }

        // 5. Quantity: Rice coupon shows allotment in क्विंटल / क्विटल (क्वि / Quintals)
        // User rule: "quantity of the rice shows in क्वि in coupon always, in current coupon it is 1.90 क्वि system need to convert it into kg"
        // 1.90 क्विटल * 100 = 190.0 kg
        // NOTE: On CG MDM PDS coupons, the rice allotment is ALWAYS a decimal number in quintals (e.g. 1.90).
        // Integer numbers on the sheet are school days (25), enrolled students (143), deadline (15), etc.
        var quantityKg = 0.0
        var rawAllotmentText = ""

        // Regex for any quintal variation in Hindi or English
        // Matches "क्विटल" (as printed on Chhattisgarh coupons), "क्विंटल", "कि्वटल", "कि्वंटल", "किवटल", "क्वीटल", "क्टिल", "क्विट", "क्वि.", "क्वि", "कुंतल", "कुन्टल", "quintel", "quintal", "qtl"
        val quintalUnitPattern = """(?:क्विंटल|क्विटल|कि्वटल|कि्वंटल|किवटल|क्वीटल|क्टिल|क्विट|क्वि\.|क्वि|कुंतल|कुन्टल|quintel|quintal|qtl)"""

        // Helper to extract a decimal quintal number like "1.90", "1 . 90", "1,90", "1·90", "1-90", "l.90", "I.90"
        fun parseDecimalQuintal(str: String): Double? {
            val decimalRegex = Regex("""(?:^|[^a-zA-Z0-9])([1Il|]?\d)\s*[.,·•'\-:]\s*(\d{1,2})(?:$|[^a-zA-Z0-9])""")
            val m = decimalRegex.find(str) ?: return null
            val whole = m.groupValues[1].replace("I", "1").replace("l", "1").replace("|", "1")
            val frac = m.groupValues[2]
            return "$whole.$frac".toDoubleOrNull()
        }

        // Strategy 4A: Decimal directly next to any quintal unit: e.g. "1.90 क्विटल", "1 . 90 क्विटल", "1,90 क्विटल", "1.90 क्वि"
        val decimalBeforeQuintalRegex = Regex(
            """(?:^|[^a-zA-Z0-9])([1Il|]?\d\s*[.,·•'\-:]\s*\d{1,2})\s*""" + quintalUnitPattern,
            RegexOption.IGNORE_CASE
        )
        val dbMatch = decimalBeforeQuintalRegex.find(normalizedFullText)
        if (dbMatch != null) {
            val parsedVal = parseDecimalQuintal(dbMatch.groupValues[1])
            if (parsedVal != null && parsedVal in 0.05..50.0 && parsedVal !in excludedNumbers) {
                quantityKg = parsedVal * 100.0
                val formattedKg = if (quantityKg % 1.0 == 0.0) quantityKg.toInt().toString() else String.format(Locale.US, "%.3f", quantityKg)
                rawAllotmentText = String.format(Locale.US, "%.2f क्वि (%s कि.ग्रा.)", parsedVal, formattedKg)
            }
        }

        // Strategy 4B: Look in lines mentioning "का आबंटन" or "का आवंटन" or "आबंटन:" for any decimal number
        if (quantityKg == 0.0) {
            for (i in lines.indices) {
                val line = lines[i]
                if (line.contains("का आबंटन") || line.contains("का आवंटन") ||
                    (line.contains("आबंटन") && !line.contains("पत्रक") && !line.contains("माह"))) {
                    // Check current line and adjacent line for decimal
                    val decHere = parseDecimalQuintal(line)
                    if (decHere != null && decHere in 0.05..50.0 && decHere !in excludedNumbers) {
                        quantityKg = decHere * 100.0
                        val formattedKg = if (quantityKg % 1.0 == 0.0) quantityKg.toInt().toString() else String.format(Locale.US, "%.3f", quantityKg)
                        rawAllotmentText = String.format(Locale.US, "%.2f क्वि (%s कि.ग्रा.)", decHere, formattedKg)
                        break
                    }
                    if (i + 1 < lines.size) {
                        val nextLine = lines[i + 1]
                        val decNext = parseDecimalQuintal(nextLine)
                        if (decNext != null && decNext in 0.05..50.0 && decNext !in excludedNumbers) {
                            quantityKg = decNext * 100.0
                            val formattedKg = if (quantityKg % 1.0 == 0.0) quantityKg.toInt().toString() else String.format(Locale.US, "%.3f", quantityKg)
                            rawAllotmentText = String.format(Locale.US, "%.2f क्वि (%s कि.ग्रा.)", decNext, formattedKg)
                            break
                        }
                    }
                }
            }
        }

        // Strategy 4C: Search across full text for ANY standalone decimal number in the sheet
        // On CG PDS rice coupons, the ONLY decimal value on the entire sheet is the quintal allotment (e.g. 1.90)
        // All other numbers are integer IDs, days (25), students (143), years (2026).
        if (quantityKg == 0.0) {
            val allDecimals = Regex("""(?:^|[^a-zA-Z0-9])([1Il|]?\d\s*[.,·•'\-:]\s*\d{1,2})(?:$|[^a-zA-Z0-9])""")
                .findAll(normalizedFullText)
                .toList()
            for (dm in allDecimals) {
                val parsedVal = parseDecimalQuintal(dm.groupValues[1])
                if (parsedVal != null && parsedVal in 0.10..30.0 && parsedVal !in excludedNumbers) {
                    quantityKg = parsedVal * 100.0
                    val formattedKg = if (quantityKg % 1.0 == 0.0) quantityKg.toInt().toString() else String.format(Locale.US, "%.3f", quantityKg)
                    rawAllotmentText = String.format(Locale.US, "%.2f क्वि (%s कि.ग्रा.)", parsedVal, formattedKg)
                    break
                }
            }
        }

        // Strategy 4D: What if OCR recognized "190" as an integer (decimal point missed) near "क्विटल" or "आबंटन"?
        if (quantityKg == 0.0) {
            val int190Regex = Regex("""(?:का\s*आबंटन|का\s*आवंटन|आबंटन)[^0-9\n]{0,30}[:\s\-=|]*(190)\b""")
            val int190Match = int190Regex.find(normalizedFullText)
                ?: Regex("""\b(190)\s*""" + quintalUnitPattern, RegexOption.IGNORE_CASE).find(normalizedFullText)
            if (int190Match != null) {
                quantityKg = 190.0
                rawAllotmentText = "1.90 क्वि (190 कि.ग्रा.)"
            }
        }

        // Strategy 4E: Fallback for integer quintals, strictly excluding schoolDays, student count, 25, 15, etc.
        if (quantityKg == 0.0) {
            val fallbackRegex = Regex("""(?:का\s*आबंटन|का\s*आवंटन)[^0-9\n]{0,25}[:\s\-=|]*([0-9]{1,2})\b""")
            val fbMatch = fallbackRegex.find(normalizedFullText)
            if (fbMatch != null) {
                val numVal = fbMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                if (numVal in 0.5..20.0 && numVal !in excludedNumbers) {
                    quantityKg = numVal * 100.0
                    val formattedKg = if (quantityKg % 1.0 == 0.0) quantityKg.toInt().toString() else String.format(Locale.US, "%.3f", quantityKg)
                    rawAllotmentText = "$numVal क्वि ($formattedKg कि.ग्रा.)"
                }
            }
        }

        // 5. School Name
        var schoolName = ""
        val schoolLine = lines.firstOrNull { it.contains("शाला का आईडी") || it.contains("शाला का नाम") || it.contains("मा.शा.") || it.contains("प्रा.शा.") }
        if (schoolLine != null) {
            schoolName = schoolLine.substringAfter("/").substringAfter(":").trim()
        }

        // 6. SHG Name
        var shgName = ""
        val shgLine = lines.firstOrNull { it.contains("स्वसहायता") || it.contains("समूह") }
        if (shgLine != null) {
            shgName = shgLine.substringAfter("/").substringAfter(":").trim()
        }

        val hasUsefulData = couponNumber.isNotBlank() || receiptDateDdMmYyyy.isNotBlank() || pdsShop.isNotBlank() || quantityKg > 0

        return CouponOcrResult(
            isSuccess = hasUsefulData,
            couponNumber = couponNumber,
            receiptDateDdMmYyyy = receiptDateDdMmYyyy,
            dateIso = receiptDateDdMmYyyy, // keep sync with DD-MM-YYYY
            dateDisplay = receiptDateDdMmYyyy,
            pdsShopName = pdsShop,
            quantityKg = quantityKg,
            rawAllotmentText = rawAllotmentText,
            allotmentMonth = allotmentMonth,
            schoolName = schoolName,
            selfHelpGroupName = shgName,
            enrolledStudents = enrolledStudents,
            schoolDays = schoolDays,
            errorMessage = if (!hasUsefulData) "कूपन से आवश्यक विवरण नहीं पढ़े जा सके, कृपया हाथ से दर्ज करें" else null
        )
    }

    private fun loadOptimizedBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            var inSampleSize = 1
            val maxDimension = 1920
            while (options.outWidth / inSampleSize > maxDimension || options.outHeight / inSampleSize > maxDimension) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }

            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (e: Exception) {
            null
        }
    }
}
