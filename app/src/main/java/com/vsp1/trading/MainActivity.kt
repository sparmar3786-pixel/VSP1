package com.vsp1.trading

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Bg = Color(0xFF080B12)
private val Panel = Color(0xFF111722)
private val Border = Color(0xFF202A3A)
private val Muted = Color(0xFF8D98A8)
private val Accent = Color(0xFF55E6A5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Vsp1App() }
    }
}

@Composable
fun Vsp1App() {
    var selected by remember { mutableIntStateOf(0) }
    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            Column(Modifier.fillMaxSize()) {
                Header()
                TabBar(selected) { selected = it }
                TerminalScreen(TerminalTabs.items[selected], selected)
            }
        }
    }
}

@Composable
private fun Header() {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("VSP1", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("NSE AI TERMINAL", color = Muted, fontSize = 11.sp)
        }
        AssistChip(onClick = {}, label = { Text(TerminalTabs.modeLabel, fontSize = 10.sp) })
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Muted)
    }
}

@Composable
private fun TabBar(selected: Int, onSelected: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TerminalTabs.items.forEachIndexed { index, item ->
            Button(onClick = { onSelected(index) }, shape = RoundedCornerShape(10.dp)) {
                Text("${item.icon}  ${item.title}", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun TerminalScreen(tab: TerminalTab, index: Int) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tab.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Screen ${index + 1}  •  PAPER / DEMO", color = Muted, fontSize = 12.sp)
            }
            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Accent)
        }
        if (index == 0 || index == 3 || index == 4 || index == 12 || index == 14) SignalCard()
        MetricGrid()
        DetailCard(tab.title)
        if (index == 14) AiLayers()
    }
}

@Composable
private fun SignalCard() {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), border = BorderStroke(1.dp, Border)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = Accent)
                Spacer(Modifier.width(8.dp))
                Text("AI SIGNAL", color = Muted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text("WAIT / NO TRADE", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Data-quality gate active • no live order placement", color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MetricGrid() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Metric("NIFTY 50", "24,850.20", Accent, Modifier.weight(1f))
        Metric("PCR", "0.92", Color.White, Modifier.weight(1f))
        Metric("ATM", "24,850", Color.White, Modifier.weight(1f))
    }
}

@Composable
private fun Metric(label: String, value: String, valueColor: Color, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Panel), border = BorderStroke(1.dp, Border)) {
        Column(Modifier.padding(12.dp)) {
            Text(label, color = Muted, fontSize = 10.sp)
            Spacer(Modifier.height(5.dp))
            Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun DetailCard(title: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), border = BorderStroke(1.dp, Border)) {
        Column(Modifier.padding(16.dp)) {
            Text("$title monitor", color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            listOf(
                "Data freshness" to "LIVE-SAFE",
                "Backend" to "Ready for API",
                "Risk mode" to "Paper only",
                "Signal confidence" to "Awaiting live feed"
            ).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(row.first, color = Muted, modifier = Modifier.weight(1f), fontSize = 12.sp)
                    Text(row.second, color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun AiLayers() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("6-LAYER AI PIPELINE", color = Color.White, fontWeight = FontWeight.Bold)
        listOf(
            "L1 DataGuard" to "Freshness / completeness",
            "L2 RegimeDetector" to "Trend / range / volatility",
            "L3 Ensemble" to "Strategy consensus",
            "L4 OnlineML" to "Adaptive scoring",
            "L5 RiskGuard" to "Risk and exposure checks",
            "L6 Supervisor" to "Final validation"
        ).forEachIndexed { i, pair ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel), border = BorderStroke(1.dp, Border)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("L${i + 1}", color = Accent, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
                    Column {
                        Text(pair.first, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(pair.second, color = Muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
