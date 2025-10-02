package ru.netology.nmedia.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.R
import ru.netology.nmedia.model.MarkerPoint

class MarkerAdapter(
    private val markers: List<MarkerPoint>,
    private val onItemClick: (MarkerPoint) -> Unit
) : RecyclerView.Adapter<MarkerAdapter.MarkerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarkerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_marker_point, parent, false)
        return MarkerViewHolder(view)
    }

    override fun onBindViewHolder(holder: MarkerViewHolder, position: Int) {
        val marker = markers[position]
        holder.pointNumber.text = (position + 1).toString()
        holder.pointName.text = marker.name

        holder.itemView.setOnClickListener {
            onItemClick(marker)
        }
    }

    override fun getItemCount(): Int = markers.size

    class MarkerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val pointNumber: TextView = view.findViewById(R.id.pointNumber)
        val pointName: TextView = view.findViewById(R.id.pointName)
    }
}
