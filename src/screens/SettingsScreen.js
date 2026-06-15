import React, { useState } from 'react';
import { StyleSheet, Text, View, TouchableOpacity, SafeAreaView, ScrollView, Alert, ActivityIndicator } from 'react-native';
import { auth, db } from '../config/firebase';
import { signOut, deleteUser, updatePassword } from 'firebase/auth';
import { doc, deleteDoc } from 'firebase/firestore';
import { Feather } from '@expo/vector-icons';

export default function SettingsScreen({ navigation }) {
  const [loadingAction, setLoadingAction] = useState(null);

  const handlePasswordChange = () => {
    Alert.prompt(
      "Change Password",
      "Enter your new password below (minimum 6 characters):",
      [
        { text: "Cancel", style: "cancel" },
        { 
          text: "Save", 
          onPress: async (newPassword) => {
            if (newPassword.length < 6) return Alert.alert("Error", "Password must be at least 6 characters.");
            setLoadingAction('password');
            try {
              await updatePassword(auth.currentUser, newPassword);
              Alert.alert("Success", "Your password has been updated.");
            } catch (error) {
              Alert.alert("Authentication Required", "For security, please log out and log back in before changing your password.");
            } finally {
              setLoadingAction(null);
            }
          } 
        }
      ],
      "secure-text"
    );
  };

  const handleLogOut = async () => {
    setLoadingAction('logout');
    try {
      await signOut(auth);
      navigation.reset({ index: 0, routes: [{ name: 'Login' }] });
    } catch (error) {
      Alert.alert("Error", error.message);
      setLoadingAction(null);
    }
  };

  const handleDeleteAccount = () => {
    Alert.alert("Delete Account", "This action is permanent and cannot be undone.", [
      { text: "Cancel", style: "cancel" },
      { text: "Delete", style: "destructive", onPress: async () => {
          setLoadingAction('delete');
          try {
            const user = auth.currentUser;
            await deleteDoc(doc(db, 'users', user.uid)); 
            await deleteUser(user); 
            navigation.reset({ index: 0, routes: [{ name: 'Login' }] });
          } catch (error) {
            Alert.alert("Action Required", "For security, please log out and log back in before deleting your account.");
            setLoadingAction(null);
          }
        } 
      }
    ]);
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backBtn}>
          <Feather name="arrow-left" size={24} color="#000" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Settings</Text>
        <View style={{ width: 24 }} />
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        
        <Text style={styles.sectionTitle}>Security</Text>
        <View style={styles.card}>
          <TouchableOpacity style={styles.row} onPress={handlePasswordChange} disabled={loadingAction === 'password'}>
            <View style={styles.rowLeft}>
              <Feather name="lock" size={20} color="#000" />
              <Text style={styles.rowText}>Change Password</Text>
            </View>
            {loadingAction === 'password' ? <ActivityIndicator size="small" color="#000" /> : <Feather name="chevron-right" size={20} color="#ccc" />}
          </TouchableOpacity>
        </View>

        <Text style={styles.sectionTitle}>Account</Text>
        <View style={styles.card}>
          <TouchableOpacity style={styles.row} onPress={handleLogOut} disabled={loadingAction === 'logout'}>
            <View style={styles.rowLeft}>
              <Feather name="log-out" size={20} color="#000" />
              <Text style={styles.rowText}>Log Out</Text>
            </View>
            {loadingAction === 'logout' ? <ActivityIndicator size="small" color="#000" /> : <Feather name="chevron-right" size={20} color="#ccc" />}
          </TouchableOpacity>
          
          <View style={styles.divider} />

          <TouchableOpacity style={styles.row} onPress={handleDeleteAccount} disabled={loadingAction === 'delete'}>
            <View style={styles.rowLeft}>
              <Feather name="trash-2" size={20} color="#FF3B30" />
              <Text style={[styles.rowText, { color: '#FF3B30' }]}>Delete Account</Text>
            </View>
            {loadingAction === 'delete' ? <ActivityIndicator size="small" color="#FF3B30" /> : <Feather name="chevron-right" size={20} color="#FF3B30" opacity={0.5} />}
          </TouchableOpacity>
        </View>

      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FAFAFA' },
  header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 20, paddingTop: 10, paddingBottom: 20, borderBottomWidth: 1, borderColor: '#EAEAEA' },
  backBtn: { padding: 4 },
  headerTitle: { fontSize: 18, fontWeight: '700', color: '#000' },
  content: { padding: 20 },
  sectionTitle: { fontSize: 13, fontWeight: '600', color: '#888', textTransform: 'uppercase', marginBottom: 8, marginLeft: 4, letterSpacing: 1 },
  card: { backgroundColor: '#FFF', borderRadius: 16, borderWidth: 1, borderColor: '#EAEAEA', overflow: 'hidden', marginBottom: 32 },
  row: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', padding: 16 },
  rowLeft: { flexDirection: 'row', alignItems: 'center' },
  rowText: { fontSize: 16, fontWeight: '500', color: '#000', marginLeft: 12 },
  divider: { height: 1, backgroundColor: '#EAEAEA', marginLeft: 48 },
});
