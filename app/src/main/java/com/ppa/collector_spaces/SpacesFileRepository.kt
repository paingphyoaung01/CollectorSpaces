package com.ppa.collector_spaces

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.internal.StaticCredentialsProvider
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.ppa.collector_spaces.MainActivity.Companion.progress
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*


interface SpaceRegionRepresentable {
    fun endpoint(): String
}

/**
 * Represents a region in which a Digital Ocean Space can be created
 */
enum class SpaceRegion : SpaceRegionRepresentable {
    SFO {
        override fun endpoint(): String {
            return "https://sfo2.digitaloceanspaces.com"
        }
    },
    AMS {
        override fun endpoint(): String {
            return "https://ams3.digitaloceanspaces.com"
        }
    },
    SGP {
        override fun endpoint(): String {
            return "https://sgp1.digitaloceanspaces.com"
        }
    }
}

class SpacesFileRepository(context: Context) {

    private val accesskey = "TJBV5M4355SZCPTB7KOX"
    private val secretkey = "jNiqyJ9BzXSbBtqHu+7xV25JlGBGcm1RN5BggrPcT0I"
    private val spacename = "apk-update"     // https://apk-update.sgp1.digitaloceanspaces.com
    private val spaceregion = SpaceRegion.SGP

    private val filename = "example_application"
    private val filetype = "apk"

    private var transferUtility: TransferUtility
    private var appContext: Context

    lateinit var file: File
    init {
        val credentials = StaticCredentialsProvider(BasicAWSCredentials(accesskey, secretkey))
        val client = AmazonS3Client(credentials, Region.getRegion("us-east-1"))
        client.endpoint = spaceregion.endpoint()

        TransferNetworkLossHandler.getInstance(context)
        transferUtility = TransferUtility.builder().s3Client(client).context(context).build()
        appContext = context
    }

    /**
     * Converts a APK resource to a file for uploading with the S3 SDK
     */
    private fun convertResourceToFile(): File {
        val exampleIdentifier =
            appContext.resources.getIdentifier(filename, "drawable", appContext.packageName)
        val exampleBitmap = BitmapFactory.decodeResource(appContext.resources, exampleIdentifier)

        val exampleFile = File(appContext.filesDir, Date().toString())
        exampleFile.createNewFile()
//        val exampleFile = File(file, Date().toString())
//        exampleFile.createNewFile()

        val outputStream = ByteArrayOutputStream()
        exampleBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val exampleBitmapData = outputStream.toByteArray()

        val fileOutputStream = FileOutputStream(exampleFile)
        fileOutputStream.write(exampleBitmapData)
        fileOutputStream.flush()
        fileOutputStream.close()

        return exampleFile

    }

    /**
     * Uploads the example file to a DO Space
     */
    fun uploadExampleFile(file: File) {
        val filePermission = CannedAccessControlList.PublicRead
        //Starts the upload of our file
        var listener =
            transferUtility.upload(spacename,"$filename.$filetype",file)

        //Listens to the file upload progress, or any errors that might occur
        listener.setTransferListener(object : TransferListener {
            override fun onError(id: Int, ex: Exception?) {
//                Log.d("S3 Upload", ex.toString())
                Log.d("msg",ex.toString())
                Toast.makeText(appContext,ex.toString(),Toast.LENGTH_LONG).show()
                progress.dismiss()
            }

            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
//                Log.d("S3 Upload", "Progress ${((bytesCurrent / bytesTotal) * 100)}")
                Log.d("msg","Progress ${((bytesCurrent / bytesTotal) * 100)}")
            }

            override fun onStateChanged(id: Int, state: TransferState?) {
                if (state == TransferState.COMPLETED) {
//                    Log.d("S3 Upload", "Completed")
                    Log.d("msg","Upload Completed")
                    Toast.makeText(appContext,"Upload Completed",Toast.LENGTH_LONG).show()
                    progress.dismiss()
                }
            }
        })
    }


    /**
     * Downloads example file from a DO Space
     */
    fun downloadExampleFile(callback: (File?, Exception?) -> Unit) {
        //Create a local File object to save the remote file to
//        val file = File("${appContext.cacheDir}/$filename.$filetype")
        val path = Environment.getExternalStorageDirectory().path
        val folder = File(path,"Space Download")
        folder.mkdirs()
        file = File(folder,"example.apk")

        if (file.exists()) file.delete()

        //Download the file from DO Space
        var listener = transferUtility.download(spacename, "$filename.$filetype", file)

        //Listen to the progress of the download, and call the callback when the download is complete
        listener.setTransferListener(object : TransferListener {
            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                Log.d("S3 Download", "Progress ${((bytesCurrent / bytesTotal) * 100)}")
            }

            override fun onStateChanged(id: Int, state: TransferState?) {
                if (state == TransferState.COMPLETED) {
                    Log.d("S3 Download", "Completed")
                    Toast.makeText(appContext,"Download Completed",Toast.LENGTH_LONG).show()
                    showInstallOption()
                    callback(file, null)
                }
            }

            override fun onError(id: Int, ex: Exception?) {
                Log.d("S3 Download", ex.toString())
                Toast.makeText(appContext,ex.toString(),Toast.LENGTH_LONG).show()
                callback(null, ex)
            }
        })
    }
    fun showInstallOption(){
        val intent = Intent(Intent.ACTION_VIEW)
//        val file = File("/mnt/sdcard/myapkfile.apk")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
        appContext.startActivity(intent)
    }
}