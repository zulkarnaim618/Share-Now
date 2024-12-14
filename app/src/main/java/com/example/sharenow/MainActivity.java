package com.example.sharenow;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.view.ViewCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.DhcpInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

public class MainActivity extends AppCompatActivity {

    WifiManager wifiManager;
    WifiManager.LocalOnlyHotspotReservation hotspotReservation;
    boolean hotspot;
    QRGEncoder qrgEncoder;
    String absolutePath = "/storage/emulated/0";
    TextView pathText;
    String goBack = "/storage/emulated/0";
    int height;
    int width;
    int selected = 0;
    int size = 1024*1024;
    HashMap<LinearLayout,Boolean> selectedList = new HashMap<>();
    HashMap<String,ProgressBar> progressBarHashMap = new HashMap<>();
    List<String> selectedPath = new ArrayList<>();
    List<String> list = new ArrayList<>();
    NetworkUtil networkUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        height = displayMetrics.heightPixels;
        width = displayMetrics.widthPixels;
        wifiManager = (WifiManager)
                this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }
    public void onSend(View v) {
        setContentView(R.layout.filemanager);
        pathText = findViewById(R.id.path);
        selectedPath = new ArrayList<>();
        selectedList = new HashMap<>();
        selected = 0;
        loadPath(absolutePath);
    }
    public void onStartSending(View v) {
        setContentView(R.layout.sending);
        final DhcpInfo dhcp = wifiManager.getDhcpInfo();
        final String address = Formatter.formatIpAddress(dhcp.gateway);
        Toast.makeText(getApplicationContext(),address,Toast.LENGTH_SHORT).show();
        startClient(selectedPath,address);
    }
    public void onReceive(View v) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            //ActivityCompat.requestPermissions(this,new String[] { Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            try {
                System.out.println(Build.VERSION.SDK_INT);
                wifiManager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {
                    @Override
                    public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                        super.onStarted(reservation);
                        hotspot = true;
                        hotspotReservation = reservation;
                        String key = hotspotReservation.getWifiConfiguration().preSharedKey;
                        String ussid = hotspotReservation.getWifiConfiguration().SSID;
                        System.out.println("KEY: " + key);
                        System.out.println("USSID: " + ussid);
                        System.out.println("STARTED THE HOTSPOT");
                        setContentView(R.layout.receiving);
                        printHotspotInfo();
                        startServer();
                    }

                    @Override
                    public void onStopped() {
                        super.onStopped();
                    }

                    @Override
                    public void onFailed(int reason) {
                        super.onFailed(reason);
                    }
                }, new Handler());
            }
            catch (Exception e) {
                System.out.println(e);
                setContentView(R.layout.receiving);
                startServer();
            }
        }
        else {
            System.out.println("not");
            setContentView(R.layout.receiving);
            startServer();
        }
    }

    public void printHotspotInfo() {
        ConstraintLayout constraintLayout = findViewById(R.id.hotspot);
        TextView tv = new TextView(this,null,0,R.style.nameText);
        String text = "";
        String ssid = "";
        String preSharedKey = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            preSharedKey = hotspotReservation.getWifiConfiguration().preSharedKey;
            ssid = hotspotReservation.getWifiConfiguration().SSID;
        }
        text = "Key : " + preSharedKey+ "\nSSID : " + ssid;
        tv.setText(text);
        tv.setId(ViewCompat.generateViewId());
        tv.setLayoutParams(new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200));
        tv.setGravity(Gravity.CENTER);
        constraintLayout.addView(tv);
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);
        constraintSet.connect(tv.getId(), ConstraintSet.LEFT, R.id.hotspot, ConstraintSet.LEFT, 0);
        constraintSet.connect(tv.getId(), ConstraintSet.TOP, R.id.hotspot, ConstraintSet.TOP, 0);
        constraintSet.applyTo(constraintLayout);
        String inputValue = "WIFI:S:" + ssid + ";T:WPA;P:" + preSharedKey + ";;";
        ImageView qrImage = new ImageView(this);
        qrImage.setId(ViewCompat.generateViewId());
        qrImage.setLayoutParams(new ConstraintLayout.LayoutParams(600, 600));
        constraintLayout.addView(qrImage);
        ConstraintSet constraintSet1 = new ConstraintSet();
        constraintSet1.clone(constraintLayout);
        constraintSet1.connect(qrImage.getId(), ConstraintSet.LEFT, R.id.hotspot, ConstraintSet.LEFT, width/6);
        constraintSet1.connect(qrImage.getId(), ConstraintSet.TOP, R.id.hotspot, ConstraintSet.TOP, 300);
        constraintSet1.applyTo(constraintLayout);
        int smallerDimension = width*2/3;
        qrgEncoder = new QRGEncoder(
                inputValue, null,
                QRGContents.Type.TEXT,
                smallerDimension);
        try {
            Bitmap bitmap = qrgEncoder.encodeAsBitmap();
            qrImage.setImageBitmap(bitmap);
        } catch (Exception e) {

        }

    }
    public void stopReceiving(View v) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (hotspot) {
                hotspotReservation.close();
                hotspot = false;
            }
        }
        setContentView(R.layout.activity_main);
        try {
            networkUtil.closeConnection();
        }
        catch (Exception e) {

        }
    }
    public void stopSending(View v) {
        setContentView(R.layout.activity_main);
    }
    public void stop(View v) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (hotspot) {
                hotspotReservation.close();
                hotspot = false;
            }
        }
        setContentView(R.layout.activity_main);
        try {
            networkUtil.closeConnection();
        }
        catch (Exception e) {

        }
    }
    public void loadPath(String path) {
        pathText.setText(path);
        File file = new File(path);
        goBack = file.getParent();
        Button button = findViewById(R.id.back);
        if (goBack.equals("/storage/emulated")) {
            button.setVisibility(View.INVISIBLE);
        } else button.setVisibility(View.VISIBLE);
        String[] list1 = file.list();
        if (list1 == null) list1 = new String[0];
        Arrays.sort(list1);
        String[] list = new String[list1.length];
        int j = 0;
        for (int i = 0; i < list1.length; i++) {
            if (new File(path + "/" + list1[i]).isDirectory()) {
                list[j] = list1[i];
                j++;
            }
        }
        for (int i = 0; i < list1.length; i++) {
            if (!new File(path + "/" + list1[i]).isDirectory()) {
                list[j] = list1[i];
                j++;
            }
        }
        LinearLayout content = findViewById(R.id.content);
        content.removeAllViews();
        for (int i = 0; i < list.length; i++) {
            File child = new File(path + "/" + list[i]);
            LinearLayout linearLayout = makeLayout(child);
            linearLayout.setBackgroundColor(Color.parseColor("#00000000"));
            content.addView(linearLayout);
            int finalI = i;
            linearLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (child.isDirectory() && selected == 0) loadPath(path + "/" + list[finalI]);
                    else {
                        if (selectedList.get(linearLayout) == null || !selectedList.get(linearLayout)) {
                            selected++;
                            selectedPath.add(path + "/" + list[finalI]);
                            selectedList.put(linearLayout, true);
                            linearLayout.setBackgroundColor(Color.parseColor("red"));
                        } else {
                            selected--;
                            selectedPath.remove(path + "/" + list[finalI]);
                            selectedList.put(linearLayout, false);
                            linearLayout.setBackgroundColor(Color.parseColor("#00000000"));
                        }
                    }
                    System.out.println(selected);
                    for (int j = 0; j < selectedPath.size(); j++)
                        System.out.println(selectedPath.get(j));
                }
            });
            linearLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (selectedList.get(linearLayout) == null || !selectedList.get(linearLayout)) {
                        selected++;
                        selectedPath.add(path + "/" + list[finalI]);
                        selectedList.put(linearLayout, true);
                        linearLayout.setBackgroundColor(Color.parseColor("red"));
                    } else {
                        linearLayout.setBackgroundColor(Color.parseColor("#00000000"));
                        selected--;
                        selectedPath.remove(path + "/" + list[finalI]);
                        selectedList.put(linearLayout, false);
                    }
                    System.out.println(selected);
                    for (int j = 0; j < selectedPath.size(); j++)
                        System.out.println(selectedPath.get(j));
                    return true;
                }
            });
        }
    }
    public void goBack(View v) {
        System.out.println(goBack);
        loadPath(goBack);
    }
    public LinearLayout makeLayout(File file) {
        //ConstraintLayout constraintLayout = new ConstraintLayout(this);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,170));
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        ConstraintLayout iconConst = new ConstraintLayout(this);
        iconConst.setLayoutParams(new LinearLayout.LayoutParams(width/10, ViewGroup.LayoutParams.MATCH_PARENT));
        TextView icon = new TextView(this);
        icon.setLayoutParams(new LinearLayout.LayoutParams(80,80));
        icon.setBackgroundColor(Color.parseColor("#ffff0000"));
        icon.setVisibility(View.INVISIBLE);
        iconConst.addView(icon);
        linearLayout.addView(iconConst);
        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setLayoutParams(new LinearLayout.LayoutParams((width*9/10)-50, ViewGroup.LayoutParams.MATCH_PARENT));
        TextView name = new TextView(this,null,0,R.style.nameText);
        name.setText(file.getName());
        name.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 110));
        name.setBackgroundColor(Color.parseColor("#00000000"));
        name.setGravity(Gravity.CENTER_VERTICAL);
        details.addView(name);
        LinearLayout others = new LinearLayout(this);
        others.setOrientation(LinearLayout.HORIZONTAL);
        others.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 60));
        TextView date = new TextView(this,null,0,R.style.detailColor);
        String a = new Date(file.lastModified()).toString();
        char[] b = new char[15];
        a.getChars(0,10,b,0);
        a.getChars(29,34,b,10);
        a = new String(b);
        date.setText(a);
        date.setLayoutParams(new LinearLayout.LayoutParams(width*3/5, ViewGroup.LayoutParams.MATCH_PARENT));
        date.setBackgroundColor(Color.parseColor("#00000000"));
        others.addView(date);
        TextView items = new TextView(this,null,0,R.style.detailColor);
        int itemNumber = (file.list() == null ? 0 : file.list().length);
        items.setText(itemNumber + " items");
        if (!file.isDirectory()) items.setVisibility(View.INVISIBLE);
        items.setLayoutParams(new LinearLayout.LayoutParams(width*1/5, ViewGroup.LayoutParams.MATCH_PARENT));
        items.setBackgroundColor(Color.parseColor("#00000000"));
        others.addView(items);
        details.addView(others);
        linearLayout.addView(details);
        return linearLayout;
    }
    public void fill(List<String> files) {
        progressBarHashMap = new HashMap<>();
        LinearLayout transferList = findViewById(R.id.transferList);
        for (int i=0;i<files.size();i++) {
            LinearLayout linearLayout = new LinearLayout(this);
            linearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,150));
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout horizontal = new LinearLayout(this);
            horizontal.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            horizontal.setOrientation(LinearLayout.HORIZONTAL);
            TextView padding = new TextView(this);
            padding.setLayoutParams(new LinearLayout.LayoutParams(100, ViewGroup.LayoutParams.MATCH_PARENT));
            padding.setBackgroundColor(Color.parseColor("#00000000"));
            horizontal.addView(padding);
            LinearLayout details = new LinearLayout(this);
            details.setLayoutParams(new LinearLayout.LayoutParams(width-200, ViewGroup.LayoutParams.MATCH_PARENT));
            details.setOrientation(LinearLayout.VERTICAL);
            TextView name = new TextView(this);
            name.setText(files.get(i));
            name.setGravity(Gravity.CENTER_VERTICAL);
            name.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100));
            name.setBackgroundColor(Color.parseColor("#00000000"));
            details.addView(name);
            ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            progressBar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,50));
            progressBar.setProgress(0);
            details.addView(progressBar);
            horizontal.addView(details);
            linearLayout.addView(horizontal);
            progressBarHashMap.put(files.get(i),progressBar);
            transferList.addView(linearLayout);
        }

    }
    public void startServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String dPath = "/storage/emulated/0/Share Now/";
                    ServerSocket serverSocket = new ServerSocket(33335);
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("client accepted");
                    NetworkUtil networkUtil = new NetworkUtil(clientSocket);
                    List<String> files = (List<String>) networkUtil.read();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setContentView(R.layout.sending);
                            fill(files);
                        }
                    });
                    OutputStream outputStream = null;
                    File file;
                    byte[] bytes;
                    while (true) {
                        Data data = (Data) networkUtil.read();
                        if (data.getBytes().length == 0) break;
                        if (outputStream == null) {
                            file = new File(dPath + data.getPath());
                            file.getParentFile().mkdirs();
                            file.createNewFile();
                            outputStream = new FileOutputStream(dPath + data.getPath());
                        }
                        outputStream.write(data.getBytes());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBarHashMap.get(data.getPath()).setProgress((int) ((1.0-data.getRatio())*100));
                            }
                        });
                        if (data.isEndPart()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(),"Received : " + data.getPath(),Toast.LENGTH_SHORT).show();
                                }
                            });
                            outputStream.flush();
                            outputStream = null;
                        }
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),"Received",Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                catch (Exception e) {

                }
            }
        }).start();
    }
    public void startClient(List<String> files, String serverAddress) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HashMap<String, String> relativepaths = new HashMap<>();
                    getAllFiles(files, relativepaths);
                    list = new ArrayList<>();
                    for (int i =0;i< files.size();i++) list.add(relativepaths.get(files.get(i)));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            fill(list);
                        }
                    });
                    networkUtil = new NetworkUtil(serverAddress,33335);
                    networkUtil.write(list);
                    for (int i = 0; i < files.size(); i++) {
                        sendFile(files.get(i), relativepaths.get(files.get(i)));
                        //System.out.println(files.get(i)+ " " + relativepaths.get(files.get(i)));
                    }
                    networkUtil.write(new Data());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),"Sent",Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                catch(Exception e){

                    }
                }

        }).start();
    }
        public void getAllFiles(List<String> list, HashMap<String,String> relativePaths) {
            for (int i=0;i<list.size();i++) {
                File file = new File(list.get(i));
                if (file.isDirectory()) {
                    String rPath = relativePaths.get(list.get(i));
                    if (rPath == null) {
                        String[] paths = list.get(i).split("/");
                        rPath = paths[paths.length-1];
                    }
                    else {
                        String[] paths = list.get(i).split("/");
                        rPath += "/" + paths[paths.length-1];
                    }
                    String[] subDirectories = file.list();
                    for (int j=0;j<subDirectories.length;j++) {
                        list.add(list.get(i) + "/" + subDirectories[j]);
                        relativePaths.put(list.get(i) + "/" + subDirectories[j],rPath);
                    }
                    relativePaths.remove(list.get(i));
                    list.remove(i);
                    i--;
                }
                else {
                    String rPath = relativePaths.get(list.get(i));
                    if (rPath == null) {
                        String[] paths = list.get(i).split("/");
                        relativePaths.put(list.get(i), paths[paths.length-1]);
                    }
                    else {
                        String[] paths = list.get(i).split("/");
                        relativePaths.put(list.get(i), rPath + "/" + paths[paths.length-1]);
                    }
                }
            }
        }
        public void sendFile(String path,String rPath) throws Exception {
            //System.out.println(path);
            File file = new File(path);
            if (!file.isDirectory()) {
                //System.out.println("Not directory");
                InputStream inputStream = new FileInputStream(path);
                double totalSize = file.length();
                long remainingSize = file.length();
                byte[] bytes = new byte[size];
                Data data = new Data();
                //System.out.println("Total size " + file.length());
                while (true) {
                    if (remainingSize <= size) {
                        //System.out.println("Last "+ remainingSize);
                        bytes = new byte[(int)remainingSize];
                        inputStream.read(bytes);
                        remainingSize = 0;
                        data = new Data(rPath,bytes,(remainingSize/totalSize),true);
                        networkUtil.write(data);
                        double finalRemainingSize1 = remainingSize;
                        //System.out.println("Remaining size: " + finalRemainingSize1);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBarHashMap.get(rPath).setProgress((int) ((1.0-(finalRemainingSize1 /totalSize))*100));
                                //Toast.makeText(getApplicationContext(),"" + (int) ((1.0-(finalRemainingSize1 /totalSize))*100),Toast.LENGTH_SHORT).show();
                                Toast.makeText(getApplicationContext(),"Sent : " + path,Toast.LENGTH_SHORT).show();
                            }
                        });
                        break;
                    }
                    //System.out.println("Remaining "+ remainingSize);
                    inputStream.read(bytes);
                    remainingSize -= size;
                    data = new Data(rPath,bytes,(remainingSize/totalSize),false);
                    networkUtil.write(data);

                    double finalRemainingSize = remainingSize;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Toast.makeText(getApplicationContext(),"" + (int) ((1.0-(finalRemainingSize /totalSize))*100),Toast.LENGTH_SHORT).show();
                            progressBarHashMap.get(rPath).setProgress((int) ((1.0-(finalRemainingSize /totalSize))*100));
                        }
                    });
                }
                inputStream.close();
            }
        }
}