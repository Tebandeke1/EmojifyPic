package com.tabutech.emojyfypic;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import timber.log.Timber;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 1;

    FloatingActionButton mClearFab,mSaveFab,mShareFab;
    Button mEmojifyButton,galleryButton;
    ImageView mImageView;
    TextView mTitleTextView;

    private boolean checkWhichButtonPressed = false;

    Uri imageUri;

    private static final String FILE_PROVIDER_AUTHORITY = "com.example.android.fileprovider";

    private String mTempPhotoPath;

    private Bitmap mResultsBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
         mClearFab = findViewById(R.id.clear_button);
         mSaveFab = findViewById(R.id.save_button);
         mEmojifyButton = findViewById(R.id.emojify_button);
         mTitleTextView = findViewById(R.id.title_text_view);
         mShareFab = findViewById(R.id.share_button);

         mImageView = findViewById(R.id.image_view);
         galleryButton = findViewById(R.id.emojify_button2);

         //setting on click Listeners
         mEmojifyButton.setOnClickListener(v -> emojifyMe());

         mClearFab.setOnClickListener(v -> clearImage());

         mSaveFab.setOnClickListener(v -> saveMe());

         mShareFab.setOnClickListener(v -> shareMe());

         galleryButton.setOnClickListener(v -> getImageFromGallery());


        // Set up Timber
        timber.log.Timber.plant(new Timber.DebugTree());
    }

    /**
     * onclicking on the getImageFromGallery, then you get image from the Gallery!!
     */

    //TODO COMPLETE THIS METHOD
    private void getImageFromGallery() {
        Intent gallery = new Intent();
        gallery.setType("image/*");
        gallery.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(gallery,"select Picture"),REQUEST_IMAGE_CAPTURE);
        checkWhichButtonPressed = true;
    }

    /**
     * OnClick method for "Emojify Me!" Button. Launches the camera app.
     */

    public void emojifyMe() {
        // Check for the external storage permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // If you do not have permission, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
        } else {
            // Launch the camera if the permission exists
            launchCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // Called when you request permission to read and write to external storage
        switch (requestCode) {
            case REQUEST_STORAGE_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // If you get permission, launch the camera
                    launchCamera();
                } else {
                    // If you do not get permission, show a Toast
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    /**
     * Creates a temporary image file and captures a picture to store in it.
     */
    private void launchCamera() {

        // Create the capture image intent
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the temporary File where the photo should go
            File photoFile = null;
            try {
                photoFile = BitMapUtils.createTempImageFile(this);
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {

                // Get the path of the temporary file
                mTempPhotoPath = photoFile.getAbsolutePath();

                // Get the content URI for the image file
                Uri photoURI = FileProvider.getUriForFile(this,
                        FILE_PROVIDER_AUTHORITY,
                        photoFile);

                // Add the URI so the camera can store the image
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                // Launch the camera activity
               // Toast.makeText(this, "The camera here is not working..", Toast.LENGTH_SHORT).show();
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // If the image capture activity was called and was successful
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            if (checkWhichButtonPressed) {
                processImageFromGallery(data);
            } else{
                // Process the image and set it to the imageView
                processAndSetImage();
             }
        } else {

            // Otherwise, delete the temporary image file
            BitMapUtils.deleteImageFile(this, mTempPhotoPath);
        }
    }


    /**
     * First this method will help in getting Image fileName or path.
     */


    /**
     * Method for processing the images picked from the gallery to the imageView
     */

    private void processImageFromGallery(Intent data){

        // Toggle Visibility of the views
        mEmojifyButton.setVisibility(View.GONE);
        galleryButton.setVisibility(View.GONE);
        mTitleTextView.setVisibility(View.GONE);
        mSaveFab.setVisibility(View.VISIBLE);
        mShareFab.setVisibility(View.VISIBLE);
        mClearFab.setVisibility(View.VISIBLE);

        imageUri = data.getData();

        try {

            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),imageUri);


            mTempPhotoPath = imageUri.getPath();

            // Resample the saved image to fit the ImageView
            mResultsBitmap = BitMapUtils.resamplePic(this, mTempPhotoPath);

            // Detect the faces and overlay the appropriate emoji
            mResultsBitmap = Emojifier.detectFacesAndOverlayEmoji(this, bitmap);

            //set the picked image to the image view
            mImageView.setImageBitmap(mResultsBitmap);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Method for processing the captured image and setting it to the Image View
     */
    private void processAndSetImage() {

        // Toggle Visibility of the views
        mEmojifyButton.setVisibility(View.GONE);
        galleryButton.setVisibility(View.GONE);
        mTitleTextView.setVisibility(View.GONE);
        mSaveFab.setVisibility(View.VISIBLE);
        mShareFab.setVisibility(View.VISIBLE);
        mClearFab.setVisibility(View.VISIBLE);

        // Resample the saved image to fit the ImageView
        mResultsBitmap = BitMapUtils.resamplePic(this, mTempPhotoPath);


        // Detect the faces and overlay the appropriate emoji
        mResultsBitmap = Emojifier.detectFacesAndOverlayEmoji(this, mResultsBitmap);

        // Set the new bitmap to the ImageView
        mImageView.setImageBitmap(mResultsBitmap);
    }


    /**
     * OnClick method for the save button.
     */
    public void saveMe() {
        // Delete the temporary image file
        BitMapUtils.deleteImageFile(this, mTempPhotoPath);

        // Save the image
        BitMapUtils.saveImage(this, mResultsBitmap);
    }

    /**
     * OnClick method for the share button, saves and shares the new bitmap.
     */
    public void shareMe() {
        // Delete the temporary image file
        BitMapUtils.deleteImageFile(this, mTempPhotoPath);

        // Save the image
        BitMapUtils.saveImage(this, mResultsBitmap);

        // Share the image
        BitMapUtils.shareImage(this, mTempPhotoPath);
    }

    /**
     * OnClick for the clear button, resets the app to original state.
     */

    public void clearImage() {
        // Clear the image and toggle the view visibility
        mImageView.setImageResource(0);
        mEmojifyButton.setVisibility(View.VISIBLE);
        galleryButton.setVisibility(View.VISIBLE);
        mTitleTextView.setVisibility(View.VISIBLE);
        mShareFab.setVisibility(View.GONE);
        mSaveFab.setVisibility(View.GONE);
        mClearFab.setVisibility(View.GONE);

        // Delete the temporary image file
        BitMapUtils.deleteImageFile(this, mTempPhotoPath);
    }

}