package com.example.chatapplication.activities;

import static java.lang.Thread.sleep;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chatapplication.R;
import com.example.chatapplication.MainActivity;
import com.example.chatapplication.utilities.Constant;
import com.example.chatapplication.utilities.PreferenceManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class signUp_activity extends AppCompatActivity {

    TextView alreadyHaveAccount;
    Button google,signUpButton;
    private long pressedTime;

    private PreferenceManager preferenceManager;
    private FirebaseAuth mAuth;
    private TextView NAME, EMAIL, PASSWORD;
    private String name, email, password;
    FirebaseUser userG;
    GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        //Google Signup
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        alreadyHaveAccount = findViewById(R.id.alreadyHaveAccount);
        google = findViewById(R.id.googleButton2);
        NAME = findViewById(R.id.editTextTextPersonName);
        EMAIL = findViewById(R.id.email);
        PASSWORD = findViewById(R.id.password);
        signUpButton = findViewById(R.id.signUpButton);
        mAuth = FirebaseAuth.getInstance();
        preferenceManager = new PreferenceManager(getApplicationContext());
        alreadyHaveAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(),signIn_activity.class);
                startActivity(intent);
                finish();
            }
        });

        google.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isValidSignUpDetails())
                {
                    String email = EMAIL.getText().toString();
                    String password = PASSWORD.getText().toString();
                    email = email.equals("") ? "null" : email;
                    password = password.equals("") ? "null" : password;
                    mAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(getApplicationContext(), "Account created successfully", Toast.LENGTH_SHORT).show();
                                        signUp();
                                    }
                                    else {
                                        Toast.makeText(getApplicationContext(), task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                }

                            });
                }
            }
        });


    }

    @Override
    public void onBackPressed() {
        if (pressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed();
            finish();
        } else {
            Toast.makeText(getBaseContext(), "Press back again to exit", Toast.LENGTH_SHORT).show();
        }
        pressedTime = System.currentTimeMillis();
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void signUp(){
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constant.KEY_NAME,NAME.getText().toString());
        user.put(Constant.KEY_EMAIL, EMAIL.getText().toString());
        user.put("displayName",NAME.getText().toString());
        database.collection(Constant.KEY_COLLECTION_USERS).document(mAuth.getCurrentUser().getUid())
                .set(user)
                .addOnSuccessListener(documentReference -> {
                    preferenceManager.putBoolean(Constant.KEY_IS_SIGNED_IN, true);
                    preferenceManager.putString(Constant.KEY_USER_ID, mAuth.getCurrentUser().getUid());
                    preferenceManager.putString(Constant.KEY_NAME, NAME.getText().toString());
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(exception -> {
                    showToast(exception.getMessage());
                });

    }

    private Boolean isValidSignUpDetails() {

        name = NAME.getText().toString();
        email = EMAIL.getText().toString();
        password = PASSWORD.getText().toString();

        if(TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showToast("Please fill all required fields!");
            return false;
        }
        else
            return true;
    }

    //Google
    int RC_SIGN_IN = 65;
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);

                Log.d("TAG", "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w("TAG", "Google sign in failed", e);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Toast.makeText(getApplicationContext(), "Logged In with Google", Toast.LENGTH_SHORT).show();
                            FirebaseFirestore database = FirebaseFirestore.getInstance();
                            userG = FirebaseAuth.getInstance().getCurrentUser();
                            HashMap<String, Object> user = new HashMap<>();
                            user.put(Constant.KEY_NAME, userG.getDisplayName());
                            user.put(Constant.KEY_EMAIL,userG.getEmail().toString());
                            user.put("displayName",userG.getDisplayName());
                            //user.put(Constant.KEY_PASSWORD, PASSWORD.getText().toString());
                            database.collection(Constant.KEY_COLLECTION_USERS).document(mAuth.getCurrentUser().getUid())
                                    .set(user)
                                    .addOnSuccessListener(documentReference -> {
                                        FirebaseUser use = mAuth.getCurrentUser();
                                        preferenceManager.putBoolean(Constant.KEY_IS_SIGNED_IN, true);
                                        preferenceManager.putString(Constant.KEY_USER_ID,mAuth.getCurrentUser().getUid());
                                        preferenceManager.putString(Constant.KEY_NAME,userG.getDisplayName());
                                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .addOnFailureListener(exception -> {
                                        Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_SHORT).show();
                                    });

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("TAG", "signInWithCredential:failure", task.getException());

                        }
                    }
                });
    }


}