package student.fh.sensorapplication.fragment;


import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import student.fh.sensorapplication.Adapter.RecyclerItemClickListener;
import student.fh.sensorapplication.Adapter.RecyclerViewAdapter;
import student.fh.sensorapplication.Nearby.NearbyConnection;
import student.fh.sensorapplication.R;

public class DialogFragmentClient extends DialogFragment {

    public RecyclerView recyclerView;
    public static RecyclerView.Adapter adapter;

    private NearbyConnection nearbyConnection;

    private static DialogFragmentClient frag;


    public static DialogFragmentClient newInstance(String title, RecyclerView.Adapter mAdapter) {

        frag = new DialogFragmentClient();
        Bundle args = new Bundle();
        args.putString("title", title);
        frag.setArguments(args);

        adapter = mAdapter;

        return frag;
    }


    public static DialogFragmentClient dismissDialog() {

        frag.dismiss();
        return frag;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        nearbyConnection = new NearbyConnection(this, getActivity().getApplicationContext());

        View v = inflater.inflate(R.layout.custom_dialog_layout, container, false);

        recyclerView = v.findViewById(R.id.recyclerViewDialog);
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getActivity()));

        adapter = new RecyclerViewAdapter(NearbyConnection.endPoints);
        recyclerView.setAdapter(adapter);

        return v;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String title = getArguments().getString("title", "Hosts");
        getDialog().setTitle(title);

        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(
                getActivity().getApplicationContext(), recyclerView, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {

                Nearby.getConnectionsClient(getActivity()).requestConnection(
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

            }
        }));
    }

    public static RecyclerView.Adapter getAdapter() {
        return adapter;
    }
}
