package vn.co.cake.security.utils;

import org.apache.logging.log4j.util.Strings;
import org.postgresql.util.Base64;

import javax.servlet.http.Cookie;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * AuthenticationUtils
 */
public class AuthenticationUtils {

    private static final String SESSION = "SESSION";

    public static String getSid(Cookie[] cookies) {
        if (null != cookies && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (SESSION.equals(cookie.getName())) {
                    return new String(Base64.decode(cookie.getValue()));
                }
            }
        }
        return Strings.EMPTY;
    }

    /**
     * Read the object from Base64 string.
     */
    public static Object fromSerializableString(String s) throws Exception {
        byte[] data = Base64.decode(s);
        try (
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))
        ) {
            return ois.readObject();
        }
    }

    /**
     * Write the object to a Base64 string.
     */
    public static String toSerializableString(Object o) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (
                ObjectOutputStream oos = new ObjectOutputStream(baos)
        ) {
            oos.writeObject(o);
        }
        return Base64.encodeBytes(baos.toByteArray());
    }
}
