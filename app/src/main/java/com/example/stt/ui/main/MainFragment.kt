package com.example.stt.ui.main

import android.app.Activity.RESULT_OK
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.SpeechRecognizer.createSpeechRecognizer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.example.stt.R
import kotlinx.android.synthetic.main.main_fragment.*
import kotlinx.android.synthetic.main.main_fragment.view.*
import org.jetbrains.anko.toast

class MainFragment : Fragment(), RecognitionListener {

    companion object {
        fun newInstance() = MainFragment()
        val SPEECH_REQUEST_CODE = 0
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var speechRecognizer: SpeechRecognizer
    var localesArray: ArrayList<String> = ArrayList()
    lateinit var adapter: ArrayAdapter<String>
    var selectedItemPosition: Int = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.main_fragment, container, false)
        view.button.setOnClickListener {
            var languageTag: String? = null
            if (view.spinner.selectedItemPosition != 0) {
                languageTag = view.spinner.selectedItem as String
            }
            if (view.toggleLocale.isChecked) languageTag = view.toggleLocale.text.toString()
            val offline = !view.toggleOnline.isChecked
            speechRecognizer.startListening(createRecognizerIntent(languageTag, offline))
            //displaySpeechRecognizer(createRecognizerIntent(languageTag, offline))
        }

        val isRecognitionAvailable = SpeechRecognizer.isRecognitionAvailable(context)
        Log.d("SpeechRecognizer", "isRecognitionAvailable: $isRecognitionAvailable")
        speechRecognizer = createSpeechRecognizer(context)
        speechRecognizer.setRecognitionListener(this)

        view.spinner.adapter = adapter
        selectedItemPosition = savedInstanceState?.getInt("position") ?: 0
        view.spinner.setSelection(selectedItemPosition)
        return view
    }

    fun createRecognizerIntent(languageTag: String?, offline: Boolean = true): Intent {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        if (offline) intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        if (languageTag != null) {
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
        }
        return intent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, localesArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        message.text = viewModel.spokenText.value
        val languageDetailsIntent = RecognizerIntent.getVoiceDetailsIntent(activity)
        languageDetailsIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        if (adapter.isEmpty)
            activity!!.sendOrderedBroadcast(languageDetailsIntent, null, LanguageDetailsReceiver(), null, AppCompatActivity.RESULT_OK, null, null)
    }

    private fun displaySpeechRecognizer(intent: Intent) {
        startActivityForResult(intent, SPEECH_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0)
            viewModel.spokenText.value = spokenText
            message.text = viewModel.spokenText.value
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        speechRecognizer.destroy()
    }

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d("SpeechRecognizer", "onReadyForSpeech: $params")
    }

    override fun onRmsChanged(rmsdB: Float) {
        Log.d("SpeechRecognizer", "onRmsChanged: $rmsdB")
    }

    override fun onBufferReceived(buffer: ByteArray?) {
        Log.d("SpeechRecognizer", "onBufferReceived: $buffer")
    }

    override fun onPartialResults(partialResults: Bundle?) {
        Log.d("SpeechRecognizer", "onPartialResults: $partialResults")
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        Log.d("SpeechRecognizer", "onEvent: $eventType")
    }

    override fun onBeginningOfSpeech() {
        Log.d("SpeechRecognizer", "onBeginningOfSpeech")
        view?.button?.text = getString(R.string.listening)
    }

    override fun onEndOfSpeech() {
        Log.d("SpeechRecognizer", "onEndOfSpeech")
    }

    override fun onError(error: Int) {
        Log.d("SpeechRecognizer", "onError: $error")
        context?.toast("Error: ${getResources().getStringArray(R.array.errors)[error]}")
        view?.button?.text = getString(R.string.start_listening)
    }

    override fun onResults(results: Bundle?) {
        view?.button?.text = getString(R.string.start_listening)
        val resultList = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        Log.d("SpeechRecognizer", "results list size: ${resultList?.size}")
        val confidenceScores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)

        var resultString = ""
        if (resultList != null) {
            for (i in 0..resultList.size - 1) {
                val spokenText = resultList.get(i)
                val confidenceScore = confidenceScores?.get(i)
                resultString += "$spokenText\nconfidence score: $confidenceScore\n\n"
            }
        }
        viewModel.spokenText.value = resultString
        message.text = viewModel.spokenText.value
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("position", spinner.selectedItemPosition)
    }

    inner class LanguageDetailsReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val resultCode = resultCode
            Log.d(this.javaClass.simpleName, "resultCode: $resultCode")
            val resultData = resultData
            Log.d(this.javaClass.simpleName, "resultData: $resultData")
            val results = getResultExtras(false)
            Log.d(this.javaClass.simpleName, "results: $results")
            val languagePreference = results?.getString(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE)
            Log.d(this.javaClass.simpleName, "languagePreference: $languagePreference")
            val supportedLanguages = results?.getStringArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)
            Log.d(this.javaClass.simpleName, "supportedLanguages: $supportedLanguages")
            adapter.clear()
            adapter.add("")
            adapter.addAll(supportedLanguages)
            view?.spinner?.setSelection(selectedItemPosition)
        }
    }
}
