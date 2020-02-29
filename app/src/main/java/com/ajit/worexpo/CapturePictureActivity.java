package com.ajit.worexpo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class CapturePictureActivity extends AppCompatActivity {
    private static final String TAG = "CapturePicture";
    static final int REQUEST_PICTURE_CAPTURE = 1;
    private ImageView image;
    private String pictureFilePath;
    private String deviceIdentifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_img);

        image = findViewById(R.id.picture);

Button captureButton = findViewById(R.id.capture);
captureButton.setOnClickListener(capture);
if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
    captureButton.setEnabled(false);
}

        findViewById(R.id.save_local).setOnClickListener(saveGallery);
        findViewById(R.id.save_cloud).setOnClickListener(saveCloud);

        getInstallationIdentifier();
    }

    private View.OnClickListener capture = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
                if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
                    sendTakePictureIntent();
                }
        }
    };
    private void sendTakePictureIntent() {

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra( MediaStore.EXTRA_FINISH_ON_COMPLETION, true);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, REQUEST_PICTURE_CAPTURE);

            File pictureFile = null;
            try {
                pictureFile = getPictureFile();
            } catch (IOException ex) {
                Toast.makeText(this,
                        "Photo file can't be created, please try again",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (pictureFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.ajit.android.fileprovider",
                        pictureFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(cameraIntent, REQUEST_PICTURE_CAPTURE);
            }
        }
    }
    private File getPictureFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String pictureFile = "WOREXPO_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(pictureFile,  ".jpg", storageDir);
        pictureFilePath = image.getAbsolutePath();
        return image;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICTURE_CAPTURE && resultCode == RESULT_OK) {
            File imgFile = new File(pictureFilePath);
            if (imgFile.exists()) {
                image.setImageURI(Uri.fromFile(imgFile));
            }
        }
    }
    //save captured picture in gallery
    private View.OnClickListener saveGallery = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            addToGallery();
        }
    };
    private void addToGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(pictureFilePath);
        Uri picUri = Uri.fromFile(f);
        galleryIntent.setData(picUri);
        this.sendBroadcast(galleryIntent);
    }
    //save captured picture on cloud storage
    private View.OnClickListener saveCloud = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            //addToCloudStorage();
        }
    };
   /* private void addToCloudStorage() {
        File f = new File(pictureFilePath);
        Uri picUri = Uri.fromFile(f);
        final String cloudFilePath = deviceIdentifier + picUri.getLastPathSegment();

        FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
        StorageReference storageRef = firebaseStorage.getReference();
        StorageReference uploadeRef = storageRef.child(cloudFilePath);

        uploadeRef.putFile(picUri).addOnFailureListener(new OnFailureListener(){
            public void onFailure(@NonNull Exception exception){
                Log.e(TAG,"Failed to upload picture to cloud storage");
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>(){
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot){
                Toast.makeText(CapturePictureActivity.this,
                        "Image has been uploaded to cloud storage",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }*/
    protected synchronized String getInstallationIdentifier() {
        if (deviceIdentifier == null) {
            SharedPreferences sharedPrefs = this.getSharedPreferences(
                    "DEVICE_ID", Context.MODE_PRIVATE);
            deviceIdentifier = sharedPrefs.getString("DEVICE_ID", null);
            if (deviceIdentifier == null) {
                deviceIdentifier = UUID.randomUUID().toString();
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString("DEVICE_ID", deviceIdentifier);
                editor.commit();
            }
        }
        return deviceIdentifier;
    }
}