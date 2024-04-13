package org.hse.base;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.lights.Light;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.Manifest;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import android.app.AlertDialog;
import androidx.annotation.Nullable;
import android.view.View;
import android.widget.Button;







import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor light;
    private TextView sensorLight;
    private static final int REQUEST_PERMISSION_CODE = 2;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String TAG = "NameOfYourClass";
    String PERMISSION = Manifest.permission.CAMERA;
    ImageView image;
    private Bitmap imageBitmap;
    EditText nameEditText;
    private ListView sensorListView;
    private List<String> sensorList;
    public static final String NAME_KEY = "name";
    private static final String TAGSensor = "Sensor";


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorLight = findViewById(R.id.sensorLight);
        image = findViewById(R.id.imageView);
        sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);
        // Проверяем наличие файла и отображаем его, если существует
        File file = new File(getFilesDir(), "avatar.jpg");
        if (file.exists()) {
            imageBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            image.setImageBitmap(imageBitmap);
        }
        Button takePhotoButton = findViewById(R.id.buttonPhoto);
        takePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermission();
            }
        });

        View buttonSave= findViewById(R.id.saveButton);
        // Обработчик кнопки "Сохранено"
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (imageBitmap != null) {
                    saveImageToStorage(imageBitmap);
                    onSaveName();
                } else {
                    String take = getString(R.string.takepicture);
                    Toast.makeText(SettingsActivity.this, take, Toast.LENGTH_SHORT).show();
                }
            }
        });
        loadImage();


        // Получаем доступ к настройкам
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Загружаем сохраненное имя из настроек и применяем к EditText
        nameEditText = findViewById(R.id.name);
        String name = sharedPreferences.getString(NAME_KEY, "");
        nameEditText.setText(name);




        //Вывести в настройках список всех доступных датчиков
        sensorListView = findViewById(R.id.listView);
        sensorList = new ArrayList<>();

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        List<Sensor> deviceSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);

        for (Sensor sensor : deviceSensors) {
            sensorList.add(sensor.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sensorList);
        sensorListView.setAdapter(adapter);

    }



    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy){
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            String accUnreliable = getString(R.string.accuracyUnreliable);
            String acc = getString(R.string.accuracy);
            if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
                Log.d(TAGSensor, accUnreliable);
            } else {
                Log.d(TAGSensor, acc + accuracy);
            }
        }
    }


        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
                float lux = event.values[0];
                String sens = getString(R.string.sensor);
                sensorLight.setText(sens);
            }
        }


    @Override
    protected void onResume(){
        super.onResume();
        sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause(){
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    public void checkPermission(){
        int permissionCheck = ActivityCompat.checkSelfPermission(this, PERMISSION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSION)){
                String explanationTitle = getString(R.string.rules);
                String explanationMessage = getString(R.string.rulesforphoto);
                showExplanation(explanationTitle, explanationMessage, PERMISSION, REQUEST_PERMISSION_CODE);
            }
            else{
                requestPermissions(new String[]{PERMISSION}, REQUEST_PERMISSION_CODE);
            }
        } else{
            dispatchTakePictureIntent();
        }
    }

    private void dispatchTakePictureIntent(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null){
            File photoFile = null;
            try{
                photoFile = createImageFile();
            }catch(IOException ex){
                String create = getString(R.string.createfile);
                Log.e(TAG, create, ex);
            }
            if (photoFile != null){
                Uri photoURI = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                try{
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }  catch (ActivityNotFoundException e){
                    String activity = getString(R.string.startactivity);
                    Log.e(TAG, activity, e);
                }
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "avatar";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName + "_" + timeStamp, ".jpg", storageDir);
        String imagePath = image.getAbsolutePath();

        // Сохраняем путь к файлу для дальнейшего использования
        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
        editor.putString("imagePath", imagePath);
        editor.apply();

        return image;
    }

    private void showExplanation(String title, String message, String permission, int requestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        requestPermissions(new String[]{permission}, requestCode);
                    }
                })
                .create()
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK){
            File file = new File(getPreferences(MODE_PRIVATE).getString("imagePath", ""));
            imageBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());

            image.setImageBitmap(imageBitmap);
            image.setScaleType(ImageView.ScaleType.FIT_XY);
        }
    }

    private void saveImageToStorage(Bitmap bitmap) {
        String filename = "image.jpg";
        FileOutputStream out = null;
        try {
            out = openFileOutput(filename, Context.MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            String saveim = getString(R.string.savesuccess);
            Log.d(TAG, saveim + filename);
        } catch (Exception e) {
            String err = getString(R.string.error);
            Log.e(TAG, err + e.getMessage());
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                String error = getString(R.string.errorclose);
                Log.e(TAG, error + e.getMessage());
            }
        }
    }

    private void loadImage() {
        try {
            FileInputStream fileInputStream = openFileInput("image.jpg");
            imageBitmap = BitmapFactory.decodeStream(fileInputStream);
            image.setImageBitmap(imageBitmap);
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                dispatchTakePictureIntent();
            } else {
                String perm = getString(R.string.perm);
                Toast.makeText(this, perm, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onSaveName() {
        // Сохраняем введенное имя в настройках
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        String name = nameEditText.getText().toString().trim();
        editor.putString(NAME_KEY, name);

        editor.apply();
    }

}


