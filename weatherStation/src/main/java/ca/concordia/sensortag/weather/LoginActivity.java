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

public class LoginActivity extends Activity {

    private Button btnLogin;
    private Button btnLinkToRegister;
    private EditText inputUserName;
    private EditText inputPassword;
    private ProgressDialog pDialog;
    private SessionManager session;
    private DBHandler db;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        inputUserName = (EditText) findViewById(R.id.username);
        inputPassword = (EditText) findViewById(R.id.password);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        btnLinkToRegister = (Button) findViewById(R.id.btnLinkToRegisterScreen);

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        // SQLite database handler
        db = new DBHandler(getApplicationContext());

        // Session manager
        session = new SessionManager(getApplicationContext());

        // Check if user is already logged in or not
        if (session.isLoggedIn()) {
            // User is already logged in. Take him to main activity
            gotoMainActivity();
        }

        // Login button Click Event
        btnLogin.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                String username = inputUserName.getText().toString().trim();
                String password = inputPassword.getText().toString().trim();

                // Check for empty data in the form
                if (!username.isEmpty() && !password.isEmpty()) {
                    // login user
                    checkLogin(username, password);
                } else {
                    // Prompt user to enter credentials
                    Toast.makeText(getApplicationContext(),
                            "Please enter the credentials!", Toast.LENGTH_LONG)
                            .show();
                }
            }

        });

        // Link to Register Screen
        btnLinkToRegister.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(),
                        RegisterActivity.class);
                startActivity(i);
                finish();
            }
        });

    }

    /**
     * function to verify login details in mysql db
     * */
    private void checkLogin(final String username, final String password) {

        pDialog.setMessage("Logging in ...");
        showDialog();

        List<User> users = db.getAllUsers();
        int totalUsers = users.size();

        for(int i=0; i<totalUsers; i++){
            if(username.equals(users.get(i).getUsername())){
                if(password.equals(users.get(i).getPassword())){
                    session.saveSession(users.get(i));
                    hideDialog();
                    gotoMainActivity();
                }
            }
        }

        hideDialog();
        Toast.makeText(getApplicationContext(), "Username or Password invalid.", Toast.LENGTH_LONG).show();
    }

    public void gotoMainActivity(){
        Intent intent = new Intent(LoginActivity.this, DeviceSelectActivity.class);
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
