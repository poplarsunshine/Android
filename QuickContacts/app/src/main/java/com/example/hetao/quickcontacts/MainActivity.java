package com.example.hetao.quickcontacts;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.GestureDetector;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Adapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hetao.quickcontacts.tools.ImageTools;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends Activity implements AdapterView.OnItemClickListener, ListView.OnScrollListener{

    final int UP = 0;
    final int LEFT = 1;
    final int DOWN = 2;
    final int RIGHT = 3;

    private Intent intent;

    private GestureDetector gestureDetector;
    private GestureDetector listGestureDetector;

    private static final int REQUEST_CONTACT = 1;

    //list
    protected SuggestionAdapter adapter;
    ListView listview;
    List<Map<String, Object>> mContactList = initList();

    private boolean scrollFlag = false;// 标记是否滑动
    private int lastVisibleItemPosition;// 标记上次滑动位置

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        adapter = new SuggestionAdapter();

        listview = (ListView)findViewById(R.id.listView);
        listview.setAdapter(adapter);
        listview.setOnItemClickListener(this);
        listview.setOnScrollListener(this);
        listview.setVerticalScrollBarEnabled(false);

        gestureDetector = new GestureDetector(MainActivity.this, onGestureListener);

        mContactList = getDefaultContacts();
    }

    private List<Map<String, Object>> initList() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        return list;
    }

    private List<Map<String, Object>> getDefaultContacts() {

        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        SharedPreferences sp = getSharedPreferences("sp", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        if (sp.getBoolean("HadRun", false) == true)
        {
            try {
                list = this.getListFromStore();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else
        {
            editor.putBoolean("HadRun", true);
            editor.commit();

            ContentResolver resolver = getContentResolver();
            Cursor phoneCursor = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null, null, null, null); //传入正确的uri
            if(phoneCursor!=null) {
                while (phoneCursor.moveToNext()) {
                    String contactId = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.Contacts._ID));
                    String name = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)); //获取联系人number
                    String contactTimes = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED)); //通话数
                    //System.out.println("contact=" + name + " usernumber=" + phoneNumber + " c=" + contactTimes);

                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("name", name);
                    map.put("num", phoneNumber);
                    map.put("times", contactTimes);
                    map.put("contactId", contactId);

                    list.add(map);
                }
            }

            Collections.sort(list, new Comparator<Map<String, Object>>() {
                public int compare(Map<String, Object> o1,
                                   Map<String, Object> o2) {
                    String s1 = (String)o1.get("times");
                    String s2 = (String)o2.get("times");
                    int i1 = Integer.parseInt(s1);
                    int i2 = Integer.parseInt(s2);
                    return (i2 - i1);
                }
            });

            int count = 3;
            if (list.size() >= count)
            {
                list = list.subList(0, count);
            }
        }
        return list;
    }

    private GestureDetector.OnGestureListener onGestureListener =
            new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                                       float velocityY) {
                    float x = e2.getX() - e1.getX();
                    float y = e2.getY() - e1.getY();

                    if (y < 0 && Math.abs(x) < Math.abs(y)) {
                        doResult(UP);
                        return true;
                    }
                    else if (x < 0 && Math.abs(y) < Math.abs(x)) {
                        doResult(LEFT);
                        return true;
                    }
                    else if (y > 0 && Math.abs(x) < Math.abs(y)) {
                        doResult(DOWN);
                        return true;
                    }
                    else if (x > 0 && Math.abs(y) < Math.abs(x)) {
                        doResult(RIGHT);
                        return true;
                    }
                    return true;
                }
            };

    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    public void doResult(int action) {

        switch (action) {
            case UP:
                System.out.println("go UP");
                intent = new Intent(Intent.ACTION_PICK, android.provider.ContactsContract.Contacts.CONTENT_URI);
                intent.setData(ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(intent, REQUEST_CONTACT);

                this.overridePendingTransition(R.anim.popshow_anim, R.anim.pophidden_anim);

                break;

            case LEFT:
                System.out.println("go LEFT");
                break;

            case DOWN:
                System.out.println("go DOWN");
                break;

            case RIGHT:
                System.out.println("go RIGHT");
                break;
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        System.out.println("onScrollStateChanged");

        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
            scrollFlag = true;
            if (listview.getFirstVisiblePosition() < lastVisibleItemPosition) {
                Log.d("dc", "下滑");
            }
            if (listview.getFirstVisiblePosition() > lastVisibleItemPosition) {
                Log.d("dc", "上滑");
            }
        } else {
            scrollFlag = false;
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        System.out.println("onScroll");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_CONTACT) {
            if (resultCode == RESULT_OK) {
                if (data == null) {
                    return;
                }
                //ContentProvider展示数据类似一个单个数据库表
                //ContentResolver实例带的方法可实现找到指定的ContentProvider并获取到ContentProvider的数据
                ContentResolver reContentResolverol = getContentResolver();
                //URI,每个ContentProvider定义一个唯一的公开的URI,用于指定到它的数据集
                Uri contactData = data.getData();
                //查询就是输入URI等参数,其中URI是必须的,其他是可选的,如果系统能找到URI对应的ContentProvider将返回一个Cursor对象.
                Cursor cursor = reContentResolverol.query(contactData, null, null, null, null);
                cursor.moveToFirst();
                //获得DATA表中的名字
                String username = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                //条件为联系人ID
                String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                // 获得DATA表中的电话号码，条件为联系人ID,因为手机号码可能会有多个
                Cursor phone = reContentResolverol.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId,
                        null,
                        null);
                while (phone.moveToNext()) {
                    String usernumber = phone.getString(phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    String contactTimes = phone.getString(phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED)); //通话数

                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("name", username);
                    map.put("num", usernumber);
                    map.put("times", contactTimes);
                    map.put("contactId", contactId);

                    mContactList.add(map);
                    try {
                        saveListToStore(mContactList);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    adapter.notifyDataSetChanged();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public List<Map<String, Object>> getListFromStore() throws JSONException {

        System.out.println("getListFromStore");

        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        SharedPreferences sp = getSharedPreferences("sp", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        String jsonString = sp.getString("cotacts", null);
        if (jsonString != null)
        {
            JSONArray jsonAry = new JSONArray(jsonString);
            list = tranceJsonArray2List(jsonAry);
        }
        return list;
    }

    public void saveListToStore(List<Map<String, Object>> list) throws JSONException {

        System.out.println("saveListToStore");

        JSONArray jsonAry = tranceList2JsonArray(list);
        String jsonString = jsonAry.toString();

        SharedPreferences sp = getSharedPreferences("sp", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        if (sp.getBoolean("HadRun", false) == true)
        editor.putString("cotacts", jsonString);
        editor.commit();
    }

    public JSONArray tranceList2JsonArray(List<Map<String, Object>> list) throws JSONException {
        JSONArray ary = new JSONArray();

        System.out.println("list size=" + list.size());

        for (int i=0; i<list.size(); i++)
        {
            Map<String, Object> map = list.get(i);
            JSONObject obj = new JSONObject();
            obj.put("contactId", (String)map.get("contactId"));
            obj.put("name", (String)map.get("name"));
            obj.put("num", (String)map.get("num"));
            obj.put("times", (String)map.get("times"));
            ary.put(obj);
        }
        return ary;
    }

    public List<Map<String, Object>> tranceJsonArray2List(JSONArray ary) throws JSONException {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (int i=0; i<ary.length(); i++)
        {
            JSONObject obj = (JSONObject)ary.getJSONObject(i);
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("contactId", obj.get("contactId"));
            map.put("name", obj.get("name"));
            map.put("num", obj.get("num"));
            map.put("times", obj.get("times"));
            list.add(map);
        }
        return list;
    }

    //
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //String itemStr = datas.get(position);

        Map<String, Object> info = mContactList.get(position);

        Toast.makeText(this, info.get("name") + " " + info.get("num"), Toast.LENGTH_SHORT)
                .show();
    }

    private void callPhoneNum(String phoneno)
    {
        if(phoneno==null||"".equals(phoneno.trim()))
        {
            Toast.makeText(getApplicationContext(), "电话号码不正确",
                    Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(getApplicationContext(), "有电话号码"+phoneno,
                    Toast.LENGTH_SHORT).show();
            //Intent intent=new Intent(Intent.ACTION_CALL,Uri.parse("tel:"+phoneno));
            //startActivity(intent);
        }
    }

    private class SuggestionAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mContactList.size();
        }

        @Override
        public Map<String, Object> getItem(int i) {
            return mContactList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(final int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.contact_item,
                        viewGroup, false);
            }

            //为每一个view项设置触控监听
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    //当按下时处理
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        System.out.println("setOnTouchListener");
                        Map<String, Object> info = mContactList.get(i);
                        callPhoneNum((String)info.get("num"));
                    }
                    return true;
                }
            });

            //
            ImageView image = (ImageView) view.findViewById(R.id.imageView);
            TextView line1 = (TextView) view.findViewById(R.id.textView_name);
            TextView line2 = (TextView) view.findViewById(R.id.textView_num);
            Button btnDel = (Button) view.findViewById(R.id.del);

            Map<String, Object> info = mContactList.get(mContactList.size() - 1 - i);

            Uri iconUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.parseLong((String)info.get("contactId")));

            InputStream input = null;
            if (iconUri != null)
            {
                input = ContactsContract.Contacts.openContactPhotoInputStream(getContentResolver(), iconUri);
            }
            if(input != null)
            {
                Bitmap bmp_head = BitmapFactory.decodeStream(input);
                ImageTools tools = new ImageTools();
                bmp_head = tools.toRoundBitmap(bmp_head);
                image.setImageBitmap(bmp_head);
            }
            else
            {
                image.setImageResource(R.drawable.ic_launcher);
            }

            line1.setText((String)info.get("name"));
            line2.setText((String)info.get("num"));

            btnDel.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mContactList.remove(i);
                    try {
                        saveListToStore(mContactList);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    notifyDataSetChanged();
                }
            });
            return view;
        }
    }
}
