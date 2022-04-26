package com.tolgahantutar.videoeditorkitmedium

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class CheckPermissions(private val mainActivity: MainActivity) {
       var requestPermissionLauncher =
            mainActivity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){permissions ->
                if (permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true &&
                    permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true &&
                    permissions[Manifest.permission.RECORD_AUDIO] == true) {

                }
            }


        fun requestPermissionsOnLaunch(): Boolean {
            when {
                ContextCompat.checkSelfPermission(
                    mainActivity,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(
                            mainActivity,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(
                            mainActivity,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED -> {
                    return true
                    //TODO app will launch the user gallery
                }
                else -> {
                    return false
                }
            }
        }

        fun launchRequestPermission() {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }