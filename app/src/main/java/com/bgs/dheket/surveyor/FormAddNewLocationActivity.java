package com.bgs.dheket.surveyor;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bgs.dheket.common.Utility;
import com.bgs.dheket.networkAndSensor.Compass;
import com.bgs.dheket.networkAndSensor.HttpGetOrPost;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapView;
import com.esri.android.map.event.OnLongPressListener;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.LinearUnit;
import com.esri.core.geometry.MultiPoint;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.Unit;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.Symbol;
import com.esri.core.tasks.geocode.Locator;
import com.facebook.login.LoginManager;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by SND on 12/02/2016.
 */
public class FormAddNewLocationActivity extends AppCompatActivity {

    android.support.v7.app.ActionBar actionBar;
    EditText editText_loc_name, editText_loc_address, editText_loc_phone, editText_loc_lat,
                editText_loc_lng, editText_loc_subCat, editText_loc_photoUrl, editText_loc_description;
    TextView textView_mapLatLng;
    MapView mMapView;
    Button button_centerLoc;
    ImageButton imageButton_getCurrentLoc,imageButton_save;
    ImageView imageView_previewPhoto;
    Spinner spinner_category;

    final static double ZOOM_BY = 10;

    SpatialReference mMapSr = null;
    GraphicsLayer mResultsLayer = null;
    PictureMarkerSymbol mAdd;

    Locator mLocator;
    Location locationTouch;
    Location location;
    ArrayList<String> mFindOutFields = new ArrayList<>();

    LocationDisplayManager mLDM;
    Compass mCompass;

    String pathFile;
    Uri _uri;
    //CallWebPageTask task;

    String url = "http://dheket.esy.es/getAllCategory.php";
    private JSONObject jObject;
    String []nama_katagori;
    int []id_kategori;
    int []id_subCat;
    int []temp_id_cat = new int[1];

    // Response
    String responseServer="";
    String loc_name,loc_address,loc_phone,loc_tag,loc_photo,loc_desc;
    double loc_lat,loc_lng;
    int loc_cat;
    String loc_userCreatedId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_add_new_location);

        actionBar = getSupportActionBar();

        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        //actionBar.setHomeAsUpIndicator(R.drawable.logo);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle("Dheket for Surveyor");
        actionBar.setSubtitle("New Location");
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#5991ff")));

        editText_loc_name = (EditText)findViewById(R.id.editText_anl_loc_name);
        editText_loc_address = (EditText)findViewById(R.id.editText_anl_loc_address);
        editText_loc_phone = (EditText)findViewById(R.id.editText_anl_loc_phone);
        editText_loc_lat = (EditText)findViewById(R.id.editText_anl_loc_lat);
        editText_loc_lng = (EditText)findViewById(R.id.editText_anl_loc_lng);
        editText_loc_subCat = (EditText)findViewById(R.id.editText_anl_subCat);
        editText_loc_subCat.setOnClickListener(tagsChoice);
        editText_loc_photoUrl = (EditText)findViewById(R.id.editText_anl_photo_url);
        editText_loc_photoUrl.setOnClickListener(photoUrl);
        editText_loc_description = (EditText)findViewById(R.id.editText_anl_loc_description);
        textView_mapLatLng = (TextView)findViewById(R.id.textView_anl_map_latlong);
        mMapView = (MapView)findViewById(R.id.map_selection);
        button_centerLoc = (Button)findViewById(R.id.button_centerLoc);
        button_centerLoc.setOnClickListener(buttonCenter);
        imageButton_getCurrentLoc = (ImageButton)findViewById(R.id.imageButton_anl_getCurrentLoc);
        imageButton_getCurrentLoc.setOnClickListener(buttonCurrentClik);
        imageButton_save = (ImageButton)findViewById(R.id.imageButton_anl_save);
        imageButton_save.setOnClickListener(buttonSave);
        imageView_previewPhoto = (ImageView)findViewById(R.id.imageView_anl_photo_prev);
        spinner_category = (Spinner)findViewById(R.id.spinner_anl_category);

        mMapView.setOnStatusChangedListener(statusChangedListener);
        mMapView.setOnSingleTapListener(mapTapCallback);
        mMapView.setOnLongPressListener(mapLongPress);
        mResultsLayer = new GraphicsLayer();
        mResultsLayer.setSelectionColorWidth(6);
        mMapView.addLayer(mResultsLayer);
        mMapView.setAllowRotationByPinch(true);

        // Enabled wrap around map.
        mMapView.enableWrapAround(true);

        // Create the Compass custom view, and add it onto the MapView.
        mCompass = new Compass(FormAddNewLocationActivity.this, null, mMapView);
        mMapView.addView(mCompass);
        mAdd = new PictureMarkerSymbol(getApplicationContext(), ContextCompat.getDrawable(getApplicationContext(), R.drawable.pin_add));

        setupLocator();
        setupLocationListener();
        getDataSpinner();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                back_to_previous_screen();
                return super.onOptionsItemSelected(item);

            case R.id.action_logout:
                logout_user();
                return super.onOptionsItemSelected(item);

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    public void logout_user(){
        String logout = getResources().getString(com.facebook.R.string.com_facebook_loginview_log_out_action);
        String cancel = getResources().getString(com.facebook.R.string.com_facebook_loginview_cancel_action);
        String message= "Are you sure?";
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setCancelable(true)
                .setPositiveButton(logout, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        LoginManager.getInstance().logOut();
                        Intent logout_user_fb = new Intent(FormAddNewLocationActivity.this, FormLoginActivity.class);
                        startActivity(logout_user_fb);
                        finish();
                    }
                })
                .setNegativeButton(cancel, null);
        builder.create().show();
    }

    @Override
    public void onBackPressed() {
        back_to_previous_screen();
    }

    public void back_to_previous_screen(){
        Intent intent = new Intent(FormAddNewLocationActivity.this,MainActivity.class);
        startActivity(intent);
        finish();
    }

    final View.OnClickListener buttonCurrentClik = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            mMapView.setRotationAngle(0);
            // Also reset the compass angle.
            mCompass.setRotationAngle(0);
            //Point point = getAsPoint(location);
            mResultsLayer.removeAll();
            if ((mLDM != null) && (mLDM.getLocation() != null)) {
                // Keep current scale and go to current location, if there is one.
                Point point = new Point();
                point.setXY(mLDM.getLocation().getLongitude(), mLDM.getLocation().getLatitude());
                locationTouch.setLatitude(point.getY());
                locationTouch.setLongitude(point.getX());

                point = getAsPoint(locationTouch);
                Symbol symbol = mAdd;

                mResultsLayer.addGraphic(new Graphic(point, symbol));
                mMapView.setExtent(point, 100);


                editText_loc_lat.setText("" + locationTouch.getLatitude());
                editText_loc_lng.setText("" + locationTouch.getLongitude());
            }

            textView_mapLatLng.setText("lat:"+locationTouch.getLatitude()+"\nlng:"+locationTouch.getLongitude());
            centerViewMap();
        }
    };

    final View.OnClickListener buttonCenter = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            centerViewMap();
        }
    };

    final View.OnClickListener photoUrl = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            //Toast.makeText(getApplicationContext(),"url pic",Toast.LENGTH_SHORT).show();
            pathFile="";
            _uri=null;
            Intent intentPhone = new Intent(Intent.ACTION_GET_CONTENT);
            intentPhone.setType("image/jpeg");
            startActivityForResult(intentPhone, 0);
        }
    };

    final View.OnClickListener tagsChoice = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showDialogSelectTag();
        }
    };

    public void showPreviewPhoto(){
        if (pathFile != null) {
            editText_loc_photoUrl.setText(pathFile);
        }
        if (_uri != null) {
            imageView_previewPhoto.setVisibility(View.VISIBLE);
            imageView_previewPhoto.setImageURI(_uri);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);

        // -------------------------------GET URI FOR
        // IMAGES----------------------------------------

        // check supaya ketika di pencet tombol back tidak error (mengantisipasi
        // nilai null pada data)
        if (data != null) {
            _uri = data.getData();
            Log.d("", "URI = " + _uri);
            if (_uri != null && "content".equals(_uri.getScheme())) {
                Cursor cursor = this
                        .getContentResolver()
                        .query(_uri,
                                new String[] { android.provider.MediaStore.Images.ImageColumns.DATA },
                                null, null, null);
                cursor.moveToFirst();
                pathFile = cursor.getString(0);

                cursor.close();
            } else {
                pathFile = _uri.getPath();
            }
            Log.d("", "Chosen path = " + pathFile);
            // --------------------------------GET URI FOR
            // IMAGES-----------------------------------------
        } else {
            pathFile = null;
            _uri = null;
        }
        showPreviewPhoto();

    }

    final View.OnClickListener buttonSave = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            loc_name = editText_loc_name.getText().toString();
            loc_address = editText_loc_address.getText().toString();
            loc_phone = editText_loc_phone.getText().toString();
            loc_tag = editText_loc_subCat.getText().toString();
            loc_photo = editText_loc_photoUrl.getText().toString();
            loc_desc = editText_loc_description.getText().toString();
            if (editText_loc_lat.getText().toString().isEmpty())
                loc_lat = 0.0;
            else
                loc_lat = Double.parseDouble(editText_loc_lat.getText().toString());
            if (editText_loc_lng.getText().toString().isEmpty())
                loc_lng = 0.0;
            else
                loc_lng = Double.parseDouble(editText_loc_lng.getText().toString());
            loc_cat = temp_id_cat[0];
            loc_userCreatedId = "0";

            AsyncTAddingDataToServer asyncT = new AsyncTAddingDataToServer();
            asyncT.execute();
        }
    };

    final OnLongPressListener mapLongPress = new OnLongPressListener() {
        @Override
        public boolean onLongPress(float v, float v1) {
            //Toast.makeText(rootView.getContext().getApplicationContext(), "this location at x= " + v + " and y= " + v1 + " | point " + onSingleTaps(v, v1) + "", Toast.LENGTH_SHORT).show();
            setPinLocation(v,v1,1);
            mMapView.setRotationAngle(0);
            // Also reset the compass angle.
            mCompass.setRotationAngle(0);
            return false;
        }
    };

    /**
     * When the map is tapped, select the graphic at that location.
     */
    final OnSingleTapListener mapTapCallback = new OnSingleTapListener() {
        @Override
        public void onSingleTap(float x, float y) {
            // Find out if we tapped on a Graphic

            //Toast.makeText(rootView.getContext().getApplicationContext(),"this location at x= "+x+" and y= "+y+" | point on SingleTaps"+onSingleTaps(x,y)+"",Toast.LENGTH_SHORT).show();
            setPinLocation(x,y,0);
            int[] graphicIDs = mResultsLayer.getGraphicIDs(x, y, 25);
            if (graphicIDs != null && graphicIDs.length > 0) {
                // If there is more than one graphic, only select the first found.

            } else {
                mMapView.setRotationAngle(0);
                // Also reset the compass angle.
                mCompass.setRotationAngle(0);
            }
        }
    };

    public void setPinLocation(float x, float y, int longOrSingle){
        mResultsLayer.removeAll();
        Point point = null;
        point= onSingleTaps(x, y);
        locationTouch.setLatitude(point.getY());
        locationTouch.setLongitude(point.getX());

        point = getAsPoint(locationTouch);
        Symbol symbol = mAdd;

        mResultsLayer.addGraphic(new Graphic(point, symbol));
        mMapView.setExtent(point, 100);

        if (longOrSingle==1){//long click
            editText_loc_lat.setText("" + locationTouch.getLatitude());
            editText_loc_lng.setText("" + locationTouch.getLongitude());
        }
        textView_mapLatLng.setText("lat:"+locationTouch.getLatitude()+"\nlng:"+locationTouch.getLongitude());
    }

    /**
     * When map is ready, set up the LocationDisplayManager.
     */
    final OnStatusChangedListener statusChangedListener = new OnStatusChangedListener() {

        private static final long serialVersionUID = 1L;

        @Override
        public void onStatusChanged(Object source, STATUS status) {
            if (source == mMapView && status == STATUS.INITIALIZED) {
                mMapSr = mMapView.getSpatialReference();
                if (mLDM == null) {
                    setupLocationListener();
                }
            }
        }
    };

    private void centerViewMap(){
        if (mMapView.isLoaded()) {
            // If LocationDisplayManager has a fix, pan to that location. If no
            // fix yet, this will happen when the first fix arrives, due to
            // callback set up previously.
            if ((mLDM != null) && (mLDM.getLocation() != null)) {
                // Keep current scale and go to current location, if there is one.
                mLDM.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
            }
        }
    }

    private void setupLocator() {
        // Parameterless constructor - uses the Esri world geocoding service.
        mLocator = Locator.createOnlineLocator();
        // Set up the outFields parameter for the search.
        mFindOutFields.add("loc_lat");
        mFindOutFields.add("loc_lng");
    }

    private void setupLocationListener() {
        if ((mMapView != null) && (mMapView.isLoaded())) {
            mLDM = mMapView.getLocationDisplayManager();
            mLDM.setLocationListener(new LocationListener() {

                boolean locationChanged = false;

                // Zooms to the current location when first GPS fix arrives.
                @Override
                public void onLocationChanged(Location loc) {
                    if (!locationChanged) {
                        Log.e("location change", ""+loc);
                        locationChanged = true;
                        locationTouch = loc;
                        location = loc;
                        mLDM.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
                        zoomToLocation(loc);
                    }
                }

                @Override
                public void onProviderDisabled(String arg0) {
                }

                @Override
                public void onProviderEnabled(String arg0) {
                }

                @Override
                public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
                }
            });

            mLDM.start();
        }
    }

    /**
     * Zoom to location using a specific size of extent.
     *
     * @param loc  the location to center the MapView at
     */
    private void zoomToLocation(Location loc) {
        Point mapPoint = getAsPoint(loc);
        Unit mapUnit = mMapSr.getUnit();
        double zoomFactor = Unit.convertUnits(ZOOM_BY,
                Unit.create(LinearUnit.Code.MILE_US), mapUnit);
        Envelope zoomExtent = new Envelope(mapPoint, zoomFactor, zoomFactor);
        mMapView.setExtent(zoomExtent);
    }

    private void clearCurrentResults() {
        if (mResultsLayer != null) {
            mResultsLayer.removeAll();
        }
    }


    private Point getAsPoint(Location loc) {
        Point wgsPoint = new Point(loc.getLongitude(), loc.getLatitude());
        return (Point) GeometryEngine.project(wgsPoint, SpatialReference.create(4326),
                mMapSr);
    }

    public Point onSingleTaps(float x, float y) {
        Point pnt = (Point) GeometryEngine.project(mMapView.toMapPoint(x, y), mMapView.getSpatialReference(), SpatialReference.create(4326));
        return pnt;
    }

    public void updateContent(Map<String, Object> attributes) {
        // This is called from UI thread (MapTap listener)

    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.pause();
        if (mLDM != null) {
            mLDM.pause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.unpause();
        if (mLDM != null) {
            mLDM.resume();
        }
        setupLocator();
        setupLocationListener();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mLDM != null) {
            mLDM.stop();
        }
    }

    /**
     * Class CallWebPageTask untuk implementasi class AscyncTask
     */
    private class CallWebPageTask extends AsyncTask<String, Void, String> {

        private ProgressDialog dialog;
        protected Context applicationContext;

        @Override
        protected void onPreExecute() {
            //this.dialog = ProgressDialog.show(applicationContext, "Login Process", "Please Wait...", true);
        }

        @Override
        protected String doInBackground(String... urls) {
            String response = "";
            HttpGetOrPost httpGetOrPost = new HttpGetOrPost();
            response = httpGetOrPost.getRequest(urls[0]);
            try {
                //simpan data dari web ke dalam array
                JSONArray menuItemArray = null;
                jObject = new JSONObject(response);
                menuItemArray = jObject.getJSONArray("dheket_allCat");
                id_kategori = new int[menuItemArray.length()];
                nama_katagori = new String[menuItemArray.length()];

                for (int i = 0; i < menuItemArray.length(); i++) {
                    id_kategori[i] = menuItemArray.getJSONObject(i).getInt("id_category");
                    nama_katagori[i] = menuItemArray.getJSONObject(i).getString("category_name").toString();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            //this.dialog.cancel();
            updateDataSpinner();
        }
    }

    public void getDataSpinner() {
        CallWebPageTask task = new CallWebPageTask();
        task.applicationContext = FormAddNewLocationActivity.this;
        String urls =url;
        Log.e("Sukses", urls);
        task.execute(new String[]{urls});
    }

    public void updateDataSpinner(){
        spinner_category.setAdapter(new MyAdapterSpinner(FormAddNewLocationActivity.this, R.layout.item_spinner_modified, nama_katagori));
        spinner_category.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                temp_id_cat[0] = id_kategori[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public class MyAdapterSpinner extends ArrayAdapter<String> {

        public MyAdapterSpinner(Context context, int textViewResourceId,   String[] objects) {
            super(context, textViewResourceId, objects);
        }

        @Override
        public View getDropDownView(int position, View convertView,ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        public View getCustomView(int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater=getLayoutInflater();
            View row=inflater.inflate(R.layout.item_spinner_modified, parent, false);
            TextView label=(TextView)row.findViewById(R.id.text_cat_name_spinner);
            label.setText(nama_katagori[position]);

            TextView sub=(TextView)row.findViewById(R.id.text_id_cat_name_spinner);
            sub.setText(""+id_kategori[position]);

            ImageView icon=(ImageView)row.findViewById(R.id.image_cat_image_spinner);
            //icon.setImageResource(arr_images[position]);

            return row;
        }
    }

    public void showDialogSelectTag(){
        LayoutInflater mInflater = LayoutInflater.from(this);
        View v = mInflater.inflate(R.layout.dialog_select_subcategory_or_tag, null);

        final AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setView(v);
        dialog.setCancelable(true);

        final ListView listView_tag;
        final Button btn_cancel, btn_ok;
        final ArrayAdapter<String> adapter;

        listView_tag = (ListView)v.findViewById(R.id.listView_dialog_select_tag);
        btn_cancel = (Button)v.findViewById(R.id.button_dialog_tag_cancel);
        btn_ok = (Button)v.findViewById(R.id.button_dialog_tag_ok);

        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_multiple_choice, nama_katagori);
        listView_tag.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView_tag.setAdapter(adapter);

        if (id_subCat!=null){
            for (int i = 0; i < id_subCat.length ; i++) {
                listView_tag.setItemChecked(id_subCat[i],true);
            }
        }

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SparseBooleanArray checked = listView_tag.getCheckedItemPositions();
                ArrayList<String> selectedItems = new ArrayList<String>();
                String tag = "";
                id_subCat = new int[checked.size()];
                for (int i = 0; i < checked.size(); i++) {
                    // Item position in adapter
                    int position = checked.keyAt(i);
                    // Add sport if it is checked i.e.) == TRUE!
                    if (checked.valueAt(i)) {
                        selectedItems.add(adapter.getItem(position));
                        id_subCat[i] = checked.keyAt(i);
                    }
                }

                String[] outputStrArr = new String[selectedItems.size()];
                for (int i = 0; i < selectedItems.size(); i++) {
                    tag = tag + selectedItems.get(i)+" ";
                }

                editText_loc_subCat.setText(tag);

                dialog.dismiss();
            }
        });

        dialog.show();
    }

    /* Inner class to get response */
    class AsyncTAddingDataToServer extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://dheket.esy.es/addNewLocation.php");

            try {

                JSONObject jsonobj = new JSONObject();

                jsonobj.put("loc_name", loc_name);
                jsonobj.put("loc_address", loc_address);
                jsonobj.put("loc_phone", loc_phone);
                jsonobj.put("loc_lat", loc_lat);
                jsonobj.put("loc_lng", loc_lng);
                jsonobj.put("loc_cat", loc_cat);
                jsonobj.put("loc_tag", loc_tag);
                jsonobj.put("loc_photo", loc_photo);
                jsonobj.put("loc_desc", loc_desc);
                jsonobj.put("loc_userCreatedId", loc_userCreatedId);

                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("dheket_new_loc", jsonobj.toString()));

                Log.e("mainToPost", "mainToPost" + nameValuePairs.toString());

                // Use UrlEncodedFormEntity to send in proper format which we need
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Execute HTTP Post Request
                HttpResponse response = httpclient.execute(httppost);
                InputStream inputStream = response.getEntity().getContent();
                InputStreamToStringExample str = new InputStreamToStringExample();
                responseServer = str.getStringFromInputStream(inputStream);
                Log.e("response", "response ----- " + responseServer + "|");

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (responseServer!=null && responseServer.equalsIgnoreCase("1")) {
                Toast.makeText(getApplicationContext(),"Success!",Toast.LENGTH_SHORT).show();
                responseServer="";
            } else {
                if (responseServer.equalsIgnoreCase("") || responseServer.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Ops, Error! Please Try Again!",Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public static class InputStreamToStringExample {

        public static void main(String[] args) throws IOException {

            // intilize an InputStream
            InputStream is = new ByteArrayInputStream("file content is process".getBytes());

            String result = getStringFromInputStream(is);

            System.out.println(result);
            System.out.println("Done");

        }

        // convert InputStream to String
        private static String getStringFromInputStream(InputStream is) {

            BufferedReader br = null;
            StringBuilder sb = new StringBuilder();

            String line;
            try {

                br = new BufferedReader(new InputStreamReader(is));
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return sb.toString();
        }

    }
}
