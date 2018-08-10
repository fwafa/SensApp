package student.fh.sensorapplication.Adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import student.fh.sensorapplication.Activities.FileListActivity;
import student.fh.sensorapplication.BuildConfig;
import student.fh.sensorapplication.Model.ModelFile;
import student.fh.sensorapplication.R;

public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.MyViewHolder> {

    private static final String SHARED_PROVIDER_AUTHORITY = BuildConfig.APPLICATION_ID;
    private static final String SHARED_FOLDER = "Sensordaten";

    ArrayList<ModelFile> fileArrayList;
    FileListActivity mActivity;
    Context context;


    public FilesAdapter(ArrayList<ModelFile> fileArrayList, Context context)
    {
        this.fileArrayList = fileArrayList;
        this.context = context;
        mActivity = (FileListActivity) context;
    }


    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        public CardView cardViewFile;
        public CheckBox checkBoxFile;
        public TextView tvFile;
        public ImageView imageView;
        public FileListActivity mActivity;

        private MyViewHolder(View itemView, FileListActivity mActivity) {
            super(itemView);
            cardViewFile = itemView.findViewById(R.id.cardViewFile);
            checkBoxFile = itemView.findViewById(R.id.checkboxFile);
            imageView    = itemView.findViewById(R.id.imageView);
            tvFile = itemView.findViewById(R.id.tvFile);
            this.mActivity = mActivity;

            cardViewFile.setOnLongClickListener(mActivity);
            checkBoxFile.setOnClickListener(this);
        }

        @Override
        public void onClick(View v)
        {
            mActivity.prepareSelection(v, getAdapterPosition());
        }
    }


    public void uploadFileToFirebaseStorage(ArrayList<ModelFile> list)
    {
        if(mActivity.isNetworkAvailable())
        {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();

            for(ModelFile modelFile : list)
            {
                Uri file = Uri.fromFile(modelFile.getFile());
                StorageReference riversRef = storageRef.child("Sensordaten/" + file.getLastPathSegment());
                UploadTask uploadTask = riversRef.putFile(file);

                // Register observers to listen for when the download is done or if it fails
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {

                        Toast.makeText(context, "Datei konnte nicht hochgeladen werden!", Toast.LENGTH_SHORT).show();
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        Toast.makeText(context, "Datei erfolgreich hochgeladen!", Toast.LENGTH_SHORT).show();
                    }
                });

                //fileArrayList.remove(modelFile);
            }
            //notifyDataSetChanged();
        }
        else
        {
            Toast.makeText(context, "Internetverbindung nicht verfügbar!", Toast.LENGTH_SHORT).show();
        }

    }

    public void deleteFiles(ArrayList<ModelFile> list)
    {
        for(ModelFile modelFile : list)
        {
            fileArrayList.remove(modelFile);
            modelFile.getFile().delete();
        }

        notifyDataSetChanged();
    }

    public void share(File file) throws IOException {

        if(file != null)
        {
            final Uri uri = FileProvider.getUriForFile(context, SHARED_PROVIDER_AUTHORITY, file);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(uri, "application/vnd.ms-excel");
            context.startActivity(intent);
        }
        else
        {
            Toast.makeText(context, "Datei nicht vorhanden!", Toast.LENGTH_SHORT).show();
        }
    }


    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.row_file, parent, false);
        return new FilesAdapter.MyViewHolder(view, mActivity);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, final int position) {

        //holder.tvFile.setText(fileArrayList.get(position).getFileName());

        String fileName = FilenameUtils.removeExtension(fileArrayList.get(position).getFileName());
        holder.tvFile.setText(fileName);
        holder.checkBoxFile.setChecked(fileArrayList.get(position).isSelected());

        if(!mActivity.isInActionMode)
        {
            holder.checkBoxFile.setVisibility(View.GONE);
        }
        else
        {
            holder.checkBoxFile.setVisibility(View.VISIBLE);
            holder.checkBoxFile.setChecked(false);
        }

        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try
                {
                    share(fileArrayList.get(position).getFile());
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return fileArrayList.size();
    }
}
