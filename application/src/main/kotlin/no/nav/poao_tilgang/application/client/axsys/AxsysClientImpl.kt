package no.nav.poao_tilgang.application.client.axsys

import no.nav.common.rest.client.RestClient.baseClient
import no.nav.poao_tilgang.application.utils.JsonUtils.fromJsonString
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType

internal class AxsysClientImpl(
	private val baseUrl: String,
	private val proxyTokenProvider: () -> String,
	private val axsysTokenProvider: () -> String,
	private val httpClient: OkHttpClient = baseClient(),
) : AxsysClient {

	override fun hentTilganger(navIdent: String): List<EnhetTilgang> {
		val request = Request.Builder()
			.url("$baseUrl/api/v2/tilgang/$navIdent?inkluderAlleEnheter=false")
			.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + proxyTokenProvider.invoke())
			.header("Downstream-Authorization", "Bearer " + axsysTokenProvider.invoke())
			.build()

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente veileders tilganger fra axsys. Status: ${response.code}")
			}

			val body = response.body?.string() ?: throw RuntimeException("Body is missing")

			val tilgangResponse = fromJsonString<TilgangResponse>(body)

			return tilgangResponse.enheter.map {
				return@map EnhetTilgang(
					enhetId = it.enhetId,
					enhetNavn = it.navn,
					temaer = it.temaer
				)
			}
		}
	}

	private data class TilgangResponse(
		val enheter: List<TilgangResponseEnhet>
	)

	private data class TilgangResponseEnhet(
		val enhetId: String,
		val temaer: List<String>,
		val navn: String
	)

}