package no.nav.familie.ba.migrering.database

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.data.relational.core.mapping.RelationalMappingContext
import org.springframework.data.relational.core.sql.IdentifierProcessing
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.sql.DataSource

@Service
@Profile("dev")
class DatabaseCleanUpService(
    private val relationalMappingContext: RelationalMappingContext,
    private val dataSource: DataSource,
) {

    private val logger = LoggerFactory.getLogger(DatabaseCleanUpService::class.java)

    private fun getJdbcTableNames(): List<String> = relationalMappingContext.persistentEntities.filter { it.hasIdProperty() }.map { entity ->
        entity.tableName.toSql(IdentifierProcessing.NONE)
    }

    /**
     * Utility method that truncates all identified tables
     */
    @Transactional
    fun truncate() {
        logger.info("Truncating tables: ${getJdbcTableNames()}")
        val statement = dataSource.connection.createStatement()
        getJdbcTableNames().forEach { tableName ->
            statement.execute("TRUNCATE TABLE $tableName CASCADE")
        }
    }
}
