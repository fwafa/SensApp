package student.fh.sensorapplication.MPAndroidChart;


import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.Payload;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import student.fh.sensorapplication.Adapter.RecyclerViewAdapter;
import student.fh.sensorapplication.Application.MyApplication;
import student.fh.sensorapplication.Nearby.NearbyConnection;
import student.fh.sensorapplication.R;
import student.fh.sensorapplication.fragment.DialogFragmentClient;

public class SensorGraphActivity extends AppCompatActivity implements SensorEventListener {

    public static SensorManager sensorMgr = null;
    public static Sensor aLinearSensor = null;
    public static Sensor aSensor = null;
    public static Sensor gSensor = null;

    public static Handler mHandler;
    public static Runnable mTimer;

    private RecyclerView.Adapter adapter;
    private NearbyConnection nearbyConnection;

    private AlertDialog mAlertDialog;
    private ActionBar actionBar;
    private Toolbar myToolbar;

    private boolean isGyroscope;
    private boolean isLinearAcc;
    public static boolean isRunning = false;
    public static boolean updateGraph = true;
    private boolean isFilter = false;
    public static boolean isWriting = false;

    public static float xAccel, yAccel, zAccel;
    public static float xGyro, yGyro, zGyro;
    public static float[] gravity = new float[3];

    private final float NS2S = 1.0f / 1000000000.0f;
    private final float[] deltaRotationVector = new float[4];
    private float timestamp;
    private final int TIMEOUT = 100;  // 100 Millisecond
    private final long NanosecondToMillisecond = (long)1E6;
    private long lastTimestamp = -1;
    public static long startTime = 0;

    private String comma = ";";
    private String fileName = "";
    public static StringBuffer buff;

    public static LineChart chartAccelerometer;
    public static LineChart chartGyroscope;

    private FragmentManager fm;
    private DialogFragmentClient dialogFragmentClient;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        setContentView(R.layout.activity_sensor_graph);

        // Toolbar
        myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        // Actionbar
        actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        nearbyConnection = new NearbyConnection(this, this);

        adapter = new RecyclerViewAdapter(NearbyConnection.endPoints);
        fm = getSupportFragmentManager();
        dialogFragmentClient = DialogFragmentClient.newInstance("Hosts", adapter);


        // MPAndroidChart-Framework initialisieren
        chartAccelerometer = findViewById(R.id.chartAccelerometer);
        chartGyroscope     = findViewById(R.id.chartGyroscope);

        mHandler = new Handler();
        sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);

        if(sensorMgr != null)
        {
            aSensor = sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gSensor = sensorMgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

            if(sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null)
            {
                sensorMgr.registerListener(this, aSensor, SensorManager.SENSOR_DELAY_FASTEST);
            }
            else
            {
                Toast.makeText(this, "Kein Accelerometer Sensor vorhanden!", Toast.LENGTH_LONG).show();
            }

            if(sensorMgr.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null)
            {
                aLinearSensor = sensorMgr.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
                sensorMgr.registerListener(this, aLinearSensor, SensorManager.SENSOR_DELAY_FASTEST);
                isLinearAcc = true;
            }
            else
            {
                isLinearAcc = false;
            }

            if(sensorMgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null)
            {
                sensorMgr.registerListener(this, gSensor, SensorManager.SENSOR_DELAY_FASTEST);
                isGyroscope = true;
            }
            else
            {
                isGyroscope = false;
                Toast.makeText(this, "Kein Gyroskop Sensor vorhanden!", Toast.LENGTH_LONG).show();
            }
        }


        initGraph(chartAccelerometer, "Accelerometer");
        initGraph(chartGyroscope, "Gyroscope");
    }

    private static void initGraph(LineChart mChart, String description)
    {
        mChart.setTouchEnabled(true);
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(false);
        mChart.setPinchZoom(true);
        mChart.setBackgroundColor(Color.TRANSPARENT);
        mChart.getDescription().setText(description);
        mChart.getDescription().setTextColor(Color.YELLOW);
        mChart.getDescription().setTextSize(10f);
        //mChart.animateXY((int) TimeUnit.SECONDS.toMillis(5), (int) TimeUnit.SECONDS.toMillis(5));

        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

        mChart.setData(data);

        Legend l = mChart.getLegend();
        l.setEnabled(true);
        l.setForm(Legend.LegendForm.SQUARE);
        l.setTextColor(Color.WHITE);

        XAxis xl = mChart.getXAxis();
        xl.setPosition(XAxis.XAxisPosition.BOTTOM);
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setLabelCount(5);
        xl.setValueFormatter(new MyAxisValueFormatter());
        xl.setEnabled(true);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMaximum(20f);
        leftAxis.setAxisMinimum(-20f);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    private static void addEntry(LineChart chart, Float valueX, Float valueY, Float valueZ)
    {
        LineData data = chart.getData();

        if (data != null)
        {
            if(updateGraph)
            {
                ILineDataSet setX = data.getDataSetByIndex(0);
                ILineDataSet setY = data.getDataSetByIndex(1);
                ILineDataSet setZ = data.getDataSetByIndex(2);


                if (setX == null || setY == null || setZ == null)
                {
                    setX = createSet("X", ColorTemplate.getHoloBlue());
                    data.addDataSet(setX);

                    setY = createSet("Y", Color.RED);
                    data.addDataSet(setY);

                    setZ = createSet("Z", Color.GREEN);
                    data.addDataSet(setZ);
                }

                setX.addEntry(new Entry(setX.getEntryCount(), valueX));
                setY.addEntry(new Entry(setY.getEntryCount(), valueY));
                setZ.addEntry(new Entry(setZ.getEntryCount(), valueZ));
                data.notifyDataChanged();
                chart.notifyDataSetChanged();
                chart.setVisibleXRangeMaximum(100);
                chart.moveViewToX(data.getEntryCount());
            }
        }
    }

    private static LineDataSet createSet(String label, Integer color)
    {
        LineDataSet set = new LineDataSet(null, label);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(color);
        set.setLineWidth(2f);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setDrawHighlightIndicators(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        /*Drawable drawable = ContextCompat.getDrawable(this, R.drawable.background);
        set.setDrawFilled(true);
        set.setFillDrawable(drawable);*/

        return set;
    }

    public static void feedSensor()
    {
        mTimer = new Runnable() {
            @Override
            public void run()
            {
                //drawLineGraph(chartAccelerometer);
                //drawLineGraph(chartGyroscope);

                addEntry(chartAccelerometer, xAccel, yAccel, zAccel);
                addEntry(chartGyroscope, xGyro, yGyro, zGyro);

                mHandler.postDelayed(mTimer, 100);
            }
        };

        mHandler.postDelayed(mTimer, 0);

        /*try
        {
            Nearby.getConnectionsClient(MyApplication.getAppContext())
                    .sendPayload(
                            NearbyConnection.hostEndpointId,
                            Payload.fromBytes("isActive".getBytes("UTF-8")));
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }*/
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if(sensorMgr.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null)
        {
            aLinearSensor = sensorMgr.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            sensorMgr.registerListener(this, aLinearSensor, SensorManager.SENSOR_DELAY_FASTEST);
            isLinearAcc = true;
        }
        else
        {
            isLinearAcc = false;
        }

        if(sensorMgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null)
        {
            gSensor = sensorMgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            sensorMgr.registerListener(this, gSensor, SensorManager.SENSOR_DELAY_FASTEST);
            isGyroscope = true;
            chartGyroscope.clearValues();
        }
        else
        {
            isGyroscope = false;
        }

        if(sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null)
        {
            sensorMgr.registerListener(this, aSensor, SensorManager.SENSOR_DELAY_FASTEST);
            chartAccelerometer.clearValues();
            chartGyroscope.clearValues();
        }
        else
        {
            Toast.makeText(this, "Kein Accelerometer Sensor vorhanden!", Toast.LENGTH_LONG).show();
        }


        buff = new StringBuffer();
        //titlesBuffer = new StringBuffer();
        nearbyConnection.endPoints.clear();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onPause()
    {
        super.onPause();

        mHandler.removeCallbacks(mTimer);
        mHandler.removeCallbacksAndMessages(null);
        sensorMgr.unregisterListener(this);

        nearbyConnection.endPoints.clear();
        adapter.notifyDataSetChanged();

        Nearby.getConnectionsClient(this).stopDiscovery();
        Nearby.getConnectionsClient(this).stopAdvertising();
        Nearby.getConnectionsClient(this).stopAllEndpoints();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        nearbyConnection.endPoints.clear();
        adapter.notifyDataSetChanged();

        Nearby.getConnectionsClient(this).stopDiscovery();
        Nearby.getConnectionsClient(this).stopAdvertising();
        Nearby.getConnectionsClient(this).stopAllEndpoints();

        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        sensorMgr.unregisterListener(this);
        buff.setLength(0);
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        final float[] aValues = event.values;


        long currentTimestamp = (event.timestamp / NanosecondToMillisecond);
        long elapsedTime = currentTimestamp - lastTimestamp;

        long millis = System.currentTimeMillis() - startTime;
        long second = TimeUnit.MILLISECONDS.toSeconds(millis);
        long minute = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.SECONDS.toMillis(second);


        if(event.sensor.getType() == aSensor.getType() && !isFilter)
        {
            xAccel = aValues[0];
            yAccel = aValues[1];
            zAccel = aValues[2];
        }

        if(isFilter)
        {
            if(isLinearAcc)
            {
                if(event.sensor.getType() == aLinearSensor.getType())
                {
                    xAccel = aValues[0];
                    yAccel = aValues[1];
                    zAccel = aValues[2];
                }
            }
            else
            {
                final float alpha = 0.8f;

                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

                xAccel = event.values[0] - gravity[0];
                yAccel = event.values[1] - gravity[1];
                zAccel = event.values[2] - gravity[2];
            }
        }


        if(isGyroscope)
        {
            if(event.sensor.getType() == gSensor.getType())
            {
                // This time step's delta rotation to be multiplied by the current rotation
                // after computing it from the gyro sample data.
                if (timestamp != 0)
                {
                    if(isFilter)
                    {
                        final float dT = (event.timestamp - timestamp) * NS2S;
                        // Axis of the rotation sample, not normalized yet.
                        float axisX = event.values[0];
                        float axisY = event.values[1];
                        float axisZ = event.values[2];

                        float omegaMagnitude = (float) Math.sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);

                        if (omegaMagnitude > (short) 0x1400)
                        {
                            axisX /= omegaMagnitude;
                            axisY /= omegaMagnitude;
                            axisZ /= omegaMagnitude;
                        }

                        float thetaOverTwo = omegaMagnitude * dT / 2.0f;
                        float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
                        float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
                        deltaRotationVector[0] = sinThetaOverTwo * axisX;
                        deltaRotationVector[1] = sinThetaOverTwo * axisY;
                        deltaRotationVector[2] = sinThetaOverTwo * axisZ;
                        deltaRotationVector[3] = cosThetaOverTwo;

                        xGyro = deltaRotationVector[0];
                        yGyro = deltaRotationVector[1];
                        zGyro = deltaRotationVector[2];
                    }
                    else
                    {
                        xGyro = event.values[0];
                        yGyro = event.values[1];
                        zGyro = event.values[2];
                    }
                }
                timestamp = event.timestamp;
                float[] deltaRotationMatrix = new float[9];
                SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
                // User code should concatenate the delta rotation we computed with the current
                // rotation in order to get the updated rotation.
                // rotationCurrent = rotationCurrent * deltaRotationMatrix;
            }
        }

        if(isWriting)
        {
            if(elapsedTime >= TIMEOUT)
            {
                String dateString = String.format(Locale.GERMAN, "%02d:%02d.%d", minute, second, (millis / 100 * 100));

                String xA = String.format(Locale.GERMAN, "%.6f", xAccel);
                String yA = String.format(Locale.GERMAN, "%.6f", yAccel);
                String zA = String.format(Locale.GERMAN, "%.6f", zAccel);
                String xG = String.format(Locale.GERMAN, "%.6f", xGyro);
                String yG = String.format(Locale.GERMAN, "%.6f", yGyro);
                String zG = String.format(Locale.GERMAN, "%.6f", zGyro);


                buff.append(String.valueOf(dateString));
                buff.append(comma);
                buff.append(comma);
                buff.append(xA);
                buff.append(comma);
                buff.append(yA);
                buff.append(comma);
                buff.append(zA);
                buff.append(comma);
                buff.append(comma);
                buff.append(xG);
                buff.append(comma);
                buff.append(yG);
                buff.append(comma);
                buff.append(zG);
                buff.append("\n");

                lastTimestamp = currentTimestamp;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_graph_view_activity, menu);

        boolean isChecked = loadState("checkboxValue");
        MenuItem item = menu.findItem(R.id.action_filter);
        item.setChecked(isChecked);
        isFilter = item.isChecked();

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_settings:
                break;

            case R.id.action_filter:

                isFilter = !isFilter;
                item.setChecked(!item.isChecked());
                saveState(item.isChecked(), "checkboxValue");
                break;

            case R.id.action_stop:
                onPause();
                isRunning = !isRunning;
                updateGraph = false;
                isWriting = false;
                invalidateOptionsMenu();
                break;

            case R.id.action_resume:
                onResume();
                startTime = System.currentTimeMillis();
                buff = new StringBuffer();
                buff.delete(0, buff.length());
                isRunning = !isRunning;
                updateGraph = true;
                isWriting = true;
                feedSensor();
                invalidateOptionsMenu();
                break;

            case R.id.action_send:
                shareFile();
                break;

            case R.id.action_save:
                fileSaveDialog();
                break;

            case R.id.action_share:
                return true;

            case R.id.action_search:
                nearbyConnection.startDiscovery(SensorGraphActivity.this);
                break;

            default:
                return super.onOptionsItemSelected(item);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        if (isRunning)
        {
            menu.findItem(R.id.action_resume).setVisible(false);
            menu.findItem(R.id.action_stop).setVisible(true);
        } else {
            menu.findItem(R.id.action_resume).setVisible(true);
            menu.findItem(R.id.action_stop).setVisible(false);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    private void fileSaveDialog()
    {
        if(!buff.toString().isEmpty())
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Filename");

            final EditText input = new EditText(this);
            input.setHint("your_filename");
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    fileName = (input.getText().toString());
                    saveFile();
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            AlertDialog dialog = builder.create();

            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
        else
        {
            Toast.makeText(getApplicationContext(), "Keine Daten vorhanden!", Toast.LENGTH_LONG).show();
        }

    }

    private void saveFile()
    {
        String ordnerName = "Sensordaten";
        String dateiName = fileName + Build.MODEL + "_" + new Date() + ".csv";

        File path = null;
        File file = null;
        BufferedWriter bwr = null;

        try
        {
            path = new File(MyApplication.getAppContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), ordnerName);
            file = new File(path, dateiName);
            bwr = new BufferedWriter(new FileWriter(file));
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        if(!path.mkdirs())
        {
            path.mkdir();
        }

        if (!file.exists())
        {
            try
            {
                file.createNewFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        try
        {
            if (bwr != null)
            {
                bwr.append(buff.toString());
                bwr.flush();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (bwr != null)
            {
                try
                {
                    bwr.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

        Toast.makeText(getApplicationContext(), "Datei wurde hier gespeichert: " + path.getAbsolutePath(),
                Toast.LENGTH_LONG).show();

    }

    private void shareFile()
    {
        String str = buff.toString();

        if(str.length() != 0)
        {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, str);
            startActivity(intent);
        }
        else
        {
            Toast.makeText(getApplicationContext(), "Keine Daten vorhanden!", Toast.LENGTH_LONG).show();
        }

    }

    private void saveState(final boolean isChecked, String value) {
        SharedPreferences sharedPref = getSharedPreferences(value, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("checkboxState", isChecked);
        editor.apply();
    }

    private boolean loadState(String value) {
        SharedPreferences sharedPref = getSharedPreferences(value, MODE_PRIVATE);
        return sharedPref.getBoolean("checkboxState", false);
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

    public void showCustomDialog()
    {
        if(!dialogFragmentClient.isAdded())
        {
            dialogFragmentClient.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialog);
            dialogFragmentClient.show(fm, "hosts_fragment");
        }
    }

    public void sendPayloadFile(StringBuffer buff, StringBuffer titlesBuffer)
    {
        String finalFilename = Build.MODEL;

        File file = null;
        BufferedWriter bwr = null;

        if(!buff.toString().isEmpty())
        {
            try
            {
                file = new File(MyApplication.getAppContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), finalFilename);
                bwr = new BufferedWriter(new FileWriter(file));
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            try
            {
                if (bwr != null)
                {
                    bwr.append(titlesBuffer.toString());
                    bwr.append(buff.toString());
                    bwr.flush();
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            finally
            {
                if (bwr != null)
                {
                    try
                    {
                        bwr.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }

        try
            {
                try
                {
                    Nearby.getConnectionsClient(MyApplication.getAppContext())
                            .sendPayload(
                                    NearbyConnection.hostEndpointId,
                                    Payload.fromBytes(finalFilename.getBytes("UTF-8")));
                }
                catch (UnsupportedEncodingException e)
                {
                    e.printStackTrace();
                }

                Nearby.getConnectionsClient(MyApplication.getAppContext())
                        .sendPayload(
                                NearbyConnection.hostEndpointId,
                                Payload.fromFile(file));
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
    }

    public static void sendPayloadStream(StringBuffer buff)
    {
        String finalFilename = Build.MODEL;
        byte[] bytes = buff.toString().getBytes();
        InputStream inputStream = new ByteArrayInputStream(bytes);

        try
        {
            Nearby.getConnectionsClient(MyApplication.getAppContext())
                    .sendPayload(
                            NearbyConnection.hostEndpointId,
                            Payload.fromBytes(finalFilename.getBytes("UTF-8")));

            Nearby.getConnectionsClient(MyApplication.getAppContext())
                    .sendPayload(
                            NearbyConnection.hostEndpointId,
                            Payload.fromStream(inputStream));
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
    }

    public static void stopDiscovering()
    {
        Nearby.getConnectionsClient(MyApplication.getAppContext()).stopDiscovery();
    }
}

