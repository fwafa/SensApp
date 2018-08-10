package student.fh.sensorapplication.Permissions;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build.VERSION;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import student.fh.sensorapplication.Activities.MainActivity;
import student.fh.sensorapplication.R;

public abstract class PermissionManager {
    private Activity activity;

    public PermissionManager(Activity activity)
    {
        this.activity = activity;
    }

    public boolean checkAndRequestPermissions(Activity activity) {
        this.activity = activity;
        if (VERSION.SDK_INT >= 23) {
            List<String> listPermissionsNeeded = this.setPermission();
            List<String> listPermissionsAssign = new ArrayList();
            Iterator var4 = listPermissionsNeeded.iterator();

            while(var4.hasNext()) {
                String per = (String)var4.next();
                if (ContextCompat.checkSelfPermission(activity.getApplicationContext(), per) != 0) {
                    listPermissionsAssign.add(per);
                }
            }

            if (!listPermissionsAssign.isEmpty()) {
                ActivityCompat.requestPermissions(activity, (String[])listPermissionsAssign.toArray(new String[listPermissionsAssign.size()]), 1212);
                return false;
            }
        }

        return true;
    }

    public ArrayList<student.fh.sensorapplication.Permissions.PermissionManager.statusArray> getStatus() {
        ArrayList<student.fh.sensorapplication.Permissions.PermissionManager.statusArray> statusPermission = new ArrayList();
        ArrayList<String> grant = new ArrayList();
        ArrayList<String> deny = new ArrayList();
        List<String> listPermissionsNeeded = this.setPermission();
        Iterator var5 = listPermissionsNeeded.iterator();

        while(var5.hasNext()) {
            String per = (String)var5.next();
            if (ContextCompat.checkSelfPermission(this.activity.getApplicationContext(), per) == 0) {
                grant.add(per);
            } else {
                deny.add(per);
            }
        }

        student.fh.sensorapplication.Permissions.PermissionManager.statusArray stat = new student.fh.sensorapplication.Permissions.PermissionManager.statusArray(grant, deny);
        statusPermission.add(stat);
        return statusPermission;
    }

    public List<String> setPermission() {
        ArrayList per = new ArrayList();

        try {
            PackageManager pm = this.activity.getApplicationContext().getPackageManager();
            PackageInfo pi = pm.getPackageInfo(this.activity.getApplicationContext().getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] permissionInfo = pi.requestedPermissions;
            String[] var5 = permissionInfo;
            int var6 = permissionInfo.length;

            for(int var7 = 0; var7 < var6; ++var7) {
                String p = var5[var7];
                per.add(p);
            }
        } catch (Exception var9) {
            ;
        }

        return per;
    }

    public void checkResult(int requestCode, String[] permissions, int[] grantResults) {
        switch(requestCode) {
            case 1212:
                List<String> listPermissionsNeeded = this.setPermission();
                Map<String, Integer> perms = new HashMap();
                Iterator var6 = listPermissionsNeeded.iterator();

                while(var6.hasNext()) {
                    String permission = (String)var6.next();
                    perms.put(permission, 0);
                }

                if (grantResults.length > 0) {
                    for(int i = 0; i < permissions.length; ++i) {
                        perms.put(permissions[i], grantResults[i]);
                    }

                    boolean isAllGranted = true;
                    Iterator var12 = listPermissionsNeeded.iterator();

                    while(var12.hasNext()) {
                        String permission = (String)var12.next();
                        if ((Integer)perms.get(permission) == -1) {
                            isAllGranted = false;
                            break;
                        }
                    }

                    if (!isAllGranted) {
                        boolean shouldRequest = false;
                        Iterator var14 = listPermissionsNeeded.iterator();

                        while(var14.hasNext()) {
                            String permission = (String)var14.next();
                            if (ActivityCompat.shouldShowRequestPermissionRationale(this.activity, permission)) {
                                shouldRequest = true;
                                break;
                            }
                        }

                        if (shouldRequest)
                        {
                            this.ifCancelledAndCanRequest(this.activity);
                        }
                        else
                        {
                            this.ifCancelledAndCannotRequest(this.activity);
                        }
                    }
                }
            default:
        }
    }

    public void ifCancelledAndCanRequest(final Activity activity)
    {
        this.showDialogOkCancel(activity, "Berechtigung erforderlich", "Einige Berechtigungen sind für dieses App erforderlich. Bitte erteilen Sie diese.", new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch(which) {
                    case -1:
                        student.fh.sensorapplication.Permissions.PermissionManager.this.checkAndRequestPermissions(activity);
                    case -2:
                    default:
                }
            }
        });
    }

    public void ifCancelledAndCannotRequest(final Activity activity)
    {
        this.showDialogOK(activity, "App wird beendet","Sie müssen die Berechtigungen erteilen, um fortzufahren! " +
                "Bitte diese in den Einstellungen vornehmen.", new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch(which) {
                    case -1:
                        activity.finishAffinity();
                    case -2:
                    default:
                }
            }
        });
    }

    private void showDialogOkCancel(Activity activity, String title, String message, OnClickListener okListener) {
        (new Builder(activity)).setTitle(title).setMessage(message).setPositiveButton("OK", okListener).setNegativeButton("Cancel", okListener).create().show();
    }

    private void showDialogOK(Activity activity, String title, String message, OnClickListener okListener) {
        (new Builder(activity)).setTitle(title).setMessage(message).setPositiveButton("OK", okListener).setCancelable(false).create().show();
    }

    public class statusArray {
        public ArrayList<String> granted;
        public ArrayList<String> denied;

        statusArray(ArrayList<String> this$0, ArrayList<String> granted) {
            this.denied = denied;
            this.granted = granted;
        }
    }
}

