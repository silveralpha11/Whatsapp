package com.adith.smash.x.whatsappclone.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.adith.smash.x.whatsappclone.R
import com.adith.smash.x.whatsappclone.activity.ConversationActivity
import com.adith.smash.x.whatsappclone.adapter.ChatsAdapter
import com.adith.smash.x.whatsappclone.listener.ChatClickListener
import com.adith.smash.x.whatsappclone.listener.FailureCallback
import com.adith.smash.x.whatsappclone.util.Chat
import com.adith.smash.x.whatsappclone.util.DATA_CHATS
import com.adith.smash.x.whatsappclone.util.DATA_USERS
import com.adith.smash.x.whatsappclone.util.DATA_USER_CHATS
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_chats.*



class ChatsFragment : Fragment(), ChatClickListener {

    private var chatsAdapter = ChatsAdapter(arrayListOf())
    private val firebaseDb = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private var failureCallback: FailureCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chats, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (userId.isNullOrEmpty()){
            failureCallback?.onUserError()
        }

    }

    fun setFailureCallback(listener: FailureCallback){
        failureCallback = listener
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chatsAdapter.setOnItemClickListener(this)
        rv_chats.apply {
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(context)
            adapter = chatsAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        firebaseDb.collection(DATA_USERS).document(userId!!)
            .addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException == null){
                    refreshChat()
                }
            }

    }

    private fun refreshChat() {
        firebaseDb.collection(DATA_USERS).document(userId!!).get()
            .addOnSuccessListener {
                if (it.contains(DATA_USER_CHATS)){
                    val patners = it[DATA_USER_CHATS]
                    val chats = arrayListOf<String>()

                    for (patner in (patners as HashMap<String, String>).keys){
                        if (patners[patner] != null){ // melakukan pengulangan untuk memperbaharui data dalam userChats
                            chats.add(patners[patner]!!)
                        }
                    }
                    chatsAdapter.updateChats(chats)
                }
            }
            .addOnFailureListener {
                it.printStackTrace()
            }
    }

    override fun onChatClicked(
        chatId: String?,
        otherUserId: String?,
        chatsImageUrl: String?,
        chatsName: String?
    ) {
        startActivity(
            ConversationActivity.newIntent(
                context,
                chatId,
                chatsImageUrl,
                otherUserId,
                chatsName
            )
        )
    }

    fun newChat(patnerId: String){
        firebaseDb.collection(DATA_USERS).document(userId!!).get()
            .addOnSuccessListener { userDocument ->
                // untuk menampung data user chat
                val userChatPatners = hashMapOf<String, String>()
                if (userDocument[DATA_USER_CHATS] != null &&
                        userDocument[DATA_USER_CHATS] is HashMap<*, *>){
                    val userDocumentMap = userDocument[DATA_USER_CHATS] as HashMap<String, String>
                    if (userDocumentMap.containsKey(patnerId)){
                        return@addOnSuccessListener
                    } else {
                        userChatPatners.putAll(userDocumentMap)
                    }
                }

                firebaseDb.collection(DATA_USERS)
                    .document(patnerId)
                    .get()
                    .addOnSuccessListener { patnerDocument ->
                        val patnerChatPatners = hashMapOf<String, String>()
                        if (patnerDocument[DATA_USER_CHATS] != null &&
                                patnerDocument[DATA_USER_CHATS] is HashMap<*,*>) {
                            val patnerDocumentMap = patnerDocument[DATA_USER_CHATS] as HashMap<String, String>
                            patnerChatPatners.putAll(patnerDocumentMap)
                        }

                        val chatParticipants = arrayListOf(userId, patnerId)
                        val chat = Chat(chatParticipants)
                        val chatRef = firebaseDb.collection(DATA_CHATS).document()
                        val userRef = firebaseDb.collection(DATA_USERS).document(userId)
                        val patnerRef = firebaseDb.collection(DATA_USERS).document(patnerId)
                        userChatPatners[patnerId] = chatRef.id
                        userChatPatners[userId] = chatRef.id

                        val batch = firebaseDb.batch()
                        batch.set(chatRef, chat)
                        batch.update(userRef, DATA_USER_CHATS, userChatPatners)
                        batch.update(patnerRef, DATA_USER_CHATS, patnerChatPatners)
                        batch.commit()
                    }
                    .addOnFailureListener {
                        it.printStackTrace()
                    }
            }

            .addOnFailureListener {
                it.printStackTrace()
            }
    }


}