package com.example.ma1

import android.content.AbstractThreadedSyncAdapter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Message
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.SimpleAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.wolfram.alpha.WAEngine
import com.wolfram.alpha.WAPlainText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.w3c.dom.Text
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {

    // EPLYPA-QAERQYQYY9

    lateinit var requestInput: TextInputEditText

    lateinit var podsAdapter: SimpleAdapter

    lateinit var progressBar: ProgressBar

    lateinit var waEngine: WAEngine

    val pods = mutableListOf<HashMap<String, String>>()

    lateinit var textToSpeech: TextToSpeech

    var isTtsReady: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("MyLog", "Start")

        initVews()
        initWalframEngine()

    }

    fun initVews() {
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        requestInput = findViewById(R.id.text_input_edit)
        requestInput.setOnEditorActionListener { v, actionId, event ->
            if(actionId == EditorInfo.IME_ACTION_DONE) {
            pods.clear()
                podsAdapter.notifyDataSetChanged()

                val question = requestInput.text.toString()
                askWolfram(question)
            }
            return@setOnEditorActionListener false
        }

        val podsList: ListView = findViewById(R.id.pod_list)
        podsAdapter = SimpleAdapter(
            applicationContext,
            pods,
            R.layout.item_pod,
            arrayOf("Title", "Content"),
            intArrayOf(R.id.title, R.id.content)
        )

        podsList.adapter = podsAdapter
        podsList.setOnItemClickListener { adapterView, view, position, id ->
            if(isTtsReady){
                val title = pods[position]["Title"]
                val content = pods[position]["Content"]
                textToSpeech.speak(content, TextToSpeech.QUEUE_FLUSH,null,title)
            }
        }

        val voiceInputButton: FloatingActionButton = findViewById(R.id.vioce_input_bottom)
        voiceInputButton.setOnClickListener {
            Log.d("MyLog", "FAB")
        }

        progressBar = findViewById(R.id.progress_bar)

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {     //для отображения меню в активити
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {  //для активации какого либо действия при нажитии иконок в меню
        when (item.itemId) {
            R.id.action_stop -> {
                if (isTtsReady){
                    textToSpeech.stop()
                }
                return true
            }
            R.id.action_clear -> {
                requestInput.text?.clear()
                pods.clear()
                podsAdapter.notifyDataSetChanged()
                return true
            }

        }
        return super.onOptionsItemSelected(item)
    }

    fun initWalframEngine() {
        waEngine = WAEngine().apply {
            appID = "EPLYPA-QAERQYQYY9"
            addFormat("plaintext")
        }
    }

    fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_INDEFINITE)
            .apply {
                setAction(android.R.string.ok) {
                    dismiss()
                }
                show()
            }

    }

    fun askWolfram(request: String) {
        progressBar.visibility = View.VISIBLE
        val launch = CoroutineScope(Dispatchers.IO).launch {
            var query = waEngine.createQuery().apply { input = request }
            runCatching {
                waEngine.performQuery(query)
            }.onSuccess { result ->
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (result.isError) {
                        showSnackbar(result.errorMessage)
                        return@withContext
                    }

                    if (!result.isSuccess) {
                        requestInput.error = getString(R.string.error_do_not_understand)
                        return@withContext
                    }

                    for (pod in result.pods) {
                        if (pod.isError) continue
                            val content = StringBuilder()
                            for (subpod in pod.subpods) {
                                for (element in subpod.contents) {

                                    if (element is WAPlainText) {
                                        content.append(element.text)
                                    }
                                }
                            }
                            pods.add(0, HashMap<String, String>().apply {
                                put("Title", pod.title)
                                put("Content", content.toString())
                            })
                        }
                        podsAdapter.notifyDataSetChanged()
                    }

                }.onFailure { t ->
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        showSnackbar(t.message ?: getString(R.string.error_something_went_wrong))
                    }
                }
            }
        }

    fun initTts(){
        textToSpeech = TextToSpeech(this){ code->
            if (code != TextToSpeech.SUCCESS){
                Log.e("MyLog","TTS error code: $code")
                showSnackbar(getString(R.string.error_tts_is_not_ready))
            }else{
                isTtsReady = true
            }
        }
        textToSpeech.language = Locale.US
    }

   }
