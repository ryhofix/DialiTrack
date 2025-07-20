package pl.mr.dialitrack;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

import java.io.InputStream;
public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> pickImagesLauncher;
    private TextView datesView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    }

    private void handleImagesResult(ActivityResult result) {
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
            return;
        }
        Intent data = result.getData();
        StringBuilder builder = new StringBuilder();

        if (data.getClipData() != null) {
            ClipData clip = data.getClipData();
            for (int i = 0; i < clip.getItemCount(); i++) {
                Uri uri = clip.getItemAt(i).getUri();
                builder.append(readDateFromExif(uri)).append("\n");
            }
        } else if (data.getData() != null) {
            builder.append(readDateFromExif(data.getData())).append("\n");
        }

        datesView.setText(builder.toString());
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
}