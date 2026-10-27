package vn.hospital.util;
import java.security.*;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
public final class Passwords {
    private Passwords() {}
    public static String hash(String password) {
        byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
        return "pbkdf2$210000$" + Base64.getEncoder().encodeToString(salt) + "$" + Base64.getEncoder().encodeToString(derive(password, salt, 210000));
    }
    private static byte[] derive(String password, byte[] salt, int iterations) {
        var spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (GeneralSecurityException e) { throw new IllegalStateException(e); }
        finally { spec.clearPassword(); }
    }
    public static boolean matches(String raw, String encoded) {
        try { String[] p = encoded.split("\\$"); return p.length == 4 && p[0].equals("pbkdf2") && MessageDigest.isEqual(derive(raw, Base64.getDecoder().decode(p[2]), Integer.parseInt(p[1])), Base64.getDecoder().decode(p[3])); }
        catch (RuntimeException e) { return false; }
    }
}
