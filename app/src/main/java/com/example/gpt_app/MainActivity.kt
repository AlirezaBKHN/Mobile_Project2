package com.example.gpt_app


import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*



class MainActivity : AppCompatActivity() {

    private val requestImageCapture  = 1
    private lateinit var currentPhotoPath: String
    private lateinit var imageView: ImageView
    private lateinit var storageReference: StorageReference
    private final var GALLERY_REQ_CODE=1000
    private lateinit var storageRef: StorageReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        val takePictureButton = findViewById<Button>(R.id.take_picture_button)
        val savePictureButton = findViewById<Button>(R.id.save_picture_button)

        // Initialize Firebase Storage
        storageReference = FirebaseStorage.getInstance().reference

        takePictureButton.setOnClickListener { view ->
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"  // Explicitly specify image selection
            startActivityForResult(intent, GALLERY_REQ_CODE)
        }

        savePictureButton.setOnClickListener {
            uploadImageToFirebase()
        }
    }

    private val takePictureResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as Bitmap
            // Now you can use the imageBitmap for further processing, display, or saving
            // For example:
            imageView.setImageBitmap(imageBitmap) // Display the image in an ImageView
        } else {
            // Handle the case when the user cancels or there's an error
            Toast.makeText(this, "Image capture canceled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            // Handle the exception (e.g., show an error message)
            null
        }

        photoFile?.let { file ->
            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "com.example.gpt_app.fileprovider",
                file
            )
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            takePictureResult.launch(takePictureIntent)
        }
    }




    @Throws(IOException::class)
    private fun createImageFile(): File {
        //val pattern = "EEEEE dd-MMMMMMM-yyyy"
        val timeStamp: String = Date().toString()
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    @Override
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == GALLERY_REQ_CODE) {
                imageView.setImageURI(data?.data) // Use safe calls for nullable data
            }
        }
    }



    private fun uploadImageToFirebase()  {
        storageRef = FirebaseStorage.getInstance().reference
        val bitmap = (imageView.drawable as BitmapDrawable).bitmap
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        // Create a reference to the image file in Firebase Storage
        val imageRef = storageRef.child("images/${UUID.randomUUID()}.jpg")

        // Upload the image
        val uploadTask = imageRef.putBytes(data)
        uploadTask.addOnSuccessListener {
            // Handle successful upload
            Toast.makeText(this, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
            // You can get the download URL using imageRef.downloadUrl
        }.addOnFailureListener {
            // Handle upload error
            Toast.makeText(this, "${it.message}", Toast.LENGTH_LONG).show()
        }
    }



}