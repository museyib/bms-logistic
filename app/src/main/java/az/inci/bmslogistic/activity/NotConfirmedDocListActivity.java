package az.inci.bmslogistic.activity;

import static android.text.TextUtils.isEmpty;

import static az.inci.bmslogistic.GlobalParameters.cameraScanning;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import az.inci.bmslogistic.R;
import az.inci.bmslogistic.model.ShipDoc;


public class NotConfirmedDocListActivity extends ScannerSupportActivity implements SearchView.OnQueryTextListener
{
    SearchView searchView;
    Button refresh;
    Button scanCam;
    EditText driverCodeEdit;
    ListView docListView;
    ImageButton printBtn;
    List<ShipDoc> docList;
    String startDate;
    String endDate;
    String driverCode;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_not_confirmed_doc_list);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

                if(!searchView.isIconified())
                    searchView.setIconified(true);
                else
                    finish();
            }
        });

        if(docList == null)
            findViewById(R.id.header).setVisibility(View.GONE);

        refresh = findViewById(R.id.refresh);

        scanCam = findViewById(R.id.scan_cam);
        printBtn = findViewById(R.id.print);
        docListView = findViewById(R.id.doc_list);
        driverCodeEdit = findViewById(R.id.driver_code);

        refresh.setOnClickListener(view -> {
            View datePicker = LayoutInflater.from(this)
                    .inflate(R.layout.date_interval_picker,
                            findViewById(android.R.id.content), false);
            EditText dateFromEdit = datePicker.findViewById(R.id.date_from);
            EditText dateToEdit = datePicker.findViewById(R.id.date_to);
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            if(startDate != null)
                dateFromEdit.setText(startDate);
            else
                dateFromEdit.setText(format.format(new Date()));
            if(endDate != null)
                dateToEdit.setText(endDate);
            else
                dateToEdit.setText(format.format(new Date()));

            AlertDialog.Builder dialog = new AlertDialog.Builder(this).setView(datePicker);
            dialog.setPositiveButton("OK", (dialogInterface, i) -> {

                driverCode = driverCodeEdit.getText().toString();
                if(!isEmpty(driverCode))
                {
                    startDate = dateFromEdit.getText().toString();
                    endDate = dateToEdit.getText().toString();
                    getDocList();
                }
            });

            dialog.show();
        });

        printBtn.setOnClickListener(v -> {
            if(docList != null && !docList.isEmpty())
            {
                showProgressDialog(true);
                print(getPrintForm());
            }
        });

        scanCam.setOnClickListener(v -> barcodeResultLauncher.launch(0));

        if(cameraScanning)
            scanCam.setVisibility(View.VISIBLE);
        else
            scanCam.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.not_confirmed_activity_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.search);
        searchView = (SearchView) searchItem.getActionView();
        if (searchView != null) {
            searchView.setOnQueryTextListener(this);
            searchView.setActivated(true);
        }

        MenuItem itemSearch = menu.findItem(R.id.check_doc_status);
        itemSearch.setOnMenuItemClickListener(menuItem -> {
            Intent intent = new Intent(this, CheckDocStatusActivity.class);
            startActivity(intent);
            finish();
            return true;
        });

        MenuItem itemWaitingDocList = menu.findItem(R.id.waiting_doc_list);
        itemWaitingDocList.setOnMenuItemClickListener(menuItem -> {
            Intent intent = new Intent(this, WaitingDocListActivity.class);
            startActivity(intent);
            finish();
            return true;
        });
        return true;
    }

    private void getDocList()
    {
        showProgressDialog(true);
        new Thread(() -> {
            String url = url("logistics", "not-confirmed-doc-list");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("start-date", startDate);
            parameters.put("end-date", endDate);
            parameters.put("driver-code", driverCode);
            url = addRequestParameters(url, parameters);
            docList = getListData(url, "GET", null, ShipDoc[].class);
            if(docList != null) runOnUiThread(this::publishResult);
        }).start();
    }

    private void publishResult()
    {
        findViewById(R.id.header).setVisibility(View.VISIBLE);
        docListView.setAdapter(new DocAdapter(this, R.id.doc_list, docList));
    }

    @Override
    public boolean onQueryTextSubmit(String s)
    {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String s)
    {
        DocAdapter adapter = (DocAdapter) docListView.getAdapter();
        if(adapter != null)
            adapter.getFilter().filter(s);
        return true;
    }

    private void print(String html)
    {
        WebView webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient()
        {
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request)
            {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url)
            {
                showProgressDialog(false);
                createWebPrintJob(view);
            }
        });

        webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null);
    }

    private void createWebPrintJob(WebView webView)
    {
        PrintManager printManager = (PrintManager) this.getSystemService(Context.PRINT_SERVICE);

        String jobName = getString(R.string.app_name) + " Document";

        PrintDocumentAdapter printAdapter;
        PrintAttributes.Builder builder = new PrintAttributes.Builder().setMediaSize(
                PrintAttributes.MediaSize.ISO_A4);

        printAdapter = webView.createPrintDocumentAdapter(jobName);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder.setDuplexMode(PrintAttributes.DUPLEX_MODE_LONG_EDGE);
        }
        printManager.print(jobName, printAdapter, builder.build());
    }

    private String getPrintForm()
    {
        String html = "<html><head><style>*{margin:0px; padding:0px}" +
                "table,tr,th,td{border: 1px solid black;border-collapse: collapse; font-size: 12px}" +
                "th{background-color: #636d72;color:white}td,th{padding:0 4px 0 4px}</style>" +
                "</head><body>";
        html = html.concat("<h3 style='text-align: center'>Təsdiqlənməmiş sənədlər</h3></br>");
        html = html.concat("<table>");
        html = html.concat("<tr><th>Sənəd №.</th>");
        html = html.concat("<th>Tarix</th>");
        html = html.concat("<th>Sürücü</th>");
        html = html.concat("<th>Status</th>");
        html = html.concat("<th>Müştəri kodu</th>");
        html = html.concat("<th>Müştəri adı</th>");
        html = html.concat("<th>Təmsilçi kodu</th>");
        html = html.concat("<th>Təmsilçi adı</th>");
        for(ShipDoc shipDoc : docList)
        {

            html = html.concat("<tr><td>" + shipDoc.getTrxNo() + "</td>");
            html = html.concat("<td nowrap>" + shipDoc.getTrxDate() + "</td>");
            html = html.concat("<td>" + shipDoc.getDriverName() + "</td>");
            html = html.concat("<td>" + shipDoc.getShipStatus() + "</td>");
            html = html.concat("<td>" + shipDoc.getBpCode() + "</td>");
            html = html.concat("<td>" + shipDoc.getBpName() + "</td>");
            html = html.concat("<td>" + shipDoc.getSbeCode() + "</td>");
            html = html.concat("<td>" + shipDoc.getSbeName() + "</td></tr>");
        }

        html = html.concat("</table></body></head>");

        return html;
    }

    @Override
    public void onScanComplete(String barcode)
    {
        driverCodeEdit.setText(barcode);
    }

    private static class DocAdapter extends ArrayAdapter<ShipDoc> implements Filterable
    {
        List<ShipDoc> docList;
        Context context;

        public DocAdapter(@NonNull Context context, int resource, @NonNull List<ShipDoc> objects)
        {
            super(context, resource, objects);
            docList = objects;
            this.context = context;
        }

        @Override
        public int getCount()
        {
            return docList.size();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
        {
            if(convertView == null)
                convertView = LayoutInflater.from(context)
                        .inflate(R.layout.doc_list_item, parent, false);

            ShipDoc doc = docList.get(position);
            TextView docNo = convertView.findViewById(R.id.doc_no);
            TextView trxNo = convertView.findViewById(R.id.trx_no);
            TextView trxDate = convertView.findViewById(R.id.trx_date);
            TextView driverName = convertView.findViewById(R.id.driver_name);
            TextView shipStatus = convertView.findViewById(R.id.ship_status);
            TextView bpCode = convertView.findViewById(R.id.bp_code);
            TextView bpName = convertView.findViewById(R.id.bp_name);
            TextView sbeCode = convertView.findViewById(R.id.sbe_code);
            TextView sbeName = convertView.findViewById(R.id.sbe_name);

            docNo.setText(doc.getDocNo());
            trxNo.setText(doc.getTrxNo());
            trxDate.setText(doc.getTrxDate());
            driverName.setText(doc.getDriverName());
            shipStatus.setText(doc.getShipStatus());
            bpCode.setText(doc.getBpCode());
            bpName.setText(doc.getBpName());
            sbeCode.setText(doc.getSbeCode());
            sbeName.setText(doc.getSbeName());

            return convertView;
        }

        @NonNull
        @Override
        public Filter getFilter()
        {
            return new Filter()
            {
                private final NotConfirmedDocListActivity activity = (NotConfirmedDocListActivity) context;

                @Override
                protected FilterResults performFiltering(CharSequence constraint)
                {
                    FilterResults results = new FilterResults();
                    List<ShipDoc> filteredArrayData = new ArrayList<>();
                    constraint = constraint.toString().toLowerCase();

                    for(ShipDoc doc : activity.docList)
                        if(doc.getTrxNo()
                                .concat(doc.getTrxNo())
                                .concat(doc.getBpName())
                                .concat(doc.getSbeName().concat(doc.getSbeCode()))
                                .concat(doc.getDriverName())
                                .toLowerCase()
                                .contains(constraint))
                            filteredArrayData.add(doc);

                    results.count = filteredArrayData.size();
                    results.values = filteredArrayData;
                    return results;
                }

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence constraint, FilterResults results)
                {
                    docList = (List<ShipDoc>) results.values;
                    notifyDataSetChanged();
                }
            };
        }
    }
}