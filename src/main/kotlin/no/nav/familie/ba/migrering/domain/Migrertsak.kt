package no.nav.familie.ba.migrering.domain

import no.nav.familie.kontrakter.felles.objectMapper
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

    @Column("feiltype")
    val feiltype: String? = null,

    @Column("aarsak")
    val aarsak: String? = null,

    @Column("call_id")
    val callId: String = "",

    @Column("resultat_fra_ba")
    val resultatFraBa: JsonWrapper? = null,
)

enum class MigreringStatus {
    UKJENT,
    MIGRERT_I_BA,
    FEILET,
    VERIFISERT
}

data class JsonWrapper(val jsonStr: String?) {
    companion object {
        fun of(obj: Any?): JsonWrapper = JsonWrapper(if (obj != null) objectMapper.writeValueAsString(obj) else null)
    }
}
