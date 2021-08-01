# :notebook:Grafobook
A free, offline and open source note taking Android application for Android.

![Screenshot_1627784149-small](https://user-images.githubusercontent.com/85042318/127760711-75498621-a55f-4c39-9c9e-91126fe0f551.png)
![Screenshot_1627784175-small](https://user-images.githubusercontent.com/85042318/127760722-95617b97-c169-47f1-a45e-24c602553b30.png)
![Screenshot_1627786172-small](https://user-images.githubusercontent.com/85042318/127760724-a53c2daf-8e4a-4e8e-94cd-b593cb38ef88.png)
![Screenshot_1627783064-small](https://user-images.githubusercontent.com/85042318/127760726-be4ddbe1-9ead-42c1-aca4-192762b90dde.png)
![Screenshot_1627783201-small](https://user-images.githubusercontent.com/85042318/127760729-a881b174-049a-47fe-bf34-c912f8dab9f9.png)
![Screenshot_1627797282-small](https://user-images.githubusercontent.com/85042318/127760934-961478ac-bdc1-4862-aff7-394cf3eba936.png)


## :rocket:Features

* Create, edit and delete notes
* Rich text support (**bold**, *italics*, +underline+, ~~strikethrough~~, highlighting, text color)
* Add widgets to your home screen
* Add tags and colors to your notes to organize them
* Auto cloud backup
* Lock notes with password
* Attach a reminder to your notes and get notified
* Search your notes by name and content in the order that you want
* Customize your application theme
* Duplicate notes
* Backup your notes, share them and import them
* Export notes as the exclusive .gfbk format or as an html file

## :iphone:Version support
The application supports Android 7 and higher (Android 12 support coming in a future update).

## :earth_americas:Translations
The app is fully translated to:
* English
* Spanish

## :hammer:Creation  process
If you want to know about the creation process, [I have a video where I try to create the application in just 10 days! (The 1.0 version)](https://youtu.be/qusf77IZOcU).

## :arrow_down:Downloads
|[Google Play Page](https://play.google.com/store/apps/details?id=com.byteseb.grafobook)|

I really appreciate all feedback. If you want to suggest some feature, report a bug or just share your opinion, you can [open a new issue](https://github.com/ByteSeb/Grafobook/issues/new). Thank you!

## :new: Upcoming release: 1.3
### ⚠️ Everything in this section is just ideas, drafts. This is just to keep in mind what could be next for Grafobook. Features mentioned below could change! If you want to propose yours, please, [open a new issue](https://github.com/ByteSeb/Grafobook/issues/new).
I have been thinking about splitting the updates into small-medium updates, because 1.2 was a very big update that took almost a month to finish. This would allow me to release them quicker and fix urgent bugs, but a the cost of less important features frequently.

🟢 = Very likely
🟡 = Likely
🔴 = Unlikely

Possible 1.2.x/1.3 features:
* 🟡Reworking the whole editor, maybe adding the ability to create format groups, or something similar (for example: having the bold and italics button in a group that the user created according to how they like their workspace to be).
* 🟡Creating a new formatting "engine" or "algorithm". To be more specific, using a new way of saving and loading the notes. It should be able to preserve formatting and adding missing formatting features that have not been added due to Span limitations. This could include Alignment, bullet lists, code blocks, checklists, etc.
* 🟢Adding a new note tile to quick settings.
* 🟡Adding support for Google Assistant
* 🟢Hiding notes
* 🟢Folders: You would be able to save notes in folders and (maybe?) create nested folders. This would include an archived and recycle bin folders.
* 🟡Sharing notes by QR code. As weird as it can sound, you could share a note quickly with a person that would just scan the QR code in their Grafobook app. The QR code stores a .gfbk file. Obviously, due to the limitations, the QR saves the file as a Json string, not the actual file.

## :scroll: Changelog

### 1.0 
* Created the application

### 1.1
* Selection and sorting dedicated buttons replaced with a PopupMenu at the bottom
* Added ability to make local backup files (.gfbk) and share them. You can also share as an html file. (If there are several ones, a zip will be created)
* New feature for duplicating notes
* Save button in editor replaced by default with Auto Save. You can toggle this behavior in settings.
* Bug fixes

### 1.1.1
* Fixed bug where an empty useless note would get saved.
* When opening a note, the soft keyboard is shown automatically

### 1.2
* Added requested feature for auto cloud backups: A backup is requested once the app detects some note or shared preference has changed, it does not perform one instantaneously, but one every 24 hours. The data is backed up to Google Drive if connected to Wi-Fi (or mobile data if specified in settings) and if the backup size is not greater than 25 MB. A backup should be restored automatically when reinstalling the app, or when performing a restore in the Setup screen in a new phone.
* Added two types of widgets: A normal note that can be pinned to your home screen, and the "Upcoming Widget" that displays your upcoming reminders.
* You can lock notes with a password. This one remains even if you share it with anyone.
* The logo has been tweaked.
* New app shortcut for importing notes quickly.
* New feature for exporting notes as plain text (No formatting).
* Improved code organization by creating new util classes: ColorUtils, DimenUtils, NotificationUtils, PrefUtils, ReminderUtils, TimeUtils and WidgetUtils
* All Activities now extend BaseActivity. This class contains theming code that most activities used, avoiding copy-pasting.
* Note cards get a slight redesign, removing the icon next to the last modified date, item that now uses a more natural interpretation of dates (ex "Tommorow 13:00" instead of "year/month/day 13:00").
* The Note Activity has gotten a small redesign to make it easier to know if the note has a color. It also has a hide/show animation that corresponds to the EditText that the user is focusing (Note's name EditText or Note's content EditText).
* Moved NoteActivity's favorites checkbox at the bottom, to be inside the editor's formatting bar
* Replaced all back buttons' icon with another one.
* Fixed bug where sometimes you could not add a reminder, claiming that it was a past date.
* You can toggle the back button at the top of the Note Activity with a new setting
* Changed Tag Sheet behavior, to be able to open notes by tapping on them.
* When opening the search bar popup menu, the only option related to selection if selection mode is not eenabled is "Select All". It was redundant to have all the selection submenu when only one option was going to be available.
* The sorting submenu replaced the checkboxes for radiobuttons to act as a visual hint that you can only select one sorting option.
* All the recycler views that show notes are now asymmetrical. This takes advantage of different sized notes, fitting more content in the same space. The previous approach forced all notes to be the same size.
* Settings page features icons and a reorganized structure.
* Some UI elements are now more/less rounded, as they now use a global dimens.xml value (ex: Search bar is less rounded, but notes are more rounded).
* Added setting for toggling the content of a locked note.
* Fixed bug where recreation did not work in ImportActivity
* Fixed bug where the notifications got canceled when the user rebooted their phone. Now they get refreshed when the boot is completed (BootReceiver class).
