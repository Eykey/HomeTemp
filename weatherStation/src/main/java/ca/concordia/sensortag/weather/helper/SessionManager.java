package ca.concordia.sensortag.weather.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import ca.concordia.sensortag.weather.model.User;

/**
 * Created by Karimi on 2017-03-25.
 */

public class SessionManager {

    // LogCat tag
    private static String TAG = SessionManager.class.getSimpleName();


    // Shared Preferences
    SharedPreferences pref;

    SharedPreferences.Editor editor;
    Context _context;

    // Shared pref mode
    int PRIVATE_MODE = 0;

    // Shared preferences file name
    private static final String PREF_NAME = "ProfilePreference";

    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_IS_BT_CONNECTED = "isBtConnected";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_FULLNAME = "fullname";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_ID = "id";

    public SessionManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }
    public void saveSession(User user){
        editor.putString(KEY_USERNAME, user.getUsername());
        editor.putString(KEY_FULLNAME, user.getFullname());
        editor.putString(KEY_PASSWORD, user.getPassword());
        editor.putLong(KEY_ID, user.getUserID());
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putBoolean(KEY_IS_BT_CONNECTED, false);

        // commit changes
        editor.commit();

        Log.d(TAG, "User login session modified!");
    }

    public void setLogin(boolean isLoggedIn) {

        editor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn);

        // commit changes
        editor.commit();

        Log.d(TAG, "User login session modified!");
    }

    public void setBt(boolean isBtConnected) {

        editor.putBoolean(KEY_IS_LOGGED_IN, isBtConnected);

        // commit changes
        editor.commit();

        Log.d(TAG, "User Bluetooth status modified!");
    }

    public boolean isLoggedIn(){
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }
    public boolean isBtConnected() { return pref.getBoolean(KEY_IS_BT_CONNECTED, false);}
}
