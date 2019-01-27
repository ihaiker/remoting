package la.renzhen.remoting.commons;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-26 20:22
 */
public class Mixins {

    public static String get(String key, String def) {
        String value = System.getProperty(key);
        if (value != null && !"".equals(value.trim())) {
            return value;
        }
        value = System.getenv(key);
        if (value != null && !"".equals(value.trim())) {
            return value;
        }
        value = System.getenv(key.toUpperCase().replaceAll("\\.", "_"));
        if (value != null && !"".equals(value.trim())) {
            return value;
        }
        return def;
    }

    public static int getInt(String key, int def) throws NumberFormatException {
        String value = get(key, null);
        if (null == value) {
            return def;
        }
        return Integer.parseInt(value);
    }

    public static long getLong(String key, int def) throws NumberFormatException {
        String value = get(key, null);
        if (null == value) {
            return def;
        }
        return Long.parseLong(value);
    }

    public static String get(String module, String key, String def) {
        String value = get(module + "." + key, null);
        if (null == value) {
            return get(key, def);
        }
        return value;
    }

    public static int getInt(String module, String key, int def) throws NumberFormatException {
        String value = get(module, key, null);
        if (null == value) {
            return def;
        }
        return Integer.parseInt(value);
    }

    public static long getLong(String module, String key, int def) throws NumberFormatException {
        String value = get(module, key, null);
        if (null == value) {
            return def;
        }
        return Long.parseLong(value);
    }
}
