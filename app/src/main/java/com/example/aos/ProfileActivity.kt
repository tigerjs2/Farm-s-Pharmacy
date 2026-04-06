package com.example.aos

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        mAuth = FirebaseAuth.getInstance()

        loadUserName()

        findViewById<TextView>(R.id.tvMyInfo).setOnClickListener {
            startActivity(Intent(this, MyInfoActivity::class.java))
        }

        findViewById<TextView>(R.id.tvLogout).setOnClickListener {
            mAuth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserName()
    }

    private fun loadUserName() {
        val uid = mAuth.currentUser?.uid ?: return
        Firebase.firestore
            .collection("Users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                val name = document.getString("name") ?: ""
                findViewById<TextView>(R.id.tvProfileName).text = name
            }
    }
}