package ru.georglider.prod.repository.advertiser

import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import ru.georglider.prod.model.advertiser.Advertiser
import java.util.UUID

@Repository
interface AdvertiserRepository : R2dbcRepository<Advertiser, UUID>