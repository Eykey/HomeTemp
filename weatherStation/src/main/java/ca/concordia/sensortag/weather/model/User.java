package ca.concordia.sensortag.weather.model;

/**
 * Created by Karimi on 2017-03-25.
 */

public class User {
    private String username;
    private String fullname;
    private String password;
    private long userID;

    public User(){}

    public User(String user, String name, String pass){
        this.username = user;
        this.fullname = name;
        this.password = pass;
    }

    //--------------------------setters------------------------------//

    public void setUsername(String user){
        this.username = user;
    }

    public void setFullname(String name) { this.fullname = name; }

    public void setPassword(String pass){
        this.password = pass;
    }

    public void setUserID(long id){
        this.userID = id;
    }

    //--------------------------getters------------------------------//

    public String getUsername(){
        return username;
    }

    public String getFullname() { return fullname; }

    public String getPassword(){
        return password;
    }

    public long getUserID(){
        return userID;
    }
}
