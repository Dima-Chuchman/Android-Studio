package com.example.myapplication

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: DiceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val diceImageViews = listOf(
            binding.dice1,
            binding.dice2,
            binding.dice3,
            binding.dice4,
            binding.dice5
        )

        viewModel.diceValues.observe(this) { values ->
            values.forEachIndexed { index, dieValue ->
                if (index < diceImageViews.size) {
                    diceImageViews[index].setImageResource(getDiceDrawableRes(dieValue))
                }
            }
        }

        viewModel.isRolling.observe(this) { isRolling ->
            binding.btnRoll.isEnabled = !isRolling
        }

        binding.btnRoll.setOnClickListener {
            viewModel.rollDice()
        }
    }

    private fun getDiceDrawableRes(value: Int): Int {
        return when (value) {
            1 -> R.drawable.dice_1
            2 -> R.drawable.dice_2
            3 -> R.drawable.dice_3
            4 -> R.drawable.dice_4
            5 -> R.drawable.dice_5
            else -> R.drawable.dice_6
        }
    }
}