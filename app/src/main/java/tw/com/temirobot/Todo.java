package tw.com.temirobot;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Todo extends AppCompatActivity {
    ImageView btnyes;
    ImageView btnno;
    private Intent it;
    private TextView detectionTextView;
    private FirebaseDatabase database = FirebaseDatabase.getInstance();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo);
        btnyes = findViewById(R.id.btnyes);
        btnno = findViewById(R.id.btnno);
        detectionTextView = findViewById(R.id.detection_text);

        it = getIntent();
        // 通過key得到得到物件
        // getSerializableExtra得到序列化資料
        String id = (String) it.getSerializableExtra("id");
        DatabaseReference myRef2 = database.getReference("/user/"+id+"/name");

        myRef2.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                String name = dataSnapshot.getValue(String.class);
                Log.d("TAG", "Value2 is: " + name);
                detectionTextView.setText("請問是" + id + ", " + name + "嗎?");
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w("TAG", "Failed to read value.", error.toException());
            }
        });
    }

    public void btnhome(View v){
        Intent it = new Intent(Todo.this,MainActivity.class);
        startActivity(it);
        finish();
    }

    public void btnyes(View v){
        Intent it = new Intent(Todo.this,Todolist.class);
        startActivity(it);
        finish();
    }

    public void btnno(View v){
        Intent it = new Intent(Todo.this,FaceRecognition.class);
        startActivity(it);
        finish();
    }

}