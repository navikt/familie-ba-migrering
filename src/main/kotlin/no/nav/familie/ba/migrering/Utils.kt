package no.nav.familie.ba.migrering

import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

fun erIkkeKjøredato(dato: LocalDate = LocalDate.now()): Boolean {
    if (dato.year != 2022) error("Mangler kjøredato for ${dato.year}")
    val kjøredato = when (dato.month) {
        Month.JANUARY -> 18
        Month.FEBRUARY -> 15
        Month.MARCH -> 18
        Month.APRIL -> 19
        Month.MAY -> 16
        Month.JUNE -> 17
        Month.JULY -> 18
        Month.AUGUST -> 18
        Month.SEPTEMBER -> 19
        Month.OCTOBER -> 18
        Month.NOVEMBER -> 17
        Month.DECEMBER -> 5
    }.run { YearMonth.from(dato).atDay(this) }

    return (dato.isBefore(kjøredato) || dato.isAfter(kjøredato.plusDays(1)))
}

fun skalKjøreMigering(migreringAktivert: Boolean, dato: LocalDate): Boolean {
    return migreringAktivert && erIkkeKjøredato(dato)
}
