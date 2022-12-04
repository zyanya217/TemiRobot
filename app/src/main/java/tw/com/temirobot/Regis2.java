package tw.com.temirobot;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Regis2 extends AppCompatActivity {
    private DatabaseReference mDatabase;
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private int y = 0;
    private TextView txtRegis;
    private TextView txtFault;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_regis2);

        System.out.println("list:3 Regis2");

        txtRegis = findViewById(R.id.txtRegis);
        txtFault = findViewById(R.id.txtFault);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        txtFault.setText("辨識中...請稍等");
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
//        DatabaseReference myRef1 = database.getReference("/face/temi1/regis/py");
//        myRef1.addValueEventListener(new ValueEventListener() {
        if (y < 10) {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                // This method is called once with the initial value and again
//                // whenever data at this location is updated.
//                Boolean value1 = dataSnapshot.getValue(Boolean.class);
//                Log.d("TAG", "Value1 is: " + value1);
//                if (value1 == false) {
            DatabaseReference myRef1 = database.getReference("/face/temi1/regis/id");
            myRef1.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // This method is called once with the initial value and again
                    // whenever data at this location is updated.
                    String value1 = dataSnapshot.getValue(String.class);
                    Log.d("TAG", "Value1 is: " + value1);
                    if (value1.trim() == "Success") {
                        txtFault.setVisibility(View.INVISIBLE);
                        txtRegis.setVisibility(View.VISIBLE);
                        txtRegis.setText("恭喜註冊成功!");
                        mDatabase.child("face").child("temi1").child("regis").child("and").setValue(false);
                    } else if (value1.trim() == "Failed") {
                        txtFault.setVisibility(View.INVISIBLE);
                        txtRegis.setVisibility(View.VISIBLE);
                        txtRegis.setText("辨識失敗，請再試一次");
                        mDatabase.child("face").child("temi1").child("regis").child("and").setValue(false);
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Failed to read value
                    Log.w("TAG", "Failed to read value.", error.toException());
                }
            });
        }
        y++;
//            @Override
//            public void onCancelled(DatabaseError error) {
//                // Failed to read value
//                Log.w("TAG", "Failed to read value.", error.toException());
//            }
//        });
    }

    public void btnhome(View v) {
        Intent it = new Intent(Regis2.this, MainActivity.class);
        startActivity(it);
        finish();
    }
}