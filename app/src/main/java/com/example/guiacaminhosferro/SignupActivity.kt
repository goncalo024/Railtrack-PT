package com.example.guiacaminhosferro

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // inicializa o FirebaseAuth
        auth = FirebaseAuth.getInstance()

        val etUsername  = findViewById<EditText>(R.id.etUsername)
        val etEmail     = findViewById<EditText>(R.id.etEmail)
        val etPassword  = findViewById<EditText>(R.id.etPassword)
        val etConfirm   = findViewById<EditText>(R.id.etConfirmPassword)
        val btnCreate   = findViewById<Button>(R.id.btnCreateAccount)

        btnCreate.setOnClickListener {
            val user    = etUsername.text.toString().trim()
            val email   = etEmail.text.toString().trim()
            val pass    = etPassword.text.toString()
            val confirm = etConfirm.text.toString()

            // validações
            if (user.isEmpty() || email.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!isValidPassword(pass)) {
                etPassword.error = "Mínimo 8 carateres, 1 maiúscula, 1 minúscula, 1 dígito e 1 símbolo"
                return@setOnClickListener
            }
            if (pass != confirm) {
                etConfirm.error = "Palavras-passe não coincidem"
                return@setOnClickListener
            }

            // cria utilizador no Firebase
            auth.createUserWithEmailAndPassword(email,pass)
                .addOnCompleteListener(this){ task ->
                    if(task.isSuccessful){
                        // grava displayName
                        auth.currentUser?.updateProfile(
                            UserProfileChangeRequest.Builder()
                                .setDisplayName(user)
                                .build()
                        )?.addOnCompleteListener{
                            Toast.makeText(this,"Conta criada com sucesso!",Toast.LENGTH_SHORT).show()
                            finish() // volta ao login
                        }
                    } else {
                        Toast.makeText(this,"Erro: ${task.exception?.message}",Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun isValidPassword(password: String): Boolean {
        val regex =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#\$%!&*()_+=\\-\\[\\]{};:'\\\\|,.<>\\/?]).{8,}\$"
                .toRegex()
        return regex.containsMatchIn(password)
    }
}
