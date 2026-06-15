import React, { useState, useEffect } from 'react';
import { StyleSheet, Text, View, SafeAreaView, TouchableOpacity, Image, Modal, TextInput, Alert, ActivityIndicator } from 'react-native';
import { auth, db } from '../config/firebase';
import { signOut, deleteUser } from 'firebase/auth';
import { doc, getDoc, updateDoc, deleteDoc } from 'firebase/firestore';
import { Ionicons } from '@expo/vector-icons';

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
      // Update local state instantly
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
      // Navigate back to the root Login screen
      navigation.reset({ index: 0, routes: [{ name: 'Login' }] });
    } catch (error) {
      Alert.alert("Error", error.message);
    }
  };

  const handleDeleteAccount = () => {
    Alert.alert("Delete Account", "Are you sure? This will permanently delete your account and data.", [
      { text: "Cancel", style: "cancel" },
      { text: "Delete", style: "destructive", onPress: async () => {
          try {
            const user = auth.currentUser;
            await deleteDoc(doc(db, 'users', user.uid)); // Delete Firestore data
            await deleteUser(user); // Delete Auth account
            navigation.reset({ index: 0, routes: [{ name: 'Login' }] });
          } catch (error) {
            Alert.alert("Action Required", "For security, please log out and log back in before deleting your account.");
          }
        } 
      }
    ]);
  };

  if (loading) {
    return <View style={styles.loadingContainer}><ActivityIndicator size="large" color="#000" /></View>;
  }

  return (
    <SafeAreaView style={styles.container}>
      {/* 1. Header */}
      <View style={styles.header}>
        <Ionicons name="lock-closed-outline" size={16} color="black" />
        <Text style={styles.headerUsername}>{userData?.username || 'user'}</Text>
        <Ionicons name="chevron-down" size={16} color="black" style={{marginRight: 'auto', marginLeft: 4}} />
        <Ionicons name="add-square-outline" size={28} color="black" style={{marginRight: 16}} />
        <Ionicons name="menu" size={32} color="black" />
      </View>

      {/* 2. Profile Info Row */}
      <View style={styles.profileDataRow}>
        <Image source={{ uri: 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png' }} style={styles.profileImage} />
        <View style={styles.statsContainer}>
          <View style={styles.statBox}>
            <Text style={styles.statNumber}>0</Text>
            <Text style={styles.statLabel}>Posts</Text>
          </View>
          <View style={styles.statBox}>
            <Text style={styles.statNumber}>124</Text>
            <Text style={styles.statLabel}>Followers</Text>
          </View>
          <View style={styles.statBox}>
            <Text style={styles.statNumber}>180</Text>
            <Text style={styles.statLabel}>Following</Text>
          </View>
        </View>
      </View>

      {/* 3. Bio Section */}
      <View style={styles.bioContainer}>
        <Text style={styles.bioName}>{userData?.name || 'Full Name'}</Text>
        <Text style={styles.bioText}>Building something awesome! 🚀</Text>
      </View>

      {/* 4. Action Buttons */}
      <View style={styles.actionButtonRow}>
        <TouchableOpacity style={styles.actionButton} onPress={() => setIsEditVisible(true)}>
          <Text style={styles.actionButtonText}>Edit profile</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.actionButton}>
          <Text style={styles.actionButtonText}>Share profile</Text>
        </TouchableOpacity>
      </View>

      {/* 5. Post Grid Placeholder */}
      <View style={styles.gridTabs}>
        <Ionicons name="grid-outline" size={24} color="black" />
      </View>
      <View style={styles.gridContainer}>
         <View style={styles.placeholderSquare} />
      </View>

      {/* --- EDIT PROFILE MODAL --- */}
      <Modal visible={isEditVisible} animationType="slide" presentationStyle="pageSheet">
        <SafeAreaView style={styles.modalContainer}>
          
          <View style={styles.modalHeader}>
            <TouchableOpacity onPress={() => setIsEditVisible(false)}>
              <Text style={styles.cancelText}>Cancel</Text>
            </TouchableOpacity>
            <Text style={styles.modalTitle}>Edit Profile</Text>
            <TouchableOpacity onPress={handleSaveProfile}>
              <Text style={styles.saveText}>Save</Text>
            </TouchableOpacity>
          </View>

          <View style={styles.modalContent}>
            <Image source={{ uri: 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png' }} style={styles.modalImage} />
            <Text style={styles.changePhotoText}>Change profile photo</Text>

            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>Name</Text>
              <TextInput style={styles.input} value={editName} onChangeText={setEditName} />
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>Username</Text>
              <TextInput style={styles.input} value={editUsername} onChangeText={setEditUsername} autoCapitalize="none" />
            </View>

            <View style={styles.dangerZone}>
              <TouchableOpacity style={styles.logoutBtn} onPress={handleLogOut}>
                <Text style={styles.logoutText}>Log Out</Text>
              </TouchableOpacity>

              <TouchableOpacity style={styles.deleteBtn} onPress={handleDeleteAccount}>
                <Text style={styles.deleteText}>Delete Account</Text>
              </TouchableOpacity>
            </View>
          </View>
        </SafeAreaView>
      </Modal>

    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  loadingContainer: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  container: { flex: 1, backgroundColor: '#ffffff' },
  header: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 16, paddingTop: 10, paddingBottom: 12 },
  headerUsername: { fontSize: 22, fontWeight: 'bold', marginLeft: 4 },
  profileDataRow: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 16, marginTop: 10 },
  profileImage: { width: 86, height: 86, borderRadius: 43 },
  statsContainer: { flex: 1, flexDirection: 'row', justifyContent: 'space-around', marginLeft: 20 },
  statBox: { alignItems: 'center' },
  statNumber: { fontSize: 18, fontWeight: 'bold' },
  statLabel: { fontSize: 13, color: '#222' },
  bioContainer: { paddingHorizontal: 16, marginTop: 12 },
  bioName: { fontWeight: 'bold', fontSize: 14 },
  bioText: { fontSize: 14, marginTop: 2 },
  actionButtonRow: { flexDirection: 'row', paddingHorizontal: 16, marginTop: 16, justifyContent: 'space-between' },
  actionButton: { flex: 1, backgroundColor: '#EFEFEF', paddingVertical: 8, borderRadius: 8, alignItems: 'center', marginHorizontal: 4 },
  actionButtonText: { fontWeight: '600', fontSize: 14, color: '#000' },
  gridTabs: { flexDirection: 'row', justifyContent: 'center', marginTop: 24, borderTopWidth: 0.5, borderColor: '#ddd', paddingVertical: 10 },
  gridContainer: { flex: 1, flexDirection: 'row' },
  placeholderSquare: { width: '33%', height: 120, backgroundColor: '#EFEFEF', borderWidth: 1, borderColor: '#fff' },
  
  // Modal Styles
  modalContainer: { flex: 1, backgroundColor: '#fff' },
  modalHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: 16, borderBottomWidth: 0.5, borderColor: '#ddd' },
  cancelText: { fontSize: 16, color: '#222' },
  modalTitle: { fontSize: 16, fontWeight: 'bold' },
  saveText: { fontSize: 16, fontWeight: 'bold', color: '#0056b3' },
  modalContent: { padding: 16, alignItems: 'center' },
  modalImage: { width: 100, height: 100, borderRadius: 50, marginBottom: 12 },
  changePhotoText: { color: '#0056b3', fontWeight: '600', marginBottom: 24 },
  inputGroup: { width: '100%', marginBottom: 16, borderBottomWidth: 0.5, borderColor: '#ddd', paddingBottom: 8 },
  inputLabel: { color: '#888', fontSize: 12, marginBottom: 4 },
  input: { fontSize: 16, paddingVertical: 4 },
  dangerZone: { marginTop: 40, width: '100%' },
  logoutBtn: { padding: 16, backgroundColor: '#f5f5f5', borderRadius: 8, alignItems: 'center', marginBottom: 12 },
  logoutText: { fontSize: 16, fontWeight: 'bold', color: '#222' },
  deleteBtn: { padding: 16, borderRadius: 8, alignItems: 'center' },
  deleteText: { fontSize: 16, fontWeight: 'bold', color: 'red' },
});
