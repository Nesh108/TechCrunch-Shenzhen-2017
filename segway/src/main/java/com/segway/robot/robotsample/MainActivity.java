package com.segway.robot.robotsample;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;
import com.google.zxing.qrcode.QRCodeReader;
import com.segway.robot.algo.Pose2D;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.baseconnectivity.Message;
import com.segway.robot.sdk.baseconnectivity.MessageConnection;
import com.segway.robot.sdk.baseconnectivity.MessageRouter;
import com.segway.robot.sdk.connectivity.BufferMessage;
import com.segway.robot.sdk.connectivity.RobotException;
import com.segway.robot.sdk.connectivity.RobotMessageRouter;
import com.segway.robot.sdk.connectivity.StringMessage;
import com.segway.robot.sdk.locomotion.sbv.Base;
import com.segway.robot.sdk.vision.Vision;
import com.segway.robot.sdk.vision.frame.Frame;
import com.segway.robot.sdk.vision.stream.StreamInfo;
import com.segway.robot.sdk.vision.stream.StreamType;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Hashtable;

public class MainActivity extends Activity {

    private static final String TAG = "RobotActivity";
    private int press = 0;
    private TextView textViewIp;

    private ImageView preview;
    private RobotMessageRouter mRobotMessageRouter = null;
    private MessageConnection mMessageConnection = null;

    private Base mBase;
    private Vision mVision;
    private boolean mBind;

    private String action = null;
    private int score1 = 0;
    private int score2 = 0;
    private boolean dead = false;
    private int frameCount = 0;
    private StreamInfo colorInfo;
    private MediaPlayer mp;
    private boolean needsReset = true;
    private boolean danceOn = false;
    MediaPlayer player;

    private ServiceBinder.BindStateListener mBindStateListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            Log.d(TAG, "onBind: ");
            try {
                //register MessageConnectionListener in the RobotMessageRouter
                mRobotMessageRouter.register(mMessageConnectionListener);
            } catch (RobotException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onUnbind(String reason) {
            Log.e(TAG, "onUnbind: " + reason);
        }
    };

    private MessageRouter.MessageConnectionListener mMessageConnectionListener = new RobotMessageRouter.MessageConnectionListener() {
        @Override
        public void onConnectionCreated(final MessageConnection connection) {
            Log.d(TAG, "onConnectionCreated: " + connection.getName());
            mMessageConnection = connection;
            try {
                mMessageConnection.setListeners(mConnectionStateListener, mMessageListener);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private MessageConnection.ConnectionStateListener mConnectionStateListener = new MessageConnection.ConnectionStateListener() {
        @Override
        public void onOpened() {
            Log.d(TAG, "onOpened: ");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "connected to: " + mMessageConnection.getName(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onClosed(String error) {
            Log.e(TAG, "onClosed: " + error);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "disconnected to: " + mMessageConnection.getName(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    private MessageConnection.MessageListener mMessageListener = new MessageConnection.MessageListener() {
        @Override
        public void onMessageSentError(Message message, String error) {

        }

        @Override
        public void onMessageSent(Message message) {
            Log.d(TAG, "onBufferMessageSent: id=" + message.getId() + ";timestamp=" + message.getTimestamp());
        }

        @Override
        public void onMessageReceived(final Message message) {
            Log.d(TAG, "onMessageReceived: id=" + message.getId() + ";timestamp=" + message.getTimestamp() + ";content=" + message.getContent().toString());
            if (message instanceof StringMessage) {
                // start here
                //message received is StringMessage
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switch (message.getContent().toString().toLowerCase()){
                            case "fwd":
                                fwd();
                                break;
                            case "back":
                                back();
                                break;
                            case "left":
                                rl();
                                break;
                            case "right":
                                rr();
                                break;
                            case "stop":
                                stop();
                                break;
                            case "play":
                                player.start();
                                break;
                        }



                    }

                });
            } else {
                //message received is BufferMessage
                byte[] bytes = (byte[]) message.getContent();
                final String name = saveFile(bytes);

                Log.d(TAG, "onMessageReceived: file name=" + name);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "file saved: " + name, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        textViewIp = (TextView) findViewById(R.id.textView_ip);

        preview = (ImageView) findViewById(R.id.imageView);

        textViewIp.setText(getDeviceIp());

        mp = MediaPlayer.create(getApplicationContext(), R.raw.victory);
        mp.setVolume(100, 100);
        player = MediaPlayer.create(MainActivity.this, R.raw.audio);
        player.setVolume(100, 100);
        //get RobotMessageRouter
        mRobotMessageRouter = RobotMessageRouter.getInstance();
        //bind to connection service in robot
        mRobotMessageRouter.bindService(this, mBindStateListener);

        mBase = Base.getInstance();
        mBase.bindService(this.getApplicationContext(), new ServiceBinder.BindStateListener() {
            @Override
            public void onBind() {
                mBase.setControlMode(mBase.CONTROL_MODE_RAW);
                // async process goes here
            }

            @Override
            public void onUnbind(String reason) {
                stop();
            }
        });

        mVision = Vision.getInstance();
        mVision.bindService(this.getApplicationContext(), new ServiceBinder.BindStateListener() {
            @Override
            public void onBind() {
                mBind = true;
                startImageTransfer();
            }

            @Override
            public void onUnbind(String reason) {
                stopImageTransfer();
                mBind = false;
            }
        });

    }

    Vision.FrameListener mFrameListener = new Vision.FrameListener() {
        @Override
        public void onNewFrame(int streamType, Frame frame) {
            if(needsReset) {
                // Reset Score
                score1 = -1;
                score2 = -1;
                setStatus("1");
                setStatus("2");
                needsReset = false;
            }

            Runnable runnable = null;
            switch (streamType) {
                case StreamType.COLOR:
                    Bitmap bm = Bitmap.createBitmap(colorInfo.getWidth(), colorInfo.getHeight(), Bitmap.Config.RGB_565);
                    final Bitmap bmp = Bitmap.createBitmap(colorInfo.getWidth(), colorInfo.getHeight(), Bitmap.Config.ARGB_8888);
                    bmp.copyPixelsFromBuffer(frame.getByteBuffer());
                    runnable = new Runnable() {
                        @Override
                        public void run() {
                            preview.setImageBitmap(bmp);
                        }
                    };

                    if(dead) {
                        frameCount++;

                        if(frameCount >= 500) {
                            dead = false;
                            frameCount = 0;
                            mp.stop();
                        } else if(frameCount < 90) {
                            rr();
                        } else if(frameCount < 350) {
                            if(danceOn) {
                                rr();
                            } else {
                                rl();
                            }

                            if(frameCount%2 == 0) {
                                fwd();
                            } else {
                                back();
                            }
                            danceOn = !danceOn;
                        } else {
                            stop();
                        }
                        return;
                    }

                    int[] intArray = new int[bmp.getWidth() * bmp.getHeight()];
                    //copy pixel data from the Bitmap into the 'intArray' array
                    bmp.getPixels(intArray, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());

                    LuminanceSource source = new RGBLuminanceSource(bmp.getWidth(), bmp.getHeight(), intArray);
                    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                    QRCodeMultiReader decoder = new QRCodeMultiReader();

                    try {
                        Result result = decoder.decode(bitmap);
                        if (result.getText().contains("weixin")) {
                            setStatus("1");
                            dead = true;
                            if(mp.isPlaying()){
                                mp.stop();
                            }
                            mp.start();
                        } else {
                            setStatus("2");
                            dead = true;
                            if(mp.isPlaying()){
                                mp.stop();
                            }
                            mp.start();
                        }
                    } catch (NotFoundException e) {
                    } catch (Exception ex) {
                    }


                    ByteBuffer data = ByteBuffer.allocateDirect(frame.getByteBuffer().capacity());
                    data.put(frame.getByteBuffer());
                    ByteBuffer buffer = ByteBuffer.wrap(data.array());
                    bm.copyPixelsFromBuffer(buffer);

                    bm = getResizedBitmap(bm, 320);

                    try {
                        int size = bm.getRowBytes() * bm.getHeight();
                        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
                        bm.copyPixelsToBuffer(byteBuffer);
                        byte[] byteArray = byteBuffer.array();
                        //message sent is BufferMessage, used a txt file to test sending BufferMessage
                        mMessageConnection.sendMessage(new BufferMessage(byteArray));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }

            if (runnable != null) {
                runOnUiThread(runnable);
            }
        }
    };


    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }

        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    public void setStatus(String id) {
        int score;
        if(id.equals("1")) {
            score1++;
            score = score1;
        } else {
            score2++;
            score = score2;
        }

        try {
            HttpClient httpclient = new DefaultHttpClient();
            Log.d("LAL", "doing...");
            HttpGet req = new HttpGet();
            URI website = new URI("http://10.1.0.132/laser_seg/set_teams.php?id=" + id + "&score=" + score);
            req.setURI(website);
            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(req);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private synchronized void startImageTransfer() {
        StreamInfo[] infos = mVision.getActivatedStreamInfo();
        for(StreamInfo info : infos) {
            switch (info.getStreamType()) {
                case StreamType.COLOR:
                    colorInfo = info;
                    mVision.startListenFrame(StreamType.COLOR, mFrameListener);
                    break;
            }
        }

    }

    /**
     * Stop transfer raw image data
     */
    private synchronized void stopImageTransfer() {
        mVision.stopListenFrame(StreamType.COLOR);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        if (press == 0) {
            Toast.makeText(this, "press again to exit", Toast.LENGTH_SHORT).show();
        }
        press++;
        if (press == 2) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (null != this.getCurrentFocus()) {
            InputMethodManager mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            return mInputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            mRobotMessageRouter.unregister();
        } catch (RobotException e) {
            e.printStackTrace();
        }
        mRobotMessageRouter.unbindService();
        Log.d(TAG, "onDestroy: ");

    }

    private String getDeviceIp() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        String ip = (ipAddress & 0xFF) + "." +
                ((ipAddress >> 8) & 0xFF) + "." +
                ((ipAddress >> 16) & 0xFF) + "." +
                (ipAddress >> 24 & 0xFF);
        return ip;
    }

    private File createFile() {
        String fileName = "robot_to_mobile.txt";
        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/" + fileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
                String content = "Segway Robotics at the Intel Developer Forum in San Francisco\n";
                FileOutputStream fileOutputStream = null;
                try {
                    fileOutputStream = new FileOutputStream(file);
                    fileOutputStream.write(content.getBytes());
                    fileOutputStream.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    private byte[] packFile(File file) {
        String fileName = file.getAbsolutePath();
        //pack txt file into byte[]
        long fileSize = file.length();
        if (fileSize > Integer.MAX_VALUE) {
            Log.d(TAG, "onClick: file too big...");
            return new byte[0];
        }
        byte[] fileByte = new byte[(int) fileSize];

        int offset = 0;
        int numRead = 0;
        try {
            FileInputStream fileIn = new FileInputStream(file);
            while (offset < fileByte.length && (numRead = fileIn.read(fileByte, offset, fileByte.length - offset)) >= 0) {
                offset += numRead;
            }
            // to be sure all the data has been read
            if (offset != fileByte.length) {
                throw new IOException("Could not completely read file "
                        + file.getName());
            }
            fileIn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] fileNameByte = fileName.getBytes();
        int fileNameSize = fileNameByte.length;
        ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + fileNameSize + (int) fileSize);
        buffer.putInt(fileNameSize);
        buffer.putInt((int) fileSize);
        buffer.put(fileNameByte);
        buffer.put(fileByte);
        buffer.flip();
        byte[] messageByte = buffer.array();
        return messageByte;
    }

    private String saveFile(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int fileNameSize = buffer.getInt();
        int fileSize = buffer.getInt();
        byte[] nameByte = new byte[fileNameSize];
        int position = buffer.position();
        Log.d(TAG, "nameSize=" + fileNameSize + ";fileSize=" + fileSize + ";p=" + position + ";length=" + bytes.length);
        buffer.mark();
        int i = 0;
        while (buffer.hasRemaining()) {
            nameByte[i] = buffer.get();
            i++;
            if (i == fileNameSize) {
                break;
            }
        }
        final String name = new String(nameByte);

        byte[] fileByte = new byte[fileSize];
        i = 0;
        while (buffer.hasRemaining()) {
            fileByte[i] = buffer.get();
            i++;
            if (i == fileSize) {
                break;
            }
        }
        File file = new File(name);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(fileByte);
            Log.d(TAG, "onBufferMessageReceived: file successfully");
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return name;
    }

    private void fwd() {
        if(action == null || !action.equals("fwd")) {
            mBase.setControlMode(Base.CONTROL_MODE_RAW);
            mBase.setLinearVelocity(1f);
            action = "fwd";
        }
    }

    private void back() {
        if(action == null || !action.equals("back")) {
            mBase.setControlMode(Base.CONTROL_MODE_RAW);
            mBase.setLinearVelocity(-1f);
            action = "back";
        }
    }

    private void rl() {
        if(action == null || !action.equals("left")) {
            mBase.setControlMode(Base.CONTROL_MODE_RAW);
            mBase.setAngularVelocity(1f);
            action = "left";
        }
    }

    private void rr() {
        if(action == null || !action.equals("right")) {
            mBase.setControlMode(Base.CONTROL_MODE_RAW);
            mBase.setAngularVelocity(-1f);
            action = "right";
        }
    }

    private void stop() {
        if(action != null) {
            mBase.setAngularVelocity(0f);
            mBase.setLinearVelocity(0f);
            action = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stop();
        if (mBind){
            mVision.unbindService();
            StreamInfo[] infos = mVision.getActivatedStreamInfo();
            for(StreamInfo info : infos) {
                switch (info.getStreamType()) {
                    case StreamType.COLOR:
                        mVision.stopListenFrame(StreamType.COLOR);
                        break;
                    case StreamType.DEPTH:
                        mVision.stopListenFrame(StreamType.DEPTH);
                        break;
                }
            }
        }
    }
}
