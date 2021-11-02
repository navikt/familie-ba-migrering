package no.nav.familie.ba.migrering.domain

import no.nav.familie.ba.migrering.domain.common.InsertUpdateRepository
import no.nav.familie.ba.migrering.domain.common.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
@Transactional
interface MigrertsakRepository : RepositoryInterface<Migrertsak, UUID>, InsertUpdateRepository<Migrertsak> {

    //TODO: implement json column query
    @Query("""select ms.* from migrertesaker ms where ms.resultat_fra_ba->>'test' = 'ok' """)
    fun finnMedBaResultat(): List<Migrertsak>
}
