package com.example.chatapplication.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapplication.utilities.Constant;
import com.example.chatapplication.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class BaseActivity extends AppCompatActivity {

    private DocumentReference documentReference;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager preferenceManager = new PreferenceManager(getApplicationContext());
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        documentReference = database.collection(Constant.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constant.KEY_USER_ID));

    }

    @Override
    protected void onPause() {
        super.onPause();
        documentReference.update(Constant.KEY_AVAILABILITY, 0);

    }

    @Override
    protected void onResume() {
        super.onResume();
        documentReference.update(Constant.KEY_AVAILABILITY, 1);
    }
}
