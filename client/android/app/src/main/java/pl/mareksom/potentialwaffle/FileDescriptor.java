package pl.mareksom.potentialwaffle;

public class FileDescriptor {
    FileDescriptor(String checksum, long timestamp) {
        checksum_ = checksum;
        timestamp_ = timestamp;
    }

    public String checksum() {
        return checksum_;
    }

    public long timestamp() {
        return timestamp_;
    }

    private String checksum_;
    private long timestamp_;
}
