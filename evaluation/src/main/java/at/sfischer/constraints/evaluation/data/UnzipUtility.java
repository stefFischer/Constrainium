package at.sfischer.constraints.evaluation.data;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UnzipUtility {

    public static void unzip(File zipFilePath, File destDir) {
        if (!destDir.exists()) {
            destDir.mkdirs(); // Ensure the destination directory exists
        }

        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                File newFile = new File(destDir, entry.getName());

                // Ensure directories are created properly
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    // Create parent directories if necessary
                    new File(newFile.getParent()).mkdirs();

                    // Write the extracted file
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                }
                zipInputStream.closeEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
