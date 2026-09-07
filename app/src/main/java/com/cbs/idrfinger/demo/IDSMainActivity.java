package com.cbs.idrfinger.demo;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.zkteco.android.IDReader.IDPhotoHelper;
import com.zkteco.android.IDReader.WLTService;
import com.zkteco.android.biometric.core.device.ParameterHelper;
import com.zkteco.android.biometric.core.device.TransportType;
import com.zkteco.android.biometric.core.utils.LogHelper;
import com.zkteco.android.biometric.module.idcard.IDCardReader;
import com.zkteco.android.biometric.module.idcard.IDCardReaderFactory;
import com.zkteco.android.biometric.module.idcard.exception.IDCardReaderException;
import com.zkteco.android.biometric.module.idcard.meta.IDCardInfo;

import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class IDSMainActivity extends AppCompatActivity {

    private EditText etSerialName;
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

    private String idSerialName = "/dev/ttyMSM2";
    private final int idBaudrate = 115200;
    private IDCardReader idCardReader = null;
    private CountDownLatch countdownLatch = null;
    private boolean isRun = false;
    private final String TAG = "IDSMainActivity";
    private boolean isOpen = false;
    private String lastId = "";

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.ids_main);
        initView();
        initListener();
    }

    private void initView() {
        etSerialName = (EditText) findViewById(R.id.etSerialName);
        mBtnOne = (Button) findViewById(R.id.btnOne);
        mBtnMul = (Button) findViewById(R.id.btnMul);
        mBtnStop = (Button) findViewById(R.id.btnStop);
        mBtnStop.setEnabled(false);
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

    private void startReadOne() {
        Log.i(TAG, "startReadOne");
        idSerialName = etSerialName.getText().toString();
        if (idSerialName == null || idSerialName.isEmpty()) {
            Toast.makeText(this, "请输入串口号", Toast.LENGTH_LONG).show();
            return;
        }
        clearCardInfo();
        countdownLatch = new CountDownLatch(1);
        startIDCardReader();
        isRun = true;
        try {
            Thread.sleep(10);
            idCardReader.open(0);
            isOpen = true;
            mBtnOne.setEnabled(false);
            mBtnMul.setEnabled(false);
            mBtnStop.setEnabled(true);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "startReadOne thread is start.");
                    while (isRun) {
                        try {
                            idCardReader.findCard(0);
                            idCardReader.selectCard(0);
                            Thread.sleep(50);
                            int retType = idCardReader.readCardEx(0, 1);
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
                        } catch (Exception e) {
                            Log.i(TAG, "startReadOne exception:" + e.getMessage());
                        }
                    }
                    Log.i(TAG, "startReadOne thread is end.");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mBtnOne.setEnabled(true);
                            mBtnMul.setEnabled(true);
                            mBtnStop.setEnabled(false);
                        }
                    });
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "startReadOne exception:" + e.getMessage());
        }
    }

    private void startReadMul() {
        Log.i(TAG, "startReadMul");
        idSerialName = etSerialName.getText().toString();
        if (idSerialName == null || idSerialName.isEmpty()) {
            Toast.makeText(this, "请输入串口号", Toast.LENGTH_LONG).show();
            return;
        }
        clearCardInfo();
        countdownLatch = new CountDownLatch(1);
        startIDCardReader();
        isRun = true;
        try {
            Thread.sleep(10);
            idCardReader.open(0);
            isOpen = true;
            mBtnOne.setEnabled(false);
            mBtnMul.setEnabled(false);
            mBtnStop.setEnabled(true);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (isRun) {
                        try {
                            idCardReader.findCard(0);
                            idCardReader.selectCard(0);
                            Thread.sleep(50);
                            int retType = idCardReader.readCardEx(0, 1);
                            if (retType == 1 || retType == 2 || retType == 3) {
                                final IDCardInfo idCardInfo = idCardReader.getLastIDCardInfo();
                                if (retType == 1 && idCardInfo != null) { //读卡成功
                                    Log.i(TAG, "read card success.");
                                    readCardInfo(idCardInfo);
                                    //if (lastId != null && idCardInfo.getId() != null && !lastId.equalsIgnoreCase(idCardInfo.getId())) {
                                        playMusic();
                                        lastId = idCardInfo.getId();
                                    //}
                                } else {
                                    Log.i(TAG, "read card failed 1.");
                                }
                            } else {
                                Log.i(TAG, "read card failed 2.");
                            }
                        } catch (Exception e) {
                            Log.i(TAG, "startReadOne exception:" + e.getMessage());
                        }
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mBtnOne.setEnabled(true);
                            mBtnMul.setEnabled(true);
                            mBtnStop.setEnabled(false);
                        }
                    });
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "startReadOne exception:" + e.getMessage());
        }


    }

    private void closeDevice() {
        {
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
            Log.i(TAG, "设备断开连接");
            isOpen = false;
        }
    }

    private void startIDCardReader() {
        Log.i(TAG, "startIDCardReader");
        LogHelper.setLevel(Log.VERBOSE);
        Map idrparams = new HashMap();
        idrparams.put(ParameterHelper.PARAM_SERIAL_SERIALNAME, idSerialName);
        idrparams.put(ParameterHelper.PARAM_SERIAL_BAUDRATE, idBaudrate);
        idCardReader = IDCardReaderFactory.createIDCardReader(this, TransportType.SERIALPORT, idrparams);
    }

    private void stopIDCardReader() {
        Log.i(TAG, "stopIDCardReader");
        if (null != countdownLatch) {
            try {
                countdownLatch.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            idCardReader.close(0);
        } catch (IDCardReaderException e) {
            e.printStackTrace();
        }

    }

    private void readCardInfo(IDCardInfo idCardInfo) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "readCardInfo");
                mTvName.setText(idCardInfo.getName());
                mTvSex.setText(idCardInfo.getSex());
                mTvNation.setText(idCardInfo.getNation());
                mTvBirthY.setText(idCardInfo.getBirth().substring(0, 4));
                mTvBirthM.setText(idCardInfo.getBirth().substring(5, 7));
                mTvBirthD.setText(idCardInfo.getBirth().substring(8, 10));
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
        Log.i(TAG, "clearCardInfo");
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

    @Override
    protected void onStart() {
        super.onStart();
        powerOn();
        startIDCardReader();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopIDCardReader();
        powerOff();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeDevice();
        IDCardReaderFactory.destroy(idCardReader);

    }

    private void powerOn() {
        Log.i(TAG, "powerOn");
        String path = "/sys/kernel/usb_switch/m6_vbus";
        try {
            FileWriter writer = new FileWriter(path);
            writer.write("on");
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void powerOff() {
        Log.i(TAG, "powerOff");
        String path = "/sys/kernel/usb_switch/m6_vbus";
        try {
            FileWriter writer = new FileWriter(path);
            writer.write("off");
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
