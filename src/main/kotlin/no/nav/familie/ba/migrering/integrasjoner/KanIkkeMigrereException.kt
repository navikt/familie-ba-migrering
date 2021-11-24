package no.nav.familie.ba.migrering.integrasjoner

class KanIkkeMigrereException(val feiltype: String, val melding: String, val throwable: Throwable?): RuntimeException(melding, throwable) {

}