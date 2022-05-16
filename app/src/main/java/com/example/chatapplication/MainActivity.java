package com.example.chatapplication;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import com.example.chatapplication.activities.BaseActivity;
import com.example.chatapplication.activities.ChatActivity;
import com.example.chatapplication.activities.UsersActivity;
import com.example.chatapplication.activities.signUp_activity;
import com.example.chatapplication.adapters.RecentConversationAdapter;
import com.example.chatapplication.listeners.ConversionListener;
import com.example.chatapplication.models.ChatMessage;
import com.example.chatapplication.models.User;
import com.example.chatapplication.utilities.Constant;
import com.example.chatapplication.utilities.PreferenceManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends BaseActivity implements ConversionListener {

    private FloatingActionButton NewChat;
    private PreferenceManager preferenceManager;
    private List<ChatMessage> conversations;
    private RecentConversationAdapter conversationsAdapter;
    private FirebaseFirestore database;
    private RecyclerView conversationsRecyclerView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NewChat = findViewById(R.id.newChat);
        preferenceManager = new PreferenceManager(getApplicationContext());
        conversationsRecyclerView = findViewById(R.id.conversationsRecyclerView);
        Toolbar toolbar = (Toolbar) findViewById(R.id.tempActivityToolbar);
        toolbar.setTitle("AUR BATAO!");
        toolbar.setTitleTextAppearance(this, R.style.RobotoBoldTextAppearance);
        setSupportActionBar(toolbar);
        NewChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), UsersActivity.class);
                startActivity(intent);
            }
        });
        init();
        listenConversation();
    }

    private void listenConversation()
    {
        database.collection(Constant.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constant.KEY_SENDER_ID, preferenceManager.getString(Constant.KEY_USER_ID))
                .addSnapshotListener(eventListener);

        database.collection(Constant.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constant.KEY_RECEIVER_ID, preferenceManager.getString(Constant.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }
    private final EventListener<QuerySnapshot> eventListener = (value, error)->{
      if(error != null) {
          return;
      }
      if(value != null)
      {
          for(DocumentChange documentChange : value.getDocumentChanges()){
              if(documentChange.getType() == DocumentChange.Type.ADDED){
                  String senderId = documentChange.getDocument().getString(Constant.KEY_SENDER_ID);
                  String receiverId = documentChange.getDocument().getString(Constant.KEY_RECEIVER_ID);
                  ChatMessage chatMessage = new ChatMessage();
                  chatMessage.senderId = senderId;
                  chatMessage.receiverId = receiverId;
                  if(preferenceManager.getString(Constant.KEY_USER_ID).equals(senderId)){
                      chatMessage.conversionName = documentChange.getDocument().getString(Constant.KEY_RECEIVER_NAME);
                      chatMessage.conversionId = documentChange.getDocument().getString(Constant.KEY_RECEIVER_ID);

                  }
                  else{
                      chatMessage.conversionName = documentChange.getDocument().getString(Constant.KEY_SENDER_NAME);
                      chatMessage.conversionId = documentChange.getDocument().getString(Constant.KEY_SENDER_ID);
                  }
                  chatMessage.message = documentChange.getDocument().getString(Constant.KEY_LAST_MESSAGE);
                  chatMessage.dateObject = documentChange.getDocument().getDate(Constant.KEY_TIMESTAMP);
                  conversations.add(chatMessage);
              }
              else if(documentChange.getType() == DocumentChange.Type.MODIFIED)
              {
                  for(int i = 0; i < conversations.size(); i++)
                  {
                      String senderId = documentChange.getDocument().getString(Constant.KEY_SENDER_ID);
                      String receiverId = documentChange.getDocument().getString(Constant.KEY_RECEIVER_ID);
                      if(conversations.get(i).senderId.equals(senderId) && conversations.get(i).receiverId.equals(receiverId))
                      {
                          conversations.get(i).message = documentChange.getDocument().getString(Constant.KEY_LAST_MESSAGE);
                          conversations.get(i).dateObject = documentChange.getDocument().getDate(Constant.KEY_TIMESTAMP);
                          break;

                      }
                  }
              }
          }
          Collections.sort(conversations, (obj1, obj2) -> obj2.dateObject.compareTo(obj1.dateObject));
          conversationsAdapter.notifyDataSetChanged();
          conversationsRecyclerView.smoothScrollToPosition(0);
          conversationsRecyclerView.setVisibility(View.VISIBLE);
      }
    };

    private void init()
    {
        conversations = new ArrayList<>();
        conversationsAdapter = new RecentConversationAdapter(conversations, this);
        conversationsRecyclerView.setAdapter(conversationsAdapter);
        database = FirebaseFirestore.getInstance();
    }

    @Override
    public void onConversionClick(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constant.KEY_USER, user);
        startActivity(intent);
    }

    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(this, signUp_activity.class);
                startActivity(intent);
                finish();
                break;
        }
        return true;
    }
}