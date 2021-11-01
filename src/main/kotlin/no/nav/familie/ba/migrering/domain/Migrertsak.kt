package no.nav.familie.ba.migrering.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

@Table("migrertesaker")
data class Migrertsak(
    @Id
    val id: UUID,

    @Column("person_ident")
    val personIdent: String,

    @Column("migreringsdato")
    val migreringsdato: LocalDateTime,

    @Column("status")
    val status: MigreringStatus,

    @Column("aarsak")
    val aarsak: String? = null,

    @Column("sak_nummer")
    val sakNummer: String,
)

enum class MigreringStatus {
    SUKKSESS,
    FEILET,
    VERIFISERT,
}
