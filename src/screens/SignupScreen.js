import React, { useState } from 'react';
import { StyleSheet, Text, View, TextInput, TouchableOpacity, Alert, KeyboardAvoidingView, Platform, ScrollView, ActivityIndicator } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { auth, db } from '../config/firebase'; 
import { createUserWithEmailAndPassword } from 'firebase/auth';
import { doc, setDoc } from 'firebase/firestore'; 
import { Feather } from '@expo/vector-icons'; 

// 1. Import the font hook and specific font style
import { useFonts, TaiHeritagePro_700Bold } from '@expo-google-fonts/tai-heritage-pro';

export default function SignupScreen({ navigation }) {
  const insets = useSafeAreaInsets();
  
  // 2. Load the font
  let [fontsLoaded] = useFonts({
    TaiHeritagePro_700Bold,
  });

  const [isLoading, setIsLoading] = useState(false); 
  
  const [name, setName] = useState('');
  const [username, setUsername] = useState('');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState(''); 
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  // 3. Wait for the font to load before rendering the screen
  if (!fontsLoaded) return null;

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
      
      navigation.reset({ index: 0, routes: [{ name: 'Home' }] });
    } catch (error) {
      Alert.alert("Error", error.message);
      setIsLoading(false); 
    }
  };

  return (
    <View style={styles.container}>
      
      {/* Absolute Back Button so it doesn't mess up centering */}
      <TouchableOpacity style={[styles.backBtn, { top: insets.top + 10 }]} onPress={() => navigation.goBack()}>
        <Feather name="arrow-left" size={26} color="#000" />
      </TouchableOpacity>

      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={{ flex: 1 }}>
        <ScrollView contentContainerStyle={[styles.scrollContent, { paddingTop: insets.top, paddingBottom: insets.bottom || 20 }]} showsVerticalScrollIndicator={false} keyboardShouldPersistTaps="handled">
          
          {/* DEAD CENTER WRAPPER */}
          <View style={styles.centerWrapper}>
            
            <View style={styles.headerTitles}>
              <Text style={styles.brandTitle}>Interraqt</Text>
              <Text style={styles.subtitleText}>Create new account</Text>
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
                  <Feather name={showPassword ? "eye" : "eye-off"} size={20} color="#999" />
                </TouchableOpacity>
              </View>

              <TouchableOpacity style={styles.primaryButton} onPress={handleSignUp} disabled={isLoading}>
                {isLoading ? <ActivityIndicator color="#FFF" size="small" /> : <Text style={styles.primaryButtonText}>Sign up</Text>}
              </TouchableOpacity>
            </View>

          </View>

          {/* BOTTOM FOOTER */}
          <View style={styles.footer}>
            <TouchableOpacity onPress={() => navigation.goBack()} disabled={isLoading}>
              <Text style={styles.footerText}>Already have an account? <Text style={styles.footerTextBlue}>Log in</Text></Text>
            </TouchableOpacity>
          </View>

        </ScrollView>
      </KeyboardAvoidingView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FFFFFF' },
  backBtn: { position: 'absolute', left: 20, zIndex: 10, padding: 4 },
  
  scrollContent: { flexGrow: 1, paddingHorizontal: 24, justifyContent: 'space-between' },
  
  centerWrapper: { flex: 1, justifyContent: 'center', alignItems: 'center', width: '100%' },
  
  headerTitles: { alignItems: 'center', marginBottom: 32 },
  
  // 4. Updated font styles for the editorial aesthetic
  brandTitle: { fontFamily: 'TaiHeritagePro_700Bold', fontSize: 48, color: '#000000' },
  
  subtitleText: { fontSize: 16, color: '#666666', fontWeight: '600', marginTop: 8 },
  
  formContainer: { width: '100%' },
  
  inputWrapper: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#FFFFFF', borderWidth: 1.5, borderColor: '#E5E5E5', borderRadius: 12, marginBottom: 16, height: 56 },
  input: { flex: 1, fontSize: 16, color: '#000000', paddingHorizontal: 16, fontWeight: '500' },
  eyeIcon: { paddingHorizontal: 16 },
  
  primaryButton: { backgroundColor: '#000000', borderRadius: 100, height: 56, justifyContent: 'center', alignItems: 'center', marginTop: 16 },
  primaryButtonText: { color: '#FFFFFF', fontSize: 16, fontWeight: '800' },
  
  footer: { alignItems: 'center', paddingVertical: 20 },
  footerText: { color: '#666666', fontSize: 15, fontWeight: '600' },
  footerTextBlue: { color: '#007AFF', fontWeight: '800' }, 
});
