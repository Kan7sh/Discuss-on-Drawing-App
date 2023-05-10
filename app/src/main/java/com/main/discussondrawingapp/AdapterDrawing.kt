package com.main.discussondrawingapp

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage


class AdapterDrawing(private val context: Context,
                     private val drawingArrayList:ArrayList<ModelDrawing2>,
                        private val rvListinerDrawing: RvListinerDrawing):
    RecyclerView.Adapter<AdapterDrawing.HolderDrawing>() {




    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderDrawing {
        val view = LayoutInflater.from(context).inflate(R.layout.row_drawing,parent,false)
        return HolderDrawing(view)
    }

    override fun onBindViewHolder(holder: HolderDrawing, position: Int) {
        val modelDrawing2 = drawingArrayList[position]
        holder.drawingName.setText(modelDrawing2.name)
        holder.drawingTime.setText(modelDrawing2.date)
        holder.drawingMarkers.setText(modelDrawing2.markers.toString())
        val storageRef = Firebase.storage.reference.child("Drawings/${modelDrawing2.imageID}").downloadUrl.addOnSuccessListener {
            Glide.with(holder.itemView)
                .load(it.toString())
                .placeholder(R.drawable.imageplaceholder)
                .into(holder.drawingThumbnail)
        }

        holder.drawingCard.setOnClickListener{
            rvListinerDrawing.onDrawingClick(modelDrawing2,position)
        }


    }

    override fun getItemCount(): Int {
        return drawingArrayList.size
    }
    inner class HolderDrawing(itemView: View): RecyclerView.ViewHolder(itemView){
        var drawingName = itemView.findViewById<TextView>(R.id.drawingName)
        var drawingTime = itemView.findViewById<TextView>(R.id.drawingTime)
        var drawingMarkers = itemView.findViewById<TextView>(R.id.drawingMarkers)
        var drawingThumbnail = itemView.findViewById<ImageView>(R.id.drawingThumbnail)
        var drawingCard  = itemView.findViewById<CardView>(R.id.drawingCard)

    }
}