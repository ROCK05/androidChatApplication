package com.example.chatapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.example.chatapplication.activities.signUp_activity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class splashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        Thread thread = new Thread(){
            public void run()
            {
                try{
                    sleep(750);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
                finally {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                    if(user == null) {
                        Intent intent1;
                        intent1 = new Intent(splashScreen.this, signUp_activity.class);
                        startActivity(intent1);
                        finish();

                    } else {
                        Intent intent2;
                        intent2 = new Intent(splashScreen.this, MainActivity.class);
                        startActivity(intent2);
                        finish();
                    }
                }
            }

        };
        thread.start();
    }

}