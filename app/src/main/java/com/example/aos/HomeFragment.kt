package com.example.aos

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class HomeFragment : Fragment() {

    private lateinit var mAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAuth = FirebaseAuth.getInstance()

        loadUserName(view)

        view.findViewById<android.widget.ImageView>(R.id.ivProfile).setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserName(requireView())
    }

    private fun loadUserName(view: View) {
        val uid = mAuth.currentUser?.uid ?: return
        val tvGreeting = view.findViewById<TextView>(R.id.tvGreeting)

        Firebase.firestore
            .collection("Users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                val name = document.getString("name") ?: "농부"
                tvGreeting.text = "Hello, ${name}님!"
            }
            .addOnFailureListener {
                tvGreeting.text = "안녕하세요!"
            }
    }
}