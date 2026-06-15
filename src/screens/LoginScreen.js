import React, { useState } from 'react';
import { StyleSheet, Text, View, TextInput, TouchableOpacity, Alert, KeyboardAvoidingView, Platform, ScrollView, ActivityIndicator, LayoutAnimation, UIManager } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { auth, db } from '../config/firebase'; 
import { createUserWithEmailAndPassword, signInWithEmailAndPassword, sendPasswordResetEmail } from 'firebase/auth';
import { doc, setDoc, collection, query, where, getDocs } from 'firebase/firestore'; 
import { Feather } from '@expo/vector-icons'; 
import { BlurView } from 'expo-blur';
import { LinearGradient } from 'expo-linear-gradient';

if (Platform.OS === 'android' && UIManager.setLayoutAnimationEnabledExperimental) {
  UIManager.setLayoutAnimationEnabledExperimental(true);
}

export default function LoginScreen({ navigation }) {
  const insets = useSafeAreaInsets();
  const [isLogin, setIsLogin] = useState(true);
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false); 
  
  const [name, setName] = useState('');
  const [username, setUsername] = useState('');
  const [phone, setPhone] = useState('');
  const [identifier, setIdentifier] = useState(''); 
  const [password, setPassword] = useState('');

  const toggleMode = () => {
    LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut);
    setIsLogin(!isLogin);
  };

  const handleAuth = async () => {
    if (!identifier || !password) return Alert.alert("Hold on", "Please fill in all fields.");
    setIsLoading(true); 
    
    try {
      if (isLogin) {
        let loginEmail = identifier.toLowerCase().trim();
        if (!loginEmail.includes('@')) {
          const usersRef = collection(db, 'users');
          const qUser = query(usersRef, where('username', '==', loginEmail));
          const snapUser = await getDocs(qUser);
          
          if (!snapUser.empty) {
            loginEmail = snapUser.docs[0].data().email;
          } else {
            const qPhone = query(usersRef, where('phone', '==', loginEmail));
            const snapPhone = await getDocs(qPhone);
            if (!snapPhone.empty) {
              loginEmail = snapPhone.docs[0].data().email;
            } else {
              setIsLoading(false);
              return Alert.alert("Not Found", "No account found.");
            }
          }
        }
        await signInWithEmailAndPassword(auth, loginEmail, password);
        navigation.replace('Home');
      } else {
        if (!name || !username) {
          setIsLoading(false);
          return Alert.alert("Required", "Please enter a Name and Username.");
        }
        const userCredential = await createUserWithEmailAndPassword(auth, identifier, password);
        const user = userCredential.user;

        await setDoc(doc(db, "users", user.uid), {
          uid: user.uid,
          name: name,
          username: username.toLowerCase().replace(/\s/g, ''),
          phone: phone,
          email: identifier.toLowerCase().trim(),
          createdAt: new Date().toISOString()
        });
        navigation.replace('Home');
      }
    } catch (error) {
      Alert.alert("Authentication Error", error.message);
      setIsLoading(false); 
    }
  };

  // NEW: Forgot Password Handler
  const handleResetPassword = async () => {
    if (!identifier || !identifier.includes('@')) {
      return Alert.alert("Email Required", "Please enter your registered email address in the field above to reset your password.");
    }
    
    setIsLoading(true);
    try {
      await sendPasswordResetEmail(auth, identifier.toLowerCase().trim());
      Alert.alert("Check your inbox", "We have sent a password reset link to " + identifier);
    } catch (error) {
      Alert.alert("Error", error.message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <View style={styles.background}>
      <View style={[styles.orb, { backgroundColor: '#007AFF', top: '5%', left: '-20%', width: 300, height: 300 }]} />
      <View style={[styles.orb, { backgroundColor: '#5856D6', top: '40%', right: '-30%', width: 250, height: 250 }]} />
      <View style={[styles.orb, { backgroundColor: '#FF2D55', bottom: '-5%', left: '10%', width: 350, height: 350 }]} />

      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={{ flex: 1 }}>
        <ScrollView contentContainerStyle={[styles.scrollContent, { paddingTop: insets.top + 20 }]} showsVerticalScrollIndicator={false} keyboardShouldPersistTaps="handled">
          
          <View style={styles.header}>
            {!isLogin && (
              <TouchableOpacity style={styles.backBtn} onPress={toggleMode}>
                <Feather name="arrow-left" size={28} color="#000" />
              </TouchableOpacity>
            )}
            <View style={styles.iconWrapper}>
              <Feather name="aperture" size={36} color="#000" />
            </View>
            <Text style={styles.logoText}>Interraqt</Text>
            <Text style={styles.subtitle}>{isLogin ? 'Welcome back.' : 'Join the network.'}</Text>
          </View>

          <BlurView intensity={80} tint="light" style={styles.glassPanel}>
            
            {!isLogin && (
              <View style={styles.expandedInputs}>
                <View style={styles.inputBox}>
                  <Feather name="user" size={20} color="#666" style={styles.icon} />
                  <TextInput style={styles.input} placeholder="Full Name" placeholderTextColor="#888" value={name} onChangeText={setName} autoCapitalize="words" editable={!isLoading} />
                </View>
                <View style={styles.inputBox}>
                  <Feather name="at-sign" size={20} color="#666" style={styles.icon} />
                  <TextInput style={styles.input} placeholder="Username" placeholderTextColor="#888" value={username} onChangeText={setUsername} autoCapitalize="none" editable={!isLoading} />
                </View>
                <View style={styles.inputBox}>
                  <Feather name="phone" size={20} color="#666" style={styles.icon} />
                  <TextInput style={styles.input} placeholder="Mobile Number" placeholderTextColor="#888" value={phone} onChangeText={setPhone} keyboardType="phone-pad" editable={!isLoading} />
                </View>
              </View>
            )}

            <View style={styles.inputBox}>
              <Feather name="mail" size={20} color="#666" style={styles.icon} />
              <TextInput 
                style={styles.input} 
                placeholder={isLogin ? "Email, Username, or Mobile" : "Email Address"} 
                placeholderTextColor="#888" 
                value={identifier} 
                onChangeText={setIdentifier} 
                autoCapitalize="none" 
                editable={!isLoading} 
              />
            </View>

            <View style={styles.inputBox}>
              <Feather name="lock" size={20} color="#666" style={styles.icon} />
              <TextInput style={styles.input} placeholder="Password" placeholderTextColor="#888" value={password} onChangeText={setPassword} secureTextEntry={!showPassword} editable={!isLoading} />
              <TouchableOpacity onPress={() => setShowPassword(!showPassword)} style={styles.eyeIcon} disabled={isLoading}>
                <Feather name={showPassword ? "eye" : "eye-off"} size={20} color="#666" />
              </TouchableOpacity>
            </View>

            {isLogin && (
              <TouchableOpacity style={styles.forgotPassword} onPress={handleResetPassword} disabled={isLoading}>
                <Text style={styles.forgotPasswordText}>Forgot password?</Text>
              </TouchableOpacity>
            )}

            <TouchableOpacity style={styles.primaryButton} onPress={handleAuth} disabled={isLoading}>
              {isLoading ? (
                <ActivityIndicator color="#FFF" size="small" />
              ) : (
                <Text style={styles.primaryButtonText}>{isLogin ? 'Log In' : 'Sign Up'}</Text>
              )}
            </TouchableOpacity>

            {isLogin && (
              <TouchableOpacity style={styles.switchModeBtn} onPress={toggleMode} disabled={isLoading}>
                <Text style={styles.switchModeText}>Don't have an account? <Text style={styles.switchModeTextBold}>Sign up</Text></Text>
              </TouchableOpacity>
            )}
          </BlurView>

        </ScrollView>
      </KeyboardAvoidingView>
    </View>
  );
}

const styles = StyleSheet.create({
  background: { flex: 1, backgroundColor: '#F2F2F7' },
  orb: { position: 'absolute', borderRadius: 999, opacity: 0.35 }, 
  scrollContent: { flexGrow: 1, paddingHorizontal: 24, justifyContent: 'center', paddingBottom: 40 },
  header: { marginBottom: 40, alignItems: 'center' },
  backBtn: { position: 'absolute', top: 10, left: 0, padding: 10, zIndex: 10 },
  iconWrapper: { width: 72, height: 72, backgroundColor: '#FFF', borderRadius: 24, alignItems: 'center', justifyContent: 'center', marginBottom: 20, shadowColor: '#000', shadowOffset: { width: 0, height: 8 }, shadowOpacity: 0.1, shadowRadius: 16, elevation: 5 },
  logoText: { fontSize: 36, fontWeight: '900', color: '#000', letterSpacing: -1 },
  subtitle: { fontSize: 16, color: '#666', marginTop: 8, fontWeight: '500' },
  glassPanel: { borderRadius: 32, padding: 24, borderWidth: 1, borderColor: 'rgba(255,255,255,0.8)', overflow: 'hidden', backgroundColor: 'rgba(255, 255, 255, 0.4)' },
  expandedInputs: { overflow: 'hidden' },
  inputBox: { flexDirection: 'row', alignItems: 'center', backgroundColor: 'rgba(255, 255, 255, 0.7)', borderWidth: 1, borderColor: 'rgba(255, 255, 255, 1)', borderRadius: 16, marginBottom: 16, paddingHorizontal: 16, height: 60, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 1 },
  icon: { marginRight: 12 },
  input: { flex: 1, fontSize: 16, color: '#000', fontWeight: '500' },
  eyeIcon: { padding: 8 },
  forgotPassword: { alignSelf: 'flex-end', marginBottom: 24 },
  forgotPasswordText: { color: '#000', fontSize: 14, fontWeight: '700' },
  primaryButton: { backgroundColor: '#000', borderRadius: 16, height: 60, justifyContent: 'center', alignItems: 'center', marginTop: 8, shadowColor: '#000', shadowOffset: { width: 0, height: 8 }, shadowOpacity: 0.2, shadowRadius: 12, elevation: 5 },
  primaryButtonText: { color: '#FFF', fontSize: 16, fontWeight: '800' },
  switchModeBtn: { marginTop: 24, alignItems: 'center', padding: 10 },
  switchModeText: { color: '#444', fontSize: 15, fontWeight: '500' },
  switchModeTextBold: { color: '#000', fontWeight: '800' },
});
