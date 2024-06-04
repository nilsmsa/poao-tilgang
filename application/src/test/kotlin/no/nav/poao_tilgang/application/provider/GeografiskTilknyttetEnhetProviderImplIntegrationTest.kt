package no.nav.poao_tilgang.application.provider

import io.kotest.matchers.shouldBe
import no.nav.poao_tilgang.application.client.pdl_pip.GeografiskTilknytningType
import no.nav.poao_tilgang.application.test_util.IntegrationTest
import no.nav.poao_tilgang.core.provider.GeografiskTilknyttetEnhetProvider
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class GeografiskTilknyttetEnhetProviderImplIntegrationTest : IntegrationTest() {

	@Autowired
	lateinit var geografiskTilknyttetEnhetProvider: GeografiskTilknyttetEnhetProvider

	@Test
	fun `henter tilhørende enhet basert på geografisk tilknyting kommune`() {
		mockPdlPipHttpServer.mockBrukerInfo(
			norskIdent = "987", gtType = GeografiskTilknytningType.KOMMUNE, gtKommune = "0570"
		)
		mockNorgHttpServer.mockTilhorendeEnhet(geografiskTilknytning = "0570", tilhorendeEnhet = "1234")

		geografiskTilknyttetEnhetProvider.hentGeografiskTilknyttetEnhet("987") shouldBe "1234"
	}

	@Test
	fun `henter tilhørende enhet basert på geografisk tilknyting bydel`() {
		mockPdlPipHttpServer.mockBrukerInfo(
			norskIdent = "988", gtType = GeografiskTilknytningType.BYDEL, gtBydel = "057021"
		)
		mockNorgHttpServer.mockTilhorendeEnhet(geografiskTilknytning = "057021", tilhorendeEnhet = "1235")

		geografiskTilknyttetEnhetProvider.hentGeografiskTilknyttetEnhet("988") shouldBe "1235"
	}

	@Test
	fun `kan ikke hente enhet basert på geografisk tilknyting utland`() {
		mockPdlPipHttpServer.mockBrukerInfo(
			norskIdent = "989", gtType = GeografiskTilknytningType.UTLAND
		)

		geografiskTilknyttetEnhetProvider.hentGeografiskTilknyttetEnhet("989") shouldBe null
	}

	@Test
	fun `skal returnere null dersom tilhørende enhet for geografisk tilknyting ikke finnes`() {
		mockPdlPipHttpServer.mockBrukerInfo(
			norskIdent = "990", gtType = GeografiskTilknytningType.KOMMUNE, gtKommune = "9999"
		)

		mockNorgHttpServer.mockIngenTilhorendeEnhet("9999")

		geografiskTilknyttetEnhetProvider.hentGeografiskTilknyttetEnhet("990") shouldBe null
	}
}
