# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /opt/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Settings sub-pages are opened by class name from app:fragment in res/xml/prefs.xml
-keep public class * extends in.androidtweak.rain.settings.ChoiceFragment {
    public <init>();
}
-keep public class in.androidtweak.rain.settings.CreditsFragment {
    public <init>();
}

# ML Kit finds its components (subject segmentation among them) by reflection, through
# their no-argument constructors; without this R8 removes them and the cut-out fails
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    public <init>();
}
