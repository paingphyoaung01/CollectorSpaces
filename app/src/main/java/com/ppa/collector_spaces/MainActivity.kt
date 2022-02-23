package com.ppa.collector_spaces

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import androidx.core.view.isVisible as isVisible1

class MainActivity : AppCompatActivity() {
    lateinit var file: File
    lateinit var uri: Uri
    lateinit var path: String
    lateinit var filePath: String
    companion object {
        lateinit var progress : ProgressDialog
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val spacesFileRepository = SpacesFileRepository(applicationContext)

//        btn_download.setOnClickListener {
//            spacesFileRepository.downloadExampleFile() { file, _ ->
//                runOnUiThread {
////                    imageView.setImageURI(Uri.fromFile(file))
////                    txt_download.text = file!!.name
//                }
//            }
//        }

        btn_file_picker.setOnClickListener {
            requestPermission()
        }

        btn_upload.setOnClickListener {
            try {
                showProgressDialog("Version Update","Uploading......")
                spacesFileRepository.uploadExampleFile(file)

            }catch (e:Exception){
                Toast.makeText(this,e.toString(),Toast.LENGTH_LONG).show()
            }
        }
    }

    // Permission Request
    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 101
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            101 -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission Deny", Toast.LENGTH_SHORT).show()
                } else {
                    FilePick()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun FilePick() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type =
            "application/vnd.android.package-archive" // application/vnd.android.package-archive
        startActivityForResult(Intent.createChooser(intent, "Title of Choose"), 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 100 && resultCode == Activity.RESULT_OK && data != null) {
            var fileSplit: File? = null
            uri = data.data!!

            try {
                val cursor: Cursor? = contentResolver.query(
                    uri, null, null, null, null
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        val file: String =
                            it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                        filePath = file
                    }
                    path = uri.path!!
                }
                fileSplit = File(path)
                val split =
                    fileSplit.path.split(":".toRegex()).toTypedArray() //split the path.

                filePath = split[1] //assign it to a string(your choice).
                file = File(Environment.getExternalStorageDirectory(), filePath)
                txt_file_picker.text = file!!.name
            } catch (e1: Exception) {
                e1.printStackTrace()
                Toast.makeText(this, "Please Choose from Internal Storage", Toast.LENGTH_LONG)
                    .show()
            }
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun showProgressDialog(title: String?, message: String?) {
        progress = ProgressDialog(this)
        progress.setTitle(title)
        progress.setMessage(message)
        progress.setCancelable(false) // disable dismiss by tapping outside of the dialog
        progress.show()
    }
}