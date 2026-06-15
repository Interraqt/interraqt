import React, { useState } from 'react';
import { StyleSheet, Text, View, TextInput, TouchableOpacity, SafeAreaView, Alert } from 'react-native';
import { auth } from '../config/firebase';
import { createUserWithEmailAndPassword, signInWithEmailAndPassword, sendEmailVerification, GoogleAuthProvider, signInWithCredential } from 'firebase/auth';
import { GoogleSignin } from '@react-native-google-signin/google-signin';

// 1. Configure Native Google Sign-In using your Web Client ID
GoogleSignin.configure({
  webClientId: '220258442080-jevharg3d1m9qmfm3trn5fpao7a9seht.apps.googleusercontent.com',
});

export default function LoginScreen() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  // 2. The New Native Login Function
  const handleGoogleLogin = async () => {
    try {
      await GoogleSignin.hasPlayServices();
      const userInfo = await GoogleSignin.signIn();
      
      // Capture the token securely
      const idToken = userInfo?.data?.idToken || userInfo?.idToken;
      
      if (idToken) {
        const credential = GoogleAuthProvider.credential(idToken);
        await signInWithCredential(auth, credential);
        Alert.alert("Success", "Welcome to Interraqt via Google!");
      }
    } catch (error) {
      Alert.alert("Google Login Error", error?.message || "Something went wrong");
    }
  };

  const handleSignUp = async () => {
    if (!email || !password) return Alert.alert("Hold on", "Please enter an email and password.");
    try {
      const userCredential = await createUserWithEmailAndPassword(auth, email, password);
      await sendEmailVerification(userCredential.user);
      Alert.alert("Account Created!", "We just sent a verification link to your email.");
    } catch (error) {
      Alert.alert("Sign Up Error", error.message);
    }
  };

  const handleLogin = async () => {
    if (!email || !password) return Alert.alert("Hold on", "Please enter your email and password.");
    try {
      const userCredential = await signInWithEmailAndPassword(auth, email, password);
      if (!userCredential.user.emailVerified) {
        Alert.alert("Verify Email", "Please check your inbox and verify your email first.");
      } else {
        Alert.alert("Success", "Welcome back to Interraqt!");
      }
    } catch (error) {
      Alert.alert("Login Error", error.message);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.content}>
        <Text style={styles.logoText}>Interraqt</Text>
        <Text style={styles.subtitle}>Sign in or create an account</Text>

        <TextInput style={styles.input} placeholder="Email address" value={email} onChangeText={setEmail} keyboardType="email-address" autoCapitalize="none" />
        <TextInput style={styles.input} placeholder="Password" value={password} onChangeText={setPassword} secureTextEntry />

        <TouchableOpacity style={styles.primaryButton} onPress={handleLogin}>
          <Text style={styles.buttonText}>Log In</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.secondaryButton} onPress={handleSignUp}>
          <Text style={styles.secondaryButtonText}>Create New Account</Text>
        </TouchableOpacity>

        <View style={styles.dividerContainer}>
          <View style={styles.divider} />
          <Text style={styles.dividerText}>OR</Text>
          <View style={styles.divider} />
        </View>

        {/* Updated Native Google Button */}
        <TouchableOpacity style={styles.googleButton} onPress={handleGoogleLogin}>
          <Text style={styles.googleButtonText}>Continue with Google</Text>
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#ffffff' },
  content: { flex: 1, paddingHorizontal: 24, justifyContent: 'center' },
  logoText: { fontSize: 36, fontWeight: 'bold', color: '#0056b3', textAlign: 'center', marginBottom: 8 },
  subtitle: { fontSize: 16, color: '#666666', textAlign: 'center', marginBottom: 32 },
  input: { backgroundColor: '#f5f5f5', padding: 16, borderRadius: 8, marginBottom: 16, fontSize: 16 },
  primaryButton: { backgroundColor: '#0056b3', padding: 16, borderRadius: 8, alignItems: 'center', marginTop: 8 },
  buttonText: { color: '#ffffff', fontSize: 16, fontWeight: 'bold' },
  secondaryButton: { backgroundColor: '#e6f0fa', padding: 16, borderRadius: 8, alignItems: 'center', marginTop: 12 },
  secondaryButtonText: { color: '#0056b3', fontSize: 16, fontWeight: 'bold' },
  dividerContainer: { flexDirection: 'row', alignItems: 'center', marginVertical: 24 },
  divider: { flex: 1, height: 1, backgroundColor: '#dddddd' },
  dividerText: { marginHorizontal: 16, color: '#888888' },
  googleButton: { backgroundColor: '#ffffff', padding: 16, borderRadius: 8, alignItems: 'center', borderWidth: 1, borderColor: '#dddddd' },
  googleButtonText: { color: '#333333', fontSize: 16, fontWeight: 'bold' },
});
