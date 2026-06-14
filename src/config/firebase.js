import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";

// REPLACE THESE WITH YOUR ACTUAL KEYS FROM FIREBASE
const firebaseConfig = {
  apiKey: "AIzaSyC5BoKyE1T8Wd-foZXYoJi1dRlnHq5jQX8",
  authDomain: "interraqt-45338.firebaseapp.com",
  projectId: "interraqt-45338",
  storageBucket: "interraqt-45338.firebasestorage.app",
  messagingSenderId: "220258442080",
  appId: "1:220258442080:web:dc7d590c9e4b5dd480dc05"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
