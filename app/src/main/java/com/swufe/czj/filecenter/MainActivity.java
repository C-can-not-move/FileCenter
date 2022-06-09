package com.swufe.czj.filecenter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class MainActivity extends ListActivity implements AdapterView.OnItemLongClickListener {


    //先定义
    private static final int REQUEST_EXTERNAL_STORAGE = 0;

    private static final String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.MANAGE_EXTERNAL_STORAGE"};

    //然后通过一个函数来申请
    public static void verifyStoragePermissions(Activity activity) {
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //定义成员变量
    //存放显示的文件的列表的名称
    private List<String> mFileName = null;
    //存放显示文件列表的相对应的路径
    private List<String> mFilePath = null;
    //起始目录“/”
    private final String mRootPath = "/storage/emulated/0";//File.separator;
    //sd卡目录
    private final String mSDCard = Environment.getExternalStorageDirectory().toString();
    private String mOldFilePath = "";
    private String mNewFilePath = "";
    private String keyWords;
    //显示当前路径
    private TextView mPath;
    //放置工具栏
    private GridView mGridViewToolbar;
    private final int[] girdView_menu_image = {R.drawable.phone, R.drawable.sd,
            R.drawable.search, R.drawable.newfile,
            R.drawable.copy, R.drawable.exit};
    private final String[] gridview_menu_title = {"手机","SD卡","搜索","创建","粘贴","退出"};
    //代表是手机还是SD卡，1是手机，2是SD卡
    private static int menuPosition = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyStoragePermissions(this);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectAll();

        //初始化菜单视图
        initGridViewMenu();
        //初始化菜单监听器
        initMenuListener();
        //为列表项绑定长按监听器
        getListView().setOnItemLongClickListener(this);
        mPath = (TextView) findViewById(R.id.mPath);
        //程序开始时加载手机目录下的文件列表
        initFileListInfo(mRootPath);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        if(isAddBackUp){//说明存在返回根目录和返回上一级两列，接下来要对这两列进行屏蔽
            if(i != 0 && i !=1){
                initItemLongClickListener(new File(mFilePath.get(i)));
            }

        }
        if(mCurrentFilePath.equals(mRootPath)||mCurrentFilePath.equals(mSDCard)){
            initItemLongClickListener(new File(mFilePath.get(i)));
        }
        return false;
    }
    private String mCopyFileName;
    private boolean isCopy = false;
    /**
     * 长按文件或文件夹弹出的带有ListView效果的功能菜单
     */
    private void initItemLongClickListener(final File file){
        //item的值是从0开始的索引值
        DialogInterface.OnClickListener listener = (dialog, item) -> {
            if(file.canRead()){//需要文件可读
                if(item==0){
                    if(file.isFile()&&"txt".equalsIgnoreCase((file.getName().substring(file.getName().lastIndexOf(".")+1)))){
                        Toast.makeText(MainActivity.this,"已复制！",Toast.LENGTH_SHORT).show();
                        isCopy = true;
                        mCopyFileName = file.getName();
                        mOldFilePath = mCurrentFilePath+ File.separator+mCopyFileName;
                    } else{
                        Toast.makeText(MainActivity.this, "现在只做了复制文本文件", Toast.LENGTH_SHORT).show();

                    }
                }else if(item ==1 ){
                    initRenameDialog(file);
                }else if(item ==2){
                    initDeleteDialog(file);
                }
            }else{
                Toast.makeText(MainActivity.this,"文件不可读",Toast.LENGTH_SHORT).show();
            }
        };
        //列表项
        String[] mMenu = {"复制","重命名","删除"};
        //操作对话框
        new AlertDialog.Builder(MainActivity.this).setTitle("请选择操作！")
                .setItems(mMenu,listener)
                .setPositiveButton("取消",null).show();
    }

    /**
     * 为GridView配置菜单资源
     */
    private void initGridViewMenu(){
        //选择菜单项
        mGridViewToolbar = (GridView) findViewById(R.id.file_gridview_toolbar);
        //设置选中时的背景图片
        mGridViewToolbar.setSelector(R.drawable.menu_item_selected);
        //设置背景图片
        mGridViewToolbar.setBackgroundResource(R.drawable.menu_background);
        //设置列数
        mGridViewToolbar.setNumColumns(6);
        //设置居中对齐
        mGridViewToolbar.setGravity(Gravity.CENTER);
        //设置水平、垂直间距为10
        mGridViewToolbar.setVerticalSpacing(10);
        mGridViewToolbar.setHorizontalSpacing(10);
        //设置适配器
        mGridViewToolbar.setAdapter(getMenuAdapter(gridview_menu_title,girdView_menu_image));
    }

    /**
     * 菜单适配器
     */
    private SimpleAdapter getMenuAdapter(String[] menuNameArray,int[] imageResourceArray){
        //数组列表用于存放映射表
        ArrayList<HashMap<String,Object>> mData = new ArrayList<>();
        for(int i=0;i<menuNameArray.length;i++){
            HashMap<String,Object> mMap = new HashMap<>();
            //将image映射成图片资源
            mMap.put("Image",imageResourceArray[i]);
            //将title映射成标题
            mMap.put("title",menuNameArray[i]);
            mData.add(mMap);
        }
        //新建简单适配器，设置适配器的布局文件和映射关系
        return new SimpleAdapter(this,mData,R.layout.item_menu,
                new String[]{"Image","title"},new int[]{R.id.item_image,R.id.item_text});
    }

    /**
     * 菜单项的监听
     */
    protected void initMenuListener(){
        mGridViewToolbar.setOnItemClickListener(
                (arg0, arg1, arg2, arg3) -> {
                    switch (arg2){
                        //回到根目录
                        case 0:
                            menuPosition = 1;
                            initFileListInfo(mRootPath);
                            break;
                        //回到SD目录
                        case 1:
                            menuPosition = 2;
                            initFileListInfo(mSDCard);
                            break;
                        //显示搜索对话框
                        case 2:
                            searchDilalog();
                            break;
                        //创建文件夹
                        case 3:
                            createFolder();
                            break;
                        //粘贴文件
                        case 4:
                            palseFile();
                            break;
                        //退出
                        case 5:
                            MainActivity.this.finish();
                            break;
                    }
                }
        );
    }

    //使用静态变量存储当前目录路径信息
    public static String mCurrentFilePath = "";
    /**
     * 根据给定的一个文件夹路径字符串遍历这个文件夹中包含的文件名称并配置到LIstView列表中
     */
    private void initFileListInfo(String filePath){
        isAddBackUp = false;
        mCurrentFilePath = filePath;
        //显示当前的路径
        mPath.setText(filePath);
        mFileName = new ArrayList<>();
        mFilePath = new ArrayList<>();
        File mFile = new File(filePath);
        //遍历该文件夹路径下的所有文件/文件夹
        File[] mFiles = mFile.listFiles();
        assert mFiles != null;
        Log.i("789", Arrays.toString(mFiles));
        //只要当前路径不是手机根目录或者是sd卡根目录，则显示”返回根目录“和”返回上一级“
        if(menuPosition == 1&&!mCurrentFilePath.equals(mRootPath)){
            initAddBackUp(filePath,mRootPath);
        }else if(menuPosition == 2&&!mCurrentFilePath.equals(mSDCard)){
            initAddBackUp(filePath,mSDCard);
        }
        //将文件信息添加到集合中
        assert mFiles != null;
        for(File mCurrentFile:mFiles){
            mFileName.add(mCurrentFile.getName());
            mFilePath.add(mCurrentFile.getPath());
        }
        //配置数据
        setListAdapter(new FileAdapter(MainActivity.this, mFileName, mFilePath));
    }
    /**
     * 根据单击”手机“还是”SD卡“来加”返回根目录“和”返回上一级“
     */
    private boolean isAddBackUp = false;
    private void initAddBackUp(String filePath, String phone_sdcard){
        if(!filePath.equals(phone_sdcard)){
            //第一项设置为返回根目录
            mFileName.add("BacktoRoot");
            mFilePath.add(phone_sdcard);
            //第二项为返回上一级
            mFileName.add("BacktoUp");
            mFilePath.add(new File(filePath).getParent());
            //将添加返回键标识位设置为true
            isAddBackUp = true;
        }
    }

    /**
     * 创建文件夹
     */
    private String mNewFolderName = "";
    private File mCreateFile;
    private static int mChecked;
    private void createFolder(){
        //标识是文件还是文件夹
        mChecked = 2;
        LayoutInflater mLI = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //初始化对话框
        final LinearLayout mLL = (LinearLayout) mLI.inflate(R.layout.create_dialog,null);
        RadioGroup mCreateRadioGroup = (RadioGroup) mLL.findViewById(R.id.radiogroup_create);
        final RadioButton mCreateFileButton = (RadioButton)mLL.findViewById(R.id.create_file);
        final RadioButton mCreateFolderButton = (RadioButton)mLL.findViewById(R.id.create_folder);
        //设置为默认创建文件夹
        mCreateFolderButton.setChecked(true);
        //设置监听器
        //当选择改变时触发
        mCreateRadioGroup.setOnCheckedChangeListener((arg0, arg1) -> {
            if(arg1 == mCreateFileButton.getId()){
                mChecked = 1;
            }else if(arg1 == mCreateFolderButton.getId()){
                mChecked = 2;
            }
        });
        //显示对话框
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this)
                .setTitle("新建")
                .setView(mLL)
                .setPositiveButton("创建", (dialog, which) -> {
                    //获得用户输入的名称
                    mNewFolderName = ((EditText)mLL.findViewById(R.id.new_filename)).getText().toString();
                    if(mChecked==1){
                        try{
                            mCreateFile = new File(mCurrentFilePath+File.separator+mNewFolderName+".txt");
                            mCreateFile.createNewFile();
                            //刷新文件列表
                            initFileListInfo(mCurrentFilePath);
                        } catch (IOException e) {
                            Toast.makeText(MainActivity.this,"文件名拼接错误。。！！",Toast.LENGTH_SHORT).show();
                        }
                    }else if (mChecked==2){
                        mCreateFile = new File(mCurrentFilePath+File.separator+mNewFolderName);
                        if(!mCreateFile.exists()&&!mCreateFile.isDirectory()&&mNewFolderName.length()!=0){
                            if(mCreateFile.mkdirs()){
                                initFileListInfo(mCurrentFilePath);
                            }else{
                                Toast.makeText(MainActivity.this,"创建失败",Toast.LENGTH_SHORT).show();
                            }
                        }else{
                            Toast.makeText(MainActivity.this,"文件名为空，或重名",Toast.LENGTH_SHORT).show();
                        }
                    }
                }).setNeutralButton("取消",null);
        mBuilder.show();
    }

    /**
     * 重命名文件
     */
    EditText mET;
    //显示重名名对话框
    private void initRenameDialog(final File file){
        LayoutInflater mLI = LayoutInflater.from(MainActivity.this);
        //初始化重命名对话框
        LinearLayout mLL = (LinearLayout)mLI.inflate(R.layout.rename_dialog,null);
        mET = mLL.findViewById(R.id.new_filename);
        //显示当前的文件名
        mET.setText(file.getName());
        //设置监听器
        DialogInterface.OnClickListener listener = (dialog, which) -> {
            String modifyName = mET.getText().toString();
            Log.i("OK!",modifyName);
            final String modifyFilePath = Objects.requireNonNull(file.getParentFile()).getPath()+File.separator;
            final String newFilePath = modifyFilePath+modifyName;
            Log.i("OK!",newFilePath);
            //判断新的文件名是否存在
            if(new File(newFilePath).exists()) {
                Log.i("OK","文件名已存在");
                if (!modifyName.equals(file.getName())) {//过滤重命名操作不做修改的情况
                    //弹出提示
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("提示！")
                            .setMessage("该文件以及存在，是否覆盖？")
                            .setPositiveButton("确认", (dialog1, which1) -> {
                                file.renameTo(new File(newFilePath));
                                Toast.makeText(MainActivity.this, "文件路径" + new File(newFilePath), Toast.LENGTH_SHORT).show();
                                //更新目录信息
                                initFileListInfo(file.getParentFile().getPath());
                            }).setNegativeButton("取消", null).show();

                }
            }else{
                file.renameTo(new File(newFilePath));
                Log.i("OK!","重命名成功！");
                initFileListInfo(file.getParentFile().getPath());
            }
        };
        //显示对话框
        AlertDialog renameDoalog = new AlertDialog.Builder(MainActivity.this).create();
        renameDoalog.setView(mLL);
        renameDoalog.setButton("确定",listener);
        renameDoalog.setButton2("取消", (dialogInterface, i) -> {

        });
        renameDoalog.show();
    }

    //弹出删除文件/文件夹的对话框
    private void initDeleteDialog(final File file){
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("提示！")
                .setMessage("你确定要删除该"+(file.isDirectory()?"文件夹":"文件")+"?")
                .setPositiveButton("确定", (dialog, which) -> {
                    if(file.isFile()){
                        //文件的话直接删除
                        file.delete();
                    }else{
                        //是文件夹则用这方法
                        deleteFolder(file);
                    }
                    //重新遍历待文件的父目录
                    initFileListInfo(file.getParent());
                })
                .setNegativeButton("取消",null)
                .show();
    }
    /**
     * 删除文件夹的方法(递归删除文件夹下的所有文件)
     */
    public void deleteFolder(File folder){
        File[] fileArray = folder.listFiles();
        assert fileArray != null;
        if(fileArray.length==0){
            //空文件夹直接删除
            folder.delete();
        }else{
            //遍历该目录
            for(File currentFile:fileArray){
                if(currentFile.exists()&&currentFile.isFile()){
                    //文件直接删除
                    currentFile.delete();
                }else{
                    //递归删除
                    deleteFolder(currentFile);
                }
            }
        }
        folder.delete();
    }

    /**
     * 粘贴
     */
    private void palseFile(){
        mNewFilePath = mCurrentFilePath+File.separator+mCopyFileName;
        Log.i("copy","mOldFilePath is "+mOldFilePath+"| mNewFilePath is"+mNewFilePath+"| isCopy "+isCopy);
        if(!mOldFilePath.equals(mNewFilePath)&& isCopy){
            //在不同路径下复制才有效
            if(!new File(mNewFilePath).exists()){
                copyFile(mOldFilePath,mNewFilePath);
                Toast.makeText(MainActivity.this,"执行了粘贴",Toast.LENGTH_SHORT).show();
                initFileListInfo(mCurrentFilePath);
            }else{
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("提示!")
                        .setMessage("该文件名已存在，是否覆盖？")
                        .setPositiveButton("确定", (dialog, which) -> {
                            copyFile(mOldFilePath,mNewFilePath);
                            initFileListInfo(mCurrentFilePath);
                        })
                        .setNegativeButton("取消",null).show();
            }
        }else{
            Toast.makeText(MainActivity.this,"未复制文件！",Toast.LENGTH_LONG).show();
        }
    }
    private int i;
    FileInputStream fis;
    FileOutputStream fos;
    //复制文件
    private void copyFile(String oldFile,String newFile){
        try{
            fis = new FileInputStream(oldFile);
            fos = new FileOutputStream(newFile);
            while(i != -1){
                if((i = fis.read())!= -1){
                    fos.write(i);
                }
            }
            if(fis != null){
                fis.close();
            }
            if(fos != null){
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示搜索对话框
     */
    public int mRadioChecked = 1;
    public RadioGroup mRadioGroup;
    public static String KEYWORD_BROADCAST = "KEYWORD_BROADCAST";
    public Intent serviceIntent;
    private void searchDilalog(){
        mRadioChecked = 1;
        LayoutInflater mLI = LayoutInflater.from(MainActivity.this);
        final View mLL = (View)mLI.inflate(R.layout.search_dialog,null);
        mRadioGroup = (RadioGroup)mLL.findViewById(R.id.radiogroup_search);
        final RadioButton mCurrentPathButton = (RadioButton)mLL.findViewById(R.id.radio_currentpath);
        final RadioButton mWholePathButton = (RadioButton)mLL.findViewById(R.id.radio_wholepath);
        //设置默认在当前路径搜索
        mCurrentPathButton.setChecked(true);
        mRadioGroup.setOnCheckedChangeListener((radioGroup, checkId) -> {
            //当前路径的标志是1
            if (checkId == mCurrentPathButton.getId()) {
                mRadioChecked = 1;
            } else if (checkId == mWholePathButton.getId()) {
                mRadioChecked = 2;
            }
        });
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this)
                .setTitle("搜索").setView(mLL)
                .setPositiveButton("确定", (arg0, arg1) -> {
                    keyWords = ((EditText)mLL.findViewById(R.id.edit_search)).getText().toString();
                    if(keyWords.length() ==0){
                        Toast.makeText(MainActivity.this,"关键字不能为空！",Toast.LENGTH_SHORT).show();
                        searchDilalog();
                    }else{
                        if(menuPosition == 1){
                            mPath.setText(mRootPath);
                        }else {
                            mPath.setText(mSDCard);
                        }
                        //获取用户输入的关键字并发送广播
                        Intent keywordIntent = new Intent(this,SearchBroadCast.class);
                        keywordIntent.setAction(KEYWORD_BROADCAST);
                        keywordIntent.setPackage(getPackageName());
                        //传递范围
                        if(mRadioChecked == 1){
                            keywordIntent.putExtra("searchpath",mCurrentFilePath);
                        }else{
                            keywordIntent.putExtra("searchpath",mSDCard);
                        }
                        keywordIntent.putExtra("keyword",keyWords);
                        getApplicationContext().sendBroadcast(keywordIntent);
                        serviceIntent = new Intent("com.android.service.FILE_SEARCH_START");
                        serviceIntent.setPackage(getPackageName());
                        MainActivity.this.startService(serviceIntent);
                        isComeBackFromNotification = false;
                    }
                })
                .setNegativeButton("取消",null);
        mBuilder.create().show();
    }

    private FileBroadcast mFileBroadcasr;
    private SearchBroadCast mServiceBroadCast;
    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(FileService.FILE_SEARCH_COMPLETED);
        mFilter.addAction(FileService.FILE_NOTIFICATION);
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(KEYWORD_BROADCAST);
        if(mFileBroadcasr == null){
            mFileBroadcasr = new FileBroadcast();
        }
        if(mServiceBroadCast == null){
            mServiceBroadCast = new SearchBroadCast();
        }
        this.registerReceiver(mFileBroadcasr, mFilter);
        this.registerReceiver(mServiceBroadCast, mIntentFilter);
    }
    /**
     * 注销广播
     */
    @Override
    protected void onDestroy(){
        super.onDestroy();
        Log.i("空指针","onDestroy");
        mFileName.clear();
        mFilePath.clear();
        this.unregisterReceiver(mFileBroadcasr);
        this.unregisterReceiver(mServiceBroadCast);
    }
    private String mAction;
    public static boolean isComeBackFromNotification = false;
    /**
     * 内部广播类
     */
    class FileBroadcast extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            mAction = intent.getAction();
            //搜索完毕的广播
            if(FileService.FILE_SEARCH_COMPLETED.equals(mAction)){
                mFileName = intent.getStringArrayListExtra("mFileNameList");
                mFilePath = intent.getStringArrayListExtra("mFilePathsList");
                Toast.makeText(MainActivity.this,"搜索完毕！",Toast.LENGTH_SHORT).show();
                //提示搜索框
                searchCompletedDialog("完成！是否显示结果?");
                getApplicationContext().stopService(serviceIntent);
            }
            //通知栏跳转过来的广播
            else if(FileService.FILE_NOTIFICATION.equals(mAction)){
                String mNotification = intent.getStringExtra("notification");
                Toast.makeText(MainActivity.this,mNotification,Toast.LENGTH_LONG).show();
                searchCompletedDialog("确定取消搜索？");
            }
        }
    }
    private void searchCompletedDialog(String message){
        AlertDialog.Builder searchDialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle("提示")
                .setMessage(message)
                .setPositiveButton("确定", (dialog, which) -> {
                    //对现阶段状况进行判断
                    if(FileService.FILE_SEARCH_COMPLETED.equals(mAction)){
                        if(mFileName.size() == 0){
                            Toast.makeText(MainActivity.this,"没有找到！！！！",Toast.LENGTH_SHORT).show();
                        }else{
                            //现实文件列表
                            setListAdapter(new FileAdapter(MainActivity.this, mFileName, mFilePath));
                        }
                    }else{
                        //设置状态
                        isComeBackFromNotification = true;
                        getApplicationContext().stopService(serviceIntent);
                    }
                })
                .setNegativeButton("取消",null);
                searchDialog.create();
                searchDialog.show();
    }
    /**
     * 自定义Adapter内部类
     */
    static class FileAdapter extends BaseAdapter{

        //图标
        private final Bitmap mBackRoot;
        private final Bitmap mBackUp;
        private final Bitmap mImage;
        private final Bitmap mAudio;
        private final Bitmap mRar;
        private final Bitmap mVideo;
        private final Bitmap mFolder;
        private final Bitmap mApk;
        private final Bitmap mOthers;
        private final Bitmap mTxt;
        private final Bitmap mWeb;

        private final Context mContext;
        //文件名列表
        private final List<String> mFileNameList;
        //对应的路径
        private final List<String> mFilePathList;

        public FileAdapter(Context context,List<String> fileName,List<String> filePath){
            this.mContext = context;
            this.mFileNameList = fileName;
            this.mFilePathList = filePath;
            //初始化图片资源
            //返回到根目录
            this.mBackRoot = getBitmap(mContext,R.drawable.back_to_root);
            //返回上一级
            this.mBackUp = getBitmap(mContext,R.drawable.back_to_up);
            //图片文件
            this.mImage = getBitmap(mContext,R.drawable.image);
            //音频文件
            this.mAudio = getBitmap(mContext,R.drawable.audio);
            //亚索文件
            this.mRar = getBitmap(mContext,R.drawable.rar);
            //视频文件
            this.mVideo = getBitmap(mContext,R.drawable.video);
            //文件夹
            this.mFolder = getBitmap(mContext,R.drawable.folder);
            //apk
            this.mApk = getBitmap(mContext,R.drawable.apk);
            //其他文件
            this.mOthers = getBitmap(mContext,R.drawable.others);
            //文本文件
            this.mTxt = getBitmap(mContext,R.drawable.txt);
            //网页
            this.mWeb = getBitmap(mContext,R.drawable.web);
        }
        @Override
        //获得文件总数
        public int getCount() {
            return mFilePathList.size();
        }

        @Override
        //获得当前位置对应的文件名
        public Object getItem(int position) {
            return mFileNameList.get(position);
        }

        @Override
        //获取当前位置
        public long getItemId(int position) {
            return position;
        }

        @Override
        //获得视图
        public View getView(int position, View convertView, ViewGroup viewGroup) {
            myViewHolder viewHolder;
            if(convertView == null){
                viewHolder = new myViewHolder();
                LayoutInflater mLI = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                //初始化列表页面界面
                convertView = mLI.inflate(R.layout.item_list,null);
                //获取列表布局页面
                viewHolder.mIV = (ImageView) convertView.findViewById(R.id.item_image_2);
                viewHolder.mTV = (TextView) convertView.findViewById(R.id.item_text_2);
                //将每一行的元素集合设置标签
                convertView.setTag(viewHolder);
            }else{
                //获取视图标签
                viewHolder = (myViewHolder) convertView.getTag();
            }
            File mFile = new File (mFilePathList.get(position));
            //如果是返回根目录
            switch (mFileNameList.get(position)) {
                case "BacktoRoot":
                    //添加返回根目录的按钮
                    viewHolder.mIV.setImageBitmap(mBackRoot);
                    viewHolder.mTV.setText("返回根目录");
                    break;
                case "BacktoUp":
                    //添加返回上一级菜单的按钮
                    viewHolder.mIV.setImageBitmap(mBackUp);
                    viewHolder.mTV.setText("返回上一级");
                    break;
                case "BacktoSearchBefore":
                    viewHolder.mIV.setImageBitmap(mBackRoot);
                    viewHolder.mTV.setText("返回搜索前的目录");
                    break;
                default:
                    String fileName = mFile.getName();
                    viewHolder.mTV.setText(fileName);
                    if (mFile.isDirectory()) {
                        viewHolder.mIV.setImageBitmap(mFolder);
                    } else {
                        String fileEnds = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                        Log.i("fileEnds",fileEnds);
                        switch (fileEnds) {
                            case "m4a":
                            case "mp3":
                            case "mid":
                            case "xmf":
                            case "ogg":
                            case "wav":
                                viewHolder.mIV.setImageBitmap(mVideo);
                                break;
                            case "3gp":
                            case "mp4":
                                viewHolder.mIV.setImageBitmap(mAudio);
                                break;
                            case "jpg":
                            case "jpeg":
                            case "png":
                            case "bmp":
                                viewHolder.mIV.setImageBitmap(mImage);
                                break;
                            case "apk":
                                viewHolder.mIV.setImageBitmap(mApk);
                                break;
                            case "txt":
                                viewHolder.mIV.setImageBitmap(mTxt);
                                break;
                            case "zip":
                            case "rar":
                                viewHolder.mIV.setImageBitmap(mRar);
                                break;
                            case "html":
                                viewHolder.mIV.setImageBitmap(mWeb);
                                break;
                            default:
                                viewHolder.mIV.setImageBitmap(mOthers);
                                break;
                        }
                    }
                    break;
            }
            return convertView;
        }
        static class myViewHolder{
            ImageView mIV;
            TextView mTV;
        }
    }
    /**
     * 返回位图
     */
    private static Bitmap getBitmap(Context context, int vectorDrawableId) {
        Bitmap bitmap = null;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            Drawable vectorDrawable = context.getDrawable(vectorDrawableId);
            bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                    vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            vectorDrawable.draw(canvas);
        } else {
            bitmap = BitmapFactory.decodeResource(context.getResources(), vectorDrawableId);
        }
        return bitmap;
    }
    /**
     *列表项单击
     */
    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id){
        final File mFile = new File(mFilePath.get(position));
        //如果文件可读
        if(mFile.canRead()){
            if(mFile.isDirectory()){
                //如果是文件夹，直接进入文件
                initFileListInfo(mFilePath.get(position));
            }else{
                //如果是文件，用相应的方法打开
                String fileName = mFile.getName();
                String fileEnds = fileName.substring(fileName.lastIndexOf(".")+1).toLowerCase();
                if(fileEnds.equals("txt")){
                    //显示进度条
                    initProgressDialog();
                    new Thread(() -> {
                        //打开文本文件
                        openTxtFile(mFile.getPath());
                    }).start();
                    new Thread(() -> {
                        while(true){
                            if(isTxtDataOk){
                                //关闭进度条
                                mProgressDialog.dismiss();
                                executeIntent(txtData,mFile.getPath());
                                break;
                            }
                            if(isCancelProgressDialog){
                                mProgressDialog.dismiss();
                                break;
                            }
                        }
                    }).start();
                }else{
                    openFile(mFile);
                }
            }
        }else{
            //如果文件不可读
            Toast.makeText(MainActivity.this,"文件权限不足！",Toast.LENGTH_SHORT).show();
        }
    }
    //进度条
    ProgressDialog mProgressDialog;
    boolean isCancelProgressDialog = false;
    /**
     * 弹出一个进度条
     */
    private void initProgressDialog(){
        isCancelProgressDialog = false;
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("提示");
        mProgressDialog.setMessage("正在处理。。。");
        mProgressDialog.setCancelable(true);
        mProgressDialog.setButton("取消", (dialogInterface, i) -> {
            isCancelProgressDialog = true;
            mProgressDialog.dismiss();
        });
        mProgressDialog.show();
    }

    /**
     * 调用系统的方法，来打开文件
     */
    private void openFile(File file){
        if(file.isDirectory()){
            initFileListInfo(file.getPath());
        }else{
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(Intent.ACTION_VIEW);
            //设置当前的文件类型
            intent.setDataAndType(Uri.fromFile(file),getMIMEType(file));
            startActivity(intent);
        }
    }
    /**
     * 获得MIME类型的方法
     */
    private String getMIMEType(File file){
        String type;
        String fileName = file.getName();
        //取出文件名后缀
        String fileEnds = fileName.substring(fileName.lastIndexOf(".")+1).toLowerCase();
        switch (fileEnds) {
            case "m4a":
            case "mp3":
            case "mid":
            case "xmf":
            case "ogg":
            case "wav":
                type = "audio/*";
                break;
            case "3gp":
            case "mp4":
                type = "video/*";
                break;
            case "jpg":
            case "jpeg":
            case "bmp":
                type = "image/*";
                break;
            default:
                type = "*/*";
                break;
        }
        return type;
    }
    /**
     * 打开文本文件
     */
    String txtData = "";
    boolean isTxtDataOk = false;
    private void openTxtFile(String file){
        isTxtDataOk = false;
        try{
            FileInputStream fis = new FileInputStream(file);
            StringBuilder mSb = new StringBuilder();
            int m;
            //读取文本文件内容
            while ((m = fis.read()) != -1){
                mSb.append((char) m);
            }
            fis.close();
            //保存数据
            txtData = mSb.toString();
            //读取完毕
            isTxtDataOk = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //跳转页面
    private void executeIntent(String data,String file){
        Intent intent = new Intent(MainActivity.this,EditTxtActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //传递路径、标题和内容
        intent.putExtra("path",file);
        intent.putExtra("title",new File(file).getName());
        intent.putExtra("data", data);
        //跳转
        startActivity(intent);
    }
}
