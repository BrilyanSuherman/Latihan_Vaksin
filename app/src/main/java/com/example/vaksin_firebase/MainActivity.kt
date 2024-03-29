package com.example.vaksin_firebase

import android.content.ClipData.Item
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vaksin_firebase.auth.SettingsActivity
import com.example.vaksin_firebase.chat.ChatActivity
import com.example.vaksin_firebase.databinding.ActivityAddPasienBinding
import com.example.vaksin_firebase.databinding.ActivityMainBinding
import com.example.vaksin_firebase.pasien.AddPasienActivity
import com.example.vaksin_firebase.pasien.Pasien
import com.example.vaksin_firebase.pasien.PasienAdapter
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var pasienRecylerView : RecyclerView
    private lateinit var pasienArrayList: ArrayList<Pasien>
    private lateinit var pasienAdapter: PasienAdapter
    private lateinit var db: FirebaseFirestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pasienRecylerView = binding.makananListView
        pasienRecylerView.layoutManager = LinearLayoutManager(this)
        pasienRecylerView.setHasFixedSize(true)

        pasienArrayList = arrayListOf()
        pasienAdapter = PasienAdapter(pasienArrayList)

        pasienRecylerView.adapter = pasienAdapter

        binding.btnAddPasien.setOnClickListener {
            val intentMain = Intent(this, AddPasienActivity::class.java)
            startActivity(intentMain)
        }

        load_data()

        binding.txtSearchPasien.addTextChangedListener (object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                val keyword = binding.txtSearchPasien.text.toString()
                if(keyword.isNotEmpty()){
                    search_data(keyword)
                }
                else{
                    load_data()
                }
            }
            override fun afterTextChanged(p0: Editable?) {}
        })

        swipeDelete()

        binding.bottomNavigation.setOnItemReselectedListener{
            when(it.itemId) {
                R.id.nav_bottom_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_bottom_setting -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_bottom_chat -> {
                    val intent = Intent(this, ChatActivity::class.java)
                    startActivity(intent)
                }
            }
            true
        }

    }

    private fun load_data(){
        pasienArrayList.clear()
        db = FirebaseFirestore.getInstance()
        db.collection("pasien").
                addSnapshotListener(object : EventListener<QuerySnapshot> {
                    override fun onEvent(
                        value: QuerySnapshot?,
                        error: FirebaseFirestoreException?
                    ) {
                        if (error !=null){
                            Log.e("Firestore Error", error.message.toString())
                            return
                        }
                        for (dc: DocumentChange in value?.documentChanges!!){
                            if (dc.type == DocumentChange.Type.ADDED)
                                pasienArrayList.add(dc.document.toObject(Pasien::class.java))
                        }
                        pasienAdapter.notifyDataSetChanged()
                    }
                })
    }


    private fun search_data(keyword :String) {
        pasienArrayList.clear()

        db = FirebaseFirestore.getInstance()

        val query = db.collection("pasien")
            .orderBy("nama")
            .startAt(keyword)
            .get()
        query.addOnSuccessListener {
            pasienArrayList.clear()
            for (document in it) {
                pasienArrayList.add(document.toObject(Pasien::class.java))
            }
            pasienAdapter.notifyDataSetChanged()
        }
    }

    private fun deletePasien(pasien: Pasien, doc_id: String) {
        val builder =AlertDialog.Builder(this)
        builder.setMessage("Apakah${pasien.nama} ingin dihapus ?")
            .setCancelable(false)
            .setPositiveButton("yes") {dialog, id ->
                lifecycleScope.launch { db.collection("pasien")
                    .document(doc_id).delete()
                    deleteFoto("img_pasien/${pasien.nik}_${pasien.nama}.jpg")

                Toast.makeText(applicationContext,pasien.nama.toString() + "is Deleted",
                Toast.LENGTH_LONG)
                    .show()
                load_data()
                }
            }
            .setNegativeButton("NO") {dialog, id ->
                dialog.dismiss()
                load_data()
            }
        val alert = builder.create()
        alert.show()
    }

    private fun swipeDelete(){
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0,ItemTouchHelper.RIGHT){
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                lifecycleScope.launch {
                    val pasien = pasienArrayList[position]
                    val personQuery = db.collection("pasien")
                        .whereEqualTo("nik", pasien.nik)
                        .whereEqualTo("nama", pasien.nama)
                        .whereEqualTo("jenis_kelamin", pasien.jenis_kelamin)
                        .whereEqualTo("tgl_lahir", pasien.tgl_lahir)
                        .whereEqualTo("penyakit_bawaan", pasien.penyakit_bawaan)
                        .get()
                        .await()

                    if (personQuery.documents.isNotEmpty()) {
                        for (document in personQuery) {
                            try {
                                deletePasien(pasien,document.id)
                                load_data()
                            }catch (e: Exception) {
                                withContext(Dispatchers.Main){
                                    Toast.makeText(
                                        applicationContext,
                                        e.message.toString(),Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                    else{
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                applicationContext,"User yang ingin anda hapus tidak ditemukan",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                }
            }
        }).attachToRecyclerView(pasienRecylerView)
    }

    private fun deleteFoto(file_name: String) {
        val storage = Firebase.storage
        val storageRef = storage.reference
        val deleteFileRef = storageRef.child(file_name)
        if (deleteFileRef != null) {
            deleteFileRef.delete().addOnSuccessListener {
                Log.e("deleted", "success")
            }.addOnFailureListener {
                Log.e("deleted", "failed")
            }
        }
    }


}