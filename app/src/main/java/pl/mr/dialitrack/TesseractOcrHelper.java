package pl.mr.dialitrack;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class TesseractOcrHelper {
    private static final String LANG = "eng";
    private final TessBaseAPI tessBaseAPI = new TessBaseAPI();

    public TesseractOcrHelper(Context context) throws IOException {
        File dir = new File(context.getFilesDir(), "tessdata");
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("Could not create tessdata dir");
            }
        }
        File trainedData = new File(dir, LANG + ".traineddata");
        if (!trainedData.exists()) {
            copyTrainedData(context, trainedData);
        }
        tessBaseAPI.init(context.getFilesDir().getAbsolutePath(), LANG);
    }

    private void copyTrainedData(Context context, File outFile) throws IOException {
        try (InputStream in = context.getAssets().open("tessdata/" + LANG + ".traineddata");
             FileOutputStream out = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }

    public String process(Bitmap bitmap) {
        tessBaseAPI.setImage(bitmap);
        String result = tessBaseAPI.getUTF8Text();
        tessBaseAPI.clear();
        return result == null ? "" : result;
    }

    public void release() {
        tessBaseAPI.end();
    }
}
