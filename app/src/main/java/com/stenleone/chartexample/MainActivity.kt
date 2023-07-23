package com.stenleone.chartexample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.coroutines.*
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    companion object {

        private const val MIN_VALUE = -5000.0
        private const val MAX_VALUE = 5000.0
    }

    private lateinit var coroutineScope: CoroutineScope

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        coroutineScope = CoroutineScope(Dispatchers.Default)

        LineChartGradientHelper(this).apply {
            setup(findViewById(R.id.chart), createRandomDataForChart())

            coroutineScope.launch {
                while (true) {
                    delay(10_000)
                    withContext(Dispatchers.Main) {
                        changeData(findViewById(R.id.chart), createRandomDataForChart())
                    }
                }
            }
        }
    }

    private fun createRandomDataForChart(): YearRevenueEntity {
        val valuesArr = DoubleArray(12)
        var smallerValue = MAX_VALUE
        var biggerValue = MIN_VALUE
        repeat(12) {
            val randValue = Random.nextDouble(MIN_VALUE, MAX_VALUE)
            valuesArr[it] = randValue
            if (randValue > biggerValue) biggerValue = randValue
            if (randValue > smallerValue) smallerValue = randValue
        }

        return YearRevenueEntity(
            Random.nextInt(2010, 2030).toString(),
            biggerValue,
            smallerValue,
            0.0,
            valuesArr.toTypedArray(),
        )
    }

    override fun onDestroy() {
        coroutineScope.cancel()
        super.onDestroy()
    }
}