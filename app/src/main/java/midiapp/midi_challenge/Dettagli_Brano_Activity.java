package midiapp.midi_challenge;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.pdrogfer.mididroid.MidiFile;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;


import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.content.Context;
import android.hardware.camera2.*;
import android.hardware.Camera.PictureCallback;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.GET_ACCOUNTS;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

/**
 * @author lucabazzanella
 */
public class Dettagli_Brano_Activity extends AppCompatActivity {
    public Brano brano = null;
    public Utente utente =null;
    private String dir = "MidiChallenge/";
    AlgoritmoMidi alMidi;
    MidiFile midiFile;
    TextView tvLog;
    TextView tvInfo1;

    Camera camera;
    Button brnFotocamera;
    private static final int REQUEST_GET_ACCOUNT = 112;
    private static final int PERMISSION_REQUEST_CODE = 200;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_TAKE_PHOTO = 1;

    private static FunzioniDatabase funzioniDatabase = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dettagli__brano);

        long l = getIntent().getLongExtra("id_brano",0);
        funzioniDatabase = new FunzioniDatabase(getBaseContext());
        brano = funzioniDatabase.trovaBrano(l);

        Button btnBrano = (Button) findViewById(R.id.buttonProvaCaricaBrano);       //BUTTON ELABORA MIDI
        btnBrano.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {

                File sdcard = Environment.getExternalStorageDirectory();         // apro MIDI file  //campanella.mid  Chopin_EtudesOp10n1.mid  happyBD.mid
                File input = new File(brano.getNomeFile());
                try {  midiFile = new MidiFile(input);
                    if(midiFile!=null) {
                        alMidi = new AlgoritmoMidi(midiFile);
                        showResults();
                    }   else{ tvLog.setText("file midi null!"); }
                }
                catch (IOException e) {
                    System.err.println("Error parsing MIDI file:");
                    e.printStackTrace();        //log.setText(e.toString());
                    midiFile=null; return;
                }
            }
        });

        brnFotocamera = (Button)findViewById(R.id.btnFotocamera);
        brnFotocamera.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                shootFoto();
            }
        });

        if(brano!=null) setTitle(brano.getTitolo());
        else setTitle("Dettagli brano: non disponibile!");
        tvLog = (TextView)findViewById(R.id.tvLog);
        TextView txtTitolo = (TextView) findViewById(R.id.txt_TitoloBrano);
        txtTitolo.setText(brano.getTitolo());

        tvInfo1 = (TextView)findViewById(R.id.lbl_Info1);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void caricaBranoUtente(Brano brano, Utente utente){
        this.brano= brano;
        this.utente=utente;
    }

    private void showResults(){
        List<String> out = alMidi.calc();
        Iterator i = out.iterator();
        tvInfo1.setText("Livello di difficoltà brano: "+alMidi.punteggio);
        tvLog.setText("Algoritmo concluso! righe output: "+out.size());
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                Intent i = new Intent(getBaseContext(),MainActivity.class);
                i.putExtra("id_utente",getIntent().getLongExtra("id_utente",-1));   //find a better way to do dis
                NavUtils.navigateUpTo(this,i);
                break;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {  //chiede permessi lettura SD
        switch (requestCode) {
            case 1: { // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) { // permission was granted, yay! Do the contacts-related task you need to do.
                    File sdcard = Environment.getExternalStorageDirectory();         // apro MIDI file
                    //File input = new File(sdcard, dir+"Chopin_EtudesOp10n1.mid"); //campanella.mid  Chopin_EtudesOp10n1.mid  happyBD.mid

                    File input = new File(brano.getNomeFile());
                    try {
                        midiFile = new MidiFile(input);
                        if (midiFile != null) {
                            alMidi = new AlgoritmoMidi(midiFile);
                            showResults();
                        } else {
                            tvLog.setText("file midi null!");
                        }
                    } catch (IOException e) {
                        System.err.println("Error parsing MIDI file:");
                        e.printStackTrace();        //log.setText(e.toString());
                        midiFile = null;
                        return;
                    }
                } else {   // permission denied, boo! Disable the functionality that depends on this permission.
                    Log.println(Log.ASSERT, "Errore lettura", "Permessi lettura SD negati!");
                }
                break;
            }  // other 'case' lines to check for other permissions this app might request
            case PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {

                    boolean locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean cameraAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (locationAccepted && cameraAccepted)
                        Toast.makeText(getApplicationContext(), "Permission Granted, Now you can access location data and camera", Toast.LENGTH_LONG).show();
                    else {
                        Toast.makeText(getApplicationContext(), "Permission Denied, You cannot access location data and camera", Toast.LENGTH_LONG).show();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE)) {
                                showMessageOKCancel("You need to allow access to both the permissions",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                    requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE},
                                                            PERMISSION_REQUEST_CODE);
                                                }
                                            }
                                        });
                                return;
                            }
                        }
                    }
                }
                break;
            }
        }
    }

    @Override
    protected void onPause() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
        super.onPause();
    }

    void shootFoto(){
         int cameraId = 0;
        // do we have a camera?
        if (checkCameraHardware(this)){
            ActivityCompat.requestPermissions(this, new String[]{GET_ACCOUNTS, CAMERA}, REQUEST_GET_ACCOUNT);
            try {
                    /*CameraManager manager = (CameraManager) getBaseContext().getSystemService(Context.CAMERA_SERVICE);
                    String[] cameraIds = manager.getCameraIdList();
                    CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraIds[cameraId]);*/
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    }
            }
            catch (Exception e){
                Log.d("Debug", e.toString());
            }
        }
        else { Log.println(Log.ASSERT,"Foto","No camera found!");  }
    }

    /*private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile(dir);
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.d("Error I/O",ex.toString());
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }*/

    /*private int findFrontFacingCamera() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                Log.d("Debug", "Camera found");
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }*/

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }
    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new android.support.v7.app.AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {     //restituisce l'immagine dopo che è stato chiamato l'intent alla fotocamera
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            ((ImageView)findViewById(R.id.imgViewSpartitoDemo)).setImageBitmap(imageBitmap); //visualizza l'immagine scattata su un imageview demo

            try {
                //String path = Environment.getExternalStorageDirectory().toString();
                String path = GenericMIDIChallengeActivity.cartellaPredefinita.toString();
                String nomeFile = "_"+brano.getTitolo()+"_" +new SimpleDateFormat("yyyyMMdd_HHmmss").format(System.currentTimeMillis())+ ".jpg";
                OutputStream fOut = null;
                File file = new File(path,nomeFile ); // the File to save , append increasing numeric counter to prevent files from getting overwritten.
                fOut = new FileOutputStream(file);

                Bitmap pictureBitmap = (Bitmap) extras.get("data"); // obtaining the Bitmap//Bitmap pictureBitmap = getImageBitmap(myurl); // obtaining the Bitmap
                pictureBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut); // saving the Bitmap to a file compressed as a JPEG with 85% compression rate
                fOut.flush(); // Not really required
                fOut.close(); // do not forget to close the stream
                MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());

                funzioniDatabase.
            }
            catch (Exception ex){
                Toast.makeText(this, "Errore salvataggio immagine!", Toast.LENGTH_LONG).show();
                Log.d("Errore i/o",ex.toString());
            }

            /*try{
                createImageFile("pippo");
                Log.d("I/O FOTO","Foto salvata correttamente!");
            }
            catch(IOException ex){
                Toast.makeText(this, "Errore salvataggio immagine!", Toast.LENGTH_LONG).show();
                Log.d("Errore i/o",ex.toString());
            }*/
        }
    }

    private File createImageFile(String percorso) throws IOException { //Crea un file con la foto
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(System.currentTimeMillis());
        //File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/Camera/");
        File storageDir = new File( GenericMIDIChallengeActivity.cartellaPredefinita.toString());
        if (!storageDir.exists())
            storageDir.mkdirs();
        File image = File.createTempFile(
                timeStamp,                   /* prefix */
                ".jpeg",                     /* suffix */
                storageDir                   /* directory */
        );
        return image;
    }

}
