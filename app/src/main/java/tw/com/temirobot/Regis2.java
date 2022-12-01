package tw.com.temirobot;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

public class Regis2 extends AppCompatActivity {
    private Intent it2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_regis2);

        it2 = getIntent();
        // 通過key得到得到物件
        // getSerializableExtra得到序列化資料
        String name = (String) it2.getSerializableExtra("key");
    }
}