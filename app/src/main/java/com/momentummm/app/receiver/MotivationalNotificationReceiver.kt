package com.momentummm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.momentummm.app.data.manager.MotivationalNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.momentummm.app.R

/**
 * Broadcast receiver for handling motivational notification actions.
 * 
 * Actions:
 * - ACTION_LOVE: Mark message as loved
 * - ACTION_ANOTHER: Request another motivational message
 * - ACTION_DISMISS: Track when notification is dismissed
 */
@AndroidEntryPoint
class MotivationalNotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MotivationalNotifRcvr"
        
        const val ACTION_LOVE = "com.momentummm.app.action.MOTIVATIONAL_LOVE"
        const val ACTION_ANOTHER = "com.momentummm.app.action.MOTIVATIONAL_ANOTHER"
        const val ACTION_DISMISS = "com.momentummm.app.action.MOTIVATIONAL_DISMISS"
        
        const val EXTRA_MESSAGE_ID = "message_id"
    }
    
    @Inject
    lateinit var motivationalNotificationManager: MotivationalNotificationManager
    
    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getStringExtra(EXTRA_MESSAGE_ID)
        
        Log.d(TAG, "Received action: ${intent.action}, messageId: $messageId")
        
        when (intent.action) {
            ACTION_LOVE -> handleLoveAction(context, messageId)
            ACTION_ANOTHER -> handleAnotherAction(context, messageId)
            ACTION_DISMISS -> handleDismissAction(messageId)
            else -> Log.w(TAG, "Unknown action: ${intent.action}")
        }
    }
    
    private fun handleLoveAction(context: Context, messageId: String?) {
        if (messageId.isNullOrEmpty()) {
            Log.w(TAG, "Love action received without message ID")
            return
        }
        
        try {
            motivationalNotificationManager.handleLoveAction(messageId)
            
            // Show quick feedback
            Toast.makeText(context, context.getString(R.string.fav_saved), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error handling love action", e)
        }
    }
    
    private fun handleAnotherAction(context: Context, messageId: String?) {
        if (messageId.isNullOrEmpty()) {
            Log.w(TAG, "Another action received without message ID")
            return
        }
        
        try {
            motivationalNotificationManager.handleAnotherMessageAction(messageId)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling another message action", e)
        }
    }
    
    private fun handleDismissAction(messageId: String?) {
        // Track dismissal for analytics
        Log.d(TAG, "Notification dismissed: $messageId")
    }
}
