# Modern AR Furniture Planner

An interactive Android Augmented Reality application that allows users to seamlessly design and visualize interior spaces. Built natively with Kotlin and Jetpack Compose, this app leverages ARCore and Sceneview to place, manipulate, and capture high-fidelity 3D furniture models in the real world.

### Video
https://github.com/user-attachments/assets/614b1f65-1840-4342-91f8-1d3af42c9963

### Key Features
* **Real-World Surface Tracking:** Utilizes ARCore SLAM to detect physical floors and lighting conditions.
* **Interactive Decorator Mode:** Users can intuitively manipulate 3D assets using native touch gestures (1-finger drag, 2-finger twist to rotate, pinch to scale).
* **Dynamic Material 3 Catalog:** Features a sleek, expandable bottom-sheet UI to browse retail 3D assets (glTF format) fetched asynchronously from the cloud.
* **In-App Capture & Share:** Implements a hardware-accelerated `PixelCopy` pipeline to take clean, UI-free snapshots of mixed-reality room layouts and push them to the Android Share Sheet.
* **Multi-Object Instantiation:** Supports rendering and independently tracking multiple heavy 3D assets simultaneously without physics drifting.

### Tech Stack
* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (Material 3)
* **AR Engine:** Sceneview `0.10.0` (ARCore & Filament C++ Backend)
* **3D Assets:** Khronos Group glTF Sample Models

### To Use it on your device
1. Clone this repository.
2. Open the project in **Android Studio**.
3. Ensure you have an ARCore-supported physical Android device (Emulators do not support hardware camera tracking).
4. Build and deploy directly via USB or Wireless Debugging.

### To Use it easily through apk
Go to <a href="https://github.com/Skanda098/AR-Furniture-Planner/releases"> Releases</a> and download the latest version apk from there

### Screenshots
<img width="270" height="598" alt="S2" src="https://github.com/user-attachments/assets/df9718f0-baaa-43be-adca-90604dd917aa" />
<img width="270" height="598" alt="S1" src="https://github.com/user-attachments/assets/767dd152-91ac-409b-92ae-3e0849bd513b" />

