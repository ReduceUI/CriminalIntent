package com.bignerdranch.android.criminalintent.ui

import android.app.Dialog
import android.app.TimePickerDialog
import android.icu.util.Calendar
import android.os.Bundle
import android.widget.TimePicker
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import java.util.Date
import java.util.GregorianCalendar

class TimePickerFragment : DialogFragment() {

    private val args: TimePickerFragmentArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val timeListener = TimePickerDialog.OnTimeSetListener {
                _: TimePicker, hourOfDay: Int, minute: Int ->

            val originalCalendar = Calendar.getInstance()
            originalCalendar.time = args.crimeDate
            val year = originalCalendar.get(Calendar.YEAR)
            val month = originalCalendar.get(Calendar.MONTH)
            val day = originalCalendar.get(Calendar.DAY_OF_MONTH)

            val resultDate: Date = GregorianCalendar(
                year,
                month,
                day,
                hourOfDay,
                minute
            ).time

            setFragmentResult(REQUEST_KEY_TIME, bundleOf(BUNDLE_KEY_TIME to resultDate))
        }

        val calendar = Calendar.getInstance()
        calendar.time = args.crimeDate
        val initialHour = calendar.get(Calendar.HOUR_OF_DAY)
        val initialMinute = calendar.get(Calendar.MINUTE)

        return TimePickerDialog(
            requireContext(),
            timeListener,
            initialHour,
            initialMinute,
            false
        )
    }

    companion object {
        const val REQUEST_KEY_TIME = "REQUEST_KEY_TIME"
        const val BUNDLE_KEY_TIME = "BUNDLE_KEY_TIME"
    }
}
