package net.gini.android.bank.sdk.screenapiexample

import android.app.Activity.RESULT_FIRST_USER
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import net.gini.android.bank.sdk.screenapiexample.NoExtractionContract.Companion.RESULT_START_GINI_CAPTURE
import net.gini.android.bank.sdk.screenapiexample.databinding.ActivityNoExtractionsBinding

/**
 * Shown when none of the Pay5 extractions were received.
 *
 * Displays information about rotating the image to the correct orientation.
 *
 * We recommend showing a similar screen in your app to aid the user in taking better pictures to improve the
 * quality of the extractions.
 */
class NoExtractionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityNoExtractionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonNew.setOnClickListener {
            setResult(RESULT_START_GINI_CAPTURE)
            finish()
        }
    }
}

class NoExtractionContract : ActivityResultContract<Unit, Boolean>() {
    override fun createIntent(context: Context, input: Unit) = Intent(
        context, NoExtractionsActivity::class.java
    )

    override fun parseResult(resultCode: Int, result: Intent?): Boolean =
        resultCode == RESULT_START_GINI_CAPTURE

    companion object {
        const val RESULT_START_GINI_CAPTURE = RESULT_FIRST_USER + 1
    }
}