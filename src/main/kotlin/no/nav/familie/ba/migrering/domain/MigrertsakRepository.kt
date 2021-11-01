package no.nav.familie.ba.migrering.domain

import no.nav.familie.ba.migrering.domain.common.InsertUpdateRepository
import no.nav.familie.ba.migrering.domain.common.RepositoryInterface
import java.util.UUID

interface MigrertsakRepository : RepositoryInterface<Migrertsak, UUID>, InsertUpdateRepository<Migrertsak>
