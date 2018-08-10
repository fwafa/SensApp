package student.fh.sensorapplication.Activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import student.fh.sensorapplication.Adapter.FilesAdapter;
import student.fh.sensorapplication.Model.ModelFile;
import student.fh.sensorapplication.Application.MyApplication;
import student.fh.sensorapplication.R;

public class FileListActivity extends AppCompatActivity implements View.OnLongClickListener{

    public  boolean isInActionMode = false;

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private RecyclerView.Adapter adapter;

    private Toolbar toolbar;
    private ActionBar actionBar;
    private TextView tvCounter;

    private ArrayList<ModelFile> fileArrayList = new ArrayList<>();
    private ArrayList<ModelFile> selectionList = new ArrayList<>();
    private int counter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        setContentView(R.layout.activity_file_list);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        tvCounter = findViewById(R.id.counter_text);
        tvCounter.setVisibility(View.GONE);

        recyclerView = findViewById(R.id.recyclerViewFiles);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);

        File root = new File(MyApplication.getAppContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "/Sensordaten");

        if(!root.mkdir())
        {
            root.mkdir();
        }

        listDir(root);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_file_list_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId())
        {
            case R.id.item_firebase:

                if(!selectionList.isEmpty())
                {
                    FilesAdapter filesAdapter = (FilesAdapter) adapter;
                    filesAdapter.uploadFileToFirebaseStorage(selectionList);
                    clearActionMode();
                }
                else
                {
                    Toast.makeText(this, "Bitte zuerst Datei auswählen!", Toast.LENGTH_SHORT).show();
                }

                break;

            case android.R.id.home:
                clearActionMode();
                adapter.notifyDataSetChanged();
                break;

            case R.id.item_share:
                return true;

            case R.id.item_delete:

                if(!selectionList.isEmpty())
                {
                    final FilesAdapter mAdapter = (FilesAdapter) adapter;

                    AlertDialog.Builder builder = new AlertDialog.Builder(FileListActivity.this);
                    builder.setTitle("Dateien löschen?");
                    builder.setMessage("Wollen Sie endgültig die Dateien löschen?");
                    builder.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            mAdapter.deleteFiles(selectionList);
                            clearActionMode();
                        }
                    });
                    builder.setNegativeButton("Nein", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            dialog.dismiss();
                        }
                    });

                    final AlertDialog mDialog = builder.create();
                    mDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialog)
                        {
                            mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.alertButtonColor));
                            mDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.alertButtonColor));
                        }
                    });

                    mDialog.show();
                }

                break;
        }

        return true;
    }

    public void clearActionMode()
    {
        isInActionMode = false;
        toolbar.getMenu().clear();
        toolbar.inflateMenu(R.menu.menu_file_list_activity);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        tvCounter.setVisibility(View.GONE);
        tvCounter.setText("0 Objekte markiert");
        counter = 0;
        selectionList.clear();
    }

    public void listDir(File f)
    {
        File[] files = f.listFiles();
        fileArrayList.clear();

        for(File file : files)
        {
            ModelFile modelFile = new ModelFile(file, file.getName(), false);
            fileArrayList.add(modelFile);
        }

        adapter = new FilesAdapter(fileArrayList, FileListActivity.this);
        recyclerView.setAdapter(adapter);
    }



    @Override
    public boolean onLongClick(View v) {

        toolbar.getMenu().clear();
        toolbar.inflateMenu(R.menu.menu_action_mode);
        tvCounter.setVisibility(View.VISIBLE);
        isInActionMode = true;
        adapter.notifyDataSetChanged();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        return true;
    }

    public void prepareSelection(View view, int position)
    {
        if(((CheckBox)view).isChecked())
        {
            selectionList.add(fileArrayList.get(position));
            counter = counter + 1;
            updateCounter(counter);
        }
        else
        {
            selectionList.remove(fileArrayList.get(position));
            counter = counter - 1;
            updateCounter(counter);
        }
    }

    public void updateCounter(int counter)
    {
        if(counter == 0)
        {
            tvCounter.setText("0 Objekte markiert");
        }
        else
        {
            tvCounter.setText(counter + " Objekte markiert");
        }
    }

    @Override
    public void onBackPressed() {

        if(isInActionMode)
        {
            clearActionMode();
            adapter.notifyDataSetChanged();
        }
        else
        {
            super.onBackPressed();
        }
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
