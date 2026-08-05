package de.haainz.kennzeichenerkennung.ui.list;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.getkeepsafe.taptargetview.TapTargetView;

import de.haainz.kennzeichenerkennung.AddCityFragment;
import de.haainz.kennzeichenerkennung.InfosFragment;
import de.haainz.kennzeichenerkennung.Kennzeichen;
import de.haainz.kennzeichenerkennung.Kennzeichen_KI;
import de.haainz.kennzeichenerkennung.KennzeichenlistAdapter;
import de.haainz.kennzeichenerkennung.R;
import de.haainz.kennzeichenerkennung.databinding.FragmentListBinding;
import de.haainz.kennzeichenerkennung.ui.ModernFastScroller;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.nativead.NativeAd;

import java.util.ArrayList;
import java.util.List;

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
    public ArrayAdapter<Object> adapter;
    private boolean selectionMode = false;
    private ArrayList<Kennzeichen> selectedKennzeichen = new ArrayList<>();
    private List<NativeAd> nativeAds = new ArrayList<>();
    private static final String PREF_TOUR_LIST_SHOWN = "tour_list_shown";
    private final android.os.Handler adHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable adRunnable;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentListBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        kennzeichenKI = new Kennzeichen_KI(getActivity());
        kennzeichenListe = kennzeichenKI.getKennzeichenListe();

        binding.textViewAnzahl.setText("" + kennzeichenListe.size() + " Kennzeichen gefunden");

        binding.scroll.setHorizontalScrollBarEnabled(false);

        setupButtonColors();
        
        // MobileAds nur einmal initialisieren
        MobileAds.initialize(requireContext(), initializationStatus -> {});

        updateList();

        getParentFragmentManager().setFragmentResultListener("history_update", getViewLifecycleOwner(), (requestKey, result) -> {
            updateList();
        });

        SharedPreferences prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        boolean tourShown = prefs.getBoolean(PREF_TOUR_LIST_SHOWN, false);

        if (!tourShown) {
            binding.list.post(() -> showTour());
            prefs.edit().putBoolean(PREF_TOUR_LIST_SHOWN, true).apply();
        }

        binding.xBtn.setOnClickListener(v -> {
            exitSelectionMode();
        });

        binding.list.setOnItemClickListener((parent, view, position, id) -> {
            Object item = parent.getItemAtPosition(position);
            if (!(item instanceof Kennzeichen)) return;
            
            Kennzeichen k = (Kennzeichen) item;

            if (selectionMode) {
                toggleSelection(k);
            } else {
                InfosFragment infosFragment = new InfosFragment(k);
                infosFragment.show(getParentFragmentManager(), "InfosFragment");;
            }
        });

        binding.list.setOnItemLongClickListener((parent, view, position, id) -> {
            Object item = parent.getItemAtPosition(position);
            if (!(item instanceof Kennzeichen)) return true;
            
            Kennzeichen k = (Kennzeichen) item;
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
            AddCityFragment addCityFragment = new AddCityFragment(null);
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
        DrawableCompat.setTint(binding.buttonNormal.getBackground(), ContextCompat.getColor(requireContext(), R.color.yellow));
        DrawableCompat.setTint(binding.buttonSonder.getBackground(), ContextCompat.getColor(requireContext(), R.color.yellow));
        DrawableCompat.setTint(binding.buttonAuslaufend.getBackground(), ContextCompat.getColor(requireContext(), R.color.yellow));

        DrawableCompat.setTint(binding.buttonAlle.getBackground(), ContextCompat.getColor(requireContext(), R.color.yellow));
        DrawableCompat.setTint(binding.buttonEigene.getBackground(), ContextCompat.getColor(requireContext(), R.color.yellow));

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
            DrawableCompat.setTint(binding.buttonAlle.getBackground(), ContextCompat.getColor(requireContext(), color));
            DrawableCompat.setTint(binding.buttonNormal.getBackground(), ContextCompat.getColor(requireContext(), color));
            DrawableCompat.setTint(binding.buttonSonder.getBackground(), ContextCompat.getColor(requireContext(), color));
            DrawableCompat.setTint(binding.buttonAuslaufend.getBackground(), ContextCompat.getColor(requireContext(), color));
            DrawableCompat.setTint(binding.buttonEigene.getBackground(), ContextCompat.getColor(requireContext(), color));

            updateList();
        });

        binding.buttonNormal.setOnClickListener(v -> {
            showNormal = !showNormal;
            int color = showNormal ? R.color.yellow : R.color.white;
            DrawableCompat.setTint(binding.buttonNormal.getBackground(), ContextCompat.getColor(requireContext(), color));
            updateDeButtonColor();
            updateList();
        });

        binding.buttonSonder.setOnClickListener(v -> {
            showSonder = !showSonder;
            int color = showSonder ? R.color.yellow : R.color.white;
            DrawableCompat.setTint(binding.buttonSonder.getBackground(), ContextCompat.getColor(requireContext(), color));
            updateDeButtonColor();
            updateList();
        });

        binding.buttonAuslaufend.setOnClickListener(v -> {
            showAuslaufend = !showAuslaufend;
            int color = showAuslaufend ? R.color.yellow : R.color.white;
            DrawableCompat.setTint(binding.buttonAuslaufend.getBackground(), ContextCompat.getColor(requireContext(), color));
            updateDeButtonColor();
            updateList();
        });

        binding.buttonEigene.setOnClickListener(v -> {
            showEigene = !showEigene;
            int color = showEigene ? R.color.yellow : R.color.white;
            DrawableCompat.setTint(binding.buttonEigene.getBackground(), ContextCompat.getColor(requireContext(), color));
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
        
        // Nicht jedes Mal neu instanziieren, das ist extrem langsam!
        if (kennzeichenKI == null) {
            kennzeichenKI = new Kennzeichen_KI(getActivity());
        }
        kennzeichenListe = kennzeichenKI.getKennzeichenListe();

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

        List<Object> combinedList = new ArrayList<>(filteredList);
        setListAdapter(combinedList);

        SharedPreferences prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        boolean showAds = prefs.getBoolean("adSwitch", false);
        Log.d("ListFragmentAds", "showAds: " + showAds + ", listSize: " + filteredList.size());

        if (showAds && !filteredList.isEmpty()) {
            if (adRunnable != null) adHandler.removeCallbacks(adRunnable);
            adRunnable = () -> loadAdsAsync();
            if (searchQuery.isEmpty()) {
                loadAdsAsync(); // Load immediately if no search
            } else {
                adHandler.postDelayed(adRunnable, 500); // Debounce search
            }
        }
    }

    private void loadAdsAsync() {
        if (adapter == null || !isAdded()) return;

        // Clear previous ads
        for (NativeAd ad : nativeAds) ad.destroy();
        nativeAds.clear();

        AdLoader.Builder builder = new AdLoader.Builder(requireContext(), getString(R.string.admob_native_ad_unit_id_list));

        builder.withAdListener(new com.google.android.gms.ads.AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull com.google.android.gms.ads.LoadAdError adError) {
                Log.e("ListFragmentAds", "Werbung konnte nicht geladen werden. Grund: " + adError.getMessage() + " (Code: " + adError.getCode() + ")");
                // Wenn Werbung fehlschlägt, tun wir nichts weiter - die Liste ist bereits durch updateList() korrekt gesetzt.
            }

            @Override
            public void onAdLoaded() {
                Log.d("ListFragmentAds", "Werbung erfolgreich geladen!");
            }
        });

        builder.forNativeAd(nativeAd -> {
            if (!isAdded() || adapter == null) {
                nativeAd.destroy();
                return;
            }
            nativeAds.add(nativeAd);

            // Rebuild items from current adapter
            List<Kennzeichen> currentKennzeichen = new ArrayList<>();
            for (int i = 0; i < adapter.getCount(); i++) {
                Object item = adapter.getItem(i);
                if (item instanceof Kennzeichen) {
                    currentKennzeichen.add((Kennzeichen) item);
                }
            }

            // Sicherstellen, dass wir Daten haben
            if (currentKennzeichen.isEmpty()) return;

            adapter.setNotifyOnChange(false);
            adapter.clear();
            adapter.add(nativeAds.get(0));
            int adIndex = 0;

            for (int i = 0; i < currentKennzeichen.size(); i++) {
                Kennzeichen k = currentKennzeichen.get(i);
                adapter.add(k);

                boolean isEndOfList = (i == currentKennzeichen.size() - 1);
                boolean isTypeChange = false;

                if (!isEndOfList) {
                    String thisType = k.getTyp();
                    String nextType = currentKennzeichen.get(i + 1).getTyp();
                    if (thisType != null && !thisType.equals(nextType)) {
                        isTypeChange = true;
                    }
                }

                if ((isEndOfList || isTypeChange) && adIndex < nativeAds.size()) {
                    adapter.add(nativeAds.get(adIndex++));
                }
            }
            adapter.setNotifyOnChange(true);
            adapter.notifyDataSetChanged();
        });

        AdLoader adLoader = builder.build();
        adLoader.loadAds(new AdRequest.Builder().build(), 5);
    }

    private void setListAdapter(List<Object> list) {
        adapter = new KennzeichenlistAdapter(getActivity(), list);
        int activeTypes = (showNormal ? 1 : 0) + (showSonder ? 1 : 0) + (showAuslaufend ? 1 : 0) + (showEigene ? 1 : 0);
        ((KennzeichenlistAdapter) adapter).setOnlyOneTypeSelected(activeTypes == 1);
        ((KennzeichenlistAdapter) adapter).setSelectedItems(selectedKennzeichen);
        
        // Count only Kennzeichen for the text view
        int kennzeichenCount = 0;
        for (Object o : list) if (o instanceof Kennzeichen) kennzeichenCount++;
        binding.textViewAnzahl.setText(kennzeichenCount + " Kennzeichen gefunden");

        binding.list.setAdapter(adapter);
        binding.fastScroller.attachToListView(binding.list);
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
            DrawableCompat.setTint(binding.buttonAlle.getBackground(), ContextCompat.getColor(requireContext(), R.color.yellow));
        } else {
            DrawableCompat.setTint(binding.buttonAlle.getBackground(), ContextCompat.getColor(requireContext(), R.color.white));
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
        for (NativeAd ad : nativeAds) {
            ad.destroy();
        }
        binding = null;
    }

    private void showTour() {
        View listItem = binding.list.getChildAt(4).findViewById(R.id.kennzeichen);
        new TapTargetSequence(requireActivity())
                .targets(
                        TapTarget.forView(binding.addBtn, "Kennzeichen hinzufügen", "Du kennst ein Kennzeichen, z.B. aus einem anderen Land. Dann füge es einfach selbst hinzu!")
                                .outerCircleColor(R.color.yellow)
                                .transparentTarget(true)
                                .targetCircleColor(android.R.color.white)
                                .titleTextColor(android.R.color.black)
                                .descriptionTextColor(android.R.color.black)
                                .targetRadius(30)
                                .cancelable(true),

                        TapTarget.forView(listItem, "Details anzeigen", "Tippe ein Kennzeichen an, um mehr zu erfahren und halte es gedrückt um mehrere auszuwählen.")
                                .outerCircleColor(R.color.red)
                                .transparentTarget(true)
                                .targetCircleColor(android.R.color.white)
                                .titleTextColor(android.R.color.black)
                                .descriptionTextColor(android.R.color.black)
                                .targetRadius(45)
                                .cancelable(true)
                )
                .continueOnCancel(true)
                .listener(new TapTargetSequence.Listener() {
                    @Override
                    public void onSequenceFinish() {
                    }

                    @Override
                    public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {
                    }

                    @Override
                    public void onSequenceCanceled(TapTarget lastTarget) {
                    }
                })
                .start();
    }
}