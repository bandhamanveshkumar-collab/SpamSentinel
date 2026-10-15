package com.spamsentinel.app

import com.spamsentinel.app.domain.ml.TextCleaner
import org.junit.Assert.assertEquals
import org.junit.Test

class TextCleanerTest {

    @Test
    fun testTextCleanerPortMatchesPythonLogic() {
        val input = "WINNER! Claim £900 at http://test.com/claim or call 08000215888 now!"
        val cleaned = TextCleaner.clean(input)

        // Lowercase, URL replaced with urltoken, 5+ digits replaced with longnum, special chars removed except £$€_
        assertEquals("winner claim £900 at urltoken or call longnum now", cleaned)
    }

    @Test
    fun testCurrencyAndSpecialSymbols() {
        val input = "Win $1000 or €500 or £200 today!!!"
        val cleaned = TextCleaner.clean(input)
        assertEquals("win $1000 or €500 or £200 today", cleaned)
    }
}
