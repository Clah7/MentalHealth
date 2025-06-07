package com.example.mentalhealth

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.activity.ComponentActivity

class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AlertDialog.Builder(this)
            .setTitle("Butuh Izin")
            .setMessage("Aplikasi memerlukan akses ke data kesehatan Anda untuk fitur ini.")
            .setPositiveButton("OK") { dialog: DialogInterface, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .show()
    }
}