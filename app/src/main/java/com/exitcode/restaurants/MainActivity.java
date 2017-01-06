package com.exitcode.restaurants;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;


public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, OnMapReadyCallback {

    private static final String DEBUG_TAG = "debug_tag-0";
    private static final String DOWNLOAD_URL = "http://www.mocky.io/v2/54ef80f5a11ac4d607752717";
    private static final String PREFS_NAME = "Settings";
    private static final String PREFS_KEY_DOWNLOAD_COMPLETE = "Download_Complete";
    private static final String INSTANCE_KEY_LAST_SELECTION = "Last_Selection";

    private static final String INSTANCE_KEY_MENU_ISSHOWING = "Menu_isShowing";

    private static final String INSTANCE_KEY_DIALOG_PICTURE_ISSHOWING = "DialogPicture_isShowing";

    private static final String INSTANCE_KEY_DIALOG_EDIT_ISSHOWING = "DialogEdit_isShowing";
    private static final String INSTANCE_KEY_DIALOG_EDIT_NAME = "DialogEdit_Name";
    private static final String INSTANCE_KEY_DIALOG_EDIT_ADDRESS = "DialogEdit_Address";

    private static final String INSTANCE_KEY_DIALOG_ADD_ISSHOWING = "DialogAdd_isShowing";
    private static final String INSTANCE_KEY_DIALOG_ADD_NAME = "DialogAdd_Name";
    private static final String INSTANCE_KEY_DIALOG_ADD_ADDRESS = "DialogAdd_Address";


    private static final int CAMERA_PIC_REQUEST_EXISTING = 1337;
    private static final int CAMERA_PIC_REQUEST_NEW = 7331;

    private static final String IMAGE_DIRECTORY_NAME = "Restaurants";
    private static final String IMAGE_TEMP_NAME = "tempPicture";
    private static final int BUFFER_SAMPLE_SIZE = 8;
    DatabaseAdapter databaseAdapter;
    GoogleMap googleMap;
    Dialog dialogPicture, dialogEdit, dialogAdd;
    GoogleApiClient googleApiClient;
    private String lastSelection = null;
    private Boolean menuIsShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!isCameraSupported()) {
            Toast.makeText(getApplicationContext(), "Device unsupported. Camera not found", Toast.LENGTH_SHORT).show();
            finish();
        }

        ((MapFragment) getFragmentManager().findFragmentById(R.id.google_map)).getMapAsync(this);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        databaseAdapter = new DatabaseAdapter(this);

        dialogPicture = new Dialog(this);
        dialogEdit = new Dialog(this);
        dialogAdd = new Dialog(this);


    }

    @Override
    protected void onResume() {
        super.onResume();
        googleApiClient.connect();


    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        Boolean dialogPictureIsShowing = savedInstanceState.getBoolean(INSTANCE_KEY_DIALOG_PICTURE_ISSHOWING, false);
        Boolean dialogEditIsShowing = savedInstanceState.getBoolean(INSTANCE_KEY_DIALOG_EDIT_ISSHOWING, false);
        Boolean dialogAddIsShowing = savedInstanceState.getBoolean(INSTANCE_KEY_DIALOG_ADD_ISSHOWING, false);

        lastSelection = savedInstanceState.getString(INSTANCE_KEY_LAST_SELECTION, null);
        menuIsShowing = savedInstanceState.getBoolean(INSTANCE_KEY_MENU_ISSHOWING, false);

        if (dialogPictureIsShowing) {
            showDialogPicture(lastSelection);
        }

        if (dialogEditIsShowing) {
            String name = savedInstanceState.getString(INSTANCE_KEY_DIALOG_EDIT_NAME, null);
            String address = savedInstanceState.getString(INSTANCE_KEY_DIALOG_EDIT_ADDRESS, null);
            showDialogEdit(lastSelection, name, address);
        }

        if (dialogAddIsShowing) {
            String name = savedInstanceState.getString(INSTANCE_KEY_DIALOG_ADD_NAME, null);
            String address = savedInstanceState.getString(INSTANCE_KEY_DIALOG_ADD_ADDRESS, null);

            showDialogAdd(name, address);
        }

        invalidateOptionsMenu();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Boolean dialogPictureIsShowing = dialogPicture.isShowing();
        Boolean dialogEditIsShowing = dialogEdit.isShowing();
        Boolean dialogAddIsShowing = dialogAdd.isShowing();

        outState.putBoolean(INSTANCE_KEY_DIALOG_PICTURE_ISSHOWING, dialogPictureIsShowing);
        outState.putBoolean(INSTANCE_KEY_DIALOG_EDIT_ISSHOWING, dialogEditIsShowing);
        outState.putBoolean(INSTANCE_KEY_DIALOG_ADD_ISSHOWING, dialogAddIsShowing);

        outState.putString(INSTANCE_KEY_LAST_SELECTION, lastSelection);
        outState.putBoolean(INSTANCE_KEY_MENU_ISSHOWING, menuIsShowing);

        if (dialogEditIsShowing) {
            EditText edtTxtName = (EditText) dialogEdit.getWindow().findViewById(R.id.edit_edtxt_name);
            EditText edtTxtAddress = (EditText) dialogEdit.getWindow().findViewById(R.id.edit_edtxt_address);

            String name = edtTxtName.getText().toString();
            String address = edtTxtAddress.getText().toString();

            outState.putString(INSTANCE_KEY_DIALOG_EDIT_NAME, name);
            outState.putString(INSTANCE_KEY_DIALOG_EDIT_ADDRESS, address);
        }

        if (dialogAddIsShowing) {
            EditText edtTxtName = (EditText) dialogAdd.getWindow().findViewById(R.id.add_edtxt_name);
            EditText edtTxtAddress = (EditText) dialogAdd.getWindow().findViewById(R.id.add_edtxt_address);

            String name = edtTxtName.getText().toString();
            String address = edtTxtAddress.getText().toString();

            outState.putString(INSTANCE_KEY_DIALOG_ADD_NAME, name);
            outState.putString(INSTANCE_KEY_DIALOG_ADD_ADDRESS, address);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (menuIsShowing) {
            menu.findItem(R.id.action_picture).setVisible(true);
            menu.findItem(R.id.action_edit).setVisible(true);
        } else {
            menu.findItem(R.id.action_picture).setVisible(false);
            menu.findItem(R.id.action_edit).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_picture:
                showDialogPicture(lastSelection);
                break;

            case R.id.action_edit:
                showDialogEdit(lastSelection, null, null);
                break;

            case R.id.action_add:
                showDialogAdd(null, null);
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onConnected(Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    public void createMarkers() {
        googleMap.clear();


        List<MarkerOptions> markerOptions = databaseAdapter.getMarkers();
        for (int i = 0; i < markerOptions.size(); i++) {
            googleMap.addMarker(markerOptions.get(i));
        }
    }

    public void showDialogPicture(final String name) {
        dialogPicture.setContentView(R.layout.dialog_picture);
        dialogPicture.setTitle("Picture: " + name);

        Button btnTakePicture = (Button) dialogPicture.findViewById(R.id.show_btn_takepic);
        Button btnCancel = (Button) dialogPicture.findViewById(R.id.show_btn_cancel);

        ImageView imgViewPicture = (ImageView) dialogPicture.findViewById(R.id.show_imgv_picture);

        File imageFile = getOutputMediaFile(name);
        if (imageFile.exists()) {

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = BUFFER_SAMPLE_SIZE;
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
            imgViewPicture.setImageBitmap(bitmap);
        }

        btnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture(name, true);
                dialogPicture.dismiss();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogPicture.cancel();
            }
        });

        dialogPicture.show();
    }


    public void showDialogEdit(final String name, String unfinishedName, String unfinishedAddress) {
        dialogEdit.setContentView(R.layout.dialog_edit);
        dialogEdit.setTitle("Editing: " + name);

        final EditText editTextName = (EditText) dialogEdit.findViewById(R.id.edit_edtxt_name);
        final EditText editTextAddress = (EditText) dialogEdit.findViewById(R.id.edit_edtxt_address);
        Button btnSubmit = (Button) dialogEdit.findViewById(R.id.edit_btn_submit);
        Button btnCancel = (Button) dialogEdit.findViewById(R.id.edit_btn_cancel);

        if (unfinishedName != null && unfinishedAddress != null) {
            editTextName.setText(unfinishedName);
            editTextAddress.setText(unfinishedAddress);
        } else {
            editTextName.setText(name);
            editTextAddress.setText(databaseAdapter.getAddress(name));
        }

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String edtTextName = editTextName.getText().toString().trim();
                String edtTextAddress = editTextAddress.getText().toString().trim();

                if (edtTextName.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                } else if (edtTextAddress.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Address cannot be empty", Toast.LENGTH_SHORT).show();
                } else {
                    if (name != null) {
                        databaseAdapter.updateNameAddress(name, edtTextName, edtTextAddress);

                        renameFile(name, edtTextName);
                        createMarkers();

                        Toast.makeText(MainActivity.this, "Data successfully changed", Toast.LENGTH_SHORT).show();
                    }

                    dialogEdit.dismiss();
                }
            }
        });


        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogEdit.cancel();
            }
        });

        dialogEdit.show();
    }

    public void showDialogAdd(String unfinishedName, String unfinishedAddress) {
        dialogAdd.setContentView(R.layout.dialog_add);
        dialogAdd.setTitle("Add new marker");

        final EditText editTextName = (EditText) dialogAdd.findViewById(R.id.add_edtxt_name);
        final EditText editTextAddress = (EditText) dialogAdd.findViewById(R.id.add_edtxt_address);
        Button btnSubmit = (Button) dialogAdd.findViewById(R.id.add_btn_submit);
        Button btnTakePic = (Button) dialogAdd.findViewById(R.id.add_btn_takepic);
        Button btnCancel = (Button) dialogAdd.findViewById(R.id.add_btn_cancel);
        ImageView imageView = (ImageView) dialogAdd.findViewById(R.id.add_imgv_picture);

        File imageFile = getOutputMediaFile(IMAGE_TEMP_NAME);

        if (imageFile.exists()) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = BUFFER_SAMPLE_SIZE;
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
            imageView.setImageBitmap(bitmap);
        }

        if (unfinishedName != null && unfinishedAddress != null) {
            editTextName.setText(unfinishedName);
            editTextAddress.setText(unfinishedAddress);
        }

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!isLocationServicesEnabled()) {
                    Toast.makeText(MainActivity.this, "Location not available. Check settings", Toast.LENGTH_SHORT).show();
                } else {

                    Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                    String edtTextName = editTextName.getText().toString();
                    String edtTextAddress = editTextAddress.getText().toString();

                    if (edtTextName.isEmpty()) {
                        Toast.makeText(MainActivity.this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                    } else if (edtTextAddress.isEmpty()) {

                        Toast.makeText(MainActivity.this, "Address cannot be empty", Toast.LENGTH_SHORT).show();
                    } else {
                        databaseAdapter.insertRow(edtTextName, edtTextAddress, location.getLatitude(), location.getLongitude());

                        googleMap.addMarker(new MarkerOptions()
                                .title(edtTextName)
                                .snippet(edtTextAddress)
                                .position(new LatLng(location.getLatitude(), location.getLongitude())));

                        Toast.makeText(MainActivity.this, "Marker successfully created", Toast.LENGTH_SHORT).show();
                        renameFile(IMAGE_TEMP_NAME, edtTextName);

                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latLng, 11);
                        googleMap.animateCamera(update);

                        dialogAdd.dismiss();
                    }


                }
            }
        });

        btnTakePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture(IMAGE_TEMP_NAME, false);
                dialogAdd.dismiss();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogAdd.cancel();
            }
        });

        dialogAdd.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {

                deleteTempFile();
            }
        });

        dialogAdd.show();
    }


    public void renameFile(String oldName, String newName) {
        File oldFile = getOutputMediaFile(oldName);

        if (oldFile.exists()) {
            File newFile = getOutputMediaFile(newName);

            Boolean renameResult = oldFile.renameTo(newFile);

            if (renameResult) {
                Log.d(DEBUG_TAG, "File successfully renamed");
            } else {
                Log.d(DEBUG_TAG, "Error occured while renaming file");
            }
        }
    }

    public void deleteTempFile() {
        File file = getOutputMediaFile(IMAGE_TEMP_NAME);

        if (file.exists()) {
            Boolean deleteResult = file.delete();

            if (deleteResult) {
                Log.d(DEBUG_TAG, "Temp file successfully deleted");
            } else {
                Log.d(DEBUG_TAG, "Error occured while deleting temp file");
            }
        }
    }

    private boolean isCameraSupported() {
        return getApplicationContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA);
    }

    private Boolean isLocationServicesEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public Boolean isInternetAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void takePicture(String name, Boolean existing) {
        Toast.makeText(MainActivity.this, "Take picture horizontally!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri fileUri = getOutputMediaFileUri(name);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

        if (intent.resolveActivity(getPackageManager()) != null) {
            if (existing) {
                startActivityForResult(intent, CAMERA_PIC_REQUEST_EXISTING);
            } else {
                startActivityForResult(intent, CAMERA_PIC_REQUEST_NEW);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Boolean resultOK = resultCode == RESULT_OK;
        switch (requestCode) {
            case CAMERA_PIC_REQUEST_EXISTING:
                if (resultOK) {
                    Toast.makeText(MainActivity.this, "Picture successfully saved", Toast.LENGTH_SHORT).show();
                    showDialogPicture(lastSelection);

                    break;
                }
            case CAMERA_PIC_REQUEST_NEW:
                if (resultOK) {
                    Toast.makeText(MainActivity.this, "Temporary picture successfully saved", Toast.LENGTH_SHORT).show();
                    showDialogAdd(null, null);

                    break;
                }


        }
    }

    public Uri getOutputMediaFileUri(String name) {
        return Uri.fromFile(getOutputMediaFile(name));
    }

    public File getOutputMediaFile(String name) {

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), IMAGE_DIRECTORY_NAME);

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }

        return new File(mediaStorageDir.getPath() + File.separator + name + ".jpg");
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(new LatLng(45.815011, 15.981919), 12);
        googleMap.animateCamera(update);

        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(final Marker marker) {
                lastSelection = marker.getTitle();

                if (!menuIsShowing) {
                    menuIsShowing = true;
                    invalidateOptionsMenu();
                }
                return false;
            }
        });

        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                lastSelection = null;

                if (menuIsShowing) {
                    menuIsShowing = false;
                    invalidateOptionsMenu();
                }
            }
        });

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        if (settings.getBoolean(PREFS_KEY_DOWNLOAD_COMPLETE, false)) {
            createMarkers();
        } else {
            new fetchParseJSON().execute();
        }
    }

    public class fetchParseJSON extends AsyncTask<Void, Void, Boolean> {

        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (!isInternetAvailable()) {
                Toast.makeText(MainActivity.this, "Internet not available. Check settings and restart application", Toast.LENGTH_LONG).show();
                cancel(true);
                finish();
            }

            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setTitle("Please wait...");
            progressDialog.setMessage("Downloading and parsing data...");
            progressDialog.setCancelable(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            InputStream in;
            BufferedReader bufferedReader;
            HttpURLConnection urlConnection = null;
            StringBuilder result = new StringBuilder();

            try {
                URL url = new URL(DOWNLOAD_URL);
                urlConnection = (HttpURLConnection) url.openConnection();

                in = new BufferedInputStream(urlConnection.getInputStream());
                bufferedReader = new BufferedReader(new InputStreamReader(in));

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    result.append(line).append('\n');
                }


            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            try {
                JSONArray resultArray = new JSONArray(result.toString());
                for (int i = 0; i < resultArray.length(); i++) {
                    JSONObject jsonObject = resultArray.getJSONObject(i);
                    String name = jsonObject.getString("Name");
                    String address = jsonObject.getString("Address");
                    Double latitude = jsonObject.getDouble("Latitude");
                    Double longitude = jsonObject.getDouble("Longitude");

                    databaseAdapter.insertRow(name, address, latitude, longitude);
                }


            } catch (JSONException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            if (result) {
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean(PREFS_KEY_DOWNLOAD_COMPLETE, true);
                editor.apply();

                createMarkers();
                Toast.makeText(MainActivity.this, "Data successfully downloaded and parsed", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Error downloading or parsing data", Toast.LENGTH_SHORT).show();
            }

            progressDialog.dismiss();
        }
    }
}