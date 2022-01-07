package no.nav.familie.ba.migrering.domain

import no.nav.familie.ba.migrering.domain.common.InsertUpdateRepository
import no.nav.familie.ba.migrering.domain.common.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.sql.SQLException
import java.util.UUID

@Repository
@Transactional
interface MigrertsakRepository : RepositoryInterface<Migrertsak, UUID>, InsertUpdateRepository<Migrertsak> {
    fun findByStatusAndPersonIdent(status: MigreringStatus, personIdent: String): List<Migrertsak>

    fun findByStatusIn(status: List<MigreringStatus>): List<Migrertsak>

    @Query("""SELECT ms.feiltype, count(ms.feiltype) as antall from migrertesaker ms where ms.status = 'FEILET' group by ms.feiltype""")
    fun tellFeiledeMigrerteSaker(): List<TellFeilResponse>

}

class TellFeilResponse(val feiltype: String, val antall: Int)

@Repository
@Transactional
class MigrertsakRepositoryForJsonQuery(private val jdbcTemplate: NamedParameterJdbcTemplate) {

    fun finnMedBaResultat(jsonFieldNavn: String, jsonFieldVerdi: String): List<Migrertsak> {
        val sql =
            """SELECT ms.* from migrertesaker ms where ms.resultat_fra_ba->>'""" + jsonFieldNavn + """' = :verdi""".trimMargin()
        val parameters = MapSqlParameterSource().addValue("verdi", jsonFieldVerdi)

        return jdbcTemplate.query(
            sql,
            parameters,
            MigrertsakRowMapper(),
        ).filterNotNull().map { it!! }
    }
}

class MigrertsakRowMapper : RowMapper<Migrertsak?> {

    @Throws(SQLException::class)
    override fun mapRow(rs: ResultSet, rowNum: Int): Migrertsak {

        return Migrertsak(
            id = UUID.fromString(rs.getString("id")),
            callId = rs.getString("call_id"),
            status = MigreringStatus.valueOf(rs.getString("status")),
            aarsak = rs.getString("aarsak"),
            resultatFraBa = JsonWrapper(rs.getString("resultat_fra_ba")),
            migreringsdato = rs.getTimestamp("migreringsdato").toLocalDateTime(),
            personIdent = rs.getString("person_ident")
        )
    }
}
