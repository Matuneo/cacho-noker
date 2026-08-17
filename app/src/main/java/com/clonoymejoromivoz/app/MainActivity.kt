package com.clonoymejoromivoz.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.clonoymejoromivoz.audio.Mp3Encoder
import com.clonoymejoromivoz.audio.PcmAudio
import com.clonoymejoromivoz.audio.WavPcmIO
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsPocketModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsSupertonicModelConfig
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {
    private val recordSampleRate = 24_000
    private val recording = AtomicBoolean(false)

    private lateinit var modeClone: RadioButton
    private lateinit var modeNarrator: RadioButton
    private lateinit var profilePanel: LinearLayout
    private lateinit var profileStatus: TextView
    private lateinit var recordButton: Button
    private lateinit var importButton: Button
    private lateinit var textInput: EditText
    private lateinit var speedSeek: SeekBar
    private lateinit var speedLabel: TextView
    private lateinit var formatSpinner: Spinner
    private lateinit var generateButton: Button
    private lateinit var playButton: Button
    private lateinit var saveButton: Button
    private lateinit var shareButton: Button
    private lateinit var progress: ProgressBar
    private lateinit var statusText: TextView

    private var recorder: AudioRecord? = null
    private var mediaPlayer: MediaPlayer? = null
    private var voiceProfile: PcmAudio? = null
    private var generatedFile: File? = null
    private var generatedMime = "audio/wav"
    private var cloneTts: OfflineTts? = null
    private var narratorTts: OfflineTts? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecording() else toast("Se necesita permiso para grabar tu voz")
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) importProfile(uri)
    }

    private val saveLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("audio/*")
    ) { uri ->
        val source = generatedFile
        if (uri != null && source != null) {
            runCatching {
                contentResolver.openOutputStream(uri)?.use { output ->
                    source.inputStream().use { input -> input.copyTo(output) }
                } ?: error("No se pudo abrir el destino")
            }.onSuccess {
                toast("Audio guardado correctamente")
            }.onFailure {
                toast("No se pudo guardar: ${it.message}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()
        configureUi()
        restoreProfile()
    }

    private fun bindViews() {
        modeClone = findViewById(R.id.modeClone)
        modeNarrator = findViewById(R.id.modeNarrator)
        profilePanel = findViewById(R.id.profilePanel)
        profileStatus = findViewById(R.id.profileStatus)
        recordButton = findViewById(R.id.recordButton)
        importButton = findViewById(R.id.importButton)
        textInput = findViewById(R.id.textInput)
        speedSeek = findViewById(R.id.speedSeek)
        speedLabel = findViewById(R.id.speedLabel)
        formatSpinner = findViewById(R.id.formatSpinner)
        generateButton = findViewById(R.id.generateButton)
        playButton = findViewById(R.id.playButton)
        saveButton = findViewById(R.id.saveButton)
        shareButton = findViewById(R.id.shareButton)
        progress = findViewById(R.id.progress)
        statusText = findViewById(R.id.statusText)
    }

    private fun configureUi() {
        formatSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("WAV · máxima fidelidad", "MP3 · 192 kbps")
        )

        findViewById<android.widget.RadioGroup>(R.id.modeGroup)
            .setOnCheckedChangeListener { _, _ ->
                profilePanel.visibility = if (modeClone.isChecked) View.VISIBLE else View.GONE
                statusText.text = if (modeClone.isChecked) {
                    "Clonación local: usa tu muestra para reproducir el timbre."
                } else {
                    "Narrador local en español: voz incluida, sin muestra previa."
                }
            }

        speedSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                speedLabel.text = String.format(Locale.US, "Velocidad: %.2f×", currentSpeed())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        recordButton.setOnClickListener {
            if (recording.get()) stopRecording() else ensurePermissionAndRecord()
        }
        importButton.setOnClickListener {
            importLauncher.launch(arrayOf("audio/wav", "audio/x-wav", "audio/*"))
        }
        generateButton.setOnClickListener { generateAudio() }
        playButton.setOnClickListener { playGenerated() }
        saveButton.setOnClickListener { saveGenerated() }
        shareButton.setOnClickListener { shareGenerated() }
    }

    private fun currentSpeed(): Float = 0.70f + speedSeek.progress / 100f

    private fun ensurePermissionAndRecord() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startRecording()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startRecording() {
        val minBuffer = AudioRecord.getMinBufferSize(
            recordSampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) {
            toast("Este teléfono no admite la configuración de grabación")
            return
        }

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            recordSampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuffer * 2
        )
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            toast("No se pudo abrir el micrófono")
            return
        }

        recorder = audioRecord
        recording.set(true)
        recordButton.text = "Detener"
        importButton.isEnabled = false
        profileStatus.text = "Grabando… habla de forma natural y continua."
        audioRecord.startRecording()

        Thread {
            val pcmBytes = ByteArrayOutputStream()
            val buffer = ShortArray(minBuffer)
            val startedAt = System.currentTimeMillis()
            try {
                while (recording.get() && System.currentTimeMillis() - startedAt < 60_000) {
                    val read = audioRecord.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        for (i in 0 until read) {
                            val value = buffer[i].toInt()
                            pcmBytes.write(value and 0xff)
                            pcmBytes.write((value ushr 8) and 0xff)
                        }
                    }
                }
            } finally {
                runCatching { audioRecord.stop() }
                audioRecord.release()
                recorder = null
                recording.set(false)
            }

            val bytes = pcmBytes.toByteArray()
            val samples = FloatArray(bytes.size / 2)
            for (i in samples.indices) {
                val lo = bytes[i * 2].toInt() and 0xff
                val hi = bytes[i * 2 + 1].toInt()
                samples[i] = (((hi shl 8) or lo).toShort().toInt() / 32768f)
            }
            val duration = samples.size.toFloat() / recordSampleRate

            runOnUiThread {
                recordButton.text = "Grabar de nuevo"
                importButton.isEnabled = true
                if (duration >= 3f) {
                    voiceProfile = PcmAudio(samples, recordSampleRate)
                    saveProfile(voiceProfile!!)
                    profileStatus.text = String.format(
                        Locale.US,
                        "Perfil listo · %.1f s · %d Hz",
                        duration,
                        recordSampleRate
                    )
                } else {
                    profileStatus.text = "La muestra fue muy corta. Graba al menos 3 segundos."
                }
            }
        }.start()
    }

    private fun stopRecording() {
        recording.set(false)
        runCatching { recorder?.stop() }
        recordButton.isEnabled = false
        profileStatus.text = "Procesando la muestra…"
        recordButton.postDelayed({ recordButton.isEnabled = true }, 500)
    }

    private fun profileFile(): File = File(filesDir, "profiles/mi_voz.wav")

    private fun saveProfile(profile: PcmAudio) {
        runCatching { WavPcmIO.writeMono16(profileFile(), profile.samples, profile.sampleRate) }
    }

    private fun restoreProfile() {
        val file = profileFile()
        if (!file.exists()) return
        runCatching { file.inputStream().use(WavPcmIO::read) }
            .onSuccess { profile ->
                voiceProfile = profile
                profileStatus.text = String.format(
                    Locale.US,
                    "Perfil guardado · %.1f s · %d Hz",
                    profile.samples.size.toFloat() / profile.sampleRate,
                    profile.sampleRate
                )
            }
    }

    private fun importProfile(uri: Uri) {
        setBusy(true, "Leyendo la muestra WAV…")
        Thread {
            runCatching {
                contentResolver.openInputStream(uri)?.use(WavPcmIO::read)
                    ?: error("No se pudo abrir el archivo")
            }.onSuccess { profile ->
                voiceProfile = profile
                saveProfile(profile)
                runOnUiThread {
                    setBusy(false, "Muestra importada correctamente.")
                    profileStatus.text = String.format(
                        Locale.US,
                        "Perfil listo · %.1f s · %d Hz",
                        profile.samples.size.toFloat() / profile.sampleRate,
                        profile.sampleRate
                    )
                }
            }.onFailure { error ->
                runOnUiThread { setBusy(false, "No se pudo importar: ${error.message}") }
            }
        }.start()
    }

    private fun generateAudio() {
        val text = textInput.text.toString().trim()
        if (text.isEmpty()) {
            toast("Escribe el texto que quieres narrar")
            return
        }
        if (modeClone.isChecked && voiceProfile == null) {
            toast("Primero graba o importa una muestra de voz")
            return
        }

        val cloneMode = modeClone.isChecked
        val mp3 = formatSpinner.selectedItemPosition == 1
        val speed = currentSpeed()
        val selectedProfile = voiceProfile
        setBusy(true, if (cloneMode) "Clonando y generando en el teléfono…" else "Generando narración local…")

        Thread {
            runCatching {
                val engine = if (cloneMode) getCloneTts() else getNarratorTts()
                val config = GenerationConfig(
                    speed = speed,
                    referenceAudio = if (cloneMode) selectedProfile!!.samples else null,
                    referenceSampleRate = if (cloneMode) selectedProfile!!.sampleRate else 0,
                    numSteps = 8,
                    extra = if (cloneMode) null else mapOf("lang" to "es")
                )
                val audio = engine.generateWithConfig(text, config)
                check(audio.samples.isNotEmpty()) { "El motor no devolvió audio" }

                val folder = File(cacheDir, "generated").apply { mkdirs() }
                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val wav = File(folder, "clono_y_mejoro_$stamp.wav")
                check(audio.save(wav.absolutePath)) { "No se pudo crear el WAV" }

                if (mp3) {
                    val output = File(folder, "clono_y_mejoro_$stamp.mp3")
                    Mp3Encoder.encode(audio.samples, audio.sampleRate, output)
                    output to "audio/mpeg"
                } else {
                    wav to "audio/wav"
                }
            }.onSuccess { (file, mime) ->
                generatedFile = file
                generatedMime = mime
                runOnUiThread {
                    setBusy(false, "Audio listo · ${file.name} · ${formatBytes(file.length())}")
                    playButton.isEnabled = true
                    saveButton.isEnabled = true
                    shareButton.isEnabled = true
                }
            }.onFailure { error ->
                runOnUiThread {
                    setBusy(false, "No se pudo generar: ${error.message}")
                    toast("Error al generar el audio")
                }
            }
        }.start()
    }

    private fun getCloneTts(): OfflineTts {
        cloneTts?.let { return it }
        narratorTts?.release()
        narratorTts = null
        val base = "pocket"
        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                pocket = OfflineTtsPocketModelConfig(
                    lmFlow = "$base/lm_flow.int8.onnx",
                    lmMain = "$base/lm_main.int8.onnx",
                    encoder = "$base/encoder.onnx",
                    decoder = "$base/decoder.int8.onnx",
                    textConditioner = "$base/text_conditioner.onnx",
                    vocabJson = "$base/vocab.json",
                    tokenScoresJson = "$base/token_scores.json",
                    voiceEmbeddingCacheCapacity = 10
                ),
                numThreads = 4,
                debug = false,
                provider = "cpu"
            )
        )
        return OfflineTts(assets, config).also { cloneTts = it }
    }

    private fun getNarratorTts(): OfflineTts {
        narratorTts?.let { return it }
        cloneTts?.release()
        cloneTts = null
        val base = "supertonic"
        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                supertonic = OfflineTtsSupertonicModelConfig(
                    durationPredictor = "$base/duration_predictor.int8.onnx",
                    textEncoder = "$base/text_encoder.int8.onnx",
                    vectorEstimator = "$base/vector_estimator.int8.onnx",
                    vocoder = "$base/vocoder.int8.onnx",
                    ttsJson = "$base/tts.json",
                    unicodeIndexer = "$base/unicode_indexer.bin",
                    voiceStyle = "$base/voice.bin"
                ),
                numThreads = 4,
                debug = false,
                provider = "cpu"
            )
        )
        return OfflineTts(assets, config).also { narratorTts = it }
    }

    private fun playGenerated() {
        val file = generatedFile ?: return
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            start()
            setOnCompletionListener { player -> player.release(); mediaPlayer = null }
        }
    }

    private fun saveGenerated() {
        val file = generatedFile ?: return
        saveLauncher.launch(file.name)
    }

    private fun shareGenerated() {
        val file = generatedFile ?: return
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = generatedMime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Compartir audio"))
    }

    private fun setBusy(busy: Boolean, message: String) {
        progress.visibility = if (busy) View.VISIBLE else View.GONE
        generateButton.isEnabled = !busy
        recordButton.isEnabled = !busy
        importButton.isEnabled = !busy
        statusText.text = message
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1_048_576 -> String.format(Locale.US, "%.1f MB", bytes / 1_048_576f)
        else -> String.format(Locale.US, "%.0f KB", bytes / 1024f)
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    override fun onDestroy() {
        recording.set(false)
        runCatching { recorder?.stop() }
        recorder?.release()
        mediaPlayer?.release()
        cloneTts?.release()
        narratorTts?.release()
        super.onDestroy()
    }
}
