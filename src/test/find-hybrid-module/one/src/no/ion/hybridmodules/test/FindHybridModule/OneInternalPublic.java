package no.ion.jhms.test.FindHybridModule;

public class OneInternalPublic {
    public String toString(Object o) {
        return getClass().getName().toString() + " with object '" +
            o.toString() + "'";
    }
}
