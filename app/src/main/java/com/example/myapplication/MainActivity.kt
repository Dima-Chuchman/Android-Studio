package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.databinding.ActivityMainBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var calculationJob: Job? = null

    private val ceh = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Глобальне перехоплення крашу: ${throwable.message}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBlock1CooperativeCancellation()
        setupBlock2And3SupervisorIsolation()
        setupBlock4GlobalHandler()
        setupBonusNestedTrap()
    }

    private fun setupBlock1CooperativeCancellation() {
        binding.btnStart.setOnClickListener {
            calculationJob?.cancel()

            calculationJob = lifecycleScope.launch(Dispatchers.Default) {
                try {
                    for (i in 1..100) {
                        Thread.sleep(50)
                        ensureActive()
                        Log.d(TAG, "Обробка: $i%")
                    }
                    Log.d(TAG, "Роботу успішно завершено!")
                } catch (e: CancellationException) {
                    Log.w(TAG, "Обробку скасовано користувачем!")
                    throw e
                }
            }
        }

        binding.btnCancel.setOnClickListener {
            calculationJob?.cancel()
        }
    }

    private fun setupBlock2And3SupervisorIsolation() {
        binding.btnTestCrash.setOnClickListener {
            lifecycleScope.launch {
                supervisorScope {
                    launch {
                        try {
                            delay(1000)
                            throw RuntimeException("Банер не завантажився!")
                        } catch (e: Exception) {
                            Log.e(TAG, "Помилка завантаження банера: ${e.message}")
                        }
                    }

                    launch {
                        delay(2000)
                        Log.d(TAG, "Стрічка новин завантажена")
                    }
                }
            }
        }
    }

    private fun setupBlock4GlobalHandler() {
        binding.btnGlobalCrash.setOnClickListener {
            val safeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main + ceh)

            safeScope.launch {
                delay(500)
                throw IllegalArgumentException("Ой!")
            }
        }
    }

    private fun setupBonusNestedTrap() {
        binding.btnNestedTrap.setOnClickListener {
            lifecycleScope.launch {
                launch(ceh) {
                    throw RuntimeException("Вкладений краш!")
                }
            }
        }
    }

    companion object {
        private const val TAG = "CoroutinesLab"
    }
}