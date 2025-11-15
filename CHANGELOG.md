# Changelog

All notable user-facing changes to this app will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

## [v1.34.0] - 2025-11-15
[v1.34.0]: https://github.com/fibelatti/photo-widget/releases/tag/v1.34.0

### Added

* New action: share photos as a tap action or from the full screen viewer

## [v1.33.1] - 2025-10-11
[v1.33.1]: https://github.com/fibelatti/photo-widget/releases/tag/v1.33.1

### Added

* Prominent warning about background restrictions when configuring the timer

### Fixed

* Fix the crash affecting the 8-leaf clover shape
* Fix the crash when tapping back to leave the tap action picker
* Fix the tap action picker back navigation icon appearance in dark mode

## [v1.33.0] - 2025-10-04
[v1.33.0]: https://github.com/fibelatti/photo-widget/releases/tag/v1.33.0

### Added

* Collect crash reports locally for optional reporting

### Changed

* Update how the image format is detected for better handling of pngs
* Allow locking widgets even when the timer is disabled
* Improve the usability of the tap action picker

## [v1.32.3] - 2025-09-07
[v1.32.3]: https://github.com/fibelatti/photo-widget/releases/tag/v1.32.3

### Changed

* Remove app from recent apps after dismissing the full screen viewer

## [v1.32.2] - 2025-08-13
[v1.32.2]: https://github.com/fibelatti/photo-widget/releases/tag/v1.32.2

### Fixed

* Fixed the full screen viewer not behaving as expected

## [v1.32.1] - 2025-08-12
[v1.32.1]: https://github.com/fibelatti/photo-widget/releases/tag/v1.32.1

### Fixed

* Fixed the appearance of the chosen tap actions in the configuration screen

## [v1.32.0] - 2025-08-12
[v1.32.0]: https://github.com/fibelatti/photo-widget/releases/tag/v1.32.0

### Added

* Added rotation controls to the crop tool
* Added the option to create a backup file to export photo widgets
* Added Tagalog
* Added support for .webp images when syncing folders

### Changed

* Revised the timer intervals to make them more flexible

## [v1.31.5] - 2025-07-26
[v1.31.5]: https://github.com/fibelatti/photo-widget/releases/tag/v1.31.5

### Fixed

* Fixed the multitouch behavior of the new crop tool
* Fixed the image quality of really large images

## [v1.31.4] - 2025-07-25
[v1.31.4]: https://github.com/fibelatti/photo-widget/releases/tag/v1.31.4

### Changed

* Lifted the tap action restriction from 1x1 widgets
* Replaced the cropping library

### Fixed

* Include the existing translations for German, Italian, Hebrew and Japanese
* Fixed scheduled photo cycling not working as expected

## [v1.31.3] - 2025-07-15
[v1.31.3]: https://github.com/fibelatti/photo-widget/releases/tag/v1.31.3

### Changed

* Reverted an internal change that could be leading to widgets not behaving as
  expected (like their actions getting stuck)

### Fixed

* Fixed a crash that could affect the Shapes format

## [v1.31.2] - 2025-07-10
[v1.31.2]: https://github.com/fibelatti/photo-widget/releases/tag/v1.31.2

### Added

* It's now possible to set a default format through settings

### Changed

* Using work manager to try to get around OEM restrictions (auto-start)
* Selected folders are now synced every 6 hours

## [v1.31.1] - 2025-06-25
[v1.31.1]: https://github.com/fibelatti/photo-widget/releases/tag/v1.31.1

### Changed

* Increased the max roundness from 64 to 128

### Fixed

* The roundness preview now matches the actual roundness of the widget
* Some shapes would crash the app and not be applied as expected

## [v1.31.0] - 2025-06-15
[v1.31.0]: https://github.com/fibelatti/photo-widget/releases/tag/v1.31.0

### Added

* Many new shapes
* Japanese, German and Italian localization

### Changed

* Updated the UI with new Material Expressive elements

### Fixed

* Certain images having unexpected borders in the full screen viewer
* Provide better placeholders while widgets are loading
* "My Widgets" filter not working as expected
* Slideshow not working in certain conditions

## [v1.30.0] - 2025-05-25
[v1.30.0]: https://github.com/fibelatti/photo-widget/releases/tag/v1.30.0

### Added

* In-app link to Crowdin (crowdsourced localization)
* Customization option to sort the directory photos

### Changed

* Updated the explanation for the "View with a gallery app" tap action
* Improved the full screen viewer
* It's now possible to choose a tap action for each tap area

### Removed

* Default tap action setting

## [v1.29.0] - 2025-04-14
[v1.29.0]: https://github.com/fibelatti/photo-widget/releases/tag/v1.29.0

### Added

* New tap action: choose the next photo
* New options to the "View photo in full screen" tap action

### Changed

* Fill widgets can no longer have their borders and corners customized

## [v1.28.4] - 2025-04-09
[v1.28.4]: https://github.com/fibelatti/photo-widget/releases/tag/v1.28.4

### Changed

* Show a confirmation message when the widget was added to the home screen
* Update resizing logic that's affecting Fill widgets

### Fixed

* Crashes when handling certain callbacks from the system

## [v1.28.3] - 2025-04-06
[v1.28.3]: https://github.com/fibelatti/photo-widget/releases/tag/v1.28.3

### Changed

* Always measure Fill widgets as if in portrait mode

## [v1.28.2] - 2025-04-05
[v1.28.2]: https://github.com/fibelatti/photo-widget/releases/tag/v1.28.2

### Changed

* Add warning to phones that don't support widget pinning

### Fixed

* Fix wrong fill format zooming and borders on tablets

## [v1.28.1] - 2025-03-30
[v1.28.1]: https://github.com/fibelatti/photo-widget/releases/tag/v1.28.1

### Changed

* Update palette border type to use dominant, vibrant and muted colors instead

## [v1.28.0] - 2025-03-29
[v1.28.0]: https://github.com/fibelatti/photo-widget/releases/tag/v1.28.0

### Added

* Add "sync" button to the My Widgets menu to fetch folder photos on demand
* New border type that matches the current photo

### Changed

* Update configure screen to use the visual format picker
* Open configure screen in the current photo when editing a widget
* Show the selected photo when configuring aspects of the widget

### Fixed

* Fix tap action not working on tiny widgets

## [v1.27.0] - 2025-03-22
[v1.27.0]: https://github.com/fibelatti/photo-widget/releases/tag/v1.27.0

### Added

* New square aspect ratio with support for rounded corners

### Changed

* Update saturation picker to allow increasing the saturation

## [v1.26.1] - 2025-03-17
[v1.26.1]: https://github.com/fibelatti/photo-widget/releases/tag/v1.26.1

### Fixed

* Fix brightness picker title

## [v1.26.0] - 2025-03-17
[v1.26.0]: https://github.com/fibelatti/photo-widget/releases/tag/v1.26.0

### Added

* Brightness picker
* Ability to "lock" widgets with the current photo in-app

### Changed

* Replaced the black and white toggle with a saturation picker for granular
  control

### Fixed

* Fix the behavior of tapping to view the previous photo when shuffle is on

## [v1.25.2] - 2025-03-07
[v1.25.2]: https://github.com/fibelatti/photo-widget/releases/tag/v1.25.2

### Changed

* Open the default gallery using a new task
* Display the troubleshooting article on Samsung and Motorola when battery is
  restricted

### Fixed

* Attempt to fix an issue that causes "Fill" widgets to zoom in the image

## [v1.25.1] - 2025-02-23
[v1.25.1]: https://github.com/fibelatti/photo-widget/releases/tag/v1.25.1

### Fixed

* Fix preferred gallery picker choice not being saved

## [v1.25.0] - 2025-02-23
[v1.25.0]: https://github.com/fibelatti/photo-widget/releases/tag/v1.25.0

### Added

* Add support for HEIC/HEIF file format when syncing folders
* Add preferred gallery picker when using "View with a gallery app"
* Add back "View next photo" tap action for 1x1 widgets

### Changed

* Update how the corner radius is calculated to better match what's seen in the
  picker
* Update the base multiplier used to adjust the widget padding and offset

### Fixed

* Fix rounded corners not working on "Fill" widgets

## [v1.24.2] - 2025-02-18
[v1.24.2]: https://github.com/fibelatti/photo-widget/releases/tag/v1.24.2

### Added

* New option to keep widgets indefinitely after removing them

### Fixed

* Fix editor preview not showing updates when setting up the widget

## [v1.24.1] - 2025-02-16
[v1.24.1]: https://github.com/fibelatti/photo-widget/releases/tag/v1.24.1

### Fixed

* Fix which dynamic color is applied to the widget border
* Fix cropping folder photos
* Attempt to fix "View in gallery" showing the app chooser

## [v1.24.0] - 2025-02-13
[v1.24.0]: https://github.com/fibelatti/photo-widget/releases/tag/v1.24.0

### Added

* Dynamic borders that match the device's theme color (Android 12 and above)
* Background restriction warning and instructions for troubleshooting
* Fill aspect ratio warning regarding OS imposed corner radius

### Changed

* Update the home screen with better visual cues for each aspect ratio
* Review tap action picker instructions to make them clearer

### Fixed

* Fix an issue where "View in gallery" would sometimes open an unexpected app
* Fix the interval behavior after manually switching the widget photo
* Fix how borders are calculated to better match what's seen in the picker

## [v1.23.0] - 2025-01-25
[v1.23.0]: https://github.com/fibelatti/photo-widget/releases/tag/v1.23.0

### Added

* New shape: star

### Changed

* Gave the heart shape a bit of love
* Enable border customization when using "fill widget area"

### Fixed

* Fix a crash that would sometimes happen when using "fill widget area"

## [v1.22.1] - 2025-01-20
[v1.22.1]: https://github.com/fibelatti/photo-widget/releases/tag/v1.22.1

### Changed

* Enable customizing the corner radius when using fill widget area

### Fixed

* Fix changes to the cycling interval not taking effect immediately when
  editing an existing widget
* Revert how the current photo is sent to the system to fix an ongoing issue
  that's affecting some phone manufacturers
* Fix an issue that would skip the first photo once the cycling has been
  complete

## [v1.22.0] - 2025-01-19
[v1.22.0]: https://github.com/fibelatti/photo-widget/releases/tag/v1.22.0

### Changed

* Update the widget configuration screen to organize settings into categories
* Using a custom "Padding" is no longer allowed when using "Fill Widget Area"

### Fixed

* Fix an issue where trying to permanently delete a widget from "My Widgets"
  would not work as expected
* Attempt to fix widgets not working on GoodLock

## [v1.21.1] - 2025-01-13
[v1.21.1]: https://github.com/fibelatti/photo-widget/releases/tag/v1.21.1

### Fixed

* Fixed a concurrency issue that could affect widgets after updating the app
  or restarting the device

## [v1.21.0] - 2025-01-13
[v1.21.0]: https://github.com/fibelatti/photo-widget/releases/tag/v1.21.0

### Added

* New option for black and white widgets

### Fixed

* Fixed an issue that could lead to a crash when trying to edit widgets synced
  with a folder
* Fixed an issue that could lead to a crash when requesting to add a widget to
  the home screen

## [v1.20.1] - 2025-01-11
[v1.20.1]: https://github.com/fibelatti/photo-widget/releases/tag/v1.20.1

### Changed

* Change how the widget is updated to improve resource usage and tentatively
  fix crashes that are happening on low end devices
* Update widget loading logic to always load from source on the background
* Update folder selection logic to avoid loading photos twice

## [v1.20.0] - 2025-01-05
[v1.20.0]: https://github.com/fibelatti/photo-widget/releases/tag/v1.20.0

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
[v1.19.0]: https://github.com/fibelatti/photo-widget/releases/tag/v1.19.0

### Added

* Colored borders
* New setting to control the compression behavior when importing photos
* New option to import from an existing widget when setting up another
* New setting to grant exact alarms permission

### Changed

* It's now possible to exclude photos from widgets synced to folders
* Updated target SDK to 35 (Android 15)

## [v1.18.1] - 2024-11-07
[v1.18.1]: https://github.com/fibelatti/photo-widget/releases/tag/v1.18.1

### Fixed

* The loading icon would be visible behind transparent widgets

## [v1.18.0] - 2024-11-06
[v1.18.0]: https://github.com/fibelatti/photo-widget/releases/tag/v1.18.0

### Added

* New tap action to pause/resume automatic photo cycling
* New app theme option for using a true black background

### Changed

* Skip the unsaved changes warning when there are no changes
* Added device details to bug reports

## [v1.17.0] - 2024-10-27
[v1.17.0]: https://github.com/fibelatti/photo-widget/releases/tag/v1.17.0

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
[v1.16.2]: https://github.com/fibelatti/photo-widget/releases/tag/v1.16.2

### Changed

* Apply signing conditionally to avoid interfering with Reproducible Builds

## [v1.16.1] - 2024-10-22
[v1.16.1]: https://github.com/fibelatti/photo-widget/releases/tag/v1.16.1

### Changed

* Automated the release build for increased reliability in regards to
  Reproducible Builds

## [v1.16.0] - 2024-10-22
[v1.16.0]: https://github.com/fibelatti/photo-widget/releases/tag/v1.16.0

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
[v1.15.2]: https://github.com/fibelatti/photo-widget/releases/tag/v1.15.2

### Fixed

* Fix drag to reorder crash

## [v1.15.1] - 2024-10-06
[v1.15.1]: https://github.com/fibelatti/photo-widget/releases/tag/v1.15.1

### Fixed

* Fix buttons not working in the full screen viewer

### Changed

* Reduce the initial scale of the full screen viewer
* Update the shortcut used to grant exact alarm permission

## [v1.15.0] - 2024-09-29
[v1.15.0]: https://github.com/fibelatti/photo-widget/releases/tag/v1.15.0

### Added

* It's now possible to set a cycle interval in days
* Added more help articles for troubleshooting

### Changed

* You can now reorder photos by dragging them when configuring a widget. Long
  press a photo and start dragging

## [v1.14.0] - 2024-09-14
[v1.14.0]: https://github.com/fibelatti/photo-widget/releases/tag/v1.14.0

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
[v1.13.3]: https://github.com/fibelatti/photo-widget/releases/tag/v1.13.3

### Fixed

* Fix shaped widgets crashing

## [v1.13.2] - 2024-08-25
[v1.13.2]: https://github.com/fibelatti/photo-widget/releases/tag/v1.13.2

### Changed

* Improve the loading performance of the "My Widgets" screen

### Fixed

* Improved image quality of widgets
* App shortcut tap action sometimes not working as expected

## [v1.13.1] - 2024-08-14
[v1.13.1]: https://github.com/fibelatti/photo-widget/releases/tag/v1.13.1

### Changed

* The photo count of each directory and subdirectory is now check independently

### Fixed

* Fixed a scenario where all photos of an existing widget could be deleted
* Fixed a scenario where a broken widget was displayed on the widget list

## [v1.13.0] - 2024-08-10
[v1.13.0]: https://github.com/fibelatti/photo-widget/releases/tag/v1.13.0

### Added

* Removed widgets can be restored if they've been deleted for less than a week

### Changed

* Update the photo/folder picker to show a hint
* Made the required adjustments to support Reproducible Builds

## [v1.12.1] - 2024-07-22
[v1.12.1]: https://github.com/fibelatti/photo-widget/releases/tag/v1.12.1

### Fixed

* Fix error handling when a photo is not found

## [v1.12.0] - 2024-07-20
[v1.12.0]: https://github.com/fibelatti/photo-widget/releases/tag/v1.12.0

### Added

* New aspect ratio to fill the widget area

### Changed

* The full screen viewer control hints are only shown once

### Fixed

* Avoid crashing when the file cannot be found

## [v1.11.0] - 2024-07-07
[v1.11.0]: https://github.com/fibelatti/photo-widget/releases/tag/v1.11.0

### Added

* Turkish translation

### Changed

* Make it possible to bypass the folder limit

## [v1.10.0] - 2024-06-23
[v1.10.0]: https://github.com/fibelatti/photo-widget/releases/tag/v1.10.0

### Added

* Make it possible to view the previous photo on tap
* Add "View in gallery" as a tap action
* Make it possible to adjust the widget padding
* Make it possible to duplicate existing widgets

### Changed

* Declare INSTALL_SHORTCUT attempting to solve Xiaomi issues

## [v1.9.1] - 2024-06-11
[v1.9.1]: https://github.com/fibelatti/photo-widget/releases/tag/v1.9.1

### Fixed

* Intervals in seconds not working correctly

## [v1.9.0] - 2024-06-10
[v1.9.0]: https://github.com/fibelatti/photo-widget/releases/tag/v1.9.0

### Added

* Add support for intervals in seconds
* Add support for widget opacity
* Add support for widget offset

### Changed

* Updated bottom nav bar icons to better represent the selected state
* Updated the button structure of the source picker dialog
* Widgets will now include subdirectories of all synced directories

## [v1.8.3-floss] - 2024-05-22
[v1.8.3-floss]: https://github.com/fibelatti/photo-widget/releases/tag/v1.8.3-floss

### Changed

* Adjust binary contents to make the app compatible with IzzyOnDroid
* Updated bottom nav bar icons to better represent the selected state
* Updated the button structure of the source picker dialog

## [v1.8.3] - 2024-05-18
[v1.8.3]: https://github.com/fibelatti/photo-widget/releases/tag/v1.8.3

### Added

* Feedback and Privacy Policy shortcuts on the settings screen

### Fixed

* Sometimes widgets would not work correctly after a device restart

## [v1.8.2] - 2024-05-15
[v1.8.2]: https://github.com/fibelatti/photo-widget/releases/tag/v1.8.2

### Changed

* Added explanation for the widget defaults

### Fixed

* Crash that could happen when loading widgets

## [v1.8.1] - 2024-05-13
[v1.8.1]: https://github.com/fibelatti/photo-widget/releases/tag/v1.8.1

### Fixed

* Brightness setting now being applied correctly

## [v1.8] - 2024-05-13
[v1.8]: https://github.com/fibelatti/photo-widget/releases/tag/v1.8

### Added

* Revamped home screen, including a new "My Widgets" screen to make it easier
  to view and edit existing widgets
* Default widget settings
* New tap action: set the widget as an app shortcut

### Changed

* Made the brightness increase optional when using the full screen viewer
* Share photos to an existing widget

## [v1.7] - 2024-05-04
[v1.7]: https://github.com/fibelatti/photo-widget/releases/tag/v1.7

### Added

* Sync widgets with multiple folders. The limit per folder was also increased
  to 1000 photos
* The full screen viewer now shows the original photo (in increased brightness
  for those using widgets to display their club cards)
* You can now view the next and previous photos from within the full screen
  viewer
* Heart shape for square widgets

## [v1.6.1] - 2024-04-17
[v1.6.1]: https://github.com/fibelatti/photo-widget/releases/tag/v1.6.1

* Folder based widgets — choose a folder to have all its photos automatically
  added to a widget
* Shuffle — flip through the widget photos in a random order
* Disable auto-flipping — you can now disable the automatic flipping of widgets

### Changed

* This update also includes a bunch of small adjustments to the interface to
  better accommodate all controls

## [v1.6] - 2024-04-17
[v1.6]: https://github.com/fibelatti/photo-widget/releases/tag/v1.6

### Added

* Folder based widgets — choose a folder to have all its photos automatically
  added to a widget
* Shuffle — flip through the widget photos in a random order
* Disable auto-flipping — you can now disable the automatic flipping of widgets

### Changed

* This update also includes a bunch of small adjustments to the interface to
  better accommodate all controls

## [v1.5] - 2024-03-18
[v1.5]: https://github.com/fibelatti/photo-widget/releases/tag/v1.5

### Added

* Flexible intervals — you can finally configure widgets to change photos every
  1 minute!
* Original aspect ratio — configure a widget to respect the original aspect
  ratio of the photo
* No tap action — for those who just want static widgets, with no actions

## [v1.4] - 2024-02-24
[v1.4]: https://github.com/fibelatti/photo-widget/releases/tag/v1.4

### Added

* Choose the tap action of each widget — View in full screen or switch to the
  next photo
* You can now zoom and pan photos when tapping to view a widget in full screen
* More flipping intervals — every 2 and 8 hours added to the options

## [v1.3] - 2024-02-02
[v1.3]: https://github.com/fibelatti/photo-widget/releases/tag/v1.3

### Added

* You can now customize the rounded corners of tall and wide widgets
* A fully square shape for those who don't like rounded corners
* The app is now localized to spanish

## [v1.2] - 2024-01-25
[v1.2]: https://github.com/fibelatti/photo-widget/releases/tag/v1.2

### Added

* You can now modify the aspect ratio of existing widgets

## [v1.1.1] - 2024-01-19
[v1.1.1]: https://github.com/fibelatti/photo-widget/releases/tag/v1.1.1

### Added

* Pick multiple photos at once when configuring a widget
* Share photos directly from your gallery to create a new widget
* Editing tools when configuring a widget to move, remove and crop photos
* Click a widget on the home screen to enlarge the current photo

### Fixed

* Adding widgets via the app not working correctly on some Samsung devices
* Handling crashes that would sometimes happen when trying to import photos

## [v1.1] - 2024-01-14
[v1.1]: https://github.com/fibelatti/photo-widget/releases/tag/v1.1

### Added

* Pick multiple photos at once when configuring a widget
* Share photos directly from your gallery to create a new widget
* Editing tools when configuring a widget to move, remove and crop photos
* Click a widget on the home screen to enlarge the current photo

### Fixed

* Adding widgets via the app not working correctly on some Samsung devices

## [v1.0] - 2023-11-15
[v1.0]: https://github.com/fibelatti/photo-widget/releases/tag/v1.0

### Added

* The first release of Material Photo Widget brings 3 different aspect ratios
  and 8 different shapes for you to customize your home screen with your
  favorite photos.
