package com.ajit.worexpo.fragment;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ajit.worexpo.MainActivity;
import com.ajit.worexpo.R;
import com.ajit.worexpo.adpter.PlaceListAdapter;
import com.ajit.worexpo.adpter.PlaceRecyclerViewAdapter;
import com.ajit.worexpo.model.Location;
import com.ajit.worexpo.model.MyPlaces;
import com.ajit.worexpo.model.Results;
import com.ajit.worexpo.utils.DirectionsJSONParser;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.model.DirectionsResult;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PlaceListFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link PlaceListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PlaceListFragment extends Fragment implements OnMapReadyCallback, PlaceListAdapter.ItemClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";



    private GoogleMap mMap;
    private double lng;
    private double lat;
    private String name;
    private String address;
    private MyPlaces myPlaces;
    private Results results;
    private Location location2;

    LatLng currentPosition;

    RecyclerView recyclerViewPlaces;
    View view;

    Polyline polyline;

    private int PATTERN_GAP_LENGTH_PX = 10;  // 1
    private Gap GAP = new Gap(PATTERN_GAP_LENGTH_PX);
    private Dot DOT = new Dot();
    private List<PatternItem> PATTERN_DOTTED = Arrays.asList(DOT, GAP);  // 2


    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    int px,width,height,padding = 0;


    public PlaceListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment PlaceListFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PlaceListFragment newInstance(String param1, String param2) {
        PlaceListFragment fragment = new PlaceListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            myPlaces = (MyPlaces)getArguments().getSerializable("places");
            //results = (Results)getArguments().getSerializable("results");
            lng = getArguments().getDouble("lng");
            lat = getArguments().getDouble("lat");
            //location2 = results.getGeometry().getLocation();
            //name = results.getName();
            //address = results.getVicinity();
        }

        px = (int) (TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 320, getResources().getDisplayMetrics()));

        width=getResources().getDisplayMetrics().widthPixels;
        height=getResources().getDisplayMetrics().heightPixels - px ;

        padding=(int)(width * 0.10);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_map_details, container, false);


        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.gmap);
        mapFragment.getMapAsync(this);

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setMinZoomPreference(11);

        currentPosition = new LatLng(lat, lng);

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(currentPosition)
                .title("Your Location")
                //.snippet(address)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                .alpha(1f);

        mMap.addMarker(markerOptions);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentPosition));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(16.5f));

        for(int i=0;i<myPlaces.getResults().size();i++) {
            showPlaces(myPlaces.getResults().get(i),i);
        }

        setUpRecyclerView();
    }

    @Override
    public void onClick(Results results, int position) {
        showPlaces(results,position);
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    private void showPlaces(Results results, int i) {

        LatLng destinationPosition = new LatLng(Double.valueOf(results.getGeometry().getLocation().getLat()), Double.valueOf(results.getGeometry().getLocation().getLng()));
        // for destination
        mMap.addMarker(new MarkerOptions().position(destinationPosition)
                .title(results.getName())
                //.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                .icon(BitmapDescriptorFactory.fromBitmap(
                        createCustomMarker(getActivity(),R.drawable.pharmacy,"")))
                .snippet(results.getVicinity())
                .alpha(1f))
                .showInfoWindow();

        if(i==0){
            showDistance(currentPosition,destinationPosition);
        }
    }

    public static Bitmap createCustomMarker(Context context, @DrawableRes int resource, String _name) {

        View marker = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.custom_marker_bottom, null);

        //TextView txt_name = (TextView)marker.findViewById(R.id.name);
        //txt_name.setText(_name);

        final CircleImageView markerImage = marker.findViewById(R.id.user_dp);

        Picasso.get()
                .setLoggingEnabled(true);
        Picasso.get()
                .load(resource)
                .resize(50,50)
                .into(markerImage);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        marker.setLayoutParams(new ViewGroup.LayoutParams(52, ViewGroup.LayoutParams.WRAP_CONTENT));
        marker.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
        marker.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
        marker.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(marker.getMeasuredWidth(), marker.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        marker.draw(canvas);

        return bitmap;
    }

    private void showDistance(LatLng currentPosition, LatLng destinationPosition) {


        String url = getDirectionsUrl(currentPosition, destinationPosition);

        FetchUrl fetchUrl = new FetchUrl();

        fetchUrl.execute(url);
        //move map camera

       /* CameraPosition cameraPosition = new CameraPosition.Builder().target(destinationPosition).zoom(14).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));*/

        //mMap.moveCamera(CameraUpdateFactory.newLatLng(currentPosition));
        //mMap.animateCamera(CameraUpdateFactory.zoomTo(14.5f));

        //mMap.moveCamera(CameraUpdateFactory.newLatLng(destinationPosition));
        //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(destinationPosition, 13.0f));
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
    }

    public void zoomRoute(List<LatLng> route) {

        if (mMap == null || route == null || route.isEmpty()) return;

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng latLngPoint : route)
            boundsBuilder.include(latLngPoint);

        //int routePadding = 500;
        LatLngBounds latLngBounds = boundsBuilder.build();


        CameraUpdate cu= CameraUpdateFactory.newLatLngBounds(latLngBounds,width,height,200);

        mMap.animateCamera(cu);

       /* mMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(latLngBounds, routePadding),
                600,
                null
        );*/
    }


    private String getDirectionsUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;


        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&key=" + this.getResources().getString(R.string.google_api_key);

        // Output format
        String output = "json";

        // Building the url to the web service
        Log.d("finalUrl", "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters);

        return "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();
            Log.d("downloadUrl", data.toString());
            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    // Fetches data from url passed
    private class FetchUrl extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
                Log.d("Background Task data", data.toString());
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);

        }
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                Log.d("ParserTask", jsonData[0].toString());
                DirectionsJSONParser parser = new DirectionsJSONParser();
                Log.d("ParserTask", parser.toString());

                // Starts parsing data
                routes = parser.parse(jObject);
                Log.d("ParserTask", "Executing routes");
                Log.d("ParserTask", routes.toString());

            } catch (Exception e) {
                Log.d("ParserTask", e.toString());
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {


            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;

            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(20);
                lineOptions.color(Color.RED);
                lineOptions.pattern(PATTERN_DOTTED);

                Log.d("onPostExecute", "onPostExecute lineoptions decoded");
            }

            // Drawing polyline in the Google Map for the i-th route
            if (lineOptions != null) {

                if (polyline != null)
                    polyline.remove();
                    polyline = mMap.addPolyline(lineOptions);
                zoomRoute(points);

            } else {
                Log.d("onPostExecute", "without Polylines drawn");
            }
        }
    }

    private void setUpRecyclerView() {

        recyclerViewPlaces = (RecyclerView)view.findViewById(R.id.placeRec);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerViewPlaces.setLayoutManager(layoutManager);
        recyclerViewPlaces.setHasFixedSize(true);
        recyclerViewPlaces.setItemAnimator(new DefaultItemAnimator());

        /**
         * Default Center
         */

        final SnapHelper snapHelperCenterDefault = new LinearSnapHelper();
        snapHelperCenterDefault.attachToRecyclerView(recyclerViewPlaces);

        final PlaceListAdapter placeRecyclerViewAdapter = new PlaceListAdapter(getActivity(), myPlaces, lat, lng);
        recyclerViewPlaces.setAdapter(placeRecyclerViewAdapter);

        recyclerViewPlaces.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                View v = snapHelperCenterDefault.findSnapView(layoutManager);
                int pos = layoutManager.getPosition(v);

                showPlaces(myPlaces.getResults().get(pos),pos);
                LatLng destinationPosition = new LatLng(Double.valueOf(myPlaces.getResults().get(pos).getGeometry().getLocation().getLat()), Double.valueOf(myPlaces.getResults().get(pos).getGeometry().getLocation().getLng()));
                showDistance(currentPosition,destinationPosition);

               /* RecyclerView.ViewHolder viewHolder = recyclerViewPlaces.findViewHolderForAdapterPosition(pos);
                RelativeLayout rl1 = viewHolder.itemView.findViewById(R.id.rl1);*/

               /* if (newState == RecyclerView.SCROLL_STATE_IDLE){
                    rl1.animate().setDuration(350).scaleX(1).scaleY(1).setInterpolator(new AccelerateInterpolator()).start();
                }else{
                    rl1.animate().setDuration(350).scaleX(0.75f).scaleY(0.75f).setInterpolator(new AccelerateInterpolator()).start();
                }*/

            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                final LatLng markerPosition = marker.getPosition();
                int selected_marker = -1;
                for (int i = 0; i < myPlaces.getResults().size(); i++) {
                    if (markerPosition.latitude == Double.parseDouble(myPlaces.getResults().get(i).getGeometry().getLocation().getLat()) && markerPosition.longitude == Double.parseDouble(myPlaces.getResults().get(i).getGeometry().getLocation().getLng())) {
                        selected_marker = i;
                        break;
                    }
                }
                CameraPosition cameraPosition = new CameraPosition.Builder().target(markerPosition).zoom(12).build();
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                placeRecyclerViewAdapter.notifyDataSetChanged();
                recyclerViewPlaces.smoothScrollToPosition(selected_marker);

                marker.showInfoWindow();

                return false;

            }

        });

    }

   /* private void calculateDirections(Marker marker){
        Log.d("Place List", "calculateDirections: calculating directions.");

        LatLng destination = new LatLng(
                marker.getPosition().latitude,
                marker.getPosition().longitude
        );
        DirectionsApiRequest directions = new DirectionsApiRequest(mGeoApiContext);

        directions.alternatives(true);
        directions.origin(
                new com.google.maps.model.LatLng(
                        mUserPosition.getGeo_point().getLatitude(),
                        mUserPosition.getGeo_point().getLongitude()
                )
        );
        Log.d("Place List", "calculateDirections: destination: " + destination.toString());
        directions.destination(destination).setCallback(new PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {
//                Log.d(TAG, "calculateDirections: routes: " + result.routes[0].toString());
//                Log.d(TAG, "calculateDirections: duration: " + result.routes[0].legs[0].duration);
//                Log.d(TAG, "calculateDirections: distance: " + result.routes[0].legs[0].distance);
//                Log.d(TAG, "calculateDirections: geocodedWayPoints: " + result.geocodedWaypoints[0].toString());

                Log.d("Place List", "onResult: successfully retrieved directions.");
                addPolylinesToMap(result);
            }

            @Override
            public void onFailure(Throwable e) {
                Log.e("Place List", "calculateDirections: Failed to get directions: " + e.getMessage() );

            }
        });
    }
*/
}
