import React, { useState } from 'react';
import { StyleSheet, Text, View, TouchableOpacity, ScrollView, Alert, ActivityIndicator } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { auth, db } from '../config/firebase';
import { signOut, deleteUser, sendPasswordResetEmail } from 'firebase/auth';
import { doc, deleteDoc } from 'firebase/firestore';
import { Feather } from '@expo/vector-icons';

export default function SettingsScreen({ navigation }) {
  const insets = useSafeAreaInsets();
  const [loadingAction, setLoadingAction] = useState(null);

  const handlePasswordChange = async () => {
    Alert.alert(
      "Reset Password",
      "We will send a password reset link to your email address.",
      [
        { text: "Cancel", style: "cancel" },
        { 
          text: "Send Link", 
          onPress: async () => {
            setLoadingAction('password');
            try {
              await sendPasswordResetEmail(auth, auth.currentUser.email);
              Alert.alert("Sent!", "Check your inbox for the reset link.");
            } catch (error) {
              Alert.alert("Error", error.message);
            } finally {
              setLoadingAction(null);
            }
          } 
        }
      ]
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
            Alert.alert("Action Required", "Please log out and log back in before deleting your account.");
            setLoadingAction(null);
          }
        } 
      }
    ]);
  };

  return (
    <View style={styles.container}>
      <View style={[styles.header, { paddingTop: insets.top + 10 }]}>
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
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FAFAFA' },
  header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 20, paddingBottom: 20, backgroundColor: '#FFF', borderBottomWidth: 1, borderColor: '#F0F0F0' },
  backBtn: { padding: 4 },
  headerTitle: { fontSize: 18, fontWeight: '700', color: '#000' },
  content: { padding: 20 },
  sectionTitle: { fontSize: 13, fontWeight: '700', color: '#999', textTransform: 'uppercase', marginBottom: 8, marginLeft: 4, letterSpacing: 1 },
  card: { backgroundColor: '#FFF', borderRadius: 16, borderWidth: 1, borderColor: '#F0F0F0', overflow: 'hidden', marginBottom: 32 },
  row: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', padding: 16 },
  rowLeft: { flexDirection: 'row', alignItems: 'center' },
  rowText: { fontSize: 16, fontWeight: '600', color: '#000', marginLeft: 12 },
  divider: { height: 1, backgroundColor: '#F0F0F0', marginLeft: 48 },
});
