# ML Kit Translate Demo with Material Design

This app demonstrates how to build an end-to-end user experience with [Google ML Kit APIs](https://developers.google.com/ml-kit) and following the new [Material for ML design guidelines](https://material.io/collections/machine-learning/).

The goal is to make it as easy as possible to integrate ML Kit into your app with an experience that has been user tested:

* Real-time translate using the on-device Text Recognition, Language ID, Translate APIs - an end-to-end solution from text recognition to translate in live camera.

![live_translate](./demo.gif)


## Steps to run the app

1. Clone this repo locally
  ```
  git clone https://github.com/googlecodelabs/mlkit-android
  ```
2. Import the project in the `translate/starter` directory. This is the starter app that doesn't have the ML Kit functionalies implemented yet. You will need to follow the codelab [here](https://codelabs.developers.google.com/codelabs/mlkit-android-translate) in order to build out the app so that it can recognize and translate text.
3. Alternatively, if you don't want to follow the codelab to build out the app, the completed version of the app can be found [here](https://github.com/googlesamples/mlkit/tree/master/android/translate-showcase).
3. Build and run it on a physical device (the simulator isn't recommended, as the app needs to use the camera on the device).

## How to use the app

This app demonstrates live text translate using the camera:
* Open the app and point the bounding box of the camera at a text of interest. The recognized text and it's detected language will show up on the top part of the bottom sheet.
* As you keep the camera stable to recognize a text, you'll see the translated version of this text on the bottom in real-time using the on-device Translate API.
* You can also switch the translate language using the drop down menu.


## License
© Google, 2019. Licensed under an [Apache-2](./LICENSE) license.

