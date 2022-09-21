package no.nav.poao_tilgang.application.test_util

import no.nav.poao_tilgang.application.Application
import no.nav.poao_tilgang.application.test_util.mock_clients.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.Duration

@ActiveProfiles("test")
@Import(TestConfig::class)
@ExtendWith(SpringExtension::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class])
open class IntegrationTest {

	@LocalServerPort
	private var port: Int = 0

	private val client = OkHttpClient.Builder()
		.readTimeout(Duration.ofMinutes(15)) // For setting debug breakpoints without the connection being killed
		.build()

	companion object {
		val mockOAuthServer = MockOAuthServer()
		val mockMicrosoftGraphHttpServer = MockMicrosoftGraphHttpServer()
		val mockSkjermetPersonHttpServer = MockSkjermetPersonHttpServer()
		val mockAxsysHttpServer = MockAxsysHttpServer()
		val mockAbacHttpServer = MockAbacHttpServer()
		val mockVeilarbarenaHttpServer = MockVeilarbarenaHttpServer()
		val mockPdlHttpServer = MockPdlHttpServer()

		@JvmStatic
		@DynamicPropertySource
		fun registerProperties(registry: DynamicPropertyRegistry) {
			mockOAuthServer.start()
			mockMicrosoftGraphHttpServer.start()
			mockSkjermetPersonHttpServer.start()
			mockAxsysHttpServer.start()
			mockAbacHttpServer.start()
			mockVeilarbarenaHttpServer.start()
			mockPdlHttpServer.start()

			registry.add("no.nav.security.jwt.issuer.azuread.discovery-url", mockOAuthServer::getDiscoveryUrl)
			registry.add("no.nav.security.jwt.issuer.azuread.accepted-audience") { "test" }

			registry.add("microsoft_graph.url", mockMicrosoftGraphHttpServer::serverUrl)
			registry.add("skjermet_person.url", mockSkjermetPersonHttpServer::serverUrl)
			registry.add("axsys.url", mockAxsysHttpServer::serverUrl)
			registry.add("abac.url", mockAbacHttpServer::serverUrl)
			registry.add("veilarbarena.url", mockVeilarbarenaHttpServer::serverUrl)
			registry.add("pdl.url", mockPdlHttpServer::serverUrl)
		}
	}

	@AfterEach
	fun reset() {
		mockMicrosoftGraphHttpServer.reset()
		mockSkjermetPersonHttpServer.reset()
		mockAxsysHttpServer.reset()
		mockAbacHttpServer.reset()
		mockVeilarbarenaHttpServer.reset()
		mockPdlHttpServer.reset()
	}

	fun serverUrl() = "http://localhost:$port"

	fun sendRequest(
		method: String,
		path: String,
		body: RequestBody? = null,
		headers: Map<String, String> = emptyMap()
	): Response {
		val reqBuilder = Request.Builder()
			.url("${serverUrl()}$path")
			.method(method, body)

		headers.forEach {
			reqBuilder.addHeader(it.key, it.value)
		}

		return client.newCall(reqBuilder.build()).execute()
	}

}
