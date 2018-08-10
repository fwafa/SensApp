package student.fh.sensorapplication.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import student.fh.sensorapplication.Activities.NearbyActivity;
import student.fh.sensorapplication.Model.ModelDevice;
import student.fh.sensorapplication.R;


public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder> {

    private List<ModelDevice> mValues;
    public static boolean icConnected = false;

    public NearbyActivity mActivity;
    public Context context;


    public RecyclerViewAdapter(List<ModelDevice> values, Context context) {
        mValues = values;
        this.context = context;
        mActivity = (NearbyActivity) context;
    }

    public RecyclerViewAdapter(List<ModelDevice> values) {
        mValues = values;
    }


    public class MyViewHolder extends RecyclerView.ViewHolder {

        public CardView cardView;
        public TextView tvCardView;
        public View layout;
        public NearbyActivity mActivity;

        private MyViewHolder(View itemView, NearbyActivity mActivity) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardViewLayout);
            tvCardView = itemView.findViewById(R.id.tvCardView);
            layout = itemView;
            this.mActivity = mActivity;
        }
    }


    @NonNull
    @Override
    public RecyclerViewAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.row_device, parent, false);
        return new MyViewHolder(view, mActivity);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapter.MyViewHolder holder, final int position) {

        ModelDevice model = mValues.get(position);
        String endpointName = model.getEndpointName();
        holder.tvCardView.setText(endpointName);

        if(model.isConnected())
        {
            holder.cardView.setCardBackgroundColor(Color.GREEN);
        }
        else
        {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#d1ccff"));
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }
}
