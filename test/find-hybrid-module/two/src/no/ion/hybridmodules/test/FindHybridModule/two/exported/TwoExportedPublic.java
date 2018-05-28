package no.ion.hybridmodules.test.FindHybridModule.two.exported;

import no.ion.hybridmodules.test.FindHybridModule.one.exported.OneExportedPublic;
import no.ion.hybridmodules.test.FindHybridModule.TwoInternalPublic;


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
