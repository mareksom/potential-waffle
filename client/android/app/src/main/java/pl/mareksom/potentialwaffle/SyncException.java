package pl.mareksom.potentialwaffle;

public class SyncException extends Exception {
    SyncException(String error) {
        super(error);
    }
}