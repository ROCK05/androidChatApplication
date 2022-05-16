package com.example.chatapplication.activities;

import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chatapplication.R;
import com.example.chatapplication.adapters.ChatAdapter;
import com.example.chatapplication.databinding.ActivityChatBinding;
import com.example.chatapplication.models.ChatMessage;
import com.example.chatapplication.models.User;
import com.example.chatapplication.utilities.Constant;
import com.example.chatapplication.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ChatActivity extends BaseActivity {

    private ActivityChatBinding binding;
    private User receiverUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private Toolbar toolbar;
    private Button send;
    private TextView inputMessage;
    private RecyclerView chatRecyclerView;
    private String conversionId = null;
    private Boolean isReceiverAvailable = false;
    private TextView textAvailability;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try{
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(R.layout.activity_chat);
            receiverUser = (User) getIntent().getSerializableExtra(Constant.KEY_USER);
            toolbar = (Toolbar) findViewById(R.id.userNameToolbar);
            toolbar.setTitle(receiverUser.name);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            send = findViewById(R.id.send);
            inputMessage = findViewById(R.id.inputMessage);
            chatRecyclerView = findViewById(R.id.chatRecyclerView);
            textAvailability = findViewById(R.id.textAvailability);
            init();
            listenMessages();
        }
        catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage().toString(), Toast.LENGTH_SHORT).show();
        }

        send.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {
                if(!TextUtils.isEmpty(inputMessage.getText().toString()) && !(inputMessage.getText().toString()).chars().allMatch(Character::isWhitespace))
                {
                    sendMessage();
                }
            }
        });
    }

    private void init()
    {
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                chatMessages,
                preferenceManager.getString(Constant.KEY_USER_ID)
        );
        chatRecyclerView.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private void sendMessage()
    {
        HashMap<String, Object>  message = new HashMap<>();
        message.put(Constant.KEY_SENDER_ID, preferenceManager.getString(Constant.KEY_USER_ID));
        message.put(Constant.KEY_RECEIVER_ID, receiverUser.id);
        message.put(Constant.KEY_MESSAGE, inputMessage.getText().toString());//binding.inputMessage.getText().toString());
        message.put(Constant.KEY_TIMESTAMP, new Date()); //Date class included
        database.collection(Constant.KEY_COLLECTION_CHAT).add(message);
        if(conversionId != null)
        {
            updateConversion(inputMessage.getText().toString());
        }
        else
        {
            HashMap<String, Object> conversion = new HashMap<>();
            conversion.put(Constant.KEY_SENDER_ID, preferenceManager.getString(Constant.KEY_USER_ID));
            conversion.put(Constant.KEY_SENDER_NAME, preferenceManager.getString(Constant.KEY_NAME));
            conversion.put(Constant.KEY_RECEIVER_ID, receiverUser.id);
            conversion.put(Constant.KEY_RECEIVER_NAME, receiverUser.name);
            conversion.put(Constant.KEY_LAST_MESSAGE, inputMessage.getText().toString());
            conversion.put(Constant.KEY_TIMESTAMP, new Date());
            addConversion(conversion);
        }
        inputMessage.setText(null);
    }

    private void listenAvailabilityOfReceiver()
    {
        database.collection(Constant.KEY_COLLECTION_USERS).document(
                receiverUser.id
        ).addSnapshotListener(ChatActivity.this, (value, error) ->{
           if(error != null) {
               return;
           }

           if(value != null)
           {
               if (value.getLong(Constant.KEY_AVAILABILITY) != null) {

                   int availability = Objects.requireNonNull(
                           value.getLong((Constant.KEY_AVAILABILITY))
                   ).intValue();
                   isReceiverAvailable = availability == 1;
               }
           }
           if(isReceiverAvailable)
           {
              textAvailability.setVisibility(View.VISIBLE);
           }
           else
           {
               textAvailability.setVisibility(View.GONE);
           }
        });
    }



    private void listenMessages()
    {
        database.collection(Constant.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constant.KEY_SENDER_ID, preferenceManager.getString(Constant.KEY_USER_ID))
                .whereEqualTo(Constant.KEY_RECEIVER_ID, receiverUser.id)
                .addSnapshotListener(eventListener);

        database.collection(Constant.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constant.KEY_SENDER_ID, receiverUser.id)
                .whereEqualTo(Constant.KEY_RECEIVER_ID, preferenceManager.getString(Constant.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
       if(error != null)
       {
           return;
       }

       if(value != null)
       {
           int count = chatMessages.size();
           for(DocumentChange documentChange : value.getDocumentChanges())
           {
               if(documentChange.getType() == DocumentChange.Type.ADDED) {
                   ChatMessage chatMessage = new ChatMessage();
                   chatMessage.senderId = documentChange.getDocument().getString(Constant.KEY_SENDER_ID);
                   chatMessage.receiverId = documentChange.getDocument().getString(Constant.KEY_RECEIVER_ID);
                   chatMessage.message = documentChange.getDocument().getString(Constant.KEY_MESSAGE);
                   chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constant.KEY_TIMESTAMP));
                   chatMessage.dateObject = documentChange.getDocument().getDate(Constant.KEY_TIMESTAMP);
                   chatMessages.add(chatMessage);
               }
           }
           Collections.sort(chatMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
           if(count == 0)
           {
               chatAdapter.notifyDataSetChanged();
           }
           else
           {
               chatAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
               chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
           }
           chatRecyclerView.setVisibility(View.VISIBLE);
       }

       if(conversionId == null)
       {
           checkForConversion();
       }
    };

    private String getReadableDateTime(Date date)
    {
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void addConversion(HashMap<String, Object> conversion)
    {
        database.collection(Constant.KEY_COLLECTION_CONVERSATIONS)
                .add(conversion)
                .addOnSuccessListener(documentReference -> conversionId = documentReference.getId());
    }

    private void updateConversion(String message)
    {
        DocumentReference documentReference =
                database.collection(Constant.KEY_COLLECTION_CONVERSATIONS).document(conversionId);

        documentReference.update(
          Constant.KEY_LAST_MESSAGE, message,
          Constant.KEY_TIMESTAMP, new Date()

        );
    }
    private void checkForConversion()
    {
        if(chatMessages.size() != 0)
        {
            checkForConversionRemotely(
                    preferenceManager.getString(Constant.KEY_USER_ID),
                    receiverUser.id
            );

            checkForConversionRemotely(
                    receiverUser.id,
                    preferenceManager.getString(Constant.KEY_USER_ID)

                    );
        }
    }

    private void checkForConversionRemotely(String senderId, String receiverId)
    {
        database.collection(Constant.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constant.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constant.KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(conversionOnCompleteListener);

    }

    private final OnCompleteListener<QuerySnapshot> conversionOnCompleteListener = task -> {
        if(task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0){
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversionId = documentSnapshot.getId();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        listenAvailabilityOfReceiver();
    }
}