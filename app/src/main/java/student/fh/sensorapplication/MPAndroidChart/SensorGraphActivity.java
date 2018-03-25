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
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import student.fh.sensorapplication.R;

public class SensorGraphActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorMgr = null;
    private Sensor aLinearSensor = null;
    private Sensor aSensor = null;
    private Sensor gSensor = null;

    private Handler mHandler;
    private Runnable mTimer;

    private boolean isGyroscope;
    private boolean isLinearAcc;
    private boolean isRunning = false;
    private boolean updateGraph = true;
    private boolean isFilter = false;
    private boolean isWriting = false;

    private float xAccel, yAccel, zAccel;
    private float xGyro, yGyro, zGyro;

    private float[] gravity = new float[3];

    private static final float NS2S = 1.0f / 1000000000.0f;
    private final float[] deltaRotationVector = new float[4];
    private float timestamp;
    private static final int TIMEOUT = 100;  // 100 Millisecond
    private static final long NanosecondToMillisecond = (long)1E6;
    private long lastTimestamp = -1;
    private long startTime = 0;

    private String databaseValue;
    private String comma = ";";
    private String fileName = "";
    private StringBuffer buff = new StringBuffer();
    private StringBuffer titlesBuffer = new StringBuffer();

    private LineChart chartAccelerometer;
    private LineChart chartGyroscope;


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
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        // Actionbar
        ActionBar ab = getSupportActionBar();
        if(ab != null)
        {
            ab.setDisplayHomeAsUpEnabled(true);
        }


        // MPAndroidChart-Framework initialisieren
        chartAccelerometer = findViewById(R.id.chartAccelerometer);
        chartGyroscope     = findViewById(R.id.chartGyroscope);


        mHandler = new Handler();

        // Titel für die Excel-Tabelle festlegen
        titlesBuffer.append("Timestamp");
        titlesBuffer.append(comma);
        titlesBuffer.append(comma);
        titlesBuffer.append("Accelerometer X");
        titlesBuffer.append(comma);
        titlesBuffer.append("Accelerometer Y");
        titlesBuffer.append(comma);
        titlesBuffer.append("Accelerometer Z");
        titlesBuffer.append(comma);
        titlesBuffer.append(comma);
        titlesBuffer.append("Gyroscope X");
        titlesBuffer.append(comma);
        titlesBuffer.append("Gyroscope Y");
        titlesBuffer.append(comma);
        titlesBuffer.append("Gyroscope Z");
        titlesBuffer.append("\n");


        // Die Sensoren werden auf Verfügbarkeit überprüft und registriert
        sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);

        if(sensorMgr != null)
        {
            aSensor = sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            aLinearSensor = sensorMgr.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            gSensor = sensorMgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }

        if(aSensor.getType() == Sensor.TYPE_ACCELEROMETER)
        {
            sensorMgr.registerListener(this, aSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }


        if (aLinearSensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION)
        {
            sensorMgr.registerListener(this, aLinearSensor, SensorManager.SENSOR_DELAY_FASTEST);
            isLinearAcc = true;
        }
        else
        {
            isLinearAcc = false;
        }

        if (gSensor.getType() == Sensor.TYPE_GYROSCOPE)
        {
            sensorMgr.registerListener(this, gSensor, SensorManager.SENSOR_DELAY_FASTEST);
            isGyroscope = true;
        }
        else
        {
            isGyroscope = false;
            Toast.makeText(this, "Kein Gyroskop vorhanden!", Toast.LENGTH_LONG).show();
        }




        initGraph(chartAccelerometer, "Accelerometer");
        initGraph(chartGyroscope, "Gyroscope");


        // Das Firebase-Framework "Realtime Database" dient zum Starten der Geräte zur gleichen Zeit
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child("sensor");
        mDatabase.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                try
                {
                    databaseValue = dataSnapshot.getValue(String.class);

                    if(databaseValue != null)
                    {
                        if(databaseValue.equalsIgnoreCase("start"))
                        {
                            chartAccelerometer.clearValues();
                            chartGyroscope.clearValues();
                            sensorMgr.registerListener(SensorGraphActivity.this, aLinearSensor, SensorManager.SENSOR_DELAY_FASTEST);
                            sensorMgr.registerListener(SensorGraphActivity.this, aSensor, SensorManager.SENSOR_DELAY_FASTEST);
                            sensorMgr.registerListener(SensorGraphActivity.this, gSensor, SensorManager.SENSOR_DELAY_FASTEST);
                            startTime = System.currentTimeMillis();
                            isRunning = !isRunning;
                            updateGraph = true;
                            invalidateOptionsMenu();
                            feedSensor();
                        }
                        else if(databaseValue.equalsIgnoreCase("stop"))
                        {
                            isRunning = !isRunning;
                            updateGraph = false;
                            invalidateOptionsMenu();
                            mHandler.removeCallbacks(mTimer);
                            mHandler.removeCallbacksAndMessages(null);
                            sensorMgr.unregisterListener(SensorGraphActivity.this);
                        }
                        else
                        {
                            Toast.makeText(SensorGraphActivity.this, "Warten auf Start!", Toast.LENGTH_LONG).show();
                        }
                    }
                    else
                    {
                        Toast.makeText(SensorGraphActivity.this, "Firebase - null", Toast.LENGTH_LONG).show();
                    }
                }
                catch (NullPointerException e)
                {
                    Toast.makeText(SensorGraphActivity.this, "Warten auf Start!", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {
                Log.w("loadPost:onCancelled", databaseError.toException());
            }
        });
    }


    private void initGraph(LineChart mChart, String description)
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


    /*private void drawLineGraph(LineChart lineChart)
    {
        Paint paint = lineChart.getRenderer().getPaintRender();
        int height = lineChart.getHeight();

        LinearGradient linGrad = new LinearGradient(0, 0, 0, height,
                getResources().getColor(android.R.color.holo_red_dark),
                getResources().getColor(android.R.color.holo_blue_dark),
                Shader.TileMode.REPEAT);
        paint.setShader(linGrad);

        // Don't forget to refresh the drawing
        lineChart.invalidate();
    }*/


    private void addEntry(LineChart chart, Float valueX, Float valueY, Float valueZ)
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


    private LineDataSet createSet(String label, Integer color)
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


    private void feedSensor()
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
    }


    @Override
    public void onResume()
    {
        super.onResume();
        revokePermission();

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

        sensorMgr.registerListener(this, aSensor, SensorManager.SENSOR_DELAY_FASTEST);
        chartAccelerometer.clearValues();
        chartGyroscope.clearValues();
    }

    @Override
    public void onPause()
    {
        super.onPause();

        mHandler.removeCallbacks(mTimer);
        mHandler.removeCallbacksAndMessages(null);
        sensorMgr.unregisterListener(this);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        finish();
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
        long hour = TimeUnit.MILLISECONDS.toHours(millis);
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
                String dateString = String.format(Locale.GERMANY, "%02d:%02d:%02d.%d", hour, minute, second, (millis / 100 * 100));

                buff.append(String.valueOf(dateString));
                buff.append(comma);
                buff.append(comma);
                buff.append(String.valueOf(xAccel));
                buff.append(comma);
                buff.append(String.valueOf(yAccel));
                buff.append(comma);
                buff.append(String.valueOf(zAccel));
                buff.append(comma);
                buff.append(comma);
                buff.append(String.valueOf(xGyro));
                buff.append(comma);
                buff.append(String.valueOf(yGyro));
                buff.append(comma);
                buff.append(String.valueOf(zGyro));
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

        boolean isChecked = loadState();
        MenuItem item = menu.findItem(R.id.action_filter);
        item.setChecked(isChecked);
        if(item.isChecked())
        {
            isFilter = true;
        }
        else
        {
            isFilter = false;
        }

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
                saveState(item.isChecked());
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
                isRunning = !isRunning;
                updateGraph = true;
                isWriting = true;
                buff.delete(0, buff.length());
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
            menu.findItem(R.id.action_resume).setVisible(true);
            menu.findItem(R.id.action_stop).setVisible(false);
        } else {
            menu.findItem(R.id.action_resume).setVisible(false);
            menu.findItem(R.id.action_stop).setVisible(true);
        }

        return super.onPrepareOptionsMenu(menu);
    }


    private void fileSaveDialog()
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


    private void saveFile()
    {
        String finalFilename = fileName + ".csv";

        File path = null;
        File file = null;
        BufferedWriter bwr = null;

        if(!buff.toString().isEmpty())
        {
            try
            {
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                file = new File(path, finalFilename);
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
                bwr.append(titlesBuffer.toString());
                bwr.append(buff.toString());
                bwr.flush();
                bwr.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            Toast.makeText(getApplicationContext(), "Datei wurde hier gespeichert: " + path.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();
        }
        else
        {
            Toast.makeText(getApplicationContext(), "Keine Daten vorhanden!", Toast.LENGTH_LONG).show();
        }

    }


    private void shareFile()
    {
        String str = buff.toString();
        String titles = titlesBuffer.toString();

        if(!str.isEmpty())
        {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, titles);
            intent.putExtra(Intent.EXTRA_TEXT, str);
            startActivity(intent);
        }
        else
        {
            Toast.makeText(getApplicationContext(), "Keine Daten vorhanden!", Toast.LENGTH_LONG).show();
        }

    }

    public void revokePermission()
    {
        String fileName = "Accelerometer.csv";

        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File file = new File(path, fileName);
        Uri uri = FileProvider.getUriForFile(getApplicationContext(), "student.fh.sensorapplication", file);
        revokeUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    private void saveState(final boolean isChecked) {
        SharedPreferences sharedPref = getSharedPreferences("myValue", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("checkboxState", isChecked);
        editor.apply();
    }

    private boolean loadState() {
        SharedPreferences sharedPref = getSharedPreferences("myValue", MODE_PRIVATE);
        return sharedPref.getBoolean("checkboxState", false);
    }
}

