package framework.web;


import com.alibaba.fastjson2.JSON;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.stream.Collectors;

public class Util {

    public static String escapeHTML(String str) {
        return str.chars().mapToObj(c -> c > 127 || "\"'<>&".indexOf(c) != -1 ?
                "&#" + c + ";" : String.valueOf((char) c)).collect(Collectors.joining());
    }

    /** Performs the SHA-256 message digest algorithm on the input
     *
     * @param input
     * @return Hex string of hashed result.
     */
    public static String hashy(byte[] input){
        try{
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(input);
            return hexStr(hash);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public static String hexStr(byte[] hash) {
        final StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            final String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static String base64Str(byte[] data){
        return new String(java.util.Base64.getEncoder().encode(data));
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
            return JSON.parseObject(in.readLine(), LocationQuery.class);
        }
    }
}
