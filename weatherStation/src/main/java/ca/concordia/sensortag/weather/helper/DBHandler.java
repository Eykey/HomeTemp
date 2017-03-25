package ca.concordia.sensortag.weather.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import ca.concordia.sensortag.weather.model.User;

/**
 * Created by Karimi on 2017-03-25.
 */

public class DBHandler extends SQLiteOpenHelper {


    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "HomeTempDB";

    // Table names
    private static final String TABLE_USER = "user";

    // Common Column Name
    private static final String KEY_USER_ID = "user_id";

    // User Table Column Names
    private static final String KEY_USERNAME = "username";
    private static final String KEY_FULLNAME = "fullname";
    private static final String KEY_PASSWORD = "password";

    // User Table create statement
    private static final String CREATE_TABLE_USER = "CREATE TABLE " + TABLE_USER + "(" + KEY_USER_ID + " INTEGER PRIMARY KEY," + KEY_USERNAME + " TEXT,"
            + KEY_FULLNAME + " TEXT," + KEY_PASSWORD + " TEXT" + ")";


    public DBHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USER);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);

        // create new table
        onCreate(db);
    }

    //--------------------------USER TABLE CRUD OPERATIONS---------------------------------//

    public long createUser(User user){
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_USERNAME, user.getUsername());
        values.put(KEY_FULLNAME, user.getFullname());
        values.put(KEY_PASSWORD, user.getPassword());

        long user_id = db.insert(TABLE_USER, null, values);

        db.close();
        return user_id;
    }

    public User getUser(long uid){
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT * FROM " + TABLE_USER + " WHERE " + KEY_USER_ID + " = " + uid;
        Cursor c = db.rawQuery(selectQuery, null);

        if (c != null)
            c.moveToFirst();

        User user = new User();
        user.setUserID(c.getLong(c.getColumnIndex(KEY_USER_ID)));
        user.setUsername(c.getString(c.getColumnIndex(KEY_USERNAME)));
        user.setFullname(c.getString(c.getColumnIndex(KEY_FULLNAME)));
        user.setPassword(c.getString(c.getColumnIndex(KEY_PASSWORD)));

        db.close();
        return user;
    }

    public List<User> getAllUsers(){
        List<User> users = new ArrayList<User>();
        String selectQuery = "SELECT * FROM " + TABLE_USER;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        if(c.moveToFirst()){
            do{
                User user = new User();
                user.setUserID(c.getLong(c.getColumnIndex(KEY_USER_ID)));
                user.setUsername(c.getString(c.getColumnIndex(KEY_USERNAME)));
                user.setFullname(c.getString(c.getColumnIndex(KEY_FULLNAME)));
                user.setPassword(c.getString(c.getColumnIndex(KEY_PASSWORD)));

                users.add(user);
            } while(c.moveToNext());
        }
        db.close();
        return users;
    }

    public int updateUser(User user){
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_USERNAME, user.getUsername());
        values.put(KEY_FULLNAME, user.getFullname());
        values.put(KEY_PASSWORD, user.getPassword());

        return db.update(TABLE_USER, values, KEY_USER_ID + " = ?", new String[] { String.valueOf(user.getUserID())});
    }

    public void deleteUser(long uid){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_USER, KEY_USER_ID + " = ?", new String[] { String.valueOf(uid)});
    }

    // closing database
    public void closeDB() {
        SQLiteDatabase db = this.getReadableDatabase();
        if (db != null && db.isOpen())
            db.close();
    }


}
