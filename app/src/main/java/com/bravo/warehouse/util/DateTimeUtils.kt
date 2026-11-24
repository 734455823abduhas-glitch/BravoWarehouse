package com.bravo.warehouse.util

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateTimeUtils {

    private fun getPreferences(context: Context) =
        context.getSharedPreferences(
            "bravo_settings",
            Context.MODE_PRIVATE
        )

    fun formatDate(context: Context, timestamp: Long): String {
        val preferences = getPreferences(context)

        val pattern = when (
            preferences.getString("date_format", "dd/MM/yyyy")
        ) {
            "yyyy-MM-dd" -> "yyyy-MM-dd"
            "dd-MM-yyyy" -> "dd-MM-yyyy"
            else -> "dd/MM/yyyy"
        }

        return SimpleDateFormat(
            pattern,
            Locale.getDefault()
        ).format(Date(timestamp))
    }

    fun formatTime(context: Context, timestamp: Long): String {
        val preferences = getPreferences(context)

        val pattern = if (
            preferences.getString("time_format", "24") == "12"
        ) {
            "hh:mm a"
        } else {
            "HH:mm"
        }

        return SimpleDateFormat(
            pattern,
            Locale.getDefault()
        ).format(Date(timestamp))
    }

    fun formatDateTime(context: Context, timestamp: Long): String {
        return "${formatDate(context, timestamp)} - " +
            formatTime(context, timestamp)
    }
}
