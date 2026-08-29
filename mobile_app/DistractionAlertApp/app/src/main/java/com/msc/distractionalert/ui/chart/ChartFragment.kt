package com.msc.distractionalert.ui.chart

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.msc.distractionalert.R
import com.msc.distractionalert.data.model.Alert
import com.msc.distractionalert.databinding.FragmentChartBinding
import com.msc.distractionalert.ui.main.AlertsViewModel
import com.msc.distractionalert.ui.main.MainActivity
import com.msc.distractionalert.util.DistractionClasses

class ChartFragment : Fragment() {

    private var _binding: FragmentChartBinding? = null
    private val binding get() = _binding!!

    private val alertsViewModel: AlertsViewModel
        get() = (requireActivity() as MainActivity).alertsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpChartAppearance()

        alertsViewModel.alerts.observe(viewLifecycleOwner) { alerts ->
            binding.emptyChartText.visibility = if (alerts.isEmpty()) View.VISIBLE else View.GONE
            renderChart(alerts)
        }
    }

    private fun setUpChartAppearance() {
        with(binding.barChart) {
            description.isEnabled = false
            legend.isEnabled = false
            setFitBars(true)
            setScaleEnabled(false)
            setPinchZoom(false)

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            xAxis.labelRotationAngle = -45f

            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
        }
    }

    private fun renderChart(alerts: List<Alert>) {
        val countsByClass = DistractionClasses.ALL.associateWith { className ->
            alerts.count { it.className == className }
        }

        val entries = countsByClass.values.mapIndexed { index, count -> BarEntry(index.toFloat(), count.toFloat()) }
        val labels = DistractionClasses.ALL.map { DistractionClasses.displayName(it) }
        val colors = CHART_COLOR_RES_IDS.map { ContextCompat.getColor(requireContext(), it) }

        val dataSet = BarDataSet(entries, getString(R.string.chart_title)).apply {
            setColors(colors)
            valueTextSize = 10f
            valueTextColor = Color.DKGRAY
        }

        binding.barChart.apply {
            data = BarData(dataSet)
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        // Parallel to DistractionClasses.ALL, one color resource per class.
        private val CHART_COLOR_RES_IDS = listOf(
            R.color.chart_1, R.color.chart_2, R.color.chart_3, R.color.chart_4, R.color.chart_5,
            R.color.chart_6, R.color.chart_7, R.color.chart_8, R.color.chart_9, R.color.chart_10
        )
    }
}
