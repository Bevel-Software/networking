package software.bevel.networking

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.bevel.networking.web_client.JavaNetReactorMonoWebClient

class ProgressUpdateTest {
    val lgr = LoggerFactory.getLogger("CustomLogger")
    val logger = object : Logger by lgr {
        override fun error(msg: String) {
            lgr.error(msg)
            throw RuntimeException(msg)
        }

        override fun error(msg: String?, t: Throwable?) {
            lgr.error(msg, t)
            throw RuntimeException(msg, t)
        }
    }
    private val webClient = JavaNetReactorMonoWebClient(logger)
    private val commsChannel = RestCommunicationInterface(webClient, "0", logger)

    @Test
    fun `test sequential progress update messages`() {
        val totalFiles = 100
        
        for (index in 0 until totalFiles) {
            val message = jacksonObjectMapper().writeValueAsString(
                mapOf(
                    "command" to "progressUpdate",
                    "args" to listOf((index + 1))
                )
            )
            commsChannel.sendWithoutResponse(message)
            Thread.sleep(100) // Rate limit to avoid overwhelming
        }
    }
}
