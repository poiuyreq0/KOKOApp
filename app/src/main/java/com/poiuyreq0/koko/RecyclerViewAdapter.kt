package com.poiuyreq0.koko

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewAdapter(
    private val dataSet: MutableMap<String, Item>,
    private val pos: Int,
    private val itemClickListener: OnItemClickListener
    ) : RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val order: TextView
        val name: TextView
        val number: TextView
        val unit: TextView
        val map: AppCompatButton

        init {
            // Define click listener for the ViewHolder's View.
            order = view.findViewById(R.id.order)
            name = view.findViewById(R.id.name)
            number = view.findViewById(R.id.number)
            unit = view.findViewById(R.id.unit)
            map = view.findViewById(R.id.map)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.recycler_item, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        val dataSetList = dataSet.values.toList()
        val congestion = dataSetList[position].congestion

        viewHolder.order.text = (position + 1).toString() + "."
        viewHolder.name.text = dataSetList[position].name

        if (pos==0) {
            viewHolder.number.text = String.format("%.2f", (dataSetList[position].distance / 1000.0))
            viewHolder.unit.text = "km"

        } else if (pos==1) {
            val seconds = dataSetList[position].duration / 1000
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60

            viewHolder.number.text = hours.toString() + "h "
            viewHolder.unit.text = minutes.toString() + "m"

        } else {
            viewHolder.number.text = congestion.toString()
            viewHolder.unit.text = "%"
        }

        if (congestion > 90) {
            viewHolder.itemView.setBackgroundColor(ContextCompat.getColor(viewHolder.itemView.context, R.color.red))
        } else if (congestion > 60) {
            viewHolder.itemView.setBackgroundColor(ContextCompat.getColor(viewHolder.itemView.context, R.color.orange))
        } else if (congestion > 30) {
            viewHolder.itemView.setBackgroundColor(ContextCompat.getColor(viewHolder.itemView.context, R.color.yellow))
        } else {
            viewHolder.itemView.setBackgroundColor(ContextCompat.getColor(viewHolder.itemView.context, R.color.green))
        }

        viewHolder.map.setOnClickListener {
            val intent = Intent(viewHolder.itemView.context, MapActivity::class.java)

            intent.putExtra("name", dataSetList[position].name)

            viewHolder.itemView.context.startActivity(intent)
        }

        viewHolder.itemView.setOnClickListener {
            if (position != RecyclerView.NO_POSITION) {
                itemClickListener.onItemClick(position, dataSet)
            }
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size
}
