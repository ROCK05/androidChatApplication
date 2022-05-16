package com.example.chatapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class signIn_activity extends AppCompatActivity {

    private long pressedTime;
    private TextView createNewAcc;
    private Button google, signInButton;
    private PreferenceManager preferenceManager;
    private String email, password;
    private TextView EMAIL, PASSWORD;
    private FirebaseAuth mAuth;
    FirebaseUser userG;
    GoogleSignInClient mGoogleSignInClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        //Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        preferenceManager = new PreferenceManager(getApplicationContext());
        createNewAcc = (TextView) findViewById(R.id.createNewAccount);
        google = (Button) findViewById(R.id.googleButton1);
        signInButton = findViewById(R.id.signInButton);
        mAuth = FirebaseAuth.getInstance();
        EMAIL = findViewById(R.id.emailLogin);
        PASSWORD = findViewById(R.id.passwordLogin);
        createNewAcc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), signUp_activity.class);
                startActivity(intent);
                finish();
            }
        });

        google.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signInGoogle();
            }
        });

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isValidSignInDetails())
                    signIn();
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

    private void signIn()
    {

        mAuth.signInWithEmailAndPassword(EMAIL.getText().toString(), PASSWORD.getText().toString())
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()) {
                            showToast("Logged in successfully");
                            FirebaseFirestore database = FirebaseFirestore.getInstance();
                            database.collection(Constant.KEY_COLLECTION_USERS)
                                .whereEqualTo(Constant.KEY_EMAIL,EMAIL.getText().toString())
                                .get()
                                .addOnCompleteListener(task1 -> {
                                    if(task1.isSuccessful() && task1.getResult() != null
                                    && task1.getResult().getDocuments().size() > 0)
                                    {
                                        DocumentSnapshot documentSnapshot = task1.getResult().getDocuments().get(0);
                                        preferenceManager.putBoolean(Constant.KEY_IS_SIGNED_IN, true);
                                        preferenceManager.putString(Constant.KEY_USER_ID,documentSnapshot.getId());
                                        preferenceManager.putString(Constant.KEY_NAME, documentSnapshot.getString(Constant.KEY_NAME));
                                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }
                                    else
                                    {
                                        showToast("Unable to sign in");
                                    }
                                });

                        }
                        else {
                            Toast.makeText(getApplicationContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
       }
    private void showToast(String message)
    {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }


    private Boolean isValidSignInDetails()
    {

        email = EMAIL.getText().toString();
        password = PASSWORD.getText().toString();

        if(TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showToast("Please fill all required fields!");
            return false;
        }
        else
            return true;
    }


    //Google
    int RC_SIGN_IN = 65;
    private void signInGoogle() {
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
                            user.put(Constant.KEY_NAME, userG.getDisplayName());//"Temp");//Objects.requireNonNull(mAuth.getCurrentUser()).getDisplayName());
                            user.put(Constant.KEY_EMAIL,userG.getEmail().toString());////mAuth.getCurrentUser().getEmail());
                            user.put("displayName",userG.getDisplayName());
                            database.collection(Constant.KEY_COLLECTION_USERS).document(userG.getUid())
                                    .set(user)
                                    .addOnSuccessListener(documentReference -> {
                                        FirebaseUser use = mAuth.getCurrentUser();
                                        preferenceManager.putBoolean(Constant.KEY_IS_SIGNED_IN, true);
                                        preferenceManager.putString(Constant.KEY_USER_ID,mAuth.getCurrentUser().getUid());//documentReference.getId());
                                        preferenceManager.putString(Constant.KEY_NAME,userG.getDisplayName());//use.getDisplayName());
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