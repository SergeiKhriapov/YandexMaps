package ru.netology.nmedia.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.MarkerAdapter
import ru.netology.nmedia.model.MarkerPoint
import ru.netology.nmedia.storage.MarkerStorage

class PointsListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var backToMapButton: Button

    interface OnMarkerSelectedListener {
        fun onMarkerSelected(marker: MarkerPoint)
    }

    private val listener: OnMarkerSelectedListener?
        get() = activity as? OnMarkerSelectedListener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_points_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.pointsRecyclerView)
        backToMapButton = view.findViewById(R.id.backToMapButton)

        recyclerView.layoutManager = LinearLayoutManager(context)

        val markers = MarkerStorage.loadMarkers(requireContext())
        val adapter = MarkerAdapter(markers) { marker ->
            listener?.onMarkerSelected(marker)
        }
        recyclerView.adapter = adapter

        backToMapButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}
