package it.unibo.ctxioport
import it.unibo.kactor.QakContext
import it.unibo.kactor.sysUtil
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
	QakContext.createContexts(
	        "localhost", this, "cargosystem.pl", "sysRules.pl", "ctxioport"
	)
}
