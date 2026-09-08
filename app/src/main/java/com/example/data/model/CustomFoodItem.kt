package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class CustomFoodItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val nameHi: String = "",
    val quantity: Double, // standard quantity per student
    val unit: String = "kg", // "kg", "g", "Pcs", "Dozen", "Ltr", "ml", "pkt"
    val rate: Double = 0.0, // optional cost rate in ₹ per student
    val isEnabled: Boolean = false
) {
    fun getDisplayName(isHindi: Boolean): String {
        return if (isHindi && nameHi.isNotBlank()) nameHi else name
    }

    fun getDisplayUnit(isHindi: Boolean): String {
        return when (unit.lowercase()) {
            "pcs" -> if (isHindi) "Pcs (नग)" else "Pcs"
            "dozen", "duzzen", "दर्जन" -> if (isHindi) "दर्जन (Dozen)" else "Dozen"
            "kg" -> if (isHindi) "किग्रा (kg)" else "kg"
            "g" -> if (isHindi) "ग्राम (g)" else "g"
            "ltr", "l" -> if (isHindi) "लीटर (Ltr)" else "Ltr"
            "ml" -> if (isHindi) "मि.ली. (ml)" else "ml"
            "pkt" -> if (isHindi) "पैकेट (pkt)" else "pkt"
            else -> unit
        }
    }

    fun getBilingualUnit(): String {
        return when (unit.lowercase()) {
            "pcs" -> "Pcs / नग"
            "dozen", "duzzen", "दर्जन" -> "Dozen / दर्जन"
            "kg" -> "kg / किग्रा"
            "g" -> "g / ग्राम"
            "ltr", "l" -> "Ltr / लीटर"
            "ml" -> "ml / मि.ली."
            "pkt" -> "pkt / पैकेट"
            else -> unit
        }
    }

    fun formattedQuantity(): String {
        val u = unit.trim().lowercase()
        if (u == "kg" || u == "ml" || u == "l" || u == "ltr") {
            return String.format(java.util.Locale.US, "%.3f", quantity)
        }
        return if (quantity == quantity.toLong().toDouble()) {
            quantity.toLong().toString()
        } else {
            String.format(java.util.Locale.US, "%.3f", quantity).trimEnd('0').trimEnd('.')
        }
    }
}

object CustomFoodItemParser {
    fun parse(json: String?): List<CustomFoodItem> {
        if (json.isNullOrBlank()) return getDefaultCustomItems()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<CustomFoodItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val rawUnit = obj.optString("unit", "kg")
                val normalizedUnit = when (rawUnit.lowercase()) {
                    "pcs", "nos" -> "Pcs"
                    "duzzen", "dozen", "दर्जन" -> "Dozen"
                    "ltr", "l" -> "Ltr"
                    else -> rawUnit
                }
                list.add(
                    CustomFoodItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", "Item"),
                        nameHi = obj.optString("nameHi", ""),
                        quantity = obj.optDouble("quantity", 0.0),
                        unit = normalizedUnit,
                        rate = obj.optDouble("rate", 0.0),
                        isEnabled = obj.optBoolean("isEnabled", true)
                    )
                )
            }
            if (list.isEmpty()) {
                getDefaultCustomItems()
            } else if (list.size <= 3 && list.any { it.id == "item_fruit" || it.id == "item_chikki" }) {
                // Migrate legacy template items to standard CG MDM items
                getDefaultCustomItems()
            } else {
                // Ensure IFA and Deworming are separated and names updated
                val migratedList = mutableListOf<CustomFoodItem>()
                for (item in list) {
                    if (item.id == "item_iron_folic_acid" || item.id == "iron_folic_acid") {
                        migratedList.add(
                            CustomFoodItem(
                                id = "item_ifa",
                                name = "Iron Folic Acid (IFA)",
                                nameHi = "आयरन फोलिक एसिड (IFA)",
                                quantity = 1.0,
                                unit = "Pcs",
                                rate = 0.0,
                                isEnabled = item.isEnabled
                            )
                        )
                        migratedList.add(
                            CustomFoodItem(
                                id = "item_deworming",
                                name = "Deworming Tablets (Albendazole)",
                                nameHi = "कृमिनाशक (अल्बेंडाजोल)",
                                quantity = 1.0,
                                unit = "Pcs",
                                rate = 0.0,
                                isEnabled = item.isEnabled
                            )
                        )
                    } else if (item.id.contains("dudh", ignoreCase = true) || item.name.contains("Soya Dudh", ignoreCase = true) || item.nameHi.contains("Soya Dudh", ignoreCase = true)) {
                        migratedList.add(
                            item.copy(
                                name = "Soya Milk",
                                nameHi = "सोयादूध (Soya Milk)"
                            )
                        )
                    } else if (item.id == "item_ifa") {
                        migratedList.add(
                            item.copy(
                                name = "Iron Folic Acid (IFA)",
                                nameHi = "आयरन फोलिक एसिड (IFA)"
                            )
                        )
                    } else {
                        migratedList.add(item)
                    }
                }
                migratedList
            }
        } catch (e: Exception) {
            getDefaultCustomItems()
        }
    }

    fun toJson(items: List<CustomFoodItem>): String {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("nameHi", item.nameHi)
            obj.put("quantity", item.quantity)
            obj.put("unit", item.unit)
            obj.put("rate", item.rate)
            obj.put("isEnabled", item.isEnabled)
            array.put(obj)
        }
        return array.toString()
    }

    fun parseUsedItemIds(json: String?): Set<String> {
        if (json.isNullOrBlank()) return emptySet()
        return try {
            val array = JSONArray(json)
            val set = mutableSetOf<String>()
            for (i in 0 until array.length()) {
                val id = array.getString(i)
                if (id == "item_iron_folic_acid" || id == "iron_folic_acid") {
                    set.add("item_ifa")
                    set.add("item_deworming")
                } else {
                    set.add(id)
                }
            }
            set
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun usedItemIdsToJson(ids: Collection<String>): String {
        val array = JSONArray()
        for (id in ids) {
            array.put(id)
        }
        return array.toString()
    }

    fun getDefaultCustomItems(): List<CustomFoodItem> = listOf(
        CustomFoodItem(
            id = "item_soyabadi",
            name = "Soyabadi (Soya Chunks)",
            nameHi = "सोयाबड़ी (Soyabadi)",
            quantity = 0.025,
            unit = "kg",
            rate = 0.0,
            isEnabled = true
        ),
        CustomFoodItem(
            id = "item_soyadudh",
            name = "Soya Milk",
            nameHi = "सोयादूध (Soya Milk)",
            quantity = 0.200,
            unit = "Ltr",
            rate = 0.0,
            isEnabled = true
        ),
        CustomFoodItem(
            id = "item_ifa",
            name = "Iron Folic Acid (IFA)",
            nameHi = "आयरन फोलिक एसिड (IFA)",
            quantity = 1.0,
            unit = "Pcs",
            rate = 0.0,
            isEnabled = true
        ),
        CustomFoodItem(
            id = "item_deworming",
            name = "Deworming Tablets (Albendazole)",
            nameHi = "कृमिनाशक (अल्बेंडाजोल)",
            quantity = 1.0,
            unit = "Pcs",
            rate = 0.0,
            isEnabled = true
        )
    )
}
