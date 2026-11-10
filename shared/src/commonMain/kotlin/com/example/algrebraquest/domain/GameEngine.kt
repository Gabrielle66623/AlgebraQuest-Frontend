package com.example.algrebraquest.domain

import algrebraquest.shared.generated.resources.Res
import algrebraquest.shared.generated.resources.imagen3
import algrebraquest.shared.generated.resources.imagen4
import algrebraquest.shared.generated.resources.imagen5
import algrebraquest.shared.generated.resources.imagen6
import algrebraquest.shared.generated.resources.imagen7
import algrebraquest.shared.generated.resources.imagen8
import algrebraquest.shared.generated.resources.personaje3
import com.example.algrebraquest.data.Mundo
import com.example.algrebraquest.data.Nivel
import com.example.algrebraquest.data.Pregunta
import org.jetbrains.compose.resources.DrawableResource
import kotlin.random.Random

/**
 * Selector aleatorio de personaje (equivalente a ImagenAleatoria en js/core/images.js).
 * Elige una imagen al azar sin repetir inmediatamente la anterior.
 */
object RandomCharacterImage {
    private val options: List<DrawableResource> = listOf(
        Res.drawable.personaje3,
        Res.drawable.imagen3,
        Res.drawable.imagen4,
        Res.drawable.imagen5,
        Res.drawable.imagen6,
        Res.drawable.imagen7,
        Res.drawable.imagen8,
    )
    private var lastIndex: Int = -1

    fun next(): DrawableResource {
        if (options.size <= 1) return options.first()
        val candidates = options.indices.filter { it != lastIndex }
        val chosen = candidates[Random.nextInt(candidates.size)]
        lastIndex = chosen
        return options[chosen]
    }
}

/**
 * Motor de partida (equivalente a Engine en js/core/engine.js, R-08 del inventario).
 * Baraja el orden de preguntas de un nivel y las opciones de cada pregunta,
 * lleva la cuenta de aciertos, racha, puntos de aula y avance porcentual.
 */
class GameEngine(
    val mundo: Mundo,
    val nivel: Nivel,
) {
    val questionOrder: List<Int> = nivel.preguntas.indices.shuffled()
    var currentStep: Int = 0
        private set
    var aciertos: Int = 0
        private set
    var racha: Int = 0
        private set
    var puntosQuiz: Int = 0
        private set

    val totalQuestions: Int get() = questionOrder.size

    val currentQuestionOriginalIndex: Int
        get() = questionOrder.getOrElse(currentStep) { 0 }

    val currentQuestion: Pregunta?
        get() = nivel.preguntas.getOrNull(currentQuestionOriginalIndex)

    val progressFraction: Float
        get() = if (totalQuestions <= 0) 0f else currentStep.toFloat() / totalQuestions.toFloat()

    val isLastQuestion: Boolean
        get() = currentStep >= totalQuestions - 1

    fun shuffledOptionsForCurrent(): List<String> =
        currentQuestion?.opciones?.shuffled().orEmpty()

    fun recordResult(correct: Boolean, pointsEarned: Int, serverStreak: Int) {
        if (correct) {
            aciertos++
            racha = if (serverStreak > 0) serverStreak else racha + 1
            puntosQuiz += pointsEarned
        } else {
            racha = 0
        }
    }

    /** Avanza a la siguiente pregunta. Devuelve true si aún quedan preguntas por responder. */
    fun nextQuestion(): Boolean {
        currentStep++
        return currentStep < totalQuestions
    }
}
