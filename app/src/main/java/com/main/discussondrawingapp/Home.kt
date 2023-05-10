package com.main.discussondrawingapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

import com.main.discussondrawingapp.databinding.ActivityHomeBinding
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.log

class Home : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private var storage = Firebase.storage
    private var storageRef = storage.reference
    private lateinit var drawingArrayList:ArrayList<ModelDrawing2>
    private lateinit var adapterDrawing: AdapterDrawing
    private lateinit var binding: ActivityHomeBinding
    private  var markers:Int=0
    companion object{
        private const val TAG = "Home Activity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)
        database = Firebase.database.reference
        binding.addImageBtn.setOnClickListener{
            showInputImageDialog()
        }
        val layoutManager = GridLayoutManager(this,2)
        binding.drawingsRv.layoutManager = layoutManager
    }
    private fun showInputImageDialog(){
        Log.d(TAG, "showInputImageDialog: fcdx")
        if(checkStoragePermission()){
            Log.d(TAG, "showInputImageDialog: fctrvfecddx")
            pickImageGallery()

        }else{
            Log.d(TAG, "showInputImageDialog: fctrgrfdvfecddx")

            requestStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

    }
    private fun pickImageGallery(){
        Log.d(TAG, "pickImageGallery: ")
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }
    @SuppressLint("MissingInflatedId")
    private val galleryActivityResultLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ){ result ->
        if(result.resultCode == Activity.RESULT_OK) {
            val view  = LayoutInflater.from(this).inflate(R.layout.dialog_drawing_name,null)
            val drawingNameEt = view.findViewById<EditText>(R.id.drawingNameEt)
            val drawingNameError = view.findViewById<TextView>(R.id.drawingNameError)
            val addDrawingBtn= view.findViewById<Button>(R.id.addDrawingBtn)
            val  builder = MaterialAlertDialogBuilder(this)
            builder.setView(view)
            val alertDialog = builder.create()
            alertDialog.show()
            addDrawingBtn.setOnClickListener{
                if(drawingNameEt.text.toString()==""){
                    drawingNameError.setText("Please enter the name")
                }else{
                    val selectedImageUri = result.data?.data
                    val currentDateTime = Calendar.getInstance().time
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val dateTimeString = dateFormat.format(currentDateTime)
                    var modelDrawing = ModelDrawing(drawingNameEt.text.toString(),dateTimeString,0)
                    var newKey = database.ref.push().key
                    database.child("Drawings").child(newKey.toString()).setValue(modelDrawing)
                    val fileRef = storageRef.child("Drawings/$newKey/")
                    if (selectedImageUri != null) {
                        fileRef.putFile(selectedImageUri).addOnSuccessListener {

                        }.addOnCanceledListener {
                        }.addOnCompleteListener{
                            loadDrawings()
                        }
                    }
                    alertDialog.dismiss()
                }
            }
        }
    }

    private fun loadDrawings() {
        drawingArrayList = ArrayList()
        database.child("Drawings").addListenerForSingleValueEvent(
            object:ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.value!=null){
                        var data =snapshot.value as Map<String,Map<String,Any>>
                        adapterDrawing = AdapterDrawing(this@Home,drawingArrayList,
                            object : RvListinerDrawing{
                                override fun onDrawingClick(modelDrawing: ModelDrawing2, position: Int) {
                                    val intent = Intent(this@Home,DrawingViewActivity::class.java)
                                    intent.putExtra("imageID","${modelDrawing.imageID}")
                                    startActivity(intent)
                                }

                            })
                        binding.drawingsRv.adapter = adapterDrawing
                        Log.d(TAG, "onDataChange:rvfecdsx $data ")
                        for(each in data){
                            Log.d(TAG, "onDataChange:rvgfvdcfecdsx $each ")
                            database.child("Markers").child(each.key.toString()).addListenerForSingleValueEvent(
                                object :ValueEventListener{
                                    override fun onDataChange(snapshot2: DataSnapshot) {
                                        if(snapshot2.value!=null){
                                            var data2 =snapshot2.value as Map<String,Map<String,Any>>
                                            markers = data2.size


                                        }else{
                                            markers = 0
                                        }
                                        val modelDrawing =ModelDrawing2(each.value.get("name").toString(),each.value.get("date").toString(),markers,each.key)
                                        drawingArrayList.add(modelDrawing)
                                        adapterDrawing.notifyItemInserted(drawingArrayList.size)

                                    }

                                    override fun onCancelled(error: DatabaseError) {

                                    }

                                }
                            )



                        }
                    }}

                override fun onCancelled(error: DatabaseError) {
                }

            }
        )

    }


    private fun checkStoragePermission():Boolean{
        return ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED
    }


    private var requestStoragePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ActivityResultCallback<Boolean> {isGranted ->

            if(isGranted){
                pickImageGallery()
            }else{
                Toast.makeText(this,"Permission Denied",Toast.LENGTH_SHORT).show()
            }

        }
    )
    override fun onResume() {
        super.onResume()
        loadDrawings()
    }


}