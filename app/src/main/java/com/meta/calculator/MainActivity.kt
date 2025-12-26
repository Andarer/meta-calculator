package com.meta.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalculatorApp()
        }
    }
}

@Composable
fun CalculatorApp() {
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextField(value = a, onValueChange = { a = it }, label = { Text("A") })
        TextField(value = b, onValueChange = { b = it }, label = { Text("B") })

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { result = calc(a, b) { x, y -> x + y } }) { Text("+") }
            Button(onClick = { result = calc(a, b) { x, y -> x - y } }) { Text("-") }
            Button(onClick = { result = calc(a, b) { x, y -> x * y } }) { Text("×") }
            Button(onClick = {
                result = if (b == "0") "∞"
                else calc(a, b) { x, y -> x / y }
            }) { Text("÷") }
        }

        Text(text = "Result: $result", style = MaterialTheme.typography.headlineMedium)
    }
}

fun calc(a: String, b: String, op: (Double, Double) -> Double): String =
    try {
        op(a.toDouble(), b.toDouble()).toString()
    } catch (e: Exception) {
        "?"
    }
