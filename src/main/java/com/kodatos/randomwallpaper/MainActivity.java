package com.kodatos.randomwallpaper;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.WallpaperManager;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.kodatos.randomwallpaper.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding mainBinding;
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        setSupportActionBar(mainBinding.toolbar);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 412);
        }
        else {
            mainBinding.wallImageView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mainBinding.wallImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    setImageWallOnLoad(1);
                }
            });
        }
        mainBinding.randomizeFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                randomizeWallpaper();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setImageWallOnLoad(int firstTime){
        String directoryPath = sp.getString(getString(R.string.directory_path_key), "N/A");
        int reqWidth = mainBinding.wallImageView.getWidth();
        int reqHeight = mainBinding.wallImageView.getHeight();
        if(!("N/A".equals(directoryPath))){
            mainBinding.pathEditText.setText(directoryPath);
        }
        new ImageLoadTask().execute(reqWidth, reqHeight, firstTime);
    }


    public void randomizeWallpaper(){
        String directoryPath = mainBinding.pathEditText.getText().toString();
        if(TextUtils.isEmpty(directoryPath)){
            Toast.makeText(this, "Please enter a path in the field", Toast.LENGTH_SHORT).show();
            return;
        }
        File directory = new File(directoryPath);
        if(!directory.exists()){
            Toast.makeText(this, "Please enter a valid directory to get images from", Toast.LENGTH_SHORT).show();
            return;
        }
        List<File> files = new ArrayList<>(Arrays.asList(directory.listFiles()));
        List<File> toRemove = new ArrayList<>();
        for(File f : files){
            if(f.isDirectory())
                toRemove.add(f);
            else if(!Utils.checkFileType(f.getPath())){
                toRemove.add(f);
            }
        }
        files.removeAll(toRemove);
        if(files.isEmpty()) {
            Toast.makeText(this, "No Images Found. Enter a different directory", Toast.LENGTH_SHORT).show();
            return;
        }
        int randomIndex = new Random().nextInt(files.size());
        try(InputStream fis = new FileInputStream(files.get(randomIndex))) {
            WallpaperManager.getInstance(this).setStream(fis);
            SharedPreferences.Editor spe = sp.edit();
            spe.putString(getString(R.string.directory_path_key), directoryPath);
            spe.putString(getString(R.string.image_path_key), files.get(randomIndex).getAbsolutePath());
            spe.apply();
            setImageWallOnLoad(0);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==412){
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                setImageWallOnLoad(1);
            }
            else finish();
        }
    }

    private class ImageLoadTask extends AsyncTask<Integer, Void, Bitmap>{

        private int firstTime = 0;
        private Palette palette;
        Palette.Swatch usefulSwatch;
        Palette.Swatch fabUsefulSwatch;

        @Override
        protected Bitmap doInBackground(Integer... params) {
            String imagePath = sp.getString(getString(R.string.image_path_key), "N/A");
            firstTime=params[2];
            if(!("N/A".equals(imagePath))){
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(imagePath, options);
                options.inSampleSize = Utils.calculateInSampleSize(options, params[0], params[1]);
                options.inJustDecodeBounds = false;
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath,options);
                palette = Palette.from(bitmap).generate();
                usefulSwatch = palette.getDominantSwatch();
                fabUsefulSwatch = Utils.getUsefulSwatch(palette, usefulSwatch);
                Log.d("Color palette", String.valueOf(usefulSwatch.getRgb())+String.valueOf(fabUsefulSwatch.getRgb()));
                return bitmap;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {

            if(bitmap!=null && firstTime==0) {
                animatedChange(bitmap);
            }
            else if(bitmap!=null) {
                nonAnimatedChange(bitmap);
            }
        }

        private void animatedChange(Bitmap bitmap){
            imageViewAnimatedChange(bitmap);
            int colorFrom = ((ColorDrawable) mainBinding.constraintLayout.getBackground()).getColor();
            int colorTo = ContextCompat.getColor(MainActivity.this, R.color.colorPrimary);
            int textColor = Color.WHITE;
            if(usefulSwatch!=null) {
                colorTo = usefulSwatch.getRgb();
                textColor = usefulSwatch.getBodyTextColor();
            }
            ValueAnimator valueAnimator1 = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
            valueAnimator1.setDuration(1000);
            valueAnimator1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int color = (int) animation.getAnimatedValue();
                    mainBinding.constraintLayout.setBackgroundColor(color);
                    mainBinding.toolbar.setBackgroundColor(color);
                    getWindow().setStatusBarColor(color);
                }
            });
            int colorFromFAB = mainBinding.randomizeFab.getBackgroundTintList().getDefaultColor();
            int colorToFAB = ContextCompat.getColor(MainActivity.this, R.color.colorAccent);
            int colorToFABIcon = Color.WHITE;
            if(fabUsefulSwatch!=null) {
                colorToFAB = fabUsefulSwatch.getRgb();
                colorToFABIcon = fabUsefulSwatch.getBodyTextColor();
            }
            ValueAnimator valueAnimator2 = ValueAnimator.ofObject(new ArgbEvaluator(), colorFromFAB, colorToFAB);
            valueAnimator2.setDuration(1000);
            valueAnimator2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int colorFab = (int) animation.getAnimatedValue();
                    mainBinding.randomizeFab.setBackgroundTintList(ColorStateList.valueOf(colorFab));
                }
            });
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(valueAnimator1, valueAnimator2);
            animatorSet.start();
            mainBinding.randomizeFab.setRippleColor(Color.argb(Color.alpha(colorToFAB), Color.red(colorToFAB)+10, Color.green(colorToFAB)+10,
                    Color.blue(colorToFAB)+10));
            mainBinding.randomizeFab.getDrawable().mutate().setTint(colorToFABIcon);
            mainBinding.pathEditText.setTextColor(textColor);
            mainBinding.descriptionTV.setTextColor(textColor);
        }

        private void nonAnimatedChange(Bitmap bitmap){
            int colorTo = ContextCompat.getColor(MainActivity.this, R.color.colorPrimary);
            int textColor = Color.WHITE;
            int colorToFAB = ContextCompat.getColor(MainActivity.this, R.color.colorAccent);
            int colorToFABIcon = Color.WHITE;
            if(usefulSwatch!=null) {
                colorTo = usefulSwatch.getRgb();
                textColor = usefulSwatch.getBodyTextColor();
            }
            if(fabUsefulSwatch!=null) {
                colorToFAB = fabUsefulSwatch.getRgb();
                colorToFABIcon = fabUsefulSwatch.getBodyTextColor();
            }
            mainBinding.wallImageView.setImageBitmap(bitmap);
            mainBinding.constraintLayout.setBackgroundColor(colorTo);
            mainBinding.toolbar.setBackgroundColor(colorTo);
            getWindow().setStatusBarColor(colorTo);
            mainBinding.randomizeFab.setBackgroundTintList(ColorStateList.valueOf(colorToFAB));
            mainBinding.randomizeFab.setRippleColor(Color.argb(Color.alpha(colorToFAB), Color.red(colorToFAB)+10, Color.green(colorToFAB)+10,
                    Color.blue(colorToFAB)+10));
            mainBinding.randomizeFab.getDrawable().mutate().setTint(colorToFABIcon);
            mainBinding.pathEditText.setTextColor(textColor);
            mainBinding.descriptionTV.setTextColor(textColor);
        }

        private void imageViewAnimatedChange(final Bitmap new_image) {
            final Animation anim_out = AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.slide_out_right);
            final Animation anim_in  = AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.slide_in_left);
            anim_out.setAnimationListener(new Animation.AnimationListener()
            {
                @Override public void onAnimationStart(Animation animation) {}
                @Override public void onAnimationRepeat(Animation animation) {}
                @Override public void onAnimationEnd(Animation animation)
                {
                    mainBinding.wallImageView.setImageBitmap(new_image);
                    anim_in.setAnimationListener(new Animation.AnimationListener() {
                        @Override public void onAnimationStart(Animation animation) {}
                        @Override public void onAnimationRepeat(Animation animation) {}
                        @Override public void onAnimationEnd(Animation animation) {}
                    });
                    mainBinding.wallImageView.startAnimation(anim_in);
                }
            });
            mainBinding.wallImageView.startAnimation(anim_out);
        }
    }

}
