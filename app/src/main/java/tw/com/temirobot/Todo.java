package tw.com.temirobot;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class Todo extends AppCompatActivity {
    private ImageView btnyes;
    private ImageView btnno;
    private Intent it2;
    private TextView detectionTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo);
        btnyes = findViewById(R.id.btnyes);
        btnno = findViewById(R.id.btnno);
        detectionTextView = findViewById(R.id.detection_text);

        it2 = getIntent();
        // 通過key得到得到物件
        // getSerializableExtra得到序列化資料
        String name = (String) it2.getSerializableExtra("key");
        detectionTextView.setText("請問是" + name + "嗎?");
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