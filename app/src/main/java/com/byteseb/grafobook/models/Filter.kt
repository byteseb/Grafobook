package com.byteseb.grafobook.models

data class Filter(
    val text: String,
    var checked: Boolean = false,
    var favFilter: Boolean = false
)