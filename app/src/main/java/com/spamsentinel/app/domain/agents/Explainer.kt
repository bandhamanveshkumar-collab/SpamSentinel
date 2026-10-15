package com.spamsentinel.app.domain.agents

import com.spamsentinel.app.domain.ml.LinearSpamModel
import com.spamsentinel.app.domain.model.TermWeight

object Explainer {

    fun explain(text: String, model: LinearSpamModel): Pair<List<TermWeight>, List<TermWeight>> {
        val contributions = model.getTermContributions(text)
        if (contributions.isEmpty()) {
            return Pair(emptyList(), emptyList())
        }

        val spamTerms = contributions
            .filter { it.second > 0f }
            .sortedByDescending { it.second }
            .take(5)
            .map { TermWeight(it.first, it.second) }

        val hamTerms = contributions
            .filter { it.second < 0f }
            .sortedBy { it.second }
            .take(5)
            .map { TermWeight(it.first, it.second) }

        return Pair(spamTerms, hamTerms)
    }
}
