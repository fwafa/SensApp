package student.fh.sensorapplication.Activities;

import android.hardware.SensorManager;
import android.os.Build;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import student.fh.sensorapplication.MPAndroidChart.SensorGraphActivity;
import student.fh.sensorapplication.R;

public class FireBaseActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private GestureDetectorCompat gestureObject;
    private Button button;
    private String databaseValue;
    private int status = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        setContentView(R.layout.activity_fire_base);

        gestureObject = new GestureDetectorCompat(this, new FireBaseActivity.MyGestureClass());

        mDatabase = FirebaseDatabase.getInstance().getReference().child("sensor");


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
                        if (databaseValue.equalsIgnoreCase("start"))
                        {
                            button.setText("STOP");
                            button.setBackgroundResource(R.drawable.button_shape_red);
                        }
                        else if(databaseValue.equalsIgnoreCase("stop"))
                        {
                            button.setText("SAVE");
                            button.setBackgroundResource(R.drawable.button_shape_yellow);
                        }
                        else
                        {
                            button.setText("START");
                            button.setBackgroundResource(R.drawable.button_shape_green);
                        }
                    }

                }
                catch (NullPointerException e)
                {

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {
                Log.w("loadPost:onCancelled", databaseError.toException());
            }
        });


        button = findViewById(R.id.start_stop_btn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (status == 0)
                {
                    button.setText("STOP");
                    button.setBackgroundResource(R.drawable.button_shape_red);
                    status = 2 ;

                    mDatabase.setValue("start");
                }
                else if(status == 1)
                {
                    button.setText("START");
                    button.setBackgroundResource(R.drawable.button_shape_green);
                    status = 0;

                    mDatabase.setValue("save");
                }
                else
                {
                    button.setText("SAVE");
                    button.setBackgroundResource(R.drawable.button_shape_yellow);
                    status = 1;

                    mDatabase.setValue("stop");
                }
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        this.gestureObject.onTouchEvent(event);
        return super.onTouchEvent(event);
    }


    class MyGestureClass extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
        {
            if(e2.getX() > e1.getX())
            {
                // swipe left to right
            }
            else if(e2.getX() < e1.getX())
            {
                // swipe right to left
                onBackPressed();
                overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left);
            }

            return true;
        }
    }
}
