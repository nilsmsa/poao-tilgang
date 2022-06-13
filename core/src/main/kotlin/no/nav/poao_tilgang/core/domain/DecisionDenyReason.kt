package no.nav.poao_tilgang.core.domain

enum class DecisionDenyReason(val reason: String) {
	MANGLER_TILGANG_TIL_AD_GRUPPE("MANGLER_TILGANG_TIL_AD_GRUPPE"),
	IKKE_MEDLEM_AV_AD_GRUPPE_SKJERMET_PERSON("IKKE_MEDLEM_AV_AD_GRUPPE_SKJERMET_PERSON"),
	IKKE_TILGANG_TIL_OPPFOLGINGSENHET("IKKE_TILGANG_TIL_OPPFOLGINGSENHET"),
	IKKE_TILGANG_TIL_MODIA("IKKE_TILGANG_TIL_MODIA")
}