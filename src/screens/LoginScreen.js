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
              return Alert.alert("Not Found", "No account found with that username or phone number.");
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
    <View style={styles.container}>
      {/* SOFT LIGHT BACKGROUND */}
      <LinearGradient colors={['#F8FAFC', '#E2E8F0', '#CBD5E1']} style={StyleSheet.absoluteFillObject} />
      
      {/* SOFT PASTEL ORBS FOR GLASS REFRACTION */}
      <View style={[styles.orb, { backgroundColor: '#93C5FD', top: '-5%', left: '-10%', width: 250, height: 250 }]} />
      <View style={[styles.orb, { backgroundColor: '#C4B5FD', bottom: '10%', right: '-10%', width: 300, height: 300 }]} />

      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={{ flex: 1 }}>
        <ScrollView contentContainerStyle={[styles.scrollContent, { paddingTop: insets.top + 20 }]} showsVerticalScrollIndicator={false} keyboardShouldPersistTaps="handled">
          
          <View style={styles.header}>
            {!isLogin && (
              <TouchableOpacity style={styles.backBtn} onPress={toggleMode}>
                <Feather name="arrow-left" size={28} color="#0F172A" />
              </TouchableOpacity>
            )}
            <View style={styles.iconWrapper}>
              <Feather name="aperture" size={36} color="#0F172A" />
            </View>
            <Text style={styles.logoText}>Interraqt</Text>
            <Text style={styles.subtitle}>{isLogin ? 'Welcome back.' : 'Join the network.'}</Text>
          </View>

          {/* LIGHT FROSTED GLASS PANEL */}
          <BlurView intensity={60} tint="light" style={styles.glassPanel}>
            
            {!isLogin && (
              <View style={styles.expandedInputs}>
                <View style={styles.inputBox}>
                  <Feather name="user" size={20} color="#64748B" style={styles.icon} />
                  <TextInput style={styles.input} placeholder="Full Name" placeholderTextColor="#94A3B8" value={name} onChangeText={setName} autoCapitalize="words" editable={!isLoading} />
                </View>
                <View style={styles.inputBox}>
                  <Feather name="at-sign" size={20} color="#64748B" style={styles.icon} />
                  <TextInput style={styles.input} placeholder="Username" placeholderTextColor="#94A3B8" value={username} onChangeText={setUsername} autoCapitalize="none" editable={!isLoading} />
                </View>
                <View style={styles.inputBox}>
                  <Feather name="phone" size={20} color="#64748B" style={styles.icon} />
                  <TextInput style={styles.input} placeholder="Mobile Number (Optional)" placeholderTextColor="#94A3B8" value={phone} onChangeText={setPhone} keyboardType="phone-pad" editable={!isLoading} />
                </View>
              </View>
            )}

            <View style={styles.inputBox}>
              <Feather name="mail" size={20} color="#64748B" style={styles.icon} />
              <TextInput 
                style={styles.input} 
                placeholder={isLogin ? "Email, Username, or Mobile" : "Email Address"} 
                placeholderTextColor="#94A3B8" 
                value={identifier} 
                onChangeText={setIdentifier} 
                autoCapitalize="none" 
                editable={!isLoading} 
              />
            </View>

            <View style={styles.inputBox}>
              <Feather name="lock" size={20} color="#64748B" style={styles.icon} />
              <TextInput style={styles.input} placeholder="Password" placeholderTextColor="#94A3B8" value={password} onChangeText={setPassword} secureTextEntry={!showPassword} editable={!isLoading} />
              <TouchableOpacity onPress={() => setShowPassword(!showPassword)} style={styles.eyeIcon} disabled={isLoading}>
                <Feather name={showPassword ? "eye" : "eye-off"} size={20} color="#64748B" />
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
  container: { flex: 1 },
  orb: { position: 'absolute', borderRadius: 999, opacity: 0.6 }, 
  scrollContent: { flexGrow: 1, paddingHorizontal: 24, justifyContent: 'center', paddingBottom: 40 },
  
  header: { marginBottom: 40, alignItems: 'center' },
  backBtn: { position: 'absolute', top: 10, left: 0, padding: 10, zIndex: 10 },
  iconWrapper: { width: 72, height: 72, backgroundColor: 'rgba(255,255,255,0.6)', borderRadius: 24, alignItems: 'center', justifyContent: 'center', marginBottom: 20, borderWidth: 1, borderColor: '#FFF', shadowColor: '#000', shadowOffset: { width: 0, height: 8 }, shadowOpacity: 0.05, shadowRadius: 16, elevation: 2 },
  logoText: { fontSize: 36, fontWeight: '900', color: '#0F172A', letterSpacing: -1 },
  subtitle: { fontSize: 16, color: '#475569', marginTop: 8, fontWeight: '500' },
  
  glassPanel: { borderRadius: 32, padding: 24, borderWidth: 1.5, borderColor: 'rgba(255,255,255,0.9)', overflow: 'hidden', backgroundColor: 'rgba(255, 255, 255, 0.45)' },
  expandedInputs: { overflow: 'hidden' },
  
  inputBox: { flexDirection: 'row', alignItems: 'center', backgroundColor: 'rgba(255, 255, 255, 0.7)', borderWidth: 1, borderColor: '#FFFFFF', borderRadius: 16, marginBottom: 16, paddingHorizontal: 16, height: 60, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.03, shadowRadius: 4, elevation: 1 },
  icon: { marginRight: 12 },
  input: { flex: 1, fontSize: 16, color: '#0F172A', fontWeight: '500' },
  eyeIcon: { padding: 8 },
  
  forgotPassword: { alignSelf: 'flex-end', marginBottom: 24 },
  forgotPasswordText: { color: '#0F172A', fontSize: 14, fontWeight: '700' },
  
  primaryButton: { backgroundColor: '#0F172A', borderRadius: 16, height: 60, justifyContent: 'center', alignItems: 'center', marginTop: 8, shadowColor: '#0F172A', shadowOffset: { width: 0, height: 8 }, shadowOpacity: 0.25, shadowRadius: 16, elevation: 5 },
  primaryButtonText: { color: '#FFF', fontSize: 16, fontWeight: '800' },
  
  switchModeBtn: { marginTop: 24, alignItems: 'center', padding: 10 },
  switchModeText: { color: '#475569', fontSize: 15, fontWeight: '500' },
  switchModeTextBold: { color: '#0F172A', fontWeight: '800' },
});
