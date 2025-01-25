package at.sfischer.constraints.evaluation.data;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class ZenodoDownloader {

    private static final int BUFFER_SIZE = 65536; // 64 KB buffer
    private static final int MAX_RETRIES = 3; // Max retry attempts
    private static final int RETRY_DELAY_MS = 5000; // 5 seconds delay between retries

    /**
     * Downloads the file for the given url.
     *
     * Note: If you have issues with this code it could be to unstable internet connection. An issue previously appeared when using a VPN.
     *
     * @param zenodoFileURL
     * @param saveDirectory
     * @return
     */
    public static File downloadFile(String zenodoFileURL, File saveDirectory) {
        File destFile = getFileName(zenodoFileURL, saveDirectory);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                long existingFileSize = destFile.exists() ? destFile.length() : 0;
                HttpURLConnection httpConn = (HttpURLConnection) new URL(zenodoFileURL).openConnection();

                // Resume if file is partially downloaded
                if (existingFileSize > 0) {
                    httpConn.setRequestProperty("Range", "bytes=" + existingFileSize + "-");
                }

                int responseCode = httpConn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_PARTIAL) {
                    long totalFileSize = httpConn.getContentLengthLong() + existingFileSize;

                    System.out.println("Downloading: " + destFile.getName());
                    System.out.println("File size: " + totalFileSize / (1024 * 1024) + " MB");

                    try (InputStream inputStream = httpConn.getInputStream();
                         RandomAccessFile raf = new RandomAccessFile(destFile, "rw")) {

                        raf.seek(existingFileSize); // Resume from where download stopped

                        byte[] buffer = new byte[BUFFER_SIZE];
                        int bytesRead;
                        long downloaded = existingFileSize;
                        long lastLoggedProgress = 0;

                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            raf.write(buffer, 0, bytesRead);
                            downloaded += bytesRead;

                            // Log progress every 5 MB
                            if (downloaded - lastLoggedProgress >= 5 * 1024 * 1024) {
                                System.out.println("Downloaded: " + downloaded / (1024 * 1024) + " MB");
                                lastLoggedProgress = downloaded;
                            }
                        }

                        System.out.println("Download complete: " + destFile.getAbsolutePath());
                        return destFile;
                    }
                } else {
                    throw new IOException("Server responded with: " + responseCode);
                }

            } catch (IOException e) {
                System.err.println("Download failed (Attempt " + attempt + "/" + MAX_RETRIES + ")");
                if (attempt < MAX_RETRIES) {
                    try {
                        System.out.println("Retrying in " + (RETRY_DELAY_MS / 1000) + " seconds...");
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ignored) {
                    }
                } else {
                    System.err.println("Max retries reached. Download failed.");
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private static File getFileName(String url, File saveDirectory) {
        String fileName = new File(url).getName();
        fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        return new File(saveDirectory, fileName);
    }
}
