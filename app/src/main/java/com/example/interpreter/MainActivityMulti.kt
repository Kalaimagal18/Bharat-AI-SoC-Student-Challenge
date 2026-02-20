package com.example.interpreter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.nl.translate.*
import java.util.*

class MainActivityMulti : AppCompatActivity(), TextToSpeech.OnInitListener {

    // Speech + TTS
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech

    // UI
    private lateinit var spinnerInput: Spinner
    private lateinit var spinnerOutput: Spinner
    private lateinit var btnSpeak: Button
    private lateinit var btnTyped: Button
    private lateinit var btnSwap: Button
    private lateinit var btnTheme: Button

    private lateinit var editTyped: EditText
    private lateinit var txtOriginal: TextView
    private lateinit var txtTranslated: TextView

    // Translator Cache
    private var translator: Translator? = null
    private var modelReady = false
    private var isListening = false
    private var continuousMode = false

    // Supported Languages
    private val langMap = mapOf(
        "English" to Pair("en-IN", TranslateLanguage.ENGLISH),
        "Hindi" to Pair("hi-IN", TranslateLanguage.HINDI),
        "Tamil" to Pair("ta-IN", TranslateLanguage.TAMIL)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_multi)

        requestMicPermission()

        // UI Init
        spinnerInput = findViewById(R.id.spinnerInput)
        spinnerOutput = findViewById(R.id.spinnerOutput)

        btnSpeak = findViewById(R.id.btnSpeak)
        btnTyped = findViewById(R.id.btnTyped)
        btnSwap = findViewById(R.id.btnSwap)
        btnTheme = findViewById(R.id.btnTheme)

        editTyped = findViewById(R.id.editTyped)

        txtOriginal = findViewById(R.id.txtOriginal)
        txtTranslated = findViewById(R.id.txtTranslated)

        // Spinner Setup
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            langMap.keys.toList()
        )

        spinnerInput.adapter = adapter
        spinnerOutput.adapter = adapter

        spinnerInput.setSelection(0) // English
        spinnerOutput.setSelection(1) // Hindi

        // Init Speech + TTS
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        tts = TextToSpeech(this, this)

        // Setup Translator once
        setupTranslator()

        // Update translator when selection changes
        spinnerInput.onItemSelectedListener = changeListener
        spinnerOutput.onItemSelectedListener = changeListener

        // Speak Button
        btnSpeak.setOnClickListener { startListening() }

        // Typed Translate Button
        btnTyped.setOnClickListener {
            val typedText = editTyped.text.toString()
            if (typedText.isBlank()) return@setOnClickListener

            txtOriginal.text = "Typed: $typedText"
            translateAndSpeak(typedText)
        }

        // Swap Languages
        btnSwap.setOnClickListener {
            val a = spinnerInput.selectedItemPosition
            val b = spinnerOutput.selectedItemPosition
            spinnerInput.setSelection(b)
            spinnerOutput.setSelection(a)
        }

        // Theme Toggle
        btnTheme.setOnClickListener {
            val mode = AppCompatDelegate.getDefaultNightMode()
            if (mode == AppCompatDelegate.MODE_NIGHT_YES)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

            recreate()
        }
    }

    // Spinner Change Listener
    private val changeListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(
            parent: AdapterView<*>, view: android.view.View?,
            position: Int, id: Long
        ) {
            setupTranslator()
        }

        override fun onNothingSelected(parent: AdapterView<*>) {}
    }

    // Setup Translator Safely
    private fun setupTranslator() {

        modelReady = false

        val inName = spinnerInput.selectedItem.toString()
        val outName = spinnerOutput.selectedItem.toString()

        val sourceLang = langMap[inName]!!.second
        val targetLang = langMap[outName]!!.second

        if (sourceLang == targetLang) {
            txtTranslated.text = "Choose different languages!"
            return
        }

        translator?.close()

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLang)
            .setTargetLanguage(targetLang)
            .build()

        translator = Translation.getClient(options)

        // Auto Download Model Once
        translator!!.downloadModelIfNeeded()
            .addOnSuccessListener {
                modelReady = true
                txtTranslated.text = "Model Ready Offline ✅"
            }
            .addOnFailureListener {
                txtTranslated.text =
                    "Model download failed. Turn ON internet once."
            }
    }

    // Speech Recognition
    private fun startListening() {
        val inputLang = spinnerInput.selectedItem.toString()

        val recogLocale = when (inputLang) {
            "English" -> "en-IN"
            "Hindi" -> "hi-IN"
            "Tamil" -> "ta-IN"
            else -> "en-IN"
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, recogLocale)

        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")

        speechRecognizer.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                txtOriginal.text = "Listening..."
            }

            override fun onResults(results: Bundle?) {

                val matches =
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                if (!matches.isNullOrEmpty()) {

                    val spokenText = matches[0]

                    txtOriginal.text = "You: $spokenText"

                    translateAndSpeak(spokenText)
                }
            }

            override fun onError(error: Int) {
                txtOriginal.text = "Speech failed (Error $error). Try again."
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer.startListening(intent)
    }

    // Translate + Speak
    private fun translateAndSpeak(text: String) {

        if (!modelReady) {
            Toast.makeText(this,
                "Model not ready yet!", Toast.LENGTH_SHORT).show()
            return
        }

        translator?.translate(text)
            ?.addOnSuccessListener { translated ->

                txtTranslated.text = "→ $translated"

                tts.setSpeechRate(0.75f)
                tts.speak(translated,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "OUT")
            }
    }

    // TTS Init
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("hi")
            tts.setSpeechRate(0.8f)  // slower
            tts.setPitch(1.1f)       // slightly higher

            val voices = tts.voices
            for (v in voices) {
                if (v.locale.language == "hi") {
                    tts.voice = v
                    break
                }
            }
        }
    }

    // Permission
    private fun requestMicPermission() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                1
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        translator?.close()
        speechRecognizer.destroy()
        tts.shutdown()
    }
}