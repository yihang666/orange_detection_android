package com.example.imagepost;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mButton;
    private ImageView mImageView,imageView1;
    private TextView mTextView,mTextViewTime,mTextViewName;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.CAMERA",};
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final String TAG = "MainActivity";
    private final int IMAGE_RESULT_CODE = 2;
    private final int PICK = 1;
    private String url = "http://47.97.105.170:3389/api/test";
    private Uri uri;
    private String imgString = "";
    private Dialog dialog;
    private String filePath;
    private int sdkVersion = Integer.valueOf(android.os.Build.VERSION.SDK);
    private static final int SUCCESS = 1;
    private static final int FALL = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 相机和相册权限验证
        verifyStoragePermissions(this);
        initView();
        initClick();

    }

    Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                //加载网络成功进行UI的更新,处理得到的图片资源
                case SUCCESS:
                    //通过message，拿到字节数组
                    byte[] Picture = (byte[]) msg.obj;
                    //使用BitmapFactory工厂，把字节数组转化为bitmap
                    Bitmap bitmap = BitmapFactory.decodeByteArray(Picture, 0, Picture.length);
                    //通过imageview，设置图片
                    imageView1.setImageBitmap(bitmap);

                    break;
                //当加载网络失败执行的逻辑代码
                case FALL:
                    Toast.makeText(MainActivity.this, "网络出现了问题", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private void initClick() {
        mButton.setOnClickListener(this);
    }

    private void initView() {
        mButton = findViewById(R.id.button);
        mImageView = findViewById(R.id.imageView);
        imageView1 = findViewById(R.id.imageView1);
        mTextView = findViewById(R.id.textView);
        mTextViewTime = findViewById(R.id.textView2);
        mTextViewName = findViewById(R.id.textView3);

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button:
                showDialog();
                break;
            default:
                break;
        }
    }

    // 弹窗
    private void showDialog() {

        dialog = new Dialog(this, R.style.DialogTheme);
        dialog.setCanceledOnTouchOutside(true);//设置点击外部弹窗取消
        dialog.setCancelable(true);
        View view = View.inflate(this, R.layout.dialog_xml, null);
        dialog.setContentView(view);


        Window window = dialog.getWindow();
        window.setWindowAnimations(R.style.main_menu_animStyle);
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        window.setGravity(Gravity.CENTER);
        dialog.show();

        dialog.findViewById(R.id.pashe).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 打开相机拍照
                Intent paiZhao = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(paiZhao, PICK);
            }
        });

        dialog.findViewById(R.id.huoqu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 从相册获取
                Intent huoqu = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(huoqu, IMAGE_RESULT_CODE);
            }
        });

        dialog.findViewById(R.id.quxiao).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });


    }

    // 检测是否有写入权限
    public static void verifyStoragePermissions(AppCompatActivity activity) {
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PICK:
                if (resultCode == RESULT_OK) {
                    // SD卡状态
                    String sdStatus = Environment.getExternalStorageState();
                    if (!sdStatus.equals(Environment.MEDIA_MOUNTED)) {
                        Log.e("MainActivity", "SD卡不存在");
                        return;
                    }

                    Bundle bundle = data.getExtras();
                    Bitmap bitmap = (Bitmap) bundle.get("data");
                    Log.d("MainActivity", "onActivityResult: " + bitmap);
                    File file = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/");
                    //File file = new File(getExternalFilesDir(null) + "/");
                    if (!file.exists()) {
                        file.mkdir();
                    }

                    String name = System.currentTimeMillis() + ".jpg";
                    filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + name;
                    BufferedOutputStream bos = null;

                    try {
                        bos = new BufferedOutputStream(new FileOutputStream(filePath));
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            bos.flush();
                            bos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    // 设置imageView显示图片
                    mImageView.setImageBitmap(bitmap);
                    imgString = bitmapToBase64(bitmap);

                    // 此部分代码用于检测base64处理图片成功与否
                    Log.d(TAG,"bitmap"+imgString);
                    //Bitmap bitmap2 = PhotoUtils.base64ToBitmap(imgString);
                    //imageView1.setImageBitmap(bitmap2);

                    dialog.dismiss();
                    okHttpUploadImage(filePath);
                }
                break;
            case IMAGE_RESULT_CODE:
                if (resultCode == RESULT_OK) {
                    uri = data.getData();
                    // 设置imageView显示图片
                    mImageView.setImageURI(uri);
                    Bitmap bitmap2 = PhotoUtils.getBitmapFromUri(uri, this);
                    imgString = bitmapToBase64(bitmap2);
                    dialog.dismiss();

                    if (sdkVersion >= 19) {
                        filePath = getPath_above19(MainActivity.this, uri);
                    } else {
                        filePath = getFilePath_below19(uri);
                    }
                    okHttpUploadImage(filePath);
                }
                break;
        }
    }

    // 进行图片处理
    public static String bitmapToBase64(Bitmap bitmap) {

        String result = null;
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

                baos.flush();
                baos.close();

                byte[] bitmapBytes = baos.toByteArray();
                result = Base64.encodeToString(bitmapBytes, Base64.DEFAULT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static byte[] GetUserHead(String img_url) throws IOException {
        URL url = new URL(img_url);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET"); // 设置请求方法为GET
        conn.setReadTimeout(5 * 1000); // 设置请求过时时间为5秒
        InputStream inputStream = conn.getInputStream(); // 通过输入流获得图片数据
        byte[] data = StreamTool.readInputStream(inputStream); // 获得图片的二进制数据
        return data;

    }


    // 上传图片
    private void okHttpUploadImage(String filePath) {
        // 创建 OkHttpClient
        OkHttpClient client = new OkHttpClient.Builder().build();
        // 要上传的文件
        File file = new File(filePath);
        MediaType mediaType = MediaType.parse("image/jpeg");
        // 把文件封装进请求体
        RequestBody fileBody = RequestBody.create(mediaType, file);
        // MultipartBody 上传文件专用的请求体
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM) // 表单类型(必填)
                .addFormDataPart("image", file.getName(), fileBody)
                .build();
        final Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:66.0) Gecko/20100101 Firefox/66.0")
                .post(body)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "上传成功：" + response.code());
                    String result = response.body().string();
                    JsonParser jsonParser = new JsonParser();
                    JsonObject jsonObject = jsonParser.parse(result).getAsJsonObject();
                    // 获取返回数据
                    final String file_name = jsonObject.get("flie_name").getAsString();
                    final String box_num = jsonObject.get("box_num").getAsString();
                    //mTextView.setText(box_num);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTextView.setText(box_num);
                            mTextViewName.setText(file_name);
                        }
                    });

                    // 接受返回的图片流，并转换成图片显示在imageView1上
                    //String img_strean = jsonObject.get("img_strean(base64)").getAsString();
                    //Log.d(TAG,"img_strean(base64):"+img_strean);
                    //Bitmap bitmap = PhotoUtils.stringtoBitmap(img_strean);
                    //Log.d(TAG,"bitmap"+bitmap);
                    //imageView1.setImageBitmap(bitmap);

                    //通过URL接收返回的图片

                    String back_img_url = jsonObject.get("back_url").getAsString();
                    Request request = new Request.Builder().url(back_img_url).build();
                    OkHttpClient okHttpClient = new OkHttpClient();
                    Call call_back = okHttpClient.newCall(request);
                    call_back.enqueue(new Callback() {
                        @Override
                        public void onFailure(@NotNull Call call, @NotNull IOException e) {

                        }

                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                            //得到从网上获取资源，转换成我们想要的类型
                            byte[] Picture_bt = response.body().bytes();
                            //通过handler更新UI
                            Message message = handler.obtainMessage();
                            message.obj = Picture_bt;
                            message.what = SUCCESS;
                            handler.sendMessage(message);
                        }
                    });

                    Log.d(TAG, "数据上传成功："+"file_name:" + file_name + ";box_num:" + box_num);

                } else {
                    System.out.println(response.code());
                    Log.e(TAG, "数据上传失败："+"respnse:" + response.code());
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG,"数据上传失败："+"onFailure: " + e.getMessage());
            }
        });
    }



    // 获取相册图片的路径，API19以下
    private String getFilePath_below19(Uri uri) {
        //这里开始的第二部分，获取图片的路径：低版本的是没问题的，但是sdk>19会获取不到
        String[] proj = {MediaStore.Images.Media.DATA};
        //好像是android多媒体数据库的封装接口，具体的看Android文档
        Cursor cursor = getContentResolver().query(uri, proj, null, null, null);
        //获得用户选择的图片的索引值
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        Log.d(TAG, "column_index:"+column_index);
        //将光标移至开头 ，这个很重要，不小心很容易引起越界
        cursor.moveToFirst();
        //最后根据索引值获取图片路径   结果类似：/mnt/sdcard/DCIM/Camera/IMG_20151124_013332.jpg
        String path = cursor.getString(column_index);
        System.out.println("path:" + path);
        return path;
    }

    // API 19以上
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getPath_above19(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
}
