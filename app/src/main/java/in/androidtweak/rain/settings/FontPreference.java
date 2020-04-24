package in.androidtweak.rain.settings;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class FontPreference  {
    public static final String DEFAULT = "manjari";

    private static final String PATH_DEFAULT = "fonts/Manjari-Regular.ttf";
    private static final String PATH_MANJARI = "fonts/Manjari-Regular.ttf";
    private static final String PATH_MEERA = "fonts/Meera.ttf";
    private static final String PATH_CHILANKA = "fonts/Chilanka-Regular.ttf";
    private static final String PATH_KERALEEYAM = "fonts/Keraleeyam.ttf";
    private static final String PATH_GAYATHRI = "fonts/Gayathri-Regular.ttf";
    private static final String PATH_ISHTIKA = "fonts/ishtika.ttf";
    private static final String PATH_DYUTHI = "fonts/Dyuthi-Regular.ttf";
    private static final String PATH_RACHANA = "fonts/Rachana-Regular.ttf";
    private static final String PATH_KARUMBI = "fonts/Karumbi-Regular.ttf";
    private static final String PATH_UROOB = "fonts/Uroob-Regular.ttf";

    private static final String NAME_DEFAULT = "മഞ്ജരി";
    private static final String NAME_MANJARI = "മഞ്ജരി";
    private static final String NAME_MEERA = "മീര";
    private static final String NAME_CHILANKA = "ചിലങ്ക";
    private static final String NAME_KERALEEYAM = "കേരളീയം";
    private static final String NAME_GAYATHRI = "ഗായത്രി";
    private static final String NAME_ISHTIKA = "ഇഷ്ടിക";
    private static final String NAME_DYUTHI = "ദ്യുതി";
    private static final String NAME_RACHANA = "രചന";
    private static final String NAME_KARUMBI = "കറുമ്പി";
    private static final String NAME_UROOB = "ഉറൂബ്";

    public static String getDisplayName(String name){
        Log.d("test", name);
        if (name.equals("manjari")) {
            return NAME_MANJARI;
        } else if (name.equals("meera")) {
            return NAME_MEERA;
        } else if (name.equals("rachana")) {
            return NAME_RACHANA;
        } else if (name.equals("gayathri")) {
            return NAME_GAYATHRI;
        } else if (name.equals("keraleeyam")) {
            return NAME_KERALEEYAM;
        } else if (name.equals("chilanka")) {
            return NAME_CHILANKA;
        } else if (name.equals("uroob")) {
            return NAME_UROOB;
        } else if (name.equals("dyuthi")) {
            return NAME_DYUTHI;
        } else if (name.equals("ishtika")) {
            return NAME_ISHTIKA;
        } else if (name.equals("karumbi")) {
            return NAME_KARUMBI;
        }

        Log.d("test", name);
        return NAME_MANJARI;
    }

    public static String getPath(String name){
        if (name.equals("manjari")) {
            return PATH_MANJARI;
        } else if (name.equals("meera")) {
            return PATH_MEERA;
        } else if (name.equals("rachana")) {
            return PATH_RACHANA;
        } else if (name.equals("gayathri")) {
            return PATH_GAYATHRI;
        } else if (name.equals("keraleeyam")) {
            return PATH_KERALEEYAM;
        } else if (name.equals("chilanka")) {
            return PATH_CHILANKA;
        } else if (name.equals("uroob")) {
            return PATH_UROOB;
        } else if (name.equals("dyuthi")) {
            return PATH_DYUTHI;
        } else if (name.equals("ishtika")) {
            return PATH_ISHTIKA;
        } else if (name.equals("karumbi")) {
            return PATH_KARUMBI;
        }

        return PATH_MANJARI;
    }
}
