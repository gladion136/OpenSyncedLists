package eu.schmidt.systems.opensyncedlists.utils;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Some preconfigured cryptography tools
 */
public class Cryptography {

    private final static int GCM_IV_LENGTH = 12;
    private final static int GCM_TAG_LENGTH = 16;

    private final static String CHARSET = "UTF-8";

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

    public static SecretKey generateAESKey() {
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
            byte[] iv = new byte[GCM_IV_LENGTH];
            (new SecureRandom()).nextBytes(iv);
            GCMParameterSpec ivSpec =
                    new GCMParameterSpec(GCM_TAG_LENGTH * Byte.SIZE, iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            // Encrypt and add iv
            byte[] ciphertext = cipher.doFinal(data.getBytes(CHARSET));
            byte[] encrypted = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, encrypted, 0, iv.length);
            System.arraycopy(ciphertext, 0, encrypted, iv.length,
                             ciphertext.length);
            return Base64.encodeToString(encrypted, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String decryptRSA(SecretKey secretKey, String data) {
        String result = "";
        try {
            byte[] decoded = Base64.decode(data, Base64.DEFAULT);

            // get iv
            byte[] iv = Arrays.copyOfRange(decoded, 0, GCM_IV_LENGTH);
            GCMParameterSpec ivSpec =
                    new GCMParameterSpec(GCM_TAG_LENGTH * Byte.SIZE, iv);

            // decrypt
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            byte[] resultBytes = cipher.doFinal(decoded, GCM_IV_LENGTH,
                                                decoded.length - GCM_IV_LENGTH);
            result = new String(resultBytes, CHARSET);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
