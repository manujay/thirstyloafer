package com.mapmyindia.ceinfo.silvassa.ui.activity;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mapmyindia.ceinfo.silvassa.R;
import com.mapmyindia.ceinfo.silvassa.provider.payment.PaymentColumns;
import com.mapmyindia.ceinfo.silvassa.provider.payment.PaymentCursor;
import com.mapmyindia.ceinfo.silvassa.provider.payment.PaymentSelection;
import com.mapmyindia.ceinfo.silvassa.provider.property.PropertyColumns;
import com.mapmyindia.ceinfo.silvassa.provider.property.PropertyCursor;
import com.mapmyindia.ceinfo.silvassa.provider.property.PropertySelection;
import com.mapmyindia.ceinfo.silvassa.provider.taxdetail.TaxdetailColumns;
import com.mapmyindia.ceinfo.silvassa.provider.taxdetail.TaxdetailCursor;
import com.mapmyindia.ceinfo.silvassa.provider.taxdetail.TaxdetailSelection;
import com.mapmyindia.ceinfo.silvassa.utils.DateTimeUtils;
import com.mapmyindia.ceinfo.silvassa.utils.INTENT_PARAMETERS;
import com.mapmyindia.ceinfo.silvassa.utils.StringUtils;

import java.util.Locale;

/**
 * Created by ceinfo on 25-03-2017.
 */

public class PrintActivity extends BaseActivity {

    private static final String TAG = PrintActivity.class.getSimpleName();
    private String propertyId;
    private long paymentId = 0;

    @Override
    public void setmTitle(String mTitle) {
        ((TextView) getToolbar().findViewById(R.id.tv_toolbar)).setText(mTitle);
    }

    @Override
    public void onActionCliked() {

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_activity_print_details);

        setToolbar((Toolbar) findViewById(R.id.toolbar));

        setSupportActionBar(getToolbar());

        Bundle extras = getIntent().getExtras();

        if (null != extras && extras.containsKey(INTENT_PARAMETERS._PREFILL_PROPERTYID)) {
            propertyId = extras.getString(INTENT_PARAMETERS._PREFILL_PROPERTYID);
        }

        if (null != extras && extras.containsKey(INTENT_PARAMETERS._PREFILL_PAYMENTID)) {
            paymentId = extras.getLong(INTENT_PARAMETERS._PREFILL_PAYMENTID);
        }

        if (!StringUtils.isNullOrEmpty(propertyId) && paymentId != 0) {
            findViewByIDs();
        } else {
            ((TextView) findViewById(R.id.print_tv)).setTextColor(getResources().getColor(R.color.login_color));
            ((TextView) findViewById(R.id.print_tv)).setText(R.string.error_payment);
        }

        findViewById(R.id.et_print_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PrintActivity.this, WebViewActivity.class);
                Bundle extras = new Bundle();
                extras.putString(INTENT_PARAMETERS._PREFILL_PROPERTYID, propertyId);
                extras.putLong(INTENT_PARAMETERS._PREFILL_PAYMENTID, paymentId);
                intent.putExtras(extras);
                startActivityForIntent(intent);
            }
        });

    }

    private void findViewByIDs() {

        LinearLayout linear_parent = (LinearLayout) findViewById(R.id.print_wv);

        int paddingLeft = (int) getResources().getDimension(R.dimen.activity_horizontal_margin);
        int paddingRight = (int) getResources().getDimension(R.dimen.activity_horizontal_margin);
        int paddingTop = (int) getResources().getDimension(R.dimen.activity_vertical_margin);
        int paddingBottom = (int) getResources().getDimension(R.dimen.activity_vertical_margin);

        linear_parent.setPadding(0, paddingTop, 0, paddingBottom);

        TaxdetailSelection selection = new TaxdetailSelection();
        selection.propertyid(propertyId);

        PropertySelection propertySelection = new PropertySelection();
        propertySelection.propertyuniqueid(propertyId);

        PaymentSelection paymentSelection = new PaymentSelection();
        paymentSelection.id(paymentId);

        TaxdetailCursor taxdetailCursor = selection.query(getContentResolver());

        PaymentCursor paymentCursor = paymentSelection.query(getContentResolver());

        PropertyCursor propertyCursor = propertySelection.query(getContentResolver());

        LinearLayout.LayoutParams tv_params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//        tv_params.leftMargin = paddingLeft;
//        tv_params.rightMargin = paddingRight;
        tv_params.bottomMargin = paddingBottom;

        if (propertyCursor.moveToFirst() && taxdetailCursor.moveToFirst() && paymentCursor.moveToFirst()) {

            TextView textView0 = new TextView(this);
            textView0.setAllCaps(false);
            textView0.setTypeface(Typeface.MONOSPACE);
            textView0.setTextSize(11.0f);
            textView0.setLayoutParams(tv_params);
            String propertyUniqueId = propertyCursor.getStringOrNull(PropertyColumns.PROPERTYUNIQUEID) == null ? "" : propertyCursor.getStringOrNull(PropertyColumns.PROPERTYUNIQUEID);
            textView0.setText(String.format(Locale.getDefault(), "%-22s : %s", "Property Id", propertyUniqueId));

            TextView textView1 = new TextView(this);
            textView1.setAllCaps(false);
            textView1.setTypeface(Typeface.MONOSPACE);
            textView1.setTextSize(11.0f);
            textView1.setLayoutParams(tv_params);
            String propertyOwner = propertyCursor.getStringOrNull(PropertyColumns.PROPERTYOWNER) == null ? "" : propertyCursor.getStringOrNull(PropertyColumns.PROPERTYOWNER);
            textView1.setText(String.format(Locale.getDefault(), "%-22s : %s", "Owner", propertyOwner));

            TextView textView2 = new TextView(this);
            textView2.setAllCaps(false);
            textView2.setTypeface(Typeface.MONOSPACE);
            textView2.setTextSize(11.0f);
            textView2.setLayoutParams(tv_params);
            String dueDate = taxdetailCursor.getStringOrNull(TaxdetailColumns.DUEDATE) == null ? "0" : taxdetailCursor.getStringOrNull(TaxdetailColumns.DUEDATE);
            textView2.setText(String.format(Locale.getDefault(), "%-22s : %s", "Due Date", DateTimeUtils.getFormattedDatefromLong(Long.parseLong(dueDate))));

            TextView textView3 = new TextView(this);
            textView3.setAllCaps(false);
            textView3.setTypeface(Typeface.MONOSPACE);
            textView3.setTextSize(11.0f);
            textView3.setLayoutParams(tv_params);
            String payableAmount = taxdetailCursor.getStringOrNull(TaxdetailColumns.PAYABLEAMOUNT) == null ? "" : taxdetailCursor.getStringOrNull(TaxdetailColumns.PAYABLEAMOUNT);
            textView3.setText(String.format(Locale.getDefault(), "%-22s : %.2f", "Payable Amount (Rs)", Double.parseDouble(payableAmount)));

            TextView textView4 = new TextView(this);
            textView4.setAllCaps(false);
            textView4.setTypeface(Typeface.MONOSPACE);
            textView4.setTextSize(11.0f);
            textView4.setLayoutParams(tv_params);
            String Amount = paymentCursor.getStringOrNull(PaymentColumns.AMOUNT) == null ? "" : paymentCursor.getStringOrNull(PaymentColumns.AMOUNT);
            textView4.setText(String.format(Locale.getDefault(), "%-22s : %.2f", "Amount (Rs)", Double.parseDouble(Amount)));

            TextView textView5 = new TextView(this);
            textView5.setAllCaps(false);
            textView5.setTypeface(Typeface.MONOSPACE);
            textView5.setTextSize(11.0f);
            textView5.setLayoutParams(tv_params);
            String Pdate = paymentCursor.getStringOrNull(PaymentColumns.PDATE) == null ? "" : paymentCursor.getStringOrNull(PaymentColumns.PDATE);
            textView5.setText(String.format(Locale.getDefault(), "%-22s : %s", "Date of Payment", DateTimeUtils.getFormattedDatefromLong(Long.parseLong(Pdate))));

            TextView textView6 = new TextView(this);
            textView6.setAllCaps(false);
            textView6.setTypeface(Typeface.MONOSPACE);
            textView6.setTextSize(11.0f);
            textView6.setLayoutParams(tv_params);
            String Mode = paymentCursor.getStringOrNull(PaymentColumns.MODE) == null ? "" : paymentCursor.getStringOrNull(PaymentColumns.MODE);
            textView6.setText(String.format(Locale.getDefault(), "%-22s : %s", "Mode of Payment", Mode));

            TextView textView7 = new TextView(this);
            textView7.setAllCaps(false);
            textView7.setTypeface(Typeface.MONOSPACE);
            textView7.setTextSize(11.0f);
            textView7.setLayoutParams(tv_params);
            String taxNo = taxdetailCursor.getStringOrNull(TaxdetailColumns.TAXNO) == null ? "" : taxdetailCursor.getStringOrNull(TaxdetailColumns.TAXNO);
            textView7.setText(String.format(Locale.getDefault(), "%-22s : %s", "Tax Number", taxNo));

            TextView textView8 = new TextView(this);
            textView8.setAllCaps(false);
            textView8.setTypeface(Typeface.MONOSPACE);
            textView8.setTextSize(11.0f);
            textView8.setLayoutParams(tv_params);
            String phone = propertyCursor.getStringOrNull(PropertyColumns.PHONE) == null ? "" : propertyCursor.getStringOrNull(PropertyColumns.PHONE);
            textView8.setText(String.format(Locale.getDefault(), "%-22s : %s", "Phone", phone));

            TextView textView9 = new TextView(this);
            textView9.setAllCaps(false);
            textView9.setTypeface(Typeface.MONOSPACE);
            textView9.setTextSize(11.0f);
            textView9.setLayoutParams(tv_params);
            String email = propertyCursor.getStringOrNull(PropertyColumns.EMAIL) == null ? "" : propertyCursor.getStringOrNull(PropertyColumns.EMAIL);
            textView9.setText(String.format(Locale.getDefault(), "%-22s : %s", "Email", email));

            TextView textView10 = new TextView(this);
            textView10.setAllCaps(false);
            textView10.setTypeface(Typeface.MONOSPACE);
            textView10.setTextSize(11.0f);
            textView10.setLayoutParams(tv_params);
            String cheque = paymentCursor.getStringOrNull(PaymentColumns.CHEQUE) == null ? "" : paymentCursor.getStringOrNull(PaymentColumns.CHEQUE);
            textView10.setText(String.format(Locale.getDefault(), "%-22s : %s", "Cheque/DD", cheque));

            linear_parent.addView(textView0);
            linear_parent.addView(textView1);
            linear_parent.addView(textView2);
            linear_parent.addView(textView3);
            linear_parent.addView(textView4);
            linear_parent.addView(textView5);
            linear_parent.addView(textView6);
            linear_parent.addView(textView7);
            linear_parent.addView(textView8);
            linear_parent.addView(textView9);
            linear_parent.addView(textView10);

            paymentCursor.close();
            propertyCursor.close();
            taxdetailCursor.close();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.silvassa_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.home:
                Intent intent = new Intent(this, SyncSearchActivity.class);
                startActivityForIntent(intent);
                finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        return;
    }
}
