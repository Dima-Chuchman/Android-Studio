package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val tag = "CalculatorLog"

    private lateinit var etFirstNumber: EditText
    private lateinit var etSecondNumber: EditText
    private lateinit var tvResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etFirstNumber = findViewById(R.id.etFirstNumber)
        etSecondNumber = findViewById(R.id.etSecondNumber)
        tvResult = findViewById(R.id.tvResult)

        val btnAdd: Button = findViewById(R.id.btnAdd)
        val btnSubtract: Button = findViewById(R.id.btnSubtract)
        val btnMultiply: Button = findViewById(R.id.btnMultiply)
        val btnDivide: Button = findViewById(R.id.btnDivide)
        val btnClear: Button = findViewById(R.id.btnClear)

        btnAdd.setOnClickListener { performOperation("+") }
        btnSubtract.setOnClickListener { performOperation("-") }
        btnMultiply.setOnClickListener { performOperation("*") }
        btnDivide.setOnClickListener { performOperation("/") }

        btnClear.setOnClickListener {
            etFirstNumber.text.clear()
            etSecondNumber.text.clear()
            tvResult.text = "Результат:"
            Log.d(tag, "Поля вводу очищено")
        }
    }

    private fun performOperation(operator: String) {
        val num1Str = etFirstNumber.text.toString()
        val num2Str = etSecondNumber.text.toString()

        if (num1Str.isEmpty() || num2Str.isEmpty()) {
            tvResult.text = "Будь ласка, введіть обидва числа"
            Log.w(tag, "Спроба обчислення з порожніми полями")
            return
        }

        val num1 = num1Str.toDouble()
        val num2 = num2Str.toDouble()
        var result = 0.0

        when (operator) {
            "+" -> result = num1 + num2
            "-" -> result = num1 - num2
            "*" -> result = num1 * num2
            "/" -> {
                if (num2 == 0.0) {
                    tvResult.text = "Помилка: ділення на нуль неможливе!"
                    Log.w(tag, "Спроба ділення на нуль (число 1: $num1)")
                    return
                }
                result = num1 / num2
            }
        }

        Log.d(tag, "Виконано операцію: $num1 $operator $num2 = $result")

        val formattedResult = if (result % 1.0 == 0.0) {
            result.toLong().toString()
        } else {
            result.toString()
        }
        tvResult.text = "Результат: $formattedResult"
    }
}