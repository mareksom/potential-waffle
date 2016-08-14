package pl.mareksom.potentialwaffle;

import android.support.v4.provider.DocumentFile;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SyncRunner implements Runnable {
    SyncRunner(MainActivity mainActivity) {
        localDirectory_ = mainActivity.localDirectory_;
        serverAddress_ =((EditText) mainActivity.findViewById(R.id.edit_server_address))
                .getText().toString();
        serverPortNumber_ = ((EditText) mainActivity.findViewById(R.id.edit_server_port_number))
                .getText().toString();
        statusUpdater = mainActivity.getStatusUpdater();
    }

    public void run() {
        int port = 0;
        java.net.InetAddress address = null;
        boolean isError = false;
        try {
            port = Integer.parseInt(serverPortNumber_);
            if (port < 1 || port > 65535) {
                statusUpdater.setErrorOnTextView(R.id.edit_server_port_number, "Port number must be in range [1, 65535].");
                isError = true;
            }
        } catch (NumberFormatException e) {
            statusUpdater.setErrorOnTextView(R.id.edit_server_port_number, "Invalid port number.");
            isError = true;
        }
        try {
            address = java.net.InetAddress.getByName(serverAddress_);
        } catch (java.net.UnknownHostException e) {
            statusUpdater.setErrorOnTextView(R.id.edit_server_address, e.getMessage());
            isError = true;
        }
        if (localDirectory_ == null) {
            statusUpdater.setErrorOnTextView(R.id.local_directory_path, "Choose a directory.");
            isError = true;
        } else {
            if (!localDirectory_.exists()) {
                statusUpdater.setErrorOnTextView(R.id.local_directory_path, "Directory '" + localDirectory_.toString() + "' doesn't exist.");
                isError = true;
            } else if (!localDirectory_.isDirectory()) {
                statusUpdater.setErrorOnTextView(R.id.local_directory_path, "Not a directory.");
                isError = true;
            }
        }
        if (!isError) {
            try {
                startSynchronization_(localDirectory_, address, port);
                statusUpdater.initProgress(1, "Everything is up to date.");
                statusUpdater.setProgress(1);
            } catch (SyncException e) {
                statusUpdater.initProgress(1, "FAIL: " + e.getMessage());
            }
        }
    }

    private File localDirectory_;
    private String serverAddress_;
    private String serverPortNumber_;

    private void startSynchronization_(
            java.io.File directory, java.net.InetAddress address, int port) throws SyncException {
        try {
            DirDescriptor descriptor = DirDescriptor.create(directory, statusUpdater);
            connectToTheServer_(directory, address, port, descriptor);
            DirDescriptor.create(directory, statusUpdater);
            deleteEmptyDirectories(directory, directory);
        } catch (DescriptorException e) {
            statusUpdater.log("DescriptorException:\n" + e.getMessage() + "\n");
            throw new SyncException(e.getMessage());
        }
    }

    private void deleteEmptyDirectories(File mainDirectory, File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            statusUpdater.log("Strange: directory.listFiles() returned null.\n");
        }
        boolean isEmpty = true;
        for (File file : files) {
            if (file.isDirectory()) {
                deleteEmptyDirectories(mainDirectory, file);
            } else if (file.isFile()) {
                if (file.isHidden()) {
                    String relativePath = mainDirectory.toURI().relativize(file.toURI()).getPath();
                    if (!relativePath.equals(".potential-waffle")) {
                        deleteFile(mainDirectory, relativePath);
                    }
                }
            }
            if (file.exists()) {
                isEmpty = false;
            }
        }
        if (isEmpty) {
            if (!directory.delete()) {
                statusUpdater.log("Tried to delete, but failed: " +
                        mainDirectory.toURI().relativize(directory.toURI()).getPath() + "\n");
            }
        }
    }

    private static final int PROTOCOL_DESCRIPTOR_COMMAND = 1145393930;
    private static final int PROTOCOL_FILE_COMMAND = 1179208714;
    private static final int PROTOCOL_FILE_CHUNK_SIZE = 4096;
    private static final int PROTOCOL_MAX_INTEGER = Integer.MIN_VALUE;

    private void deleteFile(File directory, String filename) {
        statusUpdater.setIndeterminate(true, "Deleting file: " + filename);
        File thisFile = new File(directory, filename);
        if (!thisFile.delete()) {
            if (thisFile.exists()) {
                statusUpdater.setIndeterminate(false, "Couldn't delete file: " + filename);
                statusUpdater.log("Couldn't delete file: " + filename + "\n");
            }
        } else {
            statusUpdater.setIndeterminate(false, "File deleted: " + filename);
            statusUpdater.log("File deleted successfully: " + filename + "\n");
        }
    }

    private void downloadFile(
            File directory, String filename,
            DataOutputStream dos, DataInputStream dis) throws IOException {
        dos.writeInt(PROTOCOL_FILE_COMMAND);
        byte[] filenameBytes = filename.getBytes("UTF-8");
        dos.writeInt(filenameBytes.length);
        dos.write(filenameBytes);
        statusUpdater.setIndeterminate(true, "Waiting for file info: " + filename);
        int lengthOfTheFile = dis.readInt();
        statusUpdater.log("Length of the file = " + Integer.toString(lengthOfTheFile) + "\n");
        if (lengthOfTheFile == PROTOCOL_MAX_INTEGER) {
            statusUpdater.log("Server says that file doesn't exist: " + filename + "\n");
            statusUpdater.setIndeterminate(false, "File doesn't exist: " + filename);
            return;
        }
        if ((lengthOfTheFile & 0x00000000ffffffffL) > 1024 * 1024 * 1024) {
            statusUpdater.log("The file is over 1GiB, skipping: " + filename + "\n");
            statusUpdater.setIndeterminate(false, "The file is over 1GiB: " + filename);
            return;
        }
        statusUpdater.setIndeterminate(false, "Got file info: " + filename);
        statusUpdater.initProgress(lengthOfTheFile, "Downloading " + filename);
        File thisFile = new File(directory, filename);
        File parent = thisFile.getParentFile();
        if (!parent.exists()) {
            if (!parent.mkdirs()) {
                if (!parent.exists()) {
                    statusUpdater.log("Couldn't create the directory for: " + filename + "\n");
                    statusUpdater.setProgressDescription("Couldn't create the directory for: " + filename);
                    return;
                }
            }
        }
        FileOutputStream fos = new FileOutputStream(thisFile);
        while (lengthOfTheFile > 0) {
            int thisChunkSize = java.lang.Math.min(lengthOfTheFile, PROTOCOL_FILE_CHUNK_SIZE);
            byte[] data = new byte[thisChunkSize];
            dis.readFully(data);
            fos.write(data);
            lengthOfTheFile -= thisChunkSize;
            statusUpdater.addProgress(thisChunkSize);
        }
        fos.close();
        statusUpdater.setProgressDescription("File saved: " + filename);
        statusUpdater.log("File saved: " + filename + "\n");
    }

    private void connectToTheServer_(
            File directory, InetAddress address, int port, DirDescriptor descriptor) throws SyncException {
        Socket socket = null;
        DataOutputStream dos = null;
        DataInputStream dis = null;
        try {
            statusUpdater.log("Opening connection...\n");
            statusUpdater.setIndeterminate(true, "Connecting to server.");
            socket = new Socket(address, port);
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());

            // DESCRIPTOR command
            dos.writeInt(PROTOCOL_DESCRIPTOR_COMMAND);
            statusUpdater.setIndeterminate(true, "Reading descriptor.");
            int remoteDescriptorXmlSize = dis.readInt();
            byte[] descriptorXml = new byte[remoteDescriptorXmlSize];
            dis.readFully(descriptorXml);
            statusUpdater.setIndeterminate(false, "Descriptor read.");
            statusUpdater.log("Got descriptor.\n");
            try {
                statusUpdater.setIndeterminate(true, "Parsing remote descriptor.");
                DirDescriptor remoteDescriptor = DirDescriptor.fromInputStream(
                        new ByteArrayInputStream(descriptorXml));
                statusUpdater.setIndeterminate(true, "Computing patch.");
                Patch patch = descriptor.computePatchTo(remoteDescriptor);
                statusUpdater.log("Computed patch.\n");
                statusUpdater.setIndeterminate(false, "Patch is ready.");
                for (String filename : patch.filesToDelete()) {
                    statusUpdater.log("Delete: " + filename + "\n");
                    deleteFile(directory, filename);
                }
                for (String filename : patch.filesToCopy()) {
                    statusUpdater.log("Copy:   " + filename + "\n");
                    downloadFile(directory, filename, dos, dis);
                }
                for (String filename : patch.filesToUpdate()) {
                    statusUpdater.log("Update: " + filename + "\n");
                    downloadFile(directory, filename, dos, dis);
                }
            } catch (DescriptorException e) {
                statusUpdater.log("Remote descriptor error: " + e.getMessage());
                throw new SyncException("Remote descriptor error: " + e.getMessage());
            } finally {
                dos.close();
                dos = null;
                dis.close();
                dis = null;
                socket.close();
                socket = null;
                statusUpdater.log("Connection closed.\n");
            }
        } catch (UnknownHostException e) {
            statusUpdater.log("Unknown host: " + e.getMessage() + "\n");
            throw new SyncException("Unknown host: " + e.getMessage());
        } catch (IOException e) {
            statusUpdater.log("IO error: " + e.getMessage() + "\n");
            throw new SyncException("IO error: " + e.getMessage());
        } finally {
            try {
                if (dos != null) {
                    dos.close();
                }
                if (dis != null) {
                    dis.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                statusUpdater.log("IO exception while closing: " + e.getMessage() + "\n");
                throw new SyncException("IO exception while closing: " + e.getMessage());
            }
        }
    }

    private StatusUpdater statusUpdater;
}
