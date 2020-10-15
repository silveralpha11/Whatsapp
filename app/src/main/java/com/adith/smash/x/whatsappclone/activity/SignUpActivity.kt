package com.adith.smash.x.whatsappclone.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.Window
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.adith.smash.x.whatsappclone.R
import com.adith.smash.x.whatsappclone.util.DATA_USERS
import com.adith.smash.x.whatsappclone.util.User
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_sign_up.*

class SignUpActivity : AppCompatActivity() {

    private val firebaseDb = FirebaseFirestore.getInstance()
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firebaseAuthListener = FirebaseAuth.AuthStateListener {
        val user = firebaseAuth.currentUser?.uid
        if (user != null) {
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_sign_up)

        setTextChangedListener(edt_email_reg,til_email)
        setTextChangedListener(edt_password_reg,til_password)
        setTextChangedListener(edt_name_reg,til_name)
        setTextChangedListener(edt_phone_reg,til_phone)
        reg_progress_layout.setOnTouchListener{ v, event -> true}

        btn_signup.setOnClickListener{
            onSignup()
        }

        txt_login.setOnClickListener {
            onLogin()
        }
    }

    private fun setTextChangedListener(edt: EditText, til: TextInputLayout) {
        edt.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(p0: Editable?) {

            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                til.isErrorEnabled = false
            }
        })
    }

    override fun onStart() {
        super.onStart()
        firebaseAuth.addAuthStateListener(firebaseAuthListener)
    }

    override fun onStop() {
        super.onStop()
        firebaseAuth.removeAuthStateListener(firebaseAuthListener)
    }

    private fun onSignup() {
        var proceed = true
        if (edt_name_reg.text.isNullOrEmpty()) {
            til_name.error = "Required Name"
            til_name.isErrorEnabled = true
            proceed = false
        }
        if (edt_phone_reg.text.isNullOrEmpty()) {
            til_phone.error = "Required Phone Number"
            til_phone.isErrorEnabled = true
            proceed = false
        }
        if (edt_email_reg.text.isNullOrEmpty()) {
            til_email.error = "Required Email"
            til_email.isErrorEnabled = true
            proceed = false
        }
        if (edt_password_reg.text.isNullOrEmpty()) {
            til_password.error = "Required Password"
            til_password.isErrorEnabled = true
            proceed = false
        }
        if (proceed) {
            reg_progress_layout.visibility = View.GONE
            firebaseAuth.createUserWithEmailAndPassword(
                edt_email_reg.text.toString(),
                edt_password_reg.text.toString()
            ).addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        reg_progress_layout.visibility = View.GONE
                        Toast.makeText(
                            this@SignUpActivity, "SignUpError: ${task.exception?.localizedMessage}",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else if (firebaseAuth.uid != null) {
                        val email = edt_email_reg.text.toString()
                        val phone = edt_phone_reg.text.toString()
                        val name = edt_name_reg.text.toString()
                        val user = User(email, phone, name, "", "Hello i'm new", "", "")
                        firebaseDb.collection(DATA_USERS).document(firebaseAuth.uid!!).set(user)
                    }
                    reg_progress_layout.visibility = View.GONE
                }
                .addOnFailureListener {
                    reg_progress_layout.visibility = View.GONE
                    it.printStackTrace()
                }
        }
    }
    private fun onLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}