package com.betomaluje.android.miband;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.betomaluje.android.miband.core.MiBand;
import com.betomaluje.android.miband.core.bluetooth.MiBandWrapper;
import com.betomaluje.android.miband.core.colorpicker.ColorPickerDialog;
import com.betomaluje.android.miband.models.App;
import com.betomaluje.android.miband.sqlite.AppsSQLite;
import com.wdullaer.materialdatetimepicker.time.RadialPickerLayout;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import java.util.Calendar;
import java.util.HashMap;

/**
 * Created by betomaluje on 7/6/15.
 */
public class AppDetailActivity extends AppCompatActivity implements TimePickerDialog.OnTimeSetListener {

    private final String TAG = getClass().getSimpleName();

    public static final String extra = "sent_app";
    public static final String extra_returned = "returned_app";
    public static final String extra_position = "app_position";

    private App selectedApp, savedApp;

    private int position;

    private CheckBox checkBox_notify, checkBox_times;
    private View view_notificationColor, cardView_step2, cardView_step3, linearLayout_times;
    private TextView textView_startTime, textView_endTime;
    private Button button_tryNotification;

    private boolean isStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_detail);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setBackgroundColor(Color.TRANSPARENT);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        selectedApp = getIntent().getExtras().getParcelable(extra);
        position = getIntent().getExtras().getInt(extra_position, 0);

        cloneApp();

        if (selectedApp != null) {
            // Change title
            CollapsingToolbarLayout collapser = (CollapsingToolbarLayout) findViewById(R.id.collapser);
            collapser.setTitle(selectedApp.getName());

            try {
                // Change image
                PackageManager pm = getPackageManager();
                ApplicationInfo applicationInfo = pm.getApplicationInfo(selectedApp.getSource(), PackageManager.GET_META_DATA);

                ImageView image = (ImageView) findViewById(R.id.image_paralax);
                image.setImageDrawable(pm.getApplicationIcon(applicationInfo));

                initViews();

            } catch (PackageManager.NameNotFoundException e) {

            }
        }
    }

    private void cloneApp() {
        savedApp = new App();
        savedApp.setNotify(selectedApp.isNotify());
        savedApp.setColor(selectedApp.getColor());
        savedApp.setStartTime(selectedApp.getStartTime());
        savedApp.setEndTime(selectedApp.getEndTime());
    }

    private void onExit() {
        selectedApp.setNotify(checkBox_notify.isChecked() ? 1 : 0);

        if (!checkBox_times.isChecked()) {
            selectedApp.setStartTime(-1);
            selectedApp.setEndTime(-1);
        } else if ((selectedApp.getStartTime() == -1 && selectedApp.getEndTime() != -1) ||
                (selectedApp.getStartTime() != -1 && selectedApp.getEndTime() == -1)) {
            //if we are missing one time, we reset times
            selectedApp.setStartTime(savedApp.getStartTime());
            selectedApp.setEndTime(savedApp.getEndTime());
        }

        if (somethingChanged()) {
            //display dialog to save changes
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                    AppDetailActivity.this);

            alertDialogBuilder.setTitle("Changes happened");

            alertDialogBuilder
                    .setMessage("Do you want to save the changes?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            AppsSQLite.getInstance(AppDetailActivity.this).updateApp(selectedApp);

                            Intent intent = getIntent();
                            Bundle b = new Bundle();

                            b.putParcelable(extra_returned, selectedApp);
                            b.putInt(extra_position, position);
                            intent.putExtras(b);

                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            finish();
                        }
                    });

            AlertDialog alertDialog = alertDialogBuilder.create();

            alertDialog.show();
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        onExit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {

            onExit();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean somethingChanged() {
        //if the notify section changed
        if (savedApp.isNotify() != selectedApp.isNotify())
            return true;

        //if the color changed
        if (savedApp.getColor() != selectedApp.getColor())
            return true;

        //if the times changed
        if (savedApp.getStartTime() != selectedApp.getStartTime() || savedApp.getEndTime() != selectedApp.getEndTime())
            return true;

        Log.v(TAG, "nothing changed");
        return false;
    }

    private void initViews() {
        button_tryNotification = (Button) findViewById(R.id.button_tryNotification);
        button_tryNotification.setOnClickListener(clickListener);

        checkBox_notify = (CheckBox) findViewById(R.id.checkBox_notify);

        cardView_step2 = findViewById(R.id.cardView_step2);
        cardView_step3 = findViewById(R.id.cardView_step3);

        checkBox_notify.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                toggleViews(isChecked);
            }
        });

        checkBox_notify.setChecked(selectedApp.isNotify());
        toggleViews(checkBox_notify.isChecked());

        view_notificationColor = findViewById(R.id.view_notificationColor);

        view_notificationColor.setBackgroundColor(selectedApp.getColorForView());

        view_notificationColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ColorPickerDialog(AppDetailActivity.this, selectedApp.getColor(), new ColorPickerDialog.OnColorSelectedListener() {
                    @Override
                    public void onColorSelected(int rgb) {
                        selectedApp.setColor(rgb);

                        view_notificationColor.setBackgroundColor(selectedApp.getColorForView());
                    }
                }).show();
            }
        });

        checkBox_times = (CheckBox) findViewById(R.id.checkBox_times);
        textView_startTime = (TextView) findViewById(R.id.textView_startTime);
        textView_endTime = (TextView) findViewById(R.id.textView_endTime);

        linearLayout_times = findViewById(R.id.linearLayout_times);

        checkBox_times.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                toggleTimeViews(isChecked);

                //we reset times
                selectedApp.setStartTime(savedApp.getStartTime());
                selectedApp.setEndTime(savedApp.getEndTime());

                textView_startTime.setText(selectedApp.getStartTime() != -1 ? "Start: " + String.valueOf(selectedApp.getStartTime()) : "Start Time");
                textView_endTime.setText(selectedApp.getStartTime() != -1 ? "End: " + String.valueOf(selectedApp.getStartTime()) : "End Time");
            }
        });

        checkBox_times.setChecked(selectedApp.getStartTime() != -1 && selectedApp.getEndTime() != -1);

        toggleTimeViews(checkBox_times.isChecked());

        textView_startTime.setOnClickListener(clickListener);
        textView_endTime.setOnClickListener(clickListener);

        textView_startTime.setText(selectedApp.getStartTime() != -1 ? "Start: " + String.valueOf(selectedApp.getStartTime()) : "Start Time");
        textView_endTime.setText(selectedApp.getEndTime() != -1 ? "End: " + String.valueOf(selectedApp.getEndTime()) : "End Time");
    }

    private void toggleViews(boolean isChecked) {
        cardView_step2.setVisibility(isChecked ? View.VISIBLE : View.INVISIBLE);
        cardView_step3.setVisibility(isChecked ? View.VISIBLE : View.INVISIBLE);
        button_tryNotification.setVisibility(isChecked ? View.VISIBLE : View.INVISIBLE);
    }

    private void toggleTimeViews(boolean isChecked) {
        if (isChecked) {
            linearLayout_times.setVisibility(View.VISIBLE);
        } else {
            linearLayout_times.setVisibility(View.GONE);
        }
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Calendar now = Calendar.getInstance();

            switch (v.getId()) {
                case R.id.textView_startTime:

                    isStart = true;

                    TimePickerDialog tpdStart = TimePickerDialog.newInstance(
                            AppDetailActivity.this,
                            now.get(Calendar.HOUR_OF_DAY),
                            now.get(Calendar.MINUTE),
                            true
                    );

                    tpdStart.show(getFragmentManager(), "StartTimePicker");

                    break;
                case R.id.textView_endTime:

                    isStart = false;

                    TimePickerDialog tpdEnd = TimePickerDialog.newInstance(
                            AppDetailActivity.this,
                            now.get(Calendar.HOUR_OF_DAY),
                            now.get(Calendar.MINUTE),
                            true
                    );

                    tpdEnd.show(getFragmentManager(), "EndTimePicker");

                    break;
                case R.id.button_tryNotification:

                    if (MiBand.getInstance(AppDetailActivity.this).isConnected()) {
                        HashMap<String, Integer> params = new HashMap<String, Integer>();
                        params.put("color", selectedApp.getColor());
                        params.put("pause_time", selectedApp.getPauseTime());

                        MiBand.sendAction(MiBandWrapper.ACTION_NOTIFY, params);
                    } else {
                        Snackbar.make(findViewById(R.id.coordinator), "Please pair the Mi Band first", Snackbar.LENGTH_SHORT)
                                .show();
                    }
                    break;
            }
        }
    };

    @Override
    public void onTimeSet(RadialPickerLayout radialPickerLayout, int hourOfDay, int minute) {
        Log.v(TAG, "start? " + isStart + " -> time: " + hourOfDay + ":" + minute);

        if (isStart) {
            selectedApp.setStartTime(hourOfDay);
            textView_startTime.setText("Start: " + String.valueOf(hourOfDay));
        } else {
            selectedApp.setEndTime(hourOfDay);
            textView_endTime.setText("End: " + String.valueOf(hourOfDay));
        }
    }
}