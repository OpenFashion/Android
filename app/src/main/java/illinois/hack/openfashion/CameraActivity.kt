package illinois.hack.openfashion

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import android.graphics.Bitmap
import android.R.attr.data
import android.media.Image
import android.os.Environment
import android.support.v4.app.NotificationCompat.getExtras
import android.widget.ImageView
import android.os.Environment.DIRECTORY_PICTURES
import android.support.constraint.ConstraintLayout
import android.support.v4.content.FileProvider
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.storage.UploadTask
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class CameraActivity : AppCompatActivity() {

    private val CAMERA_REQUEST_CODE: Int = 0

    private lateinit var imageView: ImageView
    private lateinit var button: Button
    private lateinit var layout: ConstraintLayout
    private lateinit var mStorage: StorageReference

    lateinit var mCurrentPhotoPath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        mStorage = FirebaseStorage.getInstance().reference

//        imageView = findViewById(R.id.imageView)
        layout = findViewById(R.id.cameraLayout)
        button = findViewById(R.id.camera_button)
        button.setOnClickListener {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    // supposed to show reasoning here but why should i
                }
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)

            } else {
                goToCamera()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

                    // permission was granted, yay!

                    goToCamera()

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                    Snackbar.make(layout, "BOOOOOOO", Snackbar.LENGTH_LONG).show()
                }
                return
            }

            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    private fun goToCamera() {
        var intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//        startActivityForResult(intent, CAMERA_REQUEST_CODE)
        // Ensure that there's a camera activity to handle the intent
        if (intent.resolveActivity(packageManager) != null) {
            // Create the File where the photo should go
//            latinit File photoFile;
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (e: IOException) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null ) {
                var photoURI: Uri = FileProvider.getUriForFile(this, "illinois.hack.openfashion.fileprovider", photoFile)
//                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(intent, CAMERA_REQUEST_CODE)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
                imageFileName, /* prefix */
                ".png", /* suffix */
                storageDir      /* directory */
        )

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.absolutePath
        return image
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
//            var uri: Uri = data.data!!
//
//            var filepath: StorageReference = mStorage.child("Photos").child(uri.lastPathSegment)
//            filepath.putFile(uri).addOnSuccessListener {
//                Snackbar.make(layout, "Uploading finished", Snackbar.LENGTH_LONG).show()
//            }
            val extras = data.extras
            var image: Bitmap = extras.get("data") as Bitmap

            val bArray = ByteArrayOutputStream()
            image.compress( Bitmap.CompressFormat.PNG, 100, bArray )

            var byte = bArray.toByteArray()

            var reference = mStorage.child( "image.png" )

            var uploadTask = reference.putBytes( byte )
            uploadTask.addOnFailureListener({ println( "upload failed" ) })
                    .addOnSuccessListener { OnSuccessListener<UploadTask.TaskSnapshot> { println( "upload succeeded" ) } }

        }
    }
}