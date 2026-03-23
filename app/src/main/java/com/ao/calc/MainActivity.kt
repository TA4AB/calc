package com.ao.calc

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

class MainActivity : AppCompatActivity() {

    private lateinit var tvExpression: TextView
    private lateinit var tvResult: TextView

    private var expression = ""
    private var justCalculated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvExpression = findViewById(R.id.tvExpression)
        tvResult = findViewById(R.id.tvResult)

        val buttonIds = mapOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2",
            R.id.btn3 to "3", R.id.btn4 to "4", R.id.btn5 to "5",
            R.id.btn6 to "6", R.id.btn7 to "7", R.id.btn8 to "8",
            R.id.btn9 to "9", R.id.btnDot to ".", R.id.btnPlus to "+",
            R.id.btnMinus to "-", R.id.btnMultiply to "×", R.id.btnDivide to "÷",
            R.id.btnPercent to "%"
        )

        buttonIds.forEach { (id, value) ->
            findViewById<Button>(id).setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onInput(value)
            }
        }

        findViewById<Button>(R.id.btnClear).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            clear()
        }

        findViewById<Button>(R.id.btnBackspace).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            backspace()
        }

        findViewById<Button>(R.id.btnEquals).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            calculate()
        }

        findViewById<Button>(R.id.btnPlusMinus).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            toggleSign()
        }

        findViewById<TextView>(R.id.tvAboutIcon).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            showAboutDialog()
        }
    }

    private fun showAboutDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_about)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        dialog.findViewById<TextView>(R.id.tvAboutVersion).text = "Sürüm $versionName"
        dialog.findViewById<Button>(R.id.btnAboutClose).setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun onInput(value: String) {
        val operators = setOf("+", "-", "×", "÷")

        if (justCalculated && value !in operators) {
            expression = ""
        }
        justCalculated = false

        if (value in operators) {
            if (expression.isEmpty()) {
                if (value == "-") expression = "-"
                updateDisplay()
                return
            }
            val last = expression.last().toString()
            if (last in operators) {
                expression = expression.dropLast(1) + value
            } else {
                expression += value
            }
            updateDisplay()
            return
        }

        if (value == ".") {
            val lastNumber = expression.split("+", "-", "×", "÷").last()
            if ("." in lastNumber) return
        }

        if (value == "%") {
            if (expression.isEmpty()) return
            val last = expression.last().toString()
            if (last in operators || last == "%") return
            expression += value
            updateDisplay()
            return
        }

        expression += value
        updateDisplay()
        showLiveResult()
    }

    private fun backspace() {
        if (justCalculated) { clear(); return }
        if (expression.isNotEmpty()) {
            expression = expression.dropLast(1)
            updateDisplay()
            showLiveResult()
        }
    }

    private fun clear() {
        expression = ""
        justCalculated = false
        tvExpression.text = "0"
        tvResult.text = ""
    }

    private fun toggleSign() {
        if (expression.isEmpty()) return
        val parts = splitExpression()
        if (parts.isEmpty()) return
        val lastNum = parts.last()
        if (lastNum.isEmpty()) return
        val prefix = expression.dropLast(lastNum.length)
        expression = if (lastNum.startsWith("-")) {
            prefix + lastNum.drop(1)
        } else {
            prefix + "-" + lastNum
        }
        updateDisplay()
        showLiveResult()
    }

    private fun splitExpression(): List<String> {
        val result = mutableListOf<String>()
        var current = ""
        for (i in expression.indices) {
            val c = expression[i].toString()
            if (c in setOf("+", "×", "÷") || (c == "-" && i > 0 && expression[i - 1].toString() !in setOf("+", "×", "÷"))) {
                result.add(current)
                current = ""
            } else {
                current += c
            }
        }
        result.add(current)
        return result
    }

    private fun calculate() {
        if (expression.isEmpty()) return
        val result = evaluate(expression) ?: return
        tvExpression.text = expression
        tvResult.text = formatNumber(result)
        expression = formatNumber(result)
        justCalculated = true
    }

    private fun showLiveResult() {
        val result = evaluate(expression)
        tvResult.text = if (result != null) formatNumber(result) else ""
    }

    private fun updateDisplay() {
        tvExpression.text = if (expression.isEmpty()) "0" else expression
    }

    private fun evaluate(expr: String): BigDecimal? {
        return try {
            val normalized = expr.replace("×", "*").replace("÷", "/").replace("%", "/100")
            parseExpression(normalized)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseExpression(expr: String): BigDecimal {
        var i = 0
        val tokens = mutableListOf<Any>()

        while (i < expr.length) {
            val c = expr[i]
            if (c == '-' && (i == 0 || expr[i - 1] in listOf('+', '-', '*', '/'))) {
                var num = "-"
                i++
                while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) { num += expr[i]; i++ }
                tokens.add(BigDecimal(num))
            } else if (c.isDigit() || c == '.') {
                var num = ""
                while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) { num += expr[i]; i++ }
                tokens.add(BigDecimal(num))
            } else if (c in listOf('+', '-', '*', '/')) {
                tokens.add(c); i++
            } else i++
        }

        var j = 1
        while (j < tokens.size) {
            val op = tokens[j]
            if (op == '*' || op == '/') {
                val a = tokens[j - 1] as BigDecimal
                val b = tokens[j + 1] as BigDecimal
                val res = if (op == '*') a.multiply(b) else a.divide(b, MathContext(15, RoundingMode.HALF_UP))
                tokens[j - 1] = res
                tokens.removeAt(j)
                tokens.removeAt(j)
            } else j += 2
        }

        var result = tokens[0] as BigDecimal
        var k = 1
        while (k < tokens.size) {
            val op = tokens[k]
            val b = tokens[k + 1] as BigDecimal
            result = if (op == '+') result.add(b) else result.subtract(b)
            k += 2
        }
        return result
    }

    private fun formatNumber(value: BigDecimal): String {
        val stripped = value.stripTrailingZeros()
        return if (stripped.scale() <= 0) {
            stripped.toBigInteger().toString()
        } else {
            stripped.toPlainString().let {
                if (it.length > 12) value.round(MathContext(10, RoundingMode.HALF_UP)).stripTrailingZeros().toPlainString() else it
            }
        }
    }
}
