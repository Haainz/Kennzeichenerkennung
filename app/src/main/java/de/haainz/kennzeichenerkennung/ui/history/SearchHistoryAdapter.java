package de.haainz.kennzeichenerkennung.ui.history;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import de.haainz.kennzeichenerkennung.ui.ModernFastScroller;
import com.bumptech.glide.Glide;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.haainz.kennzeichenerkennung.R;

public class SearchHistoryAdapter extends RecyclerView.Adapter<SearchHistoryAdapter.ViewHolder> implements Filterable, ModernFastScroller.SectionIndexer {

    private final List<SearchEntry> historyFull;
    private List<SearchEntry> history;
    private final OnHistoryClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy - HH:mm", Locale.getDefault());
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

    public interface OnHistoryClickListener {
        void onDeleteClick(SearchEntry entry);
        void onItemClick(SearchEntry entry);
    }

    public SearchHistoryAdapter(List<SearchEntry> history, OnHistoryClickListener listener) {
        this.history = history;
        this.historyFull = new ArrayList<>(history);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchEntry entry = history.get(position);
        holder.kuerzel.setText(entry.getKuerzel());
        holder.herleitung.setText(entry.getHerleitung());
        holder.date.setText(dateFormat.format(new Date(entry.getTimestamp())));

        if (entry.getImageUri() != null && !entry.getImageUri().isEmpty()) {
            holder.image.setVisibility(View.VISIBLE);
            
            Object imageSource;
            if (entry.getImageUri().startsWith("/")) {
                imageSource = new File(entry.getImageUri());
            } else {
                imageSource = Uri.parse(entry.getImageUri());
            }

            Glide.with(holder.itemView.getContext())
                    .load(imageSource)
                    .placeholder(R.drawable.camera_pic)
                    .into(holder.image);

            // Text unter Kürzel (Vertikal) rechts neben dem Bild
            holder.textContainer.setOrientation(LinearLayout.VERTICAL);
            ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) holder.textContainer.getLayoutParams();
            lp.startToEnd = holder.image.getId();
            lp.startToStart = ConstraintLayout.LayoutParams.UNSET;
            holder.textContainer.setLayoutParams(lp);
        } else {
            holder.image.setVisibility(View.GONE);
            // Ohne Bild: Kürzel und Herleitung nebeneinander (Horizontal)
            holder.textContainer.setOrientation(LinearLayout.HORIZONTAL);
            ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) holder.textContainer.getLayoutParams();
            lp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            lp.startToEnd = ConstraintLayout.LayoutParams.UNSET;
            lp.setMarginStart(10);
            holder.textContainer.setLayoutParams(lp);

            // Sicherstellen, dass Herleitung Abstand zum Kürzel hat
            LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) holder.herleitung.getLayoutParams();
            llp.setMarginStart((int) (8 * holder.itemView.getContext().getResources().getDisplayMetrics().density));
            holder.herleitung.setLayoutParams(llp);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(entry);
        });

        holder.deleteBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(entry);
            }
        });
    }

    @Override
    public int getItemCount() {
        return history.size();
    }

    public void removeItem(SearchEntry entry) {
        int index = history.indexOf(entry);
        if (index != -1) {
            history.remove(index);
            notifyItemRemoved(index);
        }
        historyFull.remove(entry);
    }

    public void restoreItem(SearchEntry entry, int position) {
        history.add(position, entry);
        historyFull.add(entry); // Note: this might not preserve original order in historyFull exactly but it's okay for now
        notifyItemInserted(position);
    }

    public int getPosition(SearchEntry entry) {
        return history.indexOf(entry);
    }

    @Override
    public Filter getFilter() {
        return historyFilter;
    }

    public List<SearchEntry> getCurrentList() {
        return history;
    }

    private final Filter historyFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<SearchEntry> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(historyFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (SearchEntry item : historyFull) {
                    // Check if the constraint is a date (dd.MM.yyyy)
                    String itemDate = dateFormat.format(new Date(item.getTimestamp())).split(" - ")[0];
                    if (item.getKuerzel().toLowerCase().contains(filterPattern) ||
                        item.getHerleitung().toLowerCase().contains(filterPattern) ||
                        itemDate.equals(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            history.clear();
            if (results.values != null) {
                history.addAll((List<SearchEntry>) results.values);
            }
            notifyDataSetChanged();
        }
    };

    @Override
    public String getSectionText(int position) {
        if (position >= 0 && position < history.size()) {
            return dayFormat.format(new Date(history.get(position).getTimestamp()));
        }
        return "";
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView kuerzel, herleitung, date;
        ImageButton deleteBtn;
        LinearLayout textContainer;

        ViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.history_image);
            kuerzel = itemView.findViewById(R.id.history_kuerzel);
            herleitung = itemView.findViewById(R.id.history_herleitung);
            date = itemView.findViewById(R.id.history_date);
            deleteBtn = itemView.findViewById(R.id.history_delete);
            textContainer = itemView.findViewById(R.id.text_container);
        }
    }
}
