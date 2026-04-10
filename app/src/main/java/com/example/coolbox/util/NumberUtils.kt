package com.example.coolbox.util

fun formatQuantity(value: Double): String {
    if (value <= 0.0) return "0"
    
    // Safer integer check
    val rounded = Math.round(value)
    if (Math.abs(value - rounded) < 0.0001) {
        return rounded.toString()
    }
    
    // Use US locale to ensure dot separator and avoid locale-specific comma issues
    val df = java.text.DecimalFormat("0.##", java.text.DecimalFormatSymbols(java.util.Locale.US))
    val result = df.format(value)
    // If it's extremely small, force show at least 2 decimals
    return if (result == "0") String.format(java.util.Locale.US, "%.2f", value) else result
}
