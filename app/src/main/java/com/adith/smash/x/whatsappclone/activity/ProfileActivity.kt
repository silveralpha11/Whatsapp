package com.adith.smash.x.whatsappclone.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.adith.smash.x.whatsappclone.R
import com.adith.smash.x.whatsappclone.util.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_login.progress_layout
import kotlinx.android.synthetic.main.activity_profile.*

class ProfileActivity : AppCompatActivity() {

    private val firebaseDb = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firebaseStorage = FirebaseStorage.getInstance().reference
    private var imageUrl: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        if (userId.isNullOrEmpty()){
            finish()
        }

        progress_layout_profile.setOnTouchListener { v, event -> true }
        
        btn_apply.setOnClickListener {
            onApply()
        }

        btn_delete_account.setOnClickListener {
            onDelete()
        }

        imbtn_profile.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_CODE_PHOTO)
        }

        btn_back.setOnClickListener {
            onBack()

        }


        populateInfo()
    }

    private fun onBack() {
        val intent = Intent(this, MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun populateInfo() {
        progress_layout_profile.visibility = View.VISIBLE
        firebaseDb.collection(DATA_USERS).document(userId!!).get()
            .addOnSuccessListener {
                val user = it.toObject(User::class.java)
                imageUrl = user?.imageUrl
                edt_name_profile.setText(user?.name, TextView.BufferType.EDITABLE)
                edt_email_profile.setText(user?.email, TextView.BufferType.EDITABLE)
                edt_phone_profile.setText(user?.phone, TextView.BufferType.EDITABLE)
                if (imageUrl != null){
                    populateImage(this, user?.imageUrl, img_profile, R.drawable.ic_user)
                }
                progress_layout_profile.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                finish()
            }
        }

    fun onApply() {
        progress_layout_profile.visibility = View.VISIBLE
        val name = edt_name_profile.text.toString()
        val email = edt_email_profile.text.toString()
        val phone = edt_phone_profile.text.toString()
        val map = HashMap<String, Any>()
        map[DATA_USER_NAME] = name
        map[DATA_USER_EMAIL] = email
        map[DATA_USER_PHONE] = phone

        firebaseDb.collection(DATA_USERS).document(userId!!).update(map)
            .addOnSuccessListener {
                Toast.makeText(this, "Update Succesful", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show()
                progress_layout_profile.visibility = View.GONE
            }
        }


    override fun onDestroy() {
        super.onDestroy()
        if (firebaseAuth.currentUser == null){
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

        private fun onDelete() {
            progress_layout_profile.visibility = View.VISIBLE
            AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("This will delete your Profile Information, Are you sure?")
                .setPositiveButton("Yes"){dialog, which ->
                    firebaseDb.collection(DATA_USERS).document(userId!!).delete()

                    firebaseStorage.child(DATA_IMAGES).child(userId).delete()
                    firebaseAuth.currentUser?.delete()?.addOnSuccessListener {
                        var intent = Intent(this, LoginActivity::class.java)
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                        finish()
                        }
                        ?.addOnFailureListener {
                            finish()
                        }
                    progress_layout_profile.visibility = View.GONE

                }
                .setNegativeButton("No"){dialog, which ->
                    progress_layout_profile.visibility = View.GONE
                }
                .setCancelable(false)
                .show()
            }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_PHOTO){
            storeImage(data?.data)
        }
    }

    private fun storeImage(uri: Uri?) {
        if (uri != null){
            Toast.makeText(this, "Uploading Image...", Toast.LENGTH_SHORT).show()
            progress_layout_profile.visibility = View.VISIBLE
            val filePath = firebaseStorage.child(DATA_IMAGES).child(userId!!)

            filePath.putFile(uri)
                .addOnSuccessListener {
                    filePath.downloadUrl
                        .addOnSuccessListener {
                            val url = it.toString()
                            firebaseDb.collection(DATA_USERS)
                                .document(userId)
                                .update(DATA_USER_IMAGE_URL, url)
                                .addOnSuccessListener {
                                    imageUrl = url
                                    populateImage(this, imageUrl, img_profile, R.drawable.ic_user)
                                }
                            progress_layout_profile.visibility = View.GONE

                        }

                        .addOnFailureListener {
                            onUploadFailured()
                        }
                }
                .addOnFailureListener {
                    onUploadFailured()
                }
        }

    }

    private fun onUploadFailured() {
        Toast.makeText(this, "Image upload failed, Please try again later.",
        Toast.LENGTH_SHORT).show()
        progress_layout_profile.visibility = View.GONE
    }
}