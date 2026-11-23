package com.movie.app.best.util

/**
 * A genuine IMDb title id, e.g. tt0111161 or tt12345678.
 * Strict on purpose: arbitrary slugs that merely start with "tt" must NOT match,
 * otherwise a normal content slug could be mistaken for an IMDb id.
 */
private val IMDB_ID_REGEX = Regex("^tt\\d{7,9}$")

fun isRealImdbId(value: String?): Boolean =
    !value.isNullOrBlank() && IMDB_ID_REGEX.matches(value)
