package pl.mareksom.potentialwaffle;

public interface StatusUpdater {
    void initProgress(int max, String description);
    void setProgress(int progress);
    void addProgress(int deltaProgress);
    void setProgressDescription(String description);
    void setIndeterminate(boolean indeterminate, String description);

    void cleanLog();
    void log(String message);
    void setErrorOnTextView(int textView, String error);
}
