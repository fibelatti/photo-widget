# Changelog

All notable user-facing changes to this app will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

## [v1.22.0] - 2025-01-19

### Changed

* Update the widget configuration screen to organize settings into categories
* Using a custom "Padding" is no longer allowed when using "Fill Widget Area"

### Fixed

* Fix an issue where trying to permanently delete a widget from "My Widgets"
  would not work as expected
* Attempt to fix widgets not working on GoodLock

## [v1.21.1] - 2025-01-13

### Fixed

* Fixed a concurrency issue that could affect widgets after updating the app
  or restarting the device

## [v1.21.0] - 2025-01-13

### Added

* New option for black and white widgets

### Fixed

* Fixed an issue that could lead to a crash when trying to edit widgets synced
  with a folder
* Fixed an issue that could lead to a crash when requesting to add a widget to
  the home screen

## [v1.20.1] - 2025-01-11

### Changed

* Change how the widget is updated to improve resource usage and tentatively
  fix crashes that are happening on low end devices
* Update widget loading logic to always load from source on the background
* Update folder selection logic to avoid loading photos twice

## [v1.20.0] - 2025-01-05

### Added

* New shape: daisy
* New tap action: URL shortcut
* Option to disable taps when using the pause tap action

### Changed

* Updated how folder photos are kept in sync with widgets to improve the
  performance of widgets with thousands of photos

### Fixed

* Transparent PNGs not working as expected when optimized storage was enabled

## [v1.19.0] - 2024-11-23

### Added

* Colored borders
* New setting to control the compression behavior when importing photos
* New option to import from an existing widget when setting up another
* New setting to grant exact alarms permission

### Changed

* It's now possible to exclude photos from widgets synced to folders
* Updated target SDK to 35 (Android 15)

## [v1.18.1] - 2024-11-07

### Fixed

* The loading icon would be visible behind transparent widgets

## [v1.18.0] - 2024-11-06

### Added

* New tap action to pause/resume automatic photo cycling
* New app theme option for using a true black background

### Changed

* Skip the unsaved changes warning when there are no changes
* Added device details to bug reports

## [v1.17.0] - 2024-10-27

### Added

* Widgets now support multiple tap actions. Tap the sides to view the previous
  or next photo, with a customizable middle action
* French translation

### Changed

* Increased widget starting size to 2x2
* Add loading indicator when loading the widget on the home screen for the
  first time

### Fixed

* Add missing progress indicator when cropping
* Fix duplicate feature not working as it should for photo widgets

## [v1.16.2] - 2024-10-23

### Changed

* Apply signing conditionally to avoid interfering with Reproducible Builds

## [v1.16.1] - 2024-10-22

### Changed

* Automated the release build for increased reliability in regards to
  Reproducible Builds

## [v1.16.0] - 2024-10-22

### Added

* Russian translation

### Changed

* Better support for egge-to-edge UI
* Better support for landscape and tablets

### Fixed

* No longer applying a corner radius when using "fill widget area"
* Persisting the original trigger date for widget alarms, to avoid restarting
  after a device boot

## [v1.15.2] - 2024-10-09

### Fixed

* Fix drag to reorder crash

## [v1.15.1] - 2024-10-06

### Fixed

* Fix buttons not working in the full screen viewer

### Changed

* Reduce the initial scale of the full screen viewer
* Update the shortcut used to grant exact alarm permission

## [v1.15.0] - 2024-09-29

### Added

* It's now possible to set a cycle interval in days
* Added more help articles for troubleshooting

### Changed

* You can now reorder photos by dragging them when configuring a widget. Long
  press a photo and start dragging

## [v1.14.0] - 2024-09-14

### Added

* It's now possible to schedule times to cycle through photos
* It's now possible to restore recently deleted photos (photo widget only, for
  photos deleted after this update)
* It's now possible to view the original photo when using the view in full
  screen tap action

### Changed

* Updated and reduced in-app motion animations
* Improved pre-crop image quality when importing photos

## [v1.13.3] - 2024-08-26

### Fixed

* Fix shaped widgets crashing

## [v1.13.2] - 2024-08-25

### Changed

* Improve the loading performance of the "My Widgets" screen

### Fixed

* Improved image quality of widgets
* App shortcut tap action sometimes not working as expected

## [v1.13.1] - 2024-08-14

### Changed

* The photo count of each directory and subdirectory is now check independently

### Fixed

* Fixed a scenario where all photos of an existing widget could be deleted
* Fixed a scenario where a broken widget was displayed on the widget list

## [v1.13] - 2024-08-10

### Added

* Removed widgets can be restored if they've been deleted for less than a week

### Changed

* Update the photo/folder picker to show a hint
* Made the required adjustments to support Reproducible Builds

## [v1.12.1] - 2024-07-22

### Fixed

* Fix error handling when a photo is not found

## [v1.12.0] - 2024-07-20

### Added

* New aspect ratio to fill the widget area

### Changed

* The full screen viewer control hints are only shown once

### Fixed

* Avoid crashing when the file cannot be found

## [v1.11.0] - 2024-07-07

### Added

* Turkish translation

### Changed

* Make it possible to bypass the folder limit

## [v1.10.0] - 2024-06-23

### Added

* Make it possible to view the previous photo on tap
* Add "View in gallery" as a tap action
* Make it possible to adjust the widget padding
* Make it possible to duplicate existing widgets

### Changed

* Declare INSTALL_SHORTCUT attempting to solve Xiaomi issues

## [v1.9.1] - 2024-06-11

### Fixed

* Intervals in seconds not working correctly

## [v1.9.0] - 2024-06-10

### Added

* Add support for intervals in seconds
* Add support for widget opacity
* Add support for widget offset

### Changed

* Updated bottom nav bar icons to better represent the selected state
* Updated the button structure of the source picker dialog
* Widgets will now include subdirectories of all synced directories

## [v1.8.3-floss] - 2024-05-22

### Changed

* Adjust binary contents to make the app compatible with IzzyOnDroid
* Updated bottom nav bar icons to better represent the selected state
* Updated the button structure of the source picker dialog

## [v1.8.3] - 2024-05-18

### Added

* Feedback and Privacy Policy shortcuts on the settings screen

### Fixed

* Sometimes widgets would not work correctly after a device restart

## [v1.8.2] - 2024-05-15

### Changed

* Added explanation for the widget defaults

### Fixed

* Crash that could happen when loading widgets

## [v1.8.1] - 2024-05-13

### Fixed

* Brightness setting now being applied correctly

## [v1.8] - 2024-05-13

### Added

* Revamped home screen, including a new "My Widgets" screen to make it easier
  to view and edit existing widgets
* Default widget settings
* New tap action: set the widget as an app shortcut

### Changed

* Made the brightness increase optional when using the full screen viewer
* Share photos to an existing widget

## [v1.7] - 2024-05-04

### Added

* Sync widgets with multiple folders. The limit per folder was also increased
  to 1000 photos
* The full screen viewer now shows the original photo (in increased brightness
  for those using widgets to display their club cards)
* You can now view the next and previous photos from within the full screen
  viewer
* Heart shape for square widgets

## [v1.6.1] - 2024-04-17

* Folder based widgets — choose a folder to have all its photos automatically
  added to a widget
* Shuffle — flip through the widget photos in a random order
* Disable auto-flipping — you can now disable the automatic flipping of widgets

### Changed

* This update also includes a bunch of small adjustments to the interface to
  better accommodate all controls

## [v1.6] - 2024-04-17

### Added

* Folder based widgets — choose a folder to have all its photos automatically
  added to a widget
* Shuffle — flip through the widget photos in a random order
* Disable auto-flipping — you can now disable the automatic flipping of widgets

### Changed

* This update also includes a bunch of small adjustments to the interface to
  better accommodate all controls

## [v1.5] - 2024-03-18

### Added

* Flexible intervals — you can finally configure widgets to change photos every
  1 minute!
* Original aspect ratio — configure a widget to respect the original aspect
  ratio of the photo
* No tap action — for those who just want static widgets, with no actions

## [v1.4] - 2024-02-24

### Added

* Choose the tap action of each widget — View in full screen or switch to the
  next photo
* You can now zoom and pan photos when tapping to view a widget in full screen
* More flipping intervals — every 2 and 8 hours added to the options

## [v1.3] - 2024-02-02

### Added

* You can now customize the rounded corners of tall and wide widgets
* A fully square shape for those who don't like rounded corners
* The app is now localized to spanish

## [v1.2] - 2024-01-25

### Added

* You can now modify the aspect ratio of existing widgets

## [v1.1.1] - 2024-01-19

### Added

* Pick multiple photos at once when configuring a widget
* Share photos directly from your gallery to create a new widget
* Editing tools when configuring a widget to move, remove and crop photos
* Click a widget on the home screen to enlarge the current photo

### Fixed

* Adding widgets via the app not working correctly on some Samsung devices
* Handling crashes that would sometimes happen when trying to import photos

## [v1.1] - 2024-01-14

### Added

* Pick multiple photos at once when configuring a widget
* Share photos directly from your gallery to create a new widget
* Editing tools when configuring a widget to move, remove and crop photos
* Click a widget on the home screen to enlarge the current photo

### Fixed

* Adding widgets via the app not working correctly on some Samsung devices

## [v1.0] - 2023-11-15

### Added

* The first release of Material Photo Widget brings 3 different aspect ratios
  and 8 different shapes for you to customize your home screen with your
  favorite photos.
