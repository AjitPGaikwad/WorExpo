package com.ajit.worexpo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.ajit.worexpo.adpter.PlaceRecyclerViewAdapter;
import com.ajit.worexpo.adpter.PlacesRecyclerViewAdapter;
import com.ajit.worexpo.model.MyPlaces;
import com.ajit.worexpo.remotes.GoogleApiService;
import com.ajit.worexpo.remotes.RetrofitBuilder;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Main Activity";
    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
    };

    double latitude;
    double longitude;

    ProgressDialog progressDialog;

    private FusedLocationProviderClient mFusedLocationClient;
    LocationManager lm;
    LocationManager locationManager;

    double lat = 0;
    double lng = 0;
    private String placeType = "atm";
    private GoogleApiService googleApiService;
    private MyPlaces myPlaces;

    private RecyclerView recyclerViewPlaces;

    protected GeoDataClient geoDataClient;
    protected PlaceDetectionClient placeDetectionClient;
    protected RecyclerView recyclerView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //placeDetectionClient = Places.getPlaceDetectionClient(this, null);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getCurrentLocation();
        }

    }

    private void locationService() {

        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {

            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Please wait while fetching data from GPS .......");
            progressDialog.setCancelable(false);
            progressDialog.show();


            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            final LocationListener locationListener = new MyLocationListener();
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                progressDialog.dismiss();

                return;
            }

            progressDialog.dismiss();

            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);

            mFusedLocationClient.getLastLocation().addOnSuccessListener(MainActivity.this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {

                    if (location != null) {

                        lat = location.getLatitude();
                        lng = location.getLongitude();
                        getNearbyPlaces();

                    } else {
                        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

                            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                return;
                            }

                            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
                        } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {

                            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10, locationListener);
                        }
                    }
                }
            });
        } else {
            Toast.makeText(MainActivity.this, "GPS off", Toast.LENGTH_SHORT).show();
        }
    }

    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location loc) {

            longitude = loc.getLongitude();
            latitude = loc.getLatitude();

            lat = loc.getLatitude();
            lng = loc.getLongitude();
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }

    private String buildUrl(double latitude, double longitude, String API_KEY) {
        StringBuilder urlString = new StringBuilder("api/place/search/json?");

        urlString.append("&location=");
        urlString.append(Double.toString(latitude));
        urlString.append(",");
        urlString.append(Double.toString(longitude));
        urlString.append("&radius=5000"); // places between 5 kilometer
        urlString.append("&types=" + placeType.toLowerCase());
        urlString.append("&sensor=false&key=" + API_KEY);

        return urlString.toString();
    }

    private void getNearbyPlaces() {

        if (lat != 0 && lng != 0) {

            final ProgressDialog dialog = new ProgressDialog(MainActivity.this);
            dialog.setMessage("Loading...");
            dialog.setCancelable(false);
            dialog.setIndeterminate(false);
            dialog.show();

            String apiKey = getResources().getString(R.string.google_api_key);
            String url = buildUrl(lat, lng, apiKey);
            Log.d("finalUrl", url);

            googleApiService = RetrofitBuilder.builder().create(GoogleApiService.class);

            Call<MyPlaces> call = googleApiService.getMyNearByPlaces(url);

            call.enqueue(new Callback<MyPlaces>() {
                @Override
                public void onResponse(Call<MyPlaces> call, Response<MyPlaces> response) {
                    //Log.d("MyPlaces", response.body().toString());
                    myPlaces = response.body();
                    Log.d("MyPlaces", myPlaces.getResults().get(0).toString());

                    dialog.dismiss();

                    setUpRecyclerView();
                    //linearLayoutShowOnMap.setVisibility(View.VISIBLE);
                }

                @Override
                public void onFailure(Call<MyPlaces> call, Throwable t) {
                    dialog.dismiss();
                    Toast.makeText(MainActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
//
//
////            String baseUrl = "https://maps.googleapis.com/maps/api/place/search/json?"
////                    + "&location="
////                    + Double.toString(lat) +","
////                    +Double.toString(lng)
////                    + "&radius=" + finalRadius
////                    +"&types="+category
////                    + "&sensor/";
//
//            String restUrl = "json?&location=" + Double.toString(lat) + ","
//                    + Double.toString(lng)
//                    + "&radius=" + finalRadius
//                    + "&types=" + category
//                    + "&sensor=false&key=" + apiKey;
//
//
//            Retrofit retrofit = new Retrofit.Builder().baseUrl(Api.BASE_URL)
//                    .addConverterFactory(GsonConverterFactory.create()).build();
//
//            Api api = retrofit.create(Api.class);
//            Call<ModelPlace> call = api.getResults(restUrl);
//
//
//            call.enqueue(new Callback<ModelPlace>() {
//                @Override
//                public void onResponse(Call<ModelPlace> call, Response<ModelPlace> response) {
//
//                    ModelPlace modelPlace = response.body();
//
//
//                    resultList = modelPlace.getResults();
//                    adapter = new PlaceAdapter(resultList, getContext(), lat, lng, categorySP.getSelectedItemPosition());
//                    RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
//                    recyclerView.setLayoutManager(layoutManager);
//                    recyclerView.setItemAnimator(new DefaultItemAnimator());
//                    recyclerView.setAdapter(adapter);
//                    adapter.notifyDataSetChanged();
//
//                    dialog.dismiss();
//
//                    if (resultList.size() > 0) {
//
//                        mapLayout.setVisibility(View.VISIBLE);
//
//                    } else {
//
//                        Toast.makeText(getContext(), "No place found between " + radius + " kilometer", Toast.LENGTH_SHORT).show();
//                    }
//
//
//                }
//
//                @Override
//                public void onFailure(Call<ModelPlace> call, Throwable t) {
//
//                    Toast.makeText(getContext(), "Error found", Toast.LENGTH_SHORT).show();
//                    dialog.dismiss();
//                    Log.d("ErrorOccur", t.getMessage());
//                }
//            });
        }
    }

    private void getCurrentLocation() {
        if (hasPermissions(this, PERMISSIONS)) {
            locationService();
            //getCurrentPlaceData();
        } else {
            checkPermission();
        }
    }

    private void checkPermission() {
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }
    }

    boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PERMISSION_ALL) {
            if (resultCode == RESULT_OK) {
                locationService();
                //getCurrentPlaceData();
            }
        }
    }

    private void setUpRecyclerView() {

       /* LinearLayoutManager layoutManagerCenter
                = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);

        PlaceRecyclerViewAdapter adapter = new PlaceRecyclerViewAdapter(MainActivity.this, myPlaces, lat, lng);
        //RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(MainActivity.this);
        recyclerViewPlaces = (RecyclerView)findViewById(R.id.placeRec);
        recyclerViewPlaces.setLayoutManager(layoutManagerCenter);
        recyclerViewPlaces.setItemAnimator(new DefaultItemAnimator());
        recyclerViewPlaces.setAdapter(adapter);
        SnapHelper snapHelperCenter = new LinearSnapHelper();
        snapHelperCenter.attachToRecyclerView(recyclerViewPlaces);
        adapter.notifyDataSetChanged();*/


       /* LinearLayoutManager layoutManagerStart
                = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        startSnapRecyclerView.setLayoutManager(layoutManagerStart);
        appListStartAdapter = new AppListAdapter(this);
        startSnapRecyclerView.setAdapter(appListStartAdapter);
        SnapHelper snapHelperStart = new StartSnapHelper();
        snapHelperStart.attachToRecyclerView(startSnapRecyclerView);*/


      /*  final RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        PlaceRecyclerViewAdapter placeRecyclerViewAdapter = new PlaceRecyclerViewAdapter(MainActivity.this, myPlaces, lat, lng);
        //RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(MainActivity.this);
        recyclerViewPlaces = (RecyclerView)findViewById(R.id.placeRec);
        recyclerViewPlaces.setHasFixedSize(true);
        recyclerViewPlaces.setLayoutManager(layoutManager);
        recyclerViewPlaces.setAdapter(placeRecyclerViewAdapter);
        final SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(recyclerViewPlaces);*/

        //ANIMATION

     /*   new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                RecyclerView.ViewHolder viewHolder = recyclerViewPlaces.findViewHolderForAdapterPosition(0);
                RelativeLayout rl1 = viewHolder.itemView.findViewById(R.id.rl1);
                rl1.animate().scaleY(1).scaleX(1).setDuration(350).setInterpolator(new AccelerateInterpolator()).start();
            }
        },100);

        recyclerViewPlaces.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                View v = snapHelper.findSnapView(layoutManager);
                int pos = layoutManager.getPosition(v);

                RecyclerView.ViewHolder viewHolder = recyclerViewPlaces.findViewHolderForAdapterPosition(pos);
                RelativeLayout rl1 = viewHolder.itemView.findViewById(R.id.rl1);

                if (newState == RecyclerView.SCROLL_STATE_IDLE){
                    rl1.animate().setDuration(350).scaleX(1).scaleY(1).setInterpolator(new AccelerateInterpolator()).start();
                }else{
                    rl1.animate().setDuration(350).scaleX(0.75f).scaleY(0.75f).setInterpolator(new AccelerateInterpolator()).start();
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });*/

        recyclerViewPlaces = (RecyclerView)findViewById(R.id.placeRec);
        recyclerViewPlaces.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewPlaces.setHasFixedSize(true);
        recyclerViewPlaces.setItemAnimator(new DefaultItemAnimator());

        /**
         * Default Center
         */
        SnapHelper snapHelperCenterDefault = new LinearSnapHelper();
        snapHelperCenterDefault.attachToRecyclerView(recyclerViewPlaces);

        PlaceRecyclerViewAdapter placeRecyclerViewAdapter = new PlaceRecyclerViewAdapter(MainActivity.this, myPlaces, lat, lng);
        recyclerViewPlaces.setAdapter(placeRecyclerViewAdapter);


    }

    @SuppressLint("MissingPermission")
    private void getCurrentPlaceData() {
        Task<PlaceLikelihoodBufferResponse> placeResult = placeDetectionClient.
                getCurrentPlace(null);
        placeResult.addOnCompleteListener(new OnCompleteListener<PlaceLikelihoodBufferResponse>() {
            @Override
            public void onComplete(@NonNull Task<PlaceLikelihoodBufferResponse> task) {
                Log.d(TAG, "current location places info");
                List<Place> placesList = new ArrayList<Place>();
                PlaceLikelihoodBufferResponse likelyPlaces = task.getResult();
                for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                    placesList.add(placeLikelihood.getPlace().freeze());
                }
                likelyPlaces.release();

                PlacesRecyclerViewAdapter recyclerViewAdapter = new
                        PlacesRecyclerViewAdapter(placesList,
                        MainActivity.this);
                recyclerViewPlaces.setAdapter(recyclerViewAdapter);
            }
        });
    }
}
