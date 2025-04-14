package ru.georglider.prod.model.client

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import ru.georglider.prod.model.common.Gender
import ru.georglider.prod.payload.request.client.BulkEntity
import java.util.*

@Table("clients")
data class Client(
    @field:Column("id") @field:Id val clientId: UUID,
    val login: String,
    val age: Int,
    val location: String,
    val gender: Gender
) {
    constructor(entity: BulkEntity) : this(entity.clientId!!, entity.login!!, entity.age!!, entity.location!!, Gender.valueOf(entity.gender!!))
}
