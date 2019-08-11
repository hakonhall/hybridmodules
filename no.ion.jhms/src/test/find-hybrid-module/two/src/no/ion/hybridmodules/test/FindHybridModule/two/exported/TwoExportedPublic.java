package no.ion.jhms.test.FindHybridModule.two.exported;

import no.ion.jhms.test.FindHybridModule.one.exported.OneExportedPublic;
import no.ion.jhms.test.FindHybridModule.TwoInternalPublic;


public class TwoExportedPublic {
    public static void main(String... args) {
        for (var arg : args) {
            System.out.println("arg: " + arg);
        }

        OneExportedPublic oep = new OneExportedPublic();

        TwoInternalPublic tip = new TwoInternalPublic();
        System.out.println(oep.toString(tip));
        
    }
}
