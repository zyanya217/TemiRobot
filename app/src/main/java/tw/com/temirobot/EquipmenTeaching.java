package tw.com.temirobot;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class EquipmenTeaching extends AppCompatActivity {
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
    ImageView ppt19;

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
    }

    public void btnhome(View v){
        Intent it = new Intent(EquipmenTeaching.this,MainActivity.class);
        startActivity(it);
        finish();
    }

    public void btnlast(View v){
        if (x==2){ppt2.setVisibility(View.INVISIBLE);}
        else if (x==3){ppt3.setVisibility(View.INVISIBLE);}
        else if (x==4){ppt4.setVisibility(View.INVISIBLE);}
        else if (x==5){ppt5.setVisibility(View.INVISIBLE);}
        else if (x==6){ppt6.setVisibility(View.INVISIBLE);}
        else if (x==7){ppt7.setVisibility(View.INVISIBLE);}
        else if (x==8){ppt8.setVisibility(View.INVISIBLE);}
        else if (x==9){ppt9.setVisibility(View.INVISIBLE);}
        else if (x==10){ppt10.setVisibility(View.INVISIBLE);}
        else if (x==11){ppt11.setVisibility(View.INVISIBLE);}
        else if (x==12){ppt12.setVisibility(View.INVISIBLE);}
        else if (x==13){ppt13.setVisibility(View.INVISIBLE);}
        else if (x==14){ppt14.setVisibility(View.INVISIBLE);}
        else if (x==15){ppt15.setVisibility(View.INVISIBLE);}
        else if (x==16){ppt16.setVisibility(View.INVISIBLE);}
        else if (x==17){ppt17.setVisibility(View.INVISIBLE);}
        else if (x==18){ppt18.setVisibility(View.INVISIBLE);}
        else if (x==1){x=2;}
        x--;
    }

    public void btnnext(View v){
        x++;
        if (x==2){ppt2.setVisibility(View.VISIBLE);}
        else if (x==3){ppt3.setVisibility(View.VISIBLE);}
        else if (x==4){ppt4.setVisibility(View.VISIBLE);}
        else if (x==5){ppt5.setVisibility(View.VISIBLE);}
        else if (x==6){ppt6.setVisibility(View.VISIBLE);}
        else if (x==7){ppt7.setVisibility(View.VISIBLE);}
        else if (x==8){ppt8.setVisibility(View.VISIBLE);}
        else if (x==9){ppt9.setVisibility(View.VISIBLE);}
        else if (x==10){ppt10.setVisibility(View.VISIBLE);}
        else if (x==11){ppt11.setVisibility(View.VISIBLE);}
        else if (x==12){ppt12.setVisibility(View.VISIBLE);}
        else if (x==13){ppt13.setVisibility(View.VISIBLE);}
        else if (x==14){ppt14.setVisibility(View.VISIBLE);}
        else if (x==15){ppt15.setVisibility(View.VISIBLE);}
        else if (x==16){ppt16.setVisibility(View.VISIBLE);}
        else if (x==17){ppt17.setVisibility(View.VISIBLE);}
        else if (x==18){ppt18.setVisibility(View.VISIBLE);}
        else if (x==19){x=18;}
    }
}