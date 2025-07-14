package de.haainz.kennzeichenerkennung;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class KennzeichenFilter {
    public enum Type { NORMAL, SONDER, AUSLAUFEND, EIGENE }

    private final Set<Type> activeTypes = EnumSet.allOf(Type.class);

    public void toggle(Type type) {
        if (activeTypes.contains(type)) activeTypes.remove(type);
        else activeTypes.add(type);
    }

    public List<Kennzeichen> filter(List<Kennzeichen> list, String query) {
        String q = query.toLowerCase();
        List<Kennzeichen> result = new ArrayList<>();

        for (Kennzeichen k : list) {
            if (!matchesQuery(k, q)) continue;

            if ((activeTypes.contains(Type.NORMAL) && k.isNormal()) ||
                    (activeTypes.contains(Type.SONDER) && k.isSonder()) ||
                    (activeTypes.contains(Type.AUSLAUFEND) && k.isAuslaufend()) ||
                    (activeTypes.contains(Type.EIGENE) && k.isEigene())) {
                result.add(k);
            }
        }
        return result;
    }

    private boolean matchesQuery(Kennzeichen k, String q) {
        return k.OertskuerzelGeben().toLowerCase().contains(q) ||
                k.OrtGeben().toLowerCase().contains(q) ||
                k.StadtKreisGeben().toLowerCase().contains(q) ||
                (k.BundeslandGeben() != null && k.BundeslandGeben().toLowerCase().contains(q));
    }
}