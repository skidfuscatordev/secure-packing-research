package dev.zenrho.driver.classloader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A custom class loader that loads classes from a JAR file and uses a native method to define them.
 * This class is designed to work across different operating systems by loading the appropriate native library.
 */
public class ProtectedClassLoader extends ClassLoader {
    static {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            System.loadLibrary("driver_rs_windows");
        } else if (osName.contains("mac")) {
            System.loadLibrary("rust_component_mac");
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            System.loadLibrary("rust_component_linux");
        } else {
            throw new UnsupportedOperationException("Unsupported OS: " + osName);
        }
    }

    private Map<String, byte[]> classBytesMap = new HashMap<>();

    /**
     * Finds and loads the class with the specified name.
     *
     * @param name the name of the class
     * @return the resulting <code>Class</code> object
     * @throws ClassNotFoundException if the class could not be found
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] classBytes = classBytesMap.get(name);
        if (classBytes == null) {
            throw new ClassNotFoundException(name);
        }

        return loadClassNative(name.replace('.', '/'), classBytes, this);
    }

    /**
     * Native method to define a class using the given class name, bytecode, and class loader.
     *
     * @param className the name of the class
     * @param classBytes the bytecode of the class
     * @param loader the class loader to use
     * @return the resulting <code>Class</code> object
     */
    private native Class<?> loadClassNative(String className, byte[] classBytes, ClassLoader loader);

    /**
     * Loads all classes from the specified JAR file and stores their bytecode in the class bytes map.
     *
     * @param jarPath the path to the JAR file
     * @throws IOException if an I/O error occurs
     */
    public void loadJar(String jarPath) throws IOException {
        JarFile jarFile = new JarFile(jarPath);
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.getName().endsWith(".class")) {
                try (InputStream is = jarFile.getInputStream(entry);
                     ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

                    int nRead;
                    byte[] data = new byte[16384];
                    while ((nRead = is.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                    byte[] classBytes = buffer.toByteArray();
                    String className = entry.getName().replace('/', '.').replace(".class", "");
                    classBytesMap.put(className, classBytes);
                }
            }
        }
        jarFile.close();
    }

    /**
     * The main method to run the class loader with specified JAR file and main class name.
     *
     * @param args command line arguments where the first argument is the path to the JAR file
     *             and the second argument is the main class name
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java ProtectedClassLoader <path-to-jar> <main-class-name>");
            System.exit(1);
        }

        String jarPath = args[0];
        String mainClassName = args[1];

        try {
            ProtectedClassLoader loader = new ProtectedClassLoader();
            loader.loadJar(jarPath);

            // Load the main class
            Class<?> mainClass = loader.loadClass(mainClassName);

            // Find the main method
            java.lang.reflect.Method mainMethod = mainClass.getMethod("main", String[].class);

            // Call the main method
            String[] mainArgs = new String[0];
            mainMethod.invoke(null, (Object) mainArgs);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
