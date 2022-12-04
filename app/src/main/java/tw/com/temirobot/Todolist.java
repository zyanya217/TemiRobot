package tw.com.temirobot;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.robotemi.sdk.Robot;
import com.robotemi.sdk.TtsRequest;

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

        Robot sRobot = Robot.getInstance();
        TtsRequest ttsRequest = TtsRequest.create("恭喜您完成報到",true);
        sRobot.speak(ttsRequest);

        TextView todolist=findViewById(R.id.todolistText);
        TextView NameText=findViewById(R.id.textView4);


        it = getIntent();
        // 通過key得到得到物件
        // getSerializableExtra得到序列化資料
        String name = (String) it.getSerializableExtra("key");

        //        initViews();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        //robot = Robot.getInstance(); // get an instance of the robot in order to begin using its features.



        FirebaseDatabase database = FirebaseDatabase.getInstance();

        DatabaseReference myRef1 = database.getReference("/face/temi1/checkin/id");


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

                        DatabaseReference myRef3 = database.getReference("/user/"+value1+"/name");
                        myRef3.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                // This method is called once with the initial value and again
                                // whenever data at this location is updated.
                                String value3 = dataSnapshot.getValue(String.class);
                                Log.d("TAG", "Value3 is: " + value3);
                                Robot sRobot = Robot.getInstance();
                                TtsRequest ttsRequest = TtsRequest.create(value2,false);
                                sRobot.speak(ttsRequest);
                                NameText.setText(value3);
                                todolist.setText(value2);

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


        Robot sRobot1 = Robot.getInstance();
        TtsRequest ttsRequest1 = TtsRequest.create("請您先進教室休息喔",true);
        sRobot1.speak(ttsRequest1);
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