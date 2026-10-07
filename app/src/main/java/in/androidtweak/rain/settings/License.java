package in.androidtweak.rain.settings;

/** The licenses the bundled fonts come under. */
public enum License {
    OFL("SIL Open Font License 1.1"),
    GPL_2("GNU GPL 2.0"),
    GPL_3_FONT_EXCEPTION("GNU GPL 3.0 + font exception");

    /** License names are kept in English, as they're published */
    public final String label;

    License(String label) {
        this.label = label;
    }
}
