package student.fh.sensorapplication.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import student.fh.sensorapplication.MPAndroidChart.SensorGraphActivity;
import student.fh.sensorapplication.R;

public class MainActivity extends AppCompatActivity {

    private final Integer REQUEST_PERMISSION = 1;

    private GestureDetectorCompat gestureObject;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        final Animation from_up_to_down = AnimationUtils.loadAnimation(this, R.anim.move_from_right);
        final ImageView imageViewAcc = findViewById(R.id.imageViewAccelerometer);
        imageViewAcc.setVisibility(View.INVISIBLE);


        final Animation from_right_to_left = AnimationUtils.loadAnimation(this, R.anim.move_from_left);
        final ImageView imageViewGyro = findViewById(R.id.imageViewGyroscope);
        imageViewGyro.setVisibility(View.INVISIBLE);


        Animation zoom_in = AnimationUtils.loadAnimation(this, R.anim.zoom_in);
        TextView textView = findViewById(R.id.textViewAppName);
        textView.setAnimation(zoom_in);

        zoom_in.setAnimationListener(new Animation.AnimationListener()
        {
            @Override
            public void onAnimationStart(Animation animation)
            {

            }

            @Override
            public void onAnimationEnd(Animation animation)
            {
                imageViewAcc.setVisibility(View.VISIBLE);
                imageViewGyro.setVisibility(View.VISIBLE);

                imageViewAcc.setAnimation(from_up_to_down);
                imageViewGyro.setAnimation(from_right_to_left);
            }

            @Override
            public void onAnimationRepeat(Animation animation)
            {

            }
        });


        askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_PERMISSION);

        Button startButton = findViewById(R.id.start_btn);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(MainActivity.this, SensorGraphActivity.class);
                startActivity(intent);
            }
        });


        ImageButton info = findViewById(R.id.button_info);
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(MainActivity.this, InfoActivity.class);
                startActivity(intent);
            }
        });


        gestureObject = new GestureDetectorCompat(this, new MyGestureClass());

    }

    private void askForPermission(String permission, Integer requestCode) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permission)) {

                //This is called if user has denied the permission before
                //In this case I am just asking the permission again
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);

            } else {

                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);
            }
        }
        /*else
        {
            Toast.makeText(this, "" + permission + " is already granted.", Toast.LENGTH_LONG).show();
        }*/
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if(requestCode == REQUEST_PERMISSION)
        {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {

            }
            else
            {
                Toast.makeText(this, "Permission was not granted", Toast.LENGTH_LONG).show();
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
                Intent intent = new Intent(MainActivity.this, FireBaseActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);

            }
            else if(e2.getX() < e1.getX())
            {
                // swipe right to left
            }

            return true;
        }
    }
}
