package tw.com.temirobot;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.robotemi.sdk.Robot;
import com.robotemi.sdk.TtsRequest;

public class Game extends AppCompatActivity implements
        Robot.TtsListener {

    private Robot robot;

    protected void onStart() {
        super.onStart();

        Robot.getInstance().addTtsListener(this);

    }

    private static final String TAG = "game";
    // 減少魔術數字
    final int SCISSORS = 0;  // 代表剪刀的常數
    final int STONE = 1;     // 代表石頭的常數
    final int PAPER = 2;     // 代表布的常數

    int round = 0;     //代表第幾局
    int PcWin = 0;     //電腦贏的場數
    int PlayerWin = 0; //玩家贏的場數

    TextView txtResult; // 顯示遊戲輸贏結果
    TextView imgPcWin;
    TextView imgPlayerWin;

    ImageView btnScissors;
    ImageView btnStone;
    ImageView btnPaper;
    ImageView imgScissors1;
    ImageView imgStone1;
    ImageView imgPaper1;
    ImageView imgScissors2;
    ImageView imgStone2;
    ImageView imgPaper2;
    ImageView imgWin1;
    ImageView imgWin2;

    Button btnAgain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        buildView();


    }

    private void buildView() {
        // 把layout內的TextView元件關聯到程式碼裡
        txtResult = (TextView)findViewById(R.id.txtResult);

        // 將layout內的Button元件關聯到程式碼裡
        btnScissors = (ImageView)findViewById(R.id.btnScissors);
        btnStone = (ImageView)findViewById(R.id.btnStone);
        btnPaper = (ImageView)findViewById(R.id.btnPaper);
        btnAgain = (Button) findViewById(R.id.btnAgain);

        imgScissors1 = (ImageView) findViewById(R.id.imgScissors1);
        imgStone1 = (ImageView) findViewById(R.id.imgStone1);
        imgPaper1 = (ImageView) findViewById(R.id.imgPaper1);
        imgWin1 = (ImageView) findViewById(R.id.imgWin1);
        imgScissors2 = (ImageView) findViewById(R.id.imgScissors2);
        imgStone2 = (ImageView) findViewById(R.id.imgStone2);
        imgPaper2 = (ImageView) findViewById(R.id.imgPaper2);
        imgWin2 = (ImageView) findViewById(R.id.imgWin2);

        imgPcWin = (TextView) findViewById(R.id.PcWin);
        imgPlayerWin = (TextView) findViewById(R.id.PlayerWin);
    }

    public void btnhome(View v){
        Intent it = new Intent(Game.this,MainActivity.class);
        startActivity(it);
        finish();
    }

    // 剪刀按鈕事件
    public void btnScissors(View v){
        round ++;
        // Math.random()會產生一個0~1但不會等於1的隨機小數
        //  0.54 * 10 = 5.4 = (5)
        //  0.999999999999999999 * 10 = 9.999999999999 = 9
        //  0.000000000000000001 * 10 = 0.000000000001 = 0
        int pc = (int)(Math.random() * 3); // 產生0~2之間的隨機整數
        // (int)(Math.random() * 55); // 產生0~54之間的隨機整數
        // (int)(Math.random() * 91) + 9;  // 產生9~99之間的隨機整數
        //                                 // 產生5~55之間的隨機整數
        imgScissors1.setVisibility(View.INVISIBLE);
        imgStone1.setVisibility(View.INVISIBLE);
        imgPaper1.setVisibility(View.INVISIBLE);
        imgScissors2.setVisibility(View.INVISIBLE);
        imgStone2.setVisibility(View.INVISIBLE);
        imgPaper2.setVisibility(View.INVISIBLE);
        imgWin1.setVisibility(View.INVISIBLE);
        imgWin2.setVisibility(View.INVISIBLE);
        btnAgain.setVisibility(View.INVISIBLE);


        imgScissors1.setVisibility(View.VISIBLE);
        if(pc == 0)
        {
            imgScissors2.setVisibility(View.VISIBLE);
            Log.e(TAG, String.valueOf(round));
            if (round% 3 == 0)
            win();
            imgPlayerWin.setText(Integer.toString(PlayerWin));
            imgPcWin.setText(Integer.toString(PcWin));
        }
        if(pc == 1)
        {
            imgStone2.setVisibility(View.VISIBLE);
            imgWin2.setVisibility(View.VISIBLE);
            Log.e(TAG, String.valueOf(round));
            PcWin ++;
            if (round% 3 == 0)
            win();
            imgPlayerWin.setText(Integer.toString(PlayerWin));
            imgPcWin.setText(Integer.toString(PcWin));
        }
        if(pc == 2)
        {
            imgPaper2.setVisibility(View.VISIBLE);
            imgWin1.setVisibility(View.VISIBLE);
            PlayerWin++;
            Log.e(TAG, String.valueOf(round));
            if (round% 3 == 0)
            win();

            imgPlayerWin.setText(Integer.toString(PlayerWin));
            imgPcWin.setText(Integer.toString(PcWin));
        }
    }

    // 石頭按鈕事件
    public void btnStone(View view){
        round ++;
        int pc = (int)(Math.random() * 3); // 產生0~2之間的隨機整數
        imgScissors1.setVisibility(View.INVISIBLE);
        imgStone1.setVisibility(View.INVISIBLE);
        imgPaper1.setVisibility(View.INVISIBLE);
        imgScissors2.setVisibility(View.INVISIBLE);
        imgStone2.setVisibility(View.INVISIBLE);
        imgPaper2.setVisibility(View.INVISIBLE);
        imgWin1.setVisibility(View.INVISIBLE);
        imgWin2.setVisibility(View.INVISIBLE);
        btnAgain.setVisibility(View.INVISIBLE);


        imgStone1.setVisibility(View.VISIBLE);
        if(pc == 0)
        {
            imgScissors2.setVisibility(View.VISIBLE);
            imgWin1.setVisibility(View.VISIBLE);
            Log.e(TAG, String.valueOf(round));
            PlayerWin++;
            if (round% 3 == 0)
            win();
            imgPlayerWin.setText(Integer.toString(PlayerWin));
            imgPcWin.setText(Integer.toString(PcWin));
        }
        if(pc == 1)
        {
            imgStone2.setVisibility(View.VISIBLE);
            Log.e(TAG, String.valueOf(round));
            if (round% 3 == 0)
            win();
            imgPlayerWin.setText(Integer.toString(PlayerWin));
            imgPcWin.setText(Integer.toString(PcWin));
        }
        if(pc == 2)
        {
            imgPaper2.setVisibility(View.VISIBLE);
            imgWin2.setVisibility(View.VISIBLE);

            PcWin ++;
            Log.e(TAG, String.valueOf(round));
            if (round% 3 == 0)
            win();

            imgPlayerWin.setText(Integer.toString(PlayerWin));
            imgPcWin.setText(Integer.toString(PcWin));
        }
    }

    // 布按鈕事件
    public void btnPaper(View view){
        round ++;
        int pc = (int)(Math.random() * 3); // 產生0~2之間的隨機整數
        imgScissors1.setVisibility(View.INVISIBLE);
        imgStone1.setVisibility(View.INVISIBLE);
        imgPaper1.setVisibility(View.INVISIBLE);
        imgScissors2.setVisibility(View.INVISIBLE);
        imgStone2.setVisibility(View.INVISIBLE);
        imgPaper2.setVisibility(View.INVISIBLE);
        imgWin1.setVisibility(View.INVISIBLE);
        imgWin2.setVisibility(View.INVISIBLE);
        btnAgain.setVisibility(View.INVISIBLE);

        imgPaper1.setVisibility(View.VISIBLE);
        if(pc == 0)
        {
            imgScissors2.setVisibility(View.VISIBLE);
            imgWin2.setVisibility(View.VISIBLE);

            Log.e(TAG, String.valueOf(round));
            PcWin ++;
            if (round% 3 == 0)
            win();

            imgPlayerWin.setText(Integer.toString(PlayerWin));
            imgPcWin.setText(Integer.toString(PcWin));
        }
        if(pc == 1)
        {
            imgStone2.setVisibility(View.VISIBLE);
            imgWin1.setVisibility(View.VISIBLE);
            Log.e(TAG, String.valueOf(round));
            PlayerWin++;
            if (round% 3 == 0)
            win();

            imgPlayerWin.setText(Integer.toString(PlayerWin));
            imgPcWin.setText(Integer.toString(PcWin));
        }
        if(pc == 2)
        {
            imgPaper2.setVisibility(View.VISIBLE);
            Log.e(TAG, String.valueOf(round));
            if (round% 3 == 0)
            win();

            imgPlayerWin.setText(Integer.toString(PlayerWin));
            imgPcWin.setText(Integer.toString(PcWin));
        }
    }
    public void win( ) {
        btnAgain.setVisibility(View.VISIBLE);
        btnScissors.setVisibility(View.INVISIBLE);
        btnStone.setVisibility(View.INVISIBLE);
        btnPaper.setVisibility(View.INVISIBLE);
            if (PcWin>PlayerWin){
                txtResult.setText(R.string.txtPcWin);
                Log.e("PcWin", String.valueOf(PcWin));
                Log.e("PlayerWin)", String.valueOf(PlayerWin));
                Robot sRobot = Robot.getInstance();
                TtsRequest ttsRequest = TtsRequest.create("再加油",false);
                sRobot.speak(ttsRequest);
            }
            else if  (PcWin<PlayerWin){
                Log.e("PcWin", String.valueOf(PcWin));
                Log.e("PlayerWin", String.valueOf(PlayerWin));
                txtResult.setText(R.string.txtPlayerWin);
                Robot sRobot = Robot.getInstance();
                TtsRequest ttsRequest = TtsRequest.create("你贏了",false);
                sRobot.speak(ttsRequest);}

            else if  (PcWin==PlayerWin){
                Log.e("PcWin", String.valueOf(PcWin));
                Log.e("PlayerWin", String.valueOf(PlayerWin));
                txtResult.setText(R.string.txtDraw);}
            

    }

    public void Again(View view) {
        txtResult.setText(" ");
        PcWin=0;
        PlayerWin=0;

        btnAgain.setVisibility(View.INVISIBLE);
        btnScissors.setVisibility(View.VISIBLE);
        btnStone.setVisibility(View.VISIBLE);
        btnPaper.setVisibility(View.VISIBLE);

        imgPlayerWin.setText(Integer.toString(PlayerWin));
        imgPcWin.setText(Integer.toString(PcWin));
    }


    public void onTtsStatusChanged(TtsRequest ttsRequest) {

        // Do whatever you like upon the status changing. after the robot finishes speaking
        // Toast.makeText(this, "speech: " + ttsRequest.getSpeech() + "\nstatus:" + ttsRequest.getStatus(), Toast.LENGTH_LONG).show();
    }
}