package pl.mareksom.potentialwaffle;

import java.io.*;
import java.security.MessageDigest;

public class Md5Checksum {
    public static byte[] createChecksum(File file, String filename, StatusUpdater statusUpdater) throws Exception {
        statusUpdater.initProgress((int) (file.length() / 4096), "Computing checksum of " + filename);
        InputStream fis =  new FileInputStream(file);
        byte[] buffer = new byte[4096];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;
        int totalRead = 0;
        int lastProgress = 0;
        do {
            numRead = fis.read(buffer);
            totalRead += numRead;
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
            int newProgress = totalRead / 4096;
            if (newProgress != lastProgress) {
                lastProgress = newProgress;
                statusUpdater.setProgress(totalRead / 4096);
            }
        } while (numRead != -1);
        fis.close();
        return complete.digest();
    }

    // see this How-to for a faster way to convert
    // a byte array to a HEX string
    public static String getMd5Checksum(File file, String filename, StatusUpdater statusUpdater) throws Exception {
        byte[] b = createChecksum(file, filename, statusUpdater);
        String result = "";
        for (int i=0; i < b.length; i++) {
            result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
        }
        statusUpdater.setProgressDescription("Checksum computed for file: " + filename);
        return result;
    }
}