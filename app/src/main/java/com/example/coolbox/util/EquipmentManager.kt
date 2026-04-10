package com.example.coolbox.util

import java.util.*

object EquipmentManager {
    private val keywords = listOf("层", "室", "Layer", "Shelf", "微冻", "冷冻", "冷藏")

    /**
     * Build 30: Convert all Chinese ordinal numbers to Arabic digits.
     */
    fun normalizeToDigit(s: String): String {
        var result = s.replace("第", "").replace("室", "")
        val composites = listOf(
            "十二" to "12", "十三" to "13", "十四" to "14", "十五" to "15",
            "十六" to "16", "十七" to "17", "十八" to "18", "十九" to "19",
            "二十" to "20", "十一" to "11", "十" to "10"
        )
        composites.forEach { (cn, digit) -> result = result.replace(cn, digit) }
        val singles = listOf("一" to "1", "二" to "2", "三" to "3", "四" to "4", "五" to "5", "六" to "6", "七" to "7", "八" to "8", "九" to "9")
        singles.forEach { (cn, digit) -> result = result.replace(cn, digit) }
        return result
    }

    /**
     * V4.1.6: Standard zone templates for specialized equipment.
     */
    fun getStandardZones(base: String): List<String> {
        return when {
            base.contains("大冰箱") -> listOf("冷藏", "微冻", "冷冻 1层", "冷冻 2层", "冷冻 3层")
            base.contains("冰柜") -> listOf("1层", "2层", "3层", "4层", "5层", "6层")
            else -> emptyList()
        }
    }

    /**
     * V4.1.9: The SINGLE SOURCE OF TRUTH for creating location strings.
     * Standard: "Device Zone" (Space separated).
     */
    fun formatFullName(device: String, zone: String): String {
        val d = device.trim()
        val z = zone.trim()
        if (z.isEmpty() || z == "默认层") return d
        return "$d $z"
    }

    /**
     * V4.1.1: Aggressive consolidation, stripping equipment-specific suffixes.
     */
    fun extractBase(fullName: String): String {
        // Build 46: Split by " - " first, then by " " carefully
        var base = fullName.split(" - ")[0]
        
        // Build 47/48: Refined splitting for "Device Zone" format
        if (!fullName.contains(" - ")) {
            val parts = base.split(" ")
            if (parts.size > 1) {
                // Check backwards for the first part that looks like a base (e.g. "大冰箱")
                // Usually the base name is the first 1-2 words.
                // Standard: "大冰箱", "冰柜", "My Fridge"
                
                // If it starts with a known base-like prefix, we might want to keep it.
                // For this app, bases are usually single words ("大冰箱", "冰柜").
                // If the user uses "My Fridge", we handle it by checking keywords.
                val firstPart = parts[0]
                if (firstPart == "大冰箱" || firstPart == "冰柜") {
                    base = firstPart
                } else {
                    // Fallback to keyword stripping
                    val lastPart = parts.last()
                    if (keywords.any { lastPart.contains(it) } || lastPart.any { it.isDigit() }) {
                        base = parts.dropLast(1).joinToString(" ").trim()
                    }
                }
            }
        }

        if (base.contains("(")) {
            base = base.substringBefore("(").trim()
        }
        
        // Build 46: Prevent stripping the base name itself if it matches a keyword
        val baseCopy = base
        keywords.forEach { kw ->
            if (base.length > kw.length && base.endsWith(kw)) {
                base = base.substring(0, base.length - kw.length).trim()
            }
            val match = Regex("(\\d+)$kw$").find(base)
            if (match != null && base.length > match.groupValues[0].length) {
                base = base.substring(0, match.range.first).trim()
            }
        }
        return if (base.isBlank()) baseCopy else base.trim()
    }

    /**
     * Extracts the zone/layer suffix for a given base device name.
     */
    fun extractZone(fullName: String, base: String): String {
        if (fullName == base) return "默认层"
        val zone = fullName.removePrefix(base).trim()
            .removePrefix("-").trim()
            .removePrefix("(").removeSuffix(")").trim()
        
        return if (zone.isEmpty()) "默认层" else zone
    }
}
