import React, { useState, useEffect } from 'react';
import { StyleSheet, Text, View, TouchableOpacity, Image, ActivityIndicator, ScrollView } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { auth, db } from '../config/firebase';
import { doc, onSnapshot } from 'firebase/firestore';
import { Feather } from '@expo/vector-icons'; 

export default function ProfileScreen({ navigation }) {
  const insets = useSafeAreaInsets(); 
  const [userData, setUserData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // onSnapshot automatically updates the screen if you change your name!
    const unsubscribe = onSnapshot(doc(db, 'users', auth.currentUser.uid), (docSnap) => {
      if (docSnap.exists()) setUserData(docSnap.data());
      setLoading(false);
    });
    return unsubscribe;
  }, []);

  if (loading) return <View style={styles.loadingContainer}><ActivityIndicator size="large" color="#000" /></View>;

  return (
    <View style={styles.container}>
      <View style={[styles.header, { paddingTop: insets.top + 10 }]}>
        <Text style={styles.headerTitle}>{userData?.username}</Text>
        <TouchableOpacity onPress={() => navigation.navigate('Settings')} style={styles.menuBtn}>
          <View style={styles.menuLine} />
          <View style={[styles.menuLine, { width: 14 }]} />
        </TouchableOpacity>
      </View>

      <ScrollView contentContainerStyle={styles.scrollContent} bounces={true} showsVerticalScrollIndicator={false}>
        <View style={styles.avatarContainer}>
          <Image source={{ uri: 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png' }} style={styles.avatar} />
        </View>

        <Text style={styles.name}>{userData?.name || 'Full Name'}</Text>
        <Text style={styles.bio}>Building something awesome! 🚀</Text>

        <TouchableOpacity style={styles.primaryBtn} onPress={() => navigation.navigate('EditProfile')}>
          <Text style={styles.primaryBtnText}>Edit Profile</Text>
        </TouchableOpacity>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  loadingContainer: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#FFF' },
  container: { flex: 1, backgroundColor: '#FFF' },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 20, paddingBottom: 16, backgroundColor: '#FFF' },
  headerTitle: { color: '#000', fontSize: 22, fontWeight: '900', letterSpacing: -0.5 },
  menuBtn: { alignItems: 'flex-end', justifyContent: 'center', paddingVertical: 8, paddingLeft: 8 },
  menuLine: { width: 22, height: 2.5, backgroundColor: '#000', borderRadius: 2, marginBottom: 5 },
  
  scrollContent: { alignItems: 'center', paddingHorizontal: 24, paddingTop: 40 },
  avatarContainer: { marginBottom: 20, shadowColor: '#000', shadowOffset: { width: 0, height: 8 }, shadowOpacity: 0.1, shadowRadius: 12, elevation: 5 },
  avatar: { width: 110, height: 110, borderRadius: 55, borderWidth: 1, borderColor: '#F0F0F0' },
  name: { fontSize: 24, fontWeight: '900', color: '#000', letterSpacing: -0.5 },
  bio: { fontSize: 15, color: '#666', marginTop: 8, marginBottom: 32, fontWeight: '500' },

  primaryBtn: { width: '100%', backgroundColor: '#000', paddingVertical: 16, borderRadius: 16, alignItems: 'center' },
  primaryBtnText: { color: '#fff', fontSize: 16, fontWeight: '800' },
});
