package test

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.After
import org.junit.Before
import org.junit.Test
import unibo.basicomm23.interfaces.Interaction
import unibo.basicomm23.tcp.TcpClientSupport
import unibo.basicomm23.utils.CommUtils

/*
 * TestPlan automatico per il prototipo dello Sprint 0.
 * Obiettivo: Verificare che il cargoservice (porta 8050) gestisca correttamente
 * la richiesta di carico (load_request) rispettando il protocollo Request/Reply.
 */
class TestPlanSprint0 {
    private var conn: Interaction? = null

    @Before
    fun setup() {
        CommUtils.outcyan("=== [TestPlanSprint0] Connessione al cargoservice (porta 8050) ===")
        try {
            conn = TcpClientSupport.connect("127.0.0.1", 8050, 10)
        } catch (e: Exception) {
            fail("Errore di connessione TCP: assicurati che il server cargoservice sia avviato.")
        }
    }

    @After
    fun teardown() {
        conn?.close()
        CommUtils.outcyan("=== [TestPlanSprint0] Connessione chiusa ===")
    }

    @Test
    fun testLoadRequest() {
        CommUtils.outmagenta("--- Test: Invio load_request e verifica risposta QAK ---")
        try {
            // 1. Costruzione messaggio QAK (Prolog): msg(MSGID, MSGTYPE, SENDER, RECEIVER, CONTENT, SEQNUM)
            val requestMsg = "msg(load_request, request, testunit, cargoservice, loadRequest(none), 1)"

            // 2. Invio sincrono su TCP e attesa della reply dal server
            val reply = conn?.request(requestMsg)
            CommUtils.outgreen("Risposta ricevuta dal server: $reply")

            // 3. Verifica: la risposta deve essere una delle 3 reply previste dal modello
            val isValidReply = reply != null && (
                reply.contains("load_accepted") ||
                reply.contains("load_retrylater") ||
                reply.contains("load_refused")
            )
            assertTrue("Il server non ha restituito una risposta di carico valida!", isValidReply)

        } catch (e: Exception) {
            fail("Test fallito durante l'esecuzione: ${e.message}")
        }
    }
}
