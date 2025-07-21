package pl.mr.dialitrack;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;

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
        tessBaseAPI.setVariable("tessedit_char_whitelist", "0123456789.");
        tessBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE);
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
        Bitmap processed = preprocess(bitmap);
        tessBaseAPI.setImage(processed);
        String result = tessBaseAPI.getUTF8Text();
        tessBaseAPI.clear();
        return result == null ? "" : result;
    }

    private Bitmap preprocess(Bitmap src) {
        if (src == null) return null;
        Bitmap gray = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(gray);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        c.drawBitmap(src, 0, 0, paint);

        int width = gray.getWidth();
        int height = gray.getHeight();
        int[] pixels = new int[width * height];
        gray.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            int color = pixels[i];
            int r = Color.red(color);
            int g = Color.green(color);
            int b = Color.blue(color);
            int grayVal = (r + g + b) / 3;
            pixels[i] = grayVal > 128 ? Color.WHITE : Color.BLACK;
        }

        Bitmap bw = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bw.setPixels(pixels, 0, width, 0, 0, width, height);
        return bw;
    }

    public void release() {
        tessBaseAPI.end();
    }
}
