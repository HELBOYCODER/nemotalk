package com.helboy.nemotalk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helboy.nemotalk.data.ChatDatabase
import com.helboy.nemotalk.sandbox.SandboxManager
import com.helboy.nemotalk.ui.theme.DarkBackground
import com.helboy.nemotalk.ui.theme.DarkBorder
import com.helboy.nemotalk.ui.theme.DarkSurface
import com.helboy.nemotalk.sandbox.SandboxManager
import com.helboy.nemotalk.sandbox.SandboxResult
import com.helboy.nemotalk.ui.theme.NvidiaGreen
import com.helboy.nemotalk.ui.theme.NvidiaNeon
import com.helboy.nemotalk.ui.theme.TextMuted
import com.helboy.nemotalk.ui.theme.TextPrimary
import com.helboy.nemotalk.ui.theme.TextSecondary

private const val QUICK_CMD = "uname -a && cat /etc/alpine-release && apk add -q curl >/dev/null 2>&1 && curl -s ifconfig.me"

/**
 * Ponytail debug panel for the embedded Linux sandbox and long-term memory.
 * Two boxes, one screen: run commands, see memory. No tabs, no navigation.
 */
@Composable
fun SandboxScreen(
    sandbox: SandboxManager,
    database: ChatDatabase,
    onBack: () -> Unit
) {
    var output by remember { mutableStateOf("") }
    var cmd by remember { mutableStateOf(QUICK_CMD) }
    var running by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("آماده") }

    var memory by remember { mutableStateOf(listOf<String>()) }
    var note by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        // Warm proot once so the first command isn't slow.
        sandbox.bootstrap()
        memory = database.allMemory()
    }

    fun runCommand() {
        if (cmd.isBlank() || running) return
        running = true
        status = "در حال اجرا…"
        Thread {
            val r = sandbox.execute(listOf(cmd), workdir = "/home/neon")
            output = r.stdout
            status = if (r.success) "انجام شد (${r.exitCode})" else "خطا (${r.exitCode})"
            running = false
        }.start()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "بازگشت", tint = TextPrimary)
            }
            Icon(Icons.Default.Code, null, tint = NvidiaNeon, modifier = Modifier.padding(end = 6.dp))
            Text(
                "سندباکس لینوکس داخلی",
                color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold
            )
        }

        Text(
            "محیط Alpine ایزوله — بدون روت، فقط درون خود اپ. کل سیستم دست‌نخورده می‌ماند.",
            color = TextMuted, fontSize = 11.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        // Command box
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = DarkSurface,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Code, null, tint = TextMuted, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("دستور", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.weight(1f))
                    Text(status, color = NvidiaNeon, fontSize = 11.sp)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = cmd,
                    onValueChange = { cmd = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    enabled = !running,
                    placeholder = { Text("apk add python3 …", color = TextMuted, fontSize = 12.sp) },
                    textStyle = TextDefaults.inputTextStyle(
                        color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 12.sp
                    )
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    Button(
                        onClick = ::runCommand,
                        enabled = !running,
                        colors = ButtonDefaults.buttonColors(containerColor = NvidiaGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (running) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp, modifier = Modifier.size(14.dp),
                                color = Color.Black
                            )
                        } else {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("اجرا", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { cmd = QUICK_CMD }, enabled = !running) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("بازنشانی", fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    color = Color(0xFF0A0E11),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = output.ifBlank { "// خروجی اینجا نمایش داده می‌شود\n// proot یک sandbox کامل Alpine را درون اپ بالا می‌آورد" },
                        color = NvidiaNeon.copy(alpha = 0.9f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Memory box
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkSurface,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Memory, null, tint = NvidiaNeon, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("حافظه بلندمدت", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("${memory.size} مورد", color = TextMuted, fontSize = 11.sp)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier.weight(1f),
                        enabled = true,
                        singleLine = true,
                        placeholder = { Text("یادآوری… مثلاً: کاربر فارسی صحبت می‌کند", color = TextMuted, fontSize = 12.sp) },
                        textStyle = TextDefaults.inputTextStyle(color = TextPrimary, fontSize = 13.sp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (note.isNotBlank()) {
                                database.insertMemory(note.trim(), "note")
                                memory = database.allMemory()
                                note = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NvidiaGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("ذخیره", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                }
                if (memory.isEmpty()) {
                    Text(
                        "هنوز چیزی به یادگار نمانده — حافظه با جستجوی FTS هر متن مرتبط را برمی‌گرداند.",
                        color = TextMuted, fontSize = 11.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(memory) { item ->
                            Text(
                                "• $item",
                                color = TextPrimary, fontSize = 12.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DarkBackground.copy(alpha = 0.4f))
                                    .padding(6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
