package tw.com.temirobot;

import android.app.AlertDialog;
import android.content.Intent;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.robotemi.sdk.Robot;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class FaceRecognition extends AppCompatActivity {
    private static Robot robot; //temi sdk宣告
    private static final String TAG = "FaceRecognition"; //log智能報到標記
    private static final int PERMISSION_CODE = 1001; //相機授權碼宣告
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA; //相機授權宣告
    private PreviewView previewView; //相機預覽畫面宣告
    private CameraSelector cameraSelector; //前後相機選擇宣告
    private ProcessCameraProvider cameraProvider; //相機提供宣告
    private int lensFacing = CameraSelector.LENS_FACING_BACK; //後相機宣告
    private Preview previewUseCase; //預覽畫面綁定宣告
    private ImageAnalysis analysisUseCase; //畫面分析綁定宣告
    private TextView txtdetect; //UI偵測結果文字宣告
    private int y = 0; //上傳圖片變數宣告

    //    private final HashMap<String, SimilarityClassifier.Recognition> registered = new HashMap<>(); //saved Faces //舊安卓端即時臉部辨識用(註冊用)(沒用到)
    private boolean flipX = false; //畫面旋轉宣告

    private static FirebaseStorage storage; //firebase storage宣告
    private StorageReference mStorageRef; //firebase storage 檔案位址宣告
    private DatabaseReference mDatabase; //firebase 即時資料庫宣告
    private static final String TAG_f = "Firebase"; //log firebase標記

    @Override
    protected void onCreate(Bundle savedInstanceState) { //安卓Activity生命週期onCreate
        super.onCreate(savedInstanceState); //執行生命週期onCreate
        setContentView(R.layout.activity_face_recognition); //綁定智能報到ui畫面

        System.out.println("list:3 FaceRecognition");
        y = 1; //上傳圖片變數初始值

        robot = Robot.getInstance(); //temi sdk 引用
        mStorageRef = FirebaseStorage.getInstance().getReference(); //firebase storage api 引用(監聽上傳狀態用)
        mDatabase = FirebaseDatabase.getInstance().getReference(); //firebase 即時資料庫 api 引用
        storage = FirebaseStorage.getInstance(); //firebase storage api 引用

        mDatabase.child("face").child("temi1").child("checkin").child("id").setValue(""); //以下為臉部辨識firebase變數初始化, 設定為temi1
        mDatabase.child("face").child("temi1").child("checkin").child("py").setValue(true);
        mDatabase.child("face").child("temi1").child("checkin").child("and").setValue(false);
        mDatabase.child("face").child("temi1").child("welcome").child("py").setValue(false);
        mDatabase.child("face").child("temi1").child("welcome").child("and").setValue(false);
        mDatabase.child("face").child("temi1").child("regis").child("py").setValue(false);
        mDatabase.child("face").child("temi1").child("regis").child("and").setValue(false);
        mDatabase.child("face").child("temi1").child("patrol").child("py").setValue(false);
        mDatabase.child("face").child("temi1").child("patrol").child("and").setValue(false);

        previewView = findViewById(R.id.previewView); //ui 預覽畫面綁定
        previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER); //ui 預覽畫面位置綁定

        txtdetect = findViewById(R.id.detection_text); //UI偵測結果文字綁定
        txtdetect.setText("臉部辨識中，請等候三秒"); //UI偵測結果文字
    }

    @Override
    protected void onStart() {
        super.onStart();
    } //安卓Activity生命週期onStart, 執行生命週期onStart

    @Override
    public void onStop()
    {
        super.onStop();
    } //安卓Activity生命週期onStop, 執行生命週期onStop

    @Override
    protected void onResume() { //安卓Activity生命週期onResume
        super.onResume(); //執行生命週期onResume
        startCamera(); //開啟相機方法呼叫
        //以下註解的部分移到FaceRecognition2頁面onResume週期
//        DatabaseReference myRef1 = database.getReference("/face/temi1/checkin/id");
//        myRef1.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                // This method is called once with the initial value and again
//                // whenever data at this location is updated.
//                String value1 = dataSnapshot.getValue(String.class);
//                if (value1 != "Unknown") {
//                    mDatabase.child("face").child("temi1").child("checkin").child("py").setValue(false);
//                    mDatabase.child("face").child("temi1").child("checkin").child("and").setValue(true);
//                    AlertDialog.Builder builder = new AlertDialog.Builder(FaceRecognition.this);
//                    builder.setTitle("請問是否要註冊照片?");
//
//                    // Set up the buttons
//                    builder.setPositiveButton("是", (dialog, which) -> {
//                        Intent it = new Intent(FaceRecognition.this,Regis.class);
//                        startActivity(it);
//                        finish();
//                    });
//                    builder.setNegativeButton("否", (dialog, which) -> {
//                        dialog.cancel();
//                        startCamera();
//                    });
//                    builder.show();
//                }
//                else if (value1 == "Failed"){
//                    mDatabase.child("face").child("temi1").child("checkin").child("py").setValue(true);
//                    mDatabase.child("face").child("temi1").child("checkin").child("and").setValue(false);
//                    startCamera();
//                }
//                else if (value1 == "null"){
//                    mDatabase.child("face").child("temi1").child("checkin").child("py").setValue(true);
//                    mDatabase.child("face").child("temi1").child("checkin").child("and").setValue(false);
//                }
//                else {
//                    mDatabase.child("face").child("temi1").child("checkin").child("py").setValue(false);
//                    mDatabase.child("face").child("temi1").child("checkin").child("and").setValue(true);
//                    Intent it = new Intent(FaceRecognition.this,Todo.class);
//                    startActivity(it);
//                    finish();
//                }
//                Log.d("TAG", "Value1 is: " + value1);
//            }
//
//            @Override
//            public void onCancelled(DatabaseError error) {
//                // Failed to read value
//                Log.w("TAG", "Failed to read value.", error.toException());
//            }
//        });
    }

    public void btnhome(View v){ //按下首頁按鈕
        Intent it = new Intent(FaceRecognition.this,MainActivity.class); //跳到首頁
        startActivity(it); //開始下一個頁面生命週期
        finish(); //結束此頁面
    }

    /** 人臉辨識 */
    /**
     * Permissions Handler
     */
    private void getPermissions() { //取得授權方法
        ActivityCompat.requestPermissions(this, new String[]{CAMERA_PERMISSION}, PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] grantResults) { //授權結果
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        if (requestCode == PERMISSION_CODE) {
            setupCamera();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * Setup camera & use cases
     */
    private void startCamera() { //開啟相機方法
        if (ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            setupCamera();
            System.out.println("list:2 startCamera1");
        } else {
            getPermissions();
            System.out.println("list:2 startCamera2");
        }
    }

    private void setupCamera() { //設置相機方法
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

    private void bindAllCameraUseCases() { //相機綁定方法
        System.out.println("list:2 bindAllCameraUseCases");

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            bindPreviewUseCase();
            bindAnalysisUseCase();
        }
    }

    private void bindPreviewUseCase() { //預覽畫面綁定方法
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

    private void bindAnalysisUseCase() { //預覽畫面分析綁定方法
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

    protected int getRotation() throws NullPointerException { //預覽畫面角度旋轉方法(沒用到)
        System.out.println("list:2 getRotation");
        return previewView.getDisplay().getRotation();
    }

    public void uploadImage(Bitmap bitmap) { //firebase上傳圖片方法(只要偵測到人臉就拍照)
        Log.d(TAG_f, "list: upload");
        // Create a storage reference from our app
        StorageReference storageRef = storage.getReference();

        StorageReference checkinRef = storageRef.child("images").child("unknown").child("unknown1.jpg"); //firebase圖片位址, 檔名固定為unknown1.jpg

        ByteArrayOutputStream baos = new ByteArrayOutputStream(); //bitmap轉換成byte
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();
        UploadTask uploadTask = checkinRef.putBytes(data);

        // Observe state change events such as progress, pause, and resume
        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) { //firebase api上傳進度
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                Log.d(TAG_f, "list: Upload is " + progress + "% done"); //log輸出上傳進度
                if(progress >= 100){ //如果進度>=100%
                    Intent it = new Intent(FaceRecognition.this,FaceRecognition2.class); //跳至FaceRecognition2頁面
                    startActivity(it); //開始下一個頁面生命週期
                    finish(); //結束此頁面
                }
            }
        }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() { //firebase api上傳暫停
            @Override
            public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG_f, "list: Upload is paused");
            }
        });

        uploadTask.addOnFailureListener(new OnFailureListener() { //firebase api上傳失敗
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() { //firebase api上傳成功
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                // ...
            }
        });
    }

    //firebase上傳狀態
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        System.out.println("list:2 onSaveInstanceState");

        // If there's a download in progress, save the reference so you can query it later
        if (mStorageRef != null) {
            outState.putString("reference", mStorageRef.toString());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        System.out.println("list:2 onRestoreInstanceState");

        // If there was a download in progress, get its reference and create a new StorageReference
        final String stringRef = savedInstanceState.getString("reference");
        if (stringRef == null) {
            return;
        }
        mStorageRef = FirebaseStorage.getInstance().getReferenceFromUrl(stringRef);

        // Find all DownloadTasks under this StorageReference (in this example, there should be one)
        List<FileDownloadTask> tasks = mStorageRef.getActiveDownloadTasks();
        if (tasks.size() > 0) {
            // Get the task monitoring the download
            FileDownloadTask task = tasks.get(0);

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

    @SuppressLint("UnsafeOptInUsageError")
    private void analyze(@NonNull ImageProxy image) { //分析(傳入預覽畫面照片)
        System.out.println("list:2 analyze");
        if (image.getImage() == null) return;

        InputImage inputImage = InputImage.fromMediaImage( //照片與角度轉換
                image.getImage(),
                image.getImageInfo().getRotationDegrees()
        );

        FaceDetector faceDetector = FaceDetection.getClient(); //開啟偵測人臉

        faceDetector.process(inputImage) //偵測人臉(輸入照片)
                .addOnSuccessListener(faces -> onSuccessListener(faces, inputImage))
                .addOnFailureListener(e -> Log.e(TAG, "Barcode process failure", e))
                .addOnCompleteListener(task -> image.close());
    }

    private void onSuccessListener(List<Face> faces, InputImage inputImage) {  //偵測人臉成功監聽器
        System.out.println("list:2 onSuccessListener"); //log輸出現在執行偵測人臉成功監聽器
        Rect boundingBox = null; //方框宣告
        //String name = null;
        //float scaleX = (float) previewView.getWidth() / (float) inputImage.getHeight();
        //float scaleY = (float) previewView.getHeight() / (float) inputImage.getWidth();

        if (faces.size() > 0) { //如果大於0張臉

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
            if (y == 2) { //如果上傳圖片變數==2, 避免不斷上傳圖片
                uploadImage(bitmapImage); //上傳圖片方法呼叫
            }
            y++;
        }
    }

    /** Recognize Processor */
    /**
     * Bitmap Converter
     *
     * @return
     */
    private Bitmap mediaImgToBmp(InputImage image2, int rotation, Rect boundingBox) { //照片轉bitmap方法
        System.out.println("list:2 mediaImgToBmp");
        System.out.println("list:2 mediaImgToBmp image: " + image2);
        Bitmap frame_bmp1 = null;

        Image image = image2.getMediaImage();
        //Convert media image to Bitmap
        Bitmap frame_bmp = toBitmap(image);
        //Adjust orientation of Face
        frame_bmp1 = rotateBitmap(frame_bmp, rotation, flipX);

//        //Crop out bounding box from whole Bitmap(image)
//        float padding = 0.0f;
//        RectF adjustedBoundingBox = new RectF(
//                boundingBox.left - padding,
//                boundingBox.top - padding,
//                boundingBox.right + padding,
//                boundingBox.bottom + padding);
//        Bitmap cropped_face = getCropBitmapByCPU(frame_bmp1, adjustedBoundingBox);
//        //Resize bitmap to 112,112
//        return getResizedBitmap(cropped_face);
        return frame_bmp1;
    }

    private static Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees, boolean flipX) { //bitmap角度計算與旋轉
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

    private static byte[] YUV_420_888toNV21(Image image) { //照片byte緩衝方法
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

    private Bitmap toBitmap(Image image) { //照片轉bitmap方法
        System.out.println("list:2 toBitmap");

        byte[] nv21 = YUV_420_888toNV21(image);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();

        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

}