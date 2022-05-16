package com.example.chatapplication.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.chatapplication.R;
import com.example.chatapplication.adapters.UserAdapter;
import com.example.chatapplication.databinding.ActivityUsersBinding;
import com.example.chatapplication.listeners.UserListener;
import com.example.chatapplication.models.User;
import com.example.chatapplication.utilities.Constant;
import com.example.chatapplication.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UsersActivity extends BaseActivity implements UserListener {

    private ActivityUsersBinding binding;
    private PreferenceManager preferenceManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Toolbar toolbar = (Toolbar) findViewById(R.id.selectUserToolbar);
        toolbar.setTitle("Select User");
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        preferenceManager = new PreferenceManager(getApplicationContext());
        getUsers();
    }

    private void getUsers()
    {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constant.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {
                    String currentUserId = preferenceManager.getString(Constant.KEY_USER_ID);

                    if(task.isSuccessful() && task.getResult() != null)
                    {
                        List<User> users = new ArrayList<>();
                        for(QueryDocumentSnapshot queryDocumentSnapshot : task.getResult())
                        {
                             if(currentUserId.equals(queryDocumentSnapshot.getId()))
                             {
                                 continue;
                             }
                             User user = new User();
                             user.name = queryDocumentSnapshot.getString(Constant.KEY_NAME);
                             user.email = queryDocumentSnapshot.getString(Constant.KEY_EMAIL);
                             user.id = queryDocumentSnapshot.getId();
                             users.add(user);
                        }
                        if(users.size() > 0)
                        {
                            UserAdapter userAdapter = new UserAdapter(users,this);
                            binding.usersRecyclerView.setAdapter(userAdapter);
                            binding.usersRecyclerView.setVisibility(View.VISIBLE);
                        }
                        else
                        {
                            Toast.makeText(getApplicationContext(), "Error: No user Found!", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(), task.getException().getMessage().toString(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

   @Override
    public void OnUserClicked(User user) {
        try {
            Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
            intent.putExtra(Constant.KEY_USER, user);
            startActivity(intent);
        }
        catch (Exception e)
        {
            Toast.makeText(getApplicationContext(), e.getMessage().toString(), Toast.LENGTH_SHORT).show();
        }
    }
}