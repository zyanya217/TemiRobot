package tw.com.temirobot;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;

import com.robotemi.sdk.Robot;
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener;
import com.robotemi.sdk.listeners.OnRobotReadyListener;
import com.robotemi.sdk.navigation.listener.OnCurrentPositionChangedListener;
import com.robotemi.sdk.navigation.model.Position;
import com.robotemi.sdk.navigation.model.SpeedLevel;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Patrol extends AppCompatActivity implements
        OnGoToLocationStatusChangedListener,
        OnCurrentPositionChangedListener,
        OnRobotReadyListener{
    private static final String LOG_TAG = "Patrol";
    private static Robot robot;
    private static final String TAG = "Patrol";
    private static final String TAG2 = "temi_sdk";
    private static final int PERMISSION_CODE = 1001;
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private PreviewView previewView;
    private CameraSelector cameraSelector;
    private ProcessCameraProvider cameraProvider;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private Preview previewUseCase;
    private ImageAnalysis analysisUseCase;

    private boolean flipX = false;
    private boolean start = true;

    private static FirebaseStorage storage;
    private StorageReference mStorageRef;
    private DatabaseReference mDatabase;
    private static final String TAG_f = "Firebase";

    private int timerval = 0;
    private int value2 = 0;
    private TimerTask task = null;
    private Timer timer = null;
    private static final long PERIOD_DAY = 24 * 60 * 60 * 1000;

    //語音文件保存路徑
//    private final String FileName = getExternalFilesDir("").getAbsolutePath();
    //語音操作對象
    private MediaPlayer mPlayer = null;
    private MediaRecorder recorder;

    private MainActivity.Type type1 = MainActivity.Type.AAC_AAC;
    private MainActivity.Type type2 = MainActivity.Type.AAC_M4A;
    private MainActivity.Type type3 = MainActivity.Type.AMR_AMR;

    public static float dbCount = 40;
    private static float lastDbCount = dbCount;
    private static float min = 0.5f;  //設置聲音最低變化
    private static float value = 0;   // 聲音分貝值
    private static final String TAGError = "Recorder";

    private static String place;

    private Calendar calendar= Calendar.getInstance();
    private SimpleDateFormat dateFormat= new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
    private String t = dateFormat.format(calendar.getTime());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        robot = Robot.getInstance();

        mStorageRef = FirebaseStorage.getInstance().getReference();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        storage = FirebaseStorage.getInstance();

        previewView = findViewById(R.id.previewView);
        previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);

        checkPermission();
        startrec();
        Bundle bundle = getIntent().getExtras();
        place = bundle.getString("place");

    }

    @Override
    protected void onStart() {
        super.onStart();
        robot.addOnCurrentPositionChangedListener(this);
        robot.addOnGoToLocationStatusChangedListener(this);
        robot.addOnRobotReadyListener(this);
        timerval = 1;
        robot.goTo(place);
//        mDatabase.child("face").child("temi1").child("checkin").child("py").setValue(false);
//        mDatabase.child("face").child("temi1").child("regis").child("py").setValue(false);
//        mDatabase.child("face").child("temi1").child("welcome").child("py").setValue(false);
        mDatabase.child("face").child("temi1").child("patrol").child("py").setValue(true);
    }

    @Override
    public void onStop()
    {
        super.onStop();
        robot.removeOnCurrentPositionChangedListener(this);
        robot.removeOnGoToLocationStatusChangedListener(this);
        robot.removeOnRobotReadyListener(this);
        timerval = 0;
        if (recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
            System.out.println("list: ----最終釋放----");
        }
        else Log.d(TAG,"list: recorder is null.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        DatabaseReference myRef2 = mDatabase.child("face").child("temi1").child("patrol").child("and");
        myRef2.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                Boolean value2 = dataSnapshot.getValue(Boolean.class);
                Log.d("TAG", "Value2 is: " + value2);
                if (value2 == true){
                    startCamera();
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w("TAG", "Failed to read value.", error.toException());
            }
        });
    }

    public void btnlock(View v){
        Intent it = new Intent(Patrol.this,MainActivity.class);
        startActivity(it);
        finish();
    }

    public void TimerManager(String hrs, String min, String strlocat) {
        int inthrs = Integer.parseInt(hrs);
        int intmin = Integer.parseInt(min);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, inthrs);
        calendar.set(Calendar.MINUTE, intmin);
        calendar.set(Calendar.SECOND, 0);
        Date date = calendar.getTime(); //第一次執行任務的時間
        //如果第一次執行定時任務的時間 小於當前時間
        //此時要在 第一次執行定時任務的時間加一天，以便此任務在下個時間點執行。如果不加一天，任務會立即執行。
        if (date.before(new Date())) {
            date = this.addDay(date, 1);
        }
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                System.out.println("list: 執行定時巡邏");
                robot.goTo(strlocat);
                startrec();
            }
        };
        //安排指定的任務在指定的時間開始進行重複的固定延遲執行。
        timer.schedule(task, date, PERIOD_DAY);
    }

    // 增加或减少天數
    public Date addDay(Date date, int num) {
        Calendar startDT = Calendar.getInstance();
        startDT.setTime(date);
        startDT.add(Calendar.DAY_OF_MONTH, num);
        return startDT.getTime();
    }

    @Override
    public void onRobotReady(boolean isReady) {
        if (isReady) {
            try {
                final ActivityInfo activityInfo = getPackageManager().getActivityInfo(getComponentName(), PackageManager.GET_META_DATA);
                robot.onStart(activityInfo);
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onCurrentPositionChanged(Position position) {
        System.out.println("list:onCurrentPosition Position: "+position.toString());
    }

    @Override
    public void onGoToLocationStatusChanged(@NotNull String location, String status, int descriptionId, @NotNull String description) {
        System.out.println("list: OnGoToLocationStatusChanged");
        switch (status) {
            case OnGoToLocationStatusChangedListener.START:
                try {
                    robot.tiltAngle(55);
                    robot.setGoToSpeed(SpeedLevel.MEDIUM);
                    System.out.println("list: OnGoToLocationStatusChangedListener_START");
                } catch (Exception e) {
                    Log.e(TAGError, "list:Error:" + e.getMessage());
                }
                break;
            case OnGoToLocationStatusChangedListener.GOING:
                try {
                    robot.tiltAngle(55);
                    robot.setGoToSpeed(SpeedLevel.MEDIUM);
                    System.out.println("list: OnGoToLocationStatusChangedListener_GOING");
                } catch (Exception e) {
                    Log.e(TAGError, "list:Error:" + e.getMessage());
                }
                break;
            case OnGoToLocationStatusChangedListener.CALCULATING:
                robot.tiltAngle(55);
                System.out.println("list: OnGoToLocationStatusChangedListener_CALCULATING");
                //計算
                break;
            case OnGoToLocationStatusChangedListener.COMPLETE:
                try {
                    robot.tiltAngle(55);
                    robot.repose();
                    robot.stopMovement();
                    Thread.sleep(5000);
                    System.out.println("list: OnGoToLocationStatusChangedListener_COMPLETE");
                    stoprec();
                } catch (Exception e) {
                    Log.e(TAGError, "list: Error:" + e.getMessage());
                }
                break;
            case OnGoToLocationStatusChangedListener.ABORT:
                robot.tiltAngle(55);
                System.out.println("list: OnGoToLocationStatusChangedListener_ABORT");
                //robot.stopMovement();
                break;
        }
    }

    public void DBTime(){
        //      DatabaseReference myRef3 = mDatabase.child("patrol").child("temi1").child("1").child("1").child("place");
//              myRef3.addValueEventListener(new ValueEventListener() {
//                            @Override
//                            public void onDataChange(DataSnapshot dataSnapshot) {
//                                // This method is called once with the initial value and again
//                                // whenever data at this location is updated.
//                                String value3 = dataSnapshot.getValue(String.class);
//                                Log.d("TAG", "Value2 is: " + value3);
//                                TimerManager(value1,value2,value3);
//                            }
//                            @Override
//                            public void onCancelled(DatabaseError error) {
//                                // Failed to read value
//                                Log.w("TAG", "Failed to read value.", error.toException());
//                            }
//                        });

//        DatabaseReference myRef1 = mDatabase.child("patrol").child("temi1").child("1").child("1").child("hrs");
//        myRef1.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                // This method is called once with the initial value and again
//                // whenever data at this location is updated.
//                String value1 = dataSnapshot.getValue(String.class);
//                Log.d("TAG", "Value2 is: " + value1);
//
//                DatabaseReference myRef2 = mDatabase.child("patrol").child("temi1").child("1").child("1").child("min");
//                myRef2.addValueEventListener(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(DataSnapshot dataSnapshot) {
//                        // This method is called once with the initial value and again
//                        // whenever data at this location is updated.
//                        String value2 = dataSnapshot.getValue(String.class);
//                        Log.d("TAG", "Value2 is: " + value2);
//
//                        DatabaseReference myRef3 = mDatabase.child("patrol").child("temi1").child("1").child("1").child("place");
//                        myRef3.addValueEventListener(new ValueEventListener() {
//                            @Override
//                            public void onDataChange(DataSnapshot dataSnapshot) {
//                                // This method is called once with the initial value and again
//                                // whenever data at this location is updated.
//                                String value3 = dataSnapshot.getValue(String.class);
//                                Log.d("TAG", "Value2 is: " + value3);
//                                TimerManager(value1,value2,value3);
//                            }
//                            @Override
//                            public void onCancelled(DatabaseError error) {
//                                // Failed to read value
//                                Log.w("TAG", "Failed to read value.", error.toException());
//                            }
//                        });
//
//                    }
//                    @Override
//                    public void onCancelled(DatabaseError error) {
//                        // Failed to read value
//                        Log.w("TAG", "Failed to read value.", error.toException());
//                    }
//                });
//            }
//            @Override
//            public void onCancelled(DatabaseError error) {
//                // Failed to read value
//                Log.w("TAG", "Failed to read value.", error.toException());
//            }
//        });
//
//        DatabaseReference myRef4 = mDatabase.child("patrol").child("temi1").child("1").child("1").child("hrs");
//        myRef4.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                // This method is called once with the initial value and again
//                // whenever data at this location is updated.
//                String value4 = dataSnapshot.getValue(String.class);
//                Log.d("TAG", "Value2 is: " + value4);
//
//                DatabaseReference myRef5 = mDatabase.child("patrol").child("temi1").child("1").child("1").child("min");
//                myRef5.addValueEventListener(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(DataSnapshot dataSnapshot) {
//                        // This method is called once with the initial value and again
//                        // whenever data at this location is updated.
//                        String value5 = dataSnapshot.getValue(String.class);
//                        Log.d("TAG", "Value2 is: " + value5);
//
//                        DatabaseReference myRef6 = mDatabase.child("patrol").child("temi1").child("1").child("1").child("place");
//                        myRef6.addValueEventListener(new ValueEventListener() {
//                            @Override
//                            public void onDataChange(DataSnapshot dataSnapshot) {
//                                // This method is called once with the initial value and again
//                                // whenever data at this location is updated.
//                                String value6 = dataSnapshot.getValue(String.class);
//                                Log.d("TAG", "Value2 is: " + value6);
//                                TimerManager(value4,value5,value6);
//                            }
//                            @Override
//                            public void onCancelled(DatabaseError error) {
//                                // Failed to read value
//                                Log.w("TAG", "Failed to read value.", error.toException());
//                            }
//                        });
//
//                    }
//                    @Override
//                    public void onCancelled(DatabaseError error) {
//                        // Failed to read value
//                        Log.w("TAG", "Failed to read value.", error.toException());
//                    }
//                });
//
//            }
//            @Override
//            public void onCancelled(DatabaseError error) {
//                // Failed to read value
//                Log.w("TAG", "Failed to read value.", error.toException());
//            }
//        });

    }

    /**
     * 錄音權限申請
     */
    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = new String[]{Manifest.permission.RECORD_AUDIO,Manifest.permission.WRITE_EXTERNAL_STORAGE};
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, permissions, 200);
                    return;
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[]
            grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && requestCode == 200) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivityForResult(intent, 200);
                    return;
                }
            }
        }
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        if (requestCode == PERMISSION_CODE) {
            setupCamera();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 200) {
            checkPermission();
        }
    }

    /**
     * 枚舉了兩種常用的類型
     */
    public enum Type {
        AAC_M4A(".m4a", MediaRecorder.AudioEncoder.AAC, MediaRecorder.OutputFormat.MPEG_4),
        AAC_AAC(".aac", MediaRecorder.AudioEncoder.AAC, MediaRecorder.OutputFormat.AAC_ADTS),
        AMR_AMR(".amr", MediaRecorder.AudioEncoder.AMR_NB, MediaRecorder.OutputFormat.AMR_NB);
        String ext;
        int audioEncoder;
        int outputFormat;
        Type(String ext, int audioEncoder, int outputFormat) {
            this.ext = ext;
            this.audioEncoder = audioEncoder;
            this.outputFormat = outputFormat;
        }
    }

    /**
     * 獲取錄音的聲音聲壓值及分貝值
     */
//    public void getDB(){
//        System.out.println("list:開始監聽, getDB");
//        try {
//            if (recorder != null) {
//                recorder.reset();
//                System.out.println("list: ----停止錄音----");
//            }
//            else Log.d(TAG,"list: recorder is null.");
//            final int[] conti = {0};
//            recorder = new MediaRecorder();
//            System.out.println("list: getDB 開始錄音1");
//            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//            recorder.setOutputFormat(type1.outputFormat);
//            recorder.setAudioEncoder(type1.audioEncoder);
//            //設置輸出文件的位置
//            recorder.setOutputFile(new File(getExternalFilesDir(""),"record_b.mp4")
//                    .getAbsolutePath());
//            System.out.println("list: 錄音存放位置:"+getExternalFilesDir("").getAbsolutePath()+"/record_b.mp4");
//            recorder.prepare();
//            recorder.start();
//            timer = new Timer();
//            task = new TimerTask() { //設置線程抽象類中的run()，這裡更新value的值
//                @Override //把value的值放到用於線程之間交流數據的Handler的message裡
//                public void run() {
//                    if (timerval == 1) {
//                        value2 = recorder.getMaxAmplitude();
//                        System.out.println("list: 聲壓值: " + value2);
//                        if (value2 > 0 && value2 < 1000000) {
//                            setDbCount(20 * (float) (Math.log10(value2)));  //將聲壓值轉為分貝值
//                            System.out.println("list: 分貝值: " + lastDbCount);
//                        } else System.out.println("list: 分貝值轉換失敗");
//                        switch(conti[0]) {
//                            case 0:
//                                if (lastDbCount >= 55 ){
//                                    startrec();
//                                    conti[0]++;
//                                }
//                                break;
//                            case 1:
//                                if (lastDbCount < 55){
//                                    stoprec();
//                                    try {
//                                        if (recorder != null) {
//                                            recorder.reset();
//                                            System.out.println("list: ----停止錄音----");
//                                        }
//                                        else System.out.println("list: recorder is null.");
//                                        recorder = new MediaRecorder();
//                                        System.out.println("list: stoprec 開始錄音1");
//                                        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//                                        recorder.setOutputFormat(type1.outputFormat);
//                                        recorder.setAudioEncoder(type1.audioEncoder);
//                                        //設置輸出文件的位置
//                                        recorder.setOutputFile(new File(getExternalFilesDir(""),"record_b.mp4")
//                                                .getAbsolutePath());
//                                        System.out.println("list: 錄音存放位置:"+getExternalFilesDir("").getAbsolutePath()+"/record_b.mp4");
//                                        recorder.prepare();
//                                        recorder.start();
//                                        System.out.println("list: stoprec 開始錄音2");
//                                    } catch (IOException e) {
//                                        e.printStackTrace();
//                                        Log.d(TAG,"list: stoprec 開始錄音失敗");
//                                    }
//                                    conti[0]--;
//                                }
//                                break;
//                        }
//                    }
//                    else {
//                        //Log.d(TAG,"list: 未提取分貝值");
//                    }
//                }
//            };
//            timer.schedule(task, 100,2000); //timer，設置為100毫秒後執行task線程(會自動調用task的start()函數)
//            //timer是計時器，作用就是在設定時間後啟動規定的線程。這用來限制getMaxAmplitude()的調用頻率，減少資源的使用(時間調太短，也會閃退)
//        } catch (IOException e) {
//            e.printStackTrace();
//            Log.d(LOG_TAG, String.valueOf(e));
//            Log.d(TAG,"list: 開始監聽失敗");
//        }
//    }

    public static float setDbCount(float dbValue) {
        if (dbValue > lastDbCount) {
            value = dbValue - lastDbCount > min ? dbValue - lastDbCount : min;
        }else{
            value = dbValue - lastDbCount < -min ? dbValue - lastDbCount : -min;
        }
        dbCount = lastDbCount + value * 0.2f ; //防止聲音變化太快
        lastDbCount = dbCount;
        return lastDbCount;
    }

    public void startrec(){
        try {
            if (recorder != null) {
                recorder.reset();
                System.out.println("list: ----停止錄音----");
            }
            else System.out.println("list: recorder is null.");
            recorder = new MediaRecorder();
            System.out.println("list: startrec 開始錄音1");
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(type1.outputFormat);
            recorder.setAudioEncoder(type1.audioEncoder);
            //設置輸出文件的位置
            recorder.setOutputFile(new File(getExternalFilesDir(""),t+".mp4")
                    .getAbsolutePath());
            System.out.println("list: 錄音存放位置:"+getExternalFilesDir("").getAbsolutePath()+"/"+t+".mp4");
            recorder.prepare();
            recorder.start();
            System.out.println("list: startrec 開始錄音2");
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG,"list: startrec 開始錄音失敗");
        }
    }

    public void stoprec(){
        if (recorder != null) {
            uploadAudio();
            recorder.reset();
            System.out.println("list: stoprec 結束錄音");
        }
        else Log.d(TAG,"list: recorder is null.");
    }

    public void uploadAudio(){
        // Create a storage reference from our app
        StorageReference storageRef = storage.getReference();

        // Create a reference to "record_a.mp4"
        StorageReference recordRef = storageRef.child(t+".mp4");

        // Create a reference to 'audios/record_a.mp4'
        StorageReference recordAudiosRef = storageRef.child("audios/"+t+".mp4");

        // While the file names are the same, the references point to different files
        recordRef.getName().equals(recordAudiosRef.getName());    // true
        recordRef.getPath().equals(recordAudiosRef.getPath());    // false

        // Create file metadata including the content type
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("audio/mpeg")
                .build();

//        Uri file = Uri.fromFile(new File("path/to/record_a.mp4"));
        Uri file = Uri.fromFile(new File(getExternalFilesDir("").getAbsolutePath() + "/"+t+".mp4"));
        recordRef = storageRef.child("audios/"+file.getLastPathSegment());
        // Upload the file and metadata
        //UploadTask uploadTask = storageRef.child("audios/record_a.mpeg").putFile(file, metadata);
        UploadTask uploadTask = recordRef.putFile(file,metadata);

        // Observe state change events such as progress, pause, and resume
        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                Log.d(TAG_f, "list: Upload is " + progress + "% done");
                if(progress == 100){
                    mDatabase.child("face").child("temi1").child("patrol").child("py").setValue(false);
                    Intent it = new Intent(Patrol.this,MainActivity.class);
                    startActivity(it);
                    finish();
                }
            }
        }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG_f, "list: Upload is paused");
            }
        });

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                int errorCode = ((StorageException) exception).getErrorCode();
                String errorMessage = exception.getMessage();
                // test the errorCode and errorMessage, and handle accordingly
                Log.d(TAG_f,"list: upload failure");
                Log.d(TAG_f,"list: errorCode: " + errorCode +", errorMessage: " + errorMessage);
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                System.out.println("list: upload task: " + taskSnapshot.toString());
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // If there's an upload in progress, save the reference so you can query it later
        if (mStorageRef != null) {
            outState.putString("reference", mStorageRef.toString());
            Log.d(TAG_f,"list: outstate: " + mStorageRef.toString());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // If there was an upload in progress, get its reference and create a new StorageReference
        final String stringRef = savedInstanceState.getString("reference");
        if (stringRef == null) {
            return;
        }
        mStorageRef = FirebaseStorage.getInstance().getReferenceFromUrl(stringRef);

        // Find all UploadTasks under this StorageReference (in this example, there should be one)
        List<UploadTask> tasks = mStorageRef.getActiveUploadTasks();
        if (tasks.size() > 0) {
            // Get the task monitoring the upload
            UploadTask task = tasks.get(0);

            // Add new listeners to the task using an Activity scope
            task.addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot state) {
                    // Success!
                    Log.d(TAG_f,"list: upload state: " + state.toString());
                }
            });
        }

        // Find all DownloadTasks under this StorageReference (in this example, there should be one)
        List<FileDownloadTask> tasks2 = mStorageRef.getActiveDownloadTasks();
        if (tasks.size() > 0) {
            // Get the task monitoring the download
            FileDownloadTask task = tasks2.get(0);

            // Add new listeners to the task using an Activity scope
            task.addOnSuccessListener(this, new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot state) {
                    // Success!
                    Log.d(TAG_f,"list: Instance Success: " + state);
                }
            });
        }
    }


    /** 人臉辨識 */
    /** Permissions Handler */
    private void getPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{CAMERA_PERMISSION}, PERMISSION_CODE);
    }

    /** Setup camera & use cases */
    private void startCamera() {
        if(ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            setupCamera();
            System.out.println("list:2 startCamera1");
        } else {
            getPermissions();
            System.out.println("list:2 startCamera2");
        }
    }

    private void setupCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);
        System.out.println("list:2 setupCamera");

        cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindAllCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "cameraProviderFuture.addListener Error", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindAllCameraUseCases() {
        System.out.println("list:2 bindAllCameraUseCases");

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            bindPreviewUseCase();
            bindAnalysisUseCase();
        }
    }

    private void bindPreviewUseCase() {
        System.out.println("list:2 bindPreviewUseCase");

        if (cameraProvider == null) {
            return;
        }

        if (previewUseCase != null) {
            cameraProvider.unbind(previewUseCase);
        }

        Preview.Builder builder = new Preview.Builder();
        builder.setTargetAspectRatio(AspectRatio.RATIO_4_3);

        previewUseCase = builder.build();
        previewUseCase.setSurfaceProvider(previewView.getSurfaceProvider());

        try {
            cameraProvider
                    .bindToLifecycle(this, cameraSelector, previewUseCase);
        } catch (Exception e) {
            Log.e(TAG, "Error when bind preview", e);
        }
    }

    private void bindAnalysisUseCase() {
        System.out.println("list:2 bindAnalysisUseCase");

        if (cameraProvider == null) {
            return;
        }

        if (analysisUseCase != null) {
            cameraProvider.unbind(analysisUseCase);
        }

        Executor cameraExecutor = Executors.newSingleThreadExecutor();

        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        builder.setTargetAspectRatio(AspectRatio.RATIO_4_3);

        analysisUseCase = builder.build();
        analysisUseCase.setAnalyzer(cameraExecutor, this::analyze);

        try {
            cameraProvider
                    .bindToLifecycle(this, cameraSelector, analysisUseCase);
        } catch (Exception e) {
            Log.e(TAG, "Error when bind analysis", e);
        }
    }

    public void uploadImage(Bitmap bitmap){
        Log.d(TAG_f, "list: upload");
        // Create a storage reference from our app
        StorageReference storageRef = storage.getReference();

        StorageReference checkinRef = storageRef.child("images").child("unknown").child("unknown1.jpg");

//        UploadTask uploadTask = checkinRef.putFile(file);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();
        UploadTask uploadTask = checkinRef.putBytes(data);

        // Observe state change events such as progress, pause, and resume
        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                Log.d(TAG_f, "list: Upload is " + progress + "% done");
            }
        }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG_f, "list: Upload is paused");
            }
        });

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                // ...
            }
        });
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void analyze(@NonNull ImageProxy image) {
        System.out.println("list:2 analyze");
        mDatabase.child("face").child("temi1").child("patrol").child("id").setValue("");

        if (image.getImage() == null) return;

        InputImage inputImage = InputImage.fromMediaImage(
                image.getImage(),
                image.getImageInfo().getRotationDegrees()
        );

        FaceDetector faceDetector = FaceDetection.getClient();

        faceDetector.process(inputImage)
                .addOnSuccessListener(faces -> onSuccessListener(faces, inputImage))
                .addOnFailureListener(e -> Log.e(TAG, "Barcode process failure", e))
                .addOnCompleteListener(task -> image.close());
    }

    private void onSuccessListener(List<Face> faces, InputImage inputImage) {
        System.out.println("list:2 onSuccessListener");
        Rect boundingBox = null;
        //String name = null;
        //float scaleX = (float) previewView.getWidth() / (float) inputImage.getHeight();
        //float scaleY = (float) previewView.getHeight() / (float) inputImage.getWidth();

        if (faces.size() > 0) {

//            // get first face detected
//            Face face = faces.get(0);
//
//            // get bounding box of face;
//            boundingBox = face.getBoundingBox();
//
            // convert img to bitmap & crop img
            Bitmap bitmapImage = mediaImgToBmp(
                    inputImage,
                    inputImage.getRotationDegrees(),
                    boundingBox);
//            System.out.println("list:2 onSuccessListener4: " + inputImage.getMediaImage());
//            System.out.println("list:2 bitmap4: " + bitmap);

            uploadImage(bitmapImage);
        }
        //graphicOverlay.draw(boundingBox, scaleX, scaleY, name);
    }

    /** Recognize Processor */
    /** Bitmap Converter
     * @return*/
    private Bitmap mediaImgToBmp(InputImage image2, int rotation, Rect boundingBox) {
        System.out.println("list:2 mediaImgToBmp");
        System.out.println("list:2 mediaImgToBmp image: " +image2);
        Bitmap frame_bmp1 = null;

        Image image = image2.getMediaImage();
        //Convert media image to Bitmap
        Bitmap frame_bmp = toBitmap(image);
        //Adjust orientation of Face
        frame_bmp1 = rotateBitmap(frame_bmp, rotation, flipX);

        return frame_bmp1;
    }

    private static Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees, boolean flipX) {
        System.out.println("list:2 rotateBitmap");

        Matrix matrix = new Matrix();

        // Rotate the image back to straight.
        matrix.postRotate(rotationDegrees);

        // Mirror the image along the X or Y axis.
        matrix.postScale(flipX ? -1.0f : 1.0f, 1.0f);
        Bitmap rotatedBitmap =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        // Recycle the old bitmap if it has changed.
        if (rotatedBitmap != bitmap) {
            bitmap.recycle();
        }
        return rotatedBitmap;
    }

    private static byte[] YUV_420_888toNV21(Image image) {
        System.out.println("list:2 YUV_420_888toNV21");

        int width = image.getWidth();//640
        int height = image.getHeight();//480
        int ySize = width*height;
        int uvSize = width*height/4;

        byte[] nv21 = new byte[ySize + uvSize*2];

        //1,2,2
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer(); // Y
        //list: yBuffer: java.nio.DirectByteBuffer[pos=0 lim=307200 cap=307200]
//      Log.d(TAG, "list: yBuffer1: "+yBuffer);

        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer(); // U
        //list: uBuffer: java.nio.DirectByteBuffer[pos=0 lim=153599 cap=153599]
//      Log.d(TAG, "list: uBuffer1: "+uBuffer);

        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer(); // V
        //list: vBuffer: java.nio.DirectByteBuffer[pos=0 lim=153599 cap=153599]
//      Log.d(TAG, "list: uBuffer1: "+vBuffer);

        int rowStride = image.getPlanes()[0].getRowStride();
//      Log.d(TAG, "list: rowStride[0]: "+rowStride);//640

        assert(image.getPlanes()[0].getPixelStride() == 1);

        int pos = 0;

        if (rowStride == width) { // likely
            yBuffer.get(nv21, 0, ySize);
            pos += ySize;
        }
        else {
            long yBufferPos = -rowStride; // not an actual position
            for (; pos<ySize; pos+=width) {
                yBufferPos += rowStride;
                yBuffer.position((int) yBufferPos);
                yBuffer.get(nv21, pos, width);
            }
        }
        rowStride = image.getPlanes()[2].getRowStride();
        int pixelStride = image.getPlanes()[2].getPixelStride();
        assert(rowStride == image.getPlanes()[1].getRowStride());
        assert(pixelStride == image.getPlanes()[1].getPixelStride());

        if (pixelStride == 2 && rowStride == width && uBuffer.get(0) == vBuffer.get(1)) {
            // maybe V an U planes overlap as per NV21, which means vBuffer[1] is alias of uBuffer[0]
            byte savePixel = vBuffer.get(1);
            try {
                vBuffer.put(1, (byte)~savePixel);
                if (uBuffer.get(0) == (byte)~savePixel) {
                    vBuffer.put(1, savePixel);
                    vBuffer.position(0);
                    uBuffer.position(0);
                    vBuffer.get(nv21, ySize, 1);
                    uBuffer.get(nv21, ySize + 1, uBuffer.remaining());

                    return nv21; // shortcut
                }
            }
            catch (ReadOnlyBufferException ex) {
                // unfortunately, we cannot check if vBuffer and uBuffer overlap
            }

            // unfortunately, the check failed. We must save U and V pixel by pixel
            vBuffer.put(1, savePixel);
        }

        // other optimizations could check if (pixelStride == 1) or (pixelStride == 2),
        // but performance gain would be less significant

        for (int row=0; row<height/2; row++) {
            for (int col=0; col<width/2; col++) {
                int vuPos = col*pixelStride + row*rowStride;
                nv21[pos++] = vBuffer.get(vuPos);
                nv21[pos++] = uBuffer.get(vuPos);
            }
        }

        return nv21;
    }

    private Bitmap toBitmap(Image image) {
        System.out.println("list:2 toBitmap");

        byte[] nv21=YUV_420_888toNV21(image);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();

        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }
}