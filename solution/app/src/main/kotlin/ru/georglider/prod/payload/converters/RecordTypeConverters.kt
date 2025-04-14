package ru.georglider.prod.payload.converters

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import ru.georglider.prod.model.advertiser.campaign.stats.RecordType

class RecordTypeConverters {

    companion object {

        @ReadingConverter
        class RecordTypeReadConverter : Converter<Short, RecordType> {
            override fun convert(source: Short): RecordType {
                return RecordType.entries[source.toInt()]
            }
        }

        @WritingConverter
        class RecordTypeWriteConverter : Converter<RecordType, Short> {
            override fun convert(source: RecordType): Short {
                return source.ordinal.toShort()
            }
        }

    }

}