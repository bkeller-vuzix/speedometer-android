package net.mypapit.mobile.speedmeter;

/***************************************************************************************
 Copyright (c) 2018, Vuzix Corporation
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 *  Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.

 *  Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.

 *  Neither the name of Vuzix Corporation nor the names of
 its contributors may be used to endorse or promote products derived
 from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 **************************************************************************************/
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import java.util.ArrayList;


/**
 * A fragment to encapsulate the run-time permissions
 */
@TargetApi(23)
public class PermissionsFragment extends Fragment {
    private final static String TAG = MainActivity.TAG + "Permission";
    public static final String PERMISSIONS_BUNDLE_ARG_KEY = "permissionsArg";
    private static final String TAG_PERMISSIONS_FRAGMENT = "permissionsTag";

    private int requestCodePermissions = 0;
    private Listener listener;
    private ArrayList<String> mPermissions;

    static public PermissionsFragment init(Activity mainActivity, Listener callbackListener, ArrayList<String> permissions){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d(TAG, "init()");
            PermissionsFragment permissionsFragment = (PermissionsFragment) mainActivity.getFragmentManager().findFragmentByTag(TAG_PERMISSIONS_FRAGMENT);
            if (permissionsFragment == null) {
                permissionsFragment = new PermissionsFragment();
                // Register as a PermissionsFragment.Listener so our permissionsGranted() is called
                Bundle argsBundle = new Bundle();
                argsBundle.putStringArrayList(PermissionsFragment.PERMISSIONS_BUNDLE_ARG_KEY, permissions);
                permissionsFragment.setArguments(argsBundle);
                mainActivity.getFragmentManager().beginTransaction().add(permissionsFragment, TAG_PERMISSIONS_FRAGMENT).commit();
                permissionsFragment.setListener(callbackListener);
                Log.d(TAG, "init() done");
            } else {
                permissionsFragment.requestMissingPermissions();
            }
            return permissionsFragment;
        } else {
            // No dynamic runtime permissions. Just return granted.
            callbackListener.permissionsGranted();
            return null;
        }
    }

    /**
     * One-time initialization. Sets up the view
     * @param savedInstanceState - we have no saved state. Just pass through to superclass
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d(TAG, "Permissions fragment. Using dynamic permissions");
            Bundle bundle = getArguments();
            mPermissions = bundle.getStringArrayList(PERMISSIONS_BUNDLE_ARG_KEY);
            requestMissingPermissions();
        } else {
            //Log.d(TAG, "Permissiosn fragment. Pre-M");
            permissionsGranted();
        }
    }

    /**
     * Make the permissions request to the system
     */
    private void requestMissingPermissions() {
        Log.d(TAG, "requestMissingPermissions");
        ArrayList missingPermissions = new ArrayList();
        for (String eachString: mPermissions) {
            if (getContext().checkSelfPermission(eachString) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "requestMissingPermissions " + eachString);
                missingPermissions.add(eachString);
            }
        }
        if(missingPermissions.size() > 0 ) {
            String[] simpleArray = new String [missingPermissions.size()];
            missingPermissions.toArray(simpleArray);
            requestCodePermissions++;
            requestPermissions(simpleArray, requestCodePermissions);
        } else {
            permissionsGranted();
        }
    }

    /**
     * Called upon the permissions being granted. Notifies the permission listener.
     */
    private synchronized void permissionsGranted() {
        Log.d(TAG, "permissionsGranted");
        if (listener != null) {
            listener.permissionsGranted();
        }
    }


    /**
     * Sets the listener on which we will call permissionsGranted()
     * @param listener pointer to the class implementing the PermissionsFragment.Listener
     */
    public synchronized void setListener(Listener listener) {
        this.listener = listener;
    }

    /**
     * Required interface for any activity that requests a run-time permission
     *
     * @see <a href="https://developer.android.com/training/permissions/requesting.html">https://developer.android.com/training/permissions/requesting.html</a>
     * @param requestCode int: The request code passed in requestPermissions(android.app.Activity, String[], int)
     * @param permissions String: The requested permissions. Never null.
     * @param grantResults int: The grant results for the corresponding permissions which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == requestCodePermissions) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    continue;
                } else if (shouldShowRequestPermissionRationale(permissions[i])) {
                    Log.d(TAG, "Permission denied - show rationale");
                    break; // from the nested for loop
                } else {
                    Log.d(TAG, "Permission denied. Finish.");
                    // Permission was denied. Give the user a hint, and exit
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", getContext().getPackageName(), null));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    getActivity().finish();
                }
            }
            requestMissingPermissions();  // Ask for permission again. Passes if we have all.
            return;
        }
        Log.e(TAG, "Unrecognized request code");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * Define the interface of a permission fragment listener
     */
    interface Listener {
        void permissionsGranted();
    }
}
