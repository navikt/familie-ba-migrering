package no.nav.familie.ba.migrering.domain

import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.SQLException
import java.util.UUID


@Repository
class MigrertsakJDBCRepository(private val jdbcTemplate: NamedParameterJdbcTemplate) {

    fun lagre(migrertsak: Migrertsak): Migrertsak {
        val sql = """insert into migrertesaker(id, person_ident, migreringsdato, status, aarsak, resultat_fra_ba, sak_nummer) 
            values (:id, :personIdent, :migreringsdato, :status, :aarsak, to_json(:resultatFraBa::json), :sakNummer)"""
        val parameters = MapSqlParameterSource()
            .addValue("id", migrertsak.id)
            .addValue("personIdent", migrertsak.personIdent)
            .addValue("migreringsdato", migrertsak.migreringsdato)
            .addValue("status", migrertsak.status.name)
            .addValue("aarsak", migrertsak.aarsak)
            .addValue("resultatFraBa", migrertsak.resultatFraBa)
            .addValue("sakNummer", migrertsak.sakNummer)

        jdbcTemplate.update(sql, parameters)
        return migrertsak
    }

    fun oppdaterStatusÅrsakOgResultat(uuid: UUID, status: String, aarsak: String, resultat: String): Migrertsak {
        val updateQuery = "update migrertesaker set status = :status, aarsak= :aarsak, resultat_fra_ba = to_json(:resultatFraBa::json)  where id = :id"
        val parameters = MapSqlParameterSource()
            .addValue("id", uuid)
            .addValue("status", status)
            .addValue("aarsak", aarsak)
            .addValue("resultatFraBa", resultat)

        jdbcTemplate.update(updateQuery, parameters)
        return findByID(uuid)
    }

    fun findByID(uuid: UUID): Migrertsak {
        val sql = """SELECT * from migrertesaker where id = :id"""
        val parameters = MapSqlParameterSource().addValue("id", uuid)

        return jdbcTemplate.queryForObject(sql,
                                           parameters,
                                           MigrertsakRowMapper())!!
    }


    class MigrertsakRowMapper : RowMapper<Migrertsak?> {

        @Throws(SQLException::class)
        override fun mapRow(rs: ResultSet, rowNum: Int): Migrertsak {
            val migrertsak = Migrertsak(id = UUID.fromString(rs.getString("id")),
                                      sakNummer = rs.getString("sak_nummer"),
                                      status = MigreringStatus.valueOf(rs.getString("status")),
                                      aarsak = rs.getString("aarsak"),
                                      resultatFraBa = rs.getString("resultat_fra_ba"),
                                      migreringsdato = rs.getTimestamp("migreringsdato").toLocalDateTime(),
                                      personIdent = rs.getString("person_ident"))

            return migrertsak
        }
    }
}