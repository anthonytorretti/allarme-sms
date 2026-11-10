# Allarme SMS

Android app for the Urmet 1059/012 panel (Vesta/Climax platform).
It sends `PROG <PIN> MODE:A|H|D` by SMS and shows the panel's reply.

## Build
1. Open this folder in Android Studio (File → Open), let Gradle sync.
2. Connect your phone (USB debugging on) and press Run, or
   Build → Build APK(s) and copy `app/build/outputs/apk/debug/app-debug.apk` to the phone.

## Use
- Enter the panel's phone number and your PIN, then tap "Salva impostazioni".
- Allow SMS permissions (send + receive).
- Each button asks for confirmation, sends the SMS, then waits up to 3 minutes
  for the reply: green = OK, red = ERROR (fault or wrong PIN), orange = no reply.
- Replies are caught even if the app is in the background and shown when you reopen it.

The keyword `PROG` is fixed in `Prefs.java` (constant KEYWORD).

## Build without Android Studio (GitHub Actions)
1. Create a new repository on GitHub and upload all files of this folder (including the hidden `.github` folder).
2. Open the "Actions" tab: the "Build APK" job starts automatically (or press "Run workflow").
3. When it is green, open the run and download the "AllarmeSMS-apk" artifact (a zip containing app-debug.apk).
4. Copy app-debug.apk to the phone and open it to install (allow "install unknown apps").
