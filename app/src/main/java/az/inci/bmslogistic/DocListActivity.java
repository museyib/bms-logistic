package az.inci.bmslogistic;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DocListActivity extends AppBaseActivity implements SearchView.OnQueryTextListener
{

    SearchView searchView;

    Button refresh;
    ListView docListView;

    List<ShipDoc> docList;
    String startDate;
    String endDate;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doc_list);

        if (docList == null)
            findViewById(R.id.header).setVisibility(View.GONE);

        refresh = findViewById(R.id.refresh);
        docListView = findViewById(R.id.doc_list);

        refresh.setOnClickListener(view ->
        {
            View datePicker = LayoutInflater.from(this).inflate(R.layout.date_interval_picker,
                    findViewById(android.R.id.content), false);
            EditText dateFromEdit = datePicker.findViewById(R.id.date_from);
            EditText dateToEdit = datePicker.findViewById(R.id.date_to);
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            if (startDate != null)
                dateFromEdit.setText(startDate);
            else
                dateFromEdit.setText(format.format(new Date()));
            if (endDate != null)
                dateToEdit.setText(endDate);
            else
                dateToEdit.setText(format.format(new Date()));

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(datePicker)
                    .setPositiveButton("OK", (dialogInterface, i) ->
                    {
                        startDate = dateFromEdit.getText().toString();
                        endDate = dateToEdit.getText().toString();
                        getDocList();
                    })
                    .create();

            dialog.show();
        });

        loadFooter();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.search_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.search);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(this);
        searchView.setActivated(true);
        return true;
    }

    @Override
    public void onBackPressed()
    {
        if (!searchView.isIconified())
            searchView.setIconified(true);
        else
            super.onBackPressed();
    }

    private void getDocList()
    {
        showProgressDialog(true);
        new Thread(() ->
        {
            String url = url("logistics", "doc-list");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("start-date", startDate);
            parameters.put("end-date", endDate);
            url = addRequestParameters(url, parameters);
            RestTemplate template = new RestTemplate();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                    .setConnectTimeout(config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());
            try
            {
                docList = Arrays.asList(template.getForObject(url, ShipDoc[].class));
                runOnUiThread(this::publishResult);
            }
            catch (RuntimeException ex)
            {
                ex.printStackTrace();
                runOnUiThread(() ->
                        showMessageDialog(getString(R.string.error),
                                getString(R.string.connection_error),
                                android.R.drawable.ic_dialog_alert)
                );
            }
            finally
            {
                runOnUiThread(() -> showProgressDialog(false));
            }
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
        if (adapter != null)
            adapter.getFilter().filter(s);
        return true;
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
            if (convertView == null)
                convertView = LayoutInflater.from(context).inflate(R.layout.doc_list_item, parent, false);

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
                private DocListActivity activity = (DocListActivity) context;

                @Override
                protected FilterResults performFiltering(CharSequence constraint)
                {
                    FilterResults results = new FilterResults();
                    List<ShipDoc> filteredArrayData = new ArrayList<>();
                    constraint = constraint.toString().toLowerCase();

                    for (ShipDoc doc : activity.docList)
                    {
                        if (doc.getTrxNo().concat(doc.getTrxNo()).concat(doc.getBpName())
                                .concat(doc.getSbeName().concat(doc.getSbeCode()))
                                .concat(doc.getDriverName()).toLowerCase().contains(constraint))
                        {
                            filteredArrayData.add(doc);
                        }
                    }

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