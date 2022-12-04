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
import com.robotemi.sdk.TtsRequest;
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener;
import com.robotemi.sdk.listeners.OnRobotReadyListener;
import com.robotemi.sdk.navigation.listener.OnCurrentPositionChangedListener;
import com.robotemi.sdk.navigation.model.Position;
import com.robotemi.sdk.navigation.model.SpeedLevel;

import org.jetbrains.annotations.NotNull;

public class Welcome2 extends AppCompatActivity implements
        OnGoToLocationStatusChangedListener,
        OnRobotReadyListener {
    private static Robot robot;
    private static String Speak = null;

    private DatabaseReference mDatabase;
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private int y = 0;
    private int x = 0;
    private ImageView btnlock;

    private static final String TAGError = "Welcome2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome2);
        robot = Robot.getInstance();
        x = 1;

        mDatabase = FirebaseDatabase.getInstance().getReference();

        btnlock = findViewById(R.id.btnlock);
        btnlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(Welcome2.this);
                builder.setTitle("回首頁請輸入密碼");

                // Set up the input
                final EditText input = new EditText(Welcome2.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT );
                input.setMaxWidth(6);
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton("確認", (dialog, which) -> {
                    //Toast.makeText(context, input.getText().toString(), Toast.LENGTH_SHORT).show();
                    //Create and Initialize new object with Face embeddings and Name.
                    if (input.getText().toString().trim().equals("1234")){
                        Intent it = new Intent(Welcome2.this, MainActivity.class);
                        startActivity(it);
                        finish();
                    }
                    else dialog.cancel();
                });
                builder.setNegativeButton("取消", (dialog, which) -> {
                    dialog.cancel();
                });
                builder.show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        robot.addOnGoToLocationStatusChangedListener(this);
        robot.addOnRobotReadyListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        robot.removeOnGoToLocationStatusChangedListener(this);
        robot.removeOnRobotReadyListener(this);
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
    public void onGoToLocationStatusChanged(@NotNull String location, String status, int descriptionId, @NotNull String description) {
        System.out.println("list: OnGoToLocationStatusChanged");
        switch (status) {
            case OnGoToLocationStatusChangedListener.START:
                try {
                    robot.tiltAngle(55);
                    robot.setGoToSpeed(SpeedLevel.SLOW);
                    System.out.println("list: OnGoToLocationStatusChangedListener_START");
                } catch (Exception e) {
                    Log.e(TAGError, "list:Error:" + e.getMessage());
                }
                break;
            case OnGoToLocationStatusChangedListener.GOING:
                try {
                    robot.tiltAngle(55);
                    robot.setGoToSpeed(SpeedLevel.SLOW);
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
                    if(x == 1){
                    TtsRequest ttsRequest2 = TtsRequest.create("請在這邊進行報到喔，請按下機器人上的智能報到按鈕，我先回去囉下次見。",true);
                    robot.speak(ttsRequest2);
                    //robot.repose();
                    //robot.stopMovement();
                    Thread.sleep(5000);
                    robot.goTo("labin2");
                    x = 2;
                    }
                    else if (x == 2){
                        Intent it = new Intent(Welcome2.this, Welcome.class);
                        startActivity(it);
                        finish();
                    }
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

    @Override
    protected void onResume() {
        super.onResume();
        if (y < 10){
            DatabaseReference myRef1 = database.getReference("/face/temi1/welcome/id");
            myRef1.addValueEventListener(new ValueEventListener() {
                //String value1 = "B0844230";//測試寫死用
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // This method is called once with the initial value and again
                    // whenever data at this location is updated.

                    String value1 = dataSnapshot.getValue(String.class); //打開

                    if (value1.trim().equals("Unknown")) {
                        y = 10;
                        //查無此人
                        mDatabase.child("face").child("temi1").child("welcome").child("py").setValue(false);
                        mDatabase.child("face").child("temi1").child("welcome").child("and").setValue(true);
                        mDatabase.child("face").child("temi1").child("welcome").child("id").setValue("1");
                        Intent it = new Intent(Welcome2.this, Welcome.class);
                        startActivity(it);
                        finish();
                    }
                    else if (value1.trim().equals("1")){
                        //尚未辨識完成
                        y++;
                    }
                    else if (value1.trim().equals("Failed")) {
                        y = 10;
                        //查無此人
                        mDatabase.child("face").child("temi1").child("welcome").child("py").setValue(false);
                        mDatabase.child("face").child("temi1").child("welcome").child("and").setValue(true);
                        mDatabase.child("face").child("temi1").child("welcome").child("id").setValue("1");
                        Intent it = new Intent(Welcome2.this, Welcome.class);
                        startActivity(it);
                        finish();
                    }
                    else{
                        y = 10;
                        //辨識到人
                        mDatabase.child("face").child("temi1").child("welcome").child("py").setValue(false);
                        mDatabase.child("face").child("temi1").child("welcome").child("and").setValue(true);
                        //去抓 value1(辨識到的這個人)
                        DatabaseReference myRef2 = database.getReference("/user/"+value1+"/greet");

                        myRef2.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                // This method is called once with the initial value and again
                                // whenever data at this location is updated.
                                String value2 = dataSnapshot.getValue(String.class);
                                Log.d("TAG", "Value2 is: " + value2);
                                Robot sRobot = Robot.getInstance();
                                TtsRequest ttsRequest = TtsRequest.create(value2+"我帶你去找報到機器人請跟著我來",true);
                                sRobot.speak(ttsRequest);
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {
                                // Failed to read value
                                Log.w("TAG", "Failed to read value.", error.toException());
                            }
                        });

                        robot.goTo("labin");
                        mDatabase.child("face").child("temi1").child("welcome").child("id").setValue("");
                    }
                    System.out.println("list: value1 = " + value1);
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

}