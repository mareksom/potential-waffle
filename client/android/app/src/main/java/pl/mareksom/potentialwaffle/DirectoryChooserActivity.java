package pl.mareksom.potentialwaffle;

import android.app.Activity;
import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class DirectoryChooserActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_directory_chooser);

        Intent intent = getIntent();
        String directory_path = intent.getStringExtra(
                MainActivity.EXTRA_DIRECTORY_CHOOSER);
        directory_ = new File(directory_path);

        TextView pathView = (TextView) findViewById(R.id.directory_chooser_path_view);
        pathView.setText("//" + Environment.getExternalStorageDirectory().toURI().relativize(directory_.toURI()).getPath());

        List<String> nested_directories = new LinkedList<>();
        File[] nested_files = directory_.listFiles();
        if (nested_files != null) {
            for (File file : nested_files) {
                if (file.exists() && file.isDirectory()) {
                    nested_directories.add(file.getName());
                }
            }
        }
        final ListView listView = (ListView) findViewById(R.id.directory_chooser_list_view);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1, nested_directories.toArray(new String[0]));
        listView.setAdapter(arrayAdapter);
        final File directory = directory_;
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) listView.getItemAtPosition(position);
                Intent intent = new Intent(view.getContext(), DirectoryChooserActivity.class);
                intent.putExtra(MainActivity.EXTRA_DIRECTORY_CHOOSER,
                        new File(directory, selection).getAbsolutePath());
                startActivityForResult(intent, MainActivity.RESULT_DIRECTORY_CHOOSER);
            }
        });
    }

    public void selectDirectory(View view) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(MainActivity.EXTRA_RETURN_DIRECTORY_CHOOSER, directory_.getAbsolutePath());
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case MainActivity.RESULT_DIRECTORY_CHOOSER: {
                if (resultCode == Activity.RESULT_OK) {
                    setResult(resultCode, data);
                    finish();
                }
                break;
            }
        }
    }

    public File directory_;
}
