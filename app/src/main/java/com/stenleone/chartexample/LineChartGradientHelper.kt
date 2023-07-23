package com.stenleone.chartexample

import android.content.Context
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.Legend.LegendForm
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class LineChartGradientHelper(private val context: Context) {

    private var lastDrawIndex = 1 // need to connect previous and current line

    private val positiveColor = ContextCompat.getColor(context, R.color.positive_color)
    private val positive50OpacityColor =
        ContextCompat.getColor(context, R.color.positive_color_opacity_50)
    private val negativeColor = ContextCompat.getColor(context, R.color.negative_color)
    private val transparentColor = ContextCompat.getColor(context, R.color.transparent)
    private val whiteColor = ContextCompat.getColor(context, R.color.white)

    fun setup(chart: LineChart, data: YearRevenueEntity?) {
        chart.apply {
            setupChartMainParameters()
            setupLabelX()
            setupLabelY()
            setData(data?.let { createLineData(it) })
            animateXY(10, 1000) // animate start draw value
            setupLegend() // must call after draw
        }
    }

    private fun LineChart.setupChartMainParameters() {
        setTouchEnabled(false)
        isDragEnabled = false
        setScaleEnabled(false)
        setPinchZoom(false)
        description = Description().apply { text = "" }
        xAxis.setDrawGridLines(false)
        axisLeft.setDrawGridLines(false)
        axisRight.setDrawGridLines(false)
    }

    private fun LineChart.setupLabelX() {
        val xAxisLabel = getMonthList()
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setValueFormatter { value, _ ->
                try {
                    xAxisLabel[abs(value.toDouble().toInt())]
                } catch (e: Exception) {
                    ""
                }
            }
            setDrawAxisLine(false)
            labelCount = 12
            granularity = 1f
        }
    }

    private fun LineChart.setupLabelY() {
        axisRight.apply {
            setDrawAxisLine(false)
            setValueFormatter { value, _ ->
                try {
                    "$${String.format("%.1f", value)}"
                } catch (e: Exception) {
                    "$value"
                }
            }
            isEnabled = true
        }
        axisRight.isEnabled = true
        axisLeft.isEnabled = false
    }

    private fun LineChart.setupLegend() {
        val l: Legend = legend
        l.isEnabled = false
        l.form = LegendForm.LINE
    }

    fun changeData(chart: LineChart, data: YearRevenueEntity?) {
        chart.data = data?.let { createLineData(it) }
        chart.animateXY(10, 1000)
    }

    private fun createLineData(data: YearRevenueEntity) = LineData(
        sliceDataToDataSet(data.values).map { createLineDataSet(it) }
    )

    private fun sliceDataToDataSet(data: Array<out Double>): Array<Array<Entry>> { // make positive and negative lines
        lastDrawIndex = 1
        val result = arrayListOf(arrayListOf<Entry>())
        var currentPlus = (data.firstOrNull() ?: 0.0).isPositive()
        var lastNumber: Double? = null

        data.forEachIndexed { index, item ->// add in current slice
            if (currentPlus && item >= 0 || !currentPlus && item < 0) {
                result.last().add(Entry(index.toFloat(), item.toFloat()))
            } else { // add in new slice

                result.add(arrayListOf<Entry>().apply {
                    lastNumber?.let { lastNumber ->// add last point previous slice as first point new slice
                        val midCenterPosition =
                            index - 1 + CenterDotCalculator(item, lastNumber).getMidPoint()
                                .toFloat()
                        result.last().add(Entry(midCenterPosition, 0.0f))
                        add(Entry(midCenterPosition, 0.0f))
                    }
                    add(Entry(index.toFloat(), item.toFloat()))
                })
                currentPlus = !currentPlus
            }

            lastNumber = item
        }

        return result.map { it.toTypedArray() }.toTypedArray()
    }

    private fun createLineDataSet(data: Array<Entry>) = LineDataSet(
        data.toList(), "Total Profit"
    ).apply {
        val isPositive = (data.getOrNull(1) ?: data.firstOrNull() ?: Entry(0f, 0f)).y >= 0

        lineWidth = 1.75f
        circleRadius = 1f
        circleHoleRadius = 1f
        color = if (isPositive) positiveColor else negativeColor
        this.setCircleColor(if (isPositive) positiveColor else negativeColor)
        fillAlpha = 110
        this.setDrawFilled(isPositive)
        this.setDrawValues(false)
        if (isPositive) {
            fillDrawable = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                arrayOf(positive50OpacityColor, whiteColor, transparentColor).toIntArray()
            )
        }
    }

    private fun Double.isPositive() = this >= 0

    private fun getMonthList() =
        context.resources.getStringArray(R.array.month_list).toCollection(ArrayList())

    private inner class CenterDotCalculator(
        private val startPoint: Double,
        private val endPoint: Double,
        private val step: Double = 1.0
    ) {

        fun getMidPoint(): Double {
            if (startPoint == 0.0)
                return step
            if (endPoint == 0.0) {
                return 0.0
            }
            val ac = findAC()
            val ad = findAD(ac)

            val cosCAD = findCosCAD(ac, ad)
            val ag = findAG(cosCAD)
            val bg = findCD(ag)

            return abs(1 - bg)
        }

        private fun findAC(): Double = abs(startPoint) + abs(endPoint)

        // Using the Pythagorean Theorem
        private fun findAD(ae: Double): Double = sqrt(ae.pow(2) + step.pow(2))

        // Using the Cosine Theorem
        private fun findCosCAD(af: Double, ae: Double) =
            ((af.pow(2) + ae.pow(2) - step.pow(2)) / (2 * af * ae))

        // Using cos property
        private fun findAG(cosEAF: Double) = abs(startPoint) / cosEAF

        // Using the Pythagorean Theorem
        private fun findCD(ad: Double) =
            sqrt(ad.pow(2) - startPoint.pow(2))
    }
}