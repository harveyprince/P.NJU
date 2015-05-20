package com.prince.harvey.harveyapp;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity {
    String loginURL = "http://p.nju.edu.cn/portal/portal_io.do";
    String onlineURL = "http://p.nju.edu.cn/proxy/online.php";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button loginButton = (Button)findViewById(R.id.button);
        loginButton.setOnClickListener(loginlistener);

        Button logoutButton = (Button)findViewById(R.id.logout);
        logoutButton.setOnClickListener(logoutlistener);

    }

    @Override
    protected void onStart(){
        super.onStart();
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        SQLiteDatabase db = openOrCreateDatabase("user.db", Context.MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS `harveyaccount`(idname varchar(20),password varchar(20))");
        String[] cols = {"idname","password"};
        Cursor c = db.query("harveyaccount",cols,null,null,null,null,null);
        String idname = null;
        String password = null;
        while(c.moveToNext()){
            idname = c.getString(c.getColumnIndex("idname"));
            password = c.getString(c.getColumnIndex("password"));
        }
        if(idname!=null&&password!=null){
            EditText idtext = (EditText)findViewById(R.id.editText);
            EditText passtext = (EditText)findViewById(R.id.editText2);
            idtext.setText(idname);
            passtext.setText(password);
            CheckBox checkBox = (CheckBox)findViewById(R.id.remember);
            checkBox.setChecked(true);
        }
        db.close();
        if (mWifi.isConnected()) {
            new Thread(loginCheckNetworkTask).start();
        }else{
            Toast toast=Toast.makeText(getApplicationContext(), "你不在wifi环境下", Toast.LENGTH_SHORT);
            toast.show();

        }

    }

    Handler expHandle = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            Toast toast=Toast.makeText(getApplicationContext(), msg.getData().getString("error"), Toast.LENGTH_SHORT);
            toast.show();
        }
    };

    Handler loginHandle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bun = msg.getData();
            TextView useridview = (TextView)findViewById(R.id.usernumber);
            useridview.setText(bun.getString("userid"));
            TextView nameview = (TextView)findViewById(R.id.username);
            nameview.setText(bun.getString("name"));
            TextView paymountview = (TextView)findViewById(R.id.paymount);
            paymountview.setText(bun.getString("payamount"));
            TextView areaview = (TextView)findViewById(R.id.area);
            areaview.setText(bun.getString("area"));
            animateHide();
        }
    };

    Handler logoutHandle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            animateBack();
        }
    };

    public Button.OnClickListener loginlistener = new Button.OnClickListener(){

        @Override
        public void onClick(View v) {
            ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (mWifi.isConnected()) {
                CheckBox checkBox = (CheckBox) findViewById(R.id.remember);
                if (checkBox.isChecked()) {
                    SQLiteDatabase db = openOrCreateDatabase("user.db", Context.MODE_PRIVATE, null);
                    db.execSQL("CREATE TABLE IF NOT EXISTS `harveyaccount`(idname varchar(20),password varchar(20))");
                    String[] cols = {"idname", "password"};
                    EditText idtext = (EditText) findViewById(R.id.editText);
                    EditText passtext = (EditText) findViewById(R.id.editText2);
                    String idname = idtext.getText().toString();
                    String password = passtext.getText().toString();
                    if (idname != null && password != null) {
                        db.execSQL("INSERT INTO harveyaccount VALUES (?, ?)", new Object[]{idname, password});
                    } else {
                        Toast toast = Toast.makeText(getApplicationContext(), "不得为空", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                    db.close();
                } else {
                    SQLiteDatabase db = openOrCreateDatabase("user.db", Context.MODE_PRIVATE, null);
                    db.execSQL("DROP TABLE `harveyaccount`");
                    db.close();
                }
                new Thread(loginNetworkTask).start();
            }else{
                Toast toast = Toast.makeText(getApplicationContext(), "您不在wifi状态下", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    };

    public Button.OnClickListener logoutlistener = new Button.OnClickListener(){

        @Override
        public void onClick(View v) {
            ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (mWifi.isConnected()) {
                new Thread(logoutNetworkTask).start();
            }else{
                Toast toast = Toast.makeText(getApplicationContext(), "您不在wifi状态下", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    };

    Runnable loginCheckNetworkTask = new Runnable() {
        @Override
        public void run() {
            NameValuePair pair1 = new BasicNameValuePair("action", "info");

            List<NameValuePair> pairList = new ArrayList<NameValuePair>();
            pairList.add(pair1);

            try {
                HttpEntity requestHttpEntity = new UrlEncodedFormEntity(
                        pairList);
                // URL使用基本URL即可，其中不需要加参数
                HttpPost httpPost = new HttpPost(onlineURL);
                // 将请求体内容加入请求中
                httpPost.setEntity(requestHttpEntity);
                // 需要客户端对象来发送请求
                HttpClient httpClient = new DefaultHttpClient();
                // 发送请求
                HttpResponse response = httpClient.execute(httpPost);
                HttpEntity entity = response.getEntity();
                if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                    String res = EntityUtils.toString(entity);
                    JSONObject resjson = new JSONObject(res);
                    int reply_code = resjson.getInt("reply_code");
                    if(reply_code==301){
                        JSONObject userjson = new JSONObject(resjson.getString("userinfo"));
                        String userid = userjson.getString("username");
                        String name = userjson.getString("fullname");
                        double payamount = userjson.getDouble("payamount");
                        String area = userjson.getString("area_name");
                        Bundle bun = new Bundle();
                        bun.putString("userid",userid);
                        bun.putString("name",name);
                        bun.putString("payamount",payamount+"");
                        bun.putString("area",area);
                        Message msg = new Message();
                        msg.setData(bun);
                        loginHandle.sendMessage(msg);
                    }else if(reply_code==302){
                        //
                    }else{
                        Bundle bun = new Bundle();
                        bun.putString("error","状态检测错误："+reply_code);
                        Message msg = new Message();
                        msg.setData(bun);
                        expHandle.sendMessage(msg);
                    }
                }

            } catch (Exception e) {
                Log.i(e.getMessage(),"Post request");
                Bundle bun = new Bundle();
                bun.putString("error","发生异常");
                Message msg = new Message();
                msg.setData(bun);
                expHandle.sendMessage(msg);
            }
        }
    };

    Runnable logoutNetworkTask = new Runnable() {
        @Override
        public void run() {
            NameValuePair pair1 = new BasicNameValuePair("action", "logout");

            List<NameValuePair> pairList = new ArrayList<NameValuePair>();
            pairList.add(pair1);
            try {
                HttpEntity requestHttpEntity = new UrlEncodedFormEntity(
                        pairList);
                // URL使用基本URL即可，其中不需要加参数
                HttpPost httpPost = new HttpPost(loginURL);
                // 将请求体内容加入请求中
                httpPost.setEntity(requestHttpEntity);
                // 需要客户端对象来发送请求
                HttpClient httpClient = new DefaultHttpClient();
                // 发送请求
                HttpResponse response = httpClient.execute(httpPost);
                HttpEntity entity = response.getEntity();
                if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                    String res = EntityUtils.toString(entity);
                    JSONObject resjson = new JSONObject(res);
                    int reply_code = resjson.getInt("reply_code");
                    if(reply_code==201){
                        logoutHandle.sendEmptyMessage(0);
                    }else{
                        Bundle bun = new Bundle();
                        bun.putString("error","下线错误："+reply_code);
                        Message msg = new Message();
                        msg.setData(bun);
                        expHandle.sendMessage(msg);
                    }
                }
            } catch (Exception e) {
                Log.i(e.getMessage(),"Post request");
                Bundle bun = new Bundle();
                bun.putString("error","发生异常");
                Message msg = new Message();
                msg.setData(bun);
                expHandle.sendMessage(msg);
            }
        }
    };

    Runnable loginNetworkTask = new Runnable() {
        @Override
        public void run() {
            EditText idtext = (EditText)findViewById(R.id.editText);
            EditText passtext = (EditText)findViewById(R.id.editText2);
            String id = idtext.getText().toString();
            String pass = passtext.getText().toString();
            NameValuePair pair1 = new BasicNameValuePair("username", id);
            NameValuePair pair2 = new BasicNameValuePair("password", pass);
            NameValuePair pair3 = new BasicNameValuePair("action", "login");

            List<NameValuePair> pairList = new ArrayList<NameValuePair>();
            pairList.add(pair1);
            pairList.add(pair2);
            pairList.add(pair3);

            try {
                HttpEntity requestHttpEntity = new UrlEncodedFormEntity(
                        pairList);
                // URL使用基本URL即可，其中不需要加参数
                HttpPost httpPost = new HttpPost(loginURL);
                // 将请求体内容加入请求中
                httpPost.setEntity(requestHttpEntity);
                // 需要客户端对象来发送请求
                HttpClient httpClient = new DefaultHttpClient();
                // 发送请求
                HttpResponse response = httpClient.execute(httpPost);
                HttpEntity entity = response.getEntity();
                if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                    String res = EntityUtils.toString(entity);
                    JSONObject resjson = new JSONObject(res);
                    int reply_code = resjson.getInt("reply_code");
                    if(reply_code==101){
                        JSONObject userjson = new JSONObject(resjson.getString("userinfo"));
                        String userid = userjson.getString("username");
                        String name = userjson.getString("fullname");
                        double payamount = userjson.getDouble("payamount");
                        String area = userjson.getString("area_name");
                        Bundle bun = new Bundle();
                        bun.putString("userid",userid);
                        bun.putString("name",name);
                        bun.putString("payamount",payamount+"");
                        bun.putString("area",area);
                        Message msg = new Message();
                        msg.setData(bun);
                        loginHandle.sendMessage(msg);
                    }else{
                        Bundle bun = new Bundle();
                        bun.putString("error","登录错误："+reply_code);
                        Message msg = new Message();
                        msg.setData(bun);
                        expHandle.sendMessage(msg);
                    }
                }

            } catch (Exception e) {
                Log.i(e.getMessage(),"Post request");
                Bundle bun = new Bundle();
                bun.putString("error","发生异常");
                Message msg = new Message();
                msg.setData(bun);
                expHandle.sendMessage(msg);
            }
        }
    };

    AnimatorSet hideAnimatorSet;
    AnimatorSet backAnimatorSet;
    private void animateBack(){
        if(hideAnimatorSet!=null&&hideAnimatorSet.isRunning()){
            hideAnimatorSet.cancel();
        }
        if(backAnimatorSet!=null&&backAnimatorSet.isRunning()){
            //do nothing
        }else{
            backAnimatorSet = new AnimatorSet();
            ObjectAnimator animator = null;
            RelativeLayout loginlayout = (RelativeLayout)findViewById(R.id.loginlayout);
            animator = ObjectAnimator.ofFloat(loginlayout, "translationY", loginlayout.getTranslationY(), 0);
            ArrayList<Animator> animators = new ArrayList<Animator>();
            animators.add(animator);
            backAnimatorSet.setDuration(300);
            backAnimatorSet.playTogether(animators);
            backAnimatorSet.start();

        }
    }
    private void animateHide(){
        if(backAnimatorSet!=null&&backAnimatorSet.isRunning()){
            backAnimatorSet.cancel();
        }
        if(hideAnimatorSet!=null&&hideAnimatorSet.isRunning()){
            //do nothing
        }else{
            hideAnimatorSet = new AnimatorSet();
            ObjectAnimator animator = null;
            RelativeLayout loginlayout = (RelativeLayout)findViewById(R.id.loginlayout);
            animator = ObjectAnimator.ofFloat(loginlayout, "translationY", loginlayout.getTranslationY(), -loginlayout.getHeight());
            ArrayList<Animator> animators = new ArrayList<Animator>();
            animators.add(animator);
            hideAnimatorSet.setDuration(300);
            hideAnimatorSet.playTogether(animators);
            hideAnimatorSet.start();

        }
    }

}
