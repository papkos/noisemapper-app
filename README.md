# NoiseMapper App

## What's this?
This repository contains the Android application for my master's thesis project at UiO.  
The final text can be found [here](https://github.com/papkos/noisemapper-text).  
There is also a companion server application, available [here](https://github.com/papkos/noisemapper-server).

## What can Noisemapper App do?
It is an Android application, that runs in the background, records short noise snippets, 
analyzes them and then uploads them to a server.  
While recording, it also collects environment metadata, e.g. GPS, time, light, proximity.

## How to compile?
1. Create or open `~/.gradle/gradle.properties` file, and add the following lines with your data
    ```ini
    # NoiseMapper secrets
    noiseMapperDefaultHost=https://<YOUR_HOST>
    noiseMapperApiAuthRemote=<The same as you used on the server>
    noiseMapperGoogleMapsApiKey=<The one you got from Google (starts with AIza)>
    ```

2. Open project in Android Studio, then *Just works™*.

## How to use?
Copy over the APK to the device, then install.  
Go to Settings, set Device name, Host URL, tune the Repeat interval and Record duration.  
Then enable the service by toggling the "Sound snippet recording service" on the main screen.
