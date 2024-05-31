package az.inci.bmslogistic.activity;

import static android.text.TextUtils.isEmpty;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import az.inci.bmslogistic.R;
import az.inci.bmslogistic.model.WaitingDocToShip;

public class WaitingDocListActivity extends ScannerSupportActivity {

    Button refresh;
    Button scanCam;
    EditText driverCodeEdit;
    RecyclerView docListView;
    List<WaitingDocToShip> docList;
    String driverCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_doc_list);

        if(docList == null)
            findViewById(R.id.header).setVisibility(View.GONE);

        refresh = findViewById(R.id.refresh);

        scanCam = findViewById(R.id.scan_cam);
        docListView = findViewById(R.id.doc_list);
        driverCodeEdit = findViewById(R.id.driver_code);

        refresh.setOnClickListener(v -> {
            driverCode = driverCodeEdit.getText().toString();
            if(!isEmpty(driverCode))
            {
                getDocList();
            }
        });

        scanCam.setOnClickListener(v -> barcodeResultLauncher.launch(0));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.waiting_activity_menu, menu);

        MenuItem itemSearch = menu.findItem(R.id.check_doc_status);
        itemSearch.setOnMenuItemClickListener(menuItem -> {
            Intent intent = new Intent(this, CheckDocStatusActivity.class);
            startActivity(intent);
            finish();
            return true;
        });

        MenuItem itemNotConfirmedDocList = menu.findItem(R.id.not_confirmed_doc_list);
        itemNotConfirmedDocList.setOnMenuItemClickListener(menuItem -> {
            Intent intent = new Intent(this, NotConfirmedDocListActivity.class);
            startActivity(intent);
            finish();
            return true;
        });
        return true;
    }

    @Override
    public void onScanComplete(String barcode) {
        driverCodeEdit.setText(barcode);
        driverCode = barcode;
        getDocList();
    }

    private void getDocList()
    {
        showProgressDialog(true);
        new Thread(() -> {
            String url = url("logistics", "waiting-doc-list-to-ship");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("driver-code", driverCode);
            url = addRequestParameters(url, parameters);
            docList = getListData(url, WaitingDocToShip[].class);
            if(docList != null) runOnUiThread(this::publishResult);
        }).start();
    }

    private void publishResult() {
        if (!docList.isEmpty()) {
            findViewById(R.id.header).setVisibility(View.VISIBLE);
            docListView.setLayoutManager(new LinearLayoutManager(this));
            docListView.setAdapter(new DocAdapter(docList));
        }
    }

    private class DocAdapter extends RecyclerView.Adapter<DocAdapter.ViewHolder>
    {
        private final List<WaitingDocToShip> localDocList;
        public DocAdapter(List<WaitingDocToShip> docList) {
            this.localDocList = docList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.waiting_doc_list_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            WaitingDocToShip doc = localDocList.get(position);
            holder.trxNo.setText(doc.getTrxNo());
            holder.trxDate.setText(doc.getTrxDate());
            holder.whsCode.setText(doc.getWhsCode());
            holder.bpCode.setText(doc.getBpCode());
            holder.bpName.setText(doc.getBpName());
            holder.sbeCode.setText(doc.getSbeCode());
            holder.sbeName.setText(doc.getSbeName());
        }

        @Override
        public int getItemCount() {
            return localDocList.size();
        }

        private class ViewHolder extends RecyclerView.ViewHolder
        {
            private final TextView trxNo;
            private final TextView trxDate;
            private final TextView whsCode;
            private final TextView bpCode;
            private final TextView bpName;
            private final TextView sbeCode;
            private final TextView sbeName;

            public ViewHolder(View itemView) {
                super(itemView);
                trxNo = itemView.findViewById(R.id.trx_no);
                trxDate = itemView.findViewById(R.id.trx_date);
                whsCode = itemView.findViewById(R.id.whs_code);
                bpCode = itemView.findViewById(R.id.bp_code);
                bpName = itemView.findViewById(R.id.bp_name);
                sbeCode = itemView.findViewById(R.id.sbe_code);
                sbeName = itemView.findViewById(R.id.sbe_name);
            }
        }
    }
}