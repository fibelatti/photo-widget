package com.fibelatti.photowidget.widget

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fibelatti.photowidget.platform.intentExtras

/**
 * Transparent activity for the sole purpose of showing user feedback where a visual context is not
 * available otherwise. This activity shows a [android.widget.Toast] and finishes itself.
 */
class HeadlessFeedbackActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Toast.makeText(
            /* context = */ this,
            /* text = */ intent.message,
            /* duration = */ Toast.LENGTH_SHORT,
        ).show()

        finish()
    }

    companion object {

        private var Intent.message: String by intentExtras()

        fun newIntent(
            context: Context,
            message: String,
        ): Intent {
            return Intent(context, HeadlessFeedbackActivity::class.java).apply {
                this.message = message
                // Always new task since this activity is meant to be started from a non-UI context
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
}
