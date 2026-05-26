package com.example.provaclasslite;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.example.provaclasslite.ml.ConvtflModelResnet50DeepArtistIdentification;



public class ArtistClassify extends AppCompatActivity {

    ImageView imageView;

    TextView result, confidence;
    TextView infoartista, player_position, player_duration;

    ImageButton play, pause, back, informazioni_artista,bff,bbf;

    SeekBar seekBar;

    Uri imageUri;

    ScheduledExecutorService timer;

    Handler handler=new Handler();
    Runnable runnable;

    String duration;

    int FileDuration;

    int startTime ,finalTime;

    int length;

    String currentPhotoPath=null;


    int imageSize = 224;

    private View decorView;

    private int currentApiVersion;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_informations_artists);

        imageView = findViewById(R.id.imageView);

        confidence = findViewById(R.id.confidence);

        informazioni_artista = findViewById(R.id.informazioni_artista);

        result = findViewById(R.id.result);
        imageView = findViewById(R.id.imageView);

        bff = findViewById(R.id.bff);
        bbf= findViewById(R.id.bbf);

        back = findViewById(R.id.back);

        play = findViewById(R.id.play);
        pause = findViewById(R.id.pause);
        seekBar=findViewById(R.id.seekbar);
        player_position = findViewById(R.id.player_position);
        player_duration = findViewById(R.id.player_duration);

        View overlay=findViewById(R.id.my_layout);

        overlay.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
              |  View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN
        );


        Intent intent = getIntent();

        String imagepath = intent.getStringExtra("image_path");
        String file = new String(imagepath);

        if(file != null) {

            Bitmap image=BitmapFactory.decodeFile(file);

            ExifInterface exif = null;
            try {
                File pictureFile = new File(file);
                exif = new ExifInterface(pictureFile.getAbsolutePath());

            } catch (IOException e) {
                e.printStackTrace();
            }

            int orientation = ExifInterface.ORIENTATION_NORMAL;

            if (exif != null)
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    image = rotateImage(image, 90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    image = rotateImage(image, 180);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    image = rotateImage(image, 270);
                    break;
            }

            image = resizeBitmap(image, 1000);
            imageView.setImageBitmap(image);

            image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
            classifyImage(image);

        }


    }


    public Bitmap resizeBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        double x;

        if (width >= height && width > maxSize) {
            x = width / height;
            width = maxSize;
            height = (int) (maxSize / x);
        } else if (height >= width && height > maxSize) {
            x = height / width;
            height = maxSize;
            width = (int) (maxSize / x);
        }
        return Bitmap.createScaledBitmap(image, width, height, false);
    }


    public void aa(){
        Intent intent = getIntent();

        String imagepath = intent.getStringExtra("image_path");

        String file = new String(imagepath);

        if(file != null) {

            Bitmap image=BitmapFactory.decodeFile(file);

            ExifInterface exif = null;
            try {
                File pictureFile = new File(file);
                exif = new ExifInterface(pictureFile.getAbsolutePath());

            } catch (IOException e) {
                e.printStackTrace();
            }

            int orientation = ExifInterface.ORIENTATION_NORMAL;

            if (exif != null)
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    image = rotateImage(image, 90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    image = rotateImage(image, 180);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    image = rotateImage(image, 270);
                    break;
            }

            int dimension = Math.min(image.getWidth(), image.getHeight());

            //resize image to fit this size and create a square
            image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);
            imageView.setImageBitmap(image);

            image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
            classifyImage(image);
        }

    }


    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }


    public void classifyImage(Bitmap image){
        try {

            ConvtflModelResnet50DeepArtistIdentification model= ConvtflModelResnet50DeepArtistIdentification.newInstance(getApplicationContext());


            //create TensorFlow Lite input tensor [1, 224, 224, 3]
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);

            //allocate ByteBuffer for normalized RGB pixel data
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            //extract pixel values from bitmap and prepare model input
            int[] intValues = new int[imageSize * imageSize];

            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
            int pixel = 0;
            //iterate over each pixel and extract the R, G, and B values. Add those values individually to the byte buffer
            for(int i = 0; i < imageSize; i ++){
                for(int j = 0; j < imageSize; j++){
                    int val = intValues[pixel++]; // RGB
                    // extract RGB channels from pixel using bitwise operations
                    // normalize input to [0, 1] as required by the TFLite model
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 255));
                }
            }

            // load pixel buffer into TensorFlow Lite input tensor
            inputFeature0.loadBuffer(byteBuffer);

            //run inference using the TFLite model
            ConvtflModelResnet50DeepArtistIdentification.Outputs outputs = model.process(inputFeature0);
            //get model output probabilities
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            //convert output tensor to probability array
            float[] confidences = outputFeature0.getFloatArray();

            // artist classes (model labels)
            String[] classes = {"Vincent_van_Gogh", "Edgar_Degas", "Pablo_Picasso", "Pierre-Auguste_Renoir", "Albrecht_DuтХа├кrer", "Paul_Gauguin", "Francisco_Goya", "Rembrandt", "Alfred_Sisley", "Titian",};

            int maxPos = 0;
            float maxConfidence = 0;

            String s = "";

            //find class with highest probability
            for (int i = 0; i < confidences.length; i++) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i];
                    maxPos = i;

                }

            }


            float Conf= maxConfidence * 100;
            String Confidence=String.valueOf(Conf);
            s += String.format(Confidence);

                result.setText(classes[maxPos]);

            confidence.setText(s);

            if (classes[maxPos] == classes[0]) {

                String cc="Vincent van Gogh (Groot-Zundert, Brabante, Paesi Bassi 1853 – Auvers-sur-Oise, Francia 1890) è stato un pittore, disegnatore e incisore attivo negli anni Ottanta dell’Ottocento," +
                        " è considerato uno dei più grande artisti olandesi moderni e il più noto tra i post-impressionisti.\n" +"\n"+

                        "La carriera artistica di Van Gogh iniziò quando si trasferì a Parigi nel 1886,li entrò in contatto con opere degli artisti impressionisti e postimpressionisti." +
                        "La sua vita è stata segnata da disturbi emotivi, avviò l’attività artistica a 27 anni e ci si dedicò totalmente nei successivi dieci, prima della sua prematura scomparsa." +
                        " Nella sua breve vita riuscì a vendere solo poche delle sue opere e venne conclamato solamente anni dopo la morte.\n" +"\n"+

                        "I dipinti di van Gogh sono noti per la loro intensa espressione emotiva, grazie all'utilizzo di colori vivaci e forme distorte per" +
                        " trasmettere i suoi i suoi sentimenti e le sue lotte interiori. " +
                        "Il suo stile contraddistinto dall’impiego di colori puri e pennellate corpose, fu d’esempio per molti artisti nel XX secolo.";


                 MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.immersive_van_gogh_audio_guide );

                runnable= new Runnable() {
                    @Override
                    public void run() {
                        //set progress on seekbar
                        seekBar.setProgress(mediaPlayer.getCurrentPosition());
                        //handler post delay for 0.5 sec
                        handler.postDelayed(this,500);
                    }
                };

                //get duration of mediaplayer
                int duration= mediaPlayer.getDuration();
                //convert millisecond to minute and second
                String sDuration=convertFormat(duration);
                //set duration on text view
                player_duration.setText(sDuration);

                play.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v){
                        //Hide play button
                        play.setVisibility(View.GONE);

                        //show pause button
                        pause.setVisibility(View.VISIBLE);

                        //start mediaplayer
                        mediaPlayer.start();

                        //set max on seekbar
                        seekBar.setMax(mediaPlayer.getDuration());
                        //start handler

                        handler.postDelayed(runnable, 0);

                         }

                });



                pause.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v){
                        //Hide pause button
                        pause.setVisibility(View.GONE);

                        //show start button
                        play.setVisibility(View.VISIBLE);

                        //pause mediaplayer
                        mediaPlayer.pause();

                        //stop handler
                        handler.removeCallbacks(runnable);

                    }

                });




                bff.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //get current position of mediaplayer
                        int currentPosition=mediaPlayer.getCurrentPosition();
                        //get duration of media player
                        int duration=mediaPlayer.getDuration();
                        //check condition
                        if(mediaPlayer.isPlaying() && duration !=currentPosition && currentPosition<duration-5000){
                            //when mediaplayer is playing and duration is not equal to current position
                            //fastforward for 5 sec
                            currentPosition=currentPosition+5000;
                            //set current position on text view
                            player_position.setText(convertFormat(currentPosition));
                            //set progess on seekbar
                            mediaPlayer.seekTo(currentPosition);

                        }

                    }
                });


                bbf.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        //get current position of mediaplayer
                        int currentPosition=mediaPlayer.getCurrentPosition();
                        //get duration of media player
                        //int duration=mediaPlayer.getDuration();
                        //check condition
                        if(mediaPlayer.isPlaying() && currentPosition>5000){
                            //when mediaplayer is playing and current position is greater than 5 sec
                            //rewind for 5 sec
                            currentPosition=currentPosition-5000;
                            //set current position on text view
                            player_position.setText(convertFormat(currentPosition));
                            //set progess on seekbar
                            mediaPlayer.seekTo(currentPosition);

                        }


                    }
                });





                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                        if(fromUser){
                            //when drag the seekbar
                            //set the progress on seekbar
                            mediaPlayer.seekTo(progress);
                        }

                        //set current position o ntext view
                        player_position.setText(convertFormat(mediaPlayer.getCurrentPosition()));


                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });


                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaplayer){
                        //Hide pause button
                        pause.setVisibility(View.GONE);

                        //show play button
                        play.setVisibility(View.VISIBLE);

                        //set mediaplayer to initial position
                        mediaPlayer.seekTo(0);

                    }

                });





                String link="https://it.wikipedia.org/wiki/Vincent_van_Gogh";

                String nomeartista="Vincent van Gogh";

                String periodo="1853-1890";

                String movimenti="Postimpressionismo,\nNeoimpressionismo,\nImpressionismo";

                informazioni_artista.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        if(mediaPlayer.isPlaying()) {
                            pause.setVisibility(View.GONE);
                            play.setVisibility(View.VISIBLE);
                            mediaPlayer.pause();
                        }


                        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.vangoghritratto);

                        bitmap = resizeBitmap(bitmap, 200);

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                        byte[] b = baos.toByteArray();


                        Intent i = new Intent(ArtistClassify.this, ArtistInformation.class);
                        i.putExtra("link", link);
                        i.putExtra("infoartista", cc);
                        i.putExtra("picture", b);

                        i.putExtra("nomeartista", nomeartista);
                        i.putExtra("periodo", periodo);
                        i.putExtra("movimenti", movimenti);


                        startActivity(i);

                    }

                });


                back.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (mediaPlayer != null) {

                            mediaPlayer.stop();
                            handler.removeCallbacks(runnable);
                            mediaPlayer.reset();

                            mediaPlayer.release();

                        }

                        else if (mediaPlayer != null && mediaPlayer.isPlaying()) {

                            mediaPlayer.stop();
                            handler.removeCallbacks(runnable);
                            mediaPlayer.reset();

                            mediaPlayer.release();


                        } else if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                            mediaPlayer.stop();
                            handler.removeCallbacks(runnable);
                            mediaPlayer.reset();
                            mediaPlayer.release();
                        }

                        Intent i = new Intent(ArtistClassify.this, MainActivity.class);

                        finish();



                    }
                });


            }


           else if (classes[maxPos] == classes[1]) {

                String cc="Edgar Degas (Parigi, 1834 – Parigi, 1917)è stato un pittore e scultore francese considerato appartenente agli impressionisti." +
                        "Degas era noto per il suo uso innovativo della prospettiva e del ritaglio nelle sue composizioni.\n" +"\n"+

                        " Spesso raffigurava scene da angolazioni insolite, come se catturasse un'istantanea sincera." +
                        "Il suo uso dei pastelli fu molto influente ed era un maestro di questo mezzo. " +
                        "Lo ha usato per creare opere con un senso di immediatezza e colore intenso." +
                        "Degas era affascinato dal mondo dell'Opera di Parigi e del balletto. Infatti," +
                        " ha creato numerose opere raffiguranti ballerini sia dentro che fuori dal palco.  \n" +"\n"+

                        "Inoltre, Edgar Degas non fu soltanto un grandissimo pittore, ma anche un importante esperto d’arte." +
                        " La sua formazione fu segnata dalla passione per i maestri antichi italiani e per l’arte contemporanea francese dell’epoca, " +
                        "mostrando particolare attenzione per i due maestri Ferdinand Victor Eugène Delacroix, padre del romanticismo francese," +
                        " e Jean-Auguste-Dominique Ingres, il più importante pittore del neoclassicismo francese." + "\n"+"\n"+
                        " Infine, è importante ricordare che Degas non si limitò soltanto a una vasta conoscenza dell’arte occidentale, ma fu anche un grande estimatore dell’arte giapponese.";



                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.edgardegas );


                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                        if(fromUser){

                            mediaPlayer.seekTo(progress);
                        }

                        player_position.setText(convertFormat(mediaPlayer.getCurrentPosition()));

                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });


                runnable= new Runnable() {
                    @Override
                    public void run() {

                        seekBar.setProgress(mediaPlayer.getCurrentPosition());

                        handler.postDelayed(this,500);
                    }
                };

                int duration= mediaPlayer.getDuration();

                String sDuration=convertFormat(duration);

                player_duration.setText(sDuration);

                play.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v){

                        play.setVisibility(View.GONE);

                        pause.setVisibility(View.VISIBLE);

                        mediaPlayer.start();

                        seekBar.setMax(mediaPlayer.getDuration());

                        handler.postDelayed(runnable, 0);

                    }

                });



                pause.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v){

                        pause.setVisibility(View.GONE);

                        play.setVisibility(View.VISIBLE);

                        mediaPlayer.pause();

                        handler.removeCallbacks(runnable);

                    }

                });


                bff.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        int currentPosition=mediaPlayer.getCurrentPosition();

                        int duration=mediaPlayer.getDuration();

                        if(mediaPlayer.isPlaying() && duration !=currentPosition && currentPosition<duration-5000){

                            currentPosition=currentPosition+5000;

                            player_position.setText(convertFormat(currentPosition));

                            mediaPlayer.seekTo(currentPosition);


                        }


                    }
                });


                bbf.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        int currentPosition=mediaPlayer.getCurrentPosition();

                        if(mediaPlayer.isPlaying() && currentPosition>5000){

                            currentPosition=currentPosition-5000;

                            player_position.setText(convertFormat(currentPosition));

                            mediaPlayer.seekTo(currentPosition);


                        }


                    }
                });


                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaplayer){

                        pause.setVisibility(View.GONE);

                        play.setVisibility(View.VISIBLE);

                        mediaPlayer.seekTo(0);

                    }

                });



                String link="https://it.wikipedia.org/wiki/Edgar_Degas";

                String nomeartista="Edgar Degas";

                String periodo="1834-1917";

                String movimenti="Impressionismo,\nRealismo";

                informazioni_artista.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if(mediaPlayer.isPlaying()) {
                            pause.setVisibility(View.GONE);
                            play.setVisibility(View.VISIBLE);
                            mediaPlayer.pause();
                        }


                        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.edgardegasfoto);

                        bitmap = resizeBitmap(bitmap, 200);

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                        byte[] b = baos.toByteArray();


                        Intent i = new Intent(ArtistClassify.this, ArtistInformation.class);
                        i.putExtra("link", link);
                        i.putExtra("infoartista", cc);
                        i.putExtra("picture", b);

                        i.putExtra("nomeartista", nomeartista);
                        i.putExtra("periodo", periodo);
                        i.putExtra("movimenti", movimenti);

                        startActivity(i);


                    }
                });


                back.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {


                        if (mediaPlayer != null) {

                            mediaPlayer.stop();
                            handler.removeCallbacks(runnable);
                            mediaPlayer.reset();


                            mediaPlayer.release();


                        }

                        else if (mediaPlayer != null && mediaPlayer.isPlaying()) {

                            mediaPlayer.stop();
                            handler.removeCallbacks(runnable);
                            mediaPlayer.reset();


                            mediaPlayer.release();


                        } else if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                            mediaPlayer.stop();
                            handler.removeCallbacks(runnable);
                            mediaPlayer.reset();
                            mediaPlayer.release();
                        }

                        Intent i = new Intent(ArtistClassify.this, MainActivity.class);

                        finish();


                    }
                });


            }



            else if (classes[maxPos] == classes[2]) {

                String cc="Pablo Picasso (Malaga 1881 – Mougins 1973) è stato uno degli artisti tra i più influenti e rivoluzionari pittori della storia dell’arte occidentale." +
                        " Fin da giovane, Picasso dimostrò un eccezionale talento nel disegno, tanto che suo padre, un modesto pittore e insegnante all'Accademia di Barcellona, lo coinvolse nella sua opera." +
                        " Questo precoce talento si è manifestato attraverso una varietà di stili e fasi artistiche durante la sua lunga carriera." + "\n"+"\n"+
                        " La produzione artistica del pittore spagnolo è stata particolarmente vasta e complessa. Per quanto possa risultare difficile delineare degli elementi costanti nello stile di un’artista così prolifico" +
                        " ci sono delle componenti che hanno caratterizzato il suo stile: chiarezza ed equilibrio nelle composizioni, forme e volumi solidi, grande senso del colore e potenza espressiva.\n" + "\n" +

                        "Picasso è stato un’artista che non si è mai accontentato di approdare ad uno stile specifico. Imprevedibile nell’animo quanto nella sua arte, " +
                        "l’artista andaluso ha avuto diverse fasi artistiche: dal periodo blu (1901-1904), al periodo rosa (1904-06), la nascita del cubismo grazie al sodalizio" +
                        " con George Braque, per tornare poi ad uno stile pittorico più tradizionale, mentre l’ultima fase è caratterizzata da un’attività molto intensa, " +
                        "rivolta anche a tecniche come la ceramica e la grafica.\n" + "\n" +

                        "Picasso si trasferì giovanissimo a Parigi, dove incontrò altri artisti importanti come André Derain," +
                        " Amedeo Modigliani e Henri Matisse. Collaborò anche con mercanti d'arte influenti come Daniel-Henry Kahnweiler" +
                        " e creò ritratti di molte figure chiave dell'epoca, inclusi ritratti della scrittrice Gertrude Stein.”.\n" + "\n" +

                        "Tra le caratteristiche costanti nel suo stile figurano la chiarezza ed equilibrio nelle composizioni, forme e volumi solidi," +
                        " un grande senso del colore e una potenza espressiva. Tuttavia, ciò che distingue Picasso è la sua capacità di evolversi costantemente" +
                        " e di spingersi oltre i confini dell'arte tradizionale.";



                 MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.picassospeaks);



                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                        if(fromUser){

                            mediaPlayer.seekTo(progress);
                        }


                        player_position.setText(convertFormat(mediaPlayer.getCurrentPosition()));

                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });



                runnable= new Runnable() {
                    @Override
                    public void run() {

                            seekBar.setProgress(mediaPlayer.getCurrentPosition());

                            handler.postDelayed(this, 500);


                    }
                };




                int duration= mediaPlayer.getDuration();

                String sDuration=convertFormat(duration);

                player_duration.setText(sDuration);

                play.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v){

                        play.setVisibility(View.GONE);


                        pause.setVisibility(View.VISIBLE);


                        mediaPlayer.start();



                        seekBar.setMax(mediaPlayer.getDuration());


                        handler.postDelayed(runnable, 0);

                    }

                });



                pause.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v){

                        pause.setVisibility(View.GONE);


                        play.setVisibility(View.VISIBLE);


                        mediaPlayer.pause();

                        handler.removeCallbacks(runnable);

                    }

                });


                bff.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        int currentPosition=mediaPlayer.getCurrentPosition();

                        int duration=mediaPlayer.getDuration();

                        if(mediaPlayer.isPlaying() && duration !=currentPosition && currentPosition<duration-5000){

                            currentPosition=currentPosition+5000;

                            player_position.setText(convertFormat(currentPosition));

                            mediaPlayer.seekTo(currentPosition);


                        }


                    }
                });


                bbf.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {


                        int currentPosition=mediaPlayer.getCurrentPosition();

                        if(mediaPlayer.isPlaying() && currentPosition>5000){

                            currentPosition=currentPosition-5000;

                            player_position.setText(convertFormat(currentPosition));

                            mediaPlayer.seekTo(currentPosition);


                        }



                    }
                });


                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaplayer){

                        pause.setVisibility(View.GONE);


                        play.setVisibility(View.VISIBLE);


                        mediaPlayer.seekTo(0);





                    }

                });

                String link="https://it.wikipedia.org/wiki/Pablo_Picasso";

                String nomeartista="Pablo Picasso";

                String periodo="1881-1973";

                String movimenti="Cubismo,\nSurrealismo";


                informazioni_artista.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {


                        if(mediaPlayer.isPlaying()) {
                            pause.setVisibility(View.GONE);
                            play.setVisibility(View.VISIBLE);
                            mediaPlayer.pause();
                        }


                        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pablopicassofoto);

                        bitmap = resizeBitmap(bitmap, 200);

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                        byte[] b = baos.toByteArray();

                        String drawablepath="";



                        Intent i = new Intent(ArtistClassify.this, ArtistInformation.class);
                        i.putExtra("link", link);
                        i.putExtra("infoartista", cc);


                        i.putExtra("picture", b);

                        i.putExtra("nomeartista", nomeartista);
                        i.putExtra("periodo", periodo);
                        i.putExtra("movimenti", movimenti);

                        startActivity(i);



                    }
                });



                    back.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {


                            if (mediaPlayer != null) {
                                mediaPlayer.stop();
                                handler.removeCallbacks(runnable);
                                mediaPlayer.reset();

                                mediaPlayer.release();

                            }
                            else if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                                mediaPlayer.stop();
                                handler.removeCallbacks(runnable);
                                mediaPlayer.reset();

                                mediaPlayer.release();

                            } else if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                                mediaPlayer.stop();
                                handler.removeCallbacks(runnable);
                                mediaPlayer.reset();
                                mediaPlayer.release();
                            }

                            Intent i = new Intent(ArtistClassify.this, MainActivity.class);

                            finish();


                        }
                    });


            }


            else if (classes[maxPos] == classes[3]) {
                String cc="Pierre-Auguste Renoir (Limoges, 1841 – Cagnes-sur-Mer, 1919) è uno dei più importanti pittori dell’Impressionismo." +
                        " Conosciuto principalmente per la sua arte marcata da una forte joie de vivre (“gioia di vivere”) e per la sua fase aigre" +
                        " (ovvero ispirata alla pittura di Ingres), oggi è considerato uno dei più importanti artisti francesi. " +"\n"+"\n"+
                        "Grazie al suo viaggio in Italia nel 1882, l’artista scopre e studia artisti come Donatello o Raffaello che influiscono e" +
                        " ispirano il suo nuovo modo di dipingere.\n" +"\n"+

                        "Renoir è meglio conosciuto per la sua associazione con il movimento impressionista. L'impressionismo era caratterizzato da " +
                        " un'enfasi sulla cattura degli effetti di luce, colore e atmosfera nelle scene quotidiane." +"\n" +"\n"+
                        "La tecnica pittorica di Renoir era caratterizzata da un tocco morbido e delicato, caratterizzato dall'uso di pennellate piccole" +
                        " e visibili che trasmettevano un senso di luminosità e luce. È celebrato per la sua capacità di catturare l'impressione di un momento nel tempo.\n"+"\n"+

                        "La vita di Renoir fu segnata da lotte personali e problemi di salute, inclusa l'artrite reumatoide. Nonostante le sue" +
                        " condizioni debilitanti, continuò a dipingere con l'aiuto di assistenti.";




                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.pierreaugusterenoir );


                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                        if(fromUser){

                            mediaPlayer.seekTo(progress);
                        }

                        player_position.setText(convertFormat(mediaPlayer.getCurrentPosition()));

                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });


                runnable= new Runnable() {
                    @Override
                    public void run() {

                        seekBar.setProgress(mediaPlayer.getCurrentPosition());

                        handler.postDelayed(this,500);
                    }
                };



                int duration= mediaPlayer.getDuration();

                String sDuration=convertFormat(duration);

                player_duration.setText(sDuration);

                play.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v){

                        play.setVisibility(View.GONE);

                        pause.setVisibility(View.VISIBLE);

                        mediaPlayer.start();

                        seekBar.setMax(mediaPlayer.getDuration());

                        handler.postDelayed(runnable, 0);

                    }

                });

                bff.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        int currentPosition=mediaPlayer.getCurrentPosition();

                        int duration=mediaPlayer.getDuration();

                        if(mediaPlayer.isPlaying() && duration !=currentPosition && currentPosition<duration-5000){

                            currentPosition=currentPosition+5000;

                            player_position.setText(convertFormat(currentPosition));

                            mediaPlayer.seekTo(currentPosition);


                        }


                    }
                });


                bbf.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {


                        int currentPosition=mediaPlayer.getCurrentPosition();

                        if(mediaPlayer.isPlaying() && currentPosition>5000){

                            currentPosition=currentPosition-5000;

                            player_position.setText(convertFormat(currentPosition));

                            mediaPlayer.seekTo(currentPosition);

                        }

                    }
                });



                pause.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v){

                        pause.setVisibility(View.GONE);

                        play.setVisibility(View.VISIBLE);

                        mediaPlayer.pause();

                        handler.removeCallbacks(runnable);

                    }

                });


                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaplayer){

                        pause.setVisibility(View.GONE);

                        play.setVisibility(View.VISIBLE);

                        mediaPlayer.seekTo(0);

                    }

                });


                String link="https://it.wikipedia.org/wiki/Pierre-Auguste_Renoir";

                String nomeartista="Pierre-Auguste_Renoir";

                String periodo="1841-1919";

                String movimenti="Impressionismo";


                informazioni_artista.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if(mediaPlayer.isPlaying()) {
                            pause.setVisibility(View.GONE);
                            play.setVisibility(View.VISIBLE);
                            mediaPlayer.pause();
                        }

                        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.renoirfoto);

                        bitmap = resizeBitmap(bitmap, 200);

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                        byte[] b = baos.toByteArray();

                        Intent i = new Intent(ArtistClassify.this, ArtistInformation.class);
                        i.putExtra("link", link);
                        i.putExtra("infoartista", cc);
                        i.putExtra("picture", b);

                        i.putExtra("nomeartista", nomeartista);
                        i.putExtra("periodo", periodo);
                        i.putExtra("movimenti", movimenti);
                        startActivity(i);


                    }
                });


                back.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {


                        if (mediaPlayer != null) {
                            mediaPlayer.stop();
                            handler.removeCallbacks(runnable);
                            mediaPlayer.reset();

                            mediaPlayer.release();

                        }

                        else if (mediaPlayer != null && mediaPlayer.isPlaying()) {

                            mediaPlayer.stop();
                            handler.removeCallbacks(runnable);
                            mediaPlayer.reset();

                            mediaPlayer.release();

                        } else if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                            mediaPlayer.stop();
                            handler.removeCallbacks(runnable);
                            mediaPlayer.reset();
                            mediaPlayer.release();
                        }

                        Intent i = new Intent(ArtistClassify.this, MainActivity.class);

                        finish();


                    }
                });



            }



            else if (classes[maxPos] == classes[4]) {
                String cc="Albrecht Dürer (1471-1528) è stato un pittore tedesco la cui influenza rimane di notevole portata. " +
                        "I suoi viaggi attraverso l'Europa, compresa l'Italia e l'Olanda, gli diedero un posto di primo piano nella stampa d'arte e nell'incisione. " +"\n"+"\n"+
                        "Famoso come uno dei migliori artisti tra i gran maestri dell'antica stampa, i suoi lavori erano profondamente religiosi e iconici." +
                        " Il soprannome Dürer significa \"artigiano delle porte\", e dà l'appropriato simbolismo a un artista che creò degli accessi a un " +
                        "mondo interiore spesso malinconico, come si evince dai suoi drammatici autoritratti.\n" + "\n" +

                        "Si allontanò dalla professione di orafo scelta dal padre, mostrando di essere una grande promessa nella pittura, già dalla giovane età di quindici anni." +
                        "Studiò sotto la guida di un importante artista di Norimberga, Michael Wolgemut, realizzando alcuni autoritratti già nel 1484." +
                        "Il primo autoritratto dipinto risale al 1493, realizzato a olio, ed è ora esposto al Louvre di Parigi." +
                        "Oltre ai successivi viaggi artistici attraverso l'Europa, Dürer perfezionò la sua giovinezza con viaggi in Germania, Svizzera e Olanda. \n" +"\n"+

                        "Nonostante i miglioramenti ottenuti nella pittura, come un rinascimento individuale, " +
                        "Dürer tornò ai suoi primi amori, la stampa, la xilografia e l'incisione." +"\n"+"\n"+

                        "Le opere di Dürer hanno conosciuto molte riprese nel corso del tempo, in particolare tra il 1570 e il 1630 e successivamente tra il 1870 e il 1945.";


                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.albrechtdurer2 );

                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                        if(fromUser){

                            mediaPlayer.seekTo(progress);
                        }

                        player_position.setText(convertFormat(mediaPlayer.getCurrentPosition()));

                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });



                runnable= new Runnable() {
                    @Override
                    public void run() {

                        seekBar.setProgress(mediaPlayer.getCurrentPosition());

                        handler.postDelayed(this,500);
                    }
                };

                int duration= mediaPlayer.getDuration();

                String sDuration=convertFormat(duration);

                player_duration.setText(sDuration);

                play.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v){

                        play.setVisibility(View.GONE);

                        pause.setVisibility(View.VISIBLE);

                        mediaPlayer.start();

                        seekBar.setMax(mediaPlayer.getDuration());

                        handler.postDelayed(runnable, 0);

                    }

                });



                pause.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v){

                        pause.setVisibility(View.GONE);

                        play.setVisibility(View.VISIBLE);

                        mediaPlayer.pause();

                        handler.removeCallbacks(runnable);

                    }

                });


                bff.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        int currentPosition=mediaPlayer.getCurrentPosition();

                        int duration=mediaPlayer.getDuration();

                        if(mediaPlayer.isPlaying() && duration !=currentPosition && currentPosition<duration-5000){

                            currentPosition=currentPosition+5000;

                            player_position.setText(convertFormat(currentPosition));

                            mediaPlayer.seekTo(currentPosition);


                        }


                    }
                });


                bbf.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {


                        int currentPosition=mediaPlayer.getCurrentPosition();

                        if(mediaPlayer.isPlaying() && currentPosition>5000){

                            currentPosition=currentPosition-5000;

                            player_position.setText(convertFormat(currentPosition));

                            mediaPlayer.seekTo(currentPosition);


                        }


                    }
                });


                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaplayer){

                        pause.setVisibility(View.GONE);

                        play.setVisibility(View.VISIBLE);

                        mediaPlayer.seekTo(0);

                    }

                });




                String link="https://it.wikipedia.org/wiki/Albrecht_D%C3%BCrer";

                String nomeartista="Albrecht Dürer";

                String periodo="1471-1528";

                String movimenti="Gotico,\nRinascimento nordico";



                informazioni_artista.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if(mediaPlayer.isPlaying()) {
                            pause.setVisibility(View.GONE);
                            play.setVisibility(View.VISIBLE);
                            mediaPlayer.pause();
                        }

                        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.durerfoto2);

                        bitmap = resizeBitmap(bitmap, 200);

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                        byte[] b = baos.toByteArray();

                        Intent i = new Intent(ArtistClassify.this, ArtistInformation.class);
                        i.putExtra("link", link);
                        i.putExtra("infoartista", cc);
                        i.putExtra("picture", b);

                        i.putExtra("nomeartista", nomeartista);
                        i.putExtra("periodo", periodo);
                        i.putExtra("movimenti", movimenti);
                        startActivity(i);

                    }
                });



                back.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (mediaPlayer != null) {

                            mediaPlayer.stop();
                            handler.removeCallbacks(runnable);
                            mediaPlayer.reset();

                            mediaPlayer.release();

                        }

                        else if (mediaPlayer != null && mediaPlayer.isPlaying()) {

                            mediaPlayer.stop();
                            handler.removeCallbacks(runnable);
                            mediaPlayer.reset();

                            mediaPlayer.release();

                        } else if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                            mediaPlayer.stop();
                            handler.removeCallbacks(runnable);
                            mediaPlayer.reset();
                            mediaPlayer.release();
                        }

                        Intent i = new Intent(ArtistClassify.this, MainActivity.class);

                        finish();


                    }
                });

            }

            else if (classes[maxPos] == classes[5]) {
                String cc="Paul Gauguin (Parigi, 1848 – Hiva Oa, 1903) è stato un pittore e scultore francese, celebre per la presenza costante nelle sue opere sia di colori puri molto accesi utilizzati in maniera ardita," +
                        " che di scene che ritraggono una natura incontaminata ed esotica, in cui sono collocate spesso figure femminili bretoni e polinesiane. " +
                        "Inoltre, le opere di Gauguin si distinguono per i contorni molto marcati delle figure e per un’aura di mistero e solennità " +
                        "che permea molte delle scene riprodotte.\n" + "\n"+

                        "Gauguin ha speso molti anni della sua vita nella ricerca di un vero ed autentico primitivismo, e questo lo ha spinto in prima istanza" +
                        " a dissociarsi dallo stile di vita urbano tipico di Parigi, preferendo spostarsi in ambienti dapprima rurali (la campagna Bretone) " +
                        "e poi esotici (Polinesia, Haiti), mentre a livello artistico lo ha condotto a rifiutare qualsiasi dettame accademico" +
                        " e a sperimentare con entusiasmo tecniche innovative. \n" + "\n"+

                        "Inizialmente, Gauguin era molto vicino al gruppo degli impressionisti, ben presto, però, sentì l'esigenza di trovare nella sua arte modalità " +
                        "più espressive. Per questo, è definito come “Post-impressionista”, più nello specifico “Sintetista”," +
                        " termine con il quale si definiscono artisti che nelle loro opere coniugano insieme" +
                        " le forme naturali, i sentimenti che i soggetti suscitano nell’artista e la purezza delle linee, delle forme e dei colori." +
                        " Il suo nome è, infine, molto legato alle figure di Vincent van Gogh e di suo fratello Theo," +
                        " con i quali ha vissuto una vicinanza artistica e personale profonda ed impetuosa. ";


                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.paulgauguin );

                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                        if(fromUser){

                            mediaPlayer.seekTo(progress);
                        }

                        player_position.setText(convertFormat(mediaPlayer.getCurrentPosition()));

                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });


                runnable= new Runnable() {
                    @Override
                    public void run() {

                        seekBar.setProgress(mediaPlayer.getCurrentPosition());

                        handler.postDelayed(this,500);
                    }
                };

                int duration= mediaPlayer.getDuration();

                String sDuration=convertFormat(duration);

                player_duration.setText(sDuration);

                play.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v){

                        play.setVisibility(View.GONE);

                        pause.setVisibility(View.VISIBLE);

                        mediaPlayer.start();

                        seekBar.setMax(mediaPlayer.getDuration());

                        handler.postDelayed(runnable, 0);

                    }

                });



                pause.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v){

                        pause.setVisibility(View.GONE);

                        play.setVisibility(View.VISIBLE);

                        mediaPlayer.pause();

                        handler.removeCallbacks(runnable);

                    }

                });


                bff.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        int currentPosition=mediaPlayer.getCurrentPosition();

                        int duration=mediaPlayer.getDuration();

                        if(mediaPlayer.isPlaying() && duration !=currentPosition && currentPosition<duration-5000){

                            currentPosition=currentPosition+5000;

                            player_position.setText(convertFormat(currentPosition));

                            mediaPlayer.seekTo(currentPosition);


                        }


                    }
                });


                bbf.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        int currentPosition=mediaPlayer.getCurrentPosition();

                        if(mediaPlayer.isPlaying() && currentPosition>5000){

                            currentPosition=currentPosition-5000;

                            player_position.setText(convertFormat(currentPosition));

                            mediaPlayer.seekTo(currentPosition);


                        }



                    }
                });


                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaplayer){

                        pause.setVisibility(View.GONE);

                        play.setVisibility(View.VISIBLE);

                        mediaPlayer.seekTo(0);

                    }

                });

                String link="https://it.wikipedia.org/wiki/Paul_Gauguin";

                String nomeartista="Paul Gauguin";

                String periodo="1848-1903";

                String movimenti="Postimpressionismo,\nPrimitivismo,\nSintesismo";


                informazioni_artista.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if(mediaPlayer.isPlaying()) {
                            pause.setVisibility(View.GONE);
                            play.setVisibility(View.VISIBLE);
                            mediaPlayer.pause();
                        }

                        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.paulgauguinfoto2);

                        bitmap = resizeBitmap(bitmap, 200);

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                        byte[] b = baos.toByteArray();

                        Intent i = new Intent(ArtistClassify.this, ArtistInformation.class);
                        i.putExtra("link", link);
                        i.putExtra("infoartista", cc);
                        i.putExtra("picture", b);

                        i.putExtra("nomeartista", nomeartista);
                        i.putExtra("periodo", periodo);
                        i.putExtra("movimenti", movimenti);
                        startActivity(i);

                    }
                });



                back.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (mediaPlayer != null) {

                            mediaPlayer.stop();
                            handler.removeCallbacks(runnable);
                            mediaPlayer.reset();

                            mediaPlayer.release();

                        }

                        else if (mediaPlayer != null && mediaPlayer.isPlaying()) {

                            mediaPlayer.stop();
                            handler.removeCallbacks(runnable);
                            mediaPlayer.reset();

                            mediaPlayer.release();

                        } else if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                            mediaPlayer.stop();
                            handler.removeCallbacks(runnable);
                            mediaPlayer.reset();
                            mediaPlayer.release();
                        }

                        Intent i = new Intent(ArtistClassify.this, MainActivity.class);

                        finish();

                    }
                });

            }

            else if (classes[maxPos] == classes[6]) {
                String cc="Importante maestro spagnolo, Francisco Goya (1746-1828) è stato un pittore spagnolo famoso per le sue rappresentazioni grafiche della guerra e" +
                        " l'espressione dei tumulti interiori. Il suo apprendistato come pittore iniziò quando aveva 14 anni. " +
                        "Fu un inizio difficoltoso per il giovane pittore spagnolo, in conflitto con il suo insegnante" +
                        " e per la bocciatura alla sua domanda di ammissione all'Accademia reale di Belle Arti di San Fernando. \n" + "\n" +

                        "Francisco Goya non venne influenzato dal suo viaggio artistico, ma trovò qualche apprezzamento in Italia, " +
                        "e tornò presto a lavorare in Spagna. Iniziò a studiare a Saragozza con Francisco Bayeu y Subias (1734 – 1795)," +
                        " di cui avrebbe sposato la sorella Josefa, ed entrò rapidamente all'Accademia Reale di Madrid." +
                        " Goya fu probabilmente il pittore preferito dalla corte reale: fu pittore di corte sotto re Carlo III (1716 – 1788), " +
                        "suo figlio Carlo IV (1748 – 1819) e per il nipote re Ferdinando (1784 – 1833). \n" + "\n" +

                        "Goya è stato influenzato sia dall'Illuminismo, con il suo razionalismo e il suo interesse per la realtà quotidiana, " +
                        "che dal Romanticismo, con la sua enfasi sul sentimento, il sogno e l'irrazionalità. Questo dualismo si riflette nelle sue opere," +
                        " che spaziano dalla rappresentazione accurata della vita quotidiana alle visioni oscure e fantasiose. \n" + "\n"+

                        "Come molti contemporanei, fu influenzato dalla Rivoluzione francese." +
                        " Nel 1792, l'artista divenne sordo e si isolò, dopodiché il suo lavoro " +
                        "divenne progressivamente più cupo e pessimista. I suoi successivi dipinti sembrano riflettere una visione desolante dell'esistenza." + "\n" +"\n"+

                        "Nel 1810 Goya realizzò una serie di incisioni, pubblicate postume, che mostravano gli orrori della guerra. Le opere raffigurano vari episodi di barbarie (uccisioni, massacri, stupri) ambientati durante il periodo della guerra d'indipendenza spagnola. ";

                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.franciscogoya );

                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                        if(fromUser){

                            mediaPlayer.seekTo(progress);
                        }

                        player_position.setText(convertFormat(mediaPlayer.getCurrentPosition()));

                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });


                runnable= new Runnable() {
                    @Override
                    public void run() {

                        seekBar.setProgress(mediaPlayer.getCurrentPosition());

                        handler.postDelayed(this,500);
                    }
                };

                int duration= mediaPlayer.getDuration();

                String sDuration=convertFormat(duration);

                player_duration.setText(sDuration);

                play.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v){

                        play.setVisibility(View.GONE);

                        pause.setVisibility(View.VISIBLE);

                        mediaPlayer.start();

                        seekBar.setMax(mediaPlayer.getDuration());

                        handler.postDelayed(runnable, 0);

                    }

                });



                pause.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v){

                        pause.setVisibility(View.GONE);

                        play.setVisibility(View.VISIBLE);

                        mediaPlayer.pause();

                        handler.removeCallbacks(runnable);

                    }

                });

                bff.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        int currentPosition=mediaPlayer.getCurrentPosition();

                        int duration=mediaPlayer.getDuration();

                        if(mediaPlayer.isPlaying() && duration !=currentPosition && currentPosition<duration-5000){

                            currentPosition=currentPosition+5000;

                            player_position.setText(convertFormat(currentPosition));

                            mediaPlayer.seekTo(currentPosition);


                        }


                    }
                });


                bbf.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        int currentPosition=mediaPlayer.getCurrentPosition();

                        if(mediaPlayer.isPlaying() && currentPosition>5000){

                            currentPosition=currentPosition-5000;

                            player_position.setText(convertFormat(currentPosition));

                            mediaPlayer.seekTo(currentPosition);


                        }



                    }
                });


                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaplayer){

                        pause.setVisibility(View.GONE);

                        play.setVisibility(View.VISIBLE);

                        mediaPlayer.seekTo(0);

                    }

                });

                String link="https://it.wikipedia.org/wiki/Francisco_Goya";

                String nomeartista="Francisco Goya";

                String periodo="1746-1828";

                String movimenti="Romanticismo,\nRococò";

                informazioni_artista.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if(mediaPlayer.isPlaying()) {
                            pause.setVisibility(View.GONE);
                            play.setVisibility(View.VISIBLE);
                            mediaPlayer.pause();
                        }

                        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.franciscogoyafoto);

                        bitmap = resizeBitmap(bitmap, 200);

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                        byte[] b = baos.toByteArray();

                        Intent i = new Intent(ArtistClassify.this, ArtistInformation.class);
                        i.putExtra("link", link);
                        i.putExtra("infoartista", cc);
                        i.putExtra("picture", b);

                        i.putExtra("nomeartista", nomeartista);
                        i.putExtra("periodo", periodo);
                        i.putExtra("movimenti", movimenti);
                        startActivity(i);

                    }
                });



                back.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (mediaPlayer != null) {

                            mediaPlayer.stop();
                            handler.removeCallbacks(runnable);
                            mediaPlayer.reset();

                            mediaPlayer.release();

                        }

                        else if (mediaPlayer != null && mediaPlayer.isPlaying()) {

                            mediaPlayer.stop();
                            handler.removeCallbacks(runnable);
                            mediaPlayer.reset();

                            mediaPlayer.release();

                        } else if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                            mediaPlayer.stop();
                            handler.removeCallbacks(runnable);
                            mediaPlayer.reset();
                            mediaPlayer.release();
                        }

                        Intent i = new Intent(ArtistClassify.this, MainActivity.class);

                        finish();

                    }
                });


            }

            else if (classes[maxPos] == classes[7]) {
                String cc="Rembrandt (1606-1669) fu uno dei più importanti artisti del secolo d'oro olandese e della pittura." +
                        "Rembrandt è nato nel 1606 a Leida, nei Paesi Bassi, ed è cresciuto in una famiglia benestante. Iniziò i suoi studi artistici " +
                        "con Pieter Lastman ad Amsterdam, dove imparò l'arte della pittura storica e mitologica." +
                        " Successivamente, aprì uno studio a Leida con il collega Jan Lievens e presto ebbe anche dei giovani studenti, tra cui Gerard Dou. \n " + "\n"+

                        "Nei primi anni della sua carriera, Rembrandt si specializzò in dipinti religiosi e allegorici di dimensioni relativamente piccole, " +
                        "influenzati dal suo maestro Pieter Lastman. " +
                        "Le sue opere di questo periodo erano ricche di dettagli e riflettevano il suo impegno nel mettere in pratica le tecniche apprese durante" +
                        " la sua formazione.\n" + "\n" +

                        "Ad Amsterdam sposò Saskia van Uylenburg (1612-1642), cugina di Hendrick van Uylenburgh (1587-1661)," +
                        "mercante d'arte che ospitò Rembrandt e lo aiutò incoraggiando la sua carriera artistica. " +
                        "Saskia fece da modella per alcuni lavori del marito e fu nello studio che Hendrick fece in modo che Rembrandt" +
                        "realizzasse la Lezione di anatomia del dottor Tulp, tra gli altri. Nei tardi anni Trenta del Seicento," +
                        "Rembrandt iniziò a eseguire incisioni di paesaggi e meno dipinti, e divenne più lunatico, a causa delle riflessioni personali " +
                        "a cui lo portò la malattia della moglie. L'artista continuò a dipingere commoventi ritratti della donna mentre si trovava nel letto di morte," +
                        "nel quale spirò nel 1642. La grande eccezione in questo periodo è il suo capolavoro, " +
                        "La ronda di notte, titolo improprio per la vivace scena diurna, ma l'opera subì alcune opacizzazioni sulla superficie," +
                        " che le donarono l'aspetto di una scena notturna. \n" + "\n" +
                        " Negli anni '50 del XVII secolo, il suo stile subì un notevole cambiamento. I suoi dipinti divennero più grandi e le colorazioni più ricche. " +
                        "In questo periodo, Rembrandt si allontanò dalle scene di gruppo e si concentrò sull'intimità dei ritratti. La sua tecnica di pittura si fece più pronunciata," +
                        " con colpi di pennello audaci che aggiunsero profondità e drammaticità alle opere. ";


                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.rembrandt );

                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                        if(fromUser){

                            mediaPlayer.seekTo(progress);
                        }

                        player_position.setText(convertFormat(mediaPlayer.getCurrentPosition()));

                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });


                runnable= new Runnable() {
                    @Override
                    public void run() {

                        seekBar.setProgress(mediaPlayer.getCurrentPosition());

                        handler.postDelayed(this,500);
                    }
                };

                int duration= mediaPlayer.getDuration();

                String sDuration=convertFormat(duration);

                player_duration.setText(sDuration);

                play.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v){

                        play.setVisibility(View.GONE);

                        pause.setVisibility(View.VISIBLE);

                        mediaPlayer.start();

                        seekBar.setMax(mediaPlayer.getDuration());

                        handler.postDelayed(runnable, 0);

                    }

                });



                pause.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v){

                        pause.setVisibility(View.GONE);

                        play.setVisibility(View.VISIBLE);

                        mediaPlayer.pause();

                        handler.removeCallbacks(runnable);

                    }

                });

                bff.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        int currentPosition=mediaPlayer.getCurrentPosition();

                        int duration=mediaPlayer.getDuration();

                        if(mediaPlayer.isPlaying() && duration !=currentPosition && currentPosition<duration-5000){

                            currentPosition=currentPosition+5000;

                            player_position.setText(convertFormat(currentPosition));

                            mediaPlayer.seekTo(currentPosition);


                        }


                    }
                });


                bbf.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        int currentPosition=mediaPlayer.getCurrentPosition();

                        if(mediaPlayer.isPlaying() && currentPosition>5000){

                            currentPosition=currentPosition-5000;

                            player_position.setText(convertFormat(currentPosition));

                            mediaPlayer.seekTo(currentPosition);


                        }



                    }
                });


                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaplayer){

                        pause.setVisibility(View.GONE);

                        play.setVisibility(View.VISIBLE);

                        mediaPlayer.seekTo(0);

                    }

                });

                String link="https://it.wikipedia.org/wiki/Rembrandt";

                String nomeartista="Rembrandt";

                String periodo="1606-1669";

                String movimenti="Barocco";


                informazioni_artista.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if(mediaPlayer.isPlaying()) {
                            pause.setVisibility(View.GONE);
                            play.setVisibility(View.VISIBLE);
                            mediaPlayer.pause();
                        }

                        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.remembrandtfoto2);

                        bitmap = resizeBitmap(bitmap, 200);

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                        byte[] b = baos.toByteArray();

                        Intent i = new Intent(ArtistClassify.this, ArtistInformation.class);
                        i.putExtra("link", link);
                        i.putExtra("infoartista", cc);
                        i.putExtra("picture", b);

                        i.putExtra("nomeartista", nomeartista);
                        i.putExtra("periodo", periodo);
                        i.putExtra("movimenti", movimenti);
                        startActivity(i);

                    }
                });



                back.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (mediaPlayer != null) {

                            mediaPlayer.stop();
                            handler.removeCallbacks(runnable);
                            mediaPlayer.reset();

                            mediaPlayer.release();

                        }

                        else if (mediaPlayer != null && mediaPlayer.isPlaying()) {

                            mediaPlayer.stop();
                            handler.removeCallbacks(runnable);
                            mediaPlayer.reset();

                            mediaPlayer.release();

                        } else if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                            mediaPlayer.stop();
                            handler.removeCallbacks(runnable);
                            mediaPlayer.reset();
                            mediaPlayer.release();
                        }

                        Intent i = new Intent(ArtistClassify.this, MainActivity.class);

                        finish();

                    }
                });



            }


            else if (classes[maxPos] == classes[8]) {
                String cc="Alfred Sisley (Parigi, 1839 – Moret-sur-Loing, 1899) è stato uno dei maggiori pittori impressionisti." +
                       "Nacque a Parigi, in Francia, nel 1839 da genitori britannici. Ha mantenuto la cittadinanza britannica per tutta la vita," +
                        " ma ha trascorso gran parte della sua vita in Francia.\n"+ "\n"+

                        "Sisley si concentrò sulla pittura paesagistica. Dipinse diverse scene della campagna francese," +
                        " in particolare lungo le rive della Senna. Le sue opere spesso presentavano scene di fiumi, prati e piccole città, " +
                        "e aveva un occhio attento per gli effetti in continua evoluzione della luce sulla natura.\n"+ "\n"+
                        "I contributi di Sisley al movimento impressionista si basano sulla sensibilità verso la natura, " +
                        "la loro capacità di catturare le qualità in continua evoluzione della luce e il loro ruolo nell'espandere" +
                        " i confini della pittura di paesaggio tradizionale.";


                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.alfredsisley );

                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                        if(fromUser){

                            mediaPlayer.seekTo(progress);
                        }

                        player_position.setText(convertFormat(mediaPlayer.getCurrentPosition()));

                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });


                runnable= new Runnable() {
                    @Override
                    public void run() {

                        seekBar.setProgress(mediaPlayer.getCurrentPosition());

                        handler.postDelayed(this,500);
                    }
                };

                int duration= mediaPlayer.getDuration();

                String sDuration=convertFormat(duration);

                player_duration.setText(sDuration);

                play.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v){

                        play.setVisibility(View.GONE);

                        pause.setVisibility(View.VISIBLE);

                        mediaPlayer.start();

                        seekBar.setMax(mediaPlayer.getDuration());

                        handler.postDelayed(runnable, 0);

                    }

                });


                pause.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v){

                        pause.setVisibility(View.GONE);

                        play.setVisibility(View.VISIBLE);

                        mediaPlayer.pause();

                        handler.removeCallbacks(runnable);

                    }

                });


                bff.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        int currentPosition=mediaPlayer.getCurrentPosition();

                        int duration=mediaPlayer.getDuration();

                        if(mediaPlayer.isPlaying() && duration !=currentPosition && currentPosition<duration-5000){

                            currentPosition=currentPosition+5000;

                            player_position.setText(convertFormat(currentPosition));

                            mediaPlayer.seekTo(currentPosition);

                        }

                    }
                });


                bbf.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        int currentPosition=mediaPlayer.getCurrentPosition();

                        if(mediaPlayer.isPlaying() && currentPosition>5000){

                            currentPosition=currentPosition-5000;

                            player_position.setText(convertFormat(currentPosition));

                            mediaPlayer.seekTo(currentPosition);

                        }

                    }
                });


                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaplayer){

                        pause.setVisibility(View.GONE);

                        play.setVisibility(View.VISIBLE);

                        mediaPlayer.seekTo(0);

                    }

                });


                String link="https://it.wikipedia.org/wiki/Alfred_Sisley";

                String nomeartista="Alfred Sisley";

                String periodo="1839-1899";

                String movimenti="Impressionismo";

                informazioni_artista.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if(mediaPlayer.isPlaying()) {
                            pause.setVisibility(View.GONE);
                            play.setVisibility(View.VISIBLE);
                            mediaPlayer.pause();
                        }

                        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.alfred_sisley_foto);

                        bitmap = resizeBitmap(bitmap, 200);

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                        byte[] b = baos.toByteArray();

                        Intent i = new Intent(ArtistClassify.this, ArtistInformation.class);
                        i.putExtra("link", link);
                        i.putExtra("infoartista", cc);
                        i.putExtra("picture", b);

                        i.putExtra("nomeartista", nomeartista);
                        i.putExtra("periodo", periodo);
                        i.putExtra("movimenti", movimenti);
                        startActivity(i);

                    }
                });



                back.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {




                        if (mediaPlayer != null) {

                            mediaPlayer.stop();
                            handler.removeCallbacks(runnable);
                            mediaPlayer.reset();

                            mediaPlayer.release();

                        }

                        else if (mediaPlayer != null && mediaPlayer.isPlaying()) {

                            mediaPlayer.stop();
                            handler.removeCallbacks(runnable);
                            mediaPlayer.reset();

                            mediaPlayer.release();

                        } else if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                            mediaPlayer.stop();
                            handler.removeCallbacks(runnable);
                            mediaPlayer.reset();
                            mediaPlayer.release();
                        }

                        Intent i = new Intent(ArtistClassify.this, MainActivity.class);

                        finish();

                    }
                });





            }


            else if (classes[maxPos] == classes[9]) {
                String cc="Tiziano Vecellio (a volte Vecelli), meglio noto semplicemente come Tiziano, fu uno degli artisti più importanti della scuola veneziana durante il Rinascimento, nel XVI secolo." +
                        " Nel corso della sua vita fu noto anche come Da Cadore, dal suo luogo di nascita, Pieve di Cadore. \n" + "\n"+
                        " Tiziano fu il più versatile tra i pittori italiani. Era abile con i ritratti così come con i paesaggi, generi che gli portarono la fama" +
                        ",ma anche con i soggetti mitologici e religiosi.\n" + "\n"+

                        "Ricevette la sua prima formazione artistica a Venezia, prima come allievo di Sebastiano Zuccato e poi come assistente del pittore Gentile Bellini." +
                        "Tiziano è spesso associato al Rinascimento veneziano, caratterizzato da un'attenzione al colore, alla luce e alla consistenza. Ha tratto influenza da contemporanei come Giorgione e Giovanni Bellini." +
                        "Tiziano fu un ritrattista prolifico. I suoi ritratti sono celebrati per la loro profondità psicologica e naturalismo. Divenne il ritrattista preferito dalla nobiltà europea, incluso l'imperatore Carlo V.\n" + "\n"+

                        "Poiché visse una vita eccezionalmente lunga per il tempo, la sua maniera ebbe modo di cambiare drasticamente nei suoi ultimi anni, " +
                        "tanto che alcuni critici vi vedono un artista diverso. Le due parti della sua carriera sono unite dal suo profondo interesse per la colorazione," +
                        " emblematico della scuola veneziana. Le sue ultime opere probabilmente non presentano le stesse tinte vivaci e luminose dei primi lavori," +
                        " ma le loro pennellate larghe e le modulazioni policromatiche e sottili non hanno precedenti nella storia dell'arte occidentale. " +
                        "L'impatto di Tiziano arrivò a tutta la pittura europea, e il suo nome è spesso ricondotto alla fonte di ispirazione di molti artisti.";


                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.titiantizianovecellio );

                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                        if(fromUser){

                            mediaPlayer.seekTo(progress);
                        }

                        player_position.setText(convertFormat(mediaPlayer.getCurrentPosition()));

                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });


                runnable= new Runnable() {
                    @Override
                    public void run() {

                        seekBar.setProgress(mediaPlayer.getCurrentPosition());

                        handler.postDelayed(this,500);
                    }
                };

                int duration= mediaPlayer.getDuration();

                String sDuration=convertFormat(duration);

                player_duration.setText(sDuration);

                play.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v){

                        play.setVisibility(View.GONE);

                        pause.setVisibility(View.VISIBLE);

                        mediaPlayer.start();

                        seekBar.setMax(mediaPlayer.getDuration());

                        handler.postDelayed(runnable, 0);

                    }

                });

                pause.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v){

                        pause.setVisibility(View.GONE);

                        play.setVisibility(View.VISIBLE);

                        mediaPlayer.pause();

                        handler.removeCallbacks(runnable);

                    }

                });


                bff.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        int currentPosition=mediaPlayer.getCurrentPosition();

                        int duration=mediaPlayer.getDuration();

                        if(mediaPlayer.isPlaying() && duration !=currentPosition && currentPosition<duration-5000){

                            currentPosition=currentPosition+5000;

                            player_position.setText(convertFormat(currentPosition));

                            mediaPlayer.seekTo(currentPosition);

                        }

                    }
                });


                bbf.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        int currentPosition=mediaPlayer.getCurrentPosition();

                        if(mediaPlayer.isPlaying() && currentPosition>5000){

                            currentPosition=currentPosition-5000;

                            player_position.setText(convertFormat(currentPosition));

                            mediaPlayer.seekTo(currentPosition);

                        }


                    }
                });


                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaplayer){

                        pause.setVisibility(View.GONE);

                        play.setVisibility(View.VISIBLE);

                        mediaPlayer.seekTo(0);

                    }

                });



                String link="https://it.wikipedia.org/wiki/Tiziano_Vecellio";

                String nomeartista="Titian";

                String periodo="1488/1490-1576";

                String movimenti="Rinascimento";

                informazioni_artista.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if(mediaPlayer.isPlaying()) {
                            pause.setVisibility(View.GONE);
                            play.setVisibility(View.VISIBLE);
                            mediaPlayer.pause();
                        }

                        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.titianfoto);

                        bitmap = resizeBitmap(bitmap, 200);

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                        byte[] b = baos.toByteArray();

                        Intent i = new Intent(ArtistClassify.this, ArtistInformation.class);
                        i.putExtra("link", link);
                        i.putExtra("infoartista", cc);
                        i.putExtra("picture", b);

                        i.putExtra("nomeartista", nomeartista);
                        i.putExtra("periodo", periodo);
                        i.putExtra("movimenti", movimenti);
                        startActivity(i);

                    }


                });



                back.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (mediaPlayer != null) {

                            mediaPlayer.stop();
                            handler.removeCallbacks(runnable);
                            mediaPlayer.reset();

                            mediaPlayer.release();

                        } else if (mediaPlayer != null && mediaPlayer.isPlaying()) {

                                mediaPlayer.stop();
                                handler.removeCallbacks(runnable);
                                mediaPlayer.reset();

                                mediaPlayer.release();

                        } else if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                            mediaPlayer.stop();
                            handler.removeCallbacks(runnable);
                            mediaPlayer.reset();
                            mediaPlayer.release();
                        }

                        Intent i = new Intent(ArtistClassify.this, MainActivity.class);

                        finish();

                    }
                });


            }

            model.close();

        } catch (IOException e) {

            // TODO Handle the exception
            onBackPressed();
        }



    }




    @SuppressLint("DefaultLocale")
    private String convertFormat(int duration){
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(duration),
                TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)));

    }

    private Drawable resize(Drawable draw) {
        Bitmap b = ((BitmapDrawable)draw).getBitmap();
        Bitmap bitmapResized = Bitmap.createScaledBitmap(b, 50, 50, false);
        return new BitmapDrawable(getResources(), bitmapResized);
    }


    //block default back button
    //da errore con mediaplayer
    @Override
    public void onBackPressed() {

        //super.onBackPressed();
        //mediaPlayer.isPlaying();

        //Intent i = new Intent(ArtistClassify.this, MainActivity.class);
        // set the new task and clear flags
        //i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        //startActivity(i);

        //finish();

    }

}





