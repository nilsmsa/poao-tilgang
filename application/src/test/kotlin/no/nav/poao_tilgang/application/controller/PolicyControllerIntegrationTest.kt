package no.nav.poao_tilgang.application.controller

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.poao_tilgang.application.client.axsys.EnhetTilgang
import no.nav.poao_tilgang.application.test_util.IntegrationTest
import no.nav.poao_tilgang.application.utils.RestUtils.toJsonRequestBody
import no.nav.poao_tilgang.core.domain.AdGruppe
import no.nav.poao_tilgang.core.domain.TilgangType
import no.nav.poao_tilgang.core.provider.AdGruppeProvider
import okhttp3.Response
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

class PolicyControllerIntegrationTest : IntegrationTest() {

	val navIdent = "Z1235"
	val norskIdent = "6456532"
	val navAnsattId = UUID.randomUUID()
	val brukersEnhet = "1234"
	val brukersKommune = "5000"

	val noAccessGroup = AdGruppe(UUID.randomUUID(), "0000-some-group")

	@Autowired
	private lateinit var adGruppeProvider: AdGruppeProvider

	@Test
	fun `should evaluate NAV_ANSATT_NAV_IDENT_SKRIVETILGANG_TIL_EKSTERN_BRUKER_V1 policy - permit`() {
		setupMocks()

		val requestId = UUID.randomUUID()

		mockAbacHttpServer.mockPermit(TilgangType.SKRIVE)

		val response = sendPolicyRequest(
			requestId,
			"""{"navIdent": "$navIdent", "norskIdent": "$norskIdent"}""",
			"NAV_ANSATT_NAV_IDENT_SKRIVETILGANG_TIL_EKSTERN_BRUKER_V1"
		)

		response.body?.string() shouldBe permitResponse(requestId)
	}

	@Test
	fun `should evaluate NAV_ANSATT_NAV_IDENT_SKRIVETILGANG_TIL_EKSTERN_BRUKER_V1 policy - deny`() {
		setupMocks(adGrupper = listOf(adGruppeProvider.hentTilgjengeligeAdGrupper().modiaGenerell))

		mockAbacHttpServer.mockDeny(TilgangType.SKRIVE)

		val requestId = UUID.randomUUID()

		val response = sendPolicyRequest(
			requestId,
			"""{"navIdent": "$navIdent", "norskIdent": "$norskIdent"}""",
			"NAV_ANSATT_NAV_IDENT_SKRIVETILGANG_TIL_EKSTERN_BRUKER_V1"
		)

		response.body?.string() shouldBe denyResponse(requestId, "NAV-ansatt mangler tilgang til AD-gruppen \\\"0000-GA-Modia-Oppfolging\\\"", "MANGLER_TILGANG_TIL_AD_GRUPPE")
	}

	@ParameterizedTest
	@EnumSource(TilgangType::class)
	fun `should evaluate NAV_ANSATT_TILGANG_TIL_EKSTERN_BRUKER_V2 policy - permit`(tilgangType: TilgangType) {
		setupMocks()

		val requestId = UUID.randomUUID()

		mockAbacHttpServer.mockPermit(tilgangType)

		val response = sendPolicyRequest(
			requestId,
			"""{"navAnsattAzureId": "$navAnsattId", "tilgangType": "${tilgangType.name}", "norskIdent": "$norskIdent"}""",
			"NAV_ANSATT_TILGANG_TIL_EKSTERN_BRUKER_V2"
		)

		response.body?.string() shouldBe permitResponse(requestId)
	}

	@ParameterizedTest
	@EnumSource(TilgangType::class)
	fun `should evaluate NAV_ANSATT_TILGANG_TIL_EKSTERN_BRUKER_V2 policy - deny`(tilgangType: TilgangType) {
		setupMocks(adGrupper = emptyList())
		mockAbacHttpServer.mockDeny(tilgangType)

		val requestId = UUID.randomUUID()

		val response = sendPolicyRequest(
			requestId,
			"""{"navAnsattAzureId": "$navAnsattId", "tilgangType": "${tilgangType.name}", "norskIdent": "$norskIdent"}""",
			"NAV_ANSATT_TILGANG_TIL_EKSTERN_BRUKER_V2"
		)

		response.body?.string() shouldContain "MANGLER_TILGANG_TIL_AD_GRUPPE"
	}

	@Test
	fun `should evaluate NAV_ANSATT_TILGANG_TIL_MODIA_V1 policy - permit`() {
		val requestId = UUID.randomUUID()
		mockAdGrupperResponse(navIdent, navAnsattId, listOf(adGruppeProvider.hentTilgjengeligeAdGrupper().modiaGenerell))

		val response = sendPolicyRequest(
			requestId,
			"""{"navAnsattAzureId":"$navAnsattId"}""",
			"NAV_ANSATT_TILGANG_TIL_MODIA_V1"
		)

		response.body?.string() shouldBe permitResponse(requestId)
	}

	@Test
	fun `should evaluate NAV_ANSATT_TILGANG_TIL_MODIA_V1 policy - deny`() {
		val requestId = UUID.randomUUID()

		mockAdGrupperResponse(navIdent, navAnsattId, listOf(noAccessGroup))

		val response = sendPolicyRequest(
			requestId,
			"""{"navAnsattAzureId":"$navAnsattId"}""",
			"NAV_ANSATT_TILGANG_TIL_MODIA_V1"
		)

		response.body?.string() shouldBe denyResponse(
			requestId,
			"NAV-ansatt mangler tilgang til en av AD-gruppene [0000-GA-BD06_ModiaGenerellTilgang, 0000-GA-Modia-Oppfolging, 0000-GA-SYFO-SENSITIV]",
			"MANGLER_TILGANG_TIL_AD_GRUPPE"
		)
	}

	@Test
	fun `should evaluate EKSTERN_BRUKER_TILGANG_TIL_EKSTERN_BRUKER_V1 policy - permit`() {

		val requestId = UUID.randomUUID()

		val response = sendPolicyRequest(
			requestId,
			"""{"rekvirentNorskIdent": "$norskIdent", "ressursNorskIdent": "$norskIdent"}""",
			"EKSTERN_BRUKER_TILGANG_TIL_EKSTERN_BRUKER_V1"
		)

		response.body?.string() shouldBe permitResponse(requestId)
	}

	@Test
	fun `should evaluate EKSTERN_BRUKER_TILGANG_TIL_EKSTERN_BRUKER_V1 policy - deny`() {

		val requestId = UUID.randomUUID()

		val response = sendPolicyRequest(
			requestId,
			"""{"rekvirentNorskIdent": "$norskIdent", "ressursNorskIdent": "453634"}""",
			"EKSTERN_BRUKER_TILGANG_TIL_EKSTERN_BRUKER_V1"
		)

		response.body?.string() shouldBe denyResponse(
			requestId, "Rekvirent har ikke samme ident som ressurs", "EKSTERN_BRUKER_HAR_IKKE_TILGANG"
		)
	}

	@Test
	fun `should evaluate NAV_ANSATT_TILGANG_TIL_NAV_ENHET_V1 policy - permit`() {
		val requestId = UUID.randomUUID()
		mockAdGrupperResponse(navIdent, navAnsattId, listOf(adGruppeProvider.hentTilgjengeligeAdGrupper().modiaOppfolging))
		mockAxsysHttpServer.mockHentTilgangerResponse(navIdent, listOf(EnhetTilgang(brukersEnhet, "BrukersEnhet", emptyList())))


		mockAbacHttpServer.mockPermitAll()

		val response = sendPolicyRequest(
			requestId,
			"""{"navAnsattAzureId": "$navAnsattId", "navEnhetId": "${brukersEnhet}"}""",
			"NAV_ANSATT_TILGANG_TIL_NAV_ENHET_V1"
		)

		response.body?.string() shouldBe permitResponse(requestId)
	}

	@Test
	fun `should evaluate NAV_ANSATT_TILGANG_TIL_NAV_ENHET_V1 policy - deny`() {
		val requestId = UUID.randomUUID()

		mockAdGrupperResponse(navIdent, navAnsattId, listOf(adGruppeProvider.hentTilgjengeligeAdGrupper().modiaOppfolging))
		mockAxsysHttpServer.mockHentTilgangerResponse(navIdent, listOf(EnhetTilgang("9999", "AnnenEnhet", emptyList())))

		mockAbacHttpServer.mockDenyAll()

		val response = sendPolicyRequest(
			requestId,
			"""{"navAnsattAzureId": "$navAnsattId", "navEnhetId": "0123"}""",
			"NAV_ANSATT_TILGANG_TIL_NAV_ENHET_V1"
		)

		response.body?.string() shouldBe denyResponse(
			requestId,
			"Har ikke tilgang til NAV enhet",
			"IKKE_TILGANG_TIL_NAV_ENHET"
		)
	}

	@Test
	fun `should evaluate NAV_ANSATT_BEHANDLE_STRENGT_FORTROLIG_BRUKERE policy - permit`() {
		val requestId = UUID.randomUUID()

		mockMicrosoftGraphHttpServer.mockHentNavIdentMedAzureIdResponse(navAnsattId, navIdent)
		mockAdGrupperResponse(navIdent, navAnsattId, listOf(adGruppeProvider.hentTilgjengeligeAdGrupper().strengtFortroligAdresse))

		val response = sendPolicyRequest(
			requestId,
			"""{"navAnsattAzureId": "$navAnsattId"}""",
			"NAV_ANSATT_BEHANDLE_STRENGT_FORTROLIG_BRUKERE_V1"
		)

		response.body?.string() shouldBe permitResponse(requestId)
	}

	@Test
	fun `should evaluate NAV_ANSATT_BEHANDLE_STRENGT_FORTROLIG_BRUKERE policy - deny`() {
		val requestId = UUID.randomUUID()

		mockAdGrupperResponse(navIdent, navAnsattId, listOf(adGruppeProvider.hentTilgjengeligeAdGrupper().modiaOppfolging))

		val response = sendPolicyRequest(
			requestId,
			"""{"navAnsattAzureId": "$navAnsattId"}""",
			"NAV_ANSATT_BEHANDLE_STRENGT_FORTROLIG_BRUKERE_V1"
		)

		response.body?.string() shouldBe denyResponse(
			requestId,
			"NAV-ansatt mangler tilgang til AD-gruppen \\\"0000-GA-Strengt_Fortrolig_Adresse\\\"",
			"MANGLER_TILGANG_TIL_AD_GRUPPE"
		)
	}

	@Test
	fun `should evaluate NAV_ANSATT_BEHANDLE_FORTROLIG_BRUKERE policy - permit`() {
		val requestId = UUID.randomUUID()

		mockAdGrupperResponse(navIdent, navAnsattId, listOf(adGruppeProvider.hentTilgjengeligeAdGrupper().fortroligAdresse))

		val response = sendPolicyRequest(
			requestId,
			"""{"navAnsattAzureId": "$navAnsattId"}""",
			"NAV_ANSATT_BEHANDLE_FORTROLIG_BRUKERE_V1"
		)

		response.body?.string() shouldBe permitResponse(requestId)
	}

	@Test
	fun `should evaluate NAV_ANSATT_BEHANDLE_FORTROLIG_BRUKERE policy - deny`() {
		val requestId = UUID.randomUUID()

		mockAdGrupperResponse(navIdent, navAnsattId, listOf(adGruppeProvider.hentTilgjengeligeAdGrupper().modiaOppfolging))

		val response = sendPolicyRequest(
			requestId,
			"""{"navAnsattAzureId": "$navAnsattId"}""",
			"NAV_ANSATT_BEHANDLE_FORTROLIG_BRUKERE_V1"
		)

		response.body?.string() shouldBe denyResponse(
			requestId,
			"NAV-ansatt mangler tilgang til AD-gruppen \\\"0000-GA-Fortrolig_Adresse\\\"",
			"MANGLER_TILGANG_TIL_AD_GRUPPE"
		)
	}

	@Test
	fun `should evaluate NAV_ANSATT_BEHANDLE_SKJERMEDE_PERSONER policy - permit`() {
		val requestId = UUID.randomUUID()

		mockAdGrupperResponse(navIdent, navAnsattId, listOf(adGruppeProvider.hentTilgjengeligeAdGrupper().egneAnsatte))

		val response = sendPolicyRequest(
			requestId,
			"""{"navAnsattAzureId": "$navAnsattId"}""",
			"NAV_ANSATT_BEHANDLE_SKJERMEDE_PERSONER_V1"
		)

		response.body?.string() shouldBe permitResponse(requestId)
	}

	@Test
	fun `should evaluate NAV_ANSATT_BEHANDLE_SKJERMEDE_PERSONER policy - deny`() {
		val requestId = UUID.randomUUID()

		mockAdGrupperResponse(navIdent, navAnsattId, listOf(adGruppeProvider.hentTilgjengeligeAdGrupper().modiaOppfolging))

		val response = sendPolicyRequest(
			requestId,
			"""{"navAnsattAzureId": "$navAnsattId"}""",
			"NAV_ANSATT_BEHANDLE_SKJERMEDE_PERSONER_V1"
		)

		response.body?.string() shouldBe denyResponse(
			requestId,
			"NAV-ansatt mangler tilgang til en av AD-gruppene [0000-GA-Egne_ansatte]",
			"MANGLER_TILGANG_TIL_AD_GRUPPE"
		)
	}

	@Test
	fun `should evaluate NAV_ANSATT_TILGANG_TIL_NAV_ENHET_MED_SPERRE_V1 policy - permit`() {
		val requestId = UUID.randomUUID()

		setupMocks()

		mockAbacHttpServer.mockPermitAll()

		val response = sendPolicyRequest(
			requestId,
			"""{"navAnsattAzureId": "$navAnsattId", "navEnhetId": "$brukersEnhet"}""",
			"NAV_ANSATT_TILGANG_TIL_NAV_ENHET_MED_SPERRE_V1"
		)

		response.body?.string() shouldBe permitResponse(requestId)
	}

	@Test
	fun `should evaluate NAV_ANSATT_TILGANG_TIL_NAV_ENHET_MED_SPERRE_V1 policy - deny`() {
		val requestId = UUID.randomUUID()

		setupMocks()

		mockAbacHttpServer.mockDenyAll()

		val response = sendPolicyRequest(
			requestId,
			"""{"navAnsattAzureId": "$navAnsattId", "navEnhetId": "9999"}""",
			"NAV_ANSATT_TILGANG_TIL_NAV_ENHET_MED_SPERRE_V1"
		)

		response.body?.string() shouldBe denyResponse(
			requestId,
			"Har ikke tilgang til NAV enhet med sperre",
			"IKKE_TILGANG_TIL_NAV_ENHET"
		)
	}

	private fun permitResponse(requestId: UUID): String {
		return """
			{"results":[{"requestId":"$requestId","decision":{"type":"PERMIT","message":null,"reason":null}}]}
		""".trimIndent()
	}

	private fun denyResponse(
		requestId: UUID,
		message: String,
		reason: String
	): String {
		return """
			{"results":[{"requestId":"$requestId","decision":{"type":"DENY","message":"$message","reason":"$reason"}}]}
		""".trimIndent()
	}

	private fun sendPolicyRequest(
		requestId: UUID,
		policyInputJsonObj: String,
		policyId: String
	): Response {
		return sendRequest(
			path = "/api/v1/policy/evaluate",
			method = "POST",
			headers = mapOf("Authorization" to "Bearer ${mockOAuthServer.issueAzureAdM2MToken()}"),
			body = """
				{"requests": [
					{
						"requestId": "$requestId",
						"policyInput": $policyInputJsonObj,
						"policyId": "$policyId"
					}
				]}
			""".trimIndent().toJsonRequestBody()
		)
	}

	private fun mockAdGrupperResponse(navIdent: String, navAnsattId: UUID, adGrupper: List<AdGruppe>) {
		mockMicrosoftGraphHttpServer.mockHentAzureIdMedNavIdentResponse(navIdent, navAnsattId)

		mockMicrosoftGraphHttpServer.mockHentNavIdentMedAzureIdResponse(navAnsattId, navIdent)

		mockMicrosoftGraphHttpServer.mockHentAdGrupperForNavAnsatt(navAnsattId, adGrupper.map { it.id })

		mockMicrosoftGraphHttpServer.mockHentAdGrupperResponse(adGrupper)
	}

	private fun setupMocks(adGrupper: List<AdGruppe> = listOf(adGruppeProvider.hentTilgjengeligeAdGrupper().modiaOppfolging)) {
		mockPdlPipHttpServer.mockBrukerInfo(
			norskIdent = norskIdent,
			gtKommune = brukersKommune
		)

		mockSkjermetPersonHttpServer.mockErSkjermet(
			mapOf(
				norskIdent to false
			)
		)
		mockNorgHttpServer.mockTilhorendeEnhet(brukersKommune, brukersEnhet)
		mockVeilarbarenaHttpServer.mockOppfolgingsenhet(brukersEnhet)
		mockAxsysHttpServer.mockHentTilgangerResponse(navIdent, listOf(EnhetTilgang(brukersEnhet, "Brukersenhet", emptyList())))
		mockAdGrupperResponse(navIdent, navAnsattId, adGrupper)
	}



}
