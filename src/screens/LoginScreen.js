import React, { useState } from 'react';
import { StyleSheet, Text, View, TextInput, TouchableOpacity, Alert, KeyboardAvoidingView, Platform, ScrollView, ActivityIndicator } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { auth, db } from '../config/firebase'; 
import { signInWithEmailAndPassword, sendPasswordResetEmail } from 'firebase/auth';
import { collection, query, where, getDocs } from 'firebase/firestore'; 
import { Feather } from '@expo/vector-icons'; 

export default function LoginScreen({ navigation }) {
  const insets = useSafeAreaInsets();
  const [isLoading, setIsLoading] = useState(false); 
  const [identifier, setIdentifier] = useState(''); 
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  const handleLogin = async () => {
    if (!identifier || !password) return Alert.alert("Required", "Please fill in all fields.");
    setIsLoading(true); 
    
    try {
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
            return Alert.alert("Not Found", "No account found with those details.");
          }
        }
      }
      
      await signInWithEmailAndPassword(auth, loginEmail, password);
      navigation.replace('Home');
    } catch (error) {
      Alert.alert("Error", error.message);
      setIsLoading(false); 
    }
  };

  const handleResetPassword = async () => {
    if (!identifier || !identifier.includes('@')) {
      return Alert.alert("Email Required", "Please enter your email address in the field above to reset your password.");
    }
    setIsLoading(true);
    try {
      await sendPasswordResetEmail(auth, identifier.toLowerCase().trim());
      Alert.alert("Sent", "Check your inbox for the reset link.");
    } catch (error) {
      Alert.alert("Error", error.message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={{ flex: 1 }}>
        <ScrollView contentContainerStyle={[styles.scrollContent, { paddingTop: insets.top, paddingBottom: insets.bottom || 20 }]} showsVerticalScrollIndicator={false} keyboardShouldPersistTaps="handled">
          
          {/* DEAD CENTER WRAPPER */}
          <View style={styles.centerWrapper}>
            
            <Text style={styles.brandTitle}>Interraqt</Text>

            <View style={styles.formContainer}>
              <View style={styles.inputWrapper}>
                <TextInput style={styles.input} placeholder="Email, Username, or Mobile" placeholderTextColor="#A0A0A0" value={identifier} onChangeText={setIdentifier} autoCapitalize="none" editable={!isLoading} />
              </View>

              <View style={styles.inputWrapper}>
                <TextInput style={styles.input} placeholder="Password" placeholderTextColor="#A0A0A0" value={password} onChangeText={setPassword} secureTextEntry={!showPassword} editable={!isLoading} />
                <TouchableOpacity onPress={() => setShowPassword(!showPassword)} style={styles.eyeIcon} disabled={isLoading}>
                  <Feather name={showPassword ? "eye" : "eye-off"} size={20} color="#999" />
                </TouchableOpacity>
              </View>

              <TouchableOpacity style={styles.forgotPassword} onPress={handleResetPassword} disabled={isLoading}>
                <Text style={styles.forgotPasswordText}>Forgot password?</Text>
              </TouchableOpacity>

              <TouchableOpacity style={styles.primaryButton} onPress={handleLogin} disabled={isLoading}>
                {isLoading ? <ActivityIndicator color="#FFF" size="small" /> : <Text style={styles.primaryButtonText}>Log in</Text>}
              </TouchableOpacity>
            </View>

          </View>

          {/* BOTTOM FOOTER */}
          <View style={styles.footer}>
            <TouchableOpacity onPress={() => navigation.navigate('Signup')} disabled={isLoading}>
              <Text style={styles.footerText}>Don't have an account? <Text style={styles.footerTextBlue}>Sign up</Text></Text>
            </TouchableOpacity>
          </View>

        </ScrollView>
      </KeyboardAvoidingView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FFFFFF' },
  scrollContent: { flexGrow: 1, paddingHorizontal: 24, justifyContent: 'space-between' },
  
  centerWrapper: { flex: 1, justifyContent: 'center', alignItems: 'center', width: '100%' },
  
  brandTitle: { fontSize: 34, fontWeight: '900', color: '#000000', letterSpacing: -1.5, marginBottom: 40 },
  
  formContainer: { width: '100%' },
  
  inputWrapper: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#FFFFFF', borderWidth: 1.5, borderColor: '#E5E5E5', borderRadius: 12, marginBottom: 16, height: 56 },
  input: { flex: 1, fontSize: 16, color: '#000000', paddingHorizontal: 16, fontWeight: '500' },
  eyeIcon: { paddingHorizontal: 16 },
  
  forgotPassword: { alignSelf: 'center', marginBottom: 32, paddingVertical: 8 },
  forgotPasswordText: { color: '#000000', fontSize: 14, fontWeight: '700' },
  
  primaryButton: { backgroundColor: '#000000', borderRadius: 100, height: 56, justifyContent: 'center', alignItems: 'center' },
  primaryButtonText: { color: '#FFFFFF', fontSize: 16, fontWeight: '800' },
  
  footer: { alignItems: 'center', paddingVertical: 20 },
  footerText: { color: '#666666', fontSize: 15, fontWeight: '600' },
  footerTextBlue: { color: '#007AFF', fontWeight: '800' }, // Apple Blue Accent
});
