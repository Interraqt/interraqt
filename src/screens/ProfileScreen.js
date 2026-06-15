import React, { useState, useEffect } from 'react';
import { StyleSheet, Text, View, TouchableOpacity, Image, Modal, TextInput, Alert, ActivityIndicator, ScrollView, SafeAreaView } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context'; // 1. FIX THE OVERLAP
import { auth, db } from '../config/firebase';
import { signOut, deleteUser } from 'firebase/auth';
import { doc, getDoc, updateDoc, deleteDoc } from 'firebase/firestore';
import { Feather } from '@expo/vector-icons'; // Modern Icons

export default function ProfileScreen({ navigation }) {
  const insets = useSafeAreaInsets(); // Grabs the exact height of your phone's status bar!
  const [userData, setUserData] = useState(null);
  const [loading, setLoading] = useState(true);
  
  // Edit Profile State
  const [isEditVisible, setIsEditVisible] = useState(false);
  const [isSaving, setIsSaving] = useState(false); // Visual feedback
  const [editName, setEditName] = useState('');
  const [editUsername, setEditUsername] = useState('');

  useEffect(() => {
    fetchUser();
  }, []);

  const fetchUser = async () => {
    try {
      const docRef = doc(db, 'users', auth.currentUser.uid);
      const docSnap = await getDoc(docRef);
      if (docSnap.exists()) {
        setUserData(docSnap.data());
        setEditName(docSnap.data().name);
        setEditUsername(docSnap.data().username);
      }
    } catch (error) {
      console.log(error);
    } finally {
      setLoading(false);
    }
  };

  const handleSaveProfile = async () => {
    setIsSaving(true);
    try {
      await updateDoc(doc(db, 'users', auth.currentUser.uid), {
        name: editName,
        username: editUsername.toLowerCase().replace(/\s/g, ''),
      });
      setUserData({ ...userData, name: editName, username: editUsername.toLowerCase().replace(/\s/g, '') });
      setIsEditVisible(false);
    } catch (error) {
      Alert.alert("Error", error.message);
    } finally {
      setIsSaving(false);
    }
  };

  const handleLogOut = async () => {
    try {
      await signOut(auth);
      navigation.reset({ index: 0, routes: [{ name: 'Login' }] });
    } catch (error) {
      Alert.alert("Error", error.message);
    }
  };

  const handleDeleteAccount = () => {
    Alert.alert("Delete Account", "This will permanently delete your data.", [
      { text: "Cancel", style: "cancel" },
      { text: "Delete", style: "destructive", onPress: async () => {
          try {
            const user = auth.currentUser;
            await deleteDoc(doc(db, 'users', user.uid)); 
            await deleteUser(user); 
            navigation.reset({ index: 0, routes: [{ name: 'Login' }] });
          } catch (error) {
            Alert.alert("Action Required", "For security, please log out and log back in before deleting your account.");
          }
        } 
      }
    ]);
  };

  if (loading) {
    return <View style={styles.loadingContainer}><ActivityIndicator size="large" color="#0056b3" /></View>;
  }

  return (
    <View style={styles.container}>
      <ScrollView bounces={false} showsVerticalScrollIndicator={false}>
        
        {/* FIX: Apply exact inset top padding to push content safely below the battery/time */}
        <View style={[styles.coverPhoto, { paddingTop: insets.top + 16, height: 160 + insets.top }]}>
          <View style={styles.coverHeader}>
            <Text style={styles.headerTitle}>@{userData?.username}</Text>
            <TouchableOpacity onPress={() => setIsEditVisible(true)} style={styles.settingsIcon}>
              <Feather name="settings" size={22} color="#fff" />
            </TouchableOpacity>
          </View>
        </View>

        <View style={styles.profileSection}>
          <View style={styles.avatarContainer}>
            <Image source={{ uri: 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png' }} style={styles.avatar} />
            <TouchableOpacity style={styles.addPhotoBadge}>
              <Feather name="camera" size={14} color="#fff" />
            </TouchableOpacity>
          </View>

          <Text style={styles.name}>{userData?.name || 'Full Name'}</Text>
          <Text style={styles.bio}>Building something awesome! 🚀✨</Text>

          <View style={styles.statsCard}>
            <View style={styles.statItem}>
              <Text style={styles.statValue}>0</Text>
              <Text style={styles.statLabel}>Posts</Text>
            </View>
            <View style={styles.statDivider} />
            <View style={styles.statItem}>
              <Text style={styles.statValue}>124</Text>
              <Text style={styles.statLabel}>Followers</Text>
            </View>
            <View style={styles.statDivider} />
            <View style={styles.statItem}>
              <Text style={styles.statValue}>180</Text>
              <Text style={styles.statLabel}>Following</Text>
            </View>
          </View>

          <View style={styles.actionRow}>
            <TouchableOpacity style={styles.primaryBtn} onPress={() => setIsEditVisible(true)}>
              <Text style={styles.primaryBtnText}>Edit Profile</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.secondaryBtn}>
              <Feather name="share" size={20} color="#0F172A" />
            </TouchableOpacity>
          </View>
        </View>

        <View style={styles.contentArea}>
          <Text style={styles.sectionTitle}>Recent Activity</Text>
          <View style={styles.placeholderCard}>
            <Feather name="image" size={32} color="#CBD5E1" />
            <Text style={styles.placeholderText}>No posts yet.</Text>
          </View>
        </View>

      </ScrollView>

      {/* EDIT PROFILE MODAL */}
      <Modal visible={isEditVisible} animationType="slide" presentationStyle="pageSheet">
        <SafeAreaView style={styles.modalContainer}>
          <View style={styles.modalHeader}>
            <TouchableOpacity onPress={() => setIsEditVisible(false)}>
              <Text style={styles.cancelText}>Cancel</Text>
            </TouchableOpacity>
            <Text style={styles.modalTitle}>Settings</Text>
            <TouchableOpacity onPress={handleSaveProfile} disabled={isSaving}>
              {isSaving ? <ActivityIndicator size="small" color="#0056b3" /> : <Text style={styles.saveText}>Save</Text>}
            </TouchableOpacity>
          </View>

          <ScrollView contentContainerStyle={styles.modalContent}>
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>Name</Text>
              <TextInput style={styles.input} value={editName} onChangeText={setEditName} />
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>Username</Text>
              <TextInput style={styles.input} value={editUsername} onChangeText={setEditUsername} autoCapitalize="none" />
            </View>

            <View style={styles.dangerZone}>
              <Text style={styles.dangerTitle}>Account Actions</Text>
              <TouchableOpacity style={styles.logoutBtn} onPress={handleLogOut}>
                <Feather name="log-out" size={20} color="#0F172A" style={{marginRight: 8}} />
                <Text style={styles.logoutText}>Log Out</Text>
              </TouchableOpacity>

              <TouchableOpacity style={styles.deleteBtn} onPress={handleDeleteAccount}>
                <Feather name="trash-2" size={20} color="#EF4444" style={{marginRight: 8}} />
                <Text style={styles.deleteText}>Delete Account</Text>
              </TouchableOpacity>
            </View>
          </ScrollView>
        </SafeAreaView>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  loadingContainer: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#F8FAFC' },
  container: { flex: 1, backgroundColor: '#F8FAFC' },
  
  // Dynamic Cover Photo
  coverPhoto: { backgroundColor: '#0056b3', paddingHorizontal: 20 },
  coverHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  headerTitle: { color: '#fff', fontSize: 18, fontWeight: 'bold' },
  settingsIcon: { padding: 6, backgroundColor: 'rgba(0,0,0,0.2)', borderRadius: 20 },

  profileSection: { backgroundColor: '#F8FAFC', borderTopLeftRadius: 28, borderTopRightRadius: 28, marginTop: -24, alignItems: 'center', paddingHorizontal: 20, paddingBottom: 24 },
  avatarContainer: { marginTop: -50, marginBottom: 12, position: 'relative' },
  avatar: { width: 100, height: 100, borderRadius: 50, borderWidth: 4, borderColor: '#F8FAFC' },
  addPhotoBadge: { position: 'absolute', bottom: 4, right: 0, backgroundColor: '#0056b3', padding: 8, borderRadius: 20, borderWidth: 3, borderColor: '#F8FAFC' },
  name: { fontSize: 24, fontWeight: '800', color: '#0F172A' },
  bio: { fontSize: 15, color: '#64748B', marginTop: 4, marginBottom: 24 },

  statsCard: { flexDirection: 'row', backgroundColor: '#FFF', width: '100%', borderRadius: 20, paddingVertical: 16, shadowColor: '#64748B', shadowOffset: { width: 0, height: 6 }, shadowOpacity: 0.08, shadowRadius: 16, elevation: 3, marginBottom: 24 },
  statItem: { flex: 1, alignItems: 'center' },
  statValue: { fontSize: 18, fontWeight: 'bold', color: '#0F172A' },
  statLabel: { fontSize: 13, color: '#64748B', marginTop: 4 },
  statDivider: { width: 1, backgroundColor: '#E2E8F0', height: '80%', alignSelf: 'center' },

  actionRow: { flexDirection: 'row', width: '100%', gap: 12 },
  primaryBtn: { flex: 1, backgroundColor: '#0F172A', paddingVertical: 16, borderRadius: 16, alignItems: 'center', shadowColor: '#0F172A', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.2, shadowRadius: 8, elevation: 4 },
  primaryBtnText: { color: '#fff', fontSize: 15, fontWeight: 'bold' },
  secondaryBtn: { backgroundColor: '#fff', padding: 16, borderRadius: 16, borderWidth: 1, borderColor: '#E2E8F0', alignItems: 'center', justifyContent: 'center' },

  contentArea: { paddingHorizontal: 20, paddingTop: 10, paddingBottom: 40 },
  sectionTitle: { fontSize: 18, fontWeight: 'bold', color: '#0F172A', marginBottom: 16 },
  placeholderCard: { height: 160, backgroundColor: '#FFF', borderRadius: 20, alignItems: 'center', justifyContent: 'center', borderWidth: 2, borderColor: '#F1F5F9', borderStyle: 'dashed' },
  placeholderText: { color: '#94A3B8', marginTop: 12, fontWeight: '500' },

  modalContainer: { flex: 1, backgroundColor: '#FFF' },
  modalHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: 20, borderBottomWidth: 1, borderColor: '#F1F5F9' },
  cancelText: { fontSize: 16, color: '#64748B' },
  modalTitle: { fontSize: 18, fontWeight: '700', color: '#0F172A' },
  saveText: { fontSize: 16, fontWeight: 'bold', color: '#0056b3' },
  modalContent: { padding: 24 },
  inputGroup: { marginBottom: 24 },
  inputLabel: { color: '#64748B', fontSize: 13, marginBottom: 8, fontWeight: '600' },
  input: { fontSize: 16, borderBottomWidth: 1, borderColor: '#E2E8F0', paddingBottom: 12, color: '#0F172A' },
  dangerZone: { marginTop: 40, borderTopWidth: 1, borderColor: '#F1F5F9', paddingTop: 24 },
  dangerTitle: { color: '#64748B', fontSize: 13, marginBottom: 16, fontWeight: '600' },
  logoutBtn: { flexDirection: 'row', backgroundColor: '#F8FAFC', padding: 16, borderRadius: 16, alignItems: 'center', justifyContent: 'center', marginBottom: 12 },
  logoutText: { fontSize: 16, fontWeight: '700', color: '#0F172A' },
  deleteBtn: { flexDirection: 'row', padding: 16, borderRadius: 16, alignItems: 'center', justifyContent: 'center', backgroundColor: '#FEF2F2' },
  deleteText: { fontSize: 16, fontWeight: '700', color: '#EF4444' },
});
