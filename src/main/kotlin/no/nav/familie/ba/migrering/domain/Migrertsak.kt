package no.nav.familie.ba.migrering.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

@Table("migrertesaker")
data class Migrertsak(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column("person_ident")
    val personIdent: String = "",

    @Column("migreringsdato")
    val migreringsdato: LocalDateTime = LocalDateTime.now(),

    @Column("status")
    val status: MigreringStatus = MigreringStatus.UKJENT,

    @Column("aarsak")
    val aarsak: String? = null,

    @Column("sak_nummer")
    val sakNummer: String = "",

    @Column("resultat_fra_ba")
    val resultatFraBa: String = "",
)

enum class MigreringStatus {
    UKJENT,
    MIGRERT_I_BA,
    FEILET,
    VERIFISERT,
}
