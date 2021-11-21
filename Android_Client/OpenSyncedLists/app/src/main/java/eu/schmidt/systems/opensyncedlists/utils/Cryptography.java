package eu.schmidt.systems.opensyncedlists.utils;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * Some preconfigured cryptography tools
 */
public class Cryptography {

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
}
