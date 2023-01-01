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
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
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
import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements
        OnGoToLocationStatusChangedListener, //temi走路狀態監聽器
        OnCurrentPositionChangedListener, //temi位置變更監聽器
        OnRobotReadyListener { //應用程式顯示在temi的首頁上
    private static final String LOG_TAG = "MainActivity"; //log首頁標記
    private static final String TAG = "MediaRecorderUtil"; //log錄音標記

    private static Robot robot; //temi sdk宣告
    private static FirebaseStorage storage; //firebase storage宣告
    private StorageReference mStorageRef; //firebase storage 檔案位址宣告
    private DatabaseReference mDatabase; //firebase 即時資料庫宣告
    private static final String TAG_f = "Firebase"; //log firebase標記

    //語音文件保存路徑(沒用到)
//    private final String FileName = getExternalFilesDir("").getAbsolutePath();
    //語音操作對象(沒用到)
//    private MediaPlayer mPlayer = null;

    private Calendar calendar = Calendar.getInstance(); //時間宣告
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss"); //日期與時間格式宣告
    private String audioname = ""; //錄音檔名稱宣告
    private String placename = ""; //temi定點位置名稱宣告
    //語音操作對象
    private MediaRecorder recorder; //安卓錄音宣告

    private MainActivity.Type type1 = MainActivity.Type.AAC_AAC; //安卓錄音3種類型宣告
    private MainActivity.Type type2 = MainActivity.Type.AAC_M4A;
    private MainActivity.Type type3 = MainActivity.Type.AMR_AMR;

    public static float dbCount = 40; //分貝變化計算宣告及初始值
    private static float lastDbCount = dbCount; //分貝變化計算宣告
    private static float min = 0.5f; //設置聲音最低變化
    private static float value = 0; //聲音分貝值

    private int timerval = 0; //錄音timer變數宣告(沒用到)
    private int y = 0; //上傳圖片變數宣告
    private TimerTask task = null; //排程timer任務宣告
    private Timer timer = null; //排程timer宣告
    private static final long PERIOD_DAY = 24 * 60 * 60 * 1000; //排程週期宣告, 設為每天
    private static final String TAGError = "Recorder"; //log錄音標記

    //臉部辨識
//    private static final String TAG_fr = "FaceRecognition"; //log臉部辨識標記
    private static final int PERMISSION_CODE = 1001; //相機授權碼宣告
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA; //相機授權宣告
    private PreviewView previewView; //相機預覽畫面宣告
    private CameraSelector cameraSelector; //前後相機選擇宣告
    private ProcessCameraProvider cameraProvider; //相機提供宣告
    private int lensFacing = CameraSelector.LENS_FACING_BACK; //後相機宣告
    private Preview previewUseCase; //預覽畫面綁定宣告
    private ImageAnalysis analysisUseCase; //畫面分析綁定宣告
    private pl.droidsonroids.gif.GifImageView gifImageView; //ui gif(笑臉)宣告
    private ImageView bgwhite; //ui 白色背景宣告
    //    private GraphicOverlay graphicOverlay; //舊安卓端即時臉部辨識用(沒用到)
//    private ImageView previewImg; //舊安卓端即時臉部辨識用(預覽畫面)(沒用到)
//
//    private final HashMap<String, SimilarityClassifier.Recognition> registered = new HashMap<>(); //saved Faces //舊安卓端即時臉部辨識用(註冊用)(沒用到)
//    private Interpreter tfLite; //舊安卓端即時臉部辨識用(tensorflow lite宣告)(沒用到)
    private boolean flipX = false; //畫面旋轉宣告
//    private boolean start = true; //舊安卓端即時臉部辨識用(控制不偵測的變數)(沒用到)
//    private boolean regis = false; //舊安卓端即時臉部辨識用(註冊變數宣告)(沒用到)
//    private float[][] embeddings; //舊安卓端即時臉部辨識用(註冊緩衝區存放特徵值用)(沒用到)
//
//    private static final float IMAGE_MEAN = 128.0f; //舊安卓端即時臉部辨識用(畫面參數宣告)(沒用到)
//    private static final float IMAGE_STD = 128.0f; //舊安卓端即時臉部辨識用(畫面參數宣告)(沒用到)
//    private static final int INPUT_SIZE = 112; //舊安卓端即時臉部辨識用(畫面輸入大小宣告)(沒用到)
//    private static final int OUTPUT_SIZE = 192; //舊安卓端即時臉部辨識用(畫面輸出大小宣告)(沒用到)

    private FirebaseDatabase database = FirebaseDatabase.getInstance(); //firebase 即時資料庫api引用宣告

    @Override
    protected void onCreate(Bundle savedInstanceState) { //安卓Activity生命週期onCreate
        super.onCreate(savedInstanceState); //執行生命週期onCreate
        setContentView(R.layout.activity_main); //綁定首頁ui畫面

        robot = Robot.getInstance(); //temi sdk 引用

        checkPermission(); //確認相機及錄音授權
        y = 0; //上傳圖片變數初始值

        mStorageRef = FirebaseStorage.getInstance().getReference(); //firebase storage api 引用(監聽上傳狀態用)
        mDatabase = FirebaseDatabase.getInstance().getReference(); //firebase 即時資料庫 api 引用
        storage = FirebaseStorage.getInstance(); //firebase storage api 引用

        previewView = findViewById(R.id.previewView); //ui 預覽畫面綁定
        previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER); //ui 預覽畫面位置綁定
//        graphicOverlay = findViewById(R.id.graphic_overlay); //ui 偵測臉部正方形框框綁定, 舊安卓端即時臉部辨識用(沒用到)
//        previewImg = findViewById(R.id.preview_img); //ui 預覽畫面綁定, 舊安卓端即時臉部辨識用(沒用到)

        bgwhite = findViewById(R.id.bgwhite); //ui 白色背景綁定
        gifImageView = findViewById(R.id.gifImageView); //ui gif笑臉綁定

        mDatabase.child("face").child("temi2").child("welcome").child("py").setValue(false); //以下為臉部辨識firebase變數初始化
        mDatabase.child("face").child("temi2").child("welcome").child("and").setValue(false);
        mDatabase.child("face").child("temi2").child("regis").child("py").setValue(false);
        mDatabase.child("face").child("temi2").child("regis").child("and").setValue(false);
        mDatabase.child("face").child("temi2").child("checkin").child("py").setValue(false);
        mDatabase.child("face").child("temi2").child("checkin").child("and").setValue(false);
        mDatabase.child("face").child("temi2").child("patrol").child("py").setValue(false);
        mDatabase.child("face").child("temi2").child("patrol").child("and").setValue(false);

        DBTime(); //呼叫巡邏時間地點方法
    }

    @Override
    protected void onStart() { //安卓Activity生命週期onStart
        super.onStart(); //執行生命週期onStart
        robot.addOnCurrentPositionChangedListener(this); //temi位置變更監聽器添加
        robot.addOnGoToLocationStatusChangedListener(this); //temi走路狀態監聽器添加
        robot.addOnRobotReadyListener(this); //應用程式顯示在temi的首頁上監聽器添加

        //測試用
//        audioname = dateFormat.format(calendar.getTime()); //錄音檔名稱設置時間
//        timerval = 1; //錄音timer變數初始值(沒用到)
//        robot.goTo(place); //temi前往地點
//        startrec(audioname); //開始錄音方法呼叫
//        stoprec(); //停止錄音方法呼叫
//        startCamera(); //開啟相機方法呼叫
//        mDatabase.child("face").child("temi1").child("patrol").child("py").setValue(true); //臉部辨識 firebase變數 巡邏開啟
//        mDatabase.child("face").child("temi1").child("checkin").child("py").setValue(false); //臉部辨識 firebase變數 報到關閉
//        mDatabase.child("face").child("temi1").child("regis").child("py").setValue(false); //臉部辨識 firebase變數 註冊關閉
//        mDatabase.child("face").child("temi1").child("welcome").child("py").setValue(false); //臉部辨識 firebase變數 迎賓關閉
    }

    @Override
    public void onStop() { //安卓Activity生命週期onStop
        super.onStop(); //執行生命週期onStop
        robot.removeOnCurrentPositionChangedListener(this); //temi位置變更監聽器移除
        robot.removeOnGoToLocationStatusChangedListener(this); //temi走路狀態監聽器移除
        robot.removeOnRobotReadyListener(this); //應用程式顯示在temi的首頁上監聽器移除
        try {
            System.out.println("list:4 stop 停止錄音: " + recorder); //輸出錄音狀態
            recorder.stop(); //安卓錄音api停止方法
            recorder.release(); //安卓錄音api釋放方法
            recorder = null; //安卓錄音狀態初始化
            uploadAudio(audioname); //上傳錄音檔方法呼叫
        } catch (RuntimeException e) {
            Log.e(TAG, e.toString()); //log 輸出 bug說明
            System.out.println("list:4 stop 停止錄音 e: " + recorder); //輸出錄音狀態
//            uploadAudio(audioname); //上傳錄音檔方法呼叫
//            recorder.reset(); //安卓錄音api還原方法
//            recorder.release(); //安卓錄音api釋放方法
            recorder = null; //安卓錄音狀態初始化
//            File file3 = new File(getExternalFilesDir(""), audioname + ".mp4"); //建立錄音檔
//            if (file3.exists()) //如果檔案存在
//                file3.delete(); //將檔案移除
//            System.out.println("list:4 stoprec file3 delete: " + audioname); //log輸出已移除檔案
        }
//        timerval = 0; //錄音timer變數(沒用到)
//        if (recorder != null) { //如果安卓錄音狀態不是初始狀態
//            recorder.stop(); //安卓錄音api停止方法
//            recorder.release(); //安卓錄音api釋放方法
//            recorder = null; //安卓錄音狀態初始化
//            System.out.println("list: ----最終釋放----"); //log輸出錄音狀態釋放
//        } else Log.d(TAG, "list: recorder is null."); //log輸出錄音狀態為初始狀態
    }

    @Override
    protected void onResume() { //安卓Activity生命週期onResume
        super.onResume(); //執行生命週期onResume
        startCamera(); //開啟相機方法呼叫
//        DatabaseReference myRef2 = mDatabase.child("face").child("temi1").child("patrol").child("py"); //臉部辨識 firebase 變數 巡邏狀態位址
//        myRef2.addValueEventListener(new ValueEventListener() { //firebase api 回傳值監聽器
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) { //firebase 回傳值與初始值有變化時
//                // This method is called once with the initial value and again
//                // whenever data at this location is updated.
//                Boolean value2 = dataSnapshot.getValue(Boolean.class);
//                Log.d("TAG", "Value2 is: " + value2);
//                if (value2 == true) { //如果變數是true
//                    startCamera(); //開啟相機方法呼叫
//                }
//            }
//
//            @Override
//            public void onCancelled(DatabaseError error) { //資料庫無法讀取到值
//                // Failed to read value
//                Log.w("TAG", "Failed to read value.", error.toException());
//            }
//        });
    }

    public void btnface(View v) { //按下智能報到按鈕
        Intent it = new Intent(MainActivity.this, FaceRecognition.class); //跳到智能報到頁面
        startActivity(it); //開始下一個頁面生命週期
        finish(); //結束此頁面
    }

    public void btngame(View v) { //按下猜拳小遊戲按鈕
        Intent it = new Intent(MainActivity.this, Game.class); //跳到猜拳小遊戲頁面
        startActivity(it); //開始下一個頁面生命週期
        finish(); //結束此頁面
    }

    public void btnregis(View v) { //按下照片註冊按鈕
        Intent it = new Intent(MainActivity.this, Regis.class); //跳到照片註冊頁面
        startActivity(it); //開始下一個頁面生命週期
        finish(); //結束此頁面
    }

    public void btnwelcome(View v) { //按下迎賓按鈕
        Intent it = new Intent(MainActivity.this, Welcome.class); //跳到迎賓頁面
        startActivity(it); //開始下一個頁面生命週期
        finish(); //結束此頁面
    }

     //測試用(播放錄音)
//    public void btnstartplay(View v){ //按下播放錄音按鈕
//        mPlayer = new MediaPlayer();
//        try {
//            mPlayer.setDataSource(new File(getExternalFilesDir(""), "record_a.mp4").getAbsolutePath()); //安卓播放api錄音檔位址
//            mPlayer.prepare(); //安卓播放api準備方法
//            mPlayer.start(); //安卓播放api開始方法
//            System.out.println("list: 播放錄音成功"); //log輸出播放錄音成功
//        } catch (IOException e) {
//            Log.e(LOG_TAG, "list: 播放失敗"); //log輸出播放失敗
//        }
//    }
//
//    public void btnstopplay(View v){ //按下播放錄音停止按鈕
//        mPlayer.release(); //安卓播放api釋放方法
//        mPlayer = null; //安卓播放api播放狀態初始化
//        System.out.println("list: 停止播放錄音");  //log輸出停止播放錄音
//    }

    public void TimerManager(String hrs, String min, String place2) { //定時定點錄音排程方法
        int inthrs = Integer.parseInt(hrs); //排程小時宣告
        int intmin = Integer.parseInt(min); //排程分鐘宣告
        Calendar calendar = Calendar.getInstance(); //安卓時間引用
        calendar.set(Calendar.HOUR_OF_DAY, inthrs); //排程小時
        calendar.set(Calendar.MINUTE, intmin); //排程分鐘
        calendar.set(Calendar.SECOND, 0); //排程秒
        Date date = calendar.getTime(); //第一次執行任務的時間
        //如果第一次執行定時任務的時間 小於當前時間
        //此時要在 第一次執行定時任務的時間加一天，以便此任務在下個時間點執行。如果不加一天，任務會立即執行。
        if (date.before(new Date())) {
            date = this.addDay(date, 1);
        }
        Timer timer = new Timer(); //timer定時定點錄音排程
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                mDatabase.child("face").child("temi1").child("patrol").child("py").setValue(true); //臉部辨識 firebase 變數 巡邏開啟
                mDatabase.child("face").child("temi1").child("checkin").child("py").setValue(false); //臉部辨識 firebase 變數 報到開啟
                mDatabase.child("face").child("temi1").child("regis").child("py").setValue(false); //臉部辨識 firebase 變數 註冊開啟
                mDatabase.child("face").child("temi1").child("welcome").child("py").setValue(false); //臉部辨識 firebase 變數 迎賓開啟
//                y = 1; //上傳圖片變數控制
                System.out.println("list:4 y1 = " + y); //log輸出上傳圖片變數值
                audioname = dateFormat.format(calendar.getTime()); //錄音檔名稱設為時間
                startrec(audioname); //開始錄音方法呼叫
                robot.goTo(place2); //temi sdk 走去指定地點
            }
        };
        //安排指定的任務在指定的時間開始進行重複的固定延遲執行。
        timer.schedule(task, date, PERIOD_DAY); //每天重複執行排程
    }

    //  排程增加或减少天數
    public Date addDay(Date date, int num) {
        Calendar startDT = Calendar.getInstance();
        startDT.setTime(date); //設定日期
        startDT.add(Calendar.DAY_OF_MONTH, num); //增加天數
        return startDT.getTime();
    }

    public void DBTime() { //讀取firebase設定的巡邏時間與地點
        List<String> patrolid = new ArrayList<>(); //巡邏編號list
        patrolid.add("1-1"); //firebase設定的巡邏編號1-1
        patrolid.add("1-2"); //firebase設定的巡邏編號1-2
        patrolid.add("1-3"); //firebase設定的巡邏編號1-3
//        patrolid.add("2-1"); //firebase設定的巡邏編號2-1
//        patrolid.add("2-2"); //firebase設定的巡邏編號2-2
//        patrolid.add("2-3"); //firebase設定的巡邏編號2-3
//        patrolid.add("3-1"); //firebase設定的巡邏編號3-1
//        patrolid.add("3-2"); //firebase設定的巡邏編號3-2
//        patrolid.add("3-3"); //firebase設定的巡邏編號3-3
        System.out.println(patrolid); //log輸出firebase設定的巡邏編號
        String strpatrol2 = ""; //字串用來放firebase設定的巡邏編號字串
        for (String strpatrol : patrolid) { //firebase設定的巡邏編號轉字串
            strpatrol2 = strpatrol; //firebase設定的巡邏編號最終字串
            DatabaseReference hrsRef = database.getReference("/temi1/" + strpatrol2 + "/hrs"); //firebase 巡邏編號位址
            String finalStrpatrol = strpatrol2; //firebase 最終巡邏編號位址字串
            hrsRef.addValueEventListener(new ValueEventListener() { //firebase 巡邏時間小時
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) { //firebase api 回傳值監聽器
                    // This method is called once with the initial value and again
                    // whenever data at this location is updated.
                    String hrs = dataSnapshot.getValue(String.class);
                    Log.d("TAG", "hrs: " + hrs);

                    DatabaseReference minRef = database.getReference("/temi1/" + finalStrpatrol + "/min");
                    minRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            // This method is called once with the initial value and again
                            // whenever data at this location is updated.
                            String min = dataSnapshot.getValue(String.class);
                            Log.d("TAG", "min: " + min);

                            DatabaseReference placeRef = database.getReference("/temi1/" + finalStrpatrol + "/place");
                            placeRef.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    // This method is called once with the initial value and again
                                    // whenever data at this location is updated.
                                    String place = dataSnapshot.getValue(String.class);
                                    Log.d("TAG", "place: " + place);
                                    if (hrs.trim().length() > 0 && min.trim().length() > 0 && place.trim().length() > 0) {
                                        TimerManager(hrs, min, place);
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError error) {
                                    // Failed to read value
                                    Log.w("TAG", "Failed to read value.", error.toException());
                                }
                            });
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            // Failed to read value
                            Log.w("TAG", "Failed to read value.", error.toException());
                        }
                    });
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Failed to read value
                    Log.w("TAG", "Failed to read value.", error.toException());
                }
            });
        }

    }

    @Override
    public void onRobotReady(boolean isReady) {//應用程式顯示在temi首頁上: https://github.com/robotemi/sdk/wiki/Home_chn
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
        System.out.println("list:onCurrentPosition Position: " + position.toString());
    }

    @Override
    public void onGoToLocationStatusChanged(@NotNull String location, String status, int descriptionId, @NotNull String description) {
        System.out.println("list: OnGoToLocationStatusChanged");
        switch (status) {
            case OnGoToLocationStatusChangedListener.START:
                try {
                    robot.tiltAngle(0);
                    robot.setGoToSpeed(SpeedLevel.SLOW);
//                    startrec();
                    System.out.println("list: OnGoToLocationStatusChangedListener_START");
                } catch (Exception e) {
                    Log.e(TAGError, "list:Error:" + e.getMessage());
                }
                break;
            case OnGoToLocationStatusChangedListener.GOING:
                try {
                    robot.tiltAngle(0);
                    robot.setGoToSpeed(SpeedLevel.SLOW);
                    System.out.println("list: OnGoToLocationStatusChangedListener_GOING");
                } catch (Exception e) {
                    Log.e(TAGError, "list:Error:" + e.getMessage());
                }
                break;
            case OnGoToLocationStatusChangedListener.CALCULATING:
                robot.tiltAngle(0);
                System.out.println("list: OnGoToLocationStatusChangedListener_CALCULATING");
                //計算
                break;
            case OnGoToLocationStatusChangedListener.COMPLETE:
                try {
                    robot.tiltAngle(55);
                    //robot.repose();
                    //robot.stopMovement();
                    y = 1;
                    System.out.println("list:4 y2 = " + y);
                    stoprec();
                    Thread.sleep(2000);
//                    mDatabase.child("face").child("temi1").child("patrol").child("py").setValue(false);
//                    bgwhite.setVisibility(View.INVISIBLE);
//                    gifImageView.setVisibility(View.INVISIBLE);
                    System.out.println("list: OnGoToLocationStatusChangedListener_COMPLETE");
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

//    private void hideKeyboard(Activity activity) {
//        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
//        //Find the currently focused view, so we can grab the correct window token from it.
//        View view = activity.getCurrentFocus();
//        //If no view currently has focus, create a new one, just so we can grab a window token from it
//        if (view == null) {
//            view = new View(activity);
//        }
//        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
//    }

    /**
     * 錄音權限申請
     */
    private void checkPermission() {
        System.out.println("list:4 checkPermission");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
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
//                if (recorder != null) {
//                    recorder.reset();
//                    System.out.println("list: ----停止錄音----");
//                }
//                else Log.d(TAG,"list: recorder is null.");
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
//
//    public static float setDbCount(float dbValue) {
//        if (dbValue > lastDbCount) {
//            value = dbValue - lastDbCount > min ? dbValue - lastDbCount : min;
//        }else{
//            value = dbValue - lastDbCount < -min ? dbValue - lastDbCount : -min;
//        }
//        dbCount = lastDbCount + value * 0.2f ; //防止聲音變化太快
//        lastDbCount = dbCount;
//        return lastDbCount;
//    }
    public void startrec(String audioname) {
        System.out.println("list:4 startrec t: " + audioname);
//        try {
//            if (recorder != null) {
//                recorder.reset();
//                System.out.println("list: ----停止錄音----");
//            }
//            else System.out.println("list: recorder is null.");
//            recorder = new MediaRecorder();
//            System.out.println("list: startrec 開始錄音1");
//            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//            recorder.setOutputFormat(type1.outputFormat);
//            recorder.setAudioEncoder(type1.audioEncoder);
//            //設置輸出文件的位置
//            recorder.setOutputFile(new File(getExternalFilesDir(""),"record_a.mp4")
//                    .getAbsolutePath());
//            System.out.println("list: 錄音存放位置:"+getExternalFilesDir("").getAbsolutePath()+"/record_a.mp4");
//            recorder.prepare();
//            recorder.start();
//            System.out.println("list: startrec 開始錄音2");
//        } catch (IOException e) {
//            e.printStackTrace();
//            Log.d(TAG,"list: startrec 開始錄音失敗");
//        }

        // 开始录音
        /* ①Initial：实例化MediaRecorder对象 */
        if (recorder == null) {
            recorder = new MediaRecorder();
            System.out.println("list:4 開始錄音: " + recorder);
        } else {
            System.out.println("list:4 開始錄音(nonnull): " + recorder);
        }
        try {
            /* ②setAudioSource/setVedioSource */
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);// 设置麦克风
            /*
             * ②设置输出文件的格式：THREE_GPP/MPEG-4/RAW_AMR/Default THREE_GPP(3gp格式
             * ，H263视频/ARM音频编码)、MPEG-4、RAW_AMR(只支持音频且音频编码要求为AMR_NB)
             */
//            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setOutputFormat(type1.outputFormat);
            /* ②设置音频文件的编码：AAC/AMR_NB/AMR_MB/Default 声音的（波形）的采样 */
//            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncoder(type1.audioEncoder);
            /* ③准备 */
            recorder.setOutputFile(new File(getExternalFilesDir(""), audioname + ".mp4").getAbsolutePath());
            recorder.prepare();
            /* ④开始 */
            recorder.start();
        } catch (IllegalStateException e) {
            Log.i(TAG, "call startAmr(File mRecAudioFile) failed!" + e.getMessage());
        } catch (IOException e) {
            Log.i(TAG, "call startAmr(File mRecAudioFile) failed!" + e.getMessage());
        }
    }

    public void stoprec() {
        try {
            System.out.println("list:4 stoprec 停止錄音: " + recorder);
            recorder.stop();
            recorder.release();
            recorder = null;
            uploadAudio(audioname);
        } catch (RuntimeException e) {
            Log.e(TAG, e.toString());
            System.out.println("list:4 stoprec 停止錄音 e: " + recorder);
            uploadAudio(audioname);
//            recorder.reset();
//            recorder.release();
            recorder = null;
//            File file3 = new File(getExternalFilesDir(""), audioname + ".mp4");
//            if (file3.exists())
//                file3.delete();
//            System.out.println("list:4 stoprec file3 delete: " + audioname);
        }
    }


    public void uploadAudio(String audioname2) {
        // Create a storage reference from our app
        StorageReference storageRef = storage.getReference();

        // Create a reference to "record_a.mp4"
        StorageReference recordRef = storageRef.child(audioname2 + ".mp4");

        // Create a reference to 'audios/record_a.mp4'
        StorageReference recordAudiosRef = storageRef.child("audios/" + audioname2 + ".mp4");

        // While the file names are the same, the references point to different files
        recordRef.getName().equals(recordAudiosRef.getName());    // true
        recordRef.getPath().equals(recordAudiosRef.getPath());    // false

        // Create file metadata including the content type
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("audio/mpeg")
                .build();

//        Uri file = Uri.fromFile(new File("path/to/record_a.mp4"));
        Uri file1 = Uri.fromFile(new File(getExternalFilesDir(""), audioname2 + ".mp4"));
        recordRef = storageRef.child("audios/" + file1.getLastPathSegment());
        // Upload the file and metadata
        //UploadTask uploadTask = storageRef.child("audios/record_a.mpeg").putFile(file, metadata);
        UploadTask uploadTask = recordRef.putFile(file1, metadata);

        // Observe state change events such as progress, pause, and resume
        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress3 = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                Log.d(TAG_f, "list:4 UploadAudio is " + progress3 + "% done");
                if (progress3 >= 100.0) {
                    File file2 = new File(file1.toString());
                    if (file2.exists()) {
                        if (file2.isFile()) {
                            file2.delete();
                        }
                    }
                    System.out.println("list:4 t uploadAudio: " + audioname2);
                    mDatabase.child("face").child("temi1").child("patrol").child("py").setValue(false);
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
                Log.d(TAG_f, "list: upload failure");
                Log.d(TAG_f, "list: errorCode: " + errorCode + ", errorMessage: " + errorMessage);
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
            Log.d(TAG_f, "list: outstate: " + mStorageRef.toString());
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
                    Log.d(TAG_f, "list: upload state: " + state.toString());
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
                    Log.d(TAG_f, "list: Instance Success: " + state);
                }
            });
        }
    }


    /** 人臉辨識 */
    /**
     * Permissions Handler
     */
    private void getPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{CAMERA_PERMISSION}, PERMISSION_CODE);
    }

    /**
     * Setup camera & use cases
     */
    private void startCamera() {
        if (ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
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
        //builder.setTargetRotation(getRotation());

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
//        builder.setTargetRotation(getRotation());

        analysisUseCase = builder.build();
        analysisUseCase.setAnalyzer(cameraExecutor, this::analyze);

        try {
            cameraProvider
                    .bindToLifecycle(this, cameraSelector, analysisUseCase);
        } catch (Exception e) {
            Log.e(TAG, "Error when bind analysis", e);
        }
    }

//    protected int getRotation() {
////            throws NullPointerException {
//        System.out.println("list:2 getRotation");
//
//        return previewView.getDisplay().getRotation();
//    }

//    public InputImage downloadImage(InputImage inputImage2, StorageReference pathReference, File localFile) {
//        System.out.println("list:2 downloadImage");
//
////        FirebaseOptions opts = FirebaseApp.getInstance().getOptions();
////        Log.d(TAG_f, "list: Bucket = " + opts.getStorageBucket());
//        //list: Bucket = temirobot-1.appspot.com
//        ///b/temirobot-1.appspot.com/o/images
//
////        // Create a storage reference from our app
////        StorageReference storageRef = storage.getReference();
//
//        pathReference.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
//            @Override
//            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
//                Log.d(TAG_f,"list:2 download: " + taskSnapshot);
//                // Local temp file has been created
//            }
//        }).addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception exception) {
//                Log.d(TAG_f,"list:2 exception" + exception);
//                // Handle any errors
//            }
//        });
//        if (localFile.exists()) {
//            try {
//                Uri newUrl2 = Uri.fromFile(localFile);
//                String newUrl3 = newUrl2.toString();
//                Log.i(TAG_f, "list:2 newUrl3: " + newUrl3);
//                inputImage2 = InputImage.fromFilePath(getApplicationContext(), newUrl2);
//            } catch (IOException e) {
//                Log.d(TAG, String.valueOf(e));
//                System.out.println("list:2 input: " + e);
//            }
//        }
////        if (inputImage2 !=null){
////            System.out.println("list:2 inputImage1 success");
////        }else System.out.println("list:2 inputImage1 null");
//
//        return inputImage2;
//    }
//
//    /** Face detection processor */
//    @SuppressLint("UnsafeOptInUsageError")
//    private void regisAnalyze() {
//        System.out.println("list:2 regisAnalyze");
//        int i = 1;
//        String[] matrix = new String[100];
//        File localFile = null;
//        StorageReference storageRef = storage.getReference();
//        StorageReference pathReference = null;
//        // Create a storage reference from our app
//
//        matrix[1] = "莊雅婷 女士, B0844230";
//        matrix[2] = "莊雅婷女士, B0844230";
//        matrix[3] = "胡語庭 女士, B0844132";
//        matrix[4] = "胡語庭女士, B0844132";
//        matrix[5] = "陳沂璇 女士, B0844227";
//        matrix[6] = "陳沂璇女士, B0844227";
//        matrix[7] = "郭靖嫈 女士, B0844138";
//        matrix[8] = "郭靖嫈女士, B0844138";
//        matrix[9] = "施懿庭 女士, B0844219";
//        matrix[10] = "施懿庭女士, B0844219";
//        matrix[11] = "張忠謀 先生, 11";
//        matrix[12] = "湯明哲 校長, 12";
//        matrix[13] = "王永慶 先生, 13";
//        matrix[14] = "琦君 女士, 14";
//
//        for(i = 1; i <= 14; i++) {
//            String i2 = Integer.toString(i);
//            String fileName = "/" + i2 + ".jpg";
////            System.out.println("list:3 i:" + i);
//            // Create a reference with an initial file path and name
//            pathReference = storageRef.child("images").child(fileName);
//            localFile = new File(getExternalFilesDir("").getAbsolutePath() + fileName);
//            final InputImage[] inputImage2 = new InputImage[103];
////            System.out.println("list:3 i:" + i);
//            System.out.println("list:2 i:" + inputImage2[i] +",path: "+ pathReference +", localFile: "+ localFile);
//            inputImage2[i] = downloadImage(inputImage2[i], pathReference, localFile);
//            if (inputImage2[i] != null) {
//                System.out.println("list:2 inputImage: success");
//                FaceDetector faceDetector = FaceDetection.getClient();
//
//                InputImage finalInputImage2 = inputImage2[i];
//
//                File finalLocalFile = localFile;
//                faceDetector.process(finalInputImage2)
//                            .addOnSuccessListener(faces -> onSuccessListener(faces, finalInputImage2, finalLocalFile))
//                            .addOnFailureListener(e -> Log.e(TAG, "Barcode process failure", e));
////                .addOnCompleteListener(task -> image2.close());
//
//                System.out.println("list:2 embedding1-0: " + embeddings);
//                String input = matrix[i];
//                embeddings = new float[1][OUTPUT_SIZE]; //output of model will be stored in this variable
//                start = false;
//                SimilarityClassifier.Recognition result = new SimilarityClassifier.Recognition(
//                        "0", "", -1f);
//                System.out.println("list:2 result: " + result);
//                System.out.println("list:2 embedding1-1: " + embeddings);
//                result.setExtra(embeddings);
//                System.out.println("list:2 embedding1-2: " + embeddings);
//                registered.put(input, result);
//                System.out.println("list:2 registered name: " + input);
//                System.out.println("list:2 registered1: " + registered);
//                start = true;
//                } else System.out.println("list:2 inputImage null");
//        }
//    }

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
            System.out.println("list:4 y3 = " + y);
            if (y == 1) {
                uploadImage2(bitmapImage);
            }
            if (y >= 1) {
                y++;
                uploadImage(bitmapImage);
            }
        }
    }

    public void uploadImage(Bitmap bitmap) {
        Log.d(TAG_f, "list:4 uploadImage1");
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
                Log.d(TAG_f, "list:4 Upload1 is " + progress + "% done");
            }
        }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG_f, "list:4 Upload1 is paused");
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


    public void uploadImage2(Bitmap bitmap) {
        Log.d(TAG_f, "list:4 uploadImage2");
        // Create a storage reference from our app
        StorageReference storageRef = storage.getReference();

        StorageReference checkinRef = storageRef.child("images").child("patrol").child(audioname + ".jpg");

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
                Log.d(TAG_f, "list:4 Upload2 is " + progress + "% done");
            }
        }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG_f, "list:4 Upload2 is paused");
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

    /** Recognize Processor */
//    public String recognizeImage(final Bitmap bitmap, boolean regis) {
//        System.out.println("list:2 recognizeImage");
//        // set image to preview
//        previewImg.setImageBitmap(bitmap);
//
//        //Create ByteBuffer to store normalized image
//        ByteBuffer imgData = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 3 * 4);
//        imgData.order(ByteOrder.nativeOrder());
//        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
//
//        System.out.println("list:2 imgData: " + imgData);
//        //get pixel values from Bitmap to normalize
//        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
//        System.out.println("list:2 getPixels: intValues = " + intValues + ", offset = 0" + "bitmap.getWidth = " + bitmap.getWidth() + ", x = 0, y = 0, bitmap.getWidth = " + bitmap.getWidth() + "bitmap.getHeight = " + bitmap.getHeight());
//        System.out.println("list:2 imgData.rewind: " + imgData);
//        imgData.rewind();
//
//        for (int i = 0; i < INPUT_SIZE; ++i) {
//            for (int j = 0; j < INPUT_SIZE; ++j) {
//                int pixelValue = intValues[i * INPUT_SIZE + j];
//                imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
//                imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
//                imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
//            }
//        }
//        System.out.println("list:2 imgData.putfloat: " + imgData);
//        //imgData is input to our model
//        Object[] inputArray = {imgData};
//        System.out.println("list:2 inputArray: " + inputArray);
//        Map<Integer, Object> outputMap = new HashMap<>();
//        System.out.println("list:2 embedding3-0: "+embeddings);
//        embeddings = new float[1][OUTPUT_SIZE]; //output of model will be stored in this variable
//        System.out.println("list:2 embedding3-1: "+embeddings);
//        outputMap.put(0, embeddings);
//        System.out.println("list:2 embedding3-2: "+embeddings);
//        System.out.println("list:2 outputMap: " + outputMap);
//        tfLite.runForMultipleInputsOutputs(inputArray, outputMap); //Run model
//
//        float distance;
//        //Compare new face with saved Faces.
//        if (!regis){
//            if (registered.size() > 0 ) {
//                final Pair<String, Float> nearest = findNearest(embeddings[0]);//Find closest matching face
//                System.out.println("list:2 findNearest embeddings[0]: " + embeddings[0]);
//                System.out.println("list:2 findNearest embeddings: " + embeddings);
//                System.out.println("list:2 Nearest: " + nearest.first);
//                if (nearest != null) {
//                    final String name = nearest.first;
//                    distance = nearest.second;
//                    if (distance < 1.000f) //If distance between Closest found face is more than 1.000 ,then output UNKNOWN face.
//                        return name;
////                    else
////                        return "unknown";
//                }
//            }
//        }
//        return null;
//    }

//    //Compare Faces by distance between face embeddings
//    private Pair<String, Float> findNearest(float[] emb) {
//        System.out.println("list:2 findNearest");
//        Pair<String, Float> ret = null;
//        for (Map.Entry<String, SimilarityClassifier.Recognition> entry : registered.entrySet()) {
//            final String name = entry.getKey();
//            final float[] knownEmb = ((float[][]) entry.getValue().getExtra())[0];
//            float distance = 0;
//            for (int i = 0; i < emb.length; i++) {
//                float diff = emb[i] - knownEmb[i];
//                distance += diff*diff;
//            }
//            distance = (float) Math.sqrt(distance);
//            if (ret == null || distance < ret.second) {
//                ret = new Pair<>(name, distance);
//            }
//        }
//        System.out.println("list:2 findNearest ret: "+ ret);
//        return ret;
//    }


    /** Recognize Processor */
    /**
     * Bitmap Converter
     *
     * @return
     */
    private Bitmap mediaImgToBmp(InputImage image2, int rotation, Rect boundingBox) {
        System.out.println("list:2 mediaImgToBmp");
        System.out.println("list:2 mediaImgToBmp image: " + image2);
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
        int ySize = width * height;
        int uvSize = width * height / 4;

        byte[] nv21 = new byte[ySize + uvSize * 2];

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

        assert (image.getPlanes()[0].getPixelStride() == 1);

        int pos = 0;

        if (rowStride == width) { // likely
            yBuffer.get(nv21, 0, ySize);
            pos += ySize;
        } else {
            long yBufferPos = -rowStride; // not an actual position
            for (; pos < ySize; pos += width) {
                yBufferPos += rowStride;
                yBuffer.position((int) yBufferPos);
                yBuffer.get(nv21, pos, width);
            }
        }
        rowStride = image.getPlanes()[2].getRowStride();
        int pixelStride = image.getPlanes()[2].getPixelStride();
        assert (rowStride == image.getPlanes()[1].getRowStride());
        assert (pixelStride == image.getPlanes()[1].getPixelStride());

        if (pixelStride == 2 && rowStride == width && uBuffer.get(0) == vBuffer.get(1)) {
            // maybe V an U planes overlap as per NV21, which means vBuffer[1] is alias of uBuffer[0]
            byte savePixel = vBuffer.get(1);
            try {
                vBuffer.put(1, (byte) ~savePixel);
                if (uBuffer.get(0) == (byte) ~savePixel) {
                    vBuffer.put(1, savePixel);
                    vBuffer.position(0);
                    uBuffer.position(0);
                    vBuffer.get(nv21, ySize, 1);
                    uBuffer.get(nv21, ySize + 1, uBuffer.remaining());

                    return nv21; // shortcut
                }
            } catch (ReadOnlyBufferException ex) {
                // unfortunately, we cannot check if vBuffer and uBuffer overlap
            }

            // unfortunately, the check failed. We must save U and V pixel by pixel
            vBuffer.put(1, savePixel);
        }

        // other optimizations could check if (pixelStride == 1) or (pixelStride == 2),
        // but performance gain would be less significant

        for (int row = 0; row < height / 2; row++) {
            for (int col = 0; col < width / 2; col++) {
                int vuPos = col * pixelStride + row * rowStride;
                nv21[pos++] = vBuffer.get(vuPos);
                nv21[pos++] = uBuffer.get(vuPos);
            }
        }

        return nv21;
    }

    private Bitmap toBitmap(Image image) {
        System.out.println("list:2 toBitmap");

        byte[] nv21 = YUV_420_888toNV21(image);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();

        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }
//
//    /** Model loader */
//    @SuppressWarnings("deprecation")
//    private void loadModel() {
//        System.out.println("list:2 loadModel");
//        try {
//            //model name
//            String modelFile = "mobile_face_net.tflite";
//            tfLite = new Interpreter(loadModelFile(MainActivity.this, modelFile));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private MappedByteBuffer loadModelFile(Activity activity, String MODEL_FILE) throws IOException {
//        System.out.println("list:2 loadModelFile");
//        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_FILE);
//        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
//        FileChannel fileChannel = inputStream.getChannel();
//        long startOffset = fileDescriptor.getStartOffset();
//        long declaredLength = fileDescriptor.getDeclaredLength();
//        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
//    }
}