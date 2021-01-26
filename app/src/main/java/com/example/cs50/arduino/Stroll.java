package com.example.cs50.arduino;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Stroll {

    public String time24;

    public Stroll(){

    }

    public Stroll(String time24) {
        this.time24 = time24;
    }

}
