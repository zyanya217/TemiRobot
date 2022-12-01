package tw.com.temirobot;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import com.robotemi.sdk.Robot;
import com.robotemi.sdk.TtsRequest;

public class EquipmenTeaching extends AppCompatActivity implements
        Robot.TtsListener {

    private Robot robot;

    protected void onStart() {
        super.onStart();

        Robot.getInstance().addTtsListener(this);

    }
    private int x = 1;

    ImageView ppt1;
    ImageView ppt2;
    ImageView ppt3;
    ImageView ppt4;
    ImageView ppt5;
    ImageView ppt6;
    ImageView ppt7;
    ImageView ppt8;
    ImageView ppt9;
    ImageView ppt10;
    ImageView ppt11;
    ImageView ppt12;
    ImageView ppt13;
    ImageView ppt14;
    ImageView ppt15;
    ImageView ppt16;
    ImageView ppt17;
    ImageView ppt18;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equipmen_teaching);

        ppt1 = (ImageView) findViewById(R.id.ppt1);
        ppt2 = (ImageView) findViewById(R.id.ppt2);
        ppt3 = (ImageView) findViewById(R.id.ppt3);
        ppt4 = (ImageView) findViewById(R.id.ppt4);
        ppt5 = (ImageView) findViewById(R.id.ppt5);
        ppt6 = (ImageView) findViewById(R.id.ppt6);
        ppt7 = (ImageView) findViewById(R.id.ppt7);
        ppt8 = (ImageView) findViewById(R.id.ppt8);
        ppt9 = (ImageView) findViewById(R.id.ppt9);
        ppt10 = (ImageView) findViewById(R.id.ppt10);
        ppt11 = (ImageView) findViewById(R.id.ppt11);
        ppt12 = (ImageView) findViewById(R.id.ppt12);
        ppt13 = (ImageView) findViewById(R.id.ppt13);
        ppt14 = (ImageView) findViewById(R.id.ppt14);
        ppt15 = (ImageView) findViewById(R.id.ppt15);
        ppt16 = (ImageView) findViewById(R.id.ppt16);
        ppt17 = (ImageView) findViewById(R.id.ppt17);
        ppt18 = (ImageView) findViewById(R.id.ppt18);

        Robot sRobot = Robot.getInstance();
        TtsRequest ttsRequest = TtsRequest.create("點擊RFID卡",false);
        sRobot.speak(ttsRequest);
    }

    public void btnhome(View v){
        Intent it = new Intent(EquipmenTeaching.this,MainActivity.class);
        startActivity(it);
        finish();
    }


    public void btnlast(View v){
        if (x==2){ppt2.setVisibility(View.INVISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("點擊RFID卡",false);
            sRobot.speak(ttsRequest);}
        else if (x==3){ppt3.setVisibility(View.INVISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("刷卡簽到",false);
            sRobot.speak(ttsRequest);}
        else if (x==4){ppt4.setVisibility(View.INVISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("簽到成功，點擊叉叉",false);
            sRobot.speak(ttsRequest);}
        else if (x==5){ppt5.setVisibility(View.INVISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("點擊生理量測",false);
            sRobot.speak(ttsRequest);}
        else if (x==6){ppt6.setVisibility(View.INVISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("點擊血壓旁邊的量測",false);
            sRobot.speak(ttsRequest);}
        else if (x==7){ppt7.setVisibility(View.INVISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("將壓脈帶套在手臂",false);
            sRobot.speak(ttsRequest);}
        else if (x==8){ppt8.setVisibility(View.INVISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("等待量測中，點擊確認",false);
            sRobot.speak(ttsRequest);}
        else if (x==9){ppt9.setVisibility(View.INVISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("血壓測量成功",false);
            sRobot.speak(ttsRequest);}
        else if (x==10){ppt10.setVisibility(View.INVISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("點擊額溫旁邊的量測",false);
            sRobot.speak(ttsRequest);}
        else if (x==11){ppt11.setVisibility(View.INVISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("額溫槍放置額頭5公分處壓下灰色按鈕測量",false);
            sRobot.speak(ttsRequest);}
        else if (x==12){ppt12.setVisibility(View.INVISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("點擊確認額溫測量成功",false);
            sRobot.speak(ttsRequest);}
        else if (x==13){ppt13.setVisibility(View.INVISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("將食指至於血氧機中點擊開機鈕",false);
            sRobot.speak(ttsRequest);}
        else if (x==14){ppt14.setVisibility(View.INVISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("點擊血氧旁邊的量測",false);
            sRobot.speak(ttsRequest);}
        else if (x==15){ppt15.setVisibility(View.INVISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("等待量測中，點擊確認",false);
            sRobot.speak(ttsRequest);}
        else if (x==16){ppt16.setVisibility(View.INVISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("血氧測量成功",false);
            sRobot.speak(ttsRequest);}
        else if (x==17){ppt17.setVisibility(View.INVISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("點擊上傳 點擊叉叉",false);
            sRobot.speak(ttsRequest);}
        else if (x==18){ppt18.setVisibility(View.INVISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("點擊右上角登出",false);
            sRobot.speak(ttsRequest);}
        else if (x==1){x=2; Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("點擊RFID卡",false);
            sRobot.speak(ttsRequest);}
        x--;
    }

    public void btnnext(View v){
        x++;
        if (x==2){ppt2.setVisibility(View.VISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("刷卡簽到",false);
            sRobot.speak(ttsRequest);}
        else if (x==3){ppt3.setVisibility(View.VISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("簽到成功，點擊叉叉",false);
            sRobot.speak(ttsRequest);}
        else if (x==4){ppt4.setVisibility(View.VISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("點擊生理量測",false);
            sRobot.speak(ttsRequest);}
        else if (x==5){ppt5.setVisibility(View.VISIBLE);Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("點擊血壓旁邊的量測",false);
            sRobot.speak(ttsRequest);}
        else if (x==6){ppt6.setVisibility(View.VISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("將壓脈帶套在手臂",false);
            sRobot.speak(ttsRequest);}
        else if (x==7){ppt7.setVisibility(View.VISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("等待量測中，點擊確認",false);
            sRobot.speak(ttsRequest);}
        else if (x==8){ppt8.setVisibility(View.VISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("血壓測量成功",false);
            sRobot.speak(ttsRequest);}
        else if (x==9){ppt9.setVisibility(View.VISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("點擊額溫旁邊的量測",false);
            sRobot.speak(ttsRequest);}
        else if (x==10){ppt10.setVisibility(View.VISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("額溫槍放置額頭5公分處壓下灰色按鈕測量",false);
            sRobot.speak(ttsRequest);}
        else if (x==11){ppt11.setVisibility(View.VISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("點擊確認額溫測量成功",false);
            sRobot.speak(ttsRequest);}
        else if (x==12){ppt12.setVisibility(View.VISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("將食指至於血氧機中點擊開機鈕",false);
            sRobot.speak(ttsRequest);}
        else if (x==13){ppt13.setVisibility(View.VISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("點擊血氧旁邊的量測",false);
            sRobot.speak(ttsRequest);}
        else if (x==14){ppt14.setVisibility(View.VISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("等待量測中，點擊確認",false);
            sRobot.speak(ttsRequest);}
        else if (x==15){ppt15.setVisibility(View.VISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("血氧測量成功",false);
            sRobot.speak(ttsRequest);}
        else if (x==16){ppt16.setVisibility(View.VISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("點擊上傳 點擊叉叉",false);
            sRobot.speak(ttsRequest);}
        else if (x==17){ppt17.setVisibility(View.VISIBLE);
            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("點擊右上角登出",false);
            sRobot.speak(ttsRequest);}
        else if (x==18){ppt18.setVisibility(View.VISIBLE);

            Robot sRobot = Robot.getInstance();
            TtsRequest ttsRequest = TtsRequest.create("恭喜您完成報到",false);
            sRobot.speak(ttsRequest);}
        else if (x==19){x=18;}
    }

    public void onTtsStatusChanged(TtsRequest ttsRequest) {

        // Do whatever you like upon the status changing. after the robot finishes speaking
        // Toast.makeText(this, "speech: " + ttsRequest.getSpeech() + "\nstatus:" + ttsRequest.getStatus(), Toast.LENGTH_LONG).show();
    }

}