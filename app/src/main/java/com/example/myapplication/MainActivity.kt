package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityMainBinding
import java.math.BigDecimal
import java.math.RoundingMode

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var firstOperand: Double? = null
    private var pendingOperation: String? = null
    private var isNewInput: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val digitButtons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3, binding.btn4,
            binding.btn5, binding.btn6, binding.btn7, binding.btn8, binding.btn9
        )

        for (button in digitButtons) {
            button.setOnClickListener {
                onDigitClick(button.text.toString())
            }
        }

        binding.btnDot.setOnClickListener { onDotClick() }
        binding.btnBackspace.setOnClickListener { onBackspaceClick() }
        binding.btnClear.setOnClickListener { clearAll() }

        binding.btnAdd.setOnClickListener { onOperatorClick("+") }
        binding.btnSubtract.setOnClickListener { onOperatorClick("-") }
        binding.btnMultiply.setOnClickListener { onOperatorClick("*") }
        binding.btnDivide.setOnClickListener { onOperatorClick("/") }

        binding.btnEquals.setOnClickListener { onEqualsClick() }
    }

    private fun onDigitClick(digit: String) {
        val currentText = binding.tvDisplay.text.toString()

        if (currentText == "Помилка") {
            clearAll()
        }

        if (isNewInput) {
            binding.tvDisplay.text = digit
            isNewInput = false
        } else {
            if (currentText == "0") {
                binding.tvDisplay.text = digit
            } else {
                binding.tvDisplay.text = currentText + digit
            }
        }
    }

    private fun onDotClick() {
        val currentText = binding.tvDisplay.text.toString()

        if (currentText == "Помилка") {
            clearAll()
            binding.tvDisplay.text = "0."
            isNewInput = false
            return
        }

        if (isNewInput) {
            binding.tvDisplay.text = "0."
            isNewInput = false
        } else if (!currentText.contains(".")) {
            binding.tvDisplay.text = "$currentText."
        }
    }

    private fun onBackspaceClick() {
        val currentText = binding.tvDisplay.text.toString()

        if (currentText == "Помилка") {
            clearAll()
            return
        }

        if (!isNewInput) {
            if (currentText.length > 1) {
                val updated = currentText.dropLast(1)
                if (updated == "-") {
                    binding.tvDisplay.text = "0"
                    isNewInput = true
                } else {
                    binding.tvDisplay.text = updated
                }
            } else {
                binding.tvDisplay.text = "0"
                isNewInput = true
            }
        }
    }

    private fun onOperatorClick(operator: String) {
        val currentText = binding.tvDisplay.text.toString()
        if (currentText == "Помилка") return

        val currentValue = currentText.toDoubleOrNull() ?: return

        if (firstOperand != null && pendingOperation != null && !isNewInput) {
            val intermediateResult = calculate(firstOperand!!, currentValue, pendingOperation!!)
            if (intermediateResult == null) {
                showError()
                return
            }
            binding.tvDisplay.text = formatNumber(intermediateResult)
            firstOperand = intermediateResult
        } else {
            firstOperand = currentValue
        }

        pendingOperation = operator
        isNewInput = true
    }

    private fun onEqualsClick() {
        val currentText = binding.tvDisplay.text.toString()
        if (currentText == "Помилка") return
        if (firstOperand == null || pendingOperation == null) return

        val secondOperand = currentText.toDoubleOrNull() ?: return
        val result = calculate(firstOperand!!, secondOperand, pendingOperation!!)

        if (result == null) {
            showError()
            return
        }

        binding.tvDisplay.text = formatNumber(result)
        firstOperand = null
        pendingOperation = null
        isNewInput = true
    }

    private fun calculate(first: Double, second: Double, operator: String): Double? {
        return when (operator) {
            "+" -> first + second
            "-" -> first - second
            "*" -> first * second
            "/" -> if (second == 0.0) null else first / second
            else -> null
        }
    }

    private fun formatNumber(value: Double): String {
        if (value == 0.0) return "0"
        return BigDecimal.valueOf(value)
            .setScale(8, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    }

    private fun showError() {
        binding.tvDisplay.text = "Помилка"
        firstOperand = null
        pendingOperation = null
        isNewInput = true
    }

    private fun clearAll() {
        binding.tvDisplay.text = "0"
        firstOperand = null
        pendingOperation = null
        isNewInput = true
    }
}