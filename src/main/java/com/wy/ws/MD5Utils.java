package com.wy.ws;

import org.springframework.util.DigestUtils;

public class MD5Utils {

    public static boolean verify(String password, String md5Digest) {
        return md5Digest.equals(generateMD5Digest(password));
    }

    public static String generateMD5Digest(String password) {
        return DigestUtils.md5DigestAsHex(password.getBytes());
    }

    public static void main(String[] args) {
        System.out.println(generateMD5Digest("14e54c31-8498-4e75-a819-79cb380836a3"));
    }
}
