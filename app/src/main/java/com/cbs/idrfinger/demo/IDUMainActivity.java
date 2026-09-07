package com.cbs.idrfinger.demo;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.sdk.mylibrary.lib.USBHelper;
import com.zkteco.android.IDReader.IDPhotoHelper;
import com.zkteco.android.IDReader.WLTService;
import com.zkteco.android.biometric.core.device.ParameterHelper;
import com.zkteco.android.biometric.core.device.TransportType;
import com.zkteco.android.biometric.core.utils.LogHelper;
import com.zkteco.android.biometric.module.idcard.IDCardReader;
import com.zkteco.android.biometric.module.idcard.IDCardReaderFactory;
import com.zkteco.android.biometric.module.idcard.exception.IDCardReaderException;
import com.zkteco.android.biometric.module.idcard.meta.IDCardInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class IDUMainActivity extends AppCompatActivity {

    private Button mBtnOne;
    private Button mBtnMul;
    private Button mBtnStop;

    private TextView mTvName;
    private TextView mTvSex;
    private TextView mTvNation;
    private TextView mTvBirthY;
    private TextView mTvBirthM;
    private TextView mTvBirthD;
    private TextView mTvAddress;
    private TextView mTvID;
    private ImageView mIvID;
    private TextView mTvDepart;
    private TextView mTvValidityTime;

    private boolean hasUsbPermission;
    private final String ACTION_USB_PERMISSION = "com.example.scarx.idcardreader.USB_PERMISSION";
    private UsbManager musbManager = null;
    private static final int VID = 1024;    //IDR VID
    private static final int PID = 50010;     //IDR PID
    private boolean isOpen = false;
    private IDCardReader idCardReader = null;
    private final String TAG = "IDUMainActivity";
    private CountDownLatch countdownLatch = null;
    private boolean isRun = false;
    private String lastId = "";

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.idu_main);
        initView();
        initListener();
        requestUsbPermission();
        registerUsbReceiver();
    }



    private void startIDCardReader() {
        // Define output log level
        LogHelper.setLevel(Log.VERBOSE);
        // Start fingerprint sensor
        Map idrparams = new HashMap();
        idrparams.put(ParameterHelper.PARAM_KEY_VID, VID);
        idrparams.put(ParameterHelper.PARAM_KEY_PID, PID);
        idCardReader = IDCardReaderFactory.createIDCardReader(this, TransportType.USB, idrparams);
    }

    private void startReadMul() {
        if (!isOpen) {
            Log.i(TAG, "startReadMul Device is not open.");
            requestUsbPermission();
        }
        countdownLatch = new CountDownLatch(1);
        isRun = true;
        Log.i(TAG, "startReadMul read card thread start.");
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(isRun) {
                    try {
                        if (!isOpen) {
                            continue;
                        }
                        Thread.sleep(10);
                        idCardReader.findCard(0);
                        idCardReader.selectCard(0);
                        Thread.sleep(50);
                        int retType = 0;
                        retType = idCardReader.readCardEx(0, 1);
                        if (retType == 1 || retType == 2 || retType == 3) {
                            final IDCardInfo idCardInfo = idCardReader.getLastIDCardInfo();
                            if (retType == 1 && idCardInfo != null) { //读卡成功
                                Log.i(TAG, "startReadMul read card success.");
                                readCardInfo(idCardInfo);
                                if (lastId != null && idCardInfo.getId() != null && !lastId.equalsIgnoreCase(idCardInfo.getId())) {
                                    playMusic();
                                    lastId = idCardInfo.getId();
                                }

                            } else {
                                Log.i(TAG, "startReadMul read card failed 1.");
                            }
                        } else {
                            Log.i(TAG, "startReadMul read card failed 2.");
                        }
                    }catch (Exception e) {
//                        Log.i(TAG, "startReadMul read card exception.");
//                        e.printStackTrace();
                    }
                }
                Log.i(TAG, "startReadMul read card thread end.");
            }
        }).start();
    }

    private void startReadOne() {
        if (!isOpen) {
            Log.i(TAG, "Device is not open.");
            requestUsbPermission();
        }
        countdownLatch = new CountDownLatch(1);
        isRun = true;
        Log.i(TAG, "read card thread start.");
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(isRun) {
                    try {
                        if (!isOpen) {
                            continue;
                        }
                        Thread.sleep(10);
                        idCardReader.findCard(0);
                        idCardReader.selectCard(0);
                        Thread.sleep(50);
                        int retType = 0;
                        retType = idCardReader.readCardEx(0, 1);
                        if (retType == 1 || retType == 2 || retType == 3) {
                            final IDCardInfo idCardInfo = idCardReader.getLastIDCardInfo();
                            if (retType == 1 && idCardInfo != null) { //读卡成功
                                Log.i(TAG, "read card success.");
                                readCardInfo(idCardInfo);
                                playMusic();
                                isRun = false;//读卡成功,退出
                            } else {
                                Log.i(TAG, "read card failed 1.");
                            }
                        } else {
                            Log.i(TAG, "read card failed 2.");
                        }
                    }catch (Exception e) {
                        Log.i(TAG, "read card exception.");
                        e.printStackTrace();
                    }
                }
                Log.i(TAG, "read card thread end.");
            }
        }).start();
    }

    private void readCardInfo(IDCardInfo idCardInfo) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTvName.setText(idCardInfo.getName());
                mTvSex.setText(idCardInfo.getSex());
                mTvNation.setText(idCardInfo.getNation());
                mTvBirthY.setText(idCardInfo.getBirth().substring(0,4));
                mTvBirthM.setText(idCardInfo.getBirth().substring(5,7));
                mTvBirthD.setText(idCardInfo.getBirth().substring(8,10));
                mTvAddress.setText(idCardInfo.getAddress());
                mTvID.setText(idCardInfo.getId());
                mTvDepart.setText(idCardInfo.getDepart());
                mTvValidityTime.setText(idCardInfo.getValidityTime());

                if (idCardInfo.getPhotolength() > 0) {
                    byte[] buf = new byte[WLTService.imgLength];
                    if (1 == WLTService.wlt2Bmp(idCardInfo.getPhoto(), buf)) {
                        mIvID.setVisibility(View.VISIBLE);
                        mIvID.setImageBitmap(IDPhotoHelper.Bgr2Bitmap(buf));
                    }
                }
            }
        });
    }

    private void clearCardInfo() {
        mTvName.setText("");
        mTvSex.setText("");
        mTvNation.setText("");
        mTvBirthY.setText("");
        mTvBirthM.setText("");
        mTvBirthD.setText("");
        mTvAddress.setText("");
        mTvID.setText("");
        mTvDepart.setText("");
        mTvValidityTime.setText("");
        mIvID.setVisibility(View.INVISIBLE);
        lastId = "";
    }

    private void playMusic() {
        Log.i(TAG, "playMusic");
        MediaPlayer player = MediaPlayer.create(this, R.raw.di);
        player.start();
    }

    private void openDevice() {
        if (isOpen) {
            Log.i(TAG, "openDevice has open.");
            return;
        }
        try {
            startIDCardReader();
            idCardReader.open(0);
            Log.i(TAG, "连接设备成功");
            isOpen = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeDevice() {
        if (!isOpen) {
            Log.i(TAG, "closeDevice has close.");
            return;
        }
        isRun = false;
        if (null != countdownLatch) {
            try {
                countdownLatch.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            clearCardInfo();
            idCardReader.close(0);
        } catch (IDCardReaderException e) {
            e.printStackTrace();
        }
        Log.i(TAG,"设备断开连接");
        isOpen = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        requestUsbPermission();
    }

    private void requestUsbPermission() {
        musbManager = (UsbManager)this.getSystemService(Context.USB_SERVICE);
        for (UsbDevice device : musbManager.getDeviceList().values()) {
            if (device.getVendorId() == VID && device.getProductId() == PID) {
                Intent intent = new Intent(ACTION_USB_PERMISSION);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
                musbManager.requestPermission(device, pendingIntent);
            }
        }
    }

    private void registerUsbReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);
    }

    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Log.i(TAG, getResources().getString(R.string.usb_device_detached));
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    if (device.getVendorId() == VID && device.getProductId() == PID) {
                        Log.i(TAG, "device detached.");
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                Log.i(TAG, getResources().getString(R.string.usb_device_attached));
                if (device != null) {
                    if (device.getVendorId() == VID && device.getProductId() == PID) {
                        Log.i(TAG, "device attached.");
                        requestUsbPermission();
                    }
                }
            }

            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        openDevice();
                    } else {
                        Toast.makeText(IDUMainActivity.this, "USB未授权", Toast.LENGTH_SHORT).show();
                        //mTxtReport.setText("USB未授权");
                    }
                }
            }
        }
    };

    private void initView() {
        mBtnOne = (Button) findViewById(R.id.btnOne);
        mBtnMul = (Button) findViewById(R.id.btnMul);
        mBtnStop = (Button) findViewById(R.id.btnStop);

        mTvName = (TextView) findViewById(R.id.tvName);
        mTvSex = (TextView) findViewById(R.id.tvSex);
        mTvNation = (TextView) findViewById(R.id.tvNation);
        mTvBirthY = (TextView) findViewById(R.id.tvBirthY);
        mTvBirthM = (TextView) findViewById(R.id.tvBirthM);
        mTvBirthD = (TextView) findViewById(R.id.tvBirthD);
        mTvAddress = (TextView) findViewById(R.id.tvAddress);
        mTvID = (TextView) findViewById(R.id.tvID);
        mIvID = (ImageView) findViewById(R.id.ivID);
        mTvDepart = (TextView) findViewById(R.id.tvDepart);
        mTvValidityTime = (TextView) findViewById(R.id.tvValidityTime);
    }

    private void initListener() {
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.btnOne) {
                    startReadOne();
                } else if (v.getId() == R.id.btnMul) {
                    startReadMul();
                } else if (v.getId() == R.id.btnStop) {
                    closeDevice();
                }
            }
        };
        mBtnOne.setOnClickListener(listener);
        mBtnMul.setOnClickListener(listener);
        mBtnStop.setOnClickListener(listener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeDevice();
    }


}
