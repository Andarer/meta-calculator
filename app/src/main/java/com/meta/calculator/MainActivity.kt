package com.meta.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
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
    val history = remember { mutableStateListOf<String>() }
    val operators = setOf('+', '-', '×', '÷')

    fun push(token: String) {
        if (display == "Error") {
            expr = ""
            display = "0"
        }
        // аккуратно: не даём начать с оператора (кроме "-")
        if (expr.isEmpty() && token in listOf("+", "×", "÷")) return
        if (token == ".") {
            if (expr.isEmpty() || expr.last() in operators) {
                expr += "0."
                display = expr
                return
            }
            val currentNumber = expr.takeLastWhile { it.isDigit() || it == '.' }
            if (currentNumber.contains('.')) return
        }
        // не даём два оператора подряд (кроме минуса после оператора как "отрицательное число" — упрощаем и режем)
        if (expr.isNotEmpty() && token in listOf("+", "-", "×", "÷")) {
            val last = expr.last().toString()
            if (last in listOf("+", "-", "×", "÷", ".")) {
                if (last == "." && token == "-") {
                    return
                }
                if (token == "-" && last in listOf("+", "×", "÷")) {
                    expr += token
                } else {
                    expr = expr.dropLast(1) + token
                }
                display = expr
                return
            }
        }
        expr += token
        display = expr
    }

    fun toggleSign() {
        if (display == "Error") {
            expr = ""
            display = "0"
        }
        if (expr.isEmpty()) {
            expr = "-"
            display = expr
            return
        }
        val last = expr.last()
        if (last in operators || last == '.') {
            if (last in operators) {
                expr += "-"
                display = expr
            }
            return
        }
        var index = expr.length - 1
        while (index >= 0 && (expr[index].isDigit() || expr[index] == '.')) {
            index--
        }
        val numberStart = index + 1
        val hasUnaryMinus = index >= 0 &&
            expr[index] == '-' &&
            (index == 0 || expr[index - 1] in operators)
        expr = if (hasUnaryMinus) {
            expr.removeRange(index, index + 1)
        } else {
            expr.substring(0, numberStart) + "-" + expr.substring(numberStart)
        }
        display = expr
    }

    fun applyPercent() {
        if (display == "Error" || expr.isBlank()) return
        val last = expr.last()
        if (last in operators || last == '.') return
        var index = expr.length - 1
        while (index >= 0 && (expr[index].isDigit() || expr[index] == '.')) {
            index--
        }
        val numberStart = index + 1
        val hasUnaryMinus = index >= 0 &&
            expr[index] == '-' &&
            (index == 0 || expr[index - 1] in operators)
        val signedStart = if (hasUnaryMinus) index else numberStart
        val number = expr.substring(signedStart).toDoubleOrNull() ?: return
        val percent = stripTrailingZeros(number / 100)
        expr = expr.substring(0, signedStart) + percent
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
        val raw = expr.trimEnd { it in operators || it == '.' }
        if (raw.isBlank()) return
        try {
            val res = ExpressionParser.eval(raw)
            if (!res.isFinite()) {
                display = "Error"
                return
            }
            expr = stripTrailingZeros(res)
            display = expr
            val entry = "$raw = $display"
            history.add(0, entry)
            if (history.size > 5) {
                history.removeLast()
            }
        } catch (_: Exception) {
            display = "Error"
        }
    }

    val buttons = listOf(
        listOf("C", "⌫", "±", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "%", "=")
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            tonalElevation = 2.dp,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = if (expr.isBlank()) " " else expr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = display,
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
                if (history.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        history.take(3).forEach { item ->
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
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
                    val colors = when (label) {
                        "C", "⌫" -> ButtonDefaults.filledTonalButtonColors()
                        "+", "-", "×", "÷", "=" -> ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                        else -> ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            when (label) {
                                "C" -> clearAll()
                                "⌫" -> backspace()
                                "=" -> evaluate()
                                "±" -> toggleSign()
                                "%" -> applyPercent()
                                else -> push(label)
                            }
                        },
                        shape = MaterialTheme.shapes.large,
                        colors = colors,
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
                var dotCount = if (c == '.') 1 else 0
                i++
                while (i < s.length && (s[i].isDigit() || s[i] == '.')) {
                    if (s[i] == '.') dotCount++
                    if (dotCount > 1) {
                        throw IllegalArgumentException("Bad number format")
                    }
                    i++
                }
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
                        var dotCount = if (s[j] == '.') 1 else 0
                        j++
                        while (j < s.length && (s[j].isDigit() || s[j] == '.')) {
                            if (s[j] == '.') dotCount++
                            if (dotCount > 1) {
                                throw IllegalArgumentException("Bad number format")
                            }
                            j++
                        }
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
                if (st.size < 2) {
                    throw IllegalArgumentException("Bad expression")
                }
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
