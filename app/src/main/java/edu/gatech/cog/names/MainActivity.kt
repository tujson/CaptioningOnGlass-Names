package edu.gatech.cog.names

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.integration.android.IntentIntegrator
import edu.gatech.cog.names.models.Message
import edu.gatech.cog.names.models.Study
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.concurrent.thread

private val TAG = MainActivity::class.java.simpleName

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: TextAdapter
    private val displayContent = mutableListOf<String>()
    private lateinit var script: List<Message>

    private var participantName = ""
    private var studyStartTime: Long = 0L
    private var activeMessage: Message? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        adapter = TextAdapter(displayContent)
        rvText.adapter = adapter
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        rvText.layoutManager = layoutManager

        startQrCodeScanner()
    }

    // https://amzn.com/dp/B07RSGXXP2
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Log.v(TAG, "KeyCode: $keyCode")
        when (keyCode) {
            KeyEvent.KEYCODE_B,
            KeyEvent.KEYCODE_PAGE_UP,
            KeyEvent.KEYCODE_PAGE_DOWN,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            -> {
                onClicker()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun onClicker() {
        if (studyStartTime == 0L) {
            startStudy(participantName)
        } else {
            activeMessage?.let {
                it.timeToAck = System.currentTimeMillis() - it.displayTime
            }
        }
    }

    override fun onPause() {
        super.onPause()

        if (participantName.isNotEmpty()) {
            val sanitizedScript = mutableListOf<Message>()
            val acknowledgedMessages = mutableListOf<Message>()

            script.forEach {
                if (it.displayTime != 0L) {
                    val sanitizedMessage = Message(
                        if (it.text == participantName) {
                            "PARTICIPANT"
                        } else {
                            it.text
                        },
                        it.displayTime,
                        it.timeToAck,
                    )
                    sanitizedScript.add(sanitizedMessage)

                    if (it.timeToAck != 0L) {
                        acknowledgedMessages.add(sanitizedMessage)
                    }
                }
            }

            val study = Study(sanitizedScript, acknowledgedMessages)
            saveStudy(study)
        }
    }

    private fun saveStudy(study: Study) {
        Toast.makeText(this, "Saving ${study.acknowledgedMessages.size}", Toast.LENGTH_SHORT).show()
        // Save locally
        this.openFileOutput("$studyStartTime-results.txt", Context.MODE_PRIVATE).use {
            it?.write(study.toString().toByteArray())
        }

        //   Save to Firebase Cloud Firestore
        FirebaseFirestore.getInstance().collection("studies")
            .document(studyStartTime.toString())
            .set(study)
            .addOnSuccessListener {
                runOnUiThread {
                    Toast.makeText(this, "Uploaded!", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun startQrCodeScanner() {
        IntentIntegrator(this)
            .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            .setPrompt("Scan QR Code")
            .setBeepEnabled(false)
            .setBarcodeImageEnabled(false)
            .initiateScan()
    }

    /**
     * Sends messages to UI
     */
    private fun startStudy(participantName: String) {
        this.participantName = participantName
        script = loadScript(resources, participantName)

        studyStartTime = System.currentTimeMillis()

        thread {
            script.forEach { scriptMessage ->
                runOnUiThread {
                    tvText.text = scriptMessage.text
                }
                scriptMessage.displayTime = System.currentTimeMillis()
                activeMessage = scriptMessage

                Thread.sleep(waitDuration(scriptMessage.text))

                runOnUiThread {
                    displayContent.add(scriptMessage.text)
                    adapter.notifyItemInserted(displayContent.size - 1)
                    rvText.scrollToPosition(displayContent.size - 1)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        IntentIntegrator.parseActivityResult(requestCode, resultCode, data)?.let { barcodeResult ->
            val statusText = barcodeResult.contents?.let { barcodeContents ->
                this.participantName = barcodeContents
                barcodeContents
            } ?: run {
                "FAILURE"
            }

            runOnUiThread {
                Toast.makeText(this, statusText, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
