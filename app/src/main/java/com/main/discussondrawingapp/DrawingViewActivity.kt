package com.main.discussondrawingapp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.RectF
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.OnMatrixChangedListener
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

import com.main.discussondrawingapp.databinding.ActivityDrawingViewBinding


class DrawingViewActivity : AppCompatActivity() {

    private  lateinit var binding: ActivityDrawingViewBinding
    private lateinit var database: DatabaseReference
    private lateinit var imageID:String
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_drawing_view)
        imageID = intent.getStringExtra("imageID").toString()
        database = Firebase.database.reference
        val storageRef = Firebase.storage.reference.child("Drawings/${imageID}").downloadUrl.addOnSuccessListener {
            Glide.with(this@DrawingViewActivity)
                .load(it.toString())
                .placeholder(R.drawable.imageplaceholder)
                .into(binding.drawingIV)
        }
        loadMarker()
        val markers = mutableListOf<Marker>()

        binding.drawingIV.setOnDoubleTapListener(object:GestureDetector.OnDoubleTapListener{
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                return false
            }

            @SuppressLint("MissingInflatedId")
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val x = e.getX(0)
                val y = e.getY(0)
                val view  = LayoutInflater.from(this@DrawingViewActivity).inflate(R.layout.dialog_marker,null)
                val title = view.findViewById<EditText>(R.id.markerTitle)
                val description =view.findViewById<EditText>(R.id.markerDesc)
                val btn = view.findViewById<Button>(R.id.addMarkerBtn)
                val error = view.findViewById<TextView>(R.id.markerError)
                val  builder = MaterialAlertDialogBuilder(this@DrawingViewActivity)
                builder.setView(view)
                val alertDialog = builder.create()
                alertDialog.show()
                btn.setOnClickListener{
                    if(title.text.toString()==""||description.text.toString()==""){
                        error.setText("Please fill all the fields")
                    }else{
                        val marker = Marker(x, y, title.text.toString(), description.text.toString())
                        markers.add(marker)
                        var newKey = database.ref.push().key
                        database.child("Markers").child(imageID).child(newKey.toString()).setValue(marker)
                        alertDialog.dismiss()
                        loadMarker()
                    }
                }


                return true
            }

            override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                return false            }
        })



/*        drawing.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (event.pointerCount == 1) {
                        // Single touch, do nothing
                    } else if (event.pointerCount == 2) {
                        // Double tap, add a new marker at the location
                        val x = event.getX(0)
                        val y = event.getY(0)
                        val marker = Marker(x, y, "Title", "Details")
                        markers.add(marker)
                        addMarkerView(marker)
                        true // consume the event
                    }
                }
                else -> false
            }
        }*/
        binding.drawingIV.setOnScaleChangeListener { scaleFactor, focusX, focusY ->
            for (markerView in markersViews) {
                val layoutParams = markerView.layoutParams as FrameLayout.LayoutParams
                layoutParams.leftMargin = ((markerView.marker.x - binding.drawingIV.displayRect.left) * binding.drawingIV.scale + binding.drawingIV.scrollX - markerView.width/2).toInt()
                layoutParams.topMargin = ((markerView.marker.y - binding.drawingIV.displayRect.top) * binding.drawingIV.scale + binding.drawingIV.scrollY - markerView.height/2).toInt()
                markerView.layoutParams = layoutParams
            }
        }


        binding.drawingIV.setOnMatrixChangeListener(object: OnMatrixChangedListener {
            override fun onMatrixChanged(rectF: RectF?) {
                for (markerView in markersViews) {
                    val layoutParams = markerView.layoutParams as FrameLayout.LayoutParams
                    layoutParams.leftMargin = (markerView.marker.x * binding.drawingIV.scale + binding.drawingIV.scrollX - markerView.width/2).toInt()
                    layoutParams.topMargin = (markerView.marker.y * binding.drawingIV.scale + binding.drawingIV.scrollY - markerView.height/2).toInt()
                    markerView.layoutParams = layoutParams
                }
            }
        })




    }

    private fun loadMarker() {
        database.child("Markers").child(imageID).addListenerForSingleValueEvent(
            object:ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.value!=null){
                        var data =snapshot.value as Map<String,Map<String,Any>>
                        for(each in data){
                            val marker = Marker(each.value.get("x").toString().toFloat(),
                                each.value.get("y").toString().toFloat(),
                                each.value.get("title").toString(),each.value.get("details").toString())
                            addMarkerView(marker,binding.drawingIV)

                        }

                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })

    }


    private val markersViews = mutableListOf<MarkerView>()



    fun addMarkerView(marker: Marker, photoView: PhotoView) {
        val markerView = ImageView(this)
        markerView.setImageResource(R.drawable.marker)

        val scaleFactor = photoView.scale
        val layoutParams = FrameLayout.LayoutParams(100, 100)
        layoutParams.leftMargin = (marker.x * scaleFactor - 50).toInt()
        layoutParams.topMargin = (marker.y * scaleFactor - 50).toInt()
        markerView.layoutParams = layoutParams
        binding.drawingContainer.addView(markerView)

        markerView.setOnClickListener {
            var bsf:BottomSheetFragment = BottomSheetFragment(marker)
            bsf.show(getSupportFragmentManager(),bsf.tag )

        }
    }


    inner class MarkerView(context: Context, val marker: Marker) : androidx.appcompat.widget.AppCompatImageView(context) {
        init {
            setImageResource(R.drawable.marker)
            val layoutParams = FrameLayout.LayoutParams(100, 100)
            layoutParams.leftMargin = (marker.x * binding.drawingIV.scale + binding.drawingIV.scrollX - 50).toInt()
            layoutParams.topMargin = (marker.y * binding.drawingIV.scale + binding.drawingIV.scrollY - 50).toInt()
            this.layoutParams = layoutParams
        }
    }
}