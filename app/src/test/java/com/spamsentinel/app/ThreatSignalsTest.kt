package com.spamsentinel.app

import com.spamsentinel.app.domain.signals.OtpGuard
import com.spamsentinel.app.domain.signals.ScamTypeTagger
import com.spamsentinel.app.domain.signals.SenderAnalyzer
import com.spamsentinel.app.domain.signals.SenderType
import com.spamsentinel.app.domain.signals.UrlAnalyzer
import com.spamsentinel.app.domain.model.ScamType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreatSignalsTest {

    @Test
    fun testSenderAnalysis() {
        val header = SenderAnalyzer.analyze("AX-HDFCBK", "Your account alert")
        assertEquals(SenderType.ALPHANUMERIC, header.senderType)

        val promoNumber = SenderAnalyzer.analyze("+919876543210", "Flat 50% discount buy now click link")
        assertEquals(SenderType.MOBILE_10, promoNumber.senderType)
        assertTrue(promoNumber.isPersonalNumberPromo)
    }

    @Test
    fun testUrlAnalysis() {
        val riskyUrl = UrlAnalyzer.analyze("Update your KYC at http://hdfc-bank-kyc.xyz now")
        assertTrue(riskyUrl.hasUrl)
        assertTrue(riskyUrl.hasRiskyUrl)
        assertEquals("hdfc-bank-kyc.xyz", riskyUrl.host)
    }

    @Test
    fun testScamTypeTagger() {
        val kyc = ScamTypeTagger.tag("Please update your PAN card and bank account KYC to avoid suspension")
        assertEquals(ScamType.PHISHING_KYC, kyc)

        val electricity = ScamTypeTagger.tag("Electricity power bill unpaid. Disconnection tonight 9:30 PM.")
        assertEquals(ScamType.ELECTRICITY_BILL, electricity)
    }
}
