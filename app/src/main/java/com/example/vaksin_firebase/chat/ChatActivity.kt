package com.example.vaksin_firebase.chat


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vaksin_firebase.R
import com.example.vaksin_firebase.databinding.ActivityChatBinding
import com.google.api.Distribution.BucketOptions.Linear
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.QuerySnapshot
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.SimpleTimeZone

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var database: DatabaseReference

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatArrayList: ArrayList<Chat>
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        chatRecyclerView = binding.chatListView
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.setHasFixedSize(true)

        chatArrayList = arrayListOf()
        chatAdapter = ChatAdapter(chatArrayList)

        chatRecyclerView.adapter = chatAdapter

        load_chat_data()

        binding.BtnSendChat.setOnClickListener{
            val message = binding.TxtChatBox.text.toString()
            val username = firebaseAuth.currentUser?.email.toString()
            Log.e("chat", message)
            send_chat(username, message)
        }

    }

    private fun load_chat_data() {
        database = FirebaseDatabase.getInstance().getReference("chat_db")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    chatArrayList.clear()
                    for (chatSnapshot in snapshot.children) {
                        val chat_data = chatSnapshot.getValue(Chat::class.java)
                        chatArrayList.add(chat_data!!)
                    }
                    chatRecyclerView.adapter = ChatAdapter(chatArrayList)
                }
            }
            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun send_chat (username: String, message: String) {
        val time = Calendar.getInstance().time
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val current_time = formatter.format(time).toString()

        val chat = Chat(username, message, current_time)

        database = FirebaseDatabase.getInstance().getReference("chat_db")

        database.child(current_time).setValue(chat).addOnSuccessListener {
            binding.TxtChatBox.text.clear()

        }.addOnFailureListener {
            Toast.makeText(this, "Kirim Chat gagal", Toast.LENGTH_SHORT).show()
            Log.e("gagal", "kirim gagal")
        }
    }
}