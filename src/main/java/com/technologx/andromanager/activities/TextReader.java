/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.technologx.andromanager.activities;


import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.technologx.andromanager.R;
import com.technologx.andromanager.activities.BaseActivity;
import com.technologx.andromanager.activities.MainActivity;
import com.technologx.andromanager.exceptions.RootNotPermittedException;
import com.technologx.andromanager.filesystem.HFile;
import com.technologx.andromanager.filesystem.RootHelper;
import com.technologx.andromanager.services.asynctasks.SearchTextTask;
import com.technologx.andromanager.utils.MapEntry;
import com.technologx.andromanager.utils.OpenMode;
import com.technologx.andromanager.utils.PreferenceUtils;
import com.technologx.andromanager.utils.RootUtils;
import com.technologx.andromanager.utils.color.ColorUsage;
import com.technologx.andromanager.utils.theme.AppTheme;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class TextReader extends BaseActivity implements TextWatcher, View.OnClickListener {
    String path;
    Context c = this;
    public EditText mInput, searchEditText;
    private java.io.File mFile;
    private String mOriginal;
    private Timer mTimer;
    private boolean mModified, isEditAllowed = true;
    private android.support.v7.widget.Toolbar toolbar;
    //ArrayList<StringBuilder> texts;
    //static final int maxlength=200;
    //int index=0;
    ScrollView scrollView;

    /*
     * List maintaining the searched text's start/end index as key/value pair
     */
    public ArrayList<MapEntry> nodes = new ArrayList<>();

    /*
     * variable to maintain the position of index
     * while pressing next/previous button in the searchBox
     */
    private int mCurrent = -1;

    /*
     * variable to maintain line number of the searched phrase
     * further used to calculate the scroll position
     */
    public int mLine = 0;

    private SearchTextTask searchTextTask;
    private static final String KEY_MODIFIED_TEXT = "modified";
    private static final String KEY_INDEX = "index";
    private static final String KEY_ORIGINAL_TEXT = "original";

    private RelativeLayout searchViewLayout;

    Uri uri=null;
    public ImageButton upButton, downButton, closeButton;
    boolean delete=false;
    private File pathFile;
    // input stream associated with the file
    private InputStream inputStream;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Sp = PreferenceManager.getDefaultSharedPreferences(this);

        if (getAppTheme().equals(AppTheme.DARK)) {
            getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.holo_dark_background));
        }
        setContentView(R.layout.search);
        searchViewLayout = (RelativeLayout) findViewById(R.id.searchview);
        toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //findViewById(R.id.lin).setBackgroundColor(Color.parseColor(skin));
        toolbar.setBackgroundColor(getColorPreference().getColor(ColorUsage.getPrimary(MainActivity.currentTab)));
        searchViewLayout.setBackgroundColor(getColorPreference().getColor(ColorUsage.getPrimary(MainActivity.currentTab)));
        if (Build.VERSION.SDK_INT >=Build.VERSION_CODES.LOLLIPOP) {
            ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription("AndroManager",
                    ((BitmapDrawable) getResources().getDrawable(R.mipmap.ic_launcher)).getBitmap(),
                    getColorPreference().getColor(ColorUsage.getPrimary(MainActivity.currentTab)));
            ((Activity) this).setTaskDescription(taskDescription);
        }

        searchEditText = (EditText) searchViewLayout.findViewById(R.id.search_box);
        upButton = (ImageButton) searchViewLayout.findViewById(R.id.prev);
        downButton = (ImageButton) searchViewLayout.findViewById(R.id.next);
        closeButton = (ImageButton) searchViewLayout.findViewById(R.id.close);

        searchEditText.addTextChangedListener(this);

        upButton.setOnClickListener(this);
        //upButton.setEnabled(false);
        downButton.setOnClickListener(this);
        //downButton.setEnabled(false);
        closeButton.setOnClickListener(this);

        getSupportActionBar().setBackgroundDrawable(getColorPreference().getDrawable(ColorUsage.getPrimary(MainActivity.currentTab)));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        int sdk = Build.VERSION.SDK_INT;

        if (sdk == Build.VERSION_CODES.KITKAT_WATCH || sdk == Build.VERSION_CODES.KITKAT) {
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(getColorPreference().getColor(ColorUsage.getPrimary(MainActivity.currentTab)));
            FrameLayout.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) findViewById(R.id.texteditor).getLayoutParams();
            SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
            p.setMargins(0, config.getStatusBarHeight(), 0, 0);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            boolean colourednavigation = Sp.getBoolean("colorednavigation", true);
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(PreferenceUtils.getStatusColor(getColorPreference().getColorAsString(ColorUsage.getPrimary(MainActivity.currentTab))));
            if (colourednavigation)
                window.setNavigationBarColor(PreferenceUtils.getStatusColor(getColorPreference().getColorAsString(ColorUsage.getPrimary(MainActivity.currentTab))));

        }
        mInput = (EditText) findViewById(R.id.fname);
        scrollView=(ScrollView)findViewById(R.id.editscroll);

        try {
            if (getIntent().getData() != null){
                uri=getIntent().getData();

                mFile = new File(getIntent().getData().getPath());
            }
            else
                mFile = new File(getIntent().getStringExtra("path"));
            Log.d(getClass().getSimpleName(), mFile.getPath());
        } catch (Exception e) {
            mFile = null;
        }
        String fileName=null;
        try {
            if (uri.getScheme().equals("file")) {
                fileName = uri.getLastPathSegment();
            } else {
                Cursor cursor = null;
                try {
                    cursor = getContentResolver().query(uri, new String[]{
                            MediaStore.Images.ImageColumns.DISPLAY_NAME
                    }, null, null, null);

                    if (cursor != null && cursor.moveToFirst()) {
                        fileName = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME));
                    }
                } finally {

                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(fileName==null || fileName.trim().length()==0)fileName=f.getName();
        getSupportActionBar().setTitle(fileName);
        mInput.addTextChangedListener(this);
        try {
            if (getAppTheme().equals(AppTheme.DARK))
                mInput.setBackgroundColor(getResources().getColor(R.color.holo_dark_background));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (Exception e) {

        }

        if (savedInstanceState!=null) {

            mOriginal = savedInstanceState.getString(KEY_ORIGINAL_TEXT);
            int index = savedInstanceState.getInt(KEY_INDEX);
            mInput.setText(savedInstanceState.getString(KEY_MODIFIED_TEXT));
            mInput.setScrollY(index);
        } else {

            load(mFile);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_MODIFIED_TEXT, mInput.getText().toString());
        outState.putInt(KEY_INDEX, mInput.getScrollY());
        outState.putString(KEY_ORIGINAL_TEXT, mOriginal);
    }

    class a extends ScrollView {
        public a(Context context) {
            super(context);
        }

        @Override
        protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
            super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);

        }
    }

    private void checkUnsavedChanges() {
        if (mOriginal != null && mInput.isShown() && !mOriginal.equals(mInput.getText().toString())) {
            new MaterialDialog.Builder(this)
                    .title(R.string.unsavedchanges)
                    .content(R.string.unsavedchangesdesc)
                    .positiveText(R.string.yes)
                    .negativeText(R.string.no)
                    .positiveColor(Color.parseColor(accentSkin))
                    .negativeColor(Color.parseColor(accentSkin))
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            writeTextFile(mFile.getPath(), mInput.getText().toString());
                            finish();
                        }

                        @Override
                        public void onNegative(MaterialDialog dialog) {
                            finish();
                        }
                    })
                    .build().show();
        } else {
            finish();
        }
    }


    File f;

    public void writeTextFile(String fileName, String s) {
        f = new File(fileName);
        mOriginal = s;
        final String s1 = s;
        if (!mFile.canWrite()) {
            f = new File(this.getFilesDir() + "/" + f.getName());
        }
        Toast.makeText(c, R.string.saving, Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                FileWriter output = null;
                try {
                    output = new FileWriter(f.getPath());
                    BufferedWriter writer = new BufferedWriter(output);
                    writer.write(s1);
                    writer.close();
                    output.close();
                } catch (IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(c, R.string.error, Toast.LENGTH_SHORT).show();
                        }
                    });
                    e.printStackTrace();


                }
                if (!mFile.canWrite())

                {
                    try {
                        RootUtils.mountOwnerRW(mFile.getParent());
                        RootHelper.runShellCommand("cat " + f.getPath() + " > " + mFile.getPath());
                    } catch (RootNotPermittedException e) {
                        e.printStackTrace();
                        // don't have root
                    }
                    f.delete();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Toast.makeText(c, "Done", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }


    private void setProgress(boolean show) {
        //mInput.setVisibility(show ? View.GONE : View.VISIBLE);
        //   findViewById(R.id.progress).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void load(final File mFile) {
        setProgress(true);
        this.mFile = mFile;
        mInput.setHint(R.string.loading);
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    inputStream=getInputStream(uri,path);
                    if (inputStream!=null) {
                        String str=null;
                        //if(texts==null)texts=new ArrayList<>();
                        StringBuilder stringBuilder=new StringBuilder();
                        BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(inputStream));
                        if(bufferedReader!=null){
                            //   int i=0,k=0;
                            //     StringBuilder stringBuilder1=new StringBuilder("");
                            while ((str=bufferedReader.readLine())!=null){
                                stringBuilder.append(str+"\n");
                         /*       if(k<maxlength){
                                    stringBuilder1.append(str+"\n");
                                    k++;
                                }else {
                                    texts.add(i,stringBuilder1);
                                    i++;
                                    stringBuilder1=new StringBuilder("");
                                    stringBuilder1.append(str+"\n");
                                    k=1;
                                }
                        */    }
                            //  texts.add(i,stringBuilder1);
                        }
                        mOriginal=stringBuilder.toString();
                        inputStream.close();
                    } else {
                        mOriginal = "";
                        StringBuilder stringBuilder=new StringBuilder();
                        List<String> arrayList = RootHelper.runNonRootShellCommand("cat " + mFile.getPath());
                        //  int i=0,k=0;
                        //StringBuilder stringBuilder1=new StringBuilder("");
                        for (String str:arrayList){
                            stringBuilder.append(str+"\n");
                        /*    if(k<maxlength){
                                stringBuilder1.append(str+"\n");
                                k++;
                            }else {
                                texts.add(i,stringBuilder1);
                                i++;
                                stringBuilder1=new StringBuilder("");
                                stringBuilder1.append(str+"\n");
                                k=1;
                            }
                        */}
                        // texts.add(i,stringBuilder1);
                        mOriginal=stringBuilder.toString();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mInput.setText(mOriginal);
                                if (mOriginal.isEmpty()) {

                                    mInput.setHint(R.string.file_empty);
                                } else
                                    mInput.setHint(null);
                            } catch (OutOfMemoryError e) {
                                mInput.setHint(R.string.error);
                            }
                            setProgress(false);
                        }
                    });
                } catch (final Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            mInput.setHint(R.string.error);
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        checkUnsavedChanges();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.text, menu);
        menu.findItem(R.id.save).setVisible(mModified);
        menu.findItem(R.id.find).setVisible(true);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                checkUnsavedChanges();
                break;
            case R.id.save:
                // Make sure EditText is visible before saving!
                writeTextFile(mFile.getPath(), mInput.getText().toString());
                break;
            case R.id.details:
                if(mFile.canRead()){
                    HFile hFile=new HFile(OpenMode.FILE,mFile.getPath());
                    hFile.generateMode(this);
                    getFutils().showProps(hFile, this, getAppTheme());
                }else Toast.makeText(this,R.string.not_allowed,Toast.LENGTH_SHORT).show();
                break;
            case R.id.openwith:
                if(mFile.canRead()){
                    getFutils().openunknown(mFile, c, false);
                }else Toast.makeText(this,R.string.not_allowed,Toast.LENGTH_SHORT).show();
                break;
            case R.id.find:
                if (searchViewLayout.isShown()) hideSearchView();
                else revealSearchView();
                break;
            default:
                return false;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // delete the file copied to cache dir
        if(delete)pathFile.delete();
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

        // condition to check if callback is called in search editText
        if (searchEditText != null && charSequence.hashCode() == searchEditText.getText().hashCode()) {

            // clearing before adding new values
            if (searchTextTask!=null) searchTextTask.cancel(true);

            cleanSpans();
        }
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        if (charSequence.hashCode() == mInput.getText().hashCode()) {
            if (mTimer != null) {
                mTimer.cancel();
                mTimer.purge();
                mTimer = null;
            }
            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    mModified = !mInput.getText().toString().equals(mOriginal);
                    invalidateOptionsMenu();
                }
            }, 250);
        }
    }

    @Override
    public void afterTextChanged(Editable editable) {

        // searchBox callback block
        if (searchEditText != null && editable.hashCode() == searchEditText.getText().hashCode()) {

            searchTextTask = new SearchTextTask(this);
            searchTextTask.execute(editable);

        }
    }

    InputStream getInputStream(Uri uri,String path){
        InputStream stream=null;
        try {
            stream=getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            stream=null;
        }
        if(stream==null)

            if(mFile.canRead()){
                try {
                    stream=new FileInputStream(path);
                } catch (FileNotFoundException e) {
                    stream=null;
                }
            } else {
                // try loading stream associated using root and cache dir
                File file1=getExternalCacheDir();
                if(file1==null)file1=getCacheDir();

                if (BaseActivity.rootMode) {

                    try {
                        RootUtils.copy(path,new File(file1.getPath(), mFile.getName()).getPath());
                        pathFile=new File(file1.getPath(),mFile.getName());
                        RootUtils.mountOwnerRW(pathFile.getPath());
                        try {
                            stream = new FileInputStream(pathFile);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            stream = null;
                        }
                    } catch (RootNotPermittedException e) {
                        e.printStackTrace();
                    }
                    delete=true;
                }
            }

        return stream;
    }

    /**
     * show search view with a circular reveal animation
     */
    void revealSearchView() {

        int startRadius = 4;
        int endRadius = Math.max(searchViewLayout.getWidth(), searchViewLayout.getHeight());

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        // hardcoded and completely random
        int cx = metrics.widthPixels - 160;
        int cy = toolbar.getBottom();
        Animator animator;
        // FIXME: 2016/11/18   ViewAnimationUtils Compatibility
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
            animator = ViewAnimationUtils.createCircularReveal(searchViewLayout, cx, cy,
                    startRadius, endRadius);
        }else {
            animator = new ObjectAnimator().ofFloat(searchViewLayout, "alpha", 0f, 1f);
        }
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setDuration(600);
        searchViewLayout.setVisibility(View.VISIBLE);
        searchEditText.setText("");
        animator.start();
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {

                searchEditText.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

    }

    /**
     * hide search view with a circular reveal animation
     */
    void hideSearchView() {

        int endRadius = 4;
        int startRadius = Math.max(searchViewLayout.getWidth(), searchViewLayout.getHeight());

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        // hardcoded and completely random
        int cx = metrics.widthPixels - 160;
        int cy = toolbar.getBottom();

        Animator animator;
        // FIXME: 2016/11/18   ViewAnimationUtils Compatibility
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
            animator = ViewAnimationUtils.createCircularReveal(searchViewLayout, cx, cy,
                    startRadius, endRadius);
        }else {
            animator = new ObjectAnimator().ofFloat(searchViewLayout, "alpha", 0f, 1f);
        }

        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setDuration(600);
        animator.start();
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {

                searchViewLayout.setVisibility(View.GONE);
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(searchEditText.getWindowToken(),
                        InputMethodManager.HIDE_IMPLICIT_ONLY);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.prev:
                // upButton
                if (mCurrent>0) {

                    // setting older span back before setting new one
                    Map.Entry keyValueOld = (Map.Entry) nodes.get(mCurrent).getKey();
                    mInput.getText().setSpan(getAppTheme().equals(AppTheme.LIGHT) ? new BackgroundColorSpan(Color.YELLOW) :
                                    new BackgroundColorSpan(Color.LTGRAY),
                            (Integer) keyValueOld.getKey(),
                            (Integer) keyValueOld.getValue(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

                    // highlighting previous element in list
                    Map.Entry keyValueNew = (Map.Entry) nodes.get(--mCurrent).getKey();
                    mInput.getText().setSpan(new BackgroundColorSpan(ContextCompat.getColor(this, R.color.search_text_highlight)),
                            (Integer) keyValueNew.getKey(),
                            (Integer) keyValueNew.getValue(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

                    // scrolling to the highlighted element
                    scrollView.scrollTo(0, (Integer) keyValueNew.getValue()
                            + mInput.getLineHeight() + Math.round(mInput.getLineSpacingExtra())
                            - getSupportActionBar().getHeight());
                }
                break;
            case R.id.next:
                // downButton
                if (mCurrent<nodes.size()-1) {

                    // setting older span back before setting new one
                    if (mCurrent!=-1) {

                        Map.Entry keyValueOld = (Map.Entry) nodes.get(mCurrent).getKey();
                        mInput.getText().setSpan(getAppTheme().equals(AppTheme.LIGHT) ? new BackgroundColorSpan(Color.YELLOW) :
                                        new BackgroundColorSpan(Color.LTGRAY),
                                (Integer) keyValueOld.getKey(),
                                (Integer) keyValueOld.getValue(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                    }

                    Map.Entry keyValueNew = (Map.Entry) nodes.get(++mCurrent).getKey();
                    mInput.getText().setSpan(new BackgroundColorSpan(ContextCompat.getColor(this, R.color.search_text_highlight)),
                            (Integer) keyValueNew.getKey(),
                            (Integer) keyValueNew.getValue(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

                    // scrolling to the highlighted element
                    scrollView.scrollTo(0, (Integer) keyValueNew.getValue()
                            + mInput.getLineHeight() + Math.round(mInput.getLineSpacingExtra())
                            - getSupportActionBar().getHeight());
                }
                break;
            case R.id.close:
                // closeButton
                findViewById(R.id.searchview).setVisibility(View.GONE);
                cleanSpans();
                break;
            default:
                return;
        }
    }

    private void cleanSpans() {

        // resetting current highlight and line number
        nodes.clear();
        mCurrent = -1;
        mLine = 0;

        // clearing textView spans
        BackgroundColorSpan[] colorSpans = mInput.getText().getSpans(0,
                mInput.length(), BackgroundColorSpan.class);
        for (BackgroundColorSpan colorSpan : colorSpans) {
            mInput.getText().removeSpan(colorSpan);
        }
    }

    public int getLineNumber() {
        return this.mLine;
    }

    public void setLineNumber(int lineNumber) {
        this.mLine = lineNumber;
    }
}
