package com.poiuyreq0.koko

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewAdapter(private val dataSet: MutableList<Item>, private val pos: Int, private val itemClickListener: OnItemClickListener) :
    RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {

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
        viewHolder.order.text = (position + 1).toString() + "."
        viewHolder.name.text = dataSet[position].name

        if (pos==0) {
            viewHolder.number.text = String.format("%.2f", (dataSet[position].value / 1000.0))
            viewHolder.unit.text = "km"
        } else {
            val seconds = dataSet[position].value / 1000
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60

            viewHolder.number.text = hours.toString() + "h "
            viewHolder.unit.text = minutes.toString() + "m"
        }

        viewHolder.map.setOnClickListener {
            val intent = Intent(viewHolder.itemView.context, MapActivity::class.java)

            intent.putExtra("name", dataSet[position].name)

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
