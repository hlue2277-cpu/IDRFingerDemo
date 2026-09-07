package com.cbs.idrfinger.demo;


import static android.content.ContentValues.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.MediaPlayer;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.sdk.ErrorStatus;
import com.sdk.mylibrary.FingerHelper;
import com.sdk.mylibrary.ImageUtils;
import com.sdk.mylibrary.FingerListener;
import com.sdk.mylibrary.UsbProtocol;
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
import com.zkteco.android.biometric.module.idcard.meta.IDPRPCardInfo;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class SerialMainActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, FingerListener {
    private String idSerialName = "/dev/ttyMSM2";
    private final int idBaudrate = 115200;
    private IDCardReader idCardReader = null;
    private TextView textView = null;
    private ImageView imageView = null;
    private boolean bopen = false;
    private boolean bStoped = false;
    private int mReadCount = 0;
    private CountDownLatch countdownLatch = null;
    private EditText idEditText = null;
    private Context mContext = null;

    private Button btnOpenOne, btn_captureimage, btn_cali, btnCloseDevice, btn_stop_capture, btn_create_template1, btn_create_template2, btn_match_template;
    private ImageView mIv_ProImage;
    private CheckBox ck_capture, ck_mcucapture, mCb_SaveProImage, mCk_redimage;
    private TextView tv_showinfo, tv_FirmwareVersion;
    private Thread captureThread = null;
    boolean isopened = false;
    int result = 0;
    public byte[] mProImageBuf = new byte[92160];
    public byte[] mEmptyImageBuf = new byte[92160];
    ImageUtils mImageUtils;
    Bitmap mBitmap;
    byte[] imagebuf = null;
    int imagequility;
    FingerHelper fingerHelper;
    int delete_fid = 0;
    byte[] b = new byte[4];
    EditText editText;
    private Button btn_enroll, btn_verify, btn_delete, btn_getallfinger, btn_delete_one_finger, btn_continue_verify, btn_cancel;
    int supmaxnumber[] = new int[1];
    int maxleng = 1500;
    public byte[] create_template1_208_192 = new byte[UsbProtocol.CSALG_TEMPLATECOUNT_208_192];
    public byte[] create_template2_208_192 = new byte[UsbProtocol.CSALG_TEMPLATECOUNT_208_192];
    public byte[] create_template1 = new byte[UsbProtocol.CSALG_TEMPLATECOUNT];
    public byte[] create_template2 = new byte[UsbProtocol.CSALG_TEMPLATECOUNT];

    int mEnrollfid = 0;
    int mVerifyfid = 0;
    int mEnrollCode = -1;
    String mErrorMsg = "";
    int mVerifyCode = -1;
    byte[] mFingerinfo = null;
    int mFingercount = 0;
    boolean isenroll_live = false;
    boolean isverify_live = false;
    boolean needimage_enroll = false;
    boolean needimage_verify = false;
    boolean iscapture = false;


    private byte[] fingerFeature = new byte[1024];
    private byte[] fingerImage = new byte[92160];
    private boolean readFingerSuccess = false;
    private boolean readIdFeatureSuccess = false;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private final String TAG = "SerialMainActivity";
    private static String[] PERMISSIONS_STORAGE = {"android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};
    private String mId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.serial_main);
        textView = (TextView) findViewById(R.id.textView);
        imageView = (ImageView) findViewById(R.id.imageView);
        idEditText = (EditText)findViewById(R.id.editText);
        mContext = this.getApplicationContext();
        mContext = this.getApplicationContext();

        registerUsbControl();
        InitView();
        initButton();
        initFingerHelper();
    }

    private void initFingerHelper() {
        mImageUtils = ImageUtils.getInstance(this);
        fingerHelper = new FingerHelper(this);
        fingerHelper.setFingerListener(this);
        fingerHelper.setFingerColor(Color.BLACK);
    }

    private void InitView() {
        btnOpenOne = findViewById(R.id.btn_openOne);
        btnOpenOne.setOnClickListener(this);
        btn_captureimage = findViewById(R.id.btn_captureimage);
        btn_captureimage.setOnClickListener(this);
        btn_cali = findViewById(R.id.btn_cali);
        btn_cali.setOnClickListener(this);
        btn_stop_capture = findViewById(R.id.stop_capture);
        btn_stop_capture.setOnClickListener(this);
        mIv_ProImage = findViewById(R.id.iv_proimg);
        btnCloseDevice = findViewById(R.id.btn_close);
        btnCloseDevice.setOnClickListener(this);
        tv_showinfo = findViewById(R.id.tv_result);
        tv_FirmwareVersion = findViewById(R.id.tv_FirmwareVersion);
        ck_capture = findViewById(R.id.ck_capture);
        ck_capture.setOnCheckedChangeListener(this);
        mCk_redimage = findViewById(R.id.ck_redimage);
        mCk_redimage.setOnCheckedChangeListener(this);
        ck_mcucapture = findViewById(R.id.ck_mcucapture);
        mCb_SaveProImage = findViewById(R.id.ck_save_proimage);
        mCb_SaveProImage.setOnCheckedChangeListener(this);
        btn_create_template1 = findViewById(R.id.btn_create_template1);
        btn_create_template1.setOnClickListener(this);
        btn_create_template2 = findViewById(R.id.btn_create_template2);
        btn_create_template2.setOnClickListener(this);
        btn_match_template = findViewById(R.id.btn_match_template);
        btn_match_template.setOnClickListener(this);

        btn_enroll = findViewById(R.id.btn_enroll);
        btn_enroll.setOnClickListener(this);
        btn_verify = findViewById(R.id.btn_verify);
        btn_verify.setOnClickListener(this);
        btn_delete = findViewById(R.id.btn_delete_finger);
        btn_delete.setOnClickListener(this);
        btn_getallfinger = findViewById(R.id.btn_get_allfinger);
        btn_getallfinger.setOnClickListener(this);
        btn_delete_one_finger = findViewById(R.id.btn_delete_one_finger);
        btn_delete_one_finger.setOnClickListener(this);
        btn_continue_verify = findViewById(R.id.btn_continue_verify);
        btn_continue_verify.setOnClickListener(this);
        btn_cancel = findViewById(R.id.btn_cancel);
        btn_cancel.setOnClickListener(this);
        editText = findViewById(R.id.edt_enrill_id);
    }

    public void registerUsbControl() {
        IntentFilter usbDeviceStateFilter = new IntentFilter();
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, usbDeviceStateFilter);
        verifyPermission();
    }

    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Log.i(TAG, getResources().getString(R.string.usb_device_detached));
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    Log.i(TAG, "pid = " + device.getProductId() + "vid :" + device.getVendorId());
                    if (USBHelper.PID == device.getProductId() && USBHelper.VID == device.getVendorId()) {
                        isopened = false;
                        fingerHelper.FingerprintCancell();
                        fingerHelper.close();
                        initButton();
                        tv_showinfo.setText(getResources().getString(R.string.usb_device_detached));
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                Log.i(TAG, getResources().getString(R.string.usb_device_attached));
                if (device != null) {
                    Log.i(TAG, "pid = " + device.getProductId() + "vid :" + device.getVendorId());
                    if (USBHelper.PID == device.getProductId() && USBHelper.VID == device.getVendorId()) {
                        tv_showinfo.setText(getResources().getString(R.string.usb_device_attached));
                        if (tv_FirmwareVersion != null) {
                            tv_FirmwareVersion.setText("");
                        }
                    }
                }
            }
        }
    };

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean ischecked) {
        int id = compoundButton.getId();
        switch (id) {
            case R.id.ck_capture:
                System.out.println("ck_capture");
                if (!ischecked) {
                    if (isopened) {
                        btnCloseDevice.setEnabled(true);
                        btn_cali.setEnabled(true);
                        btn_captureimage.setEnabled(true);
                        btnOpenOne.setEnabled(false);
                        fingerHelper.stopGetFinger();
                    }
                }
                break;
            case R.id.ck_redimage:
                System.out.println("红化指纹");
                if (ischecked) {
                    fingerHelper.setFingerColor(Color.RED);
                } else {
                    fingerHelper.setFingerColor(Color.BLACK);
                }
                break;
        }
    }

    @Override
    public void onCatched(int qulite, Bitmap bitmap, byte[] bytes) {

        if (bytes.length != 0) {
            imagebuf = new byte[bytes.length];
            mBitmap = bitmap;
            imagebuf = bytes;
            imagequility = qulite;
            mProImageBuf = Arrays.copyOf(bytes, bytes.length);
            timeHandler.sendEmptyMessage(100);
            if (!Arrays.equals(fingerImage, bytes)) {
                System.arraycopy(bytes, 0, fingerImage, 0, 92160);
            }
        }
    }

    @Override
    public void onError(int i, String s) {

    }

    @Override
    public void onEnrollSuccess(int enrollfid) {
        mEnrollfid = enrollfid;
        timeHandler.sendEmptyMessage(101);
    }

    @Override
    public void onEnrollFailed(int enrollfid, int errorcode, String errormsg) {
        mEnrollfid = enrollfid;
        mEnrollCode = errorcode;
        mErrorMsg = errormsg;
        System.out.println("onEnrollFailed" + errormsg);
        timeHandler.sendEmptyMessage(102);
    }

    @Override
    public void onVerifySuccess(int verifyfid) {
        mVerifyfid = verifyfid;
        timeHandler.sendEmptyMessage(103);
    }

    @Override
    public void onVerifyFailed(int errorcode, String errormsg) {
        mVerifyCode = errorcode;
        System.out.println("onEnrollFailed" + errormsg);
        mErrorMsg = errormsg;
        timeHandler.sendEmptyMessage(104);
    }

    @SuppressLint("HandlerLeak")
    public Handler timeHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 100:
                    if (mBitmap != null) {
                        mIv_ProImage.setImageBitmap(mBitmap);
                        if (mCb_SaveProImage.isChecked()) {
                            //保存路径在:/storage/emulated/0/Android/data/com.yuan.usbhelper/files/Pictures/
                            mImageUtils.FileSaveToInside(System.currentTimeMillis(), "finger", mBitmap);
                        }
                    }
                    tv_showinfo.setText(getResources().getString(R.string.capture_success));
                    readFingerSuccess = true;
                    verifyHandler.sendEmptyMessage(0);
                    if (iscapture) {
                        if (!ck_capture.isChecked()) {
                            iscapture = false;
                            btn_captureimage.setEnabled(true);
                            setEnrollVerifyStatus(true);
                        }
                    }/*else{
                        setEnrollVerifyStatus(true);
                    }*/
                    break;
                case 101:
                    tv_showinfo.setText("录入成功,fid为:" + mEnrollfid);
                    System.out.println("isenroll_live:" + isenroll_live);
                    if (!isenroll_live) {
                        setEnrollVerifyStatus(true);
                        needimage_enroll = false;
                    }
                    break;
                case 102:
                    tv_showinfo.setText("录入失败," + mErrorMsg);
                    if (mEnrollCode == 4 || mEnrollCode == 7) {
                        setEnrollVerifyStatus(true);
                    }
                    mErrorMsg = "";
                    break;
                case 103:
                    tv_showinfo.setText("匹配成功,fid为:" + mVerifyfid);
                    if (!isverify_live) {
                        setEnrollVerifyStatus(true);
                        needimage_verify = false;
                    }
                    break;
                case 104:
                    tv_showinfo.setText("匹配失败:" + mErrorMsg);
                    mErrorMsg = "";
                    if (mVerifyCode == 0x09) {
                        setEnrollVerifyStatus(true);
                    }
                    break;
            }
        }
    };

    void getFirmwareVersion() {
        byte[] version = new byte[46];
        byte[] algversion = new byte[17];
        int[] len = new int[1];
        fingerHelper.GetFirmwareVersion(0, version, len);
        System.out.println("version:" + new String(version) + "len:" + len[0]);
        System.arraycopy(version, 0, algversion, 0, 17);
        byte[] firmware = new byte[len[0] - 17];
        System.arraycopy(version, 17, firmware, 0, len[0] - 17);
        System.out.println("algversion:" + new String(algversion) + "firmware:" + new String(firmware));
        tv_FirmwareVersion.setText(getResources().getString(R.string.firmversion) + new String(firmware));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_openOne:
                result = fingerHelper.open(this);
                System.out.println(result);
                if (result == 0) {
                    initOpenDeviceButton();
                    isopened = true;
                    System.out.println(getResources().getString(R.string.opendevice_success));
                    tv_showinfo.setText(getResources().getString(R.string.opendevice_success));
                    result = fingerHelper.GetMaxFingerCount(supmaxnumber);
                    System.out.println("maxnumber:" + supmaxnumber[0] + "result:" + result);
                    //getFirmwareVersion();

                } else if(result== ErrorStatus.CS_SPI_ERROR){
                    isopened = false;
                    tv_showinfo.setText("SPI通讯异常");
                }else if (result==ErrorStatus.CS_USB_NOT_FIND){
                    isopened = false;
                    tv_showinfo.setText("没有检测到固定USB设备");
                }else if(result==ErrorStatus.CS_USB_OPEN_FAILED){
                    isopened = false;
                    tv_showinfo.setText("USB设备打开失败");
                }
                break;
            case R.id.stop_capture:
                btnCloseDevice.setEnabled(true);
                btn_cali.setEnabled(true);
                btn_captureimage.setEnabled(true);
                btnOpenOne.setEnabled(false);
                fingerHelper.stopGetFinger();
                tv_showinfo.setText(getResources().getString(R.string.stopcapture));
                iscapture = false;
                break;
            case R.id.btn_captureimage:
                if (isopened) {
                    mIv_ProImage.setImageBitmap(BitmapFactory.decodeByteArray(mEmptyImageBuf, 0, 92160));
                    if (ck_capture.isChecked()) {
                        btn_cali.setEnabled(false);
                        btnCloseDevice.setEnabled(false);
                        btn_captureimage.setEnabled(false);
                        fingerHelper.getFingerImage(true);

                    } else {
                        fingerHelper.getFingerImage(false);
                    }
                    iscapture = true;
                    tv_showinfo.setText(getResources().getString(R.string.pressfinger));
                    setEnrollVerifyStatus(false);
                    btn_stop_capture.setEnabled(true);
                } else {
                    tv_showinfo.setText(getResources().getString(R.string.please_open_device));
                }
                break;
            case R.id.btn_close:
                initButton();
                isopened = false;
                isverify_live = false;
                isenroll_live = false;
                fingerHelper.stopGetFinger();
                fingerHelper.FingerprintCancell();
                boolean close = fingerHelper.close();
                tv_showinfo.setText(getResources().getString(R.string.closedevice_success));
                setEnrollVerifyStatus(false);
                btn_cancel.setEnabled(false);
                break;
            case R.id.btn_cali:
                btn_captureimage.setEnabled(false);
                btn_cali.setEnabled(false);
                btnCloseDevice.setEnabled(false);
                btn_stop_capture.setEnabled(false);
                result = fingerHelper.Calibration(0);
                if (result == 1) {
                    tv_showinfo.setText(getResources().getString(R.string.cali_device_success));
                } else {
                    tv_showinfo.setText(getResources().getString(R.string.opendevice_failed));
                }
                btn_captureimage.setEnabled(true);
                btn_cali.setEnabled(true);
                btnCloseDevice.setEnabled(true);
                btn_stop_capture.setEnabled(true);
                break;
            case R.id.btn_create_template1:
                if (fingerHelper.getSensorType() != UsbProtocol.CX408A) {
                    result = fingerHelper.CreateTemplate(0, imagebuf, create_template1_208_192);
                } else {
                    result = fingerHelper.CreateTemplate(0, imagebuf, create_template1);
                }
                System.out.println("result:" + result);
                if (result > 0) {
                    System.out.println(getResources().getString(R.string.create_template1_success));
                    tv_showinfo.setText(getResources().getString(R.string.create_template1_success));
                } else {
                    System.out.println(getResources().getString(R.string.create_template1_failed));
                    tv_showinfo.setText(getResources().getString(R.string.create_template1_failed));
                }
                break;
            case R.id.btn_create_template2:
                if (fingerHelper.getSensorType() != UsbProtocol.CX408A) {
                    result = fingerHelper.CreateTemplate(0, imagebuf, create_template2_208_192);
                } else {
                    result = fingerHelper.CreateTemplate(0, imagebuf, create_template2);
                }
                System.out.println("result:" + result);
                if (result > 0) {
                    System.out.println(getResources().getString(R.string.create_template2_success));
                    tv_showinfo.setText(getResources().getString(R.string.create_template2_success));
                } else {
                    System.out.println(getResources().getString(R.string.create_template2_failed));
                    tv_showinfo.setText(getResources().getString(R.string.create_template2_failed));
                }
                break;
            case R.id.btn_match_template:
                if (fingerHelper.getSensorType() != UsbProtocol.CX408A) {
                    result = fingerHelper.CompareTemplates(0, create_template1_208_192, create_template2_208_192);
                } else {
                    result = fingerHelper.CompareTemplates(0, create_template1, create_template2);
                }
                System.out.println("result:" + result);
                if (result > 0) {
                    System.out.println(getResources().getString(R.string.match_template_success));
                    tv_showinfo.setText(getResources().getString(R.string.match_template_success));
                } else {
                    System.out.println(getResources().getString(R.string.match_template_failed));
                    tv_showinfo.setText(getResources().getString(R.string.match_template_failed));
                }
                break;
            case R.id.btn_enroll:
                String str = editText.getText().toString();
                if ("".equalsIgnoreCase(str)) {
                    Toast.makeText(this, "请输入正确的数字", Toast.LENGTH_LONG).show();
                    System.out.println("edittest is null ");
                } else {
                    int enroll_fid = Integer.parseInt(str);
                    if (enroll_fid <= maxleng) {
                        System.out.println("edittest is " + enroll_fid);
                        if (enroll_fid == 0) {
                            isenroll_live = true;
                        } else {
                            isenroll_live = false;
                        }
                        needimage_enroll = true;
                        fingerHelper.FingerprintEnroll(Integer.parseInt(str), isenroll_live, needimage_enroll);
                        if (isenroll_live) {
                            tv_showinfo.setText("连续录入中,请按压手指");
                        } else {
                            tv_showinfo.setText("录入指纹id(" + enroll_fid + "),请按压手指");
                        }
                        setEnrollVerifyStatus(false);
                    } else {
                        tv_showinfo.setText("超过录入最大数量");
                    }
                }

                break;
            case R.id.btn_verify:
                isverify_live = false;
                needimage_verify = true;
                fingerHelper.FingerprintVerify(0, isverify_live, needimage_verify);
                tv_showinfo.setText("匹配中..请按压手指");
                setEnrollVerifyStatus(false);
                break;
            case R.id.btn_cancel:
                isenroll_live = false;
                isverify_live = false;
                needimage_verify = false;
                needimage_enroll = false;
                fingerHelper.stopGetFinger();
                fingerHelper.FingerprintCancell();
                tv_showinfo.setText("取消成功..");
                initOpenDeviceButton();
                setEnrollVerifyStatus(true);
                break;
            case R.id.btn_continue_verify:
                isverify_live = true;
                fingerHelper.FingerprintVerify(0, true, true);
                tv_showinfo.setText("连续匹配中,请按压手指");
                setEnrollVerifyStatus(false);
                break;
            case R.id.btn_delete_finger:
                result = fingerHelper.FingerprintDelete(0);
                if (result == 0) {
                    tv_showinfo.setText("删除指纹成功");
                } else {
                    tv_showinfo.setText("删除指纹失败");
                }
                break;
            case R.id.btn_delete_one_finger:
                String str_del = editText.getText().toString();
                if ("".equalsIgnoreCase(str_del)) {
                    Toast.makeText(this, "请输入正确的指纹id", Toast.LENGTH_LONG).show();
                    System.out.println("edittest is null ");
                } else {
                    System.out.println("edittest is " + Integer.parseInt(str_del));
                    delete_fid = Integer.parseInt(editText.getText().toString());
                    if (delete_fid != 0) {
                        if (delete_fid <= maxleng) {
                            result = fingerHelper.FingerprintDelete(delete_fid);
                            if (result == 0) {
                                tv_showinfo.setText("删除指纹成功");
                            } else {
                                tv_showinfo.setText("删除失败,(" + delete_fid + ")指纹不存在");
                            }
                        } else {
                            tv_showinfo.setText("超过指纹最大数量");
                        }
                    } else {
                        tv_showinfo.setText("删除失败,请输入正确的指纹id进行删除");
                    }
                }
                break;

            case R.id.btn_get_allfinger:
                byte[] fingerinfo = new byte[1500]; //最大数量为500
                int[] fingercount = new int[1];
                byte[] fingernumber = new byte[2];
                StringBuilder sb = new StringBuilder();
                result = fingerHelper.getFingerInfo(fingerinfo, fingercount);
                System.out.println("11111111111");
                if (fingercount[0] > 0) {
                    System.out.println(fingercount[0]);
                    for (int i = 0; i < fingercount[0]; i++) {
                        fingernumber[0] = fingerinfo[i * 3 + 0];
                        fingernumber[1] = fingerinfo[i * 3 + 1];
                        sb.append(mImageUtils.Byte2Int(fingernumber));
                        sb.append(",");
                    }
                    tv_showinfo.setText("指纹数量为:" + fingercount[0] + "指纹id为:" + sb.toString());
                } else {
                    tv_showinfo.setText("还没有录入指纹");
                }

                break;
        }
    }

    public void initButton() {
        ck_capture.setEnabled(false);
        mCk_redimage.setEnabled(false);
        btn_cali.setEnabled(false);
        btn_captureimage.setEnabled(false);
        btnCloseDevice.setEnabled(false);
        ck_mcucapture.setEnabled(false);
        btnOpenOne.setEnabled(true);
        btn_create_template1.setEnabled(false);
        btn_create_template2.setEnabled(false);
        btn_match_template.setEnabled(false);
        btn_stop_capture.setEnabled(false);
        setEnrollVerifyStatus(false);
        btn_cancel.setEnabled(false);
    }

    void setEnrollVerifyStatus(boolean status) {
        btn_enroll.setEnabled(status);
        btn_verify.setEnabled(status);
        btn_continue_verify.setEnabled(status);
        btn_cancel.setEnabled(true);
        btn_delete.setEnabled(status);
        btn_delete_one_finger.setEnabled(status);
        btn_getallfinger.setEnabled(status);
        btn_stop_capture.setEnabled(status);
        btn_captureimage.setEnabled(status);
    }

    public void initOpenDeviceButton() {
        mCk_redimage.setEnabled(true);
        ck_capture.setEnabled(true);
        btn_cali.setEnabled(true);
        btn_captureimage.setEnabled(true);
        btnCloseDevice.setEnabled(true);
        btnOpenOne.setEnabled(false);
        btn_create_template1.setEnabled(true);
        btn_create_template2.setEnabled(true);
        btn_match_template.setEnabled(true);
        btn_stop_capture.setEnabled(true);
        ck_mcucapture.setEnabled(true);

        btn_enroll.setEnabled(true);
        btn_verify.setEnabled(true);
        btn_continue_verify.setEnabled(true);
        btn_cancel.setEnabled(true);
        btn_delete.setEnabled(true);
        btn_delete_one_finger.setEnabled(true);
        btn_getallfinger.setEnabled(true);
    }

    public Context getContext()
    {
        return this.getApplicationContext();
    }

    private void startIDCardReader() {
        // Define output log level
        LogHelper.setLevel(Log.VERBOSE);
        // Start fingerprint sensor
        Map idrparams = new HashMap();
        idrparams.put(ParameterHelper.PARAM_SERIAL_SERIALNAME, idSerialName);
        idrparams.put(ParameterHelper.PARAM_SERIAL_BAUDRATE, idBaudrate);
        idCardReader = IDCardReaderFactory.createIDCardReader(this, TransportType.SERIALPORT, idrparams);
    }

    private void stopIDCardReader() {
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



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mUsbReceiver != null) {
            unregisterReceiver(mUsbReceiver);
        }
        // Destroy fingerprint sensor when it's not used
        IDCardReaderFactory.destroy(idCardReader);
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
        isopened = false;
        fingerHelper.stopGetFinger();
        fingerHelper.FingerprintCancell();
        fingerHelper.close();
        stopIDCardReader();
        powerOff();
    }

    private void powerOn() {
        String path = "/sys/kernel/usb_switch/m6_vbus";
        try {
            FileWriter writer = new FileWriter(path);
            writer.write("on");
            writer.flush();
            writer.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void powerOff() {
        String path = "/sys/kernel/usb_switch/m6_vbus";
        try {
            FileWriter writer = new FileWriter(path);
            writer.write("off");
            writer.flush();
            writer.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    long lastTime = 0;
    private void playMusic() {
        if (System.currentTimeMillis() - lastTime < 500) {
            return;
        }
        MediaPlayer player = MediaPlayer.create(SerialMainActivity.this, R.raw.di);
        player.start();
        lastTime = System.currentTimeMillis();
    }

    private void verifyPermission() {
        try {
            int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(
                        SerialMainActivity.this,
                        PERMISSIONS_STORAGE,
                        REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeLogToFile(String log) {
        try {
            File dirFile = new File("/sdcard/zkteco/");  //目录转化成文件夹
            if (!dirFile.exists()) {              //如果不存在，那就建立这个文件夹
                dirFile.mkdirs();
            }
            String path = "/sdcard/zkteco/idrlog.txt";
            File file = new File(path);
            if (!file.exists()) {
                File dir = new File(file.getParent());
                dir.mkdirs();
                file.createNewFile();
            }
            FileOutputStream outStream = new FileOutputStream(file, true);
            log += "\r\n";
            outStream.write(log.getBytes());
            outStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean isRun = false;
    public void OnBnBeginOne(View view) {
        try {
            if (bopen)
            {
                textView.setText("设备已连接");
                return;
            }
            idSerialName = idEditText.getText().toString();
            if (null == idSerialName || idSerialName.isEmpty())
            {
                textView.setText("请输入串口名");
                return;
            }
            startIDCardReader();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            idCardReader.open(0);
            bStoped = false;
            mReadCount = 0;
            writeLogToFile("连接设备成功");
            textView.setText("连接成功");
            bopen = true;
            countdownLatch = new CountDownLatch(1);
            new Thread(new Runnable() {
                public void run() {
                    if (isRun) return;
                    isRun = true;
                    while (!bStoped) {
                        boolean ret = false;
                        final long nTickstart = System.currentTimeMillis();
                        try {
                            idCardReader.findCard(0);
                            idCardReader.selectCard(0);
                        }catch (IDCardReaderException e)
                        {
                            //LogHelper.e("errcode:" + e.getErrorCode() + ",internalerrorcode:" + e.getInternalErrorCode());
                            //continue;
                        }
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        int retType = 0;
                        try {
                            retType = idCardReader.readCardEx(0, 1);
                        }
                        catch (IDCardReaderException e)
                        {
                            writeLogToFile("读卡失败，错误信息：" + e.getMessage());
                        }
                        if (retType == 1 || retType == 2 || retType == 3)
                        {
                            final long nTickUsed = (System.currentTimeMillis()-nTickstart);
                            final int final_retType = retType;
                            writeLogToFile("读卡成功：" + (++mReadCount) + "次" + "，耗时：" + nTickUsed + "毫秒");
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    if (final_retType == 1)
                                    {
                                        final IDCardInfo idCardInfo = idCardReader.getLastIDCardInfo();
                                        //姓名adb
                                        String strName = idCardInfo.getName();
                                        //民族
                                        String strNation = idCardInfo.getNation();
                                        //出生日期
                                        String strBorn = idCardInfo.getBirth();
                                        //住址
                                        String strAddr = idCardInfo.getAddress();
                                        //身份证号
                                        String strID = idCardInfo.getId();
                                        //有效期限
                                        String strEffext = idCardInfo.getValidityTime();
                                        //签发机关
                                        String strIssueAt = idCardInfo.getDepart();
                                        String validityTime = idCardInfo.getValidityTime();
                                        String startTime = validityTime.substring(0, 10);
                                        String endTime = validityTime.substring(validityTime.length()-10, validityTime.length());
                                        textView.setText("姓名:" + strName + "\n" +
                                                "性别:" + idCardInfo.getSex() + "\n" +
                                                "民族:" + strNation + "\n" +
                                                "出生:" + strBorn + "\n" +
                                                "地址:" + strAddr + "\n" +
                                                "身份证号:" + strID + "\n" +
                                                "签发机关:" + strIssueAt + "\n" +
                                                "期限起始:" + startTime + "\n" +
                                                "期限失效:" + endTime + "\n"
                                        );

                                        if (idCardInfo.getPhotolength() > 0) {
                                            byte[] buf = new byte[WLTService.imgLength];
                                            if (1 == WLTService.wlt2Bmp(idCardInfo.getPhoto(), buf)) {
                                                imageView.setImageBitmap(IDPhotoHelper.Bgr2Bitmap(buf));
                                                if (mId != null && strID != null && !mId.equalsIgnoreCase(strID)) {
                                                    playMusic();
                                                    mId = strID;
                                                }
                                                bStoped = true;
                                                OnBnStop(view);
                                            }
                                        }

//                                        int length = idCardInfo.getFplength();
//                                        Log.i("finger", "finger length:" + length);

//                                        if (length > 0) {
//                                            byte[] fpdata = idCardInfo.getFpdata();
//                                            if(!Arrays.equals(fpdata, fingerFeature)) {
//                                                System.arraycopy(fpdata,0, fingerFeature,0, length);
//                                                readIdFeatureSuccess = true;
//                                                verifyHandler.sendEmptyMessage(0);
//                                            }
//                                        } else {
//                                            readIdFeatureSuccess = false;
//                                        }
                                    }
                                    else if (final_retType == 2)
                                    {
                                        final IDPRPCardInfo idprpCardInfo = idCardReader.getLastPRPIDCardInfo();
                                        //中文名
                                        String strCnName = idprpCardInfo.getCnName();
                                        //英文名
                                        String strEnName = idprpCardInfo.getEnName();
                                        //国家/国家地区代码
                                        String strCountry = idprpCardInfo.getCountry() + "/" + idprpCardInfo.getCountryCode();//国家/国家地区代码
                                        //出生日期
                                        String strBorn = idprpCardInfo.getBirth();
                                        //身份证号
                                        String strID = idprpCardInfo.getId();
                                        //有效期限
                                        String strEffext = idprpCardInfo.getValidityTime();
                                        //签发机关
                                        String strIssueAt = "公安部";
                                        textView.setText("读取次数："  + mReadCount + ",耗时："+  nTickUsed +  "毫秒, 卡类型：外国人永居证,中文名：" + strCnName + ",英文名：" +
                                                strEnName + "，国家：" + strCountry + ",证件号：" + strID);
                                        if (idprpCardInfo.getPhotolength() > 0) {
                                            byte[] buf = new byte[WLTService.imgLength];
                                            if (1 == WLTService.wlt2Bmp(idprpCardInfo.getPhoto(), buf)) {
                                                imageView.setImageBitmap(IDPhotoHelper.Bgr2Bitmap(buf));
                                            }
                                        }
                                    }
                                    else
                                    {
                                        final IDCardInfo idCardInfo = idCardReader.getLastIDCardInfo();
                                        //姓名
                                        String strName = idCardInfo.getName();
                                        //民族,港澳台不支持该项
                                        String strNation = "";
                                        //出生日期
                                        String strBorn = idCardInfo.getBirth();
                                        //住址
                                        String strAddr = idCardInfo.getAddress();
                                        //身份证号
                                        String strID = idCardInfo.getId();
                                        //有效期限
                                        String strEffext = idCardInfo.getValidityTime();
                                        //签发机关
                                        String strIssueAt = idCardInfo.getDepart();
                                        //通行证号
                                        String strPassNum = idCardInfo.getPassNum();
                                        //签证次数
                                        int visaTimes = idCardInfo.getVisaTimes();
//                                        textView.setText("读取次数："  + mReadCount + ",耗时："+  nTickUsed +  "毫秒, 卡类型：港澳台居住证,姓名：" + strName +
//                                                "，住址：" + strAddr + ",身份证号：" + strID + "，通行证号码：" + strPassNum +
//                                                ",签证次数：" + visaTimes);

                                        String validityTime = idCardInfo.getValidityTime();
                                        String startTime = validityTime.substring(0, 10);
                                        String endTime = validityTime.substring(validityTime.length()-10, validityTime.length());

                                        textView.setText("姓名:" + strName + "\n" +
                                                "性别:" + idCardInfo.getSex() + "\n" +
                                                "民族:" + strNation + "\n" +
                                                "出生:" + strBorn + "\n" +
                                                "地址:" + strAddr + "\n" +
                                                "身份证号:" + strID + "\n" +
                                                "签发机关:" + strIssueAt + "\n" +
                                                "期限起始:" + startTime + "\n" +
                                                "期限失效:" + endTime + "\n"
                                        );

                                        if (idCardInfo.getPhotolength() > 0) {
                                            byte[] buf = new byte[WLTService.imgLength];
                                            if (1 == WLTService.wlt2Bmp(idCardInfo.getPhoto(), buf)) {
                                                imageView.setImageBitmap(IDPhotoHelper.Bgr2Bitmap(buf));
                                            }
                                        }
                                    }
                                }
                            });
                        } else {
                            fingerFeature = new byte[1024];
                            mId = "";
                        }
                    }
                    countdownLatch.countDown();
                    isRun = false;
                }
            }).start();
        }catch (IDCardReaderException e)
        {
            writeLogToFile("连接设备失败");
            textView.setText("连接失败");
            textView.setText("开始读卡失败，错误码：" + e.getErrorCode() + "\n错误信息：" + e.getMessage() + "\n内部代码=" + e.getInternalErrorCode());
        }
    }


    public void OnBnBegin(View view) {
        try {
            if (bopen)
            {
                textView.setText("设备已连接");
                return;
            }
            idSerialName = idEditText.getText().toString();
            if (null == idSerialName || idSerialName.isEmpty())
            {
                textView.setText("请输入串口名");
                return;
            }
            startIDCardReader();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            idCardReader.open(0);
            bStoped = false;
            mReadCount = 0;
            writeLogToFile("连接设备成功");
            textView.setText("连接成功");
            bopen = true;
            countdownLatch = new CountDownLatch(1);
            new Thread(new Runnable() {
                public void run() {
                    while (!bStoped) {
                        boolean ret = false;
                        final long nTickstart = System.currentTimeMillis();
                        try {
                            idCardReader.findCard(0);
                            idCardReader.selectCard(0);
                        }catch (IDCardReaderException e)
                        {
                            //LogHelper.e("errcode:" + e.getErrorCode() + ",internalerrorcode:" + e.getInternalErrorCode());
                            //continue;
                        }
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        int retType = 0;
                        try {
                            retType = idCardReader.readCardEx(0, 1);
                        }
                        catch (IDCardReaderException e)
                        {
                            writeLogToFile("读卡失败，错误信息：" + e.getMessage());
                        }
                        if (retType == 1 || retType == 2 || retType == 3)
                        {
                            final long nTickUsed = (System.currentTimeMillis()-nTickstart);
                            final int final_retType = retType;
                            writeLogToFile("读卡成功：" + (++mReadCount) + "次" + "，耗时：" + nTickUsed + "毫秒");
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    if (final_retType == 1)
                                    {
                                        final IDCardInfo idCardInfo = idCardReader.getLastIDCardInfo();
                                        //姓名
                                        String strName = idCardInfo.getName();
                                        //民族
                                        String strNation = idCardInfo.getNation();
                                        //出生日期
                                        String strBorn = idCardInfo.getBirth();
                                        //住址
                                        String strAddr = idCardInfo.getAddress();
                                        //身份证号
                                        String strID = idCardInfo.getId();
                                        //有效期限
                                        String strEffext = idCardInfo.getValidityTime();
                                        //签发机关
                                        String strIssueAt = idCardInfo.getDepart();
                                        String validityTime = idCardInfo.getValidityTime();
                                        String startTime = validityTime.substring(0, 10);
                                        String endTime = validityTime.substring(validityTime.length()-10, validityTime.length());
                                        textView.setText("姓名:" + strName + "\n" +
                                                "性别:" + idCardInfo.getSex() + "\n" +
                                                "民族:" + strNation + "\n" +
                                                "出生:" + strBorn + "\n" +
                                                "地址:" + strAddr + "\n" +
                                                "身份证号:" + strID + "\n" +
                                                "签发机关:" + strIssueAt + "\n" +
                                                "期限起始:" + startTime + "\n" +
                                                "期限失效:" + endTime + "\n"
                                        );


//                                        textView.setText("读取次数："  + mReadCount + ",耗时："+  nTickUsed +  "毫秒, 卡类型：居民身份证,姓名：" + strName +
//                                                "，民族：" + strNation + "，住址：" + strAddr + ",身份证号：" + strID);
                                        if (idCardInfo.getPhotolength() > 0) {
                                            byte[] buf = new byte[WLTService.imgLength];
                                            if (1 == WLTService.wlt2Bmp(idCardInfo.getPhoto(), buf)) {
                                                imageView.setImageBitmap(IDPhotoHelper.Bgr2Bitmap(buf));
                                                if (mId != null && strID != null && !mId.equalsIgnoreCase(strID)) {
                                                    playMusic();
                                                    mId = strID;
                                                }
                                            }
                                        }

                                        int length = idCardInfo.getFplength();
                                        Log.i("finger", "finger length:" + length);

                                        if (length > 0) {
                                            byte[] fpdata = idCardInfo.getFpdata();
                                            if(!Arrays.equals(fpdata, fingerFeature)) {
                                                System.arraycopy(fpdata,0, fingerFeature,0, length);
                                                readIdFeatureSuccess = true;
                                                verifyHandler.sendEmptyMessage(0);
                                            }
                                        } else {
                                            readIdFeatureSuccess = false;
                                        }
                                    }
                                    else if (final_retType == 2)
                                    {
                                        final IDPRPCardInfo idprpCardInfo = idCardReader.getLastPRPIDCardInfo();
                                        //中文名
                                        String strCnName = idprpCardInfo.getCnName();
                                        //英文名
                                        String strEnName = idprpCardInfo.getEnName();
                                        //国家/国家地区代码
                                        String strCountry = idprpCardInfo.getCountry() + "/" + idprpCardInfo.getCountryCode();//国家/国家地区代码
                                        //出生日期
                                        String strBorn = idprpCardInfo.getBirth();
                                        //身份证号
                                        String strID = idprpCardInfo.getId();
                                        //有效期限
                                        String strEffext = idprpCardInfo.getValidityTime();
                                        //签发机关
                                        String strIssueAt = "公安部";
                                        textView.setText("读取次数："  + mReadCount + ",耗时："+  nTickUsed +  "毫秒, 卡类型：外国人永居证,中文名：" + strCnName + ",英文名：" +
                                                strEnName + "，国家：" + strCountry + ",证件号：" + strID);
                                        if (idprpCardInfo.getPhotolength() > 0) {
                                            byte[] buf = new byte[WLTService.imgLength];
                                            if (1 == WLTService.wlt2Bmp(idprpCardInfo.getPhoto(), buf)) {
                                                imageView.setImageBitmap(IDPhotoHelper.Bgr2Bitmap(buf));
                                            }
                                        }
                                    }
                                    else
                                    {
                                        final IDCardInfo idCardInfo = idCardReader.getLastIDCardInfo();
                                        //姓名
                                        String strName = idCardInfo.getName();
                                        //民族,港澳台不支持该项
                                        String strNation = "";
                                        //出生日期
                                        String strBorn = idCardInfo.getBirth();
                                        //住址
                                        String strAddr = idCardInfo.getAddress();
                                        //身份证号
                                        String strID = idCardInfo.getId();
                                        //有效期限
                                        String strEffext = idCardInfo.getValidityTime();
                                        //签发机关
                                        String strIssueAt = idCardInfo.getDepart();
                                        //通行证号
                                        String strPassNum = idCardInfo.getPassNum();
                                        //签证次数
                                        int visaTimes = idCardInfo.getVisaTimes();
                                        textView.setText("读取次数："  + mReadCount + ",耗时："+  nTickUsed +  "毫秒, 卡类型：港澳台居住证,姓名：" + strName +
                                                "，住址：" + strAddr + ",身份证号：" + strID + "，通行证号码：" + strPassNum +
                                                ",签证次数：" + visaTimes);
                                        if (idCardInfo.getPhotolength() > 0) {
                                            byte[] buf = new byte[WLTService.imgLength];
                                            if (1 == WLTService.wlt2Bmp(idCardInfo.getPhoto(), buf)) {
                                                imageView.setImageBitmap(IDPhotoHelper.Bgr2Bitmap(buf));
                                            }
                                        }
                                    }
                                }
                            });
                        } else {
                            mId = "";
                            fingerFeature = new byte[1024];
                        }
                    }
                    countdownLatch.countDown();
                }
            }).start();
        }catch (IDCardReaderException e)
        {
            writeLogToFile("连接设备失败");
            textView.setText("连接失败");
            textView.setText("开始读卡失败，错误码：" + e.getErrorCode() + "\n错误信息：" + e.getMessage() + "\n内部代码=" + e.getInternalErrorCode());
        }
    }


    public void OnBnStop(View view)
    {
        mId = "";
        fingerFeature = new byte[1024];
        if (!bopen)
        {
            return;
        }
        bStoped = true;
        mReadCount = 0;
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
        textView.setText("设备断开连接");
        bopen = false;
    }


    public void OnBnVerify(View view){
        readFingerSuccess = false;
        //readIdFeatureSuccess = false;
        btn_captureimage.performClick();
        //OnVerify();
    }

    public void OnVerify()
    {
        if (fingerHelper == null) return;
        byte[] feature = new byte[512];
        int ret = fingerHelper.download_image(fingerImage, feature);
        if (ret != 1) {
            Log.e(TAG, "download_image error.");
            Toast.makeText(getContext(), "download_image error.",Toast.LENGTH_LONG).show();
            return;
        } else {
            Log.i(TAG, "download_image success.");
        }

        ret = fingerHelper.download_feature(fingerFeature, 1024);
        if (ret != 1) {
            Log.e(TAG, "download_feature1 error.");
            Toast.makeText(getContext(), "download_feature error.",Toast.LENGTH_LONG).show();
            return;
        }
        Log.i(TAG, "download_feature success.");
        float[] score = new float[1];
        ret = fingerHelper.Compare_feature(score);
        if (ret != 1) {
            Log.e(TAG, "Compare_feature error,ret:" + ret);
            Toast.makeText(getContext(), "Compare_feature error.",Toast.LENGTH_LONG).show();
        }
        Log.i(TAG, "Compare_feature success.");
        Log.i(TAG, "比对结果1：" + score[0]);

        if (score[0] > 0.79) {
            ((TextView)findViewById(R.id.verifyResult)).setText("比对通过,分数：" + score[0]);
        } else {
            ((TextView)findViewById(R.id.verifyResult)).setText("比对失败,分数：" + score[0]);
        }
    }


    public Handler verifyHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            Log.i(TAG, "verifyHandler recv msg, readFingerSuccess:" + readFingerSuccess + ",readIdFeatureSuccess:" +readIdFeatureSuccess);
            if (readFingerSuccess && readIdFeatureSuccess) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        OnVerify();
                    }
                });
            }
        }
    };

}
