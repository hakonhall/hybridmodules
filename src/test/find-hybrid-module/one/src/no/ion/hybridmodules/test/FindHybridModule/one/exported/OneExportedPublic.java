package no.ion.jhms.test.FindHybridModule.one.exported;

import no.ion.jhms.test.FindHybridModule.OneInternalPublic;

public class OneExportedPublic {
    public String toString(Object o) {
        OneExported oe = new OneExported();
        OneInternalPublic oip = new OneInternalPublic();
        return getClass().getName().toString() + "\n" +
            oe.toString() + "\n" +
            oip.toString(o);
    }

    public String toString() {
        return getClass().getName();
    }
}
