package com.msc.distractionalert.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.msc.distractionalert.DistractionAlertApp
import com.msc.distractionalert.R
import com.msc.distractionalert.databinding.FragmentHistoryBinding
import com.msc.distractionalert.ui.main.AlertsViewModel
import com.msc.distractionalert.ui.main.MainActivity

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    // MainActivity owns this ViewModel; HistoryFragment and ChartFragment both
    // read the same instance so a single refresh updates both tabs.
    private val alertsViewModel: AlertsViewModel
        get() = (requireActivity() as MainActivity).alertsViewModel

    private val adapter by lazy {
        AlertAdapter((requireActivity().application as DistractionAlertApp).prefsManager)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.alertsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.alertsRecyclerView.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { alertsViewModel.refresh() }

        alertsViewModel.alerts.observe(viewLifecycleOwner) { alerts ->
            adapter.submitList(alerts)
            binding.emptyText.visibility = if (alerts.isEmpty()) View.VISIBLE else View.GONE
        }

        alertsViewModel.isRefreshing.observe(viewLifecycleOwner) { refreshing ->
            binding.swipeRefresh.isRefreshing = refreshing
        }

        alertsViewModel.refreshError.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(requireContext(), getString(R.string.refresh_failed, error), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
