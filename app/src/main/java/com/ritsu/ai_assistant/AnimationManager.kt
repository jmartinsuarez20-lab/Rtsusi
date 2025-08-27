package com.ritsu.ai_assistant

import android.util.Log

/**
 * A placeholder singleton to manage Live2D animations.
 *
 * This class provides a clean interface for other parts of the app (like ViewModels)
 * to request animation changes (e.g., "start speaking," "start thinking"). It is
 * decoupled from the core Live2D implementation.
 *
 * In a real implementation, the methods of this class would call the actual
 * Live2D SDK functions to start and stop motions and expressions on the model.
 * The current version only logs the requested actions.
 */
class AnimationManager {

    companion object {
        private var s_instance: AnimationManager? = null
        fun getInstance(): AnimationManager {
            if (s_instance == null) {
                s_instance = AnimationManager()
            }
            return s_instance!!
        }
    }

    enum class Motion(val motionName: String) {
        IDLE("Idle"),
        TAP("Tap"),
        SHAKE("Shake"),
        SPEAKING("Speaking"),
        LISTENING("Listening"),
        THINKING("Thinking")
    }

    enum class Expression(val expressionName: String) {
        NEUTRAL("Neutral"),
        HAPPY("Happy"),
        CONCERNED("Concerned")
    }

    fun startMotion(motion: Motion) {
        // In a real implementation, this would call the Live2D SDK to start a motion.
        // e.g., LAppLive2DManager.getInstance().startMotion(motion.motionName, 0, 1)
        Log.d("AnimationManager", "Starting motion: ${motion.motionName} (Placeholder)")
    }

    fun setExpression(expression: Expression) {
        // In a real implementation, this would call the Live2D SDK to set an expression.
        // e.g., LAppLive2DManager.getInstance().setExpression(expression.expressionName)
        Log.d("AnimationManager", "Setting expression: ${expression.expressionName} (Placeholder)")
    }
}
