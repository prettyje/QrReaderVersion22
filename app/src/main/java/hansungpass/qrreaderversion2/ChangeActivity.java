package hansungpass.qrreaderversion2;


import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ChangeActivity extends AppCompatActivity {
    //view Objects dd
    ImageView imageView1; //성공
    ImageView imageView2; //실패
    TextView textView; //
    String output; //qr 스캔 결과
    Handler mHandler;
    Handler cHandler;
    Bundle bundle;   //mHandler

    //qr code scanner object
    private IntentIntegrator qrScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_change);

        //View Objects
        textView = (TextView) findViewById(R.id.textView);

       // mcamera = Camera.CameraInfo.CAMERA_FACING_FRONT;

        //intializing scan object
        qrScan = new IntentIntegrator(this);
        //scan option
        qrScan.setCameraId(1); //전면카메라로 변경
        qrScan.setPrompt("HansungPass 에서 Scanning중...");
        qrScan.setOrientationLocked(true);

        //qrScan.setOrientationLocked(false);
        qrScan.initiateScan();  //qr스캐너 작동 시작



        //핸들러
        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                bundle = msg.getData();
                String ss = bundle.getString("key");


                try {
                    if (ss.equals("success")) { //성공시
                        System.out.println("성공");
                        imageView1 = (ImageView) findViewById(R.id.imageView1);
                        imageView1.setImageResource(R.drawable.success);
                        imageView1.invalidate();
                        textView.setText(output.substring(0,7)+"님 반갑습니다. 입장하십시오.");
                    } else { //실패시
                        System.out.println("실패");
                        imageView1 = (ImageView) findViewById(R.id.imageView1);
                        imageView1.setImageResource(R.drawable.failc);
                        imageView1.invalidate();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        cHandler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                startActivity(new Intent(ChangeActivity.this, MainActivity.class));
                finish();
            }
        };

    }

    //Getting the scan results
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IntentIntegrator.REQUEST_CODE) {
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

            if (result != null) {
                //qrcode 가 없으면
                if (result.getContents() == null) {

                    Bundle bundle2 = new Bundle();
                    bundle2.putString("key", "Qr is empty");
                    Message msg2 = new Message();
                    msg2.setData(bundle2);
                    mHandler.sendMessage(msg2);

                    Toast.makeText(ChangeActivity.this, "취소!", Toast.LENGTH_SHORT).show();
                    cHandler.sendEmptyMessageDelayed(0,3000);
                } else {
                    //qrcode 결과가 있으면
                    // Toast.makeText(ChangeActivity.this, "스캔완료!", Toast.LENGTH_SHORT).show();
                    try {
                        //data를 json으로 변환
                        JSONObject obj = new JSONObject(result.getContents());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    //Toast.makeText(ChangeActivity.this, result.getContents(), Toast.LENGTH_LONG).show();
                    output = result.getContents();
                   // textView.setText(output);
                    ConnectThread ct = new ConnectThread();//서버 연결 쓰레드
                    ct.start();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }

    }

    class ConnectThread extends Thread {
        public void run() {

            String host = "113.198.84.23";
            int port = 80;
            System.out.println("스레드 시작");


            try {
                Socket socket = new Socket(host, port);
                System.out.println("서버로 연결되었습니다. : " + host + ", " + port);

                ObjectOutputStream outstream = new ObjectOutputStream(socket.getOutputStream());
                outstream.writeObject("scanner");
                outstream.flush();
                System.out.println("서버로 보낸 데이터1 : scanner");

                ObjectOutputStream outstream2 = new ObjectOutputStream(socket.getOutputStream());
                outstream2.writeObject(output);
                outstream2.flush();
                System.out.println("서버로 보낸 데이터2 : " + output);

                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                Object Input = inputStream.readObject();
                String result_server = Input.toString();
                System.out.println("서버에서 받은 데이터 "  + result_server);


                Bundle bundle3 = new Bundle();
                bundle3.putString("key", result_server);
                Message msg3 = new Message();
                msg3.setData(bundle3);
                mHandler.sendMessage(msg3);
                cHandler.sendEmptyMessageDelayed(0,3000);


                outstream.close();
                outstream2.close();
                inputStream.close();
                socket.close();


            } catch (Exception e) {
                e.printStackTrace();
                Bundle bundle4 = new Bundle();
                bundle4.putString("key", "Server Connect Fail");
                Message msg4 = new Message();
                msg4.setData(bundle4);
                //Toast.makeText(ChangeActivity.this, "접근실패", Toast.LENGTH_SHORT).show();
                mHandler.sendMessage(msg4);
                cHandler.sendEmptyMessageDelayed(0,3000);
            }
        }
    }
}
