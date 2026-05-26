package com.example.provaclasslite;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import android.text.method.ScrollingMovementMethod;

public class ArtistInformation extends AppCompatActivity {

    ImageView artistImageView, artistImage;
    TextView artistInfoText, audioDurationText, artistNameText, artistMovementsText, artistPeriodText;

    ImageButton playPauseButton, wikiButton, homeButton, backButton;

    Uri imageUri;

    int imageSize = 224;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.artist_information);

        artistInfoText = findViewById(R.id.infoartista);

        artistNameText = findViewById(R.id.nomeartista);
        artistMovementsText = findViewById(R.id.movimenti);
        artistPeriodText = findViewById(R.id.periodo);

        wikiButton = findViewById(R.id.linkwiki);
        homeButton = findViewById(R.id.home);

        backButton = findViewById(R.id.back);

        // it is used to remove the default navigation bar
        View overlay = findViewById(R.id.my_layout2);

        overlay.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
        );

        homeButton.setOnClickListener(v -> {
            startActivity(new Intent(ArtistInformation.this, MainActivity.class));
            finish();
        });

        backButton.setOnClickListener(v -> onBackPressed());

        artistInfoText.setMovementMethod(new ScrollingMovementMethod());

        Intent intent = getIntent();

        String artistInfo = intent.getExtras().getString("infoartista");
        artistInfoText.setText(artistInfo);

        String artistName = intent.getExtras().getString("nomeartista");
        artistNameText.setText(artistName);

        String artistPeriod = intent.getExtras().getString("periodo");
        artistPeriodText.setText(artistPeriod);

        String artistMovements = intent.getExtras().getString("movimenti");
        artistMovementsText.setText(artistMovements);

        String wikiLink = intent.getExtras().getString("link");

        Bundle extras = intent.getExtras();
        byte[] imageBytes = extras.getByteArray("picture");

        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        ImageView artistImageView = (ImageView) findViewById(R.id.artista);
        artistImageView.setImageBitmap(bitmap);

        wikiButton.setOnClickListener(v -> gotoUrl(wikiLink));
    }

    private void gotoUrl(String url) {
        Uri uri = Uri.parse(url);
        startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }
}