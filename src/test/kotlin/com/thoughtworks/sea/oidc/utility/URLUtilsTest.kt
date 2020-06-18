package com.thoughtworks.sea.oidc.utility

import com.thoughtworks.sea.oidc.model.dto.CPLogoutParams
import com.thoughtworks.sea.oidc.model.dto.InitAuthRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class URLUtilsTest {

    @Test
    internal fun `should generate SingPass Login URL when calling generateLoginAddress method`() {

        val initAuthRequest = InitAuthRequest(
            "localhost",
            "/auth2.0",
            "SDK-TEST-001",
            "https://google.com",
            nonce = "UUID-NONCE",
            state = "UUID-STATE"
        )

        val actualURL =
            URLUtils.generateInitAuthURL(initAuthRequest)
        val expectedURL =
            "https://localhost/auth2.0?scope=openid&response_type=code&client_id=SDK-TEST-001&nonce=UUID-NONCE&state=UUID-STATE&redirect_uri=https://google.com"

        assertEquals(actualURL, expectedURL)
    }

    @Test
    internal fun `should generate CorpPass Logout URL`() {
        val logoutParams = CPLogoutParams("test.com", "https://google.com")
        val expectedURL = "https://test.com/cplogout?return_uri=https://google.com"

        val actualURL = URLUtils.generateCPLogoutURL(logoutParams)

        assertEquals(actualURL, expectedURL)
    }
}
