package ru.georglider.prod.model.internal.config

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table(name = "config")
data class ConfigEntity (
    @field:Id val id: Int,
    val value: String
) {
    fun getAsString(): String = value
    fun getAsInt(): Int = value.toInt()
}