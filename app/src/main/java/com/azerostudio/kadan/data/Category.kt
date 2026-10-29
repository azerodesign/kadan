package com.azerostudio.kadan.data

/*
 * The five categories, exactly as locked for Kadan v1, in Indonesian.
 * PR is the top-priority category: it gets the weekday 15:15 capture nudge
 * and it is sorted first in the "Hari Ini" counts. Do not rename these
 * without a migration note — they are stored as plain text in Room.
 */
enum class Category(val label: String) {
    PR("PR"),
    PEKERJAAN_SEKOLAH("Pekerjaan Sekolah"),
    PEKERJAAN_BISNIS("Pekerjaan Bisnis"),
    PLAN("Plan"),
    RUTINITAS("Rutinitas");

    companion object {
        fun fromLabel(label: String): Category =
            entries.firstOrNull { it.label == label } ?: PLAN
    }
}
