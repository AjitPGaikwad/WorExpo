package com.ajit.worexpo.adpter;

import android.content.Context;
import android.content.Intent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ajit.worexpo.MainActivity;
import com.ajit.worexpo.MedicineActivity;
import com.ajit.worexpo.PlaceOnMapFragment;
import com.ajit.worexpo.R;
import com.ajit.worexpo.fragment.PlacesOnMapFragment;
import com.ajit.worexpo.model.MyPlaces;
import com.ajit.worexpo.model.Results;
import com.google.android.gms.location.places.Place;


public class PlaceRecyclerViewAdapter extends RecyclerView.Adapter<PlaceRecyclerViewAdapter.ViewHolder> {

    private Context context;
    private MyPlaces myPlaces;
    private double lat, lng;

    public PlaceRecyclerViewAdapter(Context context, MyPlaces myPlaces, double lat, double lng) {
        this.context = context;
        this.myPlaces = myPlaces;
        this.lat = lat;
        this.lng = lng;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_place_single, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Results results = myPlaces.getResults().get(position);
        holder.bind(results);
        //showOnMap(results,lat,lng);
        holder.linearLayoutDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, MedicineActivity.class);
                intent.putExtra("result", results);
                intent.putExtra("lat", lat);
                intent.putExtra("lng", lng);
                context.startActivity(intent);
                showOnMap(results,lat,lng);
            }
        });
    }

    @Override
    public int getItemCount() {
        return myPlaces.getResults().size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView name, address;
        public LinearLayout linearLayoutDetails;
        ImageView placeIV;

        public ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.textViewPlaceName);
            address = view.findViewById(R.id.textViewAddress);
            linearLayoutDetails = view.findViewById(R.id.linearLayoutDetails);
            placeIV = view.findViewById(R.id.placeImageView);
        }

        public void bind(Results results) {
            name.setText(results.getName());
            address.setText(results.getVicinity());
        }
    }

    /*private void showOnMap(Place place){
        FragmentManager fm = ((MainActivity)context)
                .getSupportFragmentManager();

        Bundle bundle=new Bundle();
        bundle.putString("name", (String)place.getName());
        bundle.putString("address", (String)place.getAddress());
        bundle.putDouble("lat", place.getLatLng().latitude);
        bundle.putDouble("lng", place.getLatLng().longitude);

        PlaceOnMapFragment placeFragment = new PlaceOnMapFragment();
        placeFragment.setArguments(bundle);

        fm.beginTransaction().replace(R.id.map_frame, placeFragment).commit();
    } */

    private void showOnMap(Results results, double lat, double lng){
        FragmentManager fm = ((MainActivity)context)
                .getSupportFragmentManager();

        Bundle bundle=new Bundle();
        bundle.putSerializable("result", results);
        bundle.putDouble("lat", lat);
        bundle.putDouble("lng", lng);

        PlacesOnMapFragment placeFragment = new PlacesOnMapFragment();
        placeFragment.setArguments(bundle);

        fm.beginTransaction().replace(R.id.map_frame, placeFragment).commit();
    }
}
