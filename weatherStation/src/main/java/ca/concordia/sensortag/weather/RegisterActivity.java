package ca.concordia.sensortag.weather;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.List;

import ca.concordia.sensortag.weather.model.User;
import ca.concordia.sensortag.weather.helper.DBHandler;
import ca.concordia.sensortag.weather.helper.SessionManager;


/**
 * Created by Karimi on 2017-03-25.
 */

public class RegisterActivity extends Activity {

    private Button btnRegister;
    private Button btnLinkToLogin;
    private EditText inputFullName;
    private EditText inputUsername;
    private EditText inputPassword;
    private ProgressDialog pDialog;
    private SessionManager session;
    private DBHandler db;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);


        inputFullName = (EditText) findViewById(R.id.name);
        inputUsername = (EditText) findViewById(R.id.username);
        inputPassword = (EditText) findViewById(R.id.password);
        btnRegister = (Button) findViewById(R.id.btnRegister);
        btnLinkToLogin = (Button) findViewById(R.id.btnLinkToLoginScreen);

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        // Session manager
        session = new SessionManager(getApplicationContext());

        //SQLite database handler
        db = new DBHandler(getApplicationContext());

        // Check if user is already logged in or not
        if (session.isLoggedIn()) {
            gotoMainActivity();
        }

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String name = inputFullName.getText().toString().trim();
                String username = inputUsername.getText().toString().trim();
                String password = inputPassword.getText().toString().trim();

                if (!name.isEmpty() && !username.isEmpty() && !password.isEmpty()) {
                    User user = new User(username, name, password);
                    registerUser(user);
                } else {
                    Toast.makeText(getApplicationContext(), "Please enter your details!", Toast.LENGTH_LONG).show();
                }
            }
        });

        // Link to Login Screen
        btnLinkToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gotoLoginActivity();
            }
        });


    }

    private void registerUser(User user) {
        pDialog.setMessage("Registering...");
        showDialog();

        boolean usernameUnique = true;

        // Check if username is taken
        List<User> users = db.getAllUsers();
        int totalUsers = users.size();

        for(int i=0; i<totalUsers; i++){
            if(user.getUsername().equals(users.get(i).getUsername())){
                usernameUnique = false;
            }
        }

        // Create a new user account if username not taken already
        if(usernameUnique){
            long uid = db.createUser(user);
            hideDialog();
            Toast.makeText(getApplicationContext(), "Account successfully created!", Toast.LENGTH_LONG).show();
        }
        else{
            hideDialog();
            Toast.makeText(getApplicationContext(), "Username already exists", Toast.LENGTH_LONG).show();
        }
    }

    public void gotoLoginActivity(){
        Intent i = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(i);
        finish();
    }

    public void gotoMainActivity(){
        Intent intent = new Intent(RegisterActivity.this, DeviceSelectActivity.class);
        startActivity(intent);
        finish();
    }
    private void showDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private void hideDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }

}
