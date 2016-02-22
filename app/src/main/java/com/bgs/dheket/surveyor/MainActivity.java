package com.bgs.dheket.surveyor;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    final static double ZOOM_BY = 15;

    MapView mMapView = null;
    SpatialReference mMapSr = null;
    GraphicsLayer mResultsLayer = null;
    ImageView imageView_pic, imageView_pic_back;
    ImageButton imageButton_minimaze,imageButton_maximaze,imageButton_gps_center;
    PictureMarkerSymbol mCat1, mCat2, mCat3, mCat4, mCat5, mAdd;
    // Views to show selected search result information.
    TextView textView_id_loc,textView_loc_name,textView_loc_address,textView_loc_promo,textView_loc_idstance;

    android.support.v7.app.ActionBar actionBar;

    Locator mLocator;
    Location locationTouch;
    Location location;
    ArrayList<String> mFindOutFields = new ArrayList<>();

    LocationDisplayManager mLDM;

    Compass mCompass;
    Utility formatNumber = new Utility();

    private JSONObject jObject;
    private JSONArray menuItemArray;
    private String jsonResult ="";
    String temp_loc_name, temp_loc_address,temp_loc_pic;
    int temp_loc_promo,temp_id_loc,temp_loc_cat_id;
    double temp_loc_distance, temp_loc_lat, temp_loc_lng;

    double radius = 2;
    double latitude, longitude;
    String urls ="http://dheket.esy.es/getAllLocation.php";
    String parameters,kategori;
    int cat_id,loc_id;
    ViewGroup placeLayout,but_minmax;
    Bundle paket;

    boolean isFirst = true,maxView = true, minView = true;
    CallWebPageTask task;
    ActionBarDrawerToggle toggle;

    String[] loc_name, loc_address,loc_pic;
    int[] loc_promo,id_loc,loc_cat;
    double[] loc_distance, loc_lat, loc_lng;

    FloatingActionButton fab;
    //ImageView fab;
    int fabYorigin;
    int fabYnew;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        actionBar = getSupportActionBar();

        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        //actionBar.setHomeAsUpIndicator(R.drawable.logo);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle("Dheket");
        actionBar.setSubtitle("For Surveyor");

//        actionBar.setSubtitle(Html.fromHtml("<font color='#FFBF00'>Location in Radius " + formatter.format(radius) + " Km</font>"));

        final Animation animTranslate = AnimationUtils.loadAnimation(this, R.anim.anim_translate_y_2persen);

        fabYorigin = screenProperties("h") - (int)(72 * (getResources().getDisplayMetrics().density) + 0.5f);
        fabYnew = screenProperties("h") - (int)(150 * (getResources().getDisplayMetrics().density) + 0.5f);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        //fab.setY(fabYnew);
        fab.setAnimation(animTranslate);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
                Intent addLoc = new Intent(MainActivity.this, FormAddNewLocationActivity.class);
                startActivity(addLoc);
                finish();
            }
        });
        //fab.setY(fabYorigin);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        FacebookSdk.sdkInitialize(getApplicationContext());

        //Retrieve the map and initial extent from XML layout
        mMapView = (MapView)findViewById(R.id.map_single);
        mMapView.setOnStatusChangedListener(statusChangedListener);
        mMapView.setOnSingleTapListener(mapTapCallback);
       /* mMapView.setOnLongPressListener(mapLongPress);*/
/*        paket = getIntent().getExtras();
        loc_id = paket.getInt("id_loc");
        cat_id = paket.getInt("cat_id");
        radius = paket.getDouble("radius");
        latitude = paket.getDouble("latitude");
        longitude = paket.getDouble("longitude");
        kategori = paket.getString("kategori");*/

        imageView_pic = (ImageView)findViewById(R.id.imageView_tfm_pic_single);
        imageView_pic_back = (ImageView)findViewById(R.id.imageView_tfm_pic_back_single);
        imageButton_minimaze = (ImageButton)findViewById(R.id.imageButton_minimaze_single);
        imageButton_maximaze = (ImageButton)findViewById(R.id.imageButton_maximize_single);
        textView_id_loc = (TextView)findViewById(R.id.textView_tmf_id_loc_single);
        textView_loc_name = (TextView)findViewById(R.id.textView_tfm_loc_name_single);
        textView_loc_address = (TextView)findViewById(R.id.textView_tfm_loc_address_single);
        textView_loc_promo = (TextView)findViewById(R.id.textView_tfm_promo_single);
        textView_loc_idstance = (TextView)findViewById(R.id.textView_tfm_dist_single);
        placeLayout = (ViewGroup)findViewById(R.id.detail_loc_single);
        placeLayout.setVisibility(View.GONE);
        but_minmax = (ViewGroup)findViewById(R.id.button_minmax);
        but_minmax.setVisibility(View.GONE);

        placeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, DetailLocationActivity.class);
                Bundle paket = new Bundle();
                paket.putInt("id_loc", Integer.parseInt(textView_id_loc.getText().toString()));
                paket.putString("loc_name", textView_loc_name.getText().toString());
                int promo;
                if (textView_loc_promo.getText().toString().equalsIgnoreCase("promo")) promo = 1;
                else promo = 0;
                paket.putInt("loc_promo", promo);
                paket.putString("loc_address", textView_loc_address.getText().toString());
                paket.putInt("cat_id", cat_id);
                paket.putDouble("latitude", latitude);
                paket.putDouble("longitude", longitude);
                paket.putDouble("radius", radius);
                paket.putString("kategori",kategori);
                //Toast.makeText(rootView.getContext(), "Id Loc " + selectId, Toast.LENGTH_LONG).show();
                i.putExtras(paket);
                startActivity(i);
                finish();
            }
        });

        imageButton_minimaze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                placeLayout.setVisibility(View.GONE);
                minView = false;
                maxView = true;
                imageButton_maximaze.setVisibility(View.VISIBLE);
                imageButton_minimaze.setVisibility(View.GONE);
                fab.setY(fabYorigin);
            }
        });

        imageButton_maximaze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                placeLayout.setVisibility(View.VISIBLE);
                minView = true;
                maxView = false;
                imageButton_maximaze.setVisibility(View.GONE);
                imageButton_minimaze.setVisibility(View.VISIBLE);
                fab.setY(fabYnew);
            }
        });

        mResultsLayer = new GraphicsLayer();
        mResultsLayer.setSelectionColorWidth(6);
        mMapView.addLayer(mResultsLayer);
        mMapView.setAllowRotationByPinch(true);

        // Enabled wrap around map.
        mMapView.enableWrapAround(true);

        // Create the Compass custom view, and add it onto the MapView.
        mCompass = new Compass(MainActivity.this, null, mMapView);
        mCompass.setY((int)(60* (getResources().getDisplayMetrics().density) + 0.5f));
        mCompass.setX((int) (5 * (getResources().getDisplayMetrics().density) + 0.5f));
        mMapView.addView(mCompass);

        mCat1 = new PictureMarkerSymbol(getApplicationContext(), ContextCompat.getDrawable(getApplicationContext(), R.drawable.pin_red));
        mCat2 = new PictureMarkerSymbol(getApplicationContext(), ContextCompat.getDrawable(getApplicationContext(), R.drawable.pin_green));
        mCat3 = new PictureMarkerSymbol(getApplicationContext(), ContextCompat.getDrawable(getApplicationContext(), R.drawable.pin_blue));
        mCat4 = new PictureMarkerSymbol(getApplicationContext(), ContextCompat.getDrawable(getApplicationContext(), R.drawable.pin_orange));
        mCat5 = new PictureMarkerSymbol(getApplicationContext(), ContextCompat.getDrawable(getApplicationContext(), R.drawable.pin_yellow));
        mAdd = new PictureMarkerSymbol(getApplicationContext(), ContextCompat.getDrawable(getApplicationContext(), R.drawable.pin_add));

        setupLocator();
        setupLocationListener();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_layout, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        /*int id = item.getItemId();*/

        switch (item.getItemId()) {
            case R.id.locate:
                if (mMapView.isLoaded()) {
                    // If LocationDisplayManager has a fix, pan to that location. If no
                    // fix yet, this will happen when the first fix arrives, due to
                    // callback set up previously.
                    if ((mLDM != null) && (mLDM.getLocation() != null)) {
                        // Keep current scale and go to current location, if there is one.
                        mLDM.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
                    }
                }

                return true;

            /*case android.R.id.home:
                back_to_previous_screen();
                return super.onOptionsItemSelected(item);*/

            case R.id.action_logout:
                logout_user();
                return super.onOptionsItemSelected(item);

            default:
                return super.onOptionsItemSelected(item);

        }

        //noinspection SimplifiableIfStatement
        /*if (id == R.id.action_settings) {
            return true;
        }*/
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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
                        Intent logout_user_fb = new Intent(MainActivity.this, FormLoginActivity.class);
                        startActivity(logout_user_fb);
                        finish();
                    }
                })
                .setNegativeButton(cancel, null);
        builder.create().show();
    }

    public void getDataFromServer() {
        task = new CallWebPageTask();
        task.applicationContext = MainActivity.this;
        parameters = urls + "?rad="+radius+"&lat=" + latitude + "&lng=" + longitude;
        Log.e("OK Connecting Sukses", "" + parameters);
        //Log.e("Sukses", parameters);
        task.execute(new String[]{parameters});
    }

    /**
     * When the map is long tapped, select the graphic at that location.
     */
    final OnLongPressListener mapLongPress = new OnLongPressListener() {
        @Override
        public boolean onLongPress(float v, float v1) {
            //Toast.makeText(rootView.getContext().getApplicationContext(), "this location at x= " + v + " and y= " + v1 + " | point " + onSingleTaps(v, v1) + "", Toast.LENGTH_SHORT).show();
            mResultsLayer.removeAll();
            Point point = onSingleTaps(v, v1);
            Location location = locationTouch;
            locationTouch.setLatitude(point.getY());
            locationTouch.setLongitude(point.getX());
            point = getAsPoint(locationTouch);
            Symbol symbol = mAdd;

            mResultsLayer.addGraphic(new Graphic(point, symbol));
            mMapView.setExtent(point, 100);
            placeLayout.setVisibility(View.GONE);
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
            int[] graphicIDs = mResultsLayer.getGraphicIDs(x, y, 25);
            if (graphicIDs != null && graphicIDs.length > 0) {
                but_minmax.setVisibility(View.VISIBLE);
                // If there is more than one graphic, only select the first found.
                if (graphicIDs.length > 1){
                    int id = graphicIDs[0];
                    graphicIDs = new int[] { id };
                }

                // Only deselect the last graphic if user has tapped a new one. App
                // remains showing the last selected nearby service information,
                // as that is the main focus of the app.
                mResultsLayer.clearSelection();

                // Select the graphic
                mResultsLayer.setSelectedGraphics(graphicIDs, true);

                // Use the graphic attributes to update the information views.
                Graphic gr = mResultsLayer.getGraphic(graphicIDs[0]);
                Log.e("atrribut", "" + gr.getAttributes());

                if (minView){
                    imageButton_minimaze.setVisibility(View.VISIBLE);
                    imageButton_maximaze.setVisibility(View.GONE);
                    placeLayout.setVisibility(View.VISIBLE);
                    fab.setY(fabYnew);
                } else {
                    imageButton_minimaze.setVisibility(View.GONE);
                    imageButton_maximaze.setVisibility(View.VISIBLE);
                    fab.setY(fabYorigin);
                }

                if (gr.getAttributes().isEmpty()) {
                    Toast.makeText(getApplicationContext(), "No Attribute for this location!", Toast.LENGTH_SHORT).show();
                }else {
                    updateContent(gr.getAttributes());
                }
            } else {
                placeLayout.setVisibility(View.GONE);
                but_minmax.setVisibility(View.GONE);
                imageButton_minimaze.setVisibility(View.GONE);
                imageButton_maximaze.setVisibility(View.GONE);
                mMapView.setRotationAngle(0);
                // Also reset the compass angle.
                mCompass.setRotationAngle(0);
                fab.setY(fabYorigin);
            }
        }
    };
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

    private void setupLocator() {
        // Parameterless constructor - uses the Esri world geocoding service.
        mLocator = Locator.createOnlineLocator();

        // Set up the outFields parameter for the search.
        mFindOutFields.add("id_loc");
        mFindOutFields.add("loc_name");
        mFindOutFields.add("loc_address");
        mFindOutFields.add("loc_promo");
        mFindOutFields.add("loc_distance");
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
                        locationChanged = true;
                        Log.e("sukses location ", "lat " + loc.getLatitude() + " | lng " + loc.getLongitude() + " | point " + getAsPoint(loc));
                        location = loc;
                        locationTouch = location;
                        // After zooming, turn on the Location pan mode to show the location
                        // symbol. This will disable as soon as you interact with the map.
                        latitude = loc.getLatitude();
                        longitude = loc.getLongitude();
                        getDataFromServer();
                        mLDM.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
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
        textView_id_loc.setText("");
        textView_loc_name.setText("");
        textView_loc_address.setText("");
        imageView_pic.setImageResource(R.drawable.logo);
        textView_loc_promo.setText("");
        textView_loc_idstance.setText("");
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
        String title = attributes.get("loc_name").toString();
        textView_loc_name.setText(title);

        String address = attributes.get("loc_address").toString();
        textView_loc_address.setText(address);

        String id_loc = attributes.get("id_loc").toString();
        textView_id_loc.setText(id_loc);

        String distance = attributes.get("loc_distance").toString();
        double meters = Double.parseDouble(distance);
        textView_loc_idstance.setText(""+formatNumber.changeFormatNumber(meters)+" Km");

        int promo = (int) attributes.get("loc_promo");
        if (promo == 1) textView_loc_promo.setText("Promo");
        else textView_loc_promo.setText("-");
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.pause();
        if (mLDM != null) {
            mLDM.pause();
        }
        //fab.setY(fabYorigin);
        /*but_minmax.setVisibility(View.GONE);
        placeLayout.setVisibility(View.GONE);*/
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
        /*but_minmax.setVisibility(View.GONE);
        placeLayout.setVisibility(View.GONE);*/
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mLDM != null) {
            mLDM.stop();
        }
    }

    private class CallWebPageTask extends AsyncTask<String, Void, String> {

        private ProgressDialog dialog;
        protected Context applicationContext;

        @Override
        protected void onPreExecute() {
            /*if (isFirst==true){
                this.dialog = ProgressDialog.show(applicationContext, "Retrieving Data", "Please Wait...", true);
            }*/
        }

        @Override
        protected String doInBackground(String... url) {
            String response = "";
            HttpGetOrPost httpGetOrPost = new HttpGetOrPost();
            response = httpGetOrPost.getRequest(url[0]);
            try {
                //simpan data dari web ke dalam array
                menuItemArray = null;
                jObject = new JSONObject(response);
                menuItemArray = jObject.getJSONArray("dheket_allLoc");
                id_loc = new int[menuItemArray.length()];
                loc_name = new String[menuItemArray.length()];
                loc_address = new String[menuItemArray.length()];
                loc_promo = new int[menuItemArray.length()];
                loc_distance = new double[menuItemArray.length()];
                loc_pic = new String[menuItemArray.length()];
                loc_lat = new double[menuItemArray.length()];
                loc_lng = new double[menuItemArray.length()];
                loc_cat = new int[menuItemArray.length()];
                Log.e("Data dari server",""+menuItemArray);
                for (int i = 0; i < menuItemArray.length(); i++) {
                    //String id_loc, String loc_name, String loc_address, int loc_promo, double loc_distance, int loc_pic
                    //listViewItems.add(new ItemObjectCustomList(menuItemArray.getJSONObject(i).getInt("id_category")),menuItemArray.getJSONObject(i).getString("location_name").toString(),menuItemArray.getJSONObject(i).getString("location_address").toString(),menuItemArray.getJSONObject(i).getInt("isPromo"),menuItemArray.getJSONObject(i).getDouble("distance"),menuItemArray.getJSONObject(i).getString("photo").toString()));
                    id_loc[i]=menuItemArray.getJSONObject(i).getInt("id_location");
                    loc_name[i]=menuItemArray.getJSONObject(i).getString("location_name").toString();
                    loc_address[i]=menuItemArray.getJSONObject(i).getString("location_address").toString();
                    loc_promo[i]=menuItemArray.getJSONObject(i).getInt("isPromo");
                    loc_distance[i]= Double.parseDouble(formatNumber.changeFormatNumber(menuItemArray.getJSONObject(i).getDouble("distance")));
                    loc_pic[i]=menuItemArray.getJSONObject(i).getString("photo").toString();
                    loc_lat[i]=menuItemArray.getJSONObject(i).getDouble("latitude");
                    loc_lng[i]=menuItemArray.getJSONObject(i).getDouble("longitude");
                    loc_cat[i]=menuItemArray.getJSONObject(i).getInt("category_id");
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("cek 2", "error" + e);
            }

            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            if (menuItemArray!= null) updateData();
        }
    }

    public void updateData() {
        MultiPoint fullExtent = new MultiPoint();
        Symbol symbol = null;
        //-6.21267000, 106.61778566
        Map<String, Object> attr = new HashMap<String, Object>();
        if (id_loc != null) {
            mResultsLayer.removeAll();
            clearCurrentResults();
            for (int i = 0; i < id_loc.length; i++) {
                Location locationPin = location;
                locationPin.setLatitude(loc_lat[i]);
                locationPin.setLongitude(loc_lng[i]);
                Point point = getAsPoint(locationPin);
                attr.put("id_loc", id_loc[i]);
                attr.put("loc_name", loc_name[i]);
                attr.put("loc_address", loc_address[i]);
                attr.put("loc_promo", loc_promo[i]);
                attr.put("loc_distance", loc_distance[i]);
                attr.put("loc_pic", loc_pic[i]);
                attr.put("loc_lat", loc_lat[i]);
                attr.put("loc_lng", loc_lng[i]);

                //Log.e("Ok sip", "this location at lat= " + loc_lat[i] + " and lng= " + loc_lng[i] + " | point on getAsPoint" + point + "");
                if (loc_cat[i] == 1) symbol = mCat1;
                else if (loc_cat[i] == 2) symbol = mCat2;
                else if (loc_cat[i] == 3) symbol = mCat3;
                else if (loc_cat[i] == 4) symbol = mCat4;
                else symbol = mCat5;

                mResultsLayer.addGraphic(new Graphic(point, symbol, attr));
                fullExtent.add(point);
            }
            mMapView.setExtent(fullExtent, 100);
            if (id_loc.length < 2) {
                if ((mLDM != null) && (mLDM.getLocation() != null)) {
                    // Keep current scale and go to current location, if there is one.
                    zoomToLocation(location);
                    mLDM.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
                }
            }
            placeLayout.setVisibility(View.GONE);
            imageButton_maximaze.setVisibility(View.GONE);
            imageButton_minimaze.setVisibility(View.GONE);
            fab.setY(fabYorigin);
        }
    }

    public int screenProperties(String HW){
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        float screenDensity = metrics.density;
        int screenDensityDPI = metrics.densityDpi;
        float screenscaledDensity = metrics.scaledDensity;
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;

        /*Log.e("De","Screen Density=" + screenDensity + "Screen DensityDPI=" + screenDensityDPI + "Screen Scaled DensityDPI=" + screenscaledDensity + "\n"
                + "Height=" + height + "\n"
                + "Width=" + width);
                */
        if (HW.equalsIgnoreCase("h")){
            return height;
        } else {
            return width;
        }
    }
}