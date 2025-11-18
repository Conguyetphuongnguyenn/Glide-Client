package me.eldodebug.soar.utils.file;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JFileChooser;

import me.eldodebug.soar.logger.GlideLogger;
import me.eldodebug.soar.utils.file.filter.PngFileFilter;
import me.eldodebug.soar.utils.file.filter.SoundFileFilter;
import net.minecraft.util.Util;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.lwjgl.Sys;

public final class FileUtils {

    private static final int BUFFER_SIZE = 8192;
    private static final ThreadLocal<byte[]> BUFFER_POOL = ThreadLocal.withInitial(() -> new byte[BUFFER_SIZE]);

    private FileUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static boolean deleteDirectory(File directory) {
        if (directory == null || !directory.exists()) {
            return false;
        }

        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file != null) {
                        deleteDirectory(file);
                    }
                }
            }
        }

        return directory.delete();
    }

    public static long getDirectorySize(File directory) {
        if (directory == null || !directory.exists()) {
            return 0L;
        }

        long size = 0;

        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file != null) {
                        size += getDirectorySize(file);
                    }
                }
            }
        } else {
            size = directory.length();
        }

        return size;
    }

    public static void unzip(File zipFile, File destDir) throws IOException {
        if (zipFile == null || !zipFile.exists()) {
            throw new IOException("Zip file does not exist");
        }
        if (destDir == null) {
            throw new IOException("Destination directory cannot be null");
        }

        final Path destPath = destDir.toPath().toAbsolutePath().normalize();
        Files.createDirectories(destPath);

        try (FileInputStream fis = new FileInputStream(zipFile);
             ZipInputStream zis = new ZipInputStream(fis)) {

            ZipEntry zipEntry;
            byte[] buffer = BUFFER_POOL.get();

            while ((zipEntry = zis.getNextEntry()) != null) {
                String entryName = zipEntry.getName();
                Path newFilePath = destPath.resolve(entryName).normalize();

                if (!newFilePath.startsWith(destPath)) {
                    throw new IOException("Zip Slip detected: " + entryName);
                }

                if (zipEntry.isDirectory()) {
                    Files.createDirectories(newFilePath);
                } else {
                    Files.createDirectories(newFilePath.getParent());

                    try (FileOutputStream fos = new FileOutputStream(newFilePath.toFile())) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }

                zis.closeEntry();
            }
        }
    }

    public static File selectImageFile() {
        return selectFile(new PngFileFilter());
    }

    public static File selectSoundFile() {
        return selectFile(new SoundFileFilter());
    }

    private static File selectFile(javax.swing.filechooser.FileFilter filter) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(filter);
        fileChooser.setAcceptAllFileFilterUsed(false);

        int result = fileChooser.showOpenDialog(null);
        return (result == JFileChooser.APPROVE_OPTION) ? fileChooser.getSelectedFile() : null;
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (sourceFile == null || !sourceFile.exists()) {
            throw new IOException("Source file does not exist");
        }
        if (destFile == null) {
            throw new IOException("Destination file cannot be null");
        }

        byte[] buffer = BUFFER_POOL.get();

        try (InputStream input = new FileInputStream(sourceFile);
             OutputStream output = new FileOutputStream(destFile)) {

            int length;
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
        }
    }

    public static void downloadFile(String url, File output) {
        if (url == null || url.isEmpty()) {
            GlideLogger.error("URL cannot be null or empty");
            return;
        }
        if (output == null) {
            GlideLogger.error("Output file cannot be null");
            return;
        }

        byte[] buffer = BUFFER_POOL.get();

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                HttpEntity entity = response.getEntity();

                if (entity != null) {
                    try (InputStream inputStream = entity.getContent();
                         OutputStream outputStream = new FileOutputStream(output)) {

                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }
        } catch (IOException e) {
            GlideLogger.error("Failed to download file: " + url, e);
        }
    }

    public static String getBaseName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }

    public static String getBaseName(File file) {
        return (file != null) ? getBaseName(file.getName()) : "";
    }

    public static String getExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    public static String getExtension(File file) {
        return (file != null) ? getExtension(file.getName()) : "";
    }

    public static boolean isAudioFile(File file) {
        if (file == null) {
            return false;
        }

        String ext = getExtension(file).toLowerCase();
        return ext.equals("mp3") || ext.equals("wav") || ext.equals("ogg");
    }

    public static boolean isImageFile(File file) {
        if (file == null) {
            return false;
        }

        String ext = getExtension(file).toLowerCase();
        return ext.equals("jpeg") || ext.equals("png") || ext.equals("jpg");
    }

    public static void openFolderAtPath(File folder) {
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            return;
        }

        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.OPEN)) {
                    desktop.open(folder);
                    return;
                }
            }

            Util.EnumOS os = Util.getOSType();
            String path = folder.getAbsolutePath();

            if (os == Util.EnumOS.WINDOWS) {
                new ProcessBuilder("explorer.exe", path).start();
            } else if (os == Util.EnumOS.OSX) {
                new ProcessBuilder("open", path).start();
            } else {
                Sys.openURL("file://" + path);
            }
        } catch (Exception e) {
            GlideLogger.error("Failed to open folder: " + folder.getAbsolutePath(), e);
            try {
                Sys.openURL("file://" + folder.getAbsolutePath());
            } catch (Exception ex) {
                GlideLogger.error("Fallback failed", ex);
            }
        }
    }
}