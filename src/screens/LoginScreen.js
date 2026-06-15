import React, { useState } from 'react';
import { StyleSheet, Text, View, TextInput, TouchableOpacity, SafeAreaView, Alert, KeyboardAvoidingView, Platform, ScrollView, ActivityIndicator, LayoutAnimation, UIManager } from 'react-native';
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

  return (
    <LinearGradient colors={['#141E30', '#243B55', '#0f172a']} style={styles.background}>
      <SafeAreaView style={styles.container}>
        <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={{ flex: 1 }}>
          <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false} keyboardShouldPersistTaps="handled">
            
            <View style={styles.header}>
              {!isLogin && (
                <TouchableOpacity style={styles.backBtn} onPress={toggleMode}>
                  <Feather name="arrow-left" size={28} color="#FFF" />
                </TouchableOpacity>
              )}
              <View style={styles.iconWrapper}>
                <Feather name="aperture" size={36} color="#FFF" />
              </View>
              <Text style={styles.logoText}>Interraqt</Text>
              <Text style={styles.subtitle}>{isLogin ? 'Welcome back.' : 'Join the network.'}</Text>
            </View>

            {/* FROSTED GLASS PANEL */}
            <BlurView intensity={30} tint="dark" style={styles.glassPanel}>
              
              {!isLogin && (
                <View style={styles.expandedInputs}>
                  <View style={styles.inputBox}>
                    <Feather name="user" size={20} color="rgba(255,255,255,0.7)" style={styles.icon} />
                    <TextInput style={styles.input} placeholder="Full Name" placeholderTextColor="rgba(255,255,255,0.5)" value={name} onChangeText={setName} autoCapitalize="words" editable={!isLoading} />
                  </View>
                  <View style={styles.inputBox}>
                    <Feather name="at-sign" size={20} color="rgba(255,255,255,0.7)" style={styles.icon} />
                    <TextInput style={styles.input} placeholder="Username" placeholderTextColor="rgba(255,255,255,0.5)" value={username} onChangeText={setUsername} autoCapitalize="none" editable={!isLoading} />
                  </View>
                  <View style={styles.inputBox}>
                    <Feather name="phone" size={20} color="rgba(255,255,255,0.7)" style={styles.icon} />
                    <TextInput style={styles.input} placeholder="Mobile Number (Optional)" placeholderTextColor="rgba(255,255,255,0.5)" value={phone} onChangeText={setPhone} keyboardType="phone-pad" editable={!isLoading} />
                  </View>
                </View>
              )}

              <View style={styles.inputBox}>
                <Feather name="mail" size={20} color="rgba(255,255,255,0.7)" style={styles.icon} />
                <TextInput 
                  style={styles.input} 
                  placeholder={isLogin ? "Email, Username, or Mobile" : "Email Address"} 
                  placeholderTextColor="rgba(255,255,255,0.5)" 
                  value={identifier} 
                  onChangeText={setIdentifier} 
                  autoCapitalize="none" 
                  editable={!isLoading} 
                />
              </View>

              <View style={styles.inputBox}>
                <Feather name="lock" size={20} color="rgba(255,255,255,0.7)" style={styles.icon} />
                <TextInput style={styles.input} placeholder="Password" placeholderTextColor="rgba(255,255,255,0.5)" value={password} onChangeText={setPassword} secureTextEntry={!showPassword} editable={!isLoading} />
                <TouchableOpacity onPress={() => setShowPassword(!showPassword)} style={styles.eyeIcon} disabled={isLoading}>
                  <Feather name={showPassword ? "eye" : "eye-off"} size={20} color="rgba(255,255,255,0.7)" />
                </TouchableOpacity>
              </View>

              {isLogin && (
                <TouchableOpacity style={styles.forgotPassword} disabled={isLoading}>
                  <Text style={styles.forgotPasswordText}>Forgot password?</Text>
                </TouchableOpacity>
              )}

              <TouchableOpacity style={styles.primaryButton} onPress={handleAuth} disabled={isLoading}>
                {isLoading ? (
                  <ActivityIndicator color="#000" size="small" />
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
      </SafeAreaView>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  background: { flex: 1 },
  container: { flex: 1 },
  scrollContent: { flexGrow: 1, paddingHorizontal: 24, justifyContent: 'center', paddingBottom: 40 },
  header: { marginTop: 40, marginBottom: 40, alignItems: 'center' },
  backBtn: { position: 'absolute', top: 10, left: 0, padding: 10, zIndex: 10 },
  iconWrapper: { width: 72, height: 72, backgroundColor: 'rgba(255,255,255,0.1)', borderRadius: 24, alignItems: 'center', justifyContent: 'center', marginBottom: 20, borderWidth: 1, borderColor: 'rgba(255,255,255,0.2)' },
  logoText: { fontSize: 36, fontWeight: '900', color: '#FFF', letterSpacing: -1 },
  subtitle: { fontSize: 16, color: 'rgba(255,255,255,0.7)', marginTop: 8 },
  
  glassPanel: { borderRadius: 32, padding: 24, borderWidth: 1, borderColor: 'rgba(255,255,255,0.2)', overflow: 'hidden' },
  expandedInputs: { overflow: 'hidden' },
  
  inputBox: { flexDirection: 'row', alignItems: 'center', backgroundColor: 'rgba(255, 255, 255, 0.08)', borderWidth: 1, borderColor: 'rgba(255, 255, 255, 0.15)', borderRadius: 16, marginBottom: 16, paddingHorizontal: 16, height: 60 },
  icon: { marginRight: 12 },
  input: { flex: 1, fontSize: 16, color: '#FFF' },
  eyeIcon: { padding: 8 },
  
  forgotPassword: { alignSelf: 'flex-end', marginBottom: 24 },
  forgotPasswordText: { color: 'rgba(255,255,255,0.8)', fontSize: 14, fontWeight: '600' },
  
  primaryButton: { backgroundColor: '#FFF', borderRadius: 16, height: 60, justifyContent: 'center', alignItems: 'center', marginTop: 8, shadowColor: '#FFF', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.2, shadowRadius: 8, elevation: 4 },
  primaryButtonText: { color: '#000', fontSize: 16, fontWeight: '800' },
  
  switchModeBtn: { marginTop: 24, alignItems: 'center', padding: 10 },
  switchModeText: { color: 'rgba(255,255,255,0.7)', fontSize: 15 },
  switchModeTextBold: { color: '#FFF', fontWeight: '800' },
});
