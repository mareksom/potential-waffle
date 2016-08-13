package pl.mareksom.potentialwaffle;

import java.util.LinkedList;
import java.util.List;

public class Patch {
    Patch() {
        filesToCopy = new LinkedList<String>();
        filesToUpdate = new LinkedList<String>();
        filesToDelete = new LinkedList<String>();
    }

    public String[] filesToCopy() {
        return filesToCopy.toArray(new String[0]);
    }

    public String[] filesToUpdate() {
        return filesToUpdate.toArray(new String[0]);
    }

    public String[] filesToDelete() {
        return filesToDelete.toArray(new String[0]);
    }

    public void addFileToCopy(String filename) {
        filesToCopy.add(filename);
    }

    public void addFileToUpdate(String filename) {
        filesToUpdate.add(filename);
    }

    public void addFileToDelete(String filename) {
        filesToDelete.add(filename);
    }

    private List<String> filesToCopy, filesToUpdate, filesToDelete;
}
