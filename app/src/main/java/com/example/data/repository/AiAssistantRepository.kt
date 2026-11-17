package com.example.data.repository

import com.example.data.model.ResearchFramework
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface AiAssistantRepository {
    fun streamBusinessAdvice(
        prompt: String,
        businessContext: String
    ): Flow<String>
}

class FirebaseAiAssistantRepository : AiAssistantRepository {
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "You are Project X AI Business Intelligence, an SME operations advisor for Nakuru Town, Kenya, " +
                        "aligned with the research study: '${ResearchFramework.RESEARCH_TITLE}'. " +
                        "Frame advice strictly around the 4 independent variables (Digital Marketing, Digital Payment Systems, " +
                        "Computerized Record Keeping, Online Communication) and SME Performance (sales growth, customer growth and reach, " +
                        "operational efficiency, profitability). Always quote money in Kenyan Shillings (KES). " +
                        "Ground every recommendation strictly in the provided data, clearly distinguish sample demo data from live Firestore data, " +
                        "and explicitly distinguish observed data relationships from causal conclusions—never fabricate research findings."
                )
            }
        )
    }

    override fun streamBusinessAdvice(prompt: String, businessContext: String): Flow<String> {
        val fullPrompt = buildString {
            appendLine("Business & Research Context:")
            appendLine(businessContext)
            appendLine()
            appendLine("SME Owner Question:")
            append(prompt)
        }
        return model.generateContentStream(fullPrompt)
            .map { chunk -> chunk.text.orEmpty() }
    }
}
