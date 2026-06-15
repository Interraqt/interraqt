import React, { useState } from 'react';
import { StyleSheet, Text, View, TextInput, TouchableOpacity, SafeAreaView, Alert, KeyboardAvoidingView, Platform, ScrollView, ActivityIndicator, LayoutAnimation, UIManager } from 'react-native';
import { auth, db } from '../config/firebase'; 
import { createUserWithEmailAndPassword, signInWithEmailAndPassword, sendPasswordResetEmail } from 'firebase/auth';
import { doc, setDoc } from 'firebase/firestore'; 
import { Feather } from '@expo/vector-icons'; // Modern Lucide-style icons

// Enable fluid layout animations for Android
if (Platform.OS === 'android' && UIManager.setLayoutAnimationEnabledExperimental) {
  UIManager.setLayoutAnimationEnabledExperimental(true);
}

export default function LoginScreen({ navigation }) {
  const [isLogin, setIsLogin] = useState(true);
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false); // Visual feedback state
  
  // Form State
  const [name, setName] = useState('');
  const [username, setUsername] = useState('');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const toggleMode = () => {
    // Smooth animation when switching between Login and Sign Up
    LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut);
    setIsLogin(!isLogin);
  };

  const handleAuth = async () => {
    if (!email || !password) return Alert.alert("Hold on", "Please fill in your email and password.");
    
    setIsLoading(true); // Start loading spinner
    
    try {
      if (isLogin) {
        await signInWithEmailAndPassword(auth, email, password);
        navigation.replace('Home');
      } else {
        if (!name || !username) {
          setIsLoading(false);
          return Alert.alert("Required", "Please enter a Name and Username.");
        }
        
        const userCredential = await createUserWithEmailAndPassword(auth, email, password);
        const user = userCredential.user;

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
      setIsLoading(false); // Stop loading if there's an error
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
            <View style={styles.iconWrapper}>
              <Feather name="zap" size={32} color="#0056b3" />
            </View>
            <Text style={styles.logoText}>Interraqt</Text>
            <Text style={styles.subtitle}>{isLogin ? 'Welcome back.' : 'Create your account.'}</Text>
          </View>

          <View style={styles.formContainer}>
            
            {!isLogin && (
              <View style={styles.expandedInputs}>
                <View style={styles.inputBox}>
                  <Feather name="user" size={20} color="#888" style={styles.icon} />
                  <TextInput style={styles.input} placeholder="Full Name" value={name} onChangeText={setName} autoCapitalize="words" editable={!isLoading} />
                </View>

                <View style={styles.inputBox}>
                  <Feather name="at-sign" size={20} color="#888" style={styles.icon} />
                  <TextInput style={styles.input} placeholder="Username" value={username} onChangeText={setUsername} autoCapitalize="none" editable={!isLoading} />
                </View>

                <View style={styles.inputBox}>
                  <Feather name="phone" size={20} color="#888" style={styles.icon} />
                  <TextInput style={styles.input} placeholder="Phone Number (Optional)" value={phone} onChangeText={setPhone} keyboardType="phone-pad" editable={!isLoading} />
                </View>
              </View>
            )}

            <View style={styles.inputBox}>
              <Feather name="mail" size={20} color="#888" style={styles.icon} />
              <TextInput style={styles.input} placeholder="Email Address" value={email} onChangeText={setEmail} keyboardType="email-address" autoCapitalize="none" editable={!isLoading} />
            </View>

            <View style={styles.inputBox}>
              <Feather name="lock" size={20} color="#888" style={styles.icon} />
              <TextInput style={styles.input} placeholder="Password" value={password} onChangeText={setPassword} secureTextEntry={!showPassword} editable={!isLoading} />
              <TouchableOpacity onPress={() => setShowPassword(!showPassword)} style={styles.eyeIcon} disabled={isLoading}>
                <Feather name={showPassword ? "eye" : "eye-off"} size={20} color="#888" />
              </TouchableOpacity>
            </View>

            {isLogin && (
              <TouchableOpacity onPress={handleResetPassword} style={styles.forgotPassword} disabled={isLoading}>
                <Text style={styles.forgotPasswordText}>Forgot password?</Text>
              </TouchableOpacity>
            )}

            <TouchableOpacity 
              style={[styles.primaryButton, isLoading && styles.primaryButtonDisabled]} 
              onPress={handleAuth}
              disabled={isLoading}
            >
              {isLoading ? (
                <ActivityIndicator color="#fff" size="small" />
              ) : (
                <Text style={styles.primaryButtonText}>{isLogin ? 'Log In' : 'Sign Up'}</Text>
              )}
            </TouchableOpacity>

            <TouchableOpacity style={styles.switchModeBtn} onPress={toggleMode} disabled={isLoading}>
              <Text style={styles.switchModeText}>
                {isLogin ? "Don't have an account? " : "Already have an account? "}
                <Text style={styles.switchModeTextBold}>{isLogin ? 'Sign up' : 'Log in'}</Text>
              </Text>
            </TouchableOpacity>
          </View>

        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#F8FAFC' },
  scrollContent: { flexGrow: 1, paddingHorizontal: 28, justifyContent: 'center', paddingBottom: 40 },
  header: { marginTop: 60, marginBottom: 40 },
  iconWrapper: { width: 56, height: 56, backgroundColor: '#EFF6FF', borderRadius: 16, alignItems: 'center', justifyContent: 'center', marginBottom: 20 },
  logoText: { fontSize: 36, fontWeight: '900', color: '#0F172A', letterSpacing: -1 },
  subtitle: { fontSize: 16, color: '#64748B', marginTop: 8 },
  formContainer: { width: '100%' },
  expandedInputs: { overflow: 'hidden' },
  inputBox: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#FFFFFF', borderWidth: 1, borderColor: '#E2E8F0', borderRadius: 16, marginBottom: 16, paddingHorizontal: 16, height: 56, shadowColor: '#64748B', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.05, shadowRadius: 10, elevation: 2 },
  icon: { marginRight: 12 },
  input: { flex: 1, fontSize: 16, color: '#0F172A' },
  eyeIcon: { padding: 8 },
  forgotPassword: { alignSelf: 'flex-end', marginBottom: 24 },
  forgotPasswordText: { color: '#0056b3', fontSize: 14, fontWeight: '600' },
  primaryButton: { backgroundColor: '#0F172A', borderRadius: 16, height: 56, justifyContent: 'center', alignItems: 'center', shadowColor: '#0F172A', shadowOffset: { width: 0, height: 8 }, shadowOpacity: 0.2, shadowRadius: 12, elevation: 4, marginTop: 8 },
  primaryButtonDisabled: { backgroundColor: '#475569', shadowOpacity: 0 },
  primaryButtonText: { color: '#FFFFFF', fontSize: 16, fontWeight: '700' },
  switchModeBtn: { marginTop: 24, alignItems: 'center' },
  switchModeText: { color: '#64748B', fontSize: 15 },
  switchModeTextBold: { color: '#0F172A', fontWeight: 'bold' },
});
