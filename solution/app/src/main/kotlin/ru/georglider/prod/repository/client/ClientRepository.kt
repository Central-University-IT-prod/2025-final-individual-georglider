package ru.georglider.prod.repository.client

import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import ru.georglider.prod.model.client.Client
import java.util.UUID

@Repository
interface ClientRepository : R2dbcRepository<Client, UUID>