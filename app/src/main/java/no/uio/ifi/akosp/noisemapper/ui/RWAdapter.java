package no.uio.ifi.akosp.noisemapper.ui;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import no.uio.ifi.akosp.noisemapper.R;
import no.uio.ifi.akosp.noisemapper.Utils;
import no.uio.ifi.akosp.noisemapper.model.ProcessedRecord;

/**
 * Created on 2017.01.25..
 *
 * @author Ákos Pap
 */
public class RWAdapter extends RecyclerView.Adapter<RWAdapter.RWViewHolder> {

    public static final String TAG = "RWAdapter";

    public static final SimpleDateFormat TIMESTAMP_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    private static final OnSelectionChangedListener DEFAULT_CALLBACK = new OnSelectionChangedListener() {
        @Override
        public void onSelectionChanged(List<ProcessedRecord> selectedPrs) {
            Log.w(TAG, "Callback not registered!");
        }
    };

    private List<RecordWrapper> dataset;
    protected OnSelectionChangedListener callback = DEFAULT_CALLBACK;

    private final RWViewHolder.ItemOnClickListener itemOnClickListener = new RWViewHolder.ItemOnClickListener() {
        @Override
        public void onItemClicked(int position) {
            RecordWrapper item = dataset.get(position);
            item.isSelected = !item.isSelected;
            notifyDataSetChanged();

            List<ProcessedRecord> selection = new ArrayList<>();
            for (RecordWrapper rw : dataset) {
                if (rw.isSelected) {
                    selection.add(rw.record);
                }
            }
            callback.onSelectionChanged(selection);
        }
    };

    private final RWViewHolder.ItemOnLongClickListener itemOnLongClickListener = new RWViewHolder.ItemOnLongClickListener() {
        @Override
        public void onItemLongClicked(int position) {
            RecordWrapper item = dataset.get(position);
            Utils.exportRecording(item.record.getFilename());
        }
    };

    public RWAdapter() {
        this.dataset = new ArrayList<>();
    }

    public void setRecordWrappers(@NonNull List<ProcessedRecord> notes) {
        dataset.clear();

        for (ProcessedRecord pr : notes) {
            dataset.add(new RecordWrapper(pr));
        }
        notifyDataSetChanged();
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.callback = listener;
    }

    @Override
    public RWAdapter.RWViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_record_wrapper, parent, false);
        return new RWViewHolder(view, itemOnClickListener, itemOnLongClickListener);
    }

    @Override
    public void onBindViewHolder(RWAdapter.RWViewHolder holder, int position) {
        RecordWrapper rw = dataset.get(position);
        final ProcessedRecord record = rw.record;
        holder.timestampView.setText(TIMESTAMP_FORMAT.format(record.getTimestamp()));
        holder.selectionIndicatorView.setChecked(rw.isSelected);

        try {
            JSONObject results = new JSONObject(record.getProcessResult());
            holder.dataView.setText(String.format(Locale.US,
                    "max=%.3f avg=%.3f",
                    results.getDouble("max"),
                    results.getDouble("avg")
            ));
        } catch (JSONException e) {
            Log.e(TAG, "Unable to decode process result JSON", e);
            holder.dataView.setText("ERROR");
        }

    }

    @Override
    public int getItemCount() {
        return dataset.size();
    }


    private static class RecordWrapper {
        ProcessedRecord record;
        boolean isSelected = false;

        RecordWrapper(ProcessedRecord pr) {
            this.record = pr;
        }
    }

    static class RWViewHolder extends RecyclerView.ViewHolder {

        TextView timestampView;
        TextView dataView;
        CheckBox selectionIndicatorView;

        RWViewHolder(View itemView, final ItemOnClickListener onClickListener, final ItemOnLongClickListener onLongClickListener) {
            super(itemView);
            timestampView = (TextView) itemView.findViewById(R.id.timestamp);
            dataView = (TextView) itemView.findViewById(R.id.data);
            selectionIndicatorView = (CheckBox) itemView.findViewById(R.id.selectionIndicator);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onClickListener.onItemClicked(getAdapterPosition());
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onLongClickListener.onItemLongClicked(getAdapterPosition());
                    Toast.makeText(v.getContext(), "Exported", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }

        interface ItemOnClickListener {
            void onItemClicked(int position);
        }

        interface ItemOnLongClickListener {
            void onItemLongClicked(int position);
        }
    }

    interface OnSelectionChangedListener {
        void onSelectionChanged(List<ProcessedRecord> selectedPrs);
    }
}
