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
    private int y = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_recognition2);

        System.out.println("list:3 FaceRecognition2");

        mDatabase = FirebaseDatabase.getInstance().getReference();
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
    protected void onResume() {
        super.onResume();
        if (y < 10){
            DatabaseReference myRef1 = database.getReference("/face/temi1/checkin/id");
            myRef1.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // This method is called once with the initial value and again
                    // whenever data at this location is updated.
                    String value1 = dataSnapshot.getValue(String.class);
                    if (value1 == "Unknown") {
                        y = 10;
                        //查無此人
                        mDatabase.child("face").child("temi1").child("checkin").child("py").setValue(false);
                        mDatabase.child("face").child("temi1").child("checkin").child("and").setValue(true);
                        final AlertDialog.Builder builder = new AlertDialog.Builder(FaceRecognition2.this);
                        builder.setTitle("請問是否要註冊照片?");

                        // Set up the buttons
                        builder.setPositiveButton("是", (dialog, which) -> {
                            Intent it = new Intent(FaceRecognition2.this,Regis.class);
                            startActivity(it);
                            finish();
                        });
                        builder.setNegativeButton("否", (dialog, which) -> {
                            dialog.cancel();
                            Intent it = new Intent(FaceRecognition2.this,FaceRecognition.class);
                            startActivity(it);
                            finish();
                        });
                        builder.show();
                    }
                    else if (value1 == "Failed"){
                        y = 10;
                        //辨識失敗
                        Intent it = new Intent(FaceRecognition2.this,FaceRecognition.class);
                        startActivity(it);
                        finish();
                    }
                    else if (value1.trim().length() == 0){
                        //尚未辨識完成
                        y++;
                    }
                    else {
                        y = 10;
                        //辨識到人
                        mDatabase.child("face").child("temi1").child("checkin").child("py").setValue(false);
                        mDatabase.child("face").child("temi1").child("checkin").child("and").setValue(true);
                        Intent it = new Intent();
                        it.setClass(FaceRecognition2.this, Todo.class);
                        Bundle bundle = new Bundle();
                        bundle.putString("id", value1);
                        it.putExtras(bundle);   // 記得put進去，不然資料不會帶過去哦
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