package com.rusj.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity {

    final int SELECT_IMAGES = 1;
    final int CAMERA_REQUEST = 2;
    final int MY_CAMERA_PERMISSION_CODE = 100;
    ArrayList<Uri> selectedImagesPaths; // Paths of the image(s) selected by the user.
    boolean imagesSelected = false; // Whether the user selected at least an image or not.
    Bitmap btimage = null;
    Bitmap croppedImage;
    RecyclerView recyclerView;
    private Vector<VideoItem> videoItems = new Vector<VideoItem>();

    private VideoAdapter videoAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 2);
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

        setContentView(R.layout.activity_main);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        videoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/CSgDjZ_Vv8g\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
        videoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/AhP5Tg_BLIk\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
        videoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/maAQygYz70Y\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));

        videoAdapter = new VideoAdapter(videoItems);
        recyclerView.setAdapter(videoAdapter);

    }
    private void updateVideoItemsOnUiThread(final List<VideoItem> newVideoItems) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                videoItems.clear();
                videoItems.addAll(newVideoItems);
                videoAdapter.notifyDataSetChanged();
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    Toast.makeText(getApplicationContext(), "Access to Storage Permission Granted.", Toast.LENGTH_SHORT).show();
                } else {
//                    Toast.makeText(getApplicationContext(), "Access to Storage Permission Denied.", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            case 2: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    Toast.makeText(this, "Camera Permission Granted.", Toast.LENGTH_LONG).show();
                }
                else {
                    Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    public void connectServer(View v) {
        TextView responseText = findViewById(R.id.responseText);
        if (imagesSelected == false) { // This means no image is selected and thus nothing to upload.
            responseText.setText("No Image Selected to Upload. \nSelect Image and Try Again.");
            return;
        }
        try {
            InputImage i_image = InputImage.fromFilePath(getApplicationContext(), selectedImagesPaths.get(0));
            FaceDetectorOptions options =
                    new FaceDetectorOptions.Builder()
                            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                            .setMinFaceSize(0.15f)
                            .enableTracking()
                            .build();
            FaceDetector detector = FaceDetection.getClient(options);
            detector.process(i_image)
                    .addOnSuccessListener(faces -> {
                        if (faces.size() > 0) {
                            Face detectedFace = faces.get(0); // For simplicity, we only consider the first detected face.
                            Rect faceBoundingBox = detectedFace.getBoundingBox();
                            Bitmap image= null;
                            try {
                                image = getBitmapFromUri(this,selectedImagesPaths.get(0));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            Bitmap croppedImage = Bitmap.createBitmap(image, faceBoundingBox.left,
                                    faceBoundingBox.top, faceBoundingBox.width(), faceBoundingBox.height());
                            ImageView imgView = findViewById(R.id.imageView);
                            Uri uri = getImageUri(getApplicationContext(), croppedImage);
                            Log.d("ImageDetails", "URI : " + uri);
                            selectedImagesPaths = new ArrayList<>();
                            selectedImagesPaths.add(uri);
                            imgView.setImageURI(selectedImagesPaths.get(0));

                            responseText.setText("Sending the Files. Please Wait ...");
                            String postUrl = "https://naznin18701069-uq3b5ckh1u6426yu.socketxp.com"+"/predict/";
                            MultipartBody.Builder multipartBodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);

                            for (int i = 0; i < selectedImagesPaths.size(); i++) {
                                byte[] byteArray = null;
                                try {
                                    InputStream inputStream = getContentResolver().openInputStream(selectedImagesPaths.get(i));
                                    ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
                                    int bufferSize = 1024;
                                    byte[] buffer = new byte[bufferSize];

                                    int len = 0;
                                    while ((len = inputStream.read(buffer)) != -1) {
                                        byteBuffer.write(buffer, 0, len);
                                    }
                                    byteArray = byteBuffer.toByteArray();

                                }catch(Exception e) {
                                    responseText.setText("Please Make Sure the Selected File is an Image.");
                                    return;
                                }
                                multipartBodyBuilder.addFormDataPart("image" + i, "input_img.jpg", RequestBody.create(MediaType.parse("image/*jpg"), byteArray));
                            }

                            RequestBody postBodyImage = multipartBodyBuilder.build();
                            postRequest(postUrl, postBodyImage);
                        } else {
                            Toast.makeText(this, "No face detected", Toast.LENGTH_SHORT).show();}
                    })
                    .addOnFailureListener(e -> {Toast.makeText(this, "Error occurred during face detection", Toast.LENGTH_SHORT).show();
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void postRequest(String postUrl, RequestBody postBody) {

        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(300, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(300, TimeUnit.SECONDS).build();;

        Request request = new Request.Builder()
                .url(postUrl)
                .post(postBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                TextView responseText = findViewById(R.id.responseText);

                MainActivity.this.runOnUiThread(() -> responseText.setText(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseData = response.body().string();
                TextView responseText = findViewById(R.id.responseText);
                MainActivity.this.runOnUiThread(() -> responseText.setText("Your estimated age is : "+responseData));
                int value = Integer.parseInt(responseData);
                List<VideoItem> newVideoItems = new ArrayList<>();
                if (value >= 0 && value <= 5) {
                    newVideoItems.add(new VideoItem("<iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/4NkFMkgh0wY\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/AjgD3CvWzS0\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/OTUg_4TvCWY\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/gSqCz4aiqSs\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/sDCQV4RK-vg\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/OTUg_4TvCWY\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    // Add more VideoItem objects for this range KDtLfsNrViA
                } else if (value >= 6 && value <= 10) {
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/kYJt5kWNsbM\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/ammw6GG2AXw\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/KDtLfsNrViA\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/d0LKxL7aFgg\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/FdlLsxR5AE0\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/xzZLdYd78_8\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    // Add more VideoItem objects for this range
                } else if (value >= 11 && value <=21) {
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/EuwMB1Dal-4\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/AMqkz79KrnM\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/InwUQgGhwRU\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/bC0hlK7WGcM\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/0vdPxLfAsqo\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/HTfYv3IEOqM\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/TYPFenJQciw\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/9FqwhW0B3tY\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                }
                else if (value >= 22 && value <= 30) {
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/in9MC2bFttU\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/cdZZpaB2kDM\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/reUZRyXxUs4\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/zIwLWfaAg-8&t=26s\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/mp-yVMiIo0A\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/FAUXP2tsnPg\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/tdUwWOZPn1M\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/78iLznY0ae4\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/mp-yVMiIo0A\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                } else if (value >= 31 && value <= 45) {
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/1-OJX1rwnl8\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/7zC8-06198g\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/Y9i3OIMitRQ\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/JYYsXzt1VDc\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/4cl8X02Xd1I\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/FEkwz0XuHkY\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/KlFXl--H8eM\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/rkZl2gsLUp4\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/E3QpXj_QOqQ\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));

                } else if (value >= 46 && value <= 65) {
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/2s6F97zYJ1Y\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/PyWb6vyqYMg\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/QiYvXKQksgI\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/kKQV9bEcV28\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/YJQSuUZdcV4\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/QiYvXKQksgI\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/kKQV9bEcV28\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/LWiM-LuRe6w\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/FSD5ps9bLQ0\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/KlV0fyDC3Gc\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                }else {
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/Ogce5D2XMZ0\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/AhP5Tg_BLIk\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/9AThycGCakk\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/8ix-_-ZBXH8\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/nkz2woiPbdg\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/maAQygYz70Y\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                    newVideoItems.add(new VideoItem("<iframe width=\"300\" height=\"300\" src=\"https://www.youtube.com/embed/CSgDjZ_Vv8g\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"));
                }
                // Call the method to update the RecyclerView
                updateVideoItemsOnUiThread(newVideoItems);
            }

        });

    }

    public void captureImage(View v) {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
        }
        else {
            Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, CAMERA_REQUEST);
        }
    }

    public void selectImage(View v) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_IMAGES);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == SELECT_IMAGES && resultCode == RESULT_OK && data != null) {
                selectedImagesPaths = new ArrayList<>();
                TextView imgName = findViewById(R.id.imgName);
                ImageView imgView = findViewById(R.id.imageView);
                if (data.getData() != null) {
                    Uri uri = data.getData();
                    Log.d("ImageDetails", "URI : " + uri);
                    selectedImagesPaths.add(uri);
                    imagesSelected = true;
                    imgName.setText("");
                    imgView.setImageURI(selectedImagesPaths.get(0));
                }
            } else if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK && data != null) {
                selectedImagesPaths = new ArrayList<>();
                TextView imgName = findViewById(R.id.imgName);
                ImageView imgView = findViewById(R.id.imageView);
                if (data.getExtras().get("data") != null) {
                    Bitmap photo = (Bitmap) data.getExtras().get("data");
                    Uri uri = getImageUri(getApplicationContext(), photo);
                    Log.d("ImageDetails", "URI : " + uri);
                    selectedImagesPaths.add(uri);
                    imagesSelected = true;
                    imgName.setText("");
                    imgView.setImageURI(selectedImagesPaths.get(0));
                }
            } else {
                Toast.makeText(this, "You haven't Picked any Image.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Something Went Wrong.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }
    public Bitmap getBitmapFromUri(Context context, Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        inputStream.close();
        return bitmap;
    }


    /* public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    //result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }*/

}
