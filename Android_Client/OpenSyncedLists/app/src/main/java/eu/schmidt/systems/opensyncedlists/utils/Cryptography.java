package eu.schmidt.systems.opensyncedlists.utils;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * Some preconfigured cryptography tools
 */
public class Cryptography {

    public static String getSHAasString(String input) {
        return byteArraytoString(getSHA(input));
    }

    /**
     * Get SHA-256 Hash of a String
     *
     * @param input InputString
     * @return SHA-256 Hash
     */
    public static byte[] getSHA(String input) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return md.digest(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate a random string with letters A-Z and a-z
     *
     * @param length Length of String
     * @return random string
     */
    public static String generatingRandomString(int length) {
        int leftLimit = 65; // letter 'A'
        int rightLimit = 90; // letter 'Z'
        int leftLimit2 = 97; // letter 'a'
        int rightLimit2 = 122; // letter 'Z'

        Random random = new Random();
        StringBuilder buffer = new StringBuilder(length);
        int distance = (leftLimit2 - rightLimit);
        for (int i = 0; i < length; i++) {
            int randomLimitedInt = leftLimit + (int) (random.nextFloat() *
                    (rightLimit2 - leftLimit - distance + 1));
            buffer.append(
                    randomLimitedInt < rightLimit ? (char) randomLimitedInt
                                                  : (char) (randomLimitedInt +
                                                          distance));
        }
        return buffer.toString();
    }

    public static byte[] stringtoByteArray(String s) {
        /* From Hex String

        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }*/
        return Base64.decode(s, Base64.DEFAULT);
    }

    public static String byteArraytoString(byte[] bytes) {
        /* To Hex String

        StringBuilder res = new StringBuilder();
        for (byte b : bytes) {
            res.append(String.format("%02X-",b));
        }
        return res.toString().replaceAll("-", "");*/
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }


    public static SecretKey generateAESKey()
    {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256);
            return generator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static String encryptRSA(SecretKey secretKey, String data) {
        String result = "";
        try {
            // Breakable by known-plaintext attacks
            Cipher cipher
                    = Cipher.getInstance("AES");
            cipher.init(
                    Cipher.ENCRYPT_MODE, secretKey);
            return Base64.encodeToString(cipher.doFinal(
                            data.getBytes("UTF-8")), Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String decryptRSA(SecretKey secretKey, String data) {
        String result = "";
        try {
            Cipher cipher
                    = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE,
                        secretKey);
            byte[] resultBytes
                    = cipher.doFinal(Base64.decode(data, Base64.DEFAULT));
            result =  new String(resultBytes, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
