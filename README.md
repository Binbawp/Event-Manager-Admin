# Event Manager – Admin App (Android)

This is the **Admin app** for my COMP1786 coursework. It is a native Android app written in **Java**. The admin uses it to create events and their sessions, search them, and upload everything to the cloud so the User app can show it.

- Student name: [YOUR NAME]
- Student ID: [YOUR ID]

The other half of the project is the **User app** (.NET MAUI), which is in the `Event-Manager-User` folder. It reads the events that this app uploads.

---

## What the app does

- Add, edit and delete **events** (name, date, location, organiser, and optional description, contact email and phone)
- Add, edit and delete **sessions** inside an event (name, time, type, and optional speaker, room and description)
- Checks the input before saving (for example the date must be a real date in DD/MM/YYYY and the time must be HH:mm)
- Stops duplicate events (same name and same date)
- Shows a **confirmation screen** with all the details before a new event is saved
- Deleting an event also deletes its sessions, and it warns you first
- **Search** events by name or description, search sessions inside an event, and an **Advanced search** by date and organiser
- **Upload to cloud**: sends all events and sessions to Firebase in one go, after checking there is internet
- Extra features: the upload runs in the background so the screen does not freeze, a sync status text (for example "Changes not uploaded yet"), and an **auto-sync** switch that uploads by itself after changes
- Dark theme

The app has 3 tabs at the bottom: **Dashboard**, **Events** and **Settings**.

---

## What you need

| Thing | Version |
|---|---|
| Android Studio | A recent version that supports Android Gradle Plugin 9.4.1 (I used: [YOUR ANDROID STUDIO VERSION]) |
| Android SDK Platform | **API 37** (the project uses compileSdk 37) |
| Minimum Android version | Android 7.0 (API 24) |
| Gradle | 9.6.0 (downloaded automatically by the Gradle wrapper) |
| Internet | Needed the first time (to download Gradle and libraries) and for the cloud upload |

The project asks Gradle for Java 25. If you do not have it, Gradle should download it by itself the first time, so you do not need to install it yourself.

---

## How to compile and run it

1. **Unzip** the submission. You should have two folders: `Event-Manager-Admin` and `Event-Manager-User`.
2. Open **Android Studio**, click **File > Open**, and choose the `Event-Manager-Admin` folder (the one that contains `settings.gradle.kts`). If it asks "Trust project?", click **Trust**.
3. Wait for **Gradle sync** to finish (see the progress bar at the bottom). The first time can take a few minutes because it downloads things.
4. If Android Studio says the SDK is missing, go to **Tools > SDK Manager**, tick **Android API 37**, and click Apply.
5. Create a phone to run on. Go to **Tools > Device Manager > Create Virtual Device**, pick any phone (for example a Pixel), and choose a system image with API 24 or higher. You can also use a real phone with USB debugging turned on.
6. Pick your device at the top and press the green **Run** button.
7. The app is called **Event Manager**. It will open on the Dashboard.

The app starts **empty** because there is no demo data. Tap the **+** button to add your first event.

---

## How to use it (quick tour)

1. **Add an event**: tap **+** (or "New event" on the Dashboard), fill in the form, and tap save. A confirmation window shows your details. Tap **Confirm** to save, or **Edit** to go back.
2. **Add sessions**: open an event and add sessions to it. The session ID is made automatically.
3. **Search**: on the Events tab type in the search box. Use **Advanced search** to look by date and organiser.
4. **Upload to cloud**: go to the **Settings** tab and tap upload (the cloud button on the Dashboard does the same). The phone or emulator must have internet. A message tells you if it worked.
5. Now open the **User app**. Pull down to refresh and you will see your events.

---

## Where the data goes

- Events and sessions are saved on the phone in a **SQLite** database.
- When you upload, the app sends everything to a **Firebase Realtime Database** at `/events.json`. Each upload **replaces** what was in the cloud, so the cloud is always a copy of what is on the phone.
- The database address is written in `CloudSyncManager.java`. The User app uses the same address (in its `EventService.cs`), which is how the two apps share data.
- The Firebase access rules are set to allow access until **30 June 2027**. After that date Firebase would refuse the requests, so the cloud part would stop working unless the rules are renewed.

---

## Project structure

```
Event-Manager-Admin/
├── app/src/main/
│   ├── java/com/example/eventassistant/   <- all the Java code
│   │   ├── MainActivity.java              main screen with the 3 tabs
│   │   ├── DashboardFragment.java         Dashboard tab
│   │   ├── EventsFragment.java            Events tab (list and search)
│   │   ├── SettingsFragment.java          Settings tab (upload and auto-sync)
│   │   ├── AddEditEventActivity.java      event form and confirmation
│   │   ├── AddEditSessionActivity.java    session form
│   │   ├── EventDetailsActivity.java      one event with its sessions
│   │   ├── AdvancedSearchActivity.java    search by date and organiser
│   │   ├── DatabaseHelper.java            SQLite tables and queries
│   │   ├── CloudSyncManager.java          uploads to Firebase
│   │   ├── SyncPrefs.java                 remembers sync status and auto-sync setting
│   │   ├── ValidationUtils.java           checks date, time, email, phone
│   │   ├── NetworkUtils.java              checks the internet connection
│   │   ├── UiUtils.java                   date/time pickers and delete dialog
│   │   ├── Event.java, Session.java       data classes
│   │   └── EventAdapter.java, SessionAdapter.java   list adapters
│   ├── res/layout/                        screen layouts (XML)
│   ├── res/values/                        texts, colours, themes
│   └── AndroidManifest.xml
├── gradle/                                Gradle wrapper and version file
├── build.gradle.kts, settings.gradle.kts, gradle.properties
└── README.md
```

---

## If something goes wrong

- **Gradle sync fails or is very slow**: check your internet, then use **File > Sync Project with Gradle Files** to try again.
- **"SDK location not found"**: Android Studio normally creates `local.properties` by itself when you open the project. If not, set the SDK path in **File > Project Structure > SDK Location**.
- **"Failed to find target with hash string android-37"**: install **API 37** in the SDK Manager.
- **Upload says no connection / failed**: check the emulator or phone has internet. On an emulator, try restarting it, or the Wi-Fi inside it.
- **App crashes right after changing the code**: do **Build > Clean Project** and then **Build > Rebuild Project**.

---

## Things I know could be better

I wrote about these in my report too.

- The upload is **one way** (phone to cloud). Changes made in the cloud do not come back into the Admin app.
- There is **no login** for the cloud, so anyone who has the database address could change the data. Adding Firebase Authentication is the main thing I would do next.
- The layouts are made for phones. I did not make special layouts for tablets or landscape.
- There are no automated tests yet, only the default Android Studio test files.