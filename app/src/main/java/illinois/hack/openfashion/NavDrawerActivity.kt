package illinois.hack.openfashion

import android.Manifest
import android.annotation.SuppressLint
import android.app.FragmentManager
import android.app.FragmentTransaction
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.support.v4.widget.DrawerLayout
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.TextView
import de.hdodenhof.circleimageview.CircleImageView
import illinois.hack.openfashion.utils.SharedPrefManager

import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.widget.Toast

import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.common.api.Status
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Gino Osahon on 03/03/2017.
 */

// This class is a simple activity with NavigationDrawer
// we get data stored in sharedPrefference and display on the header view of the NavigationDrawer
class NavDrawerActivity : BaseActivity(), GoogleApiClient.OnConnectionFailedListener {

    internal var mContext: Context = this

    private var drawerLayout: DrawerLayout? = null
    private var toolbar: Toolbar? = null
    private var navigationView: NavigationView? = null
    private var mFullNameTextView: TextView? = null
    private var mEmailTextView: TextView? = null
    private var mProfileImageView: CircleImageView? = null
    private var mUsername: String? = null
    private var mEmail: String? = null

    private lateinit var sharedPrefManager: SharedPrefManager
    private var mGoogleApiClient: GoogleApiClient? = null
    private val mAuth: FirebaseAuth? = null


    private val CAMERA_REQUEST_CODE: Int = 0

    private lateinit var bottomNavView: BottomNavigationView
    private lateinit var layout: DrawerLayout
    private lateinit var mStorage: StorageReference

    lateinit var mCurrentPhotoPath: String

//    lateinit var fragmentManager: FragmentManager
//    lateinit var fragmentTransaction: FragmentTransaction

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav_drawer)

        toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        initNavigationDrawer()

        val header = navigationView!!.getHeaderView(0)

        mFullNameTextView = header.findViewById<View>(R.id.fullName) as TextView
        mEmailTextView = header.findViewById<View>(R.id.email) as TextView
        mProfileImageView = header.findViewById<View>(R.id.profileImage) as CircleImageView

        // create an object of sharedPreferenceManager and get stored user data
        sharedPrefManager = SharedPrefManager(mContext)
        mUsername = sharedPrefManager.name
        mEmail = sharedPrefManager.userEmail
        val uri = sharedPrefManager.photo
        val mPhotoUri = Uri.parse(uri)

        //Set data gotten from SharedPreference to the Navigation Header view
        mFullNameTextView!!.text = mUsername
        mEmailTextView!!.text = mEmail

        Picasso.with(mContext)
                .load(mPhotoUri)
                .placeholder(android.R.drawable.sym_def_app_icon)
                .error(android.R.drawable.sym_def_app_icon)
                .into(mProfileImageView)

        configureSignIn()

        mStorage = FirebaseStorage.getInstance().reference

//        fragmentManager = getFragmentManager()
//        fragmentTransaction = fragmentManager.beginTransaction()

        bottomNavView = findViewById(R.id.bottomNavView)
        layout = findViewById(R.id.drawer)
        bottomNavView.selectedItemId = R.id.action_search
        bottomNavView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.action_camera -> {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                            // supposed to show reasoning here but why should i
                        }
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)

                    } else {
                        goToCamera()
                    }
                    true
                }
                R.id.action_search -> {

                    true
                }
                R.id.action_closet -> {

                    true
                }
                else -> false
            }
        }
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {

    }

    // Initialize and add Listener to NavigationDrawer
    fun initNavigationDrawer() {

        navigationView = findViewById<View>(R.id.navigation_view) as NavigationView
        navigationView!!.setNavigationItemSelectedListener { item ->
            val id = item.itemId

            when (id) {
                R.id.payment -> {
                    drawerLayout!!.closeDrawers()
                }
                R.id.freebie -> {
                    Toast.makeText(applicationContext, "Home", Toast.LENGTH_SHORT).show()
                    drawerLayout!!.closeDrawers()
                }
                R.id.trip -> {
                    Toast.makeText(applicationContext, "Trash", Toast.LENGTH_SHORT).show()
                    drawerLayout!!.closeDrawers()
                }
                R.id.logout -> {
                    signOut()
                    drawerLayout!!.closeDrawers()
                }
                R.id.tips -> {
                    Toast.makeText(applicationContext, "Trash", Toast.LENGTH_SHORT).show()
                    drawerLayout!!.closeDrawers()
                }
            }
            false
        }

        //set up navigation drawer
        drawerLayout = findViewById<View>(R.id.drawer) as DrawerLayout
        val actionBarDrawerToggle = object : ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {
            override fun onDrawerClosed(drawerView: View?) {
                super.onDrawerClosed(drawerView)
            }

            override fun onDrawerOpened(drawerView: View?) {
                super.onDrawerOpened(drawerView)
            }
        }
        drawerLayout!!.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()
    }

    // This method configures Google SignIn
    fun configureSignIn() {
        // Configure sign-in to request the user's basic profile like name and email
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()

        // Build a GoogleApiClient with access to GoogleSignIn.API and the options above.
        mGoogleApiClient = GoogleApiClient.Builder(mContext)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, options)
                .build()
        mGoogleApiClient!!.connect()
    }

    //method to logout
    private fun signOut() {
        SharedPrefManager(mContext).clear()
        mAuth!!.signOut()

        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback {
            val intent = Intent(this@NavDrawerActivity, illinois.hack.openfashion.MainActivity::class.java)
            startActivity(intent)
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

    @SuppressLint("SimpleDateFormat")
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

    @SuppressLint("SimpleDateFormat")
    private fun fileName(): String {
        val timeStamp = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(Date())
        return "In/$timeStamp.png"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == AppCompatActivity.RESULT_OK) {
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

            var reference = mStorage.child( fileName() )

            var uploadTask = reference.putBytes( byte )
            uploadTask.addOnFailureListener({ println( "upload failed" ) })
                    .addOnSuccessListener { OnSuccessListener<UploadTask.TaskSnapshot> { println( "upload succeeded" ) } }

        }
    }
}