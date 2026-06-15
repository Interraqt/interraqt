import React, { useState, useEffect } from 'react';
import { StyleSheet, Text, View, TouchableOpacity, Image, Modal, TextInput, Alert, ActivityIndicator, ScrollView, SafeAreaView } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { auth, db } from '../config/firebase';
import { doc, getDoc, updateDoc } from 'firebase/firestore';
import { Feather } from '@expo/vector-icons'; 

export default function ProfileScreen({ navigation }) {
  const insets = useSafeAreaInsets(); 
  const [userData, setUserData] = useState(null);
  const [loading, setLoading] = useState(true);
  
  const [isEditVisible, setIsEditVisible] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [editName, setEditName] = useState('');
  const [editUsername, setEditUsername] = useState('');

  useEffect(() => { fetchUser(); }, []);

  const fetchUser = async () => {
    try {
      const docSnap = await getDoc(doc(db, 'users', auth.currentUser.uid));
      if (docSnap.exists()) {
        setUserData(docSnap.data());
        setEditName(docSnap.data().name);
        setEditUsername(docSnap.data().username);
      }
    } catch (error) { console.log(error); } finally { setLoading(false); }
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
    } finally { setIsSaving(false); }
  };

  if (loading) return <View style={styles.loadingContainer}><ActivityIndicator size="large" color="#000" /></View>;

  return (
    <View style={styles.container}>
      <ScrollView bounces={false} showsVerticalScrollIndicator={false}>
        
        <View style={[styles.header, { paddingTop: insets.top + 10 }]}>
          <Text style={styles.headerTitle}>{userData?.username}</Text>
          <TouchableOpacity onPress={() => navigation.navigate('Settings')} style={styles.menuBtn}>
            <View style={styles.menuLine} />
            <View style={[styles.menuLine, { width: 16 }]} />
          </TouchableOpacity>
        </View>

        <View style={styles.profileSection}>
          <View style={styles.avatarContainer}>
            <Image source={{ uri: 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png' }} style={styles.avatar} />
          </View>

          <Text style={styles.name}>{userData?.name || 'Full Name'}</Text>
          <Text style={styles.bio}>Building something awesome! 🚀</Text>

          <View style={styles.statsCard}>
            <View style={styles.statItem}><Text style={styles.statValue}>0</Text><Text style={styles.statLabel}>Posts</Text></View>
            <View style={styles.statDivider} />
            <View style={styles.statItem}><Text style={styles.statValue}>124</Text><Text style={styles.statLabel}>Followers</Text></View>
            <View style={styles.statDivider} />
            <View style={styles.statItem}><Text style={styles.statValue}>180</Text><Text style={styles.statLabel}>Following</Text></View>
          </View>

          <View style={styles.actionRow}>
            <TouchableOpacity style={styles.primaryBtn} onPress={() => setIsEditVisible(true)}>
              <Text style={styles.primaryBtnText}>Edit Profile</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.secondaryBtn}>
              <Feather name="share" size={20} color="#000" />
            </TouchableOpacity>
          </View>
        </View>
      </ScrollView>

      {/* SLEEK B&W EDIT MODAL */}
      <Modal visible={isEditVisible} animationType="slide" presentationStyle="pageSheet">
        <SafeAreaView style={styles.modalContainer}>
          <View style={styles.modalHeader}>
            <TouchableOpacity onPress={() => setIsEditVisible(false)}><Text style={styles.cancelText}>Cancel</Text></TouchableOpacity>
            <Text style={styles.modalTitle}>Edit Profile</Text>
            <TouchableOpacity onPress={handleSaveProfile} disabled={isSaving}>
              {isSaving ? <ActivityIndicator size="small" color="#000" /> : <Text style={styles.saveText}>Save</Text>}
            </TouchableOpacity>
          </View>

          <ScrollView contentContainerStyle={styles.modalContent} keyboardShouldPersistTaps="handled">
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>Name</Text>
              <TextInput style={styles.input} value={editName} onChangeText={setEditName} />
            </View>
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>Username</Text>
              <TextInput style={styles.input} value={editUsername} onChangeText={setEditUsername} autoCapitalize="none" />
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
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 20, paddingBottom: 16, backgroundColor: '#FFF', borderBottomWidth: 1, borderColor: '#EAEAEA' },
  headerTitle: { color: '#000', fontSize: 20, fontWeight: '800' },
  menuBtn: { alignItems: 'flex-end', justifyContent: 'center', paddingVertical: 8, paddingLeft: 8 },
  menuLine: { width: 22, height: 2.5, backgroundColor: '#000', borderRadius: 2, marginBottom: 5 },
  
  profileSection: { backgroundColor: '#FAFAFA', alignItems: 'center', paddingHorizontal: 20, paddingTop: 24, paddingBottom: 24 },
  avatarContainer: { marginBottom: 16 },
  avatar: { width: 90, height: 90, borderRadius: 45, borderWidth: 1, borderColor: '#EAEAEA' },
  name: { fontSize: 22, fontWeight: '800', color: '#000' },
  bio: { fontSize: 15, color: '#666', marginTop: 4, marginBottom: 24 },

  statsCard: { flexDirection: 'row', width: '100%', marginBottom: 24 },
  statItem: { flex: 1, alignItems: 'center' },
  statValue: { fontSize: 18, fontWeight: '800', color: '#000' },
  statLabel: { fontSize: 13, color: '#888', marginTop: 4 },
  statDivider: { width: 1, backgroundColor: '#EAEAEA', height: '60%', alignSelf: 'center' },

  actionRow: { flexDirection: 'row', width: '100%', gap: 12 },
  primaryBtn: { flex: 1, backgroundColor: '#000', paddingVertical: 14, borderRadius: 12, alignItems: 'center' },
  primaryBtnText: { color: '#fff', fontSize: 15, fontWeight: '700' },
  secondaryBtn: { backgroundColor: '#FAFAFA', padding: 14, borderRadius: 12, borderWidth: 1, borderColor: '#EAEAEA', alignItems: 'center', justifyContent: 'center' },

  modalContainer: { flex: 1, backgroundColor: '#FFF' },
  modalHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: 20, borderBottomWidth: 1, borderColor: '#EAEAEA' },
  cancelText: { fontSize: 16, color: '#000' },
  modalTitle: { fontSize: 18, fontWeight: '800', color: '#000' },
  saveText: { fontSize: 16, fontWeight: 'bold', color: '#000' },
  modalContent: { padding: 24 },
  inputGroup: { marginBottom: 24 },
  inputLabel: { color: '#888', fontSize: 13, marginBottom: 8, fontWeight: '600' },
  input: { fontSize: 16, borderBottomWidth: 1, borderColor: '#EAEAEA', paddingBottom: 12, color: '#000' },
});
