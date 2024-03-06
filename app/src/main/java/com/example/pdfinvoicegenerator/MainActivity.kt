package com.example.pdfinvoicegenerator

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pdfinvoicegenerator.databinding.ActivityMainBinding
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding

    private val PERMISSION_REQUEST_CODE = 10
    private val CREATE_FILE = 1
    private val TAG = "Tag"
    private var pdfHeight = 1080
    private var pdfWidth = 720
    private var document: PdfDocument? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[android.Manifest.permission.WRITE_EXTERNAL_STORAGE] == true &&
                permissions[android.Manifest.permission.READ_EXTERNAL_STORAGE] == true
            ) {
                generatePDF(binding.editPdfContent.text.toString())
            } else {
                showToast("Permission denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.createFromEditButton.setOnClickListener {
            if (checkPermissions()) {
                generatePDF(binding.editPdfContent.text.toString())
            } else {
                requestPermission()
            }
        }

        binding.createFromViewButton.setOnClickListener {
            val invoiceDialog = Dialog(this)
            invoiceDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            invoiceDialog.setContentView(R.layout.invoice_view)
            invoiceDialog.setCancelable(true)
            val lp = WindowManager.LayoutParams()
            lp.copyFrom(invoiceDialog.window?.attributes)
            lp.width = WindowManager.LayoutParams.MATCH_PARENT
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
            invoiceDialog.window?.attributes = lp

            val downloadInvoiceButton =
                invoiceDialog.findViewById<Button>(R.id.button_generate_invoice)
            invoiceDialog.show()

            downloadInvoiceButton.setOnClickListener {
                generatePDFFromView(invoiceDialog.findViewById(R.id.invoice_view))
            }
        }
    }

    private fun generatePDF(text: String) {
        document = PdfDocument()
        val paintText = Paint()

        val pageInfo = PdfDocument.PageInfo.Builder(pdfWidth, pdfHeight, 1).create()
        val page = document?.startPage(pageInfo)
        val canvas = page?.canvas

        paintText.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.NORMAL)
        paintText.textSize = 25f
        paintText.color = ContextCompat.getColor(this, R.color.black)
        paintText.textAlign = Paint.Align.CENTER
        canvas?.drawText("(PDF by Jamal Ali)", 396f, 50f, paintText)

        paintText.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        paintText.color = ContextCompat.getColor(this, R.color.grey)
        paintText.textSize = 17f
        paintText.textAlign = Paint.Align.LEFT
        canvas?.drawText(text, 50f, 100f, paintText)

        document?.finishPage(page)
        createFile()
    }

    private fun generatePDFFromView(view: View) {
        val bitmap = getBitmapFromView(view)
        document = PdfDocument()
        val myPageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val myPage = document?.startPage(myPageInfo)
        val canvas = myPage?.canvas
        canvas?.drawBitmap(bitmap, 0f, 0f, null)
        document?.finishPage(myPage)
        createFile()
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable: Drawable? = view.background
        bgDrawable?.draw(canvas) ?: canvas.drawColor(Color.WHITE)
        view.draw(canvas)
        return returnedBitmap
    }

    private fun createFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/pdf"
        intent.putExtra(Intent.EXTRA_TITLE, "new_pdf.pdf")
        requestPermissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        )
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ),
            PERMISSION_REQUEST_CODE
        )
    }

    private fun checkPermissions(): Boolean {
        val permission1 =
            ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        val permission2 =
            ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )

        return permission1 == PackageManager.PERMISSION_GRANTED && permission2 == PackageManager.PERMISSION_GRANTED
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CREATE_FILE && resultCode == Activity.RESULT_OK) {
            var uri: Uri? = null
            data?.let {
                uri = it.data

                if (document != null) {
                    var pfd: ParcelFileDescriptor? = null
                    try {
                        pfd = contentResolver.openFileDescriptor(uri!!, "w")
                        val fileOutputStream = FileOutputStream(pfd?.fileDescriptor)
                        document!!.writeTo(fileOutputStream)
                        document!!.close()
                        Toast.makeText(this, "PDF saved successfully!", Toast.LENGTH_SHORT).show()
                    } catch (e: IOException) {
                        try {
                            DocumentsContract.deleteDocument(contentResolver, uri!!)
                        } catch (ex: FileNotFoundException) {
                            ex.printStackTrace()
                        }
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}

