package com.mapmyindia.ceinfo.silvassa.ui.activity;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ProgressBar;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapmyindia.ceinfo.silvassa.R;
import com.mapmyindia.ceinfo.silvassa.databinding.LayoutActivitySyncsearchBinding;
import com.mapmyindia.ceinfo.silvassa.provider.property.PropertyCursor;
import com.mapmyindia.ceinfo.silvassa.provider.property.PropertySelection;
import com.mapmyindia.ceinfo.silvassa.provider.zone.ZoneColumns;
import com.mapmyindia.ceinfo.silvassa.provider.zone.ZoneContentValues;
import com.mapmyindia.ceinfo.silvassa.provider.zone.ZoneCursor;
import com.mapmyindia.ceinfo.silvassa.provider.zone.ZoneSelection;
import com.mapmyindia.ceinfo.silvassa.restcontroller.RestApiClient;
import com.mapmyindia.ceinfo.silvassa.restcontroller.RestAppController;
import com.mapmyindia.ceinfo.silvassa.sync.SyncProvider;
import com.mapmyindia.ceinfo.silvassa.utils.Connectivity;
import com.mapmyindia.ceinfo.silvassa.utils.DialogHandler;
import com.mapmyindia.ceinfo.silvassa.utils.INTENT_PARAMETERS;
import com.mapmyindia.ceinfo.silvassa.utils.SharedPrefeHelper;
import com.mapmyindia.ceinfo.silvassa.utils.StringUtils;
import com.mapmyindia.ceinfo.silvassa.wsmodel.ZoneWSModel;
import com.orhanobut.logger.Logger;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by ceinfo on 27-02-2017.
 */

public class SyncSearchActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = SyncSearchActivity.class.getSimpleName();
    private static final int INIT_ZONE_LOADER = 12212;
    ProgressBar progressBar;
    private LayoutActivitySyncsearchBinding binding;
    private SyncSpinnerAdapter spinnerAdapter;

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        binding = DataBindingUtil.setContentView(this, R.layout.layout_activity_syncsearch);

        findViewByIDs();

        if (StringUtils.isNullOrEmpty(SharedPrefeHelper.getZoneId(this))) {
            if (!Connectivity.isConnected(this)) {
                Snackbar.make(getWindow().getDecorView(), R.string.error_network, Snackbar.LENGTH_SHORT).show();
            } else {
                getZone();
            }
        }
    }

    @Override
    public void setTitle(String mTitle) {
        binding.toolbar.setMTitle(mTitle);
    }

    private void findViewByIDs() {

        setToolbar((Toolbar) binding.toolbar.getRoot());

        setTitle("Last Synced: " + SharedPrefeHelper.getLastSync(SyncSearchActivity.this));

        binding.contentLayout.etSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSearch();
            }
        });

        binding.contentLayout.etSyncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Connectivity.isConnected(SyncSearchActivity.this)) {
                    showSnackBar(getWindow().getDecorView(), getString(R.string.error_network));
                } else {
                    doSync();
                }
            }
        });

        spinnerAdapter = new SyncSpinnerAdapter(this, null);

        binding.contentLayout.spinnerRow0.setAdapter(spinnerAdapter);

        binding.contentLayout.spinnerRow0.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                ZoneCursor cursor = ((ZoneCursor) ((SyncSpinnerAdapter) parent.getAdapter()).getCursor());

                if (cursor.moveToFirst())
                    SharedPrefeHelper.setZoneId(SyncSearchActivity.this, cursor.getZoneid());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        populateZoneSpinnerAdapter();
    }

    private void populateZoneSpinnerAdapter() {
        getSupportLoaderManager().initLoader(INIT_ZONE_LOADER, null, new LoaderManager.LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                ZoneSelection selection = new ZoneSelection();
                return selection.getCursorLoader(SyncSearchActivity.this);
            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                ZoneCursor cursor = new ZoneCursor(data);
                spinnerAdapter.changeCursor(cursor);
            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
                spinnerAdapter.swapCursor(null);
            }
        });
    }

    @Override
    public void onClick(View v) {
        Bundle bundle = new Bundle();

        switch (v.getId()) {
            case R.id.spinner_row1:
                bundle.putString(INTENT_PARAMETERS._PREFILL_KEY, INTENT_PARAMETERS._PREFILL_OWNER);
                break;
            case R.id.spinner_row2:
                bundle.putString(INTENT_PARAMETERS._PREFILL_KEY, INTENT_PARAMETERS._PREFILL_OCCUPIER);
                break;
            case R.id.spinner_row3:
                bundle.putString(INTENT_PARAMETERS._PREFILL_KEY, INTENT_PARAMETERS._PREFILL_PROPERTYID);
                break;
            default:
                break;
        }

        PropertySelection selection = new PropertySelection();
        PropertyCursor cursor = selection.query(getContentResolver());

        if (cursor.moveToFirst() && cursor.getCount() > 1) {

            Intent intent = new Intent(SyncSearchActivity.this, PrefillActivity.class);
            intent.putExtras(bundle);
            startActivityForResult(intent, INTENT_PARAMETERS._PREFILL_REQUEST);

        } else {
            new DialogHandler(SyncSearchActivity.this).showAlertDialog("\tPlease Sync Database\n\n\rRequires Network Connectvity!!");
        }
    }

    private void showProgress(boolean show) {
        if (null == progressBar) {
            progressBar = (ProgressBar) findViewById(R.id.progressBar);
        }
        if (show && progressBar.getVisibility() != View.VISIBLE) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void doSearch() {

        boolean isValid = false;

        ZoneCursor zoneCursor = (ZoneCursor) spinnerAdapter.getItem(binding.contentLayout.spinnerRow0.getSelectedItemPosition());

        String zoneId = zoneCursor.moveToFirst() ? zoneCursor.getZoneid() : SharedPrefeHelper.getZoneId(this);

        String owner = binding.contentLayout.spinnerRow1.getText().toString();
        String occupier = binding.contentLayout.spinnerRow2.getText().toString();
        String property_id = binding.contentLayout.spinnerRow3.getText().toString();

        if (!StringUtils.isNullOrEmpty(zoneId))
            isValid = true;

        if (!StringUtils.isNullOrEmpty(owner))
            isValid = true;

        if (!StringUtils.isNullOrEmpty(occupier))
            isValid = true;

        if (!StringUtils.isNullOrEmpty(property_id))
            isValid = true;

        if (isValid) {

            PropertySelection selection = new PropertySelection();
            PropertyCursor cursor = selection.query(getContentResolver());

            if (cursor.moveToFirst() && cursor.getCount() > 1) {
                Intent intent = new Intent(SyncSearchActivity.this, ResultsActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString(INTENT_PARAMETERS._PREFILL_ZONE, SharedPrefeHelper.getZoneId(this));
                bundle.putString(INTENT_PARAMETERS._PREFILL_OCCUPIER, occupier);
                bundle.putString(INTENT_PARAMETERS._PREFILL_OWNER, owner);
                bundle.putString(INTENT_PARAMETERS._PREFILL_PROPERTYID, property_id);
                intent.putExtras(bundle);
                startActivity(intent);
            } else {
                new DialogHandler(SyncSearchActivity.this).showAlertDialog("\tPlease Sync Database\n\n\rRequires Network Connectvity!!");
            }
        } else if (StringUtils.isNullOrEmpty(zoneId)) {
            new DialogHandler(SyncSearchActivity.this).showAlertDialog("Please select \n\n\tZone ID");
        } else {
            new DialogHandler(SyncSearchActivity.this).showAlertDialog("Please select\n\n\t Owner Name, Occupier Name or PropertyID");
        }
    }

    private void doSync() {

        boolean isValid = false;

        ZoneCursor zoneCursor = (ZoneCursor) spinnerAdapter.getItem(binding.contentLayout.spinnerRow0.getSelectedItemPosition());

        String zoneId = zoneCursor.moveToFirst() ? zoneCursor.getZoneid() : SharedPrefeHelper.getZoneId(this);

        if (!StringUtils.isNullOrEmpty(zoneId))
            isValid = true;

        if (isValid) {

            showProgress(true);

            String payload = payload("", "", "", zoneId);

            SyncProvider.getInstance(SyncSearchActivity.this).performSync(new SyncProvider.SyncProviderListener() {
                @Override
                public void onSyncResponse(String msg) {
                    showProgress(false);
                    new DialogHandler(SyncSearchActivity.this).showAlertDialog("Data Synced Successfully");

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    SharedPrefeHelper.setLastSync(SyncSearchActivity.this, sdf.format(new Date()));
                }

                @Override
                public void onSyncError(String msg) {
                    showProgress(false);
                    new DialogHandler(SyncSearchActivity.this).showAlertDialog("Data Synced Failed :Error\n\n\t" + msg);
                }
            }, payload);

        } else {
            new DialogHandler(SyncSearchActivity.this).showAlertDialog("Please select \n\n\tZone ID");
        }

        setTitle("Last Synced: " + SharedPrefeHelper.getLastSync(SyncSearchActivity.this));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case INTENT_PARAMETERS._PREFILL_REQUEST:
                    if (null != data && null != data.getExtras()) {
                        Bundle bundle = data.getExtras();
                        String key = bundle.getString(INTENT_PARAMETERS._PREFILL_KEY);
                        String value = bundle.getString(INTENT_PARAMETERS._PREFILL_RESULT);
                        if (null != key) {
                            if (key.equalsIgnoreCase(INTENT_PARAMETERS._PREFILL_OWNER))
                                binding.contentLayout.spinnerRow1.setText(value);
                            else if (key.equalsIgnoreCase(INTENT_PARAMETERS._PREFILL_OCCUPIER))
                                binding.contentLayout.spinnerRow2.setText(value);
                            else if (key.equalsIgnoreCase(INTENT_PARAMETERS._PREFILL_PROPERTYID))
                                binding.contentLayout.spinnerRow3.setText(value);
                        }
                    }
                    break;

                default:
                    break;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private String payload(String propertId, String occupier, String ownerName, String zoneId) {

        //{"zoneId":"05","ownerName":"","occupierName":"","propertyId":""}
        Payload payload = new Payload();
        payload.setZoneId(zoneId);
        payload.setOwnerName(ownerName);
        payload.setOccupierName(occupier);
        payload.setPropertyId(propertId);

        String toJson = new Gson().toJson(payload, Payload.class);

        Logger.d(TAG, " @payload:toJson : " + toJson);

        return toJson;
    }

    private void getZone() {
        RestApiClient apiClient = RestAppController.getRetrofitinstance().create(RestApiClient.class);

        Call<ResponseBody> call = apiClient.getZone();

        call.enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                showProgress(false);

                if (response.isSuccessful()) {

                    try {

                        JSONObject jsonObject = new JSONObject(response.body().string());

                        if (!jsonObject.getString("message").equalsIgnoreCase("Success")) {
                            Snackbar.make(getWindow().getDecorView(), R.string.error_server, Snackbar.LENGTH_SHORT);
                            return;
                        }

                        if (Integer.parseInt(jsonObject.getString("status")) != 200) {
                            Snackbar.make(getWindow().getDecorView(), R.string.error_server, Snackbar.LENGTH_SHORT);
                            return;
                        }

                        if (null == jsonObject.get("data")) {
                            Snackbar.make(getWindow().getDecorView(), R.string.error_server, Snackbar.LENGTH_SHORT);
                            return;
                        }

                        ArrayList<ZoneWSModel> data = new Gson().fromJson(jsonObject.getString("data"), new TypeToken<ArrayList<ZoneWSModel>>() {
                        }.getType());

                        for (ZoneWSModel zoneWSModel : data) {
                            insertZone(zoneWSModel.getZoneName(), zoneWSModel.getZoneId());
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    Logger.d(TAG, " @getZone : SUCCESS : " + response.body());

                } else {
                    Logger.e(TAG, " @getZone : FAILURE : " + call.request());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                showProgress(false);
                Logger.e(TAG, " @getZone : FAILURE : " + call.request());
            }
        });

        showProgress(true);
    }

    private long insertZone(String zoneName, String zoneId) {
        ZoneContentValues contentValues = new ZoneContentValues();
        contentValues.putZoneid(zoneId);
        contentValues.putZonename(zoneName);
        Uri uri = contentValues.insert(getContentResolver());
        return ContentUris.parseId(uri);
    }

    public class SyncSpinnerAdapter extends CursorAdapter {

        public SyncSpinnerAdapter(Context context, Cursor cursor) {
            super(context, cursor, 0);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.layout_zone_spinner_single_item, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            AppCompatTextView textView = (AppCompatTextView) view.findViewById(R.id.single_item_spinner);
            textView.setText(String.format(Locale.getDefault(), "%s", cursor.getString(cursor.getColumnIndexOrThrow(ZoneColumns.ZONENAME))));
        }

        @Override
        public View newDropDownView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.layout_zone_spinner_single_item, parent, false);
        }
    }

    class Payload {
        String zoneId;
        String ownerName;
        String occupierName;
        String propertyId;

        public String getZoneId() {
            return zoneId;
        }

        public void setZoneId(String zoneId) {
            this.zoneId = zoneId;
        }

        public String getOwnerName() {
            return ownerName;
        }

        public void setOwnerName(String ownerName) {
            this.ownerName = ownerName;
        }

        public String getOccupierName() {
            return occupierName;
        }

        public void setOccupierName(String occupierName) {
            this.occupierName = occupierName;
        }

        public String getPropertyId() {
            return propertyId;
        }

        public void setPropertyId(String propertyId) {
            this.propertyId = propertyId;
        }
    }
}