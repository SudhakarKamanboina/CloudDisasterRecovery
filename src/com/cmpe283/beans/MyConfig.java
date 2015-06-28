package com.cmpe283.beans;

import java.net.MalformedURLException;
import java.net.URL;

public class MyConfig
{
    public static URL getVCenterURL() throws MalformedURLException {
        URL url = new URL("https://130.65.132.114/sdk");
        return url ; 
    }
    
    public static String getUsername() {
        return "administrator" ;
    }
    public static String getPassword() {
        return "12!@qwQW" ;
    }
}
