Mazha Live Wallpaper
====================

An Android live wallpaper of Malayalam letters streaming down the screen.

This wallpaper can be downloaded from Google Play [here](https://play.google.com/store/apps/details?id=in.androidtweak.rain).

Building
--------

Requires the Android SDK (platform 37) and JDK 17+.

    cp .env.sample .env    # then edit paths and signing credentials
    make                   # list available commands
    make build             # assemble the debug APK
    make install           # build and install (asks which device; offers emulators if none)
    make wallpaper         # install and open the live wallpaper preview
    make release           # signed release APK
    make bundle            # signed release App Bundle (.aab) for Google Play


License
-------

Based on Hacker Live Wallpaper by Gulshan Singh https://github.com/gsingh93/hacker-live-wallpaper, Original source code is in MIT License.
