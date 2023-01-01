package tw.com.temirobot;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.robotemi.sdk.Robot;
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener;
import com.robotemi.sdk.listeners.OnRobotReadyListener;
import com.robotemi.sdk.navigation.listener.OnCurrentPositionChangedListener;
import com.robotemi.sdk.navigation.model.Position;
import com.robotemi.sdk.navigation.model.SpeedLevel;

import org.jetbrains.annotations.NotNull;

public class FaceRecognition2 extends AppCompatActivity {
    private DatabaseReference mDatabase;
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private int y = 0; //讀取firebase辨識結果次數變數宣告
    private TextView txtdetect; //ui辨識狀態文字宣告

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_recognition2);

        System.out.println("list:3 FaceRecognition2");

        mDatabase = FirebaseDatabase.getInstance().getReference();

        txtdetect = findViewById(R.id.detection_text); //ui辨識狀態文字綁定
//        txtdetect.setText("臉部辨識中，請等候三秒");
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() { //onResume生命週期會持續, 有迴圈的效果
        super.onResume();
        System.out.println("list:3 y = " + y); //log輸出讀取firebase辨識結果次數變數
        if (y < 10){ //如果讀取firebase辨識結果次數 < 10次
            DatabaseReference myRef1 = database.getReference("/face/temi1/checkin/id"); //firebase辨識結果位址
            myRef1.addValueEventListener(new ValueEventListener() { //讀取firebase辨識結果
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) { //firebase 回傳值與初始值有變化時
                    // This method is called once with the initial value and again
                    // whenever data at this location is updated.
                    String value1 = dataSnapshot.getValue(String.class); //字串firebase辨識結果讀取
                    if (value1.trim().equals("Unknown")) { //如果辨識結果回傳unknown, 查無此人
                        y = 10; //不再讀取firebase辨識結果
                        //查無此人
                        mDatabase.child("face").child("temi1").child("checkin").child("py").setValue(false); //臉部辨識 firebase 變數 報到狀態python端布林值關閉
                        mDatabase.child("face").child("temi1").child("checkin").child("and").setValue(false); //臉部辨識 firebase 變數 報到狀態安卓端布林值關閉
                        final AlertDialog.Builder builder = new AlertDialog.Builder(FaceRecognition2.this); //彈跳視窗
                        builder.setTitle("請問是否要註冊照片?");

                        // Set up the buttons
                        builder.setPositiveButton("是", (dialog, which) -> {
                            Intent it = new Intent(FaceRecognition2.this,Regis.class); //跳至照片註冊頁面
                            startActivity(it); //開始下一個頁面生命週期
                            finish(); //結束此頁面
                        });
                        builder.setNegativeButton("否", (dialog, which) -> {
                            dialog.cancel();
                            Intent it = new Intent(FaceRecognition2.this,FaceRecognition.class); //跳回最初報到頁面
                            startActivity(it); //開始下一個頁面生命週期
                            finish(); //結束此頁面
                        });
                        builder.show();
                    }
                    else if (value1.trim().equals("Failed")){ //如果辨識結果回傳Failed, 辨識失敗
                        y = 10; //不再讀取firebase辨識結果
                        //辨識失敗
                        mDatabase.child("face").child("temi1").child("checkin").child("py").setValue(true); //臉部辨識 firebase 變數 報到狀態python端布林值開啟
                        mDatabase.child("face").child("temi1").child("checkin").child("and").setValue(false); //臉部辨識 firebase 變數 報到狀態安卓端布林值關閉
                        Intent it = new Intent(FaceRecognition2.this,FaceRecognition.class); //跳回最初報到頁面
                        startActivity(it); //開始下一個頁面生命週期
                        finish(); //結束此頁面
                    }
                    else if (value1.trim().length() == 0){
                        //尚未辨識完成
                        y++;
                    }
                    else {
                        y = 10; //不再讀取firebase辨識結果
                        //辨識到人
                        mDatabase.child("face").child("temi1").child("checkin").child("py").setValue(false); //臉部辨識 firebase 變數 報到狀態python端布林值開啟
                        mDatabase.child("face").child("temi1").child("checkin").child("and").setValue(false); //臉部辨識 firebase 變數 報到狀態安卓端布林值關閉
                        Intent it = new Intent();
                        it.setClass(FaceRecognition2.this, Todo.class); //跳至Todo頁面
                        Bundle bundle = new Bundle();
                        bundle.putString("id", value1); //辨識到的id傳值到Todo頁面
                        it.putExtras(bundle);   // put進去，不然資料不會帶過去
                        startActivity(it);
                        finish();
                    }
                    Log.d("TAG", "Value1 is: " + value1);
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Failed to read value
                    Log.w("TAG", "Failed to read value.", error.toException());
                }
            });
        }
    }

    public void btnhome(View v){
        Intent it = new Intent(FaceRecognition2.this,MainActivity.class);
        startActivity(it);
        finish();
    }

}