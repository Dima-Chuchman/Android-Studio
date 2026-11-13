package com.example.myapplication

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.myapplication.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: SearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etSearchQuery.doOnTextChanged { text, _, _, _ ->
            viewModel.onQueryChanged(text?.toString().orEmpty())
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.searchResults.collect { results ->
                        val query = binding.etSearchQuery.text.toString().trim()
                        binding.tvSearchResults.text = when {
                            query.length < 3 -> ""
                            results.isEmpty() -> "Нічого не знайдено"
                            else -> results.joinToString(separator = "\n")
                        }
                    }
                }
            }
        }
    }
}