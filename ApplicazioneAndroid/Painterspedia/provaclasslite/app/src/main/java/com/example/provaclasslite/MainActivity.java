package com.example.provaclasslite;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.text.method.LinkMovementMethod;
import android.text.Html;
import android.widget.Toast;

import com.example.provaclasslite.ml.ConvtflModelResnet50DeepArtistIdentification;

public class MainActivity extends AppCompatActivity {
    private static final int STORAGE_PERMISSION_CODE=1000;
    private static final String TAG="PERMISSION TAG";

    static final int REQUEST_IMAGE_CAPTURE=1;

    static final int REQUEST_TAKE_PHOTO=3;
    static final int SELECT_A_PHOTO=2;

    ImageButton camera;

    ImageButton gallery;

    ImageButton popup;
    ImageButton prova;
    ImageView imageView;
    ImageView imageprova;
    TextView result, confidence;
    TextView infoartista;

    TextView linkwiki;

    private EditText folderNameEt;

    //since I used resnet50 as neural network the input taken into consideration for the image is 224x224 pixels
    int imageSize = 224;
    int  width=224;
    int height=224;

    String currentPhotoPath=null;

    String filename=null;

    Uri uri;
    Uri imageUri;

    private Button save;
    private File photoFile;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //buttons
        camera = findViewById(R.id.buttonFoto);

        popup = findViewById(R.id.popup);

        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //used to rotate image file path
                String fileName="photo";
                File storageDirectory=getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                try {
                    File imageFile = File.createTempFile(fileName, ".jpg", storageDirectory);

                    currentPhotoPath=imageFile.getAbsolutePath();

                    Uri imageUri=FileProvider.getUriForFile(MainActivity.this, "com.example.provaclasslite.fileprovider", imageFile);

                    Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
                    startActivityForResult(intent,1);
                }
                catch(IOException e){
                    e.printStackTrace();

                }

            }



        });

        popup.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if(event.getAction()==MotionEvent.ACTION_DOWN) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                    builder.setTitle("Scopo dell'app:");
                    builder.setMessage("Quest'app ha lo scopo di classificare i dipinti sulla base di 10 artisti. Gli artisti presi in considerazione sono:\n Vincent_van_Gogh,\n Edgar_Degas, \n Pablo_Picasso,\n Pierre-Auguste_Renoir,\n Albrecht_Durer,\n Paul_Gauguin,\n Francisco_Goya,\n Rembrandt,\n Alfred_Sisley,\n Titian.");
                    builder.setCancelable(false);

                    builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }


                    });

                    AlertDialog alert = builder.create();
                    alert.show();

                }

                return false;
            }
        });


    }

   private File createImageFile() throws IOException {

        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile(
                imageFileName,  // prefix
                ".jpg",         // suffix
                storageDir      // directory
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;


    }


   /*
    The default Android camera application returns a non-null intent only when passing back a thumbnail in the returned Intent.
    If you pass EXTRA_OUTPUT with a URI to write to, it will return a null intent and the picture is in the URI that you passed in.
    */


    private void dispatchTakePictureIntent() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePictureIntent, 1);

    }

    public static Bitmap getBitmapFromResources(Resources resources, int resImage) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        options.inDither = false;
        options.inSampleSize = 1;
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        return BitmapFactory.decodeResource(resources, resImage, options);
    }


    //process the image
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {


        if (requestCode == 1 && resultCode == RESULT_OK) {


                Intent intent=new Intent(MainActivity.this,ArtistClassify.class);
                intent.putExtra("image_path",currentPhotoPath);

                startActivity(intent);


            }

        super.onActivityResult(requestCode, resultCode, data);

    }


}

