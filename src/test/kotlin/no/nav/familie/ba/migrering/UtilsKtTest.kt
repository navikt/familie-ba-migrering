package no.nav.familie.ba.migrering

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate

internal class UtilsKtTest {

    @ParameterizedTest
    @CsvSource(
        "2022-01-01, true",
        "2022-01-17, true",
        "2022-01-18, false",
        "2022-01-19, false",
        "2022-01-20, false",
        "2022-05-16, false",
    )
    fun `skal returnere false hvis dato ikke er kjøredato og true hvis det er kjøredato`(input: LocalDate, expected: Boolean) {
        assertEquals(expected, erIkkeKjøredato(input))
    }

    @Test
    fun `skal kaste exception hvis dato er i et år man ikke har kjøredator`() {
        assertThrows(IllegalStateException::class.java,  { erIkkeKjøredato(LocalDate.of(2023,1,1))}, "Mangler kjøredato for 2022")
    }

    @ParameterizedTest
    @CsvSource(
        "true, 2022-01-17, true",
        "true, 2022-01-18, false",
        "true, 2022-01-19, false",
        "true, 2022-01-20, false",
        "true, 2022-01-21, true",
        "false, 2022-01-17, false",
        "false, 2022-01-18, false",
        "false, 2022-01-19, false",
    )
    fun `sjekk om man skal kjøre migreing`(migreringAktivert: Boolean, dato: LocalDate, expected: Boolean) {
        assertEquals(expected, skalKjøreMigering(migreringAktivert, dato))
    }

}