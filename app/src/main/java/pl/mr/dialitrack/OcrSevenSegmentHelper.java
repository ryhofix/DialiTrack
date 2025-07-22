package pl.mr.dialitrack;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import org.tensorflow.lite.flex.FlexDelegate;


public class OcrSevenSegmentHelper {

    private Interpreter tflite;
    private final int IMG_WIDTH = 64;
    private final int IMG_HEIGHT = 32;

    public OcrSevenSegmentHelper(Context context) throws IOException {
        Interpreter.Options options = new Interpreter.Options();
        options.addDelegate(new FlexDelegate()); // <-- dodaj ten import
        tflite = new Interpreter(loadModelFile(context, "tensorflow/model_float16.tflite"), options);
        int[] shape = tflite.getInputTensor(0).shape();
        Log.d("MODEL_SHAPE", Arrays.toString(shape));
        Log.d("OUTPUT_TYPE", tflite.getOutputTensor(0).dataType().toString());


    }

    private MappedByteBuffer loadModelFile(Context context, String filename) throws IOException {
        FileInputStream fis = new FileInputStream(context.getAssets().openFd(filename).getFileDescriptor());
        FileChannel fileChannel = fis.getChannel();
        long startOffset = context.getAssets().openFd(filename).getStartOffset();
        long declaredLength = context.getAssets().openFd(filename).getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private float[][][][] preprocess(Bitmap bitmap) {
        int height = 31;
        int width = 200;

        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, width, height, false);
        float[][][][] input = new float[1][height][width][1];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = scaled.getPixel(x, y);
                int gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3;
                input[0][y][x][0] = gray / 255.0f;
            }
        }

        return input;
    }


    public String predict(Bitmap bitmap) {
        float[][][][] input = preprocess(bitmap);

        long[][] output = new long[1][1];
        Log.d("PREDICTED_RAW", "Output: " + output[0][0]);


        tflite.run(input, output);

        StringBuilder sb = new StringBuilder();
        for (long digit : output[0]) {
            sb.append(digit);
        }

        return sb.toString();
    }


    private String decode(float[] output) {
        StringBuilder sb = new StringBuilder();
        for (float value : output) {
            int digit = Math.round(value);
            sb.append(digit);
        }
        return sb.toString();
    }
}
