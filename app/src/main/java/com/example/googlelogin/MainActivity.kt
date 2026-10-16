package com.example.googlelogin

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    private lateinit var status: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        status = TextView(this).apply {
            textSize = 18f
            text = auth.currentUser?.email ?: "Belum login"
        }

        val loginButton = Button(this).apply {
            text = "Login dengan Google"
            setOnClickListener { signInWithGoogle() }
        }

        val logoutButton = Button(this).apply {
            text = "Logout"
            setOnClickListener {
                auth.signOut()
                status.text = "Belum login"
            }
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 64, 32, 32)
            addView(status)
            addView(loginButton)
            addView(logoutButton)
        })
    }

    private fun signInWithGoogle() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val webClientId = BuildConfig.WEB_CLIENT_ID
                val option = GetGoogleIdOption.Builder()
                    .setServerClientId(webClientId)
                    .setFilterByAuthorizedAccounts(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(option)
                    .build()

                val result = CredentialManager.create(this@MainActivity)
                    .getCredential(this@MainActivity, request)

                val credential = result.credential
                if (credential !is CustomCredential ||
                    credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    status.text = "Jenis kredensial Google tidak dikenali"
                    return@launch
                }

                val googleCredential =
                    GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(
                    googleCredential.idToken,
                    null
                )

                auth.signInWithCredential(firebaseCredential).await()
                status.text = auth.currentUser?.email ?: "Login berhasil"
            } catch (e: GetCredentialException) {
                status.text = "Login dibatalkan/gagal: ${e.message}"
            } catch (e: Exception) {
                status.text = "Login gagal: ${e.message}"
            }
        }
    }
}
