package ru.georglider.prod.model.advertiser

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import ru.georglider.prod.payload.request.advertiser.BulkEntity
import java.util.*

@Table("advertisers")
data class Advertiser(
    @field:Column("id") @field:Id val advertiserId: UUID,
    val name: String
) {
    constructor(entity: BulkEntity) : this(entity.advertiserId!!, entity.name!!)
}
