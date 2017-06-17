package com.segway.robot.mobilesample;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.segway.robot.mobile.sdk.connectivity.BufferMessage;
import com.segway.robot.mobile.sdk.connectivity.MobileException;
import com.segway.robot.mobile.sdk.connectivity.MobileMessageRouter;
import com.segway.robot.mobile.sdk.connectivity.StringMessage;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.baseconnectivity.Message;
import com.segway.robot.sdk.baseconnectivity.MessageConnection;
import com.segway.robot.sdk.baseconnectivity.MessageRouter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static android.widget.Toast.*;

public class MainActivity extends Activity {

    private static final String TAG = "MobileActivity";
    private EditText editTextContext;
    private EditText editTextIp;

    private Button fwdButton;
    private Button backButton;
    private Button rlButton;
    private Button rrButton;
    private Button stopButton;

    private ImageView previewRobot;

    private int press = 0;
    private MobileMessageRouter mMobileMessageRouter = null;
    private MessageConnection mMessageConnection = null;

    private ServiceBinder.BindStateListener mBindStateListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            Log.d(TAG, "onBind: ");
            try {
                mMobileMessageRouter.register(mMessageConnectionListener);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onUnbind(String reason) {
            Log.e(TAG, "onUnbind: " + reason);
        }
    };

    private MessageRouter.MessageConnectionListener mMessageConnectionListener = new MessageRouter.MessageConnectionListener() {
        @Override
        public void onConnectionCreated(final MessageConnection connection) {
            Log.d(TAG, "onConnectionCreated: " + connection.getName());
            //get the MessageConnection instance
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
            //connection between mobile application and robot application is opened.
            //Now can send messages to each other.
            Log.d(TAG, "onOpened: " + mMessageConnection.getName());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    makeText(getApplicationContext(), "connected to: " + mMessageConnection.getName(), LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onClosed(String error) {
            //connection closed with error
            Log.e(TAG, "onClosed: " + error + ";name=" + mMessageConnection.getName());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    makeText(getApplicationContext(), "disconnected to: " + mMessageConnection.getName(), LENGTH_SHORT).show();
                }
            });
        }
    };

    private MessageConnection.MessageListener mMessageListener = new MessageConnection.MessageListener() {
        @Override
        public void onMessageReceived(final Message message) {
            // message received
            Log.d(TAG, "onMessageReceived: id=" + message.getId() + ";timestamp=" + message.getTimestamp());
            if (message instanceof StringMessage) {
                //message received is StringMessage
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    }
                });
            } else {
                //message received is BufferMessage, used a txt file to test receiving BufferMessage
                final byte[] bytes = (byte[]) message.getContent();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Bitmap bm = Bitmap.createBitmap(240, 160, Bitmap.Config.RGB_565);
                            ByteBuffer buffer = ByteBuffer.wrap(bytes);
                            bm.copyPixelsFromBuffer(buffer);
                            previewRobot.setImageBitmap(bm);
                        }catch(Exception ex){

                        }
                    }
                });
            }
        }

        @Override
        public void onMessageSentError(Message message, String error) {

        }

        @Override
        public void onMessageSent(Message message) {
            //the message  that is sent successfully
            Log.d(TAG, "onMessageSent: id=" + message.getId() + ";timestamp=" + message.getTimestamp());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: ");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);


        //get the MobileMessageRouter instance
        mMobileMessageRouter = MobileMessageRouter.getInstance();

        previewRobot = (ImageView) findViewById(R.id.imageView);
        fwdButton = (Button) findViewById(R.id.fwd_btn);
        fwdButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN ) {
                    try {
                        //message sent is StringMessage
                        mMessageConnection.sendMessage(new StringMessage("fwd"));
                    } catch (MobileException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP ){
                    try {
                        //message sent is StringMessage
                        mMessageConnection.sendMessage(new StringMessage("stop"));
                    } catch (MobileException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }
        });

        backButton = (Button) findViewById(R.id.back_btn);
        backButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN ) {
                    try {
                        //message sent is StringMessage
                        mMessageConnection.sendMessage(new StringMessage("back"));
                    } catch (MobileException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP ){
                    try {
                        //message sent is StringMessage
                        mMessageConnection.sendMessage(new StringMessage("stop"));
                    } catch (MobileException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }
        });

        rlButton = (Button) findViewById(R.id.rl_btn);
        rlButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN ) {
                    try {
                        //message sent is StringMessage
                        mMessageConnection.sendMessage(new StringMessage("left"));
                    } catch (MobileException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP ){
                    try {
                        //message sent is StringMessage
                        mMessageConnection.sendMessage(new StringMessage("stop"));
                    } catch (MobileException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }


        });

        rrButton = (Button) findViewById(R.id.rr_btn);
        rrButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN ) {
                    try {
                        //message sent is StringMessage
                        mMessageConnection.sendMessage(new StringMessage("right"));
                    } catch (MobileException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP ){
                    try {
                        //message sent is StringMessage
                        mMessageConnection.sendMessage(new StringMessage("stop"));
                    } catch (MobileException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }
        });

        stopButton = (Button) findViewById(R.id.stop_btn);
        stopButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN ) {
                    try {
                        //message sent is StringMessage
                        mMessageConnection.sendMessage(new StringMessage("stop"));
                    } catch (MobileException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP ){
                    try {
                        //message sent is StringMessage
                        mMessageConnection.sendMessage(new StringMessage("stop"));
                    } catch (MobileException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }
        });

        mMobileMessageRouter.setConnectionIp("10.1.0.241");
        //bind the connection service in robot
        mMobileMessageRouter.bindService(this, mBindStateListener);

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMobileMessageRouter.unregister();
        mMobileMessageRouter.unbindService();
        Log.d(TAG, "onDestroy: ");
    }

    @Override
    public void onBackPressed() {
        if (press == 0) {
            makeText(this, "press again to exit", LENGTH_SHORT).show();
        }
        press++;
        if (press == 2) {
            super.onBackPressed();
        }
    }

    private File createFile() {
        String fileName = Environment.getExternalStorageDirectory().getPath() + "/mobile_to_robot.txt";
        File file = new File(fileName);
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
}
