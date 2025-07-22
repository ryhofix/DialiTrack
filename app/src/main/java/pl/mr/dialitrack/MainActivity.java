package pl.mr.dialitrack;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.exifinterface.media.ExifInterface;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.IOException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.InputStream;

import pl.mr.dialitrack.TesseractOcrHelper;
public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> pickImagesLauncher;
    private TextView datesView;
    private TesseractOcrHelper ocrHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        try {
            ocrHelper = new TesseractOcrHelper(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

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
        try {
            x();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ocrHelper != null) {
            ocrHelper.release();
        }
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
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException("null stream");
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            String text = ocrHelper != null ? ocrHelper.process(bitmap) : "";
            text = fixDigits(text);
            Pattern pattern = Pattern.compile("(\\d+[.,]\\d+)");
            Matcher matcher = pattern.matcher(text);
            String weight = "";
            if (matcher.find()) {
                try {
                    float value = Float.parseFloat(matcher.group(1).replace(',', '.'));
                    int grams = Math.round(value * 1000f);
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
        } catch (Exception e) {
            builder.append(readDateFromExif(uri)).append("\n");
            datesView.setText(builder.toString());
        }
    }

    private String fixDigits(String text) {
        if (text == null) return "";
        return text.replace('e', '2')
                .replace('B', '8')
                .replace('O', '0')
                .replace('o', '0')
                .replace('I', '1')
                .replace('l', '1')
                .replace('S', '5')
                .replace('s', '5');
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

    private void x() throws IOException {
        OcrSevenSegmentHelper ocrHelper = new OcrSevenSegmentHelper(this);
        Bitmap bitmap = loadTestImageFromAssets(this, "tensorflow/waga1.jpg");

        String wynik = ocrHelper.predict(bitmap);
        Log.d("OCR_RESULT", "Rozpoznana waga: " + wynik);
    }

    public Bitmap loadTestImageFromAssets(Context context, String fileName) {
        try (InputStream is = context.getAssets().open(fileName)) {
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}