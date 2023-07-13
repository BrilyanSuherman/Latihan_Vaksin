package com.example.vaksin_firebase.pasien

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.contextaware.withContextAvailable
import androidx.appcompat.app.AppCompatActivity
import com.example.vaksin_firebase.MainActivity
import com.example.vaksin_firebase.databinding.ActivityEditPasienBinding
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Date
import kotlin.math.log


class EditPasienActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditPasienBinding
    private val db = FirebaseFirestore.getInstance()

    private val REQ_CAM = 101
    private lateinit var imgUri : Uri
    private var datagambar: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditPasienBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val (year, month, day, curr_pasien) = setDefaultValue()

        binding.TxtEditTglLahir.setOnClickListener{
            val dpd = DatePickerDialog(this,
            DatePickerDialog.OnDateSetListener{view, year, monthOfYear, dayOfMonth ->
                binding.TxtEditTglLahir.setText(
                    ""+ year +"-"+(monthOfYear +1)+ "-"+dayOfMonth)
            },year.toString().toInt(),month.toString().toInt(),day.toString().toInt()
            )
            dpd.show()
        }
        binding.BtnEditPasien.setOnClickListener {
            val new_data_pasien = newPasien()
            updatePasien(curr_pasien as Pasien, new_data_pasien)

            val intentMain = Intent(this, MainActivity::class.java)
            startActivity(intentMain)
            finish()
        }

        binding.BtnImgPasien.setOnClickListener{ openCamera() }

        showFoto()
    }

    fun setDefaultValue(): Array<Any> {
        val intent = intent
        val nik = intent.getStringExtra("nik").toString()
        val nama = intent.getStringExtra("nama").toString()
        val tgl_lahir = intent.getStringExtra("tgl_lahir").toString()
        val jenis_kelamin = intent.getStringExtra("jenis_kelamin").toString()
        val penyakit_bawaan = intent.getStringExtra("penyakit_bawaan").toString()

        binding.TxtEditNIK.setText(nik)
        binding.TxtEditNama.setText(nama)
        binding.TxtEditTglLahir.setText(tgl_lahir)

        val tgl_split = intent.getStringExtra("tgl_lahir")
            .toString().split("-").toTypedArray()
        val year = tgl_split[0].toInt()
        val month = tgl_split[1].toInt() - 1
        val day = tgl_split[2].toInt()
        if (jenis_kelamin == "Laki - Laki") {
            binding.RdnEditJKL.isChecked = true
        } else if (jenis_kelamin == "Perempuan") {
            binding.RdnEditJKP.isChecked = true
        }

        val penyakit = penyakit_bawaan.split("|").toTypedArray()
        for (p in penyakit) {
            if (p == "diabetes") {
                binding.ChkEditDiabetes.isChecked = true
            } else if (p == "jantung") {
                binding.ChkEditJantung.isChecked = true
            } else if (p == "Asma") {
                binding.ChkEditAsma.isChecked = true
            }
        }
        val curr_pasien = Pasien(nik, nama, tgl_lahir, jenis_kelamin, penyakit_bawaan)
        return arrayOf(year, month, day, curr_pasien)
    }

    fun newPasien(): Map<String, Any> {
        var nik: String = binding.TxtEditNIK.text.toString()
        var nama: String = binding.TxtEditNama.text.toString()
        var tgl_lahir: String = binding.TxtEditTglLahir.text.toString()

        var jk: String = ""
        if (binding.RdnEditJKL.isChecked) {
            jk = "Laki-Laki"
        }
        else if (binding.RdnEditJKP.isChecked) {
            jk = "Perempuan"
        }

        var penyakit = ArrayList<String>()
        if(binding.ChkEditDiabetes.isChecked){
            penyakit.add("diabetes")
        }
        if(binding.ChkEditAsma.isChecked){
            penyakit.add("Asma")
        }
        if(binding.ChkEditJantung.isChecked){
            penyakit.add("Jantung")
        }

        if (datagambar != null) {
            uploadPictFirebase(datagambar!!, "${nik}_${nama}")
        }

        val penyakit_string = penyakit.joinToString( "|" )

        val pasien = mutableMapOf<String, Any>()
        pasien["nik"] = nik
        pasien["nama"] = nama
        pasien["tgl_lahir"] = tgl_lahir
        pasien["jenis_kelamin"] = jk
        pasien["penyakit_bawaan"] = penyakit_string

        return pasien
    }


    private fun openCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            this.packageManager?.let {
                intent?.resolveActivity(it).also {
                    startActivityForResult(intent, REQ_CAM)
                }
            }
        }
    }

    fun showFoto(){
        val intent = intent
        val nik = intent.getStringExtra("nik").toString()
        val nama = intent.getStringExtra("nama").toString()

        val storageRef = FirebaseStorage.getInstance().reference.child("img_pasien/${nik}_${nama}.jpg")
        val localfile = File.createTempFile("tempImage", "jpg")
        storageRef.getFile(localfile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
            binding.BtnImgPasien.setImageBitmap(bitmap)
        }.addOnFailureListener{
            Log.e("foto ?", "gagal")
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CAM && resultCode == RESULT_OK) {
            datagambar = data?.extras?.get("data") as Bitmap
            binding.BtnImgPasien.setImageBitmap(datagambar)
        }
    }

    private fun uploadPictFirebase(img_bitmap : Bitmap, file_name: String) {
        val baos = ByteArrayOutputStream()
        val ref = FirebaseStorage.getInstance().reference.child("img_pasien/${file_name}.jpg")
        img_bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)

        val img = baos.toByteArray()
        ref.putBytes(img)
            .addOnCompleteListener{
                if (it.isSuccessful){
                    ref.downloadUrl.addOnCompleteListener { Task ->
                        Task.result.let { Uri ->
                            imgUri = Uri
                            binding.BtnImgPasien.setImageBitmap(img_bitmap)
                        }
                    }
                }
            }
    }


    private fun updatePasien(pasien: Pasien, newPasienMap: Map<String, Any>) =
        CoroutineScope(Dispatchers.IO).launch {
            val personQuery = db.collection("pasien")
                .whereEqualTo("nik", pasien.nik)
                .whereEqualTo("nama", pasien.nama)
                .whereEqualTo("tgl_lahir", pasien.tgl_lahir)
                .whereEqualTo("jenis_kelamin", pasien.jenis_kelamin)
                .whereEqualTo("peyakit_bawaan", pasien.penyakit_bawaan)
                .get()
                .await()
            if (personQuery.documents.isNotEmpty()) {
                for (document in personQuery) {
                    try {
                        db.collection("pasien").document(document.id).set(newPasienMap,
                            SetOptions.merge())
                    } catch (e:Exception){
                        withContext(Dispatchers.Main){
                            Toast.makeText(this@EditPasienActivity,e.message,Toast.LENGTH_LONG)
                                .show()
                        }
                    }
                }
            }
            else{
                withContext(Dispatchers.Main){ Toast.makeText(this@EditPasienActivity,
                    "No person matched the query.", Toast.LENGTH_LONG).show()
                }
            }
        }
}