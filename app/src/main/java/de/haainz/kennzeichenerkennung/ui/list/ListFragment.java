package de.haainz.kennzeichenerkennung.ui.list;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import de.haainz.kennzeichenerkennung.AddCityFragment;
import de.haainz.kennzeichenerkennung.InfosFragment;
import de.haainz.kennzeichenerkennung.Kennzeichen;
import de.haainz.kennzeichenerkennung.Kennzeichen_KI;
import de.haainz.kennzeichenerkennung.KennzeichenlistAdapter;
import de.haainz.kennzeichenerkennung.R;
import de.haainz.kennzeichenerkennung.databinding.FragmentListBinding;

import java.util.ArrayList;

public class ListFragment extends Fragment {
    private FragmentListBinding binding;
    private Kennzeichen_KI kennzeichenKI;
    private ArrayList<Kennzeichen> kennzeichenListe;
    private boolean showNormal = true;
    private boolean showSonder = true;
    private boolean showAuslaufend = true;
    private boolean showEigene = true;
    private boolean showOnlyLiked = false;
    private boolean showOnlyNotLiked = false;
    public ArrayAdapter<Kennzeichen> adapter;
    private boolean selectionMode = false;
    private ArrayList<Kennzeichen> selectedKennzeichen = new ArrayList<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentListBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        kennzeichenKI = new Kennzeichen_KI(getActivity());
        kennzeichenListe = kennzeichenKI.getKennzeichenListe();

        binding.textViewAnzahl.setText("" + kennzeichenListe.size() + " Kennzeichen gefunden");

        binding.scroll.setHorizontalScrollBarEnabled(false);

        setupButtonColors();
        updateList();

        binding.xBtn.setOnClickListener(v -> {
            exitSelectionMode();
        });

        binding.list.setOnItemClickListener((parent, view, position, id) -> {
            Kennzeichen k = (Kennzeichen) parent.getItemAtPosition(position);

            if (selectionMode) {
                toggleSelection(k);
            } else {
                InfosFragment infosFragment = new InfosFragment(k);
                infosFragment.show(getParentFragmentManager(), "InfosFragment");;
            }
        });

        binding.list.setOnItemLongClickListener((parent, view, position, id) -> {
            Kennzeichen k = (Kennzeichen) parent.getItemAtPosition(position);
            if (!selectionMode) {
                enterSelectionMode();
            }
            toggleSelection(k);
            return true;
        });

        binding.heartIcon.setOnClickListener(v -> {
            for (Kennzeichen k : selectedKennzeichen) {
                String newStatus = k.isSaved() ? "nein" : "ja";
                kennzeichenKI.changesavestatus(k, newStatus);
            }
            exitSelectionMode();
            updateList();
        });

        binding.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateList();
                exitSelectionMode();
                binding.swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getContext(), "Liste erfolgreich aktualisiert", Toast.LENGTH_SHORT).show();
            }
        });

        binding.refreshBtn.setOnClickListener(v -> {
            updateList();
            Toast.makeText(getContext(), "Liste erfolgreich aktualisiert", Toast.LENGTH_SHORT).show();
        });

        binding.addBtn.setOnClickListener(v -> {
            AddCityFragment addCityFragment = new AddCityFragment();
            addCityFragment.show(getParentFragmentManager(), "AddCityFragment");
        });

        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Sichtbarkeit von X je nach Inhalt
                if (s.length() == 0) {
                    binding.x.setVisibility(GONE);
                } else {
                    binding.x.setVisibility(VISIBLE);
                }
                updateList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                // Tastatur ausblenden
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(binding.searchInput.getWindowToken(), 0);
                }
                return true;
            }
            return false;
        });

        binding.x.setOnClickListener(v -> {
            binding.searchInput.setText("");
            binding.x.setVisibility(GONE);
            updateList();
        });

        setupFilterButtons();

        return root;
    }

    private void setupButtonColors() {
        binding.buttonNormal.getBackground().setColorFilter(getResources().getColor(R.color.yellow), PorterDuff.Mode.SRC_ATOP);
        binding.buttonSonder.getBackground().setColorFilter(getResources().getColor(R.color.yellow), PorterDuff.Mode.SRC_ATOP);
        binding.buttonAuslaufend.getBackground().setColorFilter(getResources().getColor(R.color.yellow), PorterDuff.Mode.SRC_ATOP);

        binding.buttonAlle.getBackground().setColorFilter(getResources().getColor(R.color.yellow), PorterDuff.Mode.SRC_ATOP);
        binding.buttonEigene.getBackground().setColorFilter(getResources().getColor(R.color.yellow), PorterDuff.Mode.SRC_ATOP);

        binding.buttonLike1.setVisibility(VISIBLE);
        binding.buttonLike2.setVisibility(GONE);
        binding.buttonLike3.setVisibility(GONE);
        binding.x.setVisibility(GONE);
    }

    private void setupFilterButtons() {
        binding.buttonAlle.setOnClickListener(v -> {
            // Filterzustände toggeln, aber Sichtbarkeit bleibt gleich
            boolean allActive = showNormal && showSonder && showAuslaufend;

            // Wenn alle aktiv → alles deaktivieren, sonst alles aktivieren
            showNormal = !allActive;
            showSonder = !allActive;
            showAuslaufend = !allActive;
            showEigene = !allActive;

            int color = allActive ? R.color.white : R.color.yellow;

            // Farben der Buttons entsprechend setzen
            binding.buttonAlle.getBackground().setColorFilter(getResources().getColor(color), PorterDuff.Mode.SRC_ATOP);
            binding.buttonNormal.getBackground().setColorFilter(getResources().getColor(color), PorterDuff.Mode.SRC_ATOP);
            binding.buttonSonder.getBackground().setColorFilter(getResources().getColor(color), PorterDuff.Mode.SRC_ATOP);
            binding.buttonAuslaufend.getBackground().setColorFilter(getResources().getColor(color), PorterDuff.Mode.SRC_ATOP);
            binding.buttonEigene.getBackground().setColorFilter(getResources().getColor(color), PorterDuff.Mode.SRC_ATOP);

            updateList();
        });

        binding.buttonNormal.setOnClickListener(v -> {
            showNormal = !showNormal;
            int color = showNormal ? R.color.yellow : R.color.white;
            binding.buttonNormal.getBackground().setColorFilter(getResources().getColor(color), PorterDuff.Mode.SRC_ATOP);
            updateDeButtonColor();
            updateList();
        });

        binding.buttonSonder.setOnClickListener(v -> {
            showSonder = !showSonder;
            int color = showSonder ? R.color.yellow : R.color.white;
            binding.buttonSonder.getBackground().setColorFilter(getResources().getColor(color), PorterDuff.Mode.SRC_ATOP);
            updateDeButtonColor();
            updateList();
        });

        binding.buttonAuslaufend.setOnClickListener(v -> {
            showAuslaufend = !showAuslaufend;
            int color = showAuslaufend ? R.color.yellow : R.color.white;
            binding.buttonAuslaufend.getBackground().setColorFilter(getResources().getColor(color), PorterDuff.Mode.SRC_ATOP);
            updateDeButtonColor();
            updateList();
        });

        binding.buttonEigene.setOnClickListener(v -> {
            showEigene = !showEigene;
            int color = showEigene ? R.color.yellow : R.color.white;
            binding.buttonEigene.getBackground().setColorFilter(getResources().getColor(color), PorterDuff.Mode.SRC_ATOP);
            updateDeButtonColor();
            updateList();
        });

        binding.buttonLike1.setOnClickListener(v -> {
            binding.buttonLike1.setVisibility(GONE);
            binding.buttonLike2.setVisibility(VISIBLE);
            showOnlyLiked = false;
            showOnlyNotLiked = true;
            updateList();
        });

        binding.buttonLike2.setOnClickListener(v -> {
            binding.buttonLike1.setVisibility(GONE);
            binding.buttonLike2.setVisibility(GONE);
            binding.buttonLike3.setVisibility(VISIBLE);
            showOnlyLiked = true;
            showOnlyNotLiked = false;
            updateList();
        });

        binding.buttonLike3.setOnClickListener(v -> {
            binding.buttonLike1.setVisibility(VISIBLE);
            binding.buttonLike2.setVisibility(GONE);
            binding.buttonLike3.setVisibility(GONE);
            showOnlyLiked = false;
            showOnlyNotLiked = false;
            updateList();
        });
    }

    public void updateList() {
        String searchQuery = binding.searchInput.getText().toString().toLowerCase();
        ArrayList<Kennzeichen> filteredList = new ArrayList<>();
        Kennzeichen_KI kennzeichenKI2 = new Kennzeichen_KI(getActivity());
        kennzeichenListe = kennzeichenKI2.getKennzeichenListe();

        for (Kennzeichen k : kennzeichenListe) {
            boolean matchesType =
                    (showNormal && k.isNormalDE()) ||
                            (showSonder && k.isSonderDE()) ||
                            (showAuslaufend && k.isAuslaufendDE()) ||
                            (showEigene && k.isEigene());

            boolean matchesLikeFilter =
                    (!showOnlyLiked && !showOnlyNotLiked) ||
                            (showOnlyLiked && k.isSaved()) ||
                            (showOnlyNotLiked && !k.isSaved());

            boolean matchesSearch = matchesQuery(k, searchQuery);

            if (matchesType && matchesLikeFilter && matchesSearch) {
                filteredList.add(k);
            }
        }

        adapter = new KennzeichenlistAdapter(getActivity(), filteredList);
        ((KennzeichenlistAdapter) adapter).setSelectedItems(selectedKennzeichen);
        binding.textViewAnzahl.setText(filteredList.size() + " Kennzeichen gefunden");
        binding.list.setAdapter(adapter);
    }

    private boolean matchesQuery(Kennzeichen k, String q) {
        return k.OertskuerzelGeben().toLowerCase().contains(q) ||
                k.OrtGeben().toLowerCase().contains(q) ||
                k.StadtKreisGeben().toLowerCase().contains(q) ||
                (k.BundeslandGeben() != null && k.BundeslandGeben().toLowerCase().contains(q));
    }

    public interface OnConfirmListener {
        void updateList();
    }

    private void updateDeButtonColor() {
        if (showNormal && showSonder && showAuslaufend && showEigene) {
            binding.buttonAlle.getBackground().setColorFilter(getResources().getColor(R.color.yellow), PorterDuff.Mode.SRC_ATOP);
        } else {
            binding.buttonAlle.getBackground().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
        }
    }

    private void enterSelectionMode() {
        selectionMode = true;
        binding.selectionHeader.setVisibility(VISIBLE);
        binding.searchUserLayout.setVisibility(GONE);
        selectedKennzeichen.clear();
        updateSelectionCount();
    }

    private void exitSelectionMode() {
        selectionMode = false;
        selectedKennzeichen.clear();
        binding.selectionHeader.setVisibility(GONE);
        binding.searchUserLayout.setVisibility(VISIBLE);
        updateList(); // um rote Punkte zu entfernen
    }

    private void toggleSelection(Kennzeichen k) {
        if (selectedKennzeichen.contains(k)) {
            selectedKennzeichen.remove(k);
        } else {
            selectedKennzeichen.add(k);
        }

        if (selectedKennzeichen.isEmpty()) {
            exitSelectionMode();
        } else {
            updateSelectionCount();
            adapter.notifyDataSetChanged();
        }
    }

    private void updateSelectionCount() {
        binding.selectionCount.setText(selectedKennzeichen.size() + " ausgewählt");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}