package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Formatters {
    private val idLocale = Locale("id", "ID")
    private val rupiahFormat = NumberFormat.getNumberInstance(idLocale)

    fun formatRupiah(amount: Long): String {
        return "Rp ${rupiahFormat.format(amount)}"
    }

    fun formatGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 4..10 -> "Selamat pagi"
            in 11..14 -> "Selamat siang"
            in 15..17 -> "Selamat sore"
            else -> "Selamat malam"
        }
    }

    fun formatShortDayDate(): String {
        val sdf = SimpleDateFormat("EEEE, d MMM", idLocale)
        return sdf.format(Date()).replaceFirstChar { it.uppercase() }
    }

    fun formatBps(bps: Long): String {
        val safe = bps.coerceAtLeast(0L)
        return when {
            safe >= 1_000_000_000L -> String.format(Locale.US, "%.2f Gbps", safe / 1_000_000_000.0)
            safe >= 1_000_000L -> String.format(Locale.US, "%.2f Mbps", safe / 1_000_000.0)
            safe >= 1_000L -> String.format(Locale.US, "%.1f Kbps", safe / 1_000.0)
            else -> "$safe bps"
        }
    }

    fun formatBytes(bytes: Long): String {
        val safe = bytes.coerceAtLeast(0L)
        return when {
            safe >= 1_099_511_627_776L -> String.format(Locale.US, "%.2f TB", safe / 1_099_511_627_776.0)
            safe >= 1_073_741_824L -> String.format(Locale.US, "%.2f GB", safe / 1_073_741_824.0)
            safe >= 1_048_576L -> String.format(Locale.US, "%.1f MB", safe / 1_048_576.0)
            safe >= 1024L -> String.format(Locale.US, "%.1f KB", safe / 1024.0)
            else -> "$safe B"
        }
    }

    fun formatMegabytes(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024.0) {
            String.format(Locale.US, "%.1f GB", mb / 1024.0)
        } else {
            String.format(Locale.US, "%.0f MB", mb)
        }
    }

    fun formatStorageSize(bytes: Long): String {
        val safe = bytes.coerceAtLeast(0L)
        return when {
            safe >= 1_099_511_627_776L -> String.format(Locale.US, "%.2f TB", safe / 1_099_511_627_776.0)
            safe >= 1_073_741_824L -> String.format(Locale.US, "%.2f GB", safe / 1_073_741_824.0)
            safe >= 1_048_576L -> String.format(Locale.US, "%.2f MB", safe / 1_048_576.0)
            safe >= 1024L -> String.format(Locale.US, "%.1f KB", safe / 1024.0)
            else -> "$safe B"
        }
    }

    fun formatTime(millis: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", idLocale)
        return sdf.format(Date(millis))
    }

    fun formatDotTime(millis: Long): String {
        val sdf = SimpleDateFormat("HH.mm.ss", idLocale)
        return sdf.format(Date(millis))
    }

    fun formatDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM, HH:mm:ss", idLocale)
        return sdf.format(Date(millis))
    }
}
