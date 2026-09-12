package com.quickqr.app.ui.history

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.quickqr.app.data.AppDatabase
import com.quickqr.app.data.ScanEntity
import com.quickqr.app.databinding.FragmentHistoryBinding
import com.quickqr.app.ui.result.ResultActivity
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = HistoryAdapter { scan -> openResult(scan) }
        binding.recyclerHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHistory.adapter = adapter

        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    deleteScan(adapter.currentList[position])
                }
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerHistory)

        binding.btnClearAll.setOnClickListener { confirmClearAll() }

        val dao = AppDatabase.getInstance(requireContext()).scanDao()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dao.getAll().collect { list ->
                    adapter.submitList(list)
                    binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    binding.btnClearAll.isEnabled = list.isNotEmpty()
                }
            }
        }
    }

    private fun openResult(scan: ScanEntity) {
        val intent = Intent(requireContext(), ResultActivity::class.java).apply {
            putExtra(ResultActivity.EXTRA_CONTENT, scan.content)
            putExtra(ResultActivity.EXTRA_FORMAT, scan.format)
            putExtra(ResultActivity.EXTRA_SKIP_SAVE, true)
        }
        startActivity(intent)
    }

    private fun deleteScan(scan: ScanEntity) {
        val dao = AppDatabase.getInstance(requireContext()).scanDao()
        viewLifecycleOwner.lifecycleScope.launch { dao.delete(scan) }
    }

    private fun confirmClearAll() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Clear history")
            .setMessage("This will remove all scanned codes from your history.")
            .setPositiveButton("Clear") { _, _ ->
                val dao = AppDatabase.getInstance(requireContext()).scanDao()
                viewLifecycleOwner.lifecycleScope.launch { dao.deleteAll() }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
