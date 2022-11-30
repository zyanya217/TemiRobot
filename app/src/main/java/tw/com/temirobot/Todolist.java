package tw.com.temirobot;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.robotemi.sdk.NlpResult;
import com.robotemi.sdk.Robot;
import com.robotemi.sdk.TtsRequest;
import com.robotemi.sdk.activitystream.ActivityStreamPublishMessage;
import com.robotemi.sdk.listeners.OnBeWithMeStatusChangedListener;
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener;
import com.robotemi.sdk.listeners.OnLocationsUpdatedListener;
import com.robotemi.sdk.listeners.OnRobotReadyListener;
import com.robotemi.sdk.Robot;
import com.robotemi.sdk.TtsRequest;
import java.util.List;

public class Todolist extends AppCompatActivity implements Robot.TtsListener {
    private Intent it;

    private Robot robot;
    private static final String Speak = "  ";

    protected void onStart() {
        super.onStart();
        Robot.getInstance().addTtsListener(this);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todolist);

        it = getIntent();
        // 通過key得到得到物件
        // getSerializableExtra得到序列化資料
        String name = (String) it.getSerializableExtra("key");

        //        initViews();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        //robot = Robot.getInstance(); // get an instance of the robot in order to begin using its features.

        FirebaseDatabase database = FirebaseDatabase.getInstance();

        DatabaseReference myRef1 = database.getReference("/facevar/recogid");
//
//        myRef.setValue("Hello, World!");

        myRef1.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                String value1 = dataSnapshot.getValue(String.class);
                Log.d("TAG", "Value1 is: " + value1);

                DatabaseReference myRef2 = database.getReference("/user/"+value1+"/todolist");
                myRef2.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // This method is called once with the initial value and again
                        // whenever data at this location is updated.
                        String value2 = dataSnapshot.getValue(String.class);
                        Log.d("TAG", "Value2 is: " + value2);
                        Robot sRobot = Robot.getInstance();
                        TtsRequest ttsRequest = TtsRequest.create(value2,false);
                        sRobot.speak(ttsRequest);
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

    public void btnhome(View v){
        Intent it = new Intent(Todolist.this,MainActivity.class);
        startActivity(it);
        finish();
    }

    @Override
    public void onTtsStatusChanged(TtsRequest ttsRequest) {

//         Do whatever you like upon the status changing. after the robot finishes speaking
//         Toast.makeText(this, "speech: " + ttsRequest.getSpeech() + "\nstatus:" + ttsRequest.getStatus(), Toast.LENGTH_LONG).show();
    }

}