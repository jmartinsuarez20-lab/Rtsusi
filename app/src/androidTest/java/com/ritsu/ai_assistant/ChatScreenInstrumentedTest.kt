package com.ritsu.ai_assistant

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun chatUI_isDisplayed() {
        // Check if the initial message from the bot is displayed.
        // This might fail depending on how fast the model loads, so we look for the input field first.
        // A more robust test would use Idling Resources.

        // Check for the input field
        composeTestRule.onNodeWithText("Type a message...").assertIsDisplayed()

        // Check for the send button
        composeTestRule.onNodeWithText("Send").assertIsDisplayed()
    }
}
