package com.segway.robot.robotsample;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
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
import android.widget.TextView;
import android.widget.Toast;

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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MainActivity extends Activity {

    private static final String TAG = "RobotActivity";
    private int press = 0;
    private TextView textViewIp;

    private TextView textViewId;
    private TextView textViewTime;
    private TextView textViewContent;
    private RobotMessageRouter mRobotMessageRouter = null;
    private MessageConnection mMessageConnection = null;

    private Base mBase;
    private Vision mVision;
    private boolean mBind;

    private String action = null;

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
                        textViewId.setText(Integer.toString(message.getId()));
                        textViewTime.setText(Long.toString(message.getTimestamp()));
                        textViewContent.setText(message.getContent().toString());
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
                        textViewId.setText(Integer.toString(message.getId()));
                        textViewTime.setText(Long.toString(message.getTimestamp()));
                        textViewContent.setText(name);
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

        textViewId = (TextView) findViewById(R.id.textView_id);
        textViewTime = (TextView) findViewById(R.id.textView_time);
        textViewContent = (TextView) findViewById(R.id.textView_content);
        textViewContent.setMovementMethod(ScrollingMovementMethod.getInstance());

        textViewIp.setText(getDeviceIp());

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
            Runnable runnable = null;
            switch (streamType) {
                case StreamType.COLOR:
                    Bitmap bm = Bitmap.createBitmap(640, 480, Bitmap.Config.RGB_565);
                    ByteBuffer data = ByteBuffer.allocateDirect(frame.getByteBuffer().capacity());
                    data.put(frame.getByteBuffer());
                    ByteBuffer buffer = ByteBuffer.wrap(data.array());
                    bm.copyPixelsFromBuffer(buffer);
                    Bitmap.createScaledBitmap(bm, 240, 160, false);

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

    private synchronized void startImageTransfer() {
        StreamInfo[] infos = mVision.getActivatedStreamInfo();
        for(StreamInfo info : infos) {
            switch (info.getStreamType()) {
                case StreamType.COLOR:
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
