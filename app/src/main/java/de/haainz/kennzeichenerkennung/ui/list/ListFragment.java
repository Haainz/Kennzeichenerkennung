package de.haainz.kennzeichenerkennung.ui.list;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import de.haainz.kennzeichenerkennung.AddCityFragment;
import de.haainz.kennzeichenerkennung.ConfirmFragment;
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
    private int buttonAlleColor = R.color.white;
    private int buttonNormalColor = R.color.white;
    private int buttonSonderColor = R.color.white;
    private int buttonAuslaufendColor = R.color.white;
    private int buttonEigeneColor = R.color.white;
    private boolean isUpdatingList = false;
    public ArrayAdapter<Kennzeichen> adapter;

    private boolean isButtonColorTeal200(int buttonColor) {
        return buttonColor == R.color.yellow;
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentListBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        kennzeichenKI = new Kennzeichen_KI(getActivity());
        ArrayList<Kennzeichen> kennzeichenListe = kennzeichenKI.getKennzeichenListe();

        // Filter the list based on button colors
        ArrayList<Kennzeichen> filteredList = new ArrayList<>();
        for (Kennzeichen kennzeichen : kennzeichenListe) {
            if (isButtonColorTeal200(buttonNormalColor) && kennzeichen.isNormal()) {
                filteredList.add(kennzeichen);
            } else if (isButtonColorTeal200(buttonSonderColor) && kennzeichen.isSonder()) {
                filteredList.add(kennzeichen);
            } else if (isButtonColorTeal200(buttonAuslaufendColor) && kennzeichen.isAuslaufend()) {
                filteredList.add(kennzeichen);
            } else if (isButtonColorTeal200(buttonEigeneColor) && kennzeichen.isEigene()) {
                filteredList.add(kennzeichen);
            }
        }

        binding.textViewAnzahl.setText("" + kennzeichenListe.size() + " Kennzeichen gefunden");

        binding.scroll.setHorizontalScrollBarEnabled(false);

        binding.buttonSonder.setBackgroundResource(R.drawable.edit_text_rounded_corner_more);
        binding.buttonSonder.getBackground().setColorFilter(getResources().getColor(R.color.yellow), PorterDuff.Mode.SRC_ATOP);
        buttonSonderColor = R.color.yellow;
        binding.buttonNormal.setBackgroundResource(R.drawable.edit_text_rounded_corner_more);
        binding.buttonNormal.getBackground().setColorFilter(getResources().getColor(R.color.yellow), PorterDuff.Mode.SRC_ATOP);
        buttonNormalColor = R.color.yellow;
        binding.buttonAuslaufend.setBackgroundResource(R.drawable.edit_text_rounded_corner_more);
        binding.buttonAuslaufend.getBackground().setColorFilter(getResources().getColor(R.color.yellow), PorterDuff.Mode.SRC_ATOP);
        buttonAuslaufendColor = R.color.yellow;
        binding.buttonEigene.setBackgroundResource(R.drawable.edit_text_rounded_corner_more);
        binding.buttonEigene.getBackground().setColorFilter(getResources().getColor(R.color.yellow), PorterDuff.Mode.SRC_ATOP);
        buttonEigeneColor = R.color.yellow;
        binding.buttonAlle.setBackgroundResource(R.drawable.edit_text_rounded_corner_more);
        binding.buttonAlle.getBackground().setColorFilter(getResources().getColor(R.color.yellow), PorterDuff.Mode.SRC_ATOP);
        buttonAlleColor = R.color.yellow;
        binding.buttonLike1.setVisibility(VISIBLE);
        binding.buttonLike2.setVisibility(GONE);
        binding.buttonLike3.setVisibility(GONE);
        binding.x.setVisibility(GONE);
        updateList();


        binding.list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Kennzeichen kennzeichen = (Kennzeichen) parent.getItemAtPosition(position);
                PopupMenu popupMenu = new PopupMenu(getActivity(), view);
                popupMenu.inflate(R.menu.popup_menu);

                if (kennzeichen.isEigene()) {
                    popupMenu.getMenu().findItem(R.id.delete).setVisible(true);
                } else {
                    popupMenu.getMenu().findItem(R.id.delete).setVisible(false);
                }

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getItemId() == R.id.infos) {
                            InfosFragment infosFragment = new InfosFragment(kennzeichen);
                            infosFragment.show(getParentFragmentManager(), "InfosFragment");
                            return true;
                        } else if (item.getItemId() == R.id.teilen) {
                            // Hier kannst du den Text teilen
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.setType("text/plain");
                            intent.putExtra(Intent.EXTRA_TEXT, "Kürzel: " + kennzeichen.OertskuerzelGeben() + "\nOrt: " + kennzeichen.OrtGeben() + "\nStadt bzw. Kreis: " + kennzeichen.StadtKreisGeben() + "\nBundesland: " + kennzeichen.BundeslandGeben());
                            startActivity(Intent.createChooser(intent, "Teilen"));
                            return true;
                        } else if (item.getItemId() == R.id.delete) {
                            ConfirmFragment confirmFragment = new ConfirmFragment();
                            confirmFragment.setKennzeichen(kennzeichen);
                            confirmFragment.setKennzeichenKI(kennzeichenKI);
                            confirmFragment.setOnConfirmListener(new ListFragment.OnConfirmListener() {
                                @Override
                                public void updateList() {
                                    if (!isUpdatingList) {
                                        isUpdatingList = true;
                                        updateList();
                                        isUpdatingList = false;
                                    }
                                }
                            });
                            confirmFragment.show(getParentFragmentManager(), "ConfirmFragment");
                            return true;
                        } else {
                            return false;
                        }
                    }
                });
                popupMenu.show();
            }
        });

        /*binding.list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Kennzeichen kennzeichen = (Kennzeichen) parent.getItemAtPosition(position);
                String text = kennzeichen.OertskuerzelGeben()+" - "+kennzeichen.OrtGeben()+" - "+kennzeichen.StadtKreisGeben()+" - "+kennzeichen.BundeslandGeben();
                String[] teile = text.split(" - ");
                String kuerzel = teile[0];
                String ort = teile[1];
                String stadtkreis = teile[2];
                String bundesland = teile[3];
                // Hier kannst du den Text teilen
                // Zum Beispiel:
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, "Kürzel: "+kuerzel+"\nOrt: "+ort+"\nStadt bzw. Kreis: "+stadtkreis+"\nBundesland: "+bundesland);
                startActivity(Intent.createChooser(intent, "Teilen"));
                return true;
            }
        });*/

        binding.refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateList();
                Toast.makeText(getContext(), "Liste erfolgreich aktualisiert", Toast.LENGTH_SHORT).show();
            }
        });


        binding.addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddCityFragment addCityFragment = new AddCityFragment();
                addCityFragment.show(getParentFragmentManager(), "AddCityFragment");
            }
        });

        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.x.setVisibility(VISIBLE);
                String searchQuery = s.toString().toLowerCase();
                ArrayList<Kennzeichen> filteredList = new ArrayList<>();
                for (Kennzeichen kennzeichen : kennzeichenKI.getKennzeichenListe()) {
                    if (!kennzeichen.OertskuerzelGeben().equals("Stadtkreis")) {
                        String ort = kennzeichen.OrtGeben();
                        String stadtkreis = kennzeichen.StadtKreisGeben();
                        String bundesland = kennzeichen.BundeslandGeben();

                        if ((kennzeichen.OertskuerzelGeben() != null && kennzeichen.OertskuerzelGeben().toLowerCase().contains(searchQuery)) ||
                                (ort != null && ort.toLowerCase().contains(searchQuery)) ||
                                (stadtkreis != null && stadtkreis.toLowerCase().contains(searchQuery)) ||
                                (bundesland != null && bundesland.toLowerCase().contains(searchQuery))) {
                            if (isButtonColorTeal200(buttonNormalColor) && kennzeichen.isNormal()) {
                                filteredList.add(kennzeichen);
                            } else if (isButtonColorTeal200(buttonSonderColor) && kennzeichen.isSonder()) {
                                filteredList.add(kennzeichen);
                            } else if (isButtonColorTeal200(buttonAuslaufendColor) && kennzeichen.isAuslaufend()) {
                                filteredList.add(kennzeichen);
                            } else if (isButtonColorTeal200(buttonEigeneColor) && kennzeichen.isEigene()) {
                                filteredList.add(kennzeichen);
                            }
                            if (binding.buttonLike3.getVisibility() == VISIBLE) {
                                // Zeige nur die geliketen Kennzeichen
                                if (!kennzeichenKI.LikeÜberprüfen(kennzeichen.OertskuerzelGeben())) {
                                    filteredList.remove(kennzeichen);
                                }
                            } else if (binding.buttonLike2.getVisibility() == VISIBLE) {
                                // Zeige nur die nicht geliketen Kennzeichen
                                if (kennzeichenKI.LikeÜberprüfen(kennzeichen.OertskuerzelGeben())) {
                                    filteredList.remove(kennzeichen);
                                }
                            }
                        }
                    }
                }
                /*ArrayAdapter<Kennzeichen> adapter = new ArrayAdapter<Kennzeichen>(getActivity(), android.R.layout.simple_list_item_1, filteredList) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);
                        TextView textView = (TextView) view.findViewById(android.R.id.text1);
                        Kennzeichen kennzeichen = getItem(position);
                        String og = kennzeichen.OrtGeben().replaceAll("\"", "");
                        String skg = kennzeichen.StadtKreisGeben().replaceAll("\"", "");
                        String bg = null;
                        if (kennzeichen.BundeslandGeben() == null) {
                            bg = "";
                        } else {
                            bg = " - " + kennzeichen.BundeslandGeben();
                        }
                        textView.setText(kennzeichen.OertskuerzelGeben() + " - " + og + " - " + skg + bg);
                        return view;
                    }
                };*/
                adapter = new KennzeichenlistAdapter(getActivity(), filteredList);
                binding.list.setAdapter(adapter);
                binding.textViewAnzahl.setText("" + filteredList.size() + " Kennzeichen gefunden");
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        binding.x.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.searchInput.setText("");
                binding.x.setVisibility(GONE);
            }
        });

        binding.buttonAlle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (buttonAlleColor == R.color.white) {
                    binding.buttonSonder.getBackground().setColorFilter(getResources().getColor(R.color.yellow), PorterDuff.Mode.SRC_ATOP);
                    buttonSonderColor = R.color.yellow;
                    binding.buttonNormal.getBackground().setColorFilter(getResources().getColor(R.color.yellow), PorterDuff.Mode.SRC_ATOP);
                    buttonNormalColor = R.color.yellow;
                    binding.buttonAuslaufend.getBackground().setColorFilter(getResources().getColor(R.color.yellow), PorterDuff.Mode.SRC_ATOP);
                    buttonAuslaufendColor = R.color.yellow;
                    binding.buttonEigene.getBackground().setColorFilter(getResources().getColor(R.color.yellow), PorterDuff.Mode.SRC_ATOP);
                    buttonEigeneColor = R.color.yellow;
                    binding.buttonAlle.getBackground().setColorFilter(getResources().getColor(R.color.yellow), PorterDuff.Mode.SRC_ATOP);
                    buttonAlleColor = R.color.yellow;
                    binding.buttonLike1.setVisibility(VISIBLE);
                    binding.buttonLike2.setVisibility(GONE);
                    binding.buttonLike3.setVisibility(GONE);
                } else if (buttonAlleColor == R.color.yellow) {
                    binding.buttonAlle.getBackground().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
                    buttonAlleColor = R.color.white;
                    binding.buttonNormal.getBackground().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
                    buttonNormalColor = R.color.white;
                    binding.buttonSonder.getBackground().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
                    buttonSonderColor = R.color.white;
                    binding.buttonNormal.getBackground().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
                    buttonNormalColor = R.color.white;
                    binding.buttonAuslaufend.getBackground().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
                    buttonAuslaufendColor = R.color.white;
                    binding.buttonEigene.getBackground().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
                    buttonEigeneColor = R.color.white;
                    binding.buttonLike1.setVisibility(VISIBLE);
                    binding.buttonLike2.setVisibility(GONE);
                    binding.buttonLike3.setVisibility(GONE);
                }
                updateList();
            }
        });

        binding.buttonNormal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (buttonNormalColor == R.color.white) {
                    if (buttonSonderColor == R.color.yellow && buttonAuslaufendColor == R.color.yellow && buttonEigeneColor == R.color.yellow && binding.buttonLike1.getVisibility() == VISIBLE) {
                        binding.buttonAlle.getBackground().setColorFilter(getResources().getColor(R.color.yellow), PorterDuff.Mode.SRC_ATOP);
                        buttonAlleColor = R.color.yellow;
                    }
                    binding.buttonNormal.getBackground().setColorFilter(getResources().getColor(R.color.yellow), PorterDuff.Mode.SRC_ATOP);
                    buttonNormalColor = R.color.yellow;
                } else if (buttonNormalColor == R.color.yellow) {
                    binding.buttonAlle.getBackground().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
                    buttonAlleColor = R.color.white;
                    binding.buttonNormal.getBackground().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
                    buttonNormalColor = R.color.white;
                }
                updateList();
            }
        });

        binding.buttonSonder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (buttonSonderColor == R.color.white) {
                    if (buttonNormalColor == R.color.yellow && buttonAuslaufendColor == R.color.yellow && buttonEigeneColor == R.color.yellow && binding.buttonLike1.getVisibility() == VISIBLE) {
                        binding.buttonAlle.getBackground().setColorFilter(getResources().getColor(R.color.yellow), PorterDuff.Mode.SRC_ATOP);
                        buttonAlleColor = R.color.yellow;
                    }
                    binding.buttonSonder.getBackground().setColorFilter(getResources().getColor(R.color.yellow), PorterDuff.Mode.SRC_ATOP);
                    buttonSonderColor = R.color.yellow;
                } else if (buttonSonderColor == R.color.yellow) {
                    binding.buttonAlle.getBackground().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
                    buttonAlleColor = R.color.white;
                    binding.buttonSonder.getBackground().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
                    buttonSonderColor = R.color.white;
                }
                updateList();
            }
        });

        binding.buttonAuslaufend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (buttonAuslaufendColor == R.color.white) {
                    if (buttonNormalColor == R.color.yellow && buttonSonderColor == R.color.yellow && buttonEigeneColor == R.color.yellow && binding.buttonLike1.getVisibility() == VISIBLE) {
                        binding.buttonAlle.getBackground().setColorFilter(getResources().getColor(R.color.yellow), PorterDuff.Mode.SRC_ATOP);
                        buttonAlleColor = R.color.yellow;
                    }
                    binding.buttonAuslaufend.getBackground().setColorFilter(getResources().getColor(R.color.yellow), PorterDuff.Mode.SRC_ATOP);
                    buttonAuslaufendColor = R.color.yellow;
                } else if (buttonAuslaufendColor == R.color.yellow) {
                    binding.buttonAlle.getBackground().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
                    buttonAlleColor = R.color.white;
                    binding.buttonAuslaufend.getBackground().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
                    buttonAuslaufendColor = R.color.white;
                }
                updateList();
            }
        });

        binding.buttonEigene.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (buttonEigeneColor == R.color.white) {
                    if (buttonNormalColor == R.color.yellow && buttonSonderColor == R.color.yellow && buttonAuslaufendColor == R.color.yellow && binding.buttonLike1.getVisibility() == VISIBLE) {
                        binding.buttonAlle.getBackground().setColorFilter(getResources().getColor(R.color.yellow), PorterDuff.Mode.SRC_ATOP);
                        buttonAlleColor = R.color.yellow;
                    }
                    binding.buttonEigene.getBackground().setColorFilter(getResources().getColor(R.color.yellow), PorterDuff.Mode.SRC_ATOP);
                    buttonEigeneColor = R.color.yellow;
                } else if (buttonEigeneColor == R.color.yellow) {
                    binding.buttonAlle.getBackground().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
                    buttonAlleColor = R.color.white;
                    binding.buttonEigene.getBackground().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
                    buttonEigeneColor = R.color.white;
                }
                updateList();
            }
        });

        binding.buttonLike1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.buttonLike1.setVisibility(GONE);
                binding.buttonLike2.setVisibility(VISIBLE);
                binding.buttonAlle.getBackground().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
                buttonAlleColor = R.color.white;
                updateList();
            }
        });
        binding.buttonLike2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.buttonLike2.setVisibility(GONE);
                binding.buttonLike3.setVisibility(VISIBLE);
                binding.buttonAlle.getBackground().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
                buttonAlleColor = R.color.white;
                updateList();
            }
        });
        binding.buttonLike3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.buttonLike3.setVisibility(GONE);
                binding.buttonLike1.setVisibility(VISIBLE);
                if (buttonNormalColor == R.color.yellow && buttonSonderColor == R.color.yellow && buttonAuslaufendColor == R.color.yellow && buttonEigeneColor == R.color.yellow) {
                    binding.buttonAlle.getBackground().setColorFilter(getResources().getColor(R.color.yellow), PorterDuff.Mode.SRC_ATOP);
                    buttonAlleColor = R.color.yellow;
                }
                updateList();
            }
        });

        return root;
    }

    public void updateList() {
        kennzeichenKI.KennzeichenLikedEinlesen();
        String searchQuery = binding.searchInput.getText().toString().toLowerCase();

        kennzeichenKI = new Kennzeichen_KI(getActivity());
        ArrayList<Kennzeichen> kennzeichenListe = kennzeichenKI.getKennzeichenListe();

        ArrayList<Kennzeichen> filteredList = new ArrayList<>();
        for (Kennzeichen kennzeichen : kennzeichenListe) {
            if (!kennzeichen.OertskuerzelGeben().equals("Stadtkreis")) {
                if ((kennzeichen.OertskuerzelGeben() != null && kennzeichen.OertskuerzelGeben().toLowerCase().contains(searchQuery)) ||
                        (kennzeichen.OrtGeben() != null && kennzeichen.OrtGeben().toLowerCase().contains(searchQuery)) ||
                        (kennzeichen.StadtKreisGeben() != null && kennzeichen.StadtKreisGeben().toLowerCase().contains(searchQuery)) ||
                        (kennzeichen.BundeslandGeben() != null && kennzeichen.BundeslandGeben().toLowerCase().contains(searchQuery))) {
                    filteredList.add(kennzeichen);
                }
            }
            if (!isButtonColorTeal200(buttonNormalColor) && kennzeichen.isNormal()) {
                filteredList.remove(kennzeichen);
            } else if (!isButtonColorTeal200(buttonSonderColor) && kennzeichen.isSonder()) {
                filteredList.remove(kennzeichen);
            } else if (!isButtonColorTeal200(buttonAuslaufendColor) && kennzeichen.isAuslaufend()) {
                filteredList.remove(kennzeichen);
            } else if (!isButtonColorTeal200(buttonEigeneColor) && kennzeichen.isEigene()) {
                filteredList.remove(kennzeichen);
            }
            if (binding.buttonLike3.getVisibility() == VISIBLE) {
                // Zeige nur die geliketen Kennzeichen
                if (!kennzeichenKI.LikeÜberprüfen(kennzeichen.OertskuerzelGeben())) {
                    filteredList.remove(kennzeichen);
                }
            } else if (binding.buttonLike2.getVisibility() == VISIBLE) {
                // Zeige nur die nicht geliketen Kennzeichen
                if (kennzeichenKI.LikeÜberprüfen(kennzeichen.OertskuerzelGeben())) {
                    filteredList.remove(kennzeichen);
                }
            }
        }

        // Setzen Sie die gefilterte Liste in den Adapter
        adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, filteredList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                Kennzeichen kennzeichen = getItem(position);
                String og = kennzeichen.OrtGeben().replaceAll("\"", "");
                String skg = kennzeichen.StadtKreisGeben().replaceAll("\"", "");
                String bg;
                if (kennzeichen.BundeslandGeben() == null) {
                    bg = "";
                } else {
                    bg = " - " + kennzeichen.BundeslandGeben();
                }
                textView.setText(kennzeichen.OertskuerzelGeben() + " - " + og + " - " + skg + bg);
                return view;
            }
        };
        KennzeichenlistAdapter adapter = new KennzeichenlistAdapter(getActivity(), filteredList);
        binding.textViewAnzahl.setText(filteredList.size() + " Kennzeichen gefunden");
        binding.list.setAdapter(adapter);
    }

    public interface OnConfirmListener {
        void updateList();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}