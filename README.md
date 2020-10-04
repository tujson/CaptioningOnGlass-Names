# Captioning On Glass - Names
https://cog.gatech.edu

Shows names on Glass/Phone with participant's name mixed in.  
- Upon app startup, scan QR code encoded with participant's name.  
- To start, have participant click the controller (e.g. presentation clicker. This may require code modification, as controllers seem to always be slightly different).  
- When the participant sees their name, they click the controller.  
- When the app is dismissed (e.g. swipe down on Glass, navigate to home screen on Phone), the data is saved locally. The app also tries to save to Firebase Cloud Firestore if possible.