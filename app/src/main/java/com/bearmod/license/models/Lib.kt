package com.bearmod.license.models

/**
 * Represents a library or file record (loosely based on `lib` table).
 */

data class Lib(
    val id: String,
    val file: String,
    val fileType: String?,
    val fileSize: Long?,
    val pass: String?,
    val time: Long?
)
