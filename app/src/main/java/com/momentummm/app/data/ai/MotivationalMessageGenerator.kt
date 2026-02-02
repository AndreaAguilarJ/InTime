package com.momentummm.app.data.ai

import android.content.Context
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.momentummm.app.BuildConfig
import com.momentummm.app.data.entity.AIPersonality
import com.momentummm.app.data.entity.MessageCategory
import com.momentummm.app.data.entity.MessageTone
import com.momentummm.app.data.entity.MotivationalMessage
import com.momentummm.app.data.repository.MotivationalMessagesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI-powered motivational message generator using Google's Gemini API.
 * 
 * Features:
 * - Generates personalized motivational messages
 * - Supports different AI personalities
 * - Contextual message generation based on user goals and progress
 * - Rate limiting to prevent API abuse
 * - Caching of generated messages
 */
@Singleton
class MotivationalMessageGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MotivationalMessagesRepository
) {
    
    companion object {
        private const val TAG = "MotivationalMsgGen"
        private const val MODEL_NAME = "gemini-1.5-flash"
        
        // API key should be stored in BuildConfig or secured storage
        // For development, you can add it to local.properties: GEMINI_API_KEY=your_key
        private val API_KEY: String by lazy {
            try {
                BuildConfig::class.java.getField("GEMINI_API_KEY").get(null) as? String ?: ""
            } catch (e: Exception) {
                ""
            }
        }
    }
    
    private val generativeModel: GenerativeModel? by lazy {
        if (API_KEY.isBlank()) {
            Log.w(TAG, "Gemini API key not configured")
            null
        } else {
            GenerativeModel(
                modelName = MODEL_NAME,
                apiKey = API_KEY,
                generationConfig = generationConfig {
                    temperature = 0.9f
                    topK = 40
                    topP = 0.95f
                    maxOutputTokens = 256
                }
            )
        }
    }
    
    /**
     * Check if AI generation is available.
     */
    fun isAvailable(): Boolean {
        return generativeModel != null && API_KEY.isNotBlank()
    }
    
    /**
     * Generate a personalized motivational message.
     */
    suspend fun generateMessage(
        personality: AIPersonality = AIPersonality.FRIENDLY_COACH,
        category: MessageCategory = MessageCategory.MOTIVATION,
        context: MessageContext = MessageContext()
    ): Result<MotivationalMessage> = withContext(Dispatchers.IO) {
        try {
            val model = generativeModel ?: return@withContext Result.failure(
                IllegalStateException("Gemini API not configured")
            )
            
            // Check rate limiting
            if (!repository.canGenerateAIMessage()) {
                return@withContext Result.failure(
                    IllegalStateException("Daily AI message limit reached")
                )
            }
            
            // Build the prompt
            val prompt = buildPrompt(personality, category, context)
            
            Log.d(TAG, "Generating message with prompt: ${prompt.take(100)}...")
            
            // Generate response
            val response = model.generateContent(prompt)
            val generatedText = response.text?.trim() ?: return@withContext Result.failure(
                IllegalStateException("Empty response from AI")
            )
            
            // Parse the response
            val message = parseResponse(generatedText, category, personality.toTone())
            
            // Save the generated message
            repository.saveAIGeneratedMessage(message.content, message.category, message.tone)
            
            // Record the generation
            repository.incrementAIGenerationCount()
            
            Log.d(TAG, "Generated message: ${message.content.take(50)}...")
            
            Result.success(message)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating message", e)
            Result.failure(e)
        }
    }
    
    /**
     * Generate a message for a specific goal context.
     */
    suspend fun generateGoalMessage(
        goalTitle: String,
        progressPercent: Int,
        personality: AIPersonality = AIPersonality.FRIENDLY_COACH
    ): Result<MotivationalMessage> {
        val context = MessageContext(
            goalTitle = goalTitle,
            goalProgress = progressPercent,
            focusOnGoal = true
        )
        
        val category = when {
            progressPercent >= 100 -> MessageCategory.ACHIEVEMENT
            progressPercent >= 75 -> MessageCategory.MOTIVATION
            progressPercent >= 50 -> MessageCategory.FOCUS
            progressPercent >= 25 -> MessageCategory.CHALLENGE
            else -> MessageCategory.MOTIVATION
        }
        
        return generateMessage(personality, category, context)
    }
    
    /**
     * Generate a message for a streak celebration.
     */
    suspend fun generateStreakMessage(
        streakDays: Int,
        personality: AIPersonality = AIPersonality.CHEERLEADER
    ): Result<MotivationalMessage> {
        val context = MessageContext(
            streakDays = streakDays,
            focusOnStreak = true
        )
        
        return generateMessage(personality, MessageCategory.STREAK, context)
    }
    
    /**
     * Generate a morning message.
     */
    suspend fun generateMorningMessage(
        userName: String? = null,
        personality: AIPersonality = AIPersonality.FRIENDLY_COACH
    ): Result<MotivationalMessage> {
        val context = MessageContext(
            userName = userName,
            timeOfDay = "morning"
        )
        
        return generateMessage(personality, MessageCategory.MORNING, context)
    }
    
    /**
     * Generate an evening message.
     */
    suspend fun generateEveningMessage(
        userName: String? = null,
        personality: AIPersonality = AIPersonality.EMPATHETIC_FRIEND
    ): Result<MotivationalMessage> {
        val context = MessageContext(
            userName = userName,
            timeOfDay = "evening"
        )
        
        return generateMessage(personality, MessageCategory.EVENING, context)
    }
    
    /**
     * Build the prompt for message generation.
     */
    private fun buildPrompt(
        personality: AIPersonality,
        category: MessageCategory,
        context: MessageContext
    ): String {
        val personalityDescription = getPersonalityDescription(personality)
        val categoryDescription = getCategoryDescription(category)
        
        val contextParts = mutableListOf<String>()
        
        context.userName?.let { contextParts.add("El usuario se llama $it.") }
        context.goalTitle?.let { contextParts.add("Está trabajando en su meta: '$it'.") }
        context.goalProgress?.let { contextParts.add("Ha completado el $it% de su meta.") }
        context.streakDays?.let { contextParts.add("Tiene una racha de $it días consecutivos.") }
        context.timeOfDay?.let { 
            val desc = when (it) {
                "morning" -> "Es por la mañana."
                "evening" -> "Es por la noche."
                else -> ""
            }
            if (desc.isNotEmpty()) contextParts.add(desc)
        }
        
        val contextDescription = if (contextParts.isNotEmpty()) {
            "\n\nContexto del usuario:\n${contextParts.joinToString("\n")}"
        } else ""
        
        return """
            |Eres un asistente de motivación personal con la siguiente personalidad:
            |$personalityDescription
            |
            |Genera UN SOLO mensaje motivacional en español para la categoría: $categoryDescription
            |$contextDescription
            |
            |REGLAS IMPORTANTES:
            |1. El mensaje debe tener entre 50 y 150 caracteres
            |2. Debe ser inspirador, positivo y accionable
            |3. Usa un tono ${personality.displayName.lowercase()}
            |4. NO uses hashtags ni emojis en el texto
            |5. NO uses comillas alrededor del mensaje
            |6. Responde SOLO con el mensaje, sin explicaciones adicionales
            |7. El mensaje debe ser único y original
            |
            |Mensaje:
        """.trimMargin()
    }
    
    /**
     * Get personality description for the prompt.
     */
    private fun getPersonalityDescription(personality: AIPersonality): String {
        return when (personality) {
            AIPersonality.FRIENDLY_COACH -> 
                "Eres un coach amigable y cercano. Animas con calidez y comprensión."
            AIPersonality.STRICT_COACH -> 
                "Eres un coach estricto pero justo. Empujas al usuario a dar lo mejor de sí."
            AIPersonality.WISE_MENTOR -> 
                "Eres un mentor sabio con perspectiva filosófica. Ofreces reflexiones profundas."
            AIPersonality.EMPATHETIC_FRIEND -> 
                "Eres un amigo empático y comprensivo. Conectas emocionalmente."
            AIPersonality.CHEERLEADER -> 
                "Eres un animador entusiasta. Celebras cada logro con energía."
            AIPersonality.ZEN_MASTER -> 
                "Eres un maestro zen calmado. Ofreces paz interior y mindfulness."
            AIPersonality.SCIENTIST -> 
                "Eres un científico curioso. Usas datos y lógica para motivar."
            AIPersonality.ADVENTURER -> 
                "Eres un aventurero audaz. Inspiras a explorar y tomar riesgos."
        }
    }
    
    /**
     * Get category description for the prompt.
     */
    private fun getCategoryDescription(category: MessageCategory): String {
        return when (category) {
            MessageCategory.MORNING -> "Buenos días - para empezar el día con energía"
            MessageCategory.EVENING -> "Buenas noches - para reflexionar y descansar"
            MessageCategory.ACHIEVEMENT -> "Logro - celebrar un éxito"
            MessageCategory.FOCUS -> "Enfoque - mantener la concentración"
            MessageCategory.GRATITUDE -> "Gratitud - apreciar lo bueno"
            MessageCategory.CHALLENGE -> "Desafío - superar dificultades"
            MessageCategory.STREAK -> "Racha - mantener consistencia"
            MessageCategory.CELEBRATION -> "Celebración - festejar momentos especiales"
            MessageCategory.MOTIVATION -> "Motivación general - inspirar acción"
            MessageCategory.PRODUCTIVITY -> "Productividad - ser más eficiente"
            MessageCategory.MINDFULNESS -> "Mindfulness - estar presente"
            MessageCategory.SELF_CARE -> "Autocuidado - cuidar de uno mismo"
            MessageCategory.GROWTH -> "Crecimiento - desarrollo personal"
            MessageCategory.RESILIENCE -> "Resiliencia - superar adversidad"
            MessageCategory.WEEKEND -> "Fin de semana - disfrutar el descanso"
            MessageCategory.MONDAY -> "Lunes - empezar la semana con fuerza"
            MessageCategory.MILESTONE -> "Hito - marcar un logro importante"
            MessageCategory.COMEBACK -> "Regreso - volver después de ausencia"
        }
    }
    
    /**
     * Parse the AI response into a MotivationalMessage.
     */
    private fun parseResponse(
        response: String,
        category: MessageCategory,
        tone: MessageTone
    ): MotivationalMessage {
        // Clean up the response
        val cleanedContent = response
            .trim()
            .removePrefix("\"")
            .removeSuffix("\"")
            .replace(Regex("[#@]\\w+"), "") // Remove hashtags
            .trim()
        
        return MotivationalMessage(
            id = UUID.randomUUID().toString(),
            content = cleanedContent,
            category = category,
            tone = tone,
            emoji = category.emoji,
            isAIGenerated = true,
            isCustom = false
        )
    }
    
    /**
     * Convert AIPersonality to MessageTone.
     */
    private fun AIPersonality.toTone(): MessageTone {
        return when (this) {
            AIPersonality.FRIENDLY_COACH -> MessageTone.COACH
            AIPersonality.STRICT_COACH -> MessageTone.COACH
            AIPersonality.WISE_MENTOR -> MessageTone.WISE
            AIPersonality.EMPATHETIC_FRIEND -> MessageTone.FRIENDLY
            AIPersonality.CHEERLEADER -> MessageTone.ENERGETIC
            AIPersonality.ZEN_MASTER -> MessageTone.CALM
            AIPersonality.SCIENTIST -> MessageTone.PRACTICAL
            AIPersonality.ADVENTURER -> MessageTone.ENERGETIC
        }
    }
}

/**
 * Context information for generating personalized messages.
 */
data class MessageContext(
    val userName: String? = null,
    val goalTitle: String? = null,
    val goalProgress: Int? = null,
    val streakDays: Int? = null,
    val timeOfDay: String? = null,
    val focusOnGoal: Boolean = false,
    val focusOnStreak: Boolean = false
)
