package com.kodatos.randomwallpaper;

import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;
import android.util.Log;

import java.util.List;

public class Utils {

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        Log.d("CalculateInSampleSize", String.valueOf(height)+"    "+String.valueOf(width));
        Log.d("CalculateInSampleSize", String.valueOf(reqHeight)+"    "+String.valueOf(reqWidth));
        if (height > reqHeight || width > reqWidth) {

            int halfHeight = height / 2;
            int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public static Palette.Swatch getUsefulSwatch(Palette palette, Palette.Swatch existingSwatch){
        List<Palette.Swatch> swatchList = palette.getSwatches();
        for (Palette.Swatch ps: swatchList) {
            if(ps.getRgb()!=existingSwatch.getRgb())
                return ps;
        }
        return null;
    }

    public static boolean checkFileType(String path){
        return path.substring(path.lastIndexOf(".") + 1).matches("jpg|png|jpeg");
    }
}
