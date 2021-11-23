package no.nav.familie.ba.migrering.domain.common

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.NoRepositoryBean

/**
 * På grunn av att vi setter id's på våre entitetet så prøver spring å oppdatere våre entiteter i stedet for å ta insert
 */
@NoRepositoryBean
interface RepositoryInterface<T, ID> : CrudRepository<T, ID>

