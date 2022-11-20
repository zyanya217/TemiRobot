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

    private final static int sendUser = 2;

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
        it2 = getIntent();
        // 通過key得到得到物件
        // getSerializableExtra得到序列化資料
        String name = (String) it2.getSerializableExtra("key");
        // 放入需要傳遞的物件
        it.putExtra("key", name);
        // 啟動意圖(意圖，請求碼(int)) 請求碼最好使用 final static定義 方便識別
        startActivityForResult(it, sendUser);
        startActivity(it);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 如果請求碼為 sendUser 返回碼 為 RESULT_OK RESULT_OK為系統自定義的int值為 -1
        if (requestCode == sendUser && resultCode == RESULT_OK) {
            // 在TextView中設定返回資訊
            System.out.println("list: 傳遞訊息: "+data.getStringExtra("key"));
        }
    }

    public void btnno(View v){
        Intent it = new Intent(Todo.this,FaceRecognition.class);
        startActivity(it);
        finish();
    }

}