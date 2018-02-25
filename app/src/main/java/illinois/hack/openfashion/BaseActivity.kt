package illinois.hack.openfashion

import android.app.ProgressDialog
import android.support.v7.app.AppCompatActivity
import android.os.Bundle

import com.firebase.client.Firebase

open class BaseActivity : AppCompatActivity() {
    var mProgressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

        //Initialize Firebase
        Firebase.setAndroidContext(this)
    }

    fun showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog(this)
            mProgressDialog!!.setMessage(getString(R.string.loading))
            mProgressDialog!!.isIndeterminate = true
        }

        mProgressDialog!!.show()
    }

    fun hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog!!.isShowing) {
            mProgressDialog!!.dismiss()
        }
    }

    override fun onStop() {
        super.onStop()
        hideProgressDialog()
    }
}