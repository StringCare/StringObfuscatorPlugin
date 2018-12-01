
import java.io.*;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.Arrays;

public class CredentialUtils {

    private static String key = null;
    private static String until = null;
    private static String error = null;
    private static boolean variantLocated = false;
    private static boolean moduleLocated = false;

    private CredentialUtils() {
        // nothing to do here..
    }

    public static String getKey(String module, String variant, boolean debug) {
        print();
        try {
            key = null;
            until = null;
            moduleLocated = false;
            variantLocated = false;
            String cmd = "";
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                cmd = "gradlew.bat";
            } else {
                cmd = "gradlew";
                Runtime.getRuntime().exec("chmod +x ./" + cmd);
            }

            String command = "./" + cmd + " signingReport";

            InputStream is = Runtime.getRuntime().exec(command).getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader buff = new BufferedReader (isr);

            String line;
            while ((line = buff.readLine()) != null) {
                parseTrace(module, variant, line, debug);
                if (key != null && !debug) {
                    break;
                } else if (key != null && until != null && debug) {
                    break;
                }
            }

        } catch (IOException e) {
            if (debug) e.printStackTrace();
        }
        return sign(key);
    }

    private static void parseTrace(String module, String variant, String line, boolean debug) {

        if (line.toLowerCase().contains("downloading")) {
            if (debug) {
                PrintUtils.print(module, line, debug);
            }
        } else if (line.toLowerCase().contains("unzipping")) {
            if (debug) {
                PrintUtils.print(module, line, debug);
            }
        } else if (line.toLowerCase().contains("permissions")) {
            if (debug) {
                PrintUtils.print(module, line, debug);
            }
        } else if (line.toLowerCase().contains("config:") && moduleLocated && variantLocated) {
            boolean valid = !line.split(": ")[1].trim().equalsIgnoreCase("none");
            if (!valid) {
                key = line.split(": ")[1].trim();
                PrintUtils.print(module, "\uD83E\uDD2F no config defined for variant " + variant, true);
                if (debug) {
                    until = key;
                }
            } else if (debug){
                PrintUtils.print(module, "Module: " + module, true);
                PrintUtils.print(module, "Variant: " + variant, true);
            }

        } else if (line.toLowerCase().contains("sha") && moduleLocated && variantLocated) {
            key = line.split(" ")[1];
            if (debug) {
                PrintUtils.print(module, line, debug);
            }
        } else if (line.toLowerCase().contains("error")) {
            error = line.split(": ")[1];
        } else if (line.toLowerCase().contains("valid until") && moduleLocated && variantLocated) {
            until = line.split(": ")[1];
            if (debug) {
                PrintUtils.print(module, line, debug);
            }
        } else if (line.toLowerCase().contains("store") && moduleLocated && variantLocated) {
            if (debug) {
                PrintUtils.print(module, line, debug);
            }
        } else if (line.toLowerCase().contains("variant") && moduleLocated) {
            String locV = line.split(" ")[1];
            if (locV.equals(variant)) {
                variantLocated = true;
            }
        } else if (line.toLowerCase().contains(":" + module)) {
            moduleLocated = true;
        }
    }

    public static native String sign(String key);

    static {
        try {
            String path = new File(CredentialUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
            // System.setProperty("java.library.path", path + "libsignKey.dylib");
            loadJarDll("libsignKey.dylib");
            // System.loadLibrary("libsignKey");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadJarDll(String name) throws IOException {
        InputStream in = CredentialUtils.class.getResourceAsStream(name);
        byte[] buffer = new byte[1024];
        int read = -1;
        File temp = File.createTempFile(name, "");
        FileOutputStream fos = new FileOutputStream(temp);

        while((read = in.read(buffer)) != -1) {
            fos.write(buffer, 0, read);
        }
        fos.close();
        in.close();

        System.load(temp.getAbsolutePath());
    }

}
