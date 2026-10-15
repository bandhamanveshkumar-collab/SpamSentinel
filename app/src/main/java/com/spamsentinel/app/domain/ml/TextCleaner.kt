package com.spamsentinel.app.domain.ml

import java.util.Locale

object TextCleaner {
    private val urlRegex = Regex("""https?://\S+|www\.\S+""", RegexOption.IGNORE_CASE)
    private val longNumRegex = Regex("""\d{5,}""")
    private val specialCharRegex = Regex("""[^a-z0-9\s£$€_]""")
    private val whitespaceRegex = Regex("""\s+""")

    fun clean(text: String): String {
        var t = text.lowercase(Locale.ROOT)
        t = urlRegex.replace(t, " urltoken ")
        t = longNumRegex.replace(t, " longnum ")
        t = specialCharRegex.replace(t, " ")
        return whitespaceRegex.replace(t, " ").trim()
    }
}
