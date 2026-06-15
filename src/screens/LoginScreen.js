import React, { useState } from 'react';
import { StyleSheet, Text, View, TextInput, TouchableOpacity, SafeAreaView, Alert, KeyboardAvoidingView, Platform, ScrollView } from 'react-native';
import { auth, db } from '../config/firebase'; // 1. Added db
import { createUserWithEmailAndPassword, signInWithEmailAndPassword, sendPasswordResetEmail, GoogleAuthProvider, signInWithCredential } from 'firebase/auth';
import { doc, setDoc, getDoc } from 'firebase/firestore'; // 2. Added Firestore functions
import { GoogleSignin } from '@react-native-google-signin/google-signin';
import { Ionicons, AntDesign } from '@expo/vector-icons';

GoogleSignin.configure({
  webClientId: '220258442080-jevharg3d1m9qmfm3trn5fpao7a9seht.apps.googleusercontent.com',
});

export default function LoginScreen({ navigation }) {
  const [isLogin, setIsLogin] = useState(true);
  const [showPassword, setShowPassword] = useState(false);
  
  // Form State
  const [name, setName] = useState('');
  const [username, setUsername] = useState(''); // 3. Added username state
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const handleAuth = async () => {
    if (!email || !password) return Alert.alert("Hold on", "Please fill in your email and password.");
    
    try {
      if (isLogin) {
        // --- LOGIN ---
        await signInWithEmailAndPassword(auth, email, password);
        navigation.replace('Home');
      } else {
        // --- SIGN UP & SAVE PROFILE ---
        if (!name || !username) return Alert.alert("Required", "Please enter a Name and Username.");
        
        const userCredential = await createUserWithEmailAndPassword(auth, email, password);
        const user = userCredential.user;

        // Save extra details to Firestore Database
        await setDoc(doc(db, "users", user.uid), {
          uid: user.uid,
          name: name,
          username: username.toLowerCase().replace(/\s/g, ''),
          phone: phone,
          email: email.toLowerCase(),
          createdAt: new Date().toISOString()
        });

        navigation.replace('Home');
      }
    } catch (error) {
      Alert.alert("Authentication Error", error.message);
    }
  };

  const handleGoogleLogin = async () => {
    try {
      await GoogleSignin.hasPlayServices();
      const userInfo = await GoogleSignin.signIn();
      const idToken = userInfo?.data?.idToken || userInfo?.idToken;
      
      if (idToken) {
        const credential = GoogleAuthProvider.credential(idToken);
        const userCredential = await signInWithCredential(auth, credential);
        const user = userCredential.user;

        // Check if user already exists in Database
        const userDoc = await getDoc(doc(db, "users", user.uid));
        
        if (!userDoc.exists()) {
          // First time logging in with Google? Create a profile!
          await setDoc(doc(db, "users", user.uid), {
            uid: user.uid,
            name: user.displayName || '',
            username: user.email.split('@')[0].toLowerCase(), 
            phone: user.phoneNumber || '',
            email: user.email.toLowerCase(),
            createdAt: new Date().toISOString()
          });
        }

        navigation.replace('Home');
      }
    } catch (error) {
      Alert.alert("Google Login Error", error?.message || "Something went wrong");
    }
  };

  const handleResetPassword = async () => {
    if (!email) return Alert.alert("Reset Password", "Please type your email address in the box first.");
    try {
      await sendPasswordResetEmail(auth, email);
      Alert.alert("Check your inbox", "We sent a password reset link to your email.");
    } catch (error) {
      Alert.alert("Error", error.message);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={{ flex: 1 }}>
        <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
          
          <View style={styles.header}>
            <Text style={styles.logoText}>Interraqt</Text>
            <Text style={styles.subtitle}>{isLogin ? 'Welcome back.' : 'Create an account.'}</Text>
          </View>

          <View style={styles.formContainer}>
            
            {/* Show these only on Sign Up */}
            {!isLogin && (
              <>
                <View style={styles.inputBox}>
                  <Ionicons name="person-outline" size={20} color="#888" style={styles.icon} />
                  <TextInput style={styles.input} placeholder="Full Name" value={name} onChangeText={setName} autoCapitalize="words" />
                </View>

                <View style={styles.inputBox}>
                  <Ionicons name="at-outline" size={20} color="#888" style={styles.icon} />
                  <TextInput style={styles.input} placeholder="Username" value={username} onChangeText={setUsername} autoCapitalize="none" />
                </View>

                <View style={styles.inputBox}>
                  <Ionicons name="call-outline" size={20} color="#888" style={styles.icon} />
                  <TextInput style={styles.input} placeholder="Phone Number (Optional)" value={phone} onChangeText={setPhone} keyboardType="phone-pad" />
                </View>
              </>
            )}

            <View style={styles.inputBox}>
              <Ionicons name="mail-outline" size={20} color="#888" style={styles.icon} />
              <TextInput style={styles.input} placeholder="Email Address" value={email} onChangeText={setEmail} keyboardType="email-address" autoCapitalize="none" />
            </View>

            <View style={styles.inputBox}>
              <Ionicons name="lock-closed-outline" size={20} color="#888" style={styles.icon} />
              <TextInput style={styles.input} placeholder="Password" value={password} onChangeText={setPassword} secureTextEntry={!showPassword} />
              <TouchableOpacity onPress={() => setShowPassword(!showPassword)} style={styles.eyeIcon}>
                <Ionicons name={showPassword ? "eye-outline" : "eye-off-outline"} size={20} color="#888" />
              </TouchableOpacity>
            </View>

            {isLogin && (
              <TouchableOpacity onPress={handleResetPassword} style={styles.forgotPassword}>
                <Text style={styles.forgotPasswordText}>Forgot password?</Text>
              </TouchableOpacity>
            )}

            <TouchableOpacity style={styles.primaryButton} onPress={handleAuth}>
              <Text style={styles.primaryButtonText}>{isLogin ? 'Log In' : 'Sign Up'}</Text>
            </TouchableOpacity>

            <TouchableOpacity style={styles.switchModeBtn} onPress={() => setIsLogin(!isLogin)}>
              <Text style={styles.switchModeText}>
                {isLogin ? "Don't have an account? " : "Already have an account? "}
                <Text style={styles.switchModeTextBold}>{isLogin ? 'Sign up' : 'Log in'}</Text>
              </Text>
            </TouchableOpacity>
          </View>

          <View style={styles.dividerContainer}>
            <View style={styles.divider} />
            <Text style={styles.dividerText}>or continue with</Text>
            <View style={styles.divider} />
          </View>

          <TouchableOpacity style={styles.googleButton} onPress={handleGoogleLogin}>
            <AntDesign name="google" size={22} color="#DB4437" />
            <Text style={styles.googleButtonText}>Google</Text>
          </TouchableOpacity>

        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FAFAFA' },
  scrollContent: { flexGrow: 1, paddingHorizontal: 24, justifyContent: 'center', paddingBottom: 40 },
  header: { marginTop: 60, marginBottom: 40 },
  logoText: { fontSize: 40, fontWeight: '900', color: '#111111', letterSpacing: -1 },
  subtitle: { fontSize: 18, color: '#666666', marginTop: 8 },
  formContainer: { width: '100%' },
  inputBox: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#FFFFFF', borderWidth: 1, borderColor: '#EAEAEA', borderRadius: 12, marginBottom: 16, paddingHorizontal: 16, height: 56, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.02, shadowRadius: 4, elevation: 1 },
  icon: { marginRight: 12 },
  input: { flex: 1, fontSize: 16, color: '#333333' },
  eyeIcon: { padding: 8 },
  forgotPassword: { alignSelf: 'flex-end', marginBottom: 24 },
  forgotPasswordText: { color: '#0056b3', fontSize: 14, fontWeight: '600' },
  primaryButton: { backgroundColor: '#000000', borderRadius: 12, height: 56, justifyContent: 'center', alignItems: 'center', shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.15, shadowRadius: 8, elevation: 3, marginTop: 8 },
  primaryButtonText: { color: '#FFFFFF', fontSize: 16, fontWeight: 'bold' },
  switchModeBtn: { marginTop: 24, alignItems: 'center' },
  switchModeText: { color: '#666666', fontSize: 15 },
  switchModeTextBold: { color: '#000000', fontWeight: 'bold' },
  dividerContainer: { flexDirection: 'row', alignItems: 'center', marginVertical: 32 },
  divider: { flex: 1, height: 1, backgroundColor: '#EAEAEA' },
  dividerText: { marginHorizontal: 16, color: '#999999', fontSize: 14 },
  googleButton: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', backgroundColor: '#FFFFFF', borderWidth: 1, borderColor: '#EAEAEA', borderRadius: 12, height: 56, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.04, shadowRadius: 4, elevation: 1 },
  googleButtonText: { color: '#111111', fontSize: 16, fontWeight: '600', marginLeft: 12 },
});
