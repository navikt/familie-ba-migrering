package no.nav.familie.ba.migrering.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

@Table("migrertesaker_logg")
data class MigrertsakLogg(
    @Id
    val id: UUID,

    @Column("person_ident")
    val personIdent: String = "",

    @Column("migreringsdato")
    val migreringsdato: LocalDateTime,

    @Column("status")
    val status: MigreringStatus,

    @Column("feiltype")
    val feiltype: String? = null,

    @Column("aarsak")
    val aarsak: String? = null,

    @Column("call_id")
    val callId: String = ""
) {

    companion object {

        fun tilMigrertsakLogg(migrertsak: Migrertsak): MigrertsakLogg {
            return MigrertsakLogg(
                id = migrertsak.id,
                personIdent = migrertsak.personIdent,
                migreringsdato = migrertsak.migreringsdato,
                status = migrertsak.status,
                feiltype = migrertsak.feiltype,
                aarsak = migrertsak.aarsak,
                callId = migrertsak.callId,
            )
        }
    }
}
