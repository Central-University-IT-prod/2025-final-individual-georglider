package ru.georglider.prod.payload.converters

import org.springframework.context.annotation.Bean
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions
import org.springframework.data.r2dbc.dialect.SqlServerDialect
import org.springframework.stereotype.Component

@Component
class R2DBCConverters {

    @Bean
    fun customConversions(): R2dbcCustomConversions {
        val converters = listOf(
            GenderConverters.Companion.GenderWriteConverter(),
            GenderConverters.Companion.GenderReadConverter(),
            RecordTypeConverters.Companion.RecordTypeWriteConverter(),
            RecordTypeConverters.Companion.RecordTypeReadConverter(),
        )
        return R2dbcCustomConversions.of(SqlServerDialect.INSTANCE, converters)
    }

}