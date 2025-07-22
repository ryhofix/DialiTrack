package pl.mr.dialitrack;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.opencv.android.OpenCVLoader;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.exifinterface.media.ExifInterface;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.InputStream;
import java.io.IOException;
public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> pickImagesLauncher;
    private TextView datesView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OpenCVLoader.initDebug();
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        datesView = findViewById(R.id.image_dates);
        Button selectButton = findViewById(R.id.select_images);

        pickImagesLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::handleImagesResult);

        selectButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            pickImagesLauncher.launch(intent);
        });

        recognizeTextFromAsset();
    }

    private void handleImagesResult(ActivityResult result) {
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
            return;
        }
        Intent data = result.getData();
        StringBuilder builder = new StringBuilder();

        datesView.setText("");
        if (data.getClipData() != null) {
            ClipData clip = data.getClipData();
            for (int i = 0; i < clip.getItemCount(); i++) {
                Uri uri = clip.getItemAt(i).getUri();
                readWeightAndDate(uri, builder);
            }
        } else if (data.getData() != null) {
            readWeightAndDate(data.getData(), builder);
        }
    }

    private void readWeightAndDate(Uri uri, StringBuilder builder) {
        try {
            InputImage image = InputImage.fromFilePath(this, uri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            recognizer.process(image)
                    .addOnSuccessListener(result -> {
                        String text = result.getText();
                        Pattern pattern = Pattern.compile("(\\d+[.,]\\d+)");
                        Matcher matcher = pattern.matcher(text);
                        String weight = "";
                        if (matcher.find()) {
                            try {
                                double value = Double.parseDouble(matcher.group(1).replace(',', '.'));
                                int grams = (int) Math.round(value * 1000);
                                weight = grams + "g";
                            } catch (NumberFormatException ignored) {
                            }
                        }
                        builder.append(readDateFromExif(uri));
                        if (!weight.isEmpty()) {
                            builder.append(" ").append(weight);
                        }
                        builder.append("\n");
                        datesView.setText(builder.toString());
                    })
                    .addOnFailureListener(e -> {
                        builder.append(readDateFromExif(uri)).append("\n");
                        datesView.setText(builder.toString());
                    });
        } catch (Exception e) {
            builder.append(readDateFromExif(uri)).append("\n");
            datesView.setText(builder.toString());
        }
    }

    private String readDateFromExif(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) return "";
            ExifInterface exif = new ExifInterface(in);
            String date = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);
            if (date == null) {
                date = exif.getAttribute(ExifInterface.TAG_DATETIME);
            }
            return date != null ? date : "";
        } catch (Exception e) {
            return "";
        }
    }

    private void recognizeTextFromAsset() {
        try (InputStream is = getAssets().open("waga.jpg")) {
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            recognizer.process(image)
                    .addOnSuccessListener(result -> Log.d("MainActivity", "Recognized text: " + result.getText()))
                    .addOnFailureListener(e -> Log.e("MainActivity", "Failed to recognize text", e));
        } catch (IOException e) {
            Log.e("MainActivity", "Failed to load asset", e);
        }
    }
}