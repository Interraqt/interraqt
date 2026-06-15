import React, { useState } from 'react';
import { StyleSheet, Text, View, TextInput, TouchableOpacity, SafeAreaView, Alert, KeyboardAvoidingView, Platform, ScrollView, ActivityIndicator, LayoutAnimation, UIManager } from 'react-native';
import { auth, db } from '../config/firebase'; 
import { createUserWithEmailAndPassword, signInWithEmailAndPassword, sendPasswordResetEmail } from 'firebase/auth';
import { doc, setDoc, collection, query, where, getDocs } from 'firebase/firestore'; 
import { Feather } from '@expo/vector-icons'; 

if (Platform.OS === 'android' && UIManager.setLayoutAnimationEnabledExperimental) {
  UIManager.setLayoutAnimationEnabledExperimental(true);
}

export default function LoginScreen({ navigation }) {
  const [isLogin, setIsLogin] = useState(true);
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false); 
  
  // Form State
  const [name, setName] = useState('');
  const [username, setUsername] = useState('');
  const [phone, setPhone] = useState('');
  const [identifier, setIdentifier] = useState(''); // Handles Email, Username, or Phone
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
        // LOGIN: Check if input is Email, Username, or Phone
        let loginEmail = identifier.toLowerCase().trim();
        
        if (!loginEmail.includes('@')) {
          // If no '@', assume it's a Username or Phone and search Firestore for the true email
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
        // SIGN UP
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
    <SafeAreaView style={styles.container}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={{ flex: 1 }}>
        {/* FIX: keyboardShouldPersistTaps ensures you don't have to tap twice to dismiss keyboard */}
        <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false} keyboardShouldPersistTaps="handled">
          
          <View style={styles.header}>
            {!isLogin && (
              <TouchableOpacity style={styles.backBtn} onPress={toggleMode}>
                <Feather name="arrow-left" size={28} color="#000" />
              </TouchableOpacity>
            )}
            <View style={styles.iconWrapper}>
              <Feather name="aperture" size={32} color="#fff" />
            </View>
            <Text style={styles.logoText}>Interraqt</Text>
            <Text style={styles.subtitle}>{isLogin ? 'Welcome back.' : 'Create your account.'}</Text>
          </View>

          <View style={styles.formContainer}>
            {!isLogin && (
              <View style={styles.expandedInputs}>
                <View style={styles.inputBox}>
                  <Feather name="user" size={20} color="#888" style={styles.icon} />
                  <TextInput style={styles.input} placeholder="Full Name" placeholderTextColor="#999" value={name} onChangeText={setName} autoCapitalize="words" editable={!isLoading} />
                </View>

                <View style={styles.inputBox}>
                  <Feather name="at-sign" size={20} color="#888" style={styles.icon} />
                  <TextInput style={styles.input} placeholder="Username" placeholderTextColor="#999" value={username} onChangeText={setUsername} autoCapitalize="none" editable={!isLoading} />
                </View>

                <View style={styles.inputBox}>
                  <Feather name="phone" size={20} color="#888" style={styles.icon} />
                  <TextInput style={styles.input} placeholder="Mobile Number (Optional)" placeholderTextColor="#999" value={phone} onChangeText={setPhone} keyboardType="phone-pad" editable={!isLoading} />
                </View>
              </View>
            )}

            <View style={styles.inputBox}>
              <Feather name="mail" size={20} color="#888" style={styles.icon} />
              <TextInput 
                style={styles.input} 
                placeholder={isLogin ? "Email, Username, or Mobile" : "Email Address"} 
                placeholderTextColor="#A0A0A0" 
                value={identifier} 
                onChangeText={setIdentifier} 
                autoCapitalize="none" 
                editable={!isLoading} 
              />
            </View>

            <View style={styles.inputBox}>
              <Feather name="lock" size={20} color="#888" style={styles.icon} />
              <TextInput style={styles.input} placeholder="Password" placeholderTextColor="#999" value={password} onChangeText={setPassword} secureTextEntry={!showPassword} editable={!isLoading} />
              <TouchableOpacity onPress={() => setShowPassword(!showPassword)} style={styles.eyeIcon} disabled={isLoading}>
                <Feather name={showPassword ? "eye" : "eye-off"} size={20} color="#888" />
              </TouchableOpacity>
            </View>

            {isLogin && (
              <TouchableOpacity style={styles.forgotPassword} disabled={isLoading}>
                <Text style={styles.forgotPasswordText}>Forgot password?</Text>
              </TouchableOpacity>
            )}

            <TouchableOpacity style={styles.primaryButton} onPress={handleAuth} disabled={isLoading}>
              {isLoading ? (
                <ActivityIndicator color="#fff" size="small" />
              ) : (
                <Text style={styles.primaryButtonText}>{isLogin ? 'Log In' : 'Sign Up'}</Text>
              )}
            </TouchableOpacity>

            {isLogin && (
              <TouchableOpacity style={styles.switchModeBtn} onPress={toggleMode} disabled={isLoading}>
                <Text style={styles.switchModeText}>Don't have an account? <Text style={styles.switchModeTextBold}>Sign up</Text></Text>
              </TouchableOpacity>
            )}
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FFFFFF' },
  scrollContent: { flexGrow: 1, paddingHorizontal: 28, justifyContent: 'center', paddingBottom: 40 },
  header: { marginTop: 40, marginBottom: 40 },
  backBtn: { position: 'absolute', top: 0, left: -10, padding: 10, zIndex: 10 },
  iconWrapper: { width: 64, height: 64, backgroundColor: '#000', borderRadius: 20, alignItems: 'center', justifyContent: 'center', marginBottom: 20, marginTop: 40 },
  logoText: { fontSize: 36, fontWeight: '900', color: '#000', letterSpacing: -1 },
  subtitle: { fontSize: 16, color: '#666', marginTop: 8 },
  formContainer: { width: '100%' },
  expandedInputs: { overflow: 'hidden' },
  inputBox: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#FAFAFA', borderWidth: 1, borderColor: '#EAEAEA', borderRadius: 16, marginBottom: 16, paddingHorizontal: 16, height: 60 },
  icon: { marginRight: 12 },
  input: { flex: 1, fontSize: 16, color: '#000' },
  eyeIcon: { padding: 8 },
  forgotPassword: { alignSelf: 'flex-end', marginBottom: 24 },
  forgotPasswordText: { color: '#000', fontSize: 14, fontWeight: '600' },
  primaryButton: { backgroundColor: '#000', borderRadius: 16, height: 60, justifyContent: 'center', alignItems: 'center', marginTop: 8 },
  primaryButtonText: { color: '#FFFFFF', fontSize: 16, fontWeight: '800' },
  switchModeBtn: { marginTop: 24, alignItems: 'center', padding: 10 },
  switchModeText: { color: '#666', fontSize: 15 },
  switchModeTextBold: { color: '#000', fontWeight: '800' },
});
