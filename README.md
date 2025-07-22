# DialiTrack  
Android app to track peritoneal dialysis via image capture and metadata analysis.

DialiTrack uses [ML Kit Text Recognition](https://developers.google.com/ml-kit/vision/text-recognition/android) to automatically read weights from the images you select. The `com.google.mlkit:text-recognition` dependency is included so the app can process text in photos.

OpenCV (`org.opencv:opencv-android`) has been added as a dependency so image preprocessing features can be implemented in the future.
