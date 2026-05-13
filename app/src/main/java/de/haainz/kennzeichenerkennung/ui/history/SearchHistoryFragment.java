package de.haainz.kennzeichenerkennung.ui.history;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import de.haainz.kennzeichenerkennung.R;
import de.haainz.kennzeichenerkennung.ui.ModernFastScroller;

public class SearchHistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyView;
    private EditText searchInput;
    private ImageButton calendarBtn, deleteVisibleBtn;
    private SearchHistoryAdapter adapter;
    private SearchHistoryManager historyManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_history, container, false);
        recyclerView = view.findViewById(R.id.history_recycler);
        emptyView = view.findViewById(R.id.empty_view);
        searchInput = view.findViewById(R.id.history_search);
        calendarBtn = view.findViewById(R.id.history_calendar);
        deleteVisibleBtn = view.findViewById(R.id.history_delete_all);
        historyManager = new SearchHistoryManager(requireContext());
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadHistory();

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) {
                    adapter.getFilter().filter(s);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        calendarBtn.setOnClickListener(v -> showDatePicker());
        deleteVisibleBtn.setOnClickListener(v -> confirmDeleteVisible());
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            String date = String.format(Locale.getDefault(), "%02d.%02d.%04d", dayOfMonth, month + 1, year);
            searchInput.setText(date);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        
        // Use existing style if possible or ensure rounding
        datePickerDialog.show();
    }

    private void confirmDeleteVisible() {
        if (adapter == null || adapter.getCurrentList().isEmpty()) return;

        int count = adapter.getCurrentList().size();
        View customView = getLayoutInflater().inflate(R.layout.dialog_confirm_delete, null);
        TextView title = customView.findViewById(R.id.dialog_title);
        TextView message = customView.findViewById(R.id.dialog_message);
        Button btnCancel = customView.findViewById(R.id.btn_cancel);
        Button btnDelete = customView.findViewById(R.id.btn_delete);

        title.setText("Löschen bestätigen");
        message.setText("Möchtest du wirklich alle " + count + " aktuell angezeigten Einträge unwiderruflich löschen?");

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.Theme_Kennzeichenerkennung_CompactDialog)
                .setView(customView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnDelete.setOnClickListener(v -> {
            List<SearchEntry> visibleEntries = new ArrayList<>(adapter.getCurrentList());
            for (SearchEntry entry : visibleEntries) {
                historyManager.deleteEntry(entry.getId());
            }
            loadHistory();
            searchInput.setText("");
            dialog.dismiss();
        });

        dialog.show();
    }

    private void loadHistory() {
        List<SearchEntry> history = historyManager.getHistory();
        if (history.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            searchInput.setVisibility(View.GONE);
            calendarBtn.setVisibility(View.GONE);
            deleteVisibleBtn.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            searchInput.setVisibility(View.VISIBLE);
            calendarBtn.setVisibility(View.VISIBLE);
            deleteVisibleBtn.setVisibility(View.VISIBLE);
            adapter = new SearchHistoryAdapter(history, new SearchHistoryAdapter.OnHistoryClickListener() {
                @Override
                public void onDeleteClick(SearchEntry entry) {
                    final int position = adapter.getPosition(entry);
                    adapter.removeItem(entry);

                    Snackbar snackbar = Snackbar.make(recyclerView, "Eintrag gelöscht", Snackbar.LENGTH_LONG)
                            .setAction("Rückgängig", v -> {
                                adapter.restoreItem(entry, position);
                                if (emptyView.getVisibility() == View.VISIBLE) {
                                    emptyView.setVisibility(View.GONE);
                                    recyclerView.setVisibility(View.VISIBLE);
                                }
                            })
                            .addCallback(new Snackbar.Callback() {
                                @Override
                                public void onDismissed(Snackbar transientBottomBar, int event) {
                                    if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                                        historyManager.deleteEntry(entry.getId());
                                        if (adapter.getItemCount() == 0) {
                                            loadHistory();
                                        }
                                    }
                                }
                            });

                    // Styling the Snackbar as a "white bar"
                    snackbar.setBackgroundTint(Color.WHITE);
                    snackbar.setTextColor(Color.BLACK);
                    snackbar.setActionTextColor(ContextCompat.getColor(requireContext(), R.color.blue_500));
                    snackbar.show();

                    if (adapter.getItemCount() == 0) {
                        emptyView.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onItemClick(SearchEntry entry) {
                    if (isAdded() && getView() != null && Navigation.findNavController(getView()).getCurrentDestination() != null) {
                        if (Navigation.findNavController(getView()).getCurrentDestination().getId() == R.id.nav_history) {
                            Bundle bundle = new Bundle();
                            bundle.putString("history_kuerzel", entry.getKuerzel());
                            bundle.putString("history_image_uri", entry.getImageUri());
                            Navigation.findNavController(getView()).navigate(R.id.nav_home, bundle);
                        }
                    }
                }
            });
            recyclerView.setAdapter(adapter);
            ModernFastScroller fastScroller = getView().findViewById(R.id.fast_scroller);
            if (fastScroller != null) {
                fastScroller.setThemeColor(Color.WHITE);
                fastScroller.attachToRecyclerView(recyclerView);
            }
        }
    }
}
