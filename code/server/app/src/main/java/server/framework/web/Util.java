package server.framework.web;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Util {

    public static String escapeHTML(String str) {
        return str.chars().mapToObj(c -> c > 127 || "\"'<>&".indexOf(c) != -1 ?
                "&#" + c + ";" : String.valueOf((char) c)).collect(Collectors.joining());
    }

    public static String hashy(byte[] input){
        try{
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(input);
            final StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < hash.length; i++) {
                final String hex = Integer.toHexString(0xff & hash[i]);
                if(hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public static class LocationQuery{
        public String query;
        public String status;
        public String message;
        public String country;
        public String countryCode;
        public String region;
        public String regionName;
        public String city;
        public String zip;
        public Float lat;
        public Float lon;
        public String timezone;
        public String isp;
        public String org;
        public String as;
    }

    public static LocationQuery queryLocation(InetAddress ip) throws Exception {
        URL yahoo = new URL("http://ip-api.com/json/" + ip.getHostAddress());
        URLConnection yc = yahoo.openConnection();
        yc.setReadTimeout(500);
        yc.setConnectTimeout(500);
        try(
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(
                                yc.getInputStream()));
        ){
            return new Gson().fromJson(in.readLine(), LocationQuery.class);
        }
    }
}
