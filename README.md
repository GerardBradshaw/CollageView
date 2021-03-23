# Collage View
A view for Android that shows multiple, resizable squares.

## Screenshots
The best way to get to know the library is to start using it. The screenshot on the left is from the example app, and right is from MixUp on the [Play Store](https://play.google.com/store/apps/details?id=com.gerardbradshaw.v2mixup).
<img src="/art/screenshot.png?raw=true" width="400px"> <img src="/art/screenshot_mix_up.png?raw=true" width="400px">

## Features
The out-of-box features of this library include:
- Long press on the corner or edge of an image or border to start resizing it
- onClick listeners for each view
- Border on/off toggle
- Customisable border colors

## Suggested Features to Add
You can extend this library in whatever way you like! With your own tweaks, you could:
- Add references to each image view so they can be set programatically.
- Add more layout types for collages with even more pictures

## Dependency
To use the library, add the following to your Gradle build files:
```groovy
allprojects { 
  repositories {
    maven {url "https://jitpack.io" }
  }
}

dependencies {
  implementation "com.github.GerardBradshaw:CollageView:1.0.0-beta0"
}
```

## Building from Source
You can build this library directly from the source code. Enter the terminal command below to clone this repo:
```shell
git clone https://github.com/GerardBradshaw/CollageView.git  
```

## Compatibility
This library is compatible with SDK21+.

## License
This library is available under the Apache Licence 2.0. See the LICENSE file for more info.