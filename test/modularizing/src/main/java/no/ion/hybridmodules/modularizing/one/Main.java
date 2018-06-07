package no.ion.hybridmodules.modularizing.one;

import org.apache.commons.collections4.list.UnmodifiableList;

import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String... args) {
        List<String> l = UnmodifiableList.unmodifiableList(Arrays.asList(args));
        System.out.println(l.size());
    }
}
