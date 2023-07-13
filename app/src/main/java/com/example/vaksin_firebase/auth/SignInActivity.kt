package com.example.vaksin_firebase.auth

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vaksin_firebase.MainActivity
import com.example.vaksin_firebase.databinding.ActivitySignInBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import kotlin.math.sign


class SignInActivity : AppCompatActivity () {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseAuth = FirebaseAuth.getInstance()

        binding.btnSignin.setOnClickListener {
            val email = binding.txtEmail.text.toString()
            val password = binding.txtPass.text.toString()
            signin_firebase(email, password)
        }

        binding.tvSignup.setOnClickListener{
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }

    private fun signin_firebase(email: String, password: String) {
        if (email.isNotEmpty() && password.isNotEmpty()) {
            firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener{
                if(it.isSuccessful) {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
                else {
                    Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()

                }
            }
        }
        else {
            Toast.makeText(this, "Lengkapi Input", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()

        if(firebaseAuth.currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}