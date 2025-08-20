package com.example.tiggle.service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

final class OtpCrypto {
    private static final SecureRandom RND = new SecureRandom();

    static String randomSalt() {
        byte[] b = new byte[16]; RND.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
    static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(md.digest(s.getBytes()));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
