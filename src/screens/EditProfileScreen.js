import React, { useState, useEffect } from 'react';
import { StyleSheet, Text, View, TouchableOpacity, TextInput, ActivityIndicator, Alert, KeyboardAvoidingView, Platform, ScrollView, Image } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { auth, db } from '../config/firebase';
import { doc, getDoc, updateDoc } from 'firebase/firestore';
import * as ImagePicker from 'expo-image-picker';
import { Feather } from '@expo/vector-icons';

export default function EditProfileScreen({ navigation }) {
  const insets = useSafeAreaInsets();
  
  const [loading, setLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  
  const [editName, setEditName] = useState('');
  const [editUsername, setEditUsername] = useState('');
  const [editBio, setEditBio] = useState('');
  const [avatarUri, setAvatarUri] = useState(null);

  const IMGBB_API_KEY = '0e141525bdb42883f16d1ed98e55d93e'; 

  useEffect(() => {
    const fetchUser = async () => {
      try {
        const docSnap = await getDoc(doc(db, 'users', auth.currentUser.uid));
        if (docSnap.exists()) {
          setEditName(docSnap.data().name || '');
          setEditUsername(docSnap.data().username || '');
          setEditBio(docSnap.data().bio || '');
          setAvatarUri(docSnap.data().avatar || null);
        }
      } catch (error) { 
        console.log(error); 
      } finally { 
        setLoading(false); 
      }
    };
    fetchUser();
  }, []);

  const pickImage = async () => {
    const permissionResult = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (permissionResult.granted === false) {
      return Alert.alert("Required", "Please allow camera roll access to change your avatar.");
    }

    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsEditing: true,
      aspect: [1, 1], 
      quality: 0.5, 
    });

    if (!result.canceled) {
      setAvatarUri(result.assets[0].uri);
    }
  };

  const uploadAvatarToImgBB = async (uri) => {
    const formData = new FormData();
    formData.append('image', { uri: uri, type: 'image/jpeg', name: 'avatar.jpg' });

    const response = await fetch(`https://api.imgbb.com/1/upload?key=${IMGBB_API_KEY}`, {
      method: 'POST',
      body: formData,
      headers: { 'Accept': 'application/json', 'Content-Type': 'multipart/form-data' },
    });
    const data = await response.json();
    return data.data.url; 
  };

  const handleSave = async () => {
    if (!editName.trim() || !editUsername.trim()) {
      return Alert.alert("Required", "Name and Username cannot be empty.");
    }

    setIsSaving(true);
    try {
      let liveAvatarUrl = avatarUri;

      // If the avatarUri is a local file from the phone, upload it to ImgBB
      if (avatarUri && avatarUri.startsWith('file://')) {
        liveAvatarUrl = await uploadAvatarToImgBB(avatarUri);
      }

      await updateDoc(doc(db, 'users', auth.currentUser.uid), {
        name: editName.trim(),
        username: editUsername.toLowerCase().replace(/\s/g, ''),
        bio: editBio.trim(),
        avatar: liveAvatarUrl
      });
      
      setIsSaving(false);
      navigation.goBack();
    } catch (error) {
      Alert.alert("Error", error.message);
      setIsSaving(false);
    } 
  };

  if (loading) return <View style={styles.loadingContainer}><ActivityIndicator size="large" color="#000" /></View>;

  return (
    <View style={styles.container}>
      
      {/* HEADER */}
      <View style={[styles.header, { paddingTop: insets.top + 10 }]}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.cancelBtn} disabled={isSaving}>
          <Text style={styles.cancelText}>Cancel</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Edit Profile</Text>
        <TouchableOpacity onPress={handleSave} disabled={isSaving} style={styles.saveBtn}>
          {isSaving ? <ActivityIndicator size="small" color="#000" /> : <Text style={styles.saveText}>Save</Text>}
        </TouchableOpacity>
      </View>

      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={{ flex: 1 }}>
        <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
          
          {/* AVATAR UPLOAD SECTION */}
          <View style={styles.avatarSection}>
            <TouchableOpacity onPress={pickImage} style={styles.avatarWrapper} disabled={isSaving}>
              <Image 
                source={{ uri: avatarUri || 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png' }} 
                style={styles.avatarImage} 
              />
              <View style={styles.editIconOverlay}>
                <Feather name="camera" size={16} color="#FFF" />
              </View>
            </TouchableOpacity>
          </View>

          {/* INPUT FIELDS */}
          <View style={styles.inputGroup}>
            <Text style={styles.inputLabel}>Name</Text>
            <TextInput style={styles.input} value={editName} onChangeText={setEditName} placeholderTextColor="#999" editable={!isSaving} />
          </View>
          
          <View style={styles.inputGroup}>
            <Text style={styles.inputLabel}>Username</Text>
            <TextInput style={styles.input} value={editUsername} onChangeText={setEditUsername} autoCapitalize="none" placeholderTextColor="#999" editable={!isSaving} />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.inputLabel}>Bio</Text>
            <TextInput 
              style={[styles.input, styles.bioInput]} 
              value={editBio} 
              onChangeText={setEditBio} 
              placeholder="Tell the world about yourself..." 
              placeholderTextColor="#999" 
              multiline 
              maxLength={150} 
              editable={!isSaving} 
            />
          </View>

        </ScrollView>
      </KeyboardAvoidingView>
    </View>
  );
}

const styles = StyleSheet.create({
  loadingContainer: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#FFF' },
  container: { flex: 1, backgroundColor: '#FFF' },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 20, paddingBottom: 16, backgroundColor: '#FFF', borderBottomWidth: 1, borderColor: '#F0F0F0' },
  cancelBtn: { minWidth: 60, alignItems: 'flex-start' },
  cancelText: { fontSize: 16, color: '#666', fontWeight: '500' },
  headerTitle: { fontSize: 18, fontWeight: '800', color: '#000' },
  saveBtn: { minWidth: 60, alignItems: 'flex-end' },
  saveText: { fontSize: 16, fontWeight: '800', color: '#000' },
  
  content: { padding: 24 },
  
  avatarSection: { alignItems: 'center', marginBottom: 32 },
  avatarWrapper: { position: 'relative' },
  avatarImage: { width: 100, height: 100, borderRadius: 50, backgroundColor: '#F0F0F0', borderWidth: 1, borderColor: '#EAEAEA' },
  editIconOverlay: { position: 'absolute', bottom: 0, right: 0, backgroundColor: '#000', width: 32, height: 32, borderRadius: 16, justifyContent: 'center', alignItems: 'center', borderWidth: 3, borderColor: '#FFF' },

  inputGroup: { marginBottom: 32 },
  inputLabel: { color: '#999', fontSize: 13, marginBottom: 8, fontWeight: '700', textTransform: 'uppercase', letterSpacing: 0.5 },
  input: { fontSize: 18, borderBottomWidth: 1, borderColor: '#EAEAEA', paddingBottom: 12, color: '#000', fontWeight: '600' },
  bioInput: { minHeight: 40, textAlignVertical: 'top' },
});
