package student.fh.sensorapplication.Nearby;


import android.app.ProgressDialog;
import android.content.Context;
import android.hardware.SensorManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.util.SimpleArrayMap;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.View;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import student.fh.sensorapplication.Activities.MainActivity;
import student.fh.sensorapplication.Activities.NearbyActivity;
import student.fh.sensorapplication.Adapter.RecyclerViewAdapter;
import student.fh.sensorapplication.Application.MyApplication;
import student.fh.sensorapplication.FileConvertion.DataConvertionUtil;
import student.fh.sensorapplication.MPAndroidChart.SensorGraphActivity;
import student.fh.sensorapplication.Model.ModelDevice;
import student.fh.sensorapplication.R;
import student.fh.sensorapplication.fragment.DialogFragmentClient;

public class NearbyConnection {

    private final SimpleArrayMap<String, Payload> incomingPayloads = new SimpleArrayMap<>();
    private final SimpleArrayMap<String, String> incomingFilenames = new SimpleArrayMap<>();


    private static final String TAG = MainActivity.class.getSimpleName();

    private static final Strategy STRATEGY = Strategy.P2P_STAR;
    private static final String SERVICE_ID = "student.fh.sensorapplication";

    private DialogFragmentClient dialogFragmentClient;
    private SensorGraphActivity sensorGraphActivity = new SensorGraphActivity();
    private NearbyActivity mActivity;
    private ModelDevice modelConnectionResult;

    private ActionBar actionBar;
    private Context dfContext;
    private Context sgContext;
    private Context context;
    private Context discoveryContext;
    private View view;

    private Map<String, ModelDevice> modelHashMap = new HashMap<>();
    public  static List<ModelDevice> endPoints = new ArrayList<>();
    public  static String mEndpointId;
    public  static String hostEndpointId;

    public  boolean isClient = false;
    public  static boolean isFileSent = false;
    private boolean isDiscoverer = false;
    private boolean isAdvertiser = false;

    private ConnectionInfo mConnectionInfo;


    public NearbyConnection(NearbyActivity mActivity, Context context, View view, ActionBar actionBar)
    {
        this.mActivity = mActivity;
        this.actionBar = actionBar;
        this.context = context;
        this.view = view;
    }

    public NearbyConnection(SensorGraphActivity sensorActivity, Context context)
    {
        this.sensorGraphActivity = sensorActivity;
        this.sgContext = context;
    }

    public NearbyConnection(DialogFragmentClient dialogFragmentClient, Context context)
    {
        this.dialogFragmentClient = dialogFragmentClient;
        this.dfContext = context;
    }



    public void startAdvertising()
    {
        AdvertisingOptions.Builder builder = new AdvertisingOptions.Builder();
        builder.setStrategy(STRATEGY);

        Nearby.getConnectionsClient(context).startAdvertising(
                Build.MODEL,
                SERVICE_ID,
                getConnectionLifecycleCallback(),
                builder.build())
                .addOnSuccessListener(new OnSuccessListener<Void>()
                {
                    @Override
                    public void onSuccess(Void unusedResult)
                    {
                        // We're advertising!
                        actionBar.setTitle("Host - " + Build.MODEL);
                        Snackbar.make(view.findViewById(R.id.relativeLayout),
                                "Ausstrahlung gestartet",
                                Snackbar.LENGTH_SHORT).show();

                        mActivity.setProgressDialog("Searching...");

                    }
                })
                .addOnFailureListener(new OnFailureListener()
                {
                    @Override
                    public void onFailure(@NonNull Exception e)
                    {
                        Snackbar.make(view.findViewById(R.id.relativeLayout),
                                "Ausstrahlung fehlgeschlagen",
                                Snackbar.LENGTH_SHORT).show();
                    }
                });
    }



    public void startDiscovery(Context context)
    {
        sensorGraphActivity.setProgressDialog("Searching...");
        discoveryContext = context;

        DiscoveryOptions.Builder builder = new DiscoveryOptions.Builder();
        builder.setStrategy(STRATEGY);

        Nearby.getConnectionsClient(context).startDiscovery(
                SERVICE_ID,
                getEndpointDiscoveryCallback(),
                builder.build())
                .addOnSuccessListener(
                        new OnSuccessListener<Void>()
                        {
                            @Override
                            public void onSuccess(Void unusedResult)
                            {

                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener()
                        {
                            @Override
                            public void onFailure(@NonNull Exception e)
                            {
                                // We were unable to start discovering.
                            }
                        });
    }


    public EndpointDiscoveryCallback getEndpointDiscoveryCallback()
    {
        EndpointDiscoveryCallback mEndpointDiscoveryCallback = new EndpointDiscoveryCallback()
        {

            ModelDevice model;

            @Override
            public void onEndpointFound(
                    final String endpointId, final DiscoveredEndpointInfo discoveredEndpointInfo)
            {
                // An endpoint was found!

                sensorGraphActivity.removeProgressDialog();
                sensorGraphActivity.showCustomDialog();

                RecyclerViewAdapter.icConnected = false;

                mEndpointId = endpointId;

                model = new ModelDevice(
                        endpointId,
                        discoveredEndpointInfo.getEndpointName(),
                        RecyclerViewAdapter.icConnected);

                modelHashMap.put(endpointId, model);
                endPoints.add(modelHashMap.get(endpointId));
                DialogFragmentClient.adapter.notifyDataSetChanged();
            }

            @Override
            public void onEndpointLost(String endpointId)
            {
                RecyclerViewAdapter.icConnected = false;

                endPoints.remove(modelHashMap.get(endpointId));
                DialogFragmentClient.adapter.notifyDataSetChanged();
            }
        };

        return mEndpointDiscoveryCallback;
    }




    public ConnectionLifecycleCallback getConnectionLifecycleCallback()
    {
        ConnectionLifecycleCallback mConnectionLifecycleCallback = new ConnectionLifecycleCallback()
        {
            @Override
            public void onConnectionInitiated(
                    String endpointId, ConnectionInfo connectionInfo) {


                if(connectionInfo.isIncomingConnection())
                {
                    isAdvertiser = true;
                    Nearby.getConnectionsClient(context).acceptConnection(endpointId, mPayloadCallback);
                }
                else
                {
                    hostEndpointId = endpointId;

                    isDiscoverer = true;
                    Nearby.getConnectionsClient(dfContext).acceptConnection(endpointId, mPayloadCallback);
                }

                mConnectionInfo = connectionInfo;
            }

            @Override
            public void onConnectionResult(String endpointId, ConnectionResolution result) {
                switch (result.getStatus().getStatusCode()) {

                    case ConnectionsStatusCodes.STATUS_OK:
                        // We're connected! Can now start sending and receiving data.

                        RecyclerViewAdapter.icConnected = true;

                        mEndpointId = endpointId;

                        modelConnectionResult = new ModelDevice(
                                endpointId,
                                mConnectionInfo.getEndpointName(),
                                RecyclerViewAdapter.icConnected);

                        modelHashMap.put(endpointId, modelConnectionResult);
                        endPoints.add(modelHashMap.get(endpointId));

                        if(isAdvertiser)
                        {
                            mActivity.tvTitle.setVisibility(View.VISIBLE);
                            mActivity.removeProgressDialog();
                            mActivity.mAdapter.notifyDataSetChanged();
                        }

                        if(isDiscoverer)
                        {
                            isClient = true;
                            DialogFragmentClient.adapter.notifyDataSetChanged();
                        }

                        break;

                    case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                        break;

                    case ConnectionsStatusCodes.STATUS_ERROR:
                        break;
                }
            }

            @Override
            public void onDisconnected(String endpointId) {

                RecyclerViewAdapter.icConnected = false;

                endPoints.remove(modelHashMap.get(endpointId));
                if(isAdvertiser)
                {
                    mActivity.tvTitle.setVisibility(View.INVISIBLE);
                    mActivity.mAdapter.notifyDataSetChanged();
                }

                if(isDiscoverer)
                {
                    DialogFragmentClient.adapter.notifyDataSetChanged();
                }
            }
        };

        return mConnectionLifecycleCallback;
    }




    private final PayloadCallback mPayloadCallback = new PayloadCallback()
    {
        @Override
        public void onPayloadReceived(@NonNull String endpointID, @NonNull Payload payload)
        {
            if (payload.getType() == Payload.Type.BYTES)
            {
                handleMessage(payload, endpointID);
            }

            else if(payload.getType() == Payload.Type.STREAM)
            {
                String fileName = incomingFilenames.remove(endpointID);
                incomingPayloads.put(endpointID, payload);
                handleStream(incomingPayloads, endpointID, fileName);
            }
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String endpointID, @NonNull PayloadTransferUpdate update)
        {

        }
    };


    private void handleMessage(Payload payload, String endpointID)
    {
        try
        {
            String message = new String(payload.asBytes(), "UTF-8");

            if(message.equalsIgnoreCase("start"))
            {
                if(isDiscoverer)
                {
                    sensorGraphActivity.stopDiscovering();
                    DialogFragmentClient.dismissDialog();

                    SensorGraphActivity.chartAccelerometer.clearValues();
                    SensorGraphActivity.chartGyroscope.clearValues();
                    SensorGraphActivity.sensorMgr.registerListener(sensorGraphActivity, SensorGraphActivity.aLinearSensor, SensorManager.SENSOR_DELAY_FASTEST);
                    SensorGraphActivity.sensorMgr.registerListener(sensorGraphActivity, SensorGraphActivity.aSensor, SensorManager.SENSOR_DELAY_FASTEST);
                    SensorGraphActivity.sensorMgr.registerListener(sensorGraphActivity, SensorGraphActivity.gSensor, SensorManager.SENSOR_DELAY_FASTEST);
                    SensorGraphActivity.startTime = System.currentTimeMillis();
                    SensorGraphActivity.isRunning = !SensorGraphActivity.isRunning;
                    SensorGraphActivity.updateGraph = true;
                    SensorGraphActivity.isWriting = true;
                    SensorGraphActivity.buff = new StringBuffer();
                    SensorGraphActivity.buff.delete(0, SensorGraphActivity.buff.length());
                    SensorGraphActivity.feedSensor();
                }
            }

            else if(message.equalsIgnoreCase("stop"))
            {
                if(isDiscoverer)
                {
                    SensorGraphActivity.isRunning = !SensorGraphActivity.isRunning;
                    SensorGraphActivity.updateGraph = false;
                    SensorGraphActivity.isWriting = false;
                    SensorGraphActivity.mHandler.removeCallbacks(SensorGraphActivity.mTimer);
                    SensorGraphActivity.mHandler.removeCallbacksAndMessages(null);
                    SensorGraphActivity.sensorMgr.unregisterListener(sensorGraphActivity);
                    SensorGraphActivity.sendPayloadStream(SensorGraphActivity.buff);

                    isFileSent = true;
                }
            }

            else
            {
                if(isAdvertiser)
                {
                    mActivity.showProgressDialog(view);
                    incomingFilenames.put(endpointID, message);
                }
            }

        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
    }


    private void handleStream(SimpleArrayMap<String, Payload> incomingPayloads, String endpointID, String fileName)
    {
        Payload payload = incomingPayloads.remove(endpointID);
        InputStream inputStream = payload.asStream().asInputStream();

        try
        {
            DataConvertionUtil.inputStreamToEXCEL(inputStream, fileName);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
