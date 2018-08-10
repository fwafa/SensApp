package student.fh.sensorapplication.Activities;



import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.karan.churi.PermissionManager.PermissionManager;

import java.io.UnsupportedEncodingException;

import student.fh.sensorapplication.Adapter.RecyclerItemClickListener;
import student.fh.sensorapplication.Adapter.RecyclerViewAdapter;
import student.fh.sensorapplication.Application.MyApplication;
import student.fh.sensorapplication.Nearby.NearbyConnection;
import student.fh.sensorapplication.R;

public class NearbyActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private boolean isRecording = true;

    public static String mExperimentName;

    public  View view;
    public  TextView tvTitle;
    private AlertDialog mAlertDialog;
    private ProgressDialog progressDialog;

    public com.github.clans.fab.FloatingActionButton fabSearch;
    public com.github.clans.fab.FloatingActionButton fabStop;
    public com.github.clans.fab.FloatingActionButton fabAction;
    public com.github.clans.fab.FloatingActionButton fabDateien;
    public com.github.clans.fab.FloatingActionMenu fabMenu;

    public  RecyclerView recyclerView;
    public  RecyclerView.Adapter mAdapter;
    public  RecyclerView.LayoutManager layoutManager;

    public  ActionBar actionBar;
    private NearbyConnection nearbyConnection;



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        setContentView(R.layout.activity_nearby);

        /**
         *  Initiate
         */

        actionBar = getSupportActionBar();

        view = getWindow().getDecorView().findViewById(R.id.relativeLayout);
        nearbyConnection = new NearbyConnection(this, getApplicationContext(), view, actionBar);

        progressDialog = new ProgressDialog(this);

        fabSearch = findViewById(R.id.fabSearch);
        fabStop = findViewById(R.id.fabStop);
        fabAction = findViewById(R.id.fabAction);
        fabDateien = findViewById(R.id.fabDateien);
        fabMenu   = findViewById(R.id.fabMenu);

        tvTitle = findViewById(R.id.tvTitle);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new RecyclerViewAdapter(NearbyConnection.endPoints, this);
        recyclerView.setAdapter(mAdapter);

        tvTitle.setVisibility(View.INVISIBLE);


        fabSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                fabMenu.close(true);
                nearbyConnection.startAdvertising();
            }
        });


        fabStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                fabMenu.close(true);

                if(mAdapter.getItemCount() == 0)
                {
                    Toast.makeText(NearbyActivity.this, "Keine Endgeräte vorhanden",
                            Toast.LENGTH_LONG).show();
                }
                else
                {
                    AlertDialog.Builder dialogStop = new AlertDialog.Builder(NearbyActivity.this);
                    dialogStop.setTitle("Verbindung trennen");
                    dialogStop.setMessage("Möchten Sie die Verbindung zu den Endgeräten trennen?");
                    dialogStop.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            Nearby.getConnectionsClient(NearbyActivity.this).stopAllEndpoints();
                            NearbyConnection.endPoints.clear();
                            mAdapter.notifyDataSetChanged();
                        }
                    });
                    dialogStop.setNegativeButton("Nein", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            dialog.dismiss();
                        }
                    });

                    final AlertDialog stopDialog = dialogStop.create();

                    stopDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialog) {

                            stopDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.alertButtonColor));
                            stopDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.alertButtonColor));
                        }
                    });

                    stopDialog.show();
                }
            }
        });


        fabDateien.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                fabMenu.close(true);

                Intent intent = new Intent(NearbyActivity.this, FileListActivity.class);
                startActivity(intent);
            }
        });


        fabAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                fabMenu.close(true);

                if(mAdapter.getItemCount() == 0)
                {
                    Toast.makeText(NearbyActivity.this, "Keine Endgeräte vorhanden.\nBitte zuerst suchen!",
                            Toast.LENGTH_LONG).show();
                }
                else
                {
                    if(isRecording)
                    {
                        AlertDialog.Builder builder1 = new AlertDialog.Builder(NearbyActivity.this);
                        LayoutInflater inflater = NearbyActivity.this.getLayoutInflater();
                        View dialogView = inflater.inflate(R.layout.custom_foldername_dialog, null);
                        builder1.setView(dialogView);

                        final EditText input = dialogView.findViewById(R.id.etDialog);

                        builder1.setTitle("Experiment");
                        builder1.setMessage("Bitte geben Sie einen Namen für das Experiment!");
                        builder1.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                if(!input.getText().toString().isEmpty())
                                {
                                    mExperimentName = input.getText().toString();

                                    AlertDialog.Builder builder = new AlertDialog.Builder(NearbyActivity.this);
                                    builder.setTitle("Vorgang starten");
                                    builder.setMessage("Möchten Sie die vorhandenen Geräte starten?");
                                    builder.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                            Nearby.getConnectionsClient(NearbyActivity.this).stopAdvertising();

                                            isRecording = !isRecording;
                                            fabAction.setImageResource(R.drawable.ic_stop_36dp);

                                            try
                                            {
                                                for(int i=0; i<NearbyConnection.endPoints.size(); i++)
                                                {
                                                    Nearby.getConnectionsClient(NearbyActivity.this)
                                                            .sendPayload(
                                                                    NearbyConnection.endPoints.get(i).getEndpointKey(),
                                                                    Payload.fromBytes("start".getBytes("UTF-8")));
                                                }

                                            }
                                            catch (UnsupportedEncodingException e)
                                            {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                    builder.setNegativeButton("Nein", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                            dialog.dismiss();
                                        }
                                    });

                                    final AlertDialog mDialog = builder.create();

                                    mDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                                        @Override
                                        public void onShow(DialogInterface dialog) {

                                            mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.alertButtonColor));
                                            mDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.alertButtonColor));
                                        }
                                    });

                                    mDialog.show();
                                }
                                else
                                {
                                    input.setError("Eingabefeld leer!");
                                }
                            }
                        });
                        builder1.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                dialog.dismiss();
                            }
                        });

                        final AlertDialog mDialog1 = builder1.create();
                        mDialog1.setOnShowListener(new DialogInterface.OnShowListener() {
                            @Override
                            public void onShow(DialogInterface dialog)
                            {
                                mDialog1.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.alertButtonColor));
                            }
                        });

                        mDialog1.show();
                    }
                    else
                    {
                        AlertDialog.Builder builder = new AlertDialog.Builder(NearbyActivity.this);
                        builder.setTitle("Vorgang beenden");
                        builder.setMessage("Möchten Sie den Vorgang beenden?");
                        builder.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                isRecording = !isRecording;
                                fabAction.setImageResource(R.drawable.ic_play_36dp);

                                try
                                {
                                    for(int i=0; i<NearbyConnection.endPoints.size(); i++)
                                    {
                                        Nearby.getConnectionsClient(NearbyActivity.this)
                                                .sendPayload(
                                                        NearbyConnection.endPoints.get(i).getEndpointKey(),
                                                        Payload.fromBytes("stop".getBytes("UTF-8")));
                                    }

                                }
                                catch (UnsupportedEncodingException e)
                                {
                                    e.printStackTrace();
                                }
                            }
                        });
                        builder.setNegativeButton("Nein", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                dialog.dismiss();
                            }
                        });
                        builder.setNeutralButton("Wiederholen", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                fabAction.setImageResource(R.drawable.ic_play_36dp);

                                try
                                {
                                    for(int i=0; i<NearbyConnection.endPoints.size(); i++)
                                    {
                                        Nearby.getConnectionsClient(NearbyActivity.this)
                                                .sendPayload(
                                                        NearbyConnection.endPoints.get(i).getEndpointKey(),
                                                        Payload.fromBytes("start".getBytes("UTF-8")));
                                    }

                                }
                                catch (UnsupportedEncodingException e)
                                {
                                    e.printStackTrace();
                                }
                            }
                        });

                        final AlertDialog mDialog = builder.create();

                        mDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                            @Override
                            public void onShow(DialogInterface dialog) {

                                mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.alertButtonColor));
                                mDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.alertButtonColor));
                                mDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(getResources().getColor(R.color.alertButtonColor));
                            }
                        });

                        mDialog.show();
                    }
                }
            }
        });


        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(
                this, recyclerView, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {

                Nearby.getConnectionsClient(NearbyActivity.this).requestConnection(
                        Build.MODEL,
                        NearbyConnection.mEndpointId,
                        nearbyConnection.getConnectionLifecycleCallback())
                        .addOnSuccessListener(new OnSuccessListener<Void>()
                        {
                            @Override
                            public void onSuccess(Void unusedResult)
                            {
                                NearbyConnection.endPoints.clear();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener()
                        {
                            @Override
                            public void onFailure(@NonNull Exception e)
                            {
                                // Nearby Connections failed to request the connection.
                            }
                        });

            }

            @Override
            public void onLongItemClick(View view, int position) {

                Toast.makeText(NearbyActivity.this, "Funktioniert", Toast.LENGTH_SHORT).show();
            }
        }));
    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Nearby.getConnectionsClient(this).stopAdvertising();
        Nearby.getConnectionsClient(this).stopDiscovery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Nearby.getConnectionsClient(this).stopAdvertising();
        Nearby.getConnectionsClient(this).stopDiscovery();
        Nearby.getConnectionsClient(this).stopAllEndpoints();

        NearbyConnection.endPoints.clear();
        mAdapter.notifyDataSetChanged();
    }


    public void setProgressDialog(String text)
    {
        int llPadding = 30;
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setPadding(llPadding, llPadding, llPadding, llPadding);
        ll.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams llParam = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        llParam.gravity = Gravity.CENTER;
        ll.setLayoutParams(llParam);

        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        progressBar.setPadding(0, 0, llPadding, 0);
        progressBar.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.alertButtonColor), android.graphics.PorterDuff.Mode.MULTIPLY);
        progressBar.setLayoutParams(llParam);

        llParam = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        llParam.gravity = Gravity.CENTER;
        TextView tvText = new TextView(this);
        tvText.setText(text);
        tvText.setTextColor(Color.parseColor("#000000"));
        tvText.setTextSize(20);
        tvText.setLayoutParams(llParam);

        ll.addView(progressBar);
        ll.addView(tvText);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setView(ll);

        mAlertDialog = builder.create();
        mAlertDialog.show();
        Window window = mAlertDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(mAlertDialog.getWindow().getAttributes());
            layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
            layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
            mAlertDialog.getWindow().setAttributes(layoutParams);
        }
    }

    public void removeProgressDialog()
    {
        mAlertDialog.dismiss();
    }


    public void showProgressDialog(View view)
    {
        progressDialog.setMessage("Empfange Daten...");
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.show();

        new MyAsyncTaskClass().execute(0);
    }


    class MyAsyncTaskClass extends AsyncTask<Integer, Integer, String>
    {

        @Override
        protected String doInBackground(Integer... integers)
        {
            try
            {
                Thread.sleep(5000);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }

            return "Task Completed";
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            progressDialog.dismiss();
        }
    }
}
