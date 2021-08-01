package com.byteseb.grafobook.utils

import android.content.Context
import android.text.format.DateUtils
import android.text.format.DateUtils.isToday
import com.byteseb.grafobook.R
import java.text.SimpleDateFormat
import java.util.*

class TimeUtils {

    companion object {
        fun isYesterday(date: Date): Boolean {
            return DateUtils.isToday(date.time + DateUtils.DAY_IN_MILLIS)
        }

        fun isTomorrow(date: Date): Boolean {
            return DateUtils.isToday(date.time - DateUtils.DAY_IN_MILLIS)
        }

        fun isYesterday(date: Long): Boolean {
            return DateUtils.isToday(date + DateUtils.DAY_IN_MILLIS)
        }

        fun isTomorrow(date: Long): Boolean {
            return DateUtils.isToday(date - DateUtils.DAY_IN_MILLIS)
        }

        fun isFutureDate(futureDate: Long): Boolean{
            val ogCalendar = GregorianCalendar()
            ogCalendar.timeInMillis = System.currentTimeMillis()
            val futureCalendar = GregorianCalendar()
            futureCalendar.timeInMillis = futureDate
            return futureCalendar.timeInMillis > ogCalendar.timeInMillis
        }

        fun inPresentYear(date: Long): Boolean {
            val presentCalendar = GregorianCalendar()
            val presentYear = presentCalendar.get(GregorianCalendar.YEAR)
            val ogCalendar = GregorianCalendar()
            ogCalendar.timeInMillis = date
            val ogYear = ogCalendar.get(GregorianCalendar.YEAR)
            return presentYear == ogYear
        }

        fun getSimpleDate(milliseconds: Long, context: Context): String {
            val formattedDate: String
            if (isToday(milliseconds)) {
                //If it is today, just show the hour and minute
                formattedDate = SimpleDateFormat("HH:mm", Locale.getDefault()).format(milliseconds)
            } else if (isYesterday(milliseconds)) {
                //If it was yesterday, show "yesterday" with the hour and minute
                formattedDate = context.getString(R.string.yesterday) + SimpleDateFormat(
                    " HH:mm",
                    Locale.getDefault()
                ).format(milliseconds)
            } else if(isTomorrow(milliseconds)){
                //If it is tomorrow, show "tomorrow" with hour and minute
                formattedDate = context.getString(R.string.tomorrow) + SimpleDateFormat(" HH:mm",
                Locale.getDefault()).format(milliseconds)
            }
            else {
                if(inPresentYear(milliseconds)){
                    formattedDate = SimpleDateFormat(
                        context.getString(R.string.month_date_hour_minute),
                        Locale.getDefault()
                    ).format(milliseconds)
                }
                else{
                    formattedDate = SimpleDateFormat(
                        context.getString(R.string.year_month_date_hour_minute),
                        Locale.getDefault()
                    ).format(milliseconds)
                }
            }
            return formattedDate
        }
    }
}