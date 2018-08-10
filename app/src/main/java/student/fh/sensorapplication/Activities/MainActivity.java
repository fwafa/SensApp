package student.fh.sensorapplication.Activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AlertDialog;
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

import student.fh.sensorapplication.MPAndroidChart.SensorGraphActivity;
import student.fh.sensorapplication.Permissions.PermissionManager;
import student.fh.sensorapplication.R;

public class MainActivity extends AppCompatActivity {

    private PermissionManager permissionManager;
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


        Button clientButton = findViewById(R.id.client_btn);
        clientButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(MainActivity.this, SensorGraphActivity.class);
                startActivity(intent);
            }
        });

        Button hostButton = findViewById(R.id.host_btn);
        hostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this, NearbyActivity.class);
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


    @Override
    protected void onStart() {
        super.onStart();
        statusCheck();
    }


    public void statusCheck()
    {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled;

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
            isGpsEnabled = false;
        }
        else
        {
            isGpsEnabled = true;
        }

        if (isGpsEnabled)
        {
            permissionManager = new PermissionManager(this) {
                @Override
                public void ifCancelledAndCanRequest(Activity activity) {
                    super.ifCancelledAndCanRequest(MainActivity.this);
                }

                @Override
                public void ifCancelledAndCannotRequest(Activity activity) {
                    super.ifCancelledAndCannotRequest(MainActivity.this);
                }
            };

            permissionManager.checkAndRequestPermissions(this);
        }

    }


    private void buildAlertMessageNoGps()
    {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("GPS ist deaktiviert");
        builder.setMessage("GPS ist ausgeschaltet. Bevor Sie fortfahren, muss es aktiviert werden." +
                " Möchten Sie es aktivieren? Falls Sie Nein wählen, wird die App beendet!")
                .setCancelable(false)
                .setPositiveButton("Ja", new DialogInterface.OnClickListener()
                {
                    public void onClick(final DialogInterface dialog, final int id)
                    {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("Nein", new DialogInterface.OnClickListener()
                {
                    public void onClick(final DialogInterface dialog, final int id)
                    {
                        dialog.cancel();
                        finishAffinity();
                    }
                });
        final AlertDialog alert = builder.create();

        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {

                alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.alertButtonColor));
                alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.alertButtonColor));
            }
        });

        alert.show();
    }



    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        permissionManager.checkResult(requestCode,permissions, grantResults);
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
                Intent intent = new Intent(MainActivity.this, NearbyActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);

            }
            else if(e2.getX() < e1.getX())
            {
                // swipe right to left
                Intent intent = new Intent(MainActivity.this, SensorGraphActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left);
            }

            return true;
        }
    }
}
