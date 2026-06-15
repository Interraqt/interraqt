import React, { useState, useEffect } from 'react';
import { StyleSheet, Text, View, TouchableOpacity, Image, Modal, TextInput, Alert, ActivityIndicator, Platform, StatusBar, ScrollView, SafeAreaView } from 'react-native';
import { auth, db } from '../config/firebase';
import { signOut, deleteUser } from 'firebase/auth';
import { doc, getDoc, updateDoc, deleteDoc } from 'firebase/firestore';
import { Ionicons, Feather } from '@expo/vector-icons';

export default function ProfileScreen({ navigation }) {
  const [userData, setUserData] = useState(null);
  const [loading, setLoading] = useState(true);
  
  // Edit Profile State
  const [isEditVisible, setIsEditVisible] = useState(false);
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
    try {
      await updateDoc(doc(db, 'users', auth.currentUser.uid), {
        name: editName,
        username: editUsername.toLowerCase().replace(/\s/g, ''),
      });
      setUserData({ ...userData, name: editName, username: editUsername.toLowerCase().replace(/\s/g, '') });
      setIsEditVisible(false);
      Alert.alert("Saved", "Your profile has been updated.");
    } catch (error) {
      Alert.alert("Error", error.message);
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
        
        {/* 1. Cover Banner & Header Icons */}
        <View style={styles.coverPhoto}>
          <View style={styles.coverHeader}>
            <Text style={styles.headerTitle}>@{userData?.username}</Text>
            <TouchableOpacity onPress={() => setIsEditVisible(true)} style={styles.settingsIcon}>
              <Ionicons name="settings-outline" size={24} color="#fff" />
            </TouchableOpacity>
          </View>
        </View>

        {/* 2. Main Profile Card */}
        <View style={styles.profileSection}>
          
          {/* Overlapping Avatar */}
          <View style={styles.avatarContainer}>
            <Image source={{ uri: 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png' }} style={styles.avatar} />
            <TouchableOpacity style={styles.addPhotoBadge}>
              <Ionicons name="camera" size={14} color="#fff" />
            </TouchableOpacity>
          </View>

          {/* User Info */}
          <Text style={styles.name}>{userData?.name || 'Full Name'}</Text>
          <Text style={styles.bio}>Building something awesome! 🚀✨</Text>

          {/* Floating Stats Pill */}
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

          {/* Sleek Action Buttons */}
          <View style={styles.actionRow}>
            <TouchableOpacity style={styles.primaryBtn} onPress={() => setIsEditVisible(true)}>
              <Text style={styles.primaryBtnText}>Edit Profile</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.secondaryBtn}>
              <Feather name="share" size={18} color="#000" />
            </TouchableOpacity>
          </View>

        </View>

        {/* 3. Unique Content Area */}
        <View style={styles.contentArea}>
          <Text style={styles.sectionTitle}>Recent Activity</Text>
          <View style={styles.placeholderCard}>
            <Ionicons name="image-outline" size={32} color="#ccc" />
            <Text style={styles.placeholderText}>No posts yet.</Text>
          </View>
        </View>

      </ScrollView>

      {/* --- EDIT PROFILE MODAL --- */}
      <Modal visible={isEditVisible} animationType="slide" presentationStyle="pageSheet">
        <SafeAreaView style={styles.modalContainer}>
          <View style={styles.modalHeader}>
            <TouchableOpacity onPress={() => setIsEditVisible(false)}>
              <Text style={styles.cancelText}>Cancel</Text>
            </TouchableOpacity>
            <Text style={styles.modalTitle}>Settings</Text>
            <TouchableOpacity onPress={handleSaveProfile}>
              <Text style={styles.saveText}>Save</Text>
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
                <Ionicons name="log-out-outline" size={20} color="#000" style={{marginRight: 8}} />
                <Text style={styles.logoutText}>Log Out</Text>
              </TouchableOpacity>

              <TouchableOpacity style={styles.deleteBtn} onPress={handleDeleteAccount}>
                <Ionicons name="trash-outline" size={20} color="red" style={{marginRight: 8}} />
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
  loadingContainer: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#FAFAFA' },
  container: { flex: 1, backgroundColor: '#FAFAFA' },
  
  // Cover Photo (Fixes Android Overlap with paddingTop)
  coverPhoto: { height: 160, backgroundColor: '#0056b3', paddingTop: Platform.OS === 'android' ? StatusBar.currentHeight + 10 : 40, paddingHorizontal: 20 },
  coverHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  headerTitle: { color: '#fff', fontSize: 18, fontWeight: 'bold' },
  settingsIcon: { padding: 4, backgroundColor: 'rgba(0,0,0,0.2)', borderRadius: 20 },

  // Profile Info Section
  profileSection: { backgroundColor: '#FAFAFA', borderTopLeftRadius: 24, borderTopRightRadius: 24, marginTop: -20, alignItems: 'center', paddingHorizontal: 20, paddingBottom: 20 },
  avatarContainer: { marginTop: -45, marginBottom: 12, position: 'relative' },
  avatar: { width: 90, height: 90, borderRadius: 45, borderWidth: 4, borderColor: '#FAFAFA' },
  addPhotoBadge: { position: 'absolute', bottom: 4, right: 0, backgroundColor: '#0056b3', padding: 6, borderRadius: 15, borderWidth: 2, borderColor: '#FAFAFA' },
  name: { fontSize: 22, fontWeight: 'bold', color: '#111' },
  bio: { fontSize: 15, color: '#666', marginTop: 4, marginBottom: 20 },

  // Floating Stats Card
  statsCard: { flexDirection: 'row', backgroundColor: '#FFF', width: '100%', borderRadius: 16, paddingVertical: 16, shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.05, shadowRadius: 10, elevation: 2, marginBottom: 20 },
  statItem: { flex: 1, alignItems: 'center' },
  statValue: { fontSize: 18, fontWeight: 'bold', color: '#111' },
  statLabel: { fontSize: 13, color: '#888', marginTop: 4 },
  statDivider: { width: 1, backgroundColor: '#F0F0F0', height: '80%', alignSelf: 'center' },

  // Action Buttons
  actionRow: { flexDirection: 'row', width: '100%', gap: 12 },
  primaryBtn: { flex: 1, backgroundColor: '#111', paddingVertical: 14, borderRadius: 12, alignItems: 'center' },
  primaryBtnText: { color: '#fff', fontSize: 15, fontWeight: 'bold' },
  secondaryBtn: { backgroundColor: '#fff', padding: 14, borderRadius: 12, borderWidth: 1, borderColor: '#EAEAEA', alignItems: 'center', justifyContent: 'center' },

  // Content Area
  contentArea: { paddingHorizontal: 20, paddingTop: 10, paddingBottom: 40 },
  sectionTitle: { fontSize: 18, fontWeight: 'bold', color: '#111', marginBottom: 12 },
  placeholderCard: { height: 150, backgroundColor: '#FFF', borderRadius: 16, alignItems: 'center', justifyContent: 'center', borderWidth: 1, borderColor: '#EAEAEA', borderStyle: 'dashed' },
  placeholderText: { color: '#aaa', marginTop: 8 },

  // Modal Styles
  modalContainer: { flex: 1, backgroundColor: '#FFF' },
  modalHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: 16, borderBottomWidth: 1, borderColor: '#F0F0F0' },
  cancelText: { fontSize: 16, color: '#666' },
  modalTitle: { fontSize: 16, fontWeight: 'bold' },
  saveText: { fontSize: 16, fontWeight: 'bold', color: '#0056b3' },
  modalContent: { padding: 20 },
  inputGroup: { marginBottom: 20 },
  inputLabel: { color: '#888', fontSize: 13, marginBottom: 6 },
  input: { fontSize: 16, borderBottomWidth: 1, borderColor: '#EAEAEA', paddingBottom: 8, color: '#111' },
  dangerZone: { marginTop: 30, borderTopWidth: 1, borderColor: '#F0F0F0', paddingTop: 20 },
  dangerTitle: { color: '#888', fontSize: 13, marginBottom: 16 },
  logoutBtn: { flexDirection: 'row', backgroundColor: '#F5F5F5', padding: 16, borderRadius: 12, alignItems: 'center', justifyContent: 'center', marginBottom: 12 },
  logoutText: { fontSize: 15, fontWeight: 'bold', color: '#111' },
  deleteBtn: { flexDirection: 'row', padding: 16, borderRadius: 12, alignItems: 'center', justifyContent: 'center', backgroundColor: '#FFF0F0' },
  deleteText: { fontSize: 15, fontWeight: 'bold', color: 'red' },
});
