package com.singingbowl.tuner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.singingbowl.tuner.audio.AudioInput
import com.singingbowl.tuner.data.AppDatabase
import com.singingbowl.tuner.data.BowlProfile
import com.singingbowl.tuner.dsp.PitchDetector
import com.singingbowl.tuner.utils.PitchConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private val REQUEST_RECORD_AUDIO_PERMISSION = 200
    private lateinit var audioInput: AudioInput
    private lateinit var pitchDetector: PitchDetector

    private lateinit var noteNameTextView: TextView
    private lateinit var frequencyTextView: TextView
    private lateinit var centsTextView: TextView
    private lateinit var stabilityTextView: TextView
    private lateinit var tunerIndicator: View
    private lateinit var saveButton: Button

    private val frequencyHistory = mutableListOf<Float>()
    private val historySize = 5

    private var currentFrequency: Float = 0.0f
    private var currentNoteResult: PitchConverter.NoteResult? = null

    private val db by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        noteNameTextView = findViewById(R.id.noteNameTextView)
        frequencyTextView = findViewById(R.id.frequencyTextView)
        centsTextView = findViewById(R.id.centsTextView)
        stabilityTextView = findViewById(R.id.stabilityTextView)
        tunerIndicator = findViewById(R.id.tunerIndicator)
        saveButton = findViewById(R.id.saveButton)

        saveButton.setOnClickListener { showSaveDialog() }

        audioInput = AudioInput(this)
        val bufferSize = audioInput.getMinBufferSize()
        pitchDetector = PitchDetector(44100, bufferSize)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        } else {
            startAudioCapture()
        }
    }

    private fun showSaveDialog() {
        if (currentFrequency <= 0 || currentNoteResult == null) {
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Сохранить профиль чаши")

        val input = EditText(this)
        input.hint = "Введите название чаши"
        builder.setView(input)

        builder.setPositiveButton("Сохранить") { dialog, _ ->
            val name = input.text.toString()
            if (name.isNotBlank()) {
                lifecycleScope.launch {
                    db.bowlProfileDao().insert(
                        BowlProfile(
                            name = name,
                            frequency = currentFrequency,
                            noteName = currentNoteResult!!.fullNoteName,
                            cents = currentNoteResult!!.cents
                        )
                    )
                }
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Отмена") { dialog, _ -> dialog.cancel() }

        builder.show()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startAudioCapture()
            } else {
                noteNameTextView.text = "Error"
                frequencyTextView.text = "Permission for audio recording was denied."
                android.widget.Toast.makeText(this, "Permission for audio recording was denied.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startAudioCapture() {
        audioInput.start()
        lifecycleScope.launch(Dispatchers.Default) {
            audioInput.audioData.collect { data ->
                if (data.isNotEmpty()) {
                    val frequency = pitchDetector.getPitch(data)
                    val (smoothedFrequency, isStable) = smoothFrequency(frequency)

                    withContext(Dispatchers.Main) {
                        updateUi(smoothedFrequency, isStable)
                    }
                }
            }
        }
    }

    private fun updateUi(frequency: Float, isStable: Boolean) {
        currentFrequency = frequency
        if (frequency > 0) {
            val noteResult = PitchConverter.frequencyToNote(frequency.toDouble())
            currentNoteResult = noteResult

            noteNameTextView.text = noteResult.noteName
            frequencyTextView.text = "%.2f Hz".format(frequency)
            centsTextView.text = "%+d cents".format(noteResult.cents)

            val translationX = (noteResult.cents / 50.0f) * (tunerIndicator.width / 2.0f)
            tunerIndicator.translationX = translationX

            if (noteResult.cents in -5..5) {
                noteNameTextView.setTextColor(ContextCompat.getColor(this, R.color.tuned_green))
            } else {
                noteNameTextView.setTextColor(ContextCompat.getColor(this, R.color.white))
            }

            if (isStable) {
                stabilityTextView.text = getString(R.string.stability_stable)
                stabilityTextView.visibility = View.VISIBLE
                saveButton.isEnabled = true
            } else {
                stabilityTextView.visibility = View.INVISIBLE
                saveButton.isEnabled = false
            }

        } else {
            currentNoteResult = null
            noteNameTextView.text = "--"
            frequencyTextView.text = "..."
            centsTextView.text = ""
            stabilityTextView.visibility = View.INVISIBLE
            tunerIndicator.translationX = 0f
            noteNameTextView.setTextColor(ContextCompat.getColor(this, R.color.white))
            saveButton.isEnabled = false
        }
    }

    private fun smoothFrequency(frequency: Float): Pair<Float, Boolean> {
        if (frequency > 0) {
            frequencyHistory.add(frequency)
            if (frequencyHistory.size > historySize) {
                frequencyHistory.removeAt(0)
            }
        }

        val validFrequencies = frequencyHistory.filter { it > 0 }
        if (validFrequencies.isEmpty()) return -1.0f to false

        val average = validFrequencies.average().toFloat()
        val deviation = validFrequencies.map { kotlin.math.abs(it - average) }.average()

        val isStable = deviation < 1.0 // Heuristic for stability
        return average to isStable
    }

    override fun onDestroy() {
        super.onDestroy()
        audioInput.stop()
    }
}
