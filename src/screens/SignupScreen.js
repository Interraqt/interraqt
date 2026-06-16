import React, { useState } from 'react';
import { StyleSheet, Text, View, TextInput, TouchableOpacity, Alert, KeyboardAvoidingView, Platform, ScrollView, ActivityIndicator } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { auth, db } from '../config/firebase'; 
import { createUserWithEmailAndPassword } from 'firebase/auth';
import { doc, setDoc } from 'firebase/firestore'; 
import { Feather } from '@expo/vector-icons'; 

export default function SignupScreen({ navigation }) {
  const insets = useSafeAreaInsets();
  const [isLoading, setIsLoading] = useState(false); 
  
  const [name, setName] = useState('');
  const [username, setUsername] = useState('');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState(''); 
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  const handleSignUp = async () => {
    if (!email || !password || !name || !username) {
      return Alert.alert("Required", "Please fill in all required fields.");
    }
    setIsLoading(true); 
    
    try {
      const userCredential = await createUserWithEmailAndPassword(auth, email.toLowerCase().trim(), password);
      const user = userCredential.user;

      await setDoc(doc(db, "users", user.uid), {
        uid: user.uid,
        name: name,
        username: username.toLowerCase().replace(/\s/g, ''),
        phone: phone,
        email: email.toLowerCase().trim(),
        createdAt: new Date().toISOString()
      });
      
      // Navigate to Home, resetting the stack so user can't swipe back to Signup
      navigation.reset({ index: 0, routes: [{ name: 'Home' }] });
    } catch (error) {
      Alert.alert("Error", error.message);
      setIsLoading(false); 
    }
  };

  return (
    <View style={styles.container}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={{ flex: 1 }}>
        <ScrollView contentContainerStyle={[styles.scrollContent, { paddingTop: insets.top + 20 }]} showsVerticalScrollIndicator={false} keyboardShouldPersistTaps="handled">
          
          <View style={styles.header}>
            <TouchableOpacity style={styles.backBtn} onPress={() => navigation.goBack()}>
              <Feather name="arrow-left" size={26} color="#000" />
            </TouchableOpacity>
            
            <View style={styles.headerContent}>
              <Text style={styles.titleText}>Create account</Text>
              <Text style={styles.subtitleText}>Join the network today.</Text>
            </View>
          </View>

          <View style={styles.formContainer}>
            <View style={styles.inputWrapper}>
              <TextInput style={styles.input} placeholder="Full Name" placeholderTextColor="#A0A0A0" value={name} onChangeText={setName} autoCapitalize="words" editable={!isLoading} />
            </View>
            <View style={styles.inputWrapper}>
              <TextInput style={styles.input} placeholder="Username" placeholderTextColor="#A0A0A0" value={username} onChangeText={setUsername} autoCapitalize="none" editable={!isLoading} />
            </View>
            <View style={styles.inputWrapper}>
              <TextInput style={styles.input} placeholder="Mobile Number (Optional)" placeholderTextColor="#A0A0A0" value={phone} onChangeText={setPhone} keyboardType="phone-pad" editable={!isLoading} />
            </View>

            <View style={styles.inputWrapper}>
              <TextInput style={styles.input} placeholder="Email Address" placeholderTextColor="#A0A0A0" value={email} onChangeText={setEmail} autoCapitalize="none" keyboardType="email-address" editable={!isLoading} />
            </View>

            <View style={styles.inputWrapper}>
              <TextInput style={styles.input} placeholder="Password" placeholderTextColor="#A0A0A0" value={password} onChangeText={setPassword} secureTextEntry={!showPassword} editable={!isLoading} />
              <TouchableOpacity onPress={() => setShowPassword(!showPassword)} style={styles.eyeIcon} disabled={isLoading}>
                <Feather name={showPassword ? "eye" : "eye-off"} size={20} color="#666" />
              </TouchableOpacity>
            </View>

            <TouchableOpacity style={styles.primaryButton} onPress={handleSignUp} disabled={isLoading}>
              {isLoading ? <ActivityIndicator color="#FFF" size="small" /> : <Text style={styles.primaryButtonText}>Sign up</Text>}
            </TouchableOpacity>
          </View>

          <View style={styles.footer}>
            <TouchableOpacity onPress={() => navigation.goBack()} disabled={isLoading}>
              <Text style={styles.footerText}>Already have an account? <Text style={styles.footerTextBold}>Log in</Text></Text>
            </TouchableOpacity>
          </View>

        </ScrollView>
      </KeyboardAvoidingView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FFFFFF' },
  scrollContent: { flexGrow: 1, paddingHorizontal: 24, paddingBottom: 40 },
  header: { marginTop: 20, marginBottom: 40 },
  backBtn: { marginBottom: 20, alignSelf: 'flex-start' },
  headerContent: { alignItems: 'flex-start' },
  titleText: { fontSize: 36, fontWeight: '900', color: '#000000', letterSpacing: -1.2, marginBottom: 8 },
  subtitleText: { fontSize: 16, color: '#666666', fontWeight: '500' },
  formContainer: { width: '100%' },
  inputWrapper: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#FAFAFA', borderWidth: 1, borderColor: '#EFEFEF', borderRadius: 16, marginBottom: 16, height: 60 },
  input: { flex: 1, fontSize: 16, color: '#000000', paddingHorizontal: 20, fontWeight: '500' },
  eyeIcon: { paddingHorizontal: 20 },
  primaryButton: { backgroundColor: '#000000', borderRadius: 100, height: 60, justifyContent: 'center', alignItems: 'center', marginTop: 10 },
  primaryButtonText: { color: '#FFFFFF', fontSize: 16, fontWeight: '800' },
  footer: { marginTop: 'auto', paddingTop: 40, alignItems: 'center' },
  footerText: { color: '#666666', fontSize: 15, fontWeight: '500' },
  footerTextBold: { color: '#000000', fontWeight: '800' },
});
