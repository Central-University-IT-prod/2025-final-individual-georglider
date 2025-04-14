package ru.georglider.prod.payload.converters

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import ru.georglider.prod.model.common.Gender

class GenderConverters {

    companion object {

        @ReadingConverter
        class GenderReadConverter : Converter<Short, Gender> {
            override fun convert(source: Short): Gender {
                return Gender.entries[source.toInt()]
            }
        }

        @WritingConverter
        class GenderWriteConverter : Converter<Gender, Short> {
            override fun convert(source: Gender): Short {
                return source.ordinal.toShort()
            }
        }

    }

}