package com.cloudiera.collegeconexion.LogIn;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Config;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cloudiera.collegeconexion.Home.HomeActivity;
import com.cloudiera.collegeconexion.R;
import com.cloudiera.collegeconexion.Utils.CheckInputs;
import com.cloudiera.collegeconexion.Utils.Database.UserDatabaseHelper;
import com.google.android.gms.common.util.SharedPreferencesUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.HashMap;
import java.util.Map;


public class LogInActivity extends AppCompatActivity {

    private Context mContext = LogInActivity.this;
    private static final String TAG = "LogInActivity";

    // FIREBASE
    private FirebaseAuth mAuth;
    private FirebaseFirestore firebaseFirestore;
    private DatabaseReference mUserDatabase;

    // XML FIELDS
    private EditText mEmail, mPassword;
    private TextView mForgotPassword, mSignUp, mSignInButton;
    private RelativeLayout mProgress;

    //OTHERS
    UserDatabaseHelper userHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        // PREVENT KEYBOARD FROM OPNEING WHEN ACTIVITY IS STARTED
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // INITIALIZE ALL THE VARIABLES AND FIELDS USED IN THE ACTIVITY
        initializeFields();

        // SETUP CLICK LISTENERS FOR REQUIRED FIELDS
        setupListeners();


    }

    private void initializeFields() {

        // INITIALIZE FIREABSE VARIABLES
        mAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        mUserDatabase = FirebaseDatabase.getInstance()
                .getReference()
                .child(getString(R.string.dbname_verified_user));

        // INITIALIZE XML FIELDS
        mEmail = findViewById(R.id.emailInput);
        mPassword = findViewById(R.id.passwordInput);
        mForgotPassword = findViewById(R.id.forgotPassword);
        mSignUp = findViewById(R.id.newAccount);
        mProgress = findViewById(R.id.progress_login);
        mSignInButton = findViewById(R.id.logInButton);

        //OTHERS
        userHelper = new UserDatabaseHelper(mContext);

    }

   //   THIS FUNCTION SETUP LISTENERS FOR DIFFERENT FIELDS AND VAIRABLES
    private void setupListeners() {

        //  SETUP SUBMIT BUTTON
        mSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressSignIn();
            }
        });

        //  ENABLE ENTER BUTTON ON KEYBOARD TO CALL SUBMIT BUTTON
        mPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    Log.i(TAG,"Enter pressed");
                    progressSignIn();

                }
                return false;
            }
        });

        //  SETUP SIGN UP BUTTON
        mSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(mContext, NewAccountActivity.class));
            }
        });

        //  SETUP FORGET PASSWORD
        mForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, ForgotPasswordActivity.class);
                startActivity(i);
            }
        });

        // REDIRECT USER TO THE HOME ACTIVITY IF ALREADY SIGNED IN
        if (mAuth.getCurrentUser() != null) {
            Intent intent = new Intent(mContext, HomeActivity.class);
            startActivity(intent);
            finish();
        }
    }

    // THIS FUNCTION IS USED TO VALIDATES THE INPUTS
    private boolean validateInputs(String email, String password) {

        // CHECK WHETHER THE INPUTS ARE NULL ARE NOT
        if (TextUtils.isEmpty(email) && TextUtils.isEmpty(password)) {
            Toast.makeText(mContext, "Fields are Empty!", Toast.LENGTH_SHORT).show();
            return false;
        }

        // CHECK WHETHER THE FORMAT OF EMAIL IS RIGHT OR NOT
        if (!CheckInputs.isEmailValid(email)) {
            Toast.makeText(mContext, "Invalid email", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    //THIS FUNCTION IS USED TO PERFORM SIGNIN PROCESS
    private void progressSignIn(){

        String email = mEmail.getText().toString();
        String password = mPassword.getText().toString();

        // VALIDATING ALL THE INPUTS
        if (validateInputs(email,password)) {

            //SETUP PROGRESS BAR
            mProgress.setVisibility(View.VISIBLE);

            //SIGNIN PROCESS STARTS HERE
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            Log.w(TAG, "signInWithEmail: Logged In successful");

                            // GET FIREBASE INSTANCE ID
                            FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                                @Override
                                public void onComplete(@NonNull Task<InstanceIdResult> task) {

                                    // HANDLE TASK FAILURE
                                    if (!task.isSuccessful()) {
                                        Log.w(TAG, "getInstanceId failed", task.getException());
                                        return;
                                    }

                                    final String token_id = task.getResult().getToken();
                                    final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                                    //  UPDATING TOKEN VALUES TO THE DATABASE
                                    final Map<String, Object> tokenMap = new HashMap<>();
                                    tokenMap.put("token_ids", FieldValue.arrayUnion(token_id));

                                    firebaseFirestore.collection("Users").document(userId)
                                            .update(tokenMap)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {

                                                    // RETRIVING ALL THE VALUES OF USER TO STORE IT LOCALLY
                                                    firebaseFirestore.collection("Users").document(userId)
                                                            .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                        @Override
                                                        public void onSuccess(DocumentSnapshot documentSnapshot) {

                                                            // STORING ALL THE FIELDS LOCALLY
                                                            String username = documentSnapshot.getString("username");
                                                            String fname = documentSnapshot.getString("fname");
                                                            String lname = documentSnapshot.getString("lname");
                                                            String email = documentSnapshot.getString("email");
                                                            String image = documentSnapshot.getString("image");
                                                            String password = mPassword.getText().toString();
                                                            String college_id = documentSnapshot.getString("college_id");
                                                            String bio = documentSnapshot.getString("bio");

                                                            userHelper.insertContact(username, fname,lname, email,
                                                                    image, password, college_id, bio);

                                                            // DISABLE PROGRESS BAR
                                                            mProgress.setVisibility(View.GONE);

                                                            // REDIRECT TO HOMEACTIVITY
                                                            Intent intent = new Intent(LogInActivity.this, HomeActivity.class);
                                                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                            startActivity(intent);
                                                            finish();


                                                        }
                                                    }).addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            mAuth.signOut();
                                                            mProgress.setVisibility(View.GONE);
                                                            Log.e("Error", ".." + e.getMessage());
                                                            Toast.makeText(mContext, "Something Went Wrong!", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });

                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    mAuth.signOut();
                                                    mProgress.setVisibility(View.GONE);
                                                    Log.e(TAG, "onFailure: " + e.getMessage() );
                                                    Toast.makeText(mContext, "Something went wrong!", Toast.LENGTH_SHORT).show();
                                                }
                                            });

                                }
                            });


//                            mUserDatabase.addValueEventListener(new ValueEventListener() {
//                                @Override
//                                public void onDataChange(DataSnapshot dataSnapshot) {
//                                    if (dataSnapshot.hasChild(userId)) {
//
//                                        mUserDatabase.child(userId).child("device_token").setValue(deviceToken)
//                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
//                                                    @Override
//                                                    public void onComplete(@NonNull Task<Void> task) {
//
//                                                        if (task.isSuccessful()) {
//
//                                                            Log.d(TAG, "onComplete: Success ! Email is Verified");
//                                                            mProgress.setVisibility(View.GONE);
//                                                            Intent intent = new Intent(LogInActivity.this, HomeActivity.class);
//                                                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                                                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                                                            startActivity(intent);
//
//                                                        } else {
//
//                                                            Log.d(TAG, "onComplete: Something Went Wrong ");
//                                                        }
//                                                    }
//                                                });
//                                    } else {
//                                        Log.d(TAG, "onDataChange:  Intent to Home Activity");
//                                        mProgress.setVisibility(View.GONE);
//                                        Intent intent = new Intent(LogInActivity.this, HomeActivity.class);
//                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                                        startActivity(intent);
//                                    }
//
//                                }
//
//                                @Override
//                                public void onCancelled(DatabaseError databaseError) {
//
//                                }
//                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "signInWithEmail:failed", e);

                            Toast.makeText(LogInActivity.this, R.string.auth_failed, Toast.LENGTH_LONG).show();
                            mProgress.setVisibility(View.GONE);
                        }
                    });

        }

    }

}
