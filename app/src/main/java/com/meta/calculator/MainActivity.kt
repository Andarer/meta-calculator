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
        setContent { CalculatorScreen() }
    }
}

@Composable
fun CalculatorScreen() {
    var expr by remember { mutableStateOf("") }
    var display by remember { mutableStateOf("0") }

    fun push(token: String) {
        // аккуратно: не даём начать с оператора (кроме "-")
        if (expr.isEmpty() && token in listOf("+", "×", "÷")) return
        // не даём два оператора подряд (кроме минуса после оператора как "отрицательное число" — упрощаем и режем)
        if (expr.isNotEmpty() && token in listOf("+", "-", "×", "÷")) {
            val last = expr.last().toString()
            if (last in listOf("+", "-", "×", "÷", ".")) {
                expr = expr.dropLast(1) + token
                display = expr
                return
            }
        }
        expr += token
        display = expr
    }

    fun clearAll() {
        expr = ""
        display = "0"
    }

    fun backspace() {
        if (expr.isNotEmpty()) {
            expr = expr.dropLast(1)
            display = if (expr.isEmpty()) "0" else expr
        }
    }

    fun evaluate() {
        val raw = expr
        if (raw.isBlank()) return
        try {
            val res = ExpressionParser.eval(raw)
            expr = stripTrailingZeros(res)
            display = expr
        } catch (_: Exception) {
            display = "Error"
        }
    }

    val buttons = listOf(
        listOf("C", "⌫", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "=")
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = display,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { label ->
                    val weight = when (label) {
                        "0" -> 2f
                        else -> 1f
                    }
                    Button(
                        onClick = {
                            when (label) {
                                "C" -> clearAll()
                                "⌫" -> backspace()
                                "=" -> evaluate()
                                else -> push(label)
                            }
                        },
                        modifier = Modifier.weight(weight).height(54.dp)
                    ) {
                        Text(label)
                    }
                }
            }
        }
    }
}

/**
 * Мини-парсер: + - × ÷ и десятичные числа.
 * Поддерживает приоритет операций: ×/÷ выше +/-
 */
object ExpressionParser {

    fun eval(input: String): Double {
        val s = input.replace("×", "*").replace("÷", "/").replace(" ", "")
        val tokens = tokenize(s)
        val rpn = toRpn(tokens)
        return evalRpn(rpn)
    }

    private fun tokenize(s: String): List<String> {
        val out = mutableListOf<String>()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c.isDigit() || c == '.') {
                val start = i
                i++
                while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                out.add(s.substring(start, i))
                continue
            }
            if (c in charArrayOf('+', '-', '*', '/')) {
                // упростим: унарный минус -> приклеиваем к числу, если возможно
                if (c == '-' && (out.isEmpty() || out.last() in listOf("+", "-", "*", "/"))) {
                    // ожидаем число после
                    var j = i + 1
                    if (j < s.length && (s[j].isDigit() || s[j] == '.')) {
                        val start = i
                        j++
                        while (j < s.length && (s[j].isDigit() || s[j] == '.')) j++
                        out.add(s.substring(start, j)) // типа "-12.3"
                        i = j
                        continue
                    }
                }
                out.add(c.toString())
                i++
                continue
            }
            throw IllegalArgumentException("Bad char: $c")
        }
        return out
    }

    private fun prec(op: String) = when (op) {
        "+", "-" -> 1
        "*", "/" -> 2
        else -> 0
    }

    private fun toRpn(tokens: List<String>): List<String> {
        val out = mutableListOf<String>()
        val stack = ArrayDeque<String>()
        for (t in tokens) {
            if (t.toDoubleOrNull() != null) {
                out.add(t)
            } else {
                while (stack.isNotEmpty() && prec(stack.last()) >= prec(t)) {
                    out.add(stack.removeLast())
                }
                stack.addLast(t)
            }
        }
        while (stack.isNotEmpty()) out.add(stack.removeLast())
        return out
    }

    private fun evalRpn(rpn: List<String>): Double {
        val st = ArrayDeque<Double>()
        for (t in rpn) {
            val num = t.toDoubleOrNull()
            if (num != null) {
                st.addLast(num)
            } else {
                val b = st.removeLast()
                val a = st.removeLast()
                val r = when (t) {
                    "+" -> a + b
                    "-" -> a - b
                    "*" -> a * b
                    "/" -> a / b
                    else -> throw IllegalArgumentException("Bad op: $t")
                }
                st.addLast(r)
            }
        }
        return st.last()
    }
}

fun stripTrailingZeros(x: Double): String {
    // аккуратно приводим 2.0 -> "2", 2.5000 -> "2.5"
    val s = x.toString()
    return if (s.contains('.')) s.trimEnd('0').trimEnd('.') else s
}
