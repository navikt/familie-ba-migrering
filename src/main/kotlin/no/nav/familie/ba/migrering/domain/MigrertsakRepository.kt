package no.nav.familie.ba.migrering.domain

import no.nav.familie.ba.migrering.domain.common.InsertUpdateRepository
import no.nav.familie.ba.migrering.domain.common.RepositoryInterface
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
@Transactional
interface MigrertsakRepository : RepositoryInterface<Migrertsak, UUID>, InsertUpdateRepository<Migrertsak>
