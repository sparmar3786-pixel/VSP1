package com.sparmar.nsealgo

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private val prefs by lazy { getSharedPreferences("nse_native", Context.MODE_PRIVATE) }
    private val http = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()
    private var socket: WebSocket? = null
    private lateinit var status: TextView
    private lateinit var backend: EditText
    private lateinit var clientId: EditText
    private lateinit var pin: EditText
    private lateinit var totp: EditText
    private lateinit var apiKey: EditText
    private lateinit var snapshot: TextView
    private lateinit var indexSelector: Spinner
    private lateinit var nseStatus: TextView
    private val indexSymbols = mutableListOf<String>()
    private val indexNames = mutableListOf<String>()
    private var selectedExchange = "NSE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        health()
        loadIndexes()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 20)
            setBackgroundColor(Color.rgb(9, 15, 29))
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(label("NSE ALGO • NATIVE LIVE TERMINAL", 22))
        content.addView(label("Kotlin Android • read-only • no order placement", 13))
        status = label("Backend: checking...", 15)
        content.addView(status)

        backend = field("Backend URL", prefs.getString("backend", "https://nse-algo-backend-live-production.up.railway.app") ?: "")
        content.addView(backend)

        content.addView(label("ANGEL ONE SMARTAPI", 18))
        clientId = field("Client ID", prefs.getString("clientId", "") ?: "")
        pin = field("MPIN", "", true)
        totp = field("Current 6-digit TOTP", "", false, InputType.TYPE_CLASS_NUMBER)
        apiKey = field("SmartAPI API Key", "", true)
        content.addView(clientId); content.addView(pin); content.addView(totp); content.addView(apiKey)

        content.addView(Button(this).apply { text = "CONNECT ANGEL ONE LIVE"; setOnClickListener { saveAndConnect() } })
        content.addView(Button(this).apply { text = "REFRESH STATUS"; setOnClickListener { health() } })
        content.addView(Button(this).apply { text = "AI VALIDATE SIGNAL"; setOnClickListener { validateAi() } })
        content.addView(label("INDEX DATA", 18))
        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tabs.addView(Button(this).apply { text = "NSE / BSE"; setOnClickListener { selectedExchange = "NSE"; loadIndexes() } }, LinearLayout.LayoutParams(0, -2, 1f))
        tabs.addView(Button(this).apply { text = "MCX"; setOnClickListener { selectedExchange = "MCX"; loadIndexes() } }, LinearLayout.LayoutParams(0, -2, 1f))
        content.addView(tabs)
        indexSelector = Spinner(this)
        content.addView(indexSelector)
        nseStatus = label("NSE data: checking...", 13)
        content.addView(nseStatus)
        indexSelector.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (position in indexSymbols.indices) fetchDashboardSnapshot(indexSymbols[position])
            }
        }
        snapshot = label("Waiting for live terminal snapshot...", 13)
        content.addView(snapshot)
        content.addView(label("Security: Angel credentials are sent only to your configured HTTPS backend. The APK never calls Angel directly. API orders are not implemented.", 12))

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun label(text: String, size: Int): TextView = TextView(this).apply {
        this.text = text
        textSize = size.toFloat()
        setTextColor(Color.WHITE)
        setPadding(0, 10, 0, 10)
    }

    private fun field(hint: String, value: String, password: Boolean = false, type: Int = InputType.TYPE_CLASS_TEXT): EditText = EditText(this).apply {
        this.hint = hint
        setText(value)
        setTextColor(Color.WHITE)
        setHintTextColor(Color.LTGRAY)
        inputType = if (password) type or InputType.TYPE_TEXT_VARIATION_PASSWORD else type
        setPadding(12, 10, 12, 10)
    }

    private fun baseUrl(): String = backend.text.toString().trim().trimEnd('/')

    private fun headers(): Headers = Headers.Builder().build()

    private fun saveAndConnect() {
        prefs.edit().putString("backend", baseUrl()).putString("clientId", clientId.text.toString().trim()).apply()
        val body = JSONObject().put("clientId", clientId.text.toString().trim()).put("pin", pin.text.toString().trim()).put("totp", totp.text.toString().trim()).put("apiKey", apiKey.text.toString().trim()).toString()
        status.text = "Backend: connecting to Angel One..."
        val req = Request.Builder().url(baseUrl() + "/v1/angel/login").headers(headers()).post(body.toRequestBody("application/json".toMediaType())).build()
        http.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, ex: java.io.IOException) { runOnUiThread { status.text = "Backend: connection failed • " + (ex.message ?: "network error") } }
            override fun onResponse(call: Call, response: Response) {
                val raw = response.body?.string().orEmpty()
                runOnUiThread {
                    val json = runCatching { JSONObject(raw) }.getOrNull()
                    if (response.isSuccessful) {
                        status.text = "Backend: CONNECTED • Angel One: CONNECTED"
                        openSocket()
                    } else {
                        val detail = json?.optJSONObject("detail")
                        val msg = detail?.optString("message") ?: json?.optString("detail") ?: ("HTTP " + response.code)
                        status.text = "Angel: REJECTED • " + msg
                    }
                }
            }
        })
    }

    private fun validateAi() {
        val h = headers()
        val get = Request.Builder().url(baseUrl() + "/v1/terminal").headers(h).get().build()
        status.text = "AI: validating supplied market evidence..."
        http.newCall(get).enqueue(object : Callback {
            override fun onFailure(call: Call, ex: java.io.IOException) { runOnUiThread { status.text = "AI: unavailable • WAIT safe state" } }
            override fun onResponse(call: Call, response: Response) {
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) { runOnUiThread { status.text = "AI: terminal evidence unavailable • WAIT" }; return }
                val body = JSONObject().put("payload", JSONObject(raw)).toString()
                val req = Request.Builder().url(baseUrl() + "/v1/ai/validate").headers(h).post(body.toRequestBody("application/json".toMediaType())).build()
                http.newCall(req).enqueue(object : Callback {
                    override fun onFailure(call: Call, ex: java.io.IOException) { runOnUiThread { status.text = "AI: unavailable • WAIT safe state" } }
                    override fun onResponse(call: Call, aiResponse: Response) {
                        val aiRaw = aiResponse.body?.string().orEmpty()
                        val j = runCatching { JSONObject(aiRaw) }.getOrNull()
                        runOnUiThread {
                            val finalState = j?.optString("final", "WAIT") ?: "WAIT"
                            val verified = j?.optBoolean("cross_verified", false) == true
                            status.text = "AI: " + finalState + if (verified) " • cross-verified" else " • local/safe fallback"
                        }
                    }
                })
            }
        })
    }

    private fun loadIndexes() {
        val req = Request.Builder().url(baseUrl() + "/api/indexes").headers(headers()).get().build()
        http.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, ex: java.io.IOException) { runOnUiThread { nseStatus.text = "Index catalog unavailable • " + (ex.message ?: "network error") } }
            override fun onResponse(call: Call, response: Response) {
                val raw = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(raw) }.getOrNull()
                val arr = json?.optJSONObject("exchanges")?.optJSONArray(selectedExchange)
                val symbols = mutableListOf<String>(); val names = mutableListOf<String>()
                if (arr != null) for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    symbols.add(item.optString("symbol"))
                    names.add(item.optString("name") + " • " + item.optString("exchange"))
                }
                runOnUiThread {
                    indexSymbols.clear(); indexSymbols.addAll(symbols)
                    indexNames.clear(); indexNames.addAll(names)
                    indexSelector.adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, indexNames)
                    if (indexSymbols.isNotEmpty()) fetchDashboardSnapshot(indexSymbols[0])
                }
            }
        })
    }

    private fun fetchDashboardSnapshot(symbol: String) {
        val encoded = java.net.URLEncoder.encode(symbol, "UTF-8")
        val req = Request.Builder().url(baseUrl() + "/api/snapshot?symbol=" + encoded).headers(headers()).get().build()
        http.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, ex: java.io.IOException) { runOnUiThread { nseStatus.text = "NSE data: unavailable • " + (ex.message ?: "network error") } }
            override fun onResponse(call: Call, response: Response) {
                val raw = response.body?.string().orEmpty()
                val j = runCatching { JSONObject(raw) }.getOrNull()
                runOnUiThread {
                    if (j == null || !response.isSuccessful) {
                        nseStatus.text = "NSE data: HTTP " + response.code
                        return@runOnUiThread
                    }
                    val source = j.optString("source_status", "UNAVAILABLE")
                    val symbolName = j.optString("symbol", symbol)
                    val spot = j.opt("underlying_ltp")
                    val age = j.opt("last_nse_fetch_age_sec")
                    nseStatus.text = when (source) {
                        "LIVE" -> "🟢 LIVE • NSE • " + symbolName + " • LTP=" + spot
                        "LAST_FETCH" -> "🟡 LAST NSE FETCH • " + symbolName + " • age=" + (age ?: "--") + "s • LTP=" + spot
                        "INDEX_CATALOG" -> "🟡 " + symbolName + " • separate MCX/BSE catalog; NSE fallback not applicable"
                        else -> "🔴 NSE DATA UNAVAILABLE • " + symbolName
                    }
                    snapshot.text = "Dashboard • " + symbolName + " • ATM=" + j.opt("atm") + " • PCR=" + j.opt("pcr") + " • Trend=" + j.optString("trend", "DATA UNAVAILABLE")
                }
            }
        })
    }

    private fun health() {
        val req = Request.Builder().url(baseUrl() + "/health").headers(headers()).get().build()
        http.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, ex: java.io.IOException) { runOnUiThread { status.text = "Backend: NOT CONNECTED • " + (ex.message ?: "network error") } }
            override fun onResponse(call: Call, response: Response) {
                val raw = response.body?.string().orEmpty()
                runOnUiThread {
                    if (!response.isSuccessful) status.text = "Backend: HTTP " + response.code
                    else {
                        val j = runCatching { JSONObject(raw) }.getOrNull()
                        val angel = j?.optBoolean("angel_connected", false) == true
                        status.text = if (angel) "Backend: CONNECTED • Angel One: CONNECTED" else "Backend: CONNECTED • Angel One: NOT CONNECTED"
                        if (angel) openSocket()
                    }
                }
            }
        })
    }

    private fun openSocket() {
        socket?.close(1000, "reconnect")
        val scheme = if (baseUrl().startsWith("https://")) "wss://" else "ws://"
        val host = baseUrl().removePrefix("https://").removePrefix("http://")
        val url = scheme + host + "/v1/ws"
        socket = http.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) { runOnUiThread { status.text = "Backend: CONNECTED • Market stream: LIVE" } }
            override fun onMessage(webSocket: WebSocket, text: String) {
                runOnUiThread {
                    val j = runCatching { JSONObject(text) }.getOrNull()
                    val conn = j?.optJSONObject("connection")
                    val angel = conn?.optBoolean("angel", false) == true
                    val spot = j?.optJSONObject("market")?.opt("spot") ?: "--"
                    val state = j?.optJSONObject("engine_state")?.optString("trend", "--") ?: "--"
                    snapshot.text = "LIVE • Angel=" + angel + " • NIFTY=" + spot + " • Trend=" + state
                }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                runOnUiThread { status.text = "Backend: connected • Market stream: reconnecting..." }
                webSocket.close(1000, null)
            }
        })
    }

    override fun onDestroy() {
        socket?.close(1000, "activity closed")
        http.dispatcher.executorService.shutdown()
        super.onDestroy()
    }
}
