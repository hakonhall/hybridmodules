module rich.descriptor {
    requires java.logging;
    requires transitive required;
    exports rich.descriptor.exported;
    exports rich.descriptor.qualified to friend;
}
