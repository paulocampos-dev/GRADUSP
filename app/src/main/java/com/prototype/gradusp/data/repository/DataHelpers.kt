package com.prototype.gradusp.data.repository

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Type adapter for converting LocalTime to/from JSON
 */
class LocalTimeTypeAdapter : TypeAdapter<LocalTime>() {
    private val formatter = DateTimeFormatter.ISO_LOCAL_TIME

    override fun write(out: JsonWriter, value: LocalTime?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(formatter.format(value))
        }
    }

    override fun read(reader: JsonReader): LocalTime? {
        val timeString = reader.nextString()
        return if (timeString == null || timeString.isEmpty()) {
            null
        } else {
            LocalTime.parse(timeString, formatter)
        }
    }
}

/**
 * Type adapter for converting DayOfWeek to/from JSON
 */
class DayOfWeekTypeAdapter : TypeAdapter<DayOfWeek>() {
    override fun write(out: JsonWriter, value: DayOfWeek?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.value)
        }
    }

    override fun read(reader: JsonReader): DayOfWeek? {
        val value = reader.nextInt()
        return DayOfWeek.of(value)
    }
}