package pl.mareksom.potentialwaffle;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.File;
import java.util.jar.Manifest;

class InvalidInputException extends Exception {}

public class MainActivity extends AppCompatActivity {
    public static final int REQUEST_PERMISSIONS_DIRECTORY_CHOOSER = 1;
    public static final int RESULT_DIRECTORY_CHOOSER = 2;

    public static final String EXTRA_DIRECTORY_CHOOSER = "pl.mareksom.potentialwaffle.DIRECTORY_CHOOSER";
    public static final String EXTRA_RETURN_DIRECTORY_CHOOSER = "pl.mareksom.potentialwaffle.RETURN_DIRECTORY_CHOOSER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        logViewScroll_ = (ScrollView) findViewById(R.id.log_view_scroll);
    }

    public StatusUpdater getStatusUpdater() {
        return new StatusUpdater() {
            @Override
            public void initProgress(final int max, final String description) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ProgressBar progressBar = getProgressBar();
                        TextView textView = getProgressBarText();
                        textView.setText(description);
                        progressBar.setProgress(0);
                        progressBar.setMax(max);
                        progressBar.setIndeterminate(false);
                    }
                });
            }

            @Override
            public void setProgress(final int progress) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getProgressBar().setProgress(progress);
                    }
                });
            }

            @Override
            public void addProgress(final int deltaProgress) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ProgressBar progressBar = getProgressBar();
                        progressBar.setProgress(progressBar.getProgress() + deltaProgress);
                    }
                });
            }

            @Override
            public void setProgressDescription(final String description) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getProgressBarText().setText(description);
                    }
                });
            }

            @Override
            public void setIndeterminate(final boolean indeterminate, final String description) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getProgressBar().setIndeterminate(indeterminate);
                        getProgressBarText().setText(description);
                    }
                });
            }

            @Override
            public void cleanLog() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getLog().setText("");
                    }
                });
            }

            @Override
            public void log(final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getLog().append(message);
                        logViewScroll_.post(new Runnable() {
                            @Override
                            public void run() {
                                logViewScroll_.fullScroll(ScrollView.FOCUS_DOWN);
                            }
                        });
                    }
                });
            }

            @Override
            public void setErrorOnTextView(final int id, final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView textView = (TextView) findViewById(id);
                        textView.setError(error);
                    }
                });
            }
        };
    }


    public ProgressBar getProgressBar() {
        return (ProgressBar) findViewById(R.id.progress_bar);
    }

    public TextView getProgressBarText() {
        return (TextView) findViewById(R.id.progress_bar_text);
    }

    public TextView getLog() {
        return (TextView) findViewById(R.id.log_view);
    }

    public void selectLocalDirectory(View view) {
        checkExternalStoragePermissionsAndRun(REQUEST_PERMISSIONS_DIRECTORY_CHOOSER);
    }

    public void selectLocalDirectoryFunction() {
        Intent intent = new Intent(this, DirectoryChooserActivity.class);
        intent.putExtra(EXTRA_DIRECTORY_CHOOSER,
                Environment.getExternalStorageDirectory().getAbsolutePath());
        startActivityForResult(intent, RESULT_DIRECTORY_CHOOSER);
    }

    public void sync(View view) {
        new Thread(new SyncRunner(this)).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RESULT_DIRECTORY_CHOOSER: {
                if (resultCode == Activity.RESULT_OK) {
                    String directory = data.getStringExtra(EXTRA_RETURN_DIRECTORY_CHOOSER);
                    localDirectory_ = new File(directory);
                    TextView textView = (TextView) findViewById(R.id.local_directory_path);
                    textView.setText("//" + Environment.getExternalStorageDirectory().toURI()
                            .relativize(localDirectory_.toURI()).getPath());
                }
                break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults == null) {
            return;
        }
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                getStatusUpdater().log(permissions[i] + " not granted.");
                return;
            }
        }
        switch (requestCode) {
            case REQUEST_PERMISSIONS_DIRECTORY_CHOOSER:
                selectLocalDirectoryFunction();
                break;
        }
    }

    public void checkExternalStoragePermissionsAndRun(int requestCode) {
        int readPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
        int writePermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (readPermission == PackageManager.PERMISSION_GRANTED && writePermission == PackageManager.PERMISSION_GRANTED) {
            onRequestPermissionsResult(requestCode, new String[]{}, new int[]{});
            return;
        }
        ActivityCompat.requestPermissions(
                this,
                new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE,
                             android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                requestCode);
        return;
    }

    private ScrollView logViewScroll_;
    public File localDirectory_;
}
