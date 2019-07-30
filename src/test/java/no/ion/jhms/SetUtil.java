package no.ion.jhms;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class SetUtil {
    @SafeVarargs
    static <T> Set<T> asSet(T... elements) {
        var set = new HashSet<T>(elements.length);
        set.addAll(Arrays.asList(elements));
        return set;
    }
}
