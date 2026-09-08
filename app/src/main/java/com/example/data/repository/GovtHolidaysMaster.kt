package com.example.data.repository

import com.example.data.local.entity.CalendarEventEntity
import java.text.SimpleDateFormat
import java.util.*

object GovtHolidaysMaster {

    const val GOVT_HOLIDAY_LIST_VERSION = "2026.3.0-CG-OFFICIAL"
    const val LAST_UPDATED_DATE = "2026-08-25"

    /**
     * Complete official list of 2026 Chhattisgarh State & Central Government holidays,
     * Education Department vacations, and local festival days.
     */
    fun getOfficialGovtHolidaysForYear(year: Int): List<CalendarEventEntity> {
        return when (year) {
            2026 -> getHolidays2026()
            2027 -> getHolidays2027()
            else -> getHolidays2026()
        }
    }

    private fun getHolidays2026(): List<CalendarEventEntity> {
        val list = mutableListOf<CalendarEventEntity>()

        // 1. NATIONAL / CENTRAL GOVT HOLIDAYS
        list.add(
            CalendarEventEntity(
                eventDate = "2026-01-26",
                eventName = "गणतंत्र दिवस (Republic Day)",
                eventType = "NATIONAL",
                isMealReportingRequired = true,
                orderNumber = "CG-ED-2026/01",
                authority = "Central Govt / Dept of School Education",
                remarks = "National Festival - Flag hoisting & special mid-day meal mandatory"
            )
        )
        list.add(
            CalendarEventEntity(
                eventDate = "2026-02-15",
                eventName = "महाशिवरात्रि (Maha Shivratri)",
                eventType = "NATIONAL",
                isMealReportingRequired = false,
                authority = "Govt of India / CG Gazetted"
            )
        )
        list.add(
            CalendarEventEntity(
                eventDate = "2026-03-04",
                eventName = "होली / धूलिवंदन (Holi / Dhulandi)",
                eventType = "NATIONAL",
                isMealReportingRequired = false,
                authority = "Govt of India / CG Gazetted"
            )
        )
        list.add(
            CalendarEventEntity(
                eventDate = "2026-03-21",
                eventName = "ईद-उल-फितर (Id-ul-Fitr / Ramadan)",
                eventType = "NATIONAL",
                isMealReportingRequired = false,
                authority = "Govt of India"
            )
        )
        list.add(
            CalendarEventEntity(
                eventDate = "2026-04-03",
                eventName = "गुड फ्राइडे (Good Friday)",
                eventType = "NATIONAL",
                isMealReportingRequired = false,
                authority = "Govt of India"
            )
        )
        list.add(
            CalendarEventEntity(
                eventDate = "2026-04-14",
                eventName = "डॉ. भीमराव अम्बेडकर जयंती (Dr. B.R. Ambedkar Jayanti)",
                eventType = "NATIONAL",
                isMealReportingRequired = false,
                authority = "Govt of India / CG Gazetted"
            )
        )
        list.add(
            CalendarEventEntity(
                eventDate = "2026-05-01",
                eventName = "बुद्ध पूर्णिमा / मजदूर दिवस (Buddha Purnima / May Day)",
                eventType = "NATIONAL",
                isMealReportingRequired = false,
                authority = "Govt of India"
            )
        )
        list.add(
            CalendarEventEntity(
                eventDate = "2026-05-28",
                eventName = "बकरीद / ईद-उल-जुहा (Bakrid / Id-ul-Zuha)",
                eventType = "NATIONAL",
                isMealReportingRequired = false,
                authority = "Govt of India"
            )
        )
        list.add(
            CalendarEventEntity(
                eventDate = "2026-06-26",
                eventName = "मोहर्रम (Muharram)",
                eventType = "NATIONAL",
                isMealReportingRequired = false,
                authority = "Govt of India"
            )
        )
        list.add(
            CalendarEventEntity(
                eventDate = "2026-08-15",
                eventName = "स्वतंत्रता दिवस (Independence Day)",
                eventType = "NATIONAL",
                isMealReportingRequired = true,
                orderNumber = "CG-ED-2026/78",
                authority = "Central Govt / Dept of School Education",
                remarks = "National Festival - Flag hoisting & special mid-day meal mandatory"
            )
        )
        list.add(
            CalendarEventEntity(
                eventDate = "2026-08-26",
                eventName = "मिलाद-उन-नबी / ईद-ए-मिलाद (Milad-un-Nabi)",
                eventType = "NATIONAL",
                isMealReportingRequired = false,
                authority = "Govt of India"
            )
        )
        list.add(
            CalendarEventEntity(
                eventDate = "2026-10-02",
                eventName = "महात्मा गांधी जयंती (Mahatma Gandhi Jayanti)",
                eventType = "NATIONAL",
                isMealReportingRequired = false,
                authority = "Govt of India"
            )
        )
        list.add(
            CalendarEventEntity(
                eventDate = "2026-10-20",
                eventName = "दशहरा / विजयदशमी (Dussehra / Vijaya Dashami)",
                eventType = "NATIONAL",
                isMealReportingRequired = false,
                authority = "Govt of India / CG Gazetted"
            )
        )
        list.add(
            CalendarEventEntity(
                eventDate = "2026-11-08",
                eventName = "दीपावली (Deepawali)",
                eventType = "NATIONAL",
                isMealReportingRequired = false,
                authority = "Govt of India / CG Gazetted"
            )
        )
        list.add(
            CalendarEventEntity(
                eventDate = "2026-11-24",
                eventName = "गुरु नानक जयंती (Guru Nanak Jayanti)",
                eventType = "NATIONAL",
                isMealReportingRequired = false,
                authority = "Govt of India"
            )
        )
        list.add(
            CalendarEventEntity(
                eventDate = "2026-12-25",
                eventName = "क्रिसमस (Christmas Day)",
                eventType = "NATIONAL",
                isMealReportingRequired = false,
                authority = "Govt of India"
            )
        )

        // 2. CHHATTISGARH STATE GAZETTED & REGIONAL HOLIDAYS
        list.add(
            CalendarEventEntity(
                eventDate = "2026-01-03",
                eventName = "मां शाकम्भरी जयंती एवं छेरछेरा (Maa Shakambhari Jayanti & Chherchera)",
                eventType = "STATE",
                isMealReportingRequired = false,
                orderNumber = "CG-GA-2026/02",
                authority = "General Administration Dept, Govt of CG",
                remarks = "CG State Gazetted Holiday"
            )
        )
        list.add(
            CalendarEventEntity(
                eventDate = "2026-03-31",
                eventName = "भक्त माता कर्मा जयंती (Bhakt Mata Karma Jayanti)",
                eventType = "STATE",
                isMealReportingRequired = false,
                orderNumber = "CG-GA-2026/18",
                authority = "Govt of Chhattisgarh"
            )
        )
        list.add(
            CalendarEventEntity(
                eventDate = "2026-08-09",
                eventName = "विश्व आदिवासी दिवस (World Tribal Day)",
                eventType = "STATE",
                isMealReportingRequired = false,
                orderNumber = "CG-GA-2026/33",
                authority = "Govt of Chhattisgarh"
            )
        )
        list.add(
            CalendarEventEntity(
                eventDate = "2026-08-27",
                eventName = "हरेली पर्व (Hareli Festival)",
                eventType = "STATE",
                isMealReportingRequired = false,
                orderNumber = "CG-GA-HOL-2026-14",
                authority = "Govt of Chhattisgarh",
                remarks = "Traditional Agricultural Festival of Chhattisgarh"
            )
        )
        list.add(
            CalendarEventEntity(
                eventDate = "2026-09-04",
                eventName = "तीजा / हरतालिका तीज (Teeja Festival)",
                eventType = "STATE",
                isMealReportingRequired = false,
                orderNumber = "CG-GA-2026/41",
                authority = "Govt of Chhattisgarh"
            )
        )
        list.add(
            CalendarEventEntity(
                eventDate = "2026-09-14",
                eventName = "गणेश चतुर्थी (Ganesh Chaturthi)",
                eventType = "STATE",
                isMealReportingRequired = false,
                authority = "Govt of Chhattisgarh"
            )
        )
        list.add(
            CalendarEventEntity(
                eventDate = "2026-11-01",
                eventName = "छत्तीसगढ़ राज्य स्थापना दिवस (CG Foundation Day)",
                eventType = "STATE",
                isMealReportingRequired = false,
                orderNumber = "CG-GA-2026/55",
                authority = "Govt of Chhattisgarh"
            )
        )
        list.add(
            CalendarEventEntity(
                eventDate = "2026-11-09",
                eventName = "गोवर्धन पूजा (Govardhan Puja / Matar)",
                eventType = "STATE",
                isMealReportingRequired = false,
                authority = "Govt of Chhattisgarh"
            )
        )
        list.add(
            CalendarEventEntity(
                eventDate = "2026-11-15",
                eventName = "छठ पूजा (Chhath Puja)",
                eventType = "STATE",
                isMealReportingRequired = false,
                authority = "Govt of Chhattisgarh"
            )
        )
        list.add(
            CalendarEventEntity(
                eventDate = "2026-12-18",
                eventName = "गुरु घासीदास जयंती (Guru Ghasidas Jayanti)",
                eventType = "STATE",
                isMealReportingRequired = false,
                orderNumber = "CG-GA-2026/62",
                authority = "Govt of Chhattisgarh"
            )
        )

        // 3. EDUCATION DEPARTMENT SPECIAL LONG VACATIONS
        // Dussehra Vacation (18 Oct to 23 Oct)
        generateDateRange(
            startDate = "2026-10-18",
            endDate = "2026-10-23",
            name = "दशहरा अवकाश (Dussehra Vacation)",
            type = "VACATION",
            orderNo = "CG/DSE/VAC-2026/04",
            authority = "School Education Dept, Govt of CG"
        ).forEach { list.add(it) }

        // Diwali Vacation (07 Nov to 12 Nov)
        generateDateRange(
            startDate = "2026-11-07",
            endDate = "2026-11-12",
            name = "दीपावली अवकाश (Diwali Vacation)",
            type = "VACATION",
            orderNo = "CG/DSE/VAC-2026/05",
            authority = "School Education Dept, Govt of CG"
        ).forEach { list.add(it) }

        // Winter Vacation (24 Dec to 31 Dec)
        generateDateRange(
            startDate = "2026-12-24",
            endDate = "2026-12-31",
            name = "शीतकालीन अवकाश (Winter Vacation)",
            type = "VACATION",
            orderNo = "CG/DSE/VAC-2026/06",
            authority = "School Education Dept, Govt of CG"
        ).forEach { list.add(it) }

        // 4. LOCAL / DISTRICT HOLIDAYS
        list.add(
            CalendarEventEntity(
                eventDate = "2026-07-16",
                eventName = "रथ यात्रा (Rath Yatra - Local District Holiday)",
                eventType = "LOCAL",
                isMealReportingRequired = false,
                orderNumber = "DM/COLL/LOC-2026/03",
                authority = "Collector & District Magistrate",
                remarks = "Declared by District Administration"
            )
        )
        list.add(
            CalendarEventEntity(
                eventDate = "2026-09-10",
                eventName = "पोला पर्व (Pola Festival - Local Holiday)",
                eventType = "LOCAL",
                isMealReportingRequired = false,
                orderNumber = "DM/COLL/LOC-2026/07",
                authority = "Collector & District Magistrate",
                remarks = "Traditional bullock worshipping day"
            )
        )

        return list.sortedBy { it.eventDate }
    }

    private fun getHolidays2027(): List<CalendarEventEntity> {
        val list = mutableListOf<CalendarEventEntity>()
        list.add(
            CalendarEventEntity(
                eventDate = "2027-01-26",
                eventName = "गणतंत्र दिवस (Republic Day)",
                eventType = "NATIONAL",
                isMealReportingRequired = true,
                orderNumber = "CG-ED-2027/01",
                authority = "Central Govt / Dept of School Education"
            )
        )
        list.add(
            CalendarEventEntity(
                eventDate = "2027-08-15",
                eventName = "स्वतंत्रता दिवस (Independence Day)",
                eventType = "NATIONAL",
                isMealReportingRequired = true,
                orderNumber = "CG-ED-2027/78",
                authority = "Central Govt / Dept of School Education"
            )
        )
        list.add(
            CalendarEventEntity(
                eventDate = "2027-10-02",
                eventName = "महात्मा गांधी जयंती (Mahatma Gandhi Jayanti)",
                eventType = "NATIONAL",
                isMealReportingRequired = false,
                authority = "Govt of India"
            )
        )
        return list
    }

    /**
     * Utility to generate single-day entries across a date range.
     */
    fun generateDateRange(
        startDate: String,
        endDate: String,
        name: String,
        type: String,
        orderNo: String = "",
        authority: String = "School Education Dept, Govt of CG",
        remarks: String = ""
    ): List<CalendarEventEntity> {
        val result = mutableListOf<CalendarEventEntity>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        try {
            val start = sdf.parse(startDate) ?: return emptyList()
            val end = sdf.parse(endDate) ?: return emptyList()
            val cal = Calendar.getInstance().apply { time = start }
            val endCal = Calendar.getInstance().apply { time = end }

            while (!cal.after(endCal)) {
                val dateStr = sdf.format(cal.time)
                result.add(
                    CalendarEventEntity(
                        eventDate = dateStr,
                        eventName = name,
                        eventType = type,
                        isMealReportingRequired = false,
                        orderNumber = orderNo,
                        authority = authority,
                        remarks = remarks
                    )
                )
                cal.add(Calendar.DAY_OF_MONTH, 1)
            }
        } catch (_: Exception) {
            // fallback
        }
        return result
    }

    /**
     * Compute count of days between two dates inclusive.
     */
    fun calculateDaysBetween(startDate: String, endDate: String): Int {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val start = sdf.parse(startDate)?.time ?: return 0
            val end = sdf.parse(endDate)?.time ?: return 0
            if (end < start) return 0
            val diffMs = end - start
            (diffMs / (1000 * 60 * 60 * 24)).toInt() + 1
        } catch (_: Exception) {
            0
        }
    }
}
