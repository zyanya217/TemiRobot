package tw.com.temirobot;

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
import android.util.Log;
import android.util.Pair;
import android.view.View;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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
    private static final String TAG = "FaceRecognition";
    private static final int PERMISSION_CODE = 1001;
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private PreviewView previewView;
    private CameraSelector cameraSelector;
    private ProcessCameraProvider cameraProvider;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private Preview previewUseCase;
    private ImageAnalysis analysisUseCase;
    private GraphicOverlay graphicOverlay;
    private ImageView previewImg;
    private TextView detectionTextView;


    private final HashMap<String, SimilarityClassifier.Recognition> registered = new HashMap<>(); //saved Faces
    private Interpreter tfLite;
    private boolean flipX = false;
    private boolean start = true;
    private boolean regis = false;
    private float[][] embeddings;
    private int j = 1;

    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;
    private static final int INPUT_SIZE = 112;
    private static final int OUTPUT_SIZE=192;

    private static FirebaseStorage storage;
    private StorageReference mStorageRef;
    private DatabaseReference mDatabase;
    private static final String TAG_f = "Firebase";

    private final static int sendUser = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_face_recognition);
        previewView = findViewById(R.id.previewView);
        previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
        graphicOverlay = findViewById(R.id.graphic_overlay);
        previewImg = findViewById(R.id.preview_img);
        detectionTextView = findViewById(R.id.detection_text);

        mStorageRef = FirebaseStorage.getInstance().getReference();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        storage = FirebaseStorage.getInstance();

        loadModel();
    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onStop()
    {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        for (j = 1; j<= 1;j++) {
//            regisAnalyze();
//        }
//        if (j >= 2){
//            j = 2;
//        }
        startCamera();
    }

    public void btnhome(View v){
        Intent it = new Intent(FaceRecognition.this,MainActivity.class);
        startActivity(it);
        finish();
    }

    /** 人臉辨識 */
    /** Permissions Handler */
    private void getPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{CAMERA_PERMISSION}, PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] grantResults) {
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

    public InputImage downloadImage(StorageReference pathReference, File localFile) {
        System.out.println("list:2 downloadImage");

//        FirebaseOptions opts = FirebaseApp.getInstance().getOptions();
//        Log.d(TAG_f, "list: Bucket = " + opts.getStorageBucket());
        //list: Bucket = temirobot-1.appspot.com
        ///b/temirobot-1.appspot.com/o/images

//        // Create a storage reference from our app
//        StorageReference storageRef = storage.getReference();

        InputImage inputImage2 = null;

        pathReference.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG_f,"list:2 download: " + taskSnapshot);
                // Local temp file has been created
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d(TAG_f,"list:2 exception" + exception);
                // Handle any errors
            }
        });
        if (localFile.exists()) {
            try {
                Uri newUrl2 = Uri.fromFile(localFile);
                String newUrl3 = newUrl2.toString();
                Log.i(TAG_f, "list:2 newUrl3: " + newUrl3);
                inputImage2 = InputImage.fromFilePath(getApplicationContext(), newUrl2);
            } catch (IOException e) {
                Log.d(TAG, String.valueOf(e));
                System.out.println("list:2 input: " + e);
            }
        }
        if (inputImage2 !=null){
            System.out.println("list:2 3-3: success");
        }else System.out.println("list:2 3-3: null");

        return inputImage2;
    }

    public void uploadImage(Bitmap bitmap){
        // Create a storage reference from our app
        StorageReference storageRef = storage.getReference();

        StorageReference checkinRef = storageRef.child("images").child("unknown").child("checkin.jpg");

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
                    Log.d(TAG_f,"list: Instance Success: " + state);
                }
            });
        }
    }

    /** Face detection processor */
    @SuppressLint("UnsafeOptInUsageError")
    private void regisAnalyze(int i) {
        System.out.println("list:2 regisAnalyze");
        int[] y = {0,1,2,5,6,11,12,13,14};
//        int i = 1;
        String[] matrix = new String[100];
        StorageReference storageRef = storage.getReference();
        StorageReference pathReference = null;
        // Create a storage reference from our app

        matrix[1] = "莊雅婷 女士, B0844230";
        matrix[2] = "莊雅婷女士, B0844230";
        matrix[3] = "胡語庭 女士, B0844132";
        matrix[4] = "胡語庭女士, B0844132";
        matrix[5] = "陳沂璇 女士, B0844227";
        matrix[6] = "陳沂璇女士, B0844227";
        matrix[7] = "郭靖嫈 女士, B0844138";
        matrix[8] = "郭靖嫈女士, B0844138";
        matrix[9] = "施懿庭 女士, B0844219";
        matrix[10] = "施懿庭女士, B0844219";
        matrix[11] = "張忠謀 先生, 11";
        matrix[12] = "湯明哲 校長, 12";
        matrix[13] = "王永慶 先生, 13";
        matrix[14] = "琦君 女士, 14";

//        for(i = 1; i <= 8; i++) {
            String i2 = Integer.toString(y[i]);
            String fileName = "/" + i2 + ".jpg";
            System.out.println("list:2 y[i]:" + y[i] + ",i: " + i);
            // Create a reference with an initial file path and name
            pathReference = storageRef.child("images").child(fileName);
            File localFile = new File(getExternalFilesDir("").getAbsolutePath() + fileName);
//            final InputImage[] inputImage2 = new InputImage[10];
            InputImage inputImage3 = downloadImage(pathReference, localFile);
            System.out.println("list:2 path: "+ pathReference +", localFile: "+ localFile);
//            inputImage2[y[i]] = downloadImage(inputImage2[y[i]], pathReference, localFile);
//            System.out.println("list:2 inputImage: " + inputImage2[y[i]]);
            if (inputImage3 != null) {
//                System.out.println("list:2 inputImage: success");
                embeddings = new float[1][OUTPUT_SIZE]; //output of model will be stored in this variable

                FaceDetector faceDetector = FaceDetection.getClient();

                faceDetector.process(inputImage3)
                        .addOnSuccessListener(faces -> onSuccessListener(faces, inputImage3, localFile, embeddings))
                        .addOnFailureListener(e -> Log.e(TAG, "Barcode process failure", e));
//                .addOnCompleteListener(task -> image2.close());

                System.out.println("list:2 embedding1-0: " + embeddings);
                String input = matrix[y[i]];
//                start = false;
                SimilarityClassifier.Recognition result = new SimilarityClassifier.Recognition(
                        i2, input, -1f);
                System.out.println("list:2 result: " + result);
                System.out.println("list:2 embedding1-1: " + embeddings);
                result.setExtra(embeddings);
                System.out.println("list:2 embedding1-2: " + embeddings);
                registered.put(input, result);
                System.out.println("list:2 registered name: " + input);
                System.out.println("list:2 registered1: " + registered);
//                start = true;
            } else System.out.println("list:2 inputImage null");
//        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void analyze(@NonNull ImageProxy image) {
        System.out.println("list:2 analyze");
            if (image.getImage() == null) return;

            InputImage inputImage = InputImage.fromMediaImage(
                    image.getImage(),
                    image.getImageInfo().getRotationDegrees()
            );

            FaceDetector faceDetector = FaceDetection.getClient();

            faceDetector.process(inputImage)
                    .addOnSuccessListener(faces -> onSuccessListener(faces, inputImage, null, null))
                    .addOnFailureListener(e -> Log.e(TAG, "Barcode process failure", e))
                    .addOnCompleteListener(task -> image.close());
    }

    private void onSuccessListener(List<Face> faces, InputImage inputImage, File localFile, float[][] embeddings) {
        System.out.println("list:2 onSuccessListener");
        if (localFile!=null){
//            System.out.println("list:2 onSuccessListener3: " + inputImage.getMediaImage());
            Rect boundingBox = null;
            String name = null;
            float scaleX = (float) inputImage.getWidth() / (float) inputImage.getHeight();
            float scaleY = (float) inputImage.getHeight() / (float) inputImage.getWidth();

            if(faces.size() > 0) {
                // get first face detected
                Face face = faces.get(0);

                // get bounding box of face;
                boundingBox = face.getBoundingBox();

                // convert img to bitmap & crop img
                Bitmap bitmap = mediaImgToBmp(
                        inputImage,
                        inputImage.getRotationDegrees(),
                        boundingBox,localFile);
                if (start) name = recognizeImage(bitmap,true, embeddings);
            }
            graphicOverlay.draw(boundingBox, scaleX, scaleY, name);
        }
        else {
            Rect boundingBox = null;
            String name = null;
            float scaleX = (float) previewView.getWidth() / (float) inputImage.getHeight();
            float scaleY = (float) previewView.getHeight() / (float) inputImage.getWidth();

            if (faces.size() > 0) {
                //detectionTextView.setText("偵測到人臉");
                // get first face detected
                Face face = faces.get(0);

                // get bounding box of face;
                boundingBox = face.getBoundingBox();

                // convert img to bitmap & crop img
                Bitmap bitmap = mediaImgToBmp(
                        inputImage,
                        inputImage.getRotationDegrees(),
                        boundingBox,null);
                System.out.println("list:2 onSuccessListener4: "+ inputImage.getMediaImage());
                System.out.println("list:2 bitmap4: "+ bitmap);

                uploadImage(bitmap);

                if (start) name = recognizeImage(bitmap,false, null);
                if (name != null) {
                    System.out.println("list:2 name: "+ name);
//                    Intent it = new Intent(FaceRecognition.this,Todo.class);
//                    // 放入需要傳遞的物件
//                    it.putExtra("key", name);
//                    // 啟動意圖(意圖，請求碼(int)) 請求碼最好使用 final static定義 方便識別
//                    startActivityForResult(it, sendUser);
//                    startActivity(it);
//                    finish();
                }
            } else {
                detectionTextView.setText("未偵測到人臉");
            }
            graphicOverlay.draw(boundingBox, scaleX, scaleY, name);
        }
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        // 如果請求碼為 sendUser 返回碼 為 RESULT_OK RESULT_OK為系統自定義的int值為 -1
//        if (requestCode == sendUser && resultCode == RESULT_OK) {
//            // 在TextView中設定返回資訊
//            System.out.println("list:2 傳遞訊息: "+data.getStringExtra("key"));
//        }
//    }

    /** Recognize Processor */
    public String recognizeImage(final Bitmap bitmap, boolean regis, float[][] embeddings) {
        System.out.println("list:2 recognizeImage");
        // set image to preview
        previewImg.setImageBitmap(bitmap);

        //Create ByteBuffer to store normalized image
        ByteBuffer imgData = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 3 * 4);
        imgData.order(ByteOrder.nativeOrder());
        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];

        //get pixel values from Bitmap to normalize
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        System.out.println("list:2 getResizedBitmap int: " + intValues );
        imgData.rewind();

        for (int i = 0; i < INPUT_SIZE; ++i) {
            for (int j = 0; j < INPUT_SIZE; ++j) {
                int pixelValue = intValues[i * INPUT_SIZE + j];
                imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
            }
        }
        //imgData is input to our model
        Object[] inputArray = {imgData};
        System.out.println("list:2 imgData: " + imgData + ", inputArray: "+inputArray);
        Map<Integer, Object> outputMap = new HashMap<>();
        System.out.println("list:2 embedding3-0: "+embeddings);
        if (!regis){
        embeddings = new float[1][OUTPUT_SIZE]; //output of model will be stored in this variable
        }
        System.out.println("list:2 embedding3-1: "+embeddings);
        outputMap.put(0, embeddings);
        System.out.println("list:2 embedding3-2: "+embeddings);
        tfLite.runForMultipleInputsOutputs(inputArray, outputMap); //Run model

        float distance;
        //Compare new face with saved Faces.
        if (!regis){
            if (registered.size() > 0 ) {
                final Pair<String, Float> nearest = findNearest(embeddings[0]);//Find closest matching face
                if (nearest != null) {
                    final String name = nearest.first;
                    distance = nearest.second;
                    if (distance < 1.000f) //If distance between Closest found face is more than 1.000 ,then output UNKNOWN face.
                        return name;
//                    else
//                        return "unknown";
                }
            }
        }
        return null;
    }

    //Compare Faces by distance between face embeddings
    private Pair<String, Float> findNearest(float[] emb) {
        System.out.println("list:2 findNearest");
        Pair<String, Float> ret = null;
        for (Map.Entry<String, SimilarityClassifier.Recognition> entry : registered.entrySet()) {
            final String name = entry.getKey();
            final float[] knownEmb = ((float[][]) entry.getValue().getExtra())[0];
            float distance = 0;
            for (int i = 0; i < emb.length; i++) {
                float diff = emb[i] - knownEmb[i];
                distance += diff*diff;
            }
            distance = (float) Math.sqrt(distance);
            if (ret == null || distance < ret.second) {
                ret = new Pair<>(name, distance);
            }
        }
        return ret;
    }

    /** Bitmap Converter
     * @return*/
    private Bitmap mediaImgToBmp(InputImage image2, int rotation, Rect boundingBox, File localFile2) {
        System.out.println("list:2 mediaImgToBmp");
        System.out.println("list:2 mediaImgToBmp image: " +image2);
        Bitmap frame_bmp1 = null;
        if (localFile2!=null){
//            File localFile = new File(getExternalFilesDir("").getAbsolutePath()+"/president.jpg");
            frame_bmp1 = BitmapFactory.decodeFile(localFile2.getAbsolutePath());
        }else {
            Image image = image2.getMediaImage();
            //Convert media image to Bitmap
            Bitmap frame_bmp = toBitmap(image);

            //Adjust orientation of Face
            frame_bmp1 = rotateBitmap(frame_bmp, rotation, flipX);
        }
        //Crop out bounding box from whole Bitmap(image)
        float padding = 0.0f;
        RectF adjustedBoundingBox = new RectF(
                boundingBox.left - padding,
                boundingBox.top - padding,
                boundingBox.right + padding,
                boundingBox.bottom + padding);
        Bitmap cropped_face = getCropBitmapByCPU(frame_bmp1, adjustedBoundingBox);
        //Resize bitmap to 112,112
        return getResizedBitmap(cropped_face);
    }

    private Bitmap getResizedBitmap(Bitmap bm) {
        System.out.println("list:2 getResizedBitmap");
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) 112) / width;
        float scaleHeight = ((float) 112) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);
        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    private static Bitmap getCropBitmapByCPU(Bitmap source, RectF cropRectF) {
        System.out.println("list:2 getCropBitmapByCPU");
        Bitmap resultBitmap = Bitmap.createBitmap((int) cropRectF.width(),
                (int) cropRectF.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(resultBitmap);

        // draw background
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        paint.setColor(Color.WHITE);
        canvas.drawRect(//from  w w  w. ja v  a  2s. c  om
                new RectF(0, 0, cropRectF.width(), cropRectF.height()),
                paint);

        Matrix matrix = new Matrix();
        matrix.postTranslate(-cropRectF.left, -cropRectF.top);

        canvas.drawBitmap(source, matrix, paint);

        if (source != null && !source.isRecycled()) {
            source.recycle();
        }

        return resultBitmap;
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

    /** Model loader */
    @SuppressWarnings("deprecation")
    private void loadModel() {
        System.out.println("list:2 loadModel");
        try {
            //model name
            String modelFile = "mobile_face_net.tflite";
            tfLite = new Interpreter(loadModelFile(FaceRecognition.this, modelFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private MappedByteBuffer loadModelFile(Activity activity, String MODEL_FILE) throws IOException {
        System.out.println("list:2 loadModelFile");
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

}