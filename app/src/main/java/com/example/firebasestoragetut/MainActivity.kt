package com.example.firebasestoragetut

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.firebasestoragetut.databinding.ActivityMainBinding
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

const val IMAGE_PICK_REQUEST_CODE = 1001

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentFile: Uri? = null

    val imgRef = Firebase.storage.reference

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.ivImage.setOnClickListener {
            Intent(Intent.ACTION_GET_CONTENT).also{
                it.type = "image/*"
                startActivityForResult(it, IMAGE_PICK_REQUEST_CODE)
            }
        }

        binding.btnUploadImage.setOnClickListener { uploadImage("my image") }

        binding.btnDownloadImage.setOnClickListener { downloadFile("my image") }

        binding.btnDeleteImage.setOnClickListener { deleteImageFile("my image") }

        listFiles()

    }

    private fun listFiles() =  CoroutineScope(Dispatchers.IO).launch {
        try {
            val images = imgRef.child("images").listAll().await()
            val imgUrls = mutableListOf<String>()
            for (image in images.items){
                val url = image.downloadUrl.await()
                imgUrls.add(url.toString())
                withContext(Dispatchers.Main){
                    val imageAdapter= ImageAdapter(imgUrls)
                    binding.rvImages.apply{
                        adapter = imageAdapter
                        layoutManager = LinearLayoutManager(this@MainActivity)
                    }
                }
            }
        }catch (e: Exception){
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity, "Error ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun deleteImageFile(filename: String) = CoroutineScope(Dispatchers.IO).launch {
        try {
            imgRef.child("images/$filename").delete().await()
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity, "successfully deleted!", Toast.LENGTH_SHORT).show()
            }
        }catch (e: Exception){
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity, "Error ${e.message} occurred!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun downloadFile(filename: String) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val maxDownloadSize = 12L * 1024 * 1024
            val bytes = imgRef.child("images/$filename").getBytes(maxDownloadSize).await()
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            withContext(Dispatchers.Main){
                binding.ivImage.setImageBitmap(bmp)
            }
        }catch (e: Exception){
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity, "Error ${e.message} occurred!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadImage(filename: String) = CoroutineScope(Dispatchers.IO).launch {
        try {
            currentFile?.let{
                imgRef.child("images/$filename").putFile(it).await()
                withContext(Dispatchers.Main){
                    Toast.makeText(this@MainActivity, "successfully uploaded!", Toast.LENGTH_SHORT).show()
                }
            }

        }catch (e: Exception){
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity, "Error ${e.message} occurred!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_REQUEST_CODE){
            data?.data?.let {
                currentFile = it
                binding.ivImage.setImageURI(it)
            }
        }
    }


}