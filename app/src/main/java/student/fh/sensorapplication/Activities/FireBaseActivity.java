package student.fh.sensorapplication.Activities;

import android.os.Build;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import student.fh.sensorapplication.R;

public class FireBaseActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private GestureDetectorCompat gestureObject;
    private Button button;
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

        button = findViewById(R.id.start_stop_btn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (status == 0)
                {
                    button.setText("STOP");
                    button.setBackgroundResource(R.drawable.button_shape_red);
                    status=1 ;

                    mDatabase.setValue("start");
                }

                else {
                    button.setText("START");
                    button.setBackgroundResource(R.drawable.button_shape_green);
                    status =0;//change the status to 0 so the at the second clic , the if will be executed

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
