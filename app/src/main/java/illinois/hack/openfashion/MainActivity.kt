package illinois.hack.openfashion

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.Toast

import com.firebase.client.FirebaseError
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.ServerValue
import com.firebase.client.Firebase
import android.Manifest

import java.util.HashMap

import illinois.hack.openfashion.model.User
import illinois.hack.openfashion.utils.Constants
import illinois.hack.openfashion.utils.SharedPrefManager
import illinois.hack.openfashion.utils.Utils

class MainActivity : BaseActivity(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    private var mGoogleApiClient: GoogleApiClient? = null
    private var mAuth: FirebaseAuth? = null
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null
    private var idToken: String? = null
    private lateinit var sharedPrefManager: SharedPrefManager
    private val mContext = this

    private var name: String? = null
    private var email: String? = null
    private var photo: String? = null
    private var photoUri: Uri? = null
    private var mSignInButton: SignInButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mSignInButton = findViewById<View>(R.id.login_with_google) as SignInButton
        mSignInButton!!.setSize(SignInButton.SIZE_WIDE)

        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE), REQUEST_INTERNET)
        }

        mSignInButton!!.setOnClickListener(this)

        configureSignIn()

        mAuth = com.google.firebase.auth.FirebaseAuth.getInstance()

        //this is where we start the Auth state Listener to listen for whether the user is signed in or not
        mAuthListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            // Get signedIn user
            val user = firebaseAuth.currentUser

            //if user is signed in, we call a helper method to save the user details to Firebase
            if (user != null) {
                // User is signed in
                createUserInFirebaseHelper()
                Log.d(TAG, "onAuthStateChanged:signed_in:" + user.uid)
            } else {
                // User is signed out
                Log.d(TAG, "onAuthStateChanged:signed_out")
            }
        }
    }

    private fun createUserInFirebaseHelper() {
        val encodedEmail = Utils.encodeEmail(email!!.toLowerCase())

        val userLocation = Firebase(Constants.FIREBASE_URL_USERS).child(encodedEmail)

        userLocation.addListenerForSingleValueEvent(object : com.firebase.client.ValueEventListener {
            override fun onDataChange(dataSnapshot: com.firebase.client.DataSnapshot) {
                if (dataSnapshot.value == null) {
                    val timestampJoined = HashMap<String, Any>()
                    timestampJoined[Constants.FIREBASE_PROPERTY_TIMESTAMP] = ServerValue.TIMESTAMP

                    val newUser = User(name, photo, encodedEmail, timestampJoined)
                    userLocation.setValue(newUser)

                    Toast.makeText(this@MainActivity, "Account created!", Toast.LENGTH_SHORT).show()

                    // After saving data to Firebase, goto next activity
                    //                    Intent intent = new Intent(MainActivity.this, NavDrawerActivity.class);
                    //                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    //                    startActivity(intent);
                    //                    finish();
                }
            }

            override fun onCancelled(firebaseError: FirebaseError) {

                Log.d(TAG, getString(R.string.log_error_occurred) + firebaseError.message)
                //hideProgressDialog();
                if (firebaseError.code == FirebaseError.EMAIL_TAKEN) {
                } else {
                    Toast.makeText(this@MainActivity, firebaseError.message, Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    fun configureSignIn() {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(this@MainActivity.resources.getString(R.string.web_client_id))
                .requestEmail()
                .build()

        mGoogleApiClient = GoogleApiClient.Builder(mContext)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, options)
                .build()
        mGoogleApiClient!!.connect()
    }

    private fun signIn() {
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result.isSuccess) {
                val account = result.signInAccount

                idToken = account!!.idToken

                name = account.displayName
                email = account.email
                photoUri = account.photoUrl
                photo = photoUri.toString()

                sharedPrefManager = SharedPrefManager(mContext)
                sharedPrefManager.saveIsLoggedIn(mContext, true)

                sharedPrefManager.saveEmail(mContext, email)
                sharedPrefManager.saveName(mContext, name)
                sharedPrefManager.savePhoto(mContext, photo)

                sharedPrefManager.saveToken(mContext, idToken)
                //sharedPrefManager.saveIsLoggedIn(mContext, true);

                val credential = GoogleAuthProvider.getCredential(idToken, null)
                firebaseAuthWithGoogle(credential)
            } else {
                Log.e(TAG, "Login Unsuccessful. ")
                Toast.makeText(this, "Login Unsuccessful", Toast.LENGTH_SHORT)
                        .show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(credential: AuthCredential) {
        showProgressDialog()
        mAuth!!.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful)
                    if (!task.isSuccessful) {
                        Log.w(TAG, "signInWithCredential" + task.exception!!.message)
                        task.exception!!.printStackTrace()
                        Toast.makeText(this@MainActivity, "Authentication failed.",
                                Toast.LENGTH_SHORT).show()
                    } else {
                        createUserInFirebaseHelper()
                        Toast.makeText(this@MainActivity, "Login successful",
                                Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@MainActivity, NavDrawerActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                    hideProgressDialog()
                }
    }

    override fun onStart() {
        super.onStart()
        if (mAuthListener != null) {
            FirebaseAuth.getInstance().signOut()
        }
        mAuth!!.addAuthStateListener(mAuthListener!!)
    }

    override fun onStop() {
        super.onStop()
        if (mAuthListener != null) {
            mAuth!!.removeAuthStateListener(mAuthListener!!)
        }
    }

    override fun onConnected(bundle: Bundle?) {

    }

    override fun onConnectionSuspended(i: Int) {

    }

    override fun onClick(view: View) {

        val utils = Utils(this)
        val id = view.id

        if (id == R.id.login_with_google) {
            if (utils.isNetworkAvailable) {
                signIn()
            } else {
                Toast.makeText(this@MainActivity, "Oops! no internet connection!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {

    }

    companion object {
        private val RC_SIGN_IN = 9001
        private val TAG = "MainActivity"
        private val REQUEST_INTERNET = 200
    }
}
