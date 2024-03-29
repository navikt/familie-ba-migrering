package no.nav.familie.ba.migrering.config

import no.nav.familie.ba.migrering.domain.JsonWrapper
import no.nav.familie.prosessering.PropertiesWrapperTilStringConverter
import no.nav.familie.prosessering.StringTilPropertiesWrapperConverter
import org.postgresql.util.PGobject
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories

@Configuration
@EnableJdbcAuditing
@EnableJdbcRepositories("no.nav.familie.prosessering", "no.nav.familie.ba.migrering.domain")
class DatabaseConfig : AbstractJdbcConfiguration() {
    @Bean
    override fun jdbcCustomConversions(): JdbcCustomConversions {
        return JdbcCustomConversions(
            listOf(
                PropertiesWrapperTilStringConverter(),
                StringTilPropertiesWrapperConverter(),
                PGObjectTilJsonWrapperConverter(),
                JsonWrapperTilPGObjectConverter(),
            ),
        )
    }
}

@ReadingConverter
class PGObjectTilJsonWrapperConverter : Converter<PGobject, JsonWrapper?> {
    override fun convert(source: PGobject): JsonWrapper? =
        if (source.value == null || source.type.lowercase() != "json") null else JsonWrapper(source.value)
}

@WritingConverter
class JsonWrapperTilPGObjectConverter : Converter<JsonWrapper, PGobject?> {
    override fun convert(source: JsonWrapper): PGobject? = if (source.jsonStr == null) {
        null
    } else {
        PGobject().apply {
            this.value = source.jsonStr
            this.type = "Json"
        }
    }
}
