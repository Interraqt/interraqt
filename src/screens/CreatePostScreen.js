import React, { useState } from 'react';
import { StyleSheet, Text, View, TextInput, TouchableOpacity, Image, KeyboardAvoidingView, Platform, ScrollView, ActivityIndicator, Alert } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Feather } from '@expo/vector-icons';
import * as ImagePicker from 'expo-image-picker';
import { auth, db } from '../config/firebase';
import { doc, getDoc, collection, addDoc } from 'firebase/firestore';

export default function CreatePostScreen({ navigation }) {
  const insets = useSafeAreaInsets();
  const [caption, setCaption] = useState('');
  const [mediaUri, setMediaUri] = useState(null);
  const [mediaType, setMediaType] = useState(null); 
  const [isUploading, setIsUploading] = useState(false);

  // --- LIVE CLOUD KEYS CONNECTED ---
  const IMGBB_API_KEY = '0e141525bdb42883f16d1ed98e55d93e'; 
  const CLOUDINARY_CLOUD_NAME = 'dybqzihn1';
  const CLOUDINARY_UPLOAD_PRESET = 'Interraqt'; 

  const pickMedia = async (type) => {
    const permissionResult = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (permissionResult.granted === false) {
      return Alert.alert("Required", "Please allow camera roll access to upload.");
    }

    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: type === 'video' ? ImagePicker.MediaTypeOptions.Videos : ImagePicker.MediaTypeOptions.Images,
      allowsEditing: true,
      quality: 0.8,
    });

    if (!result.canceled) {
      setMediaUri(result.assets[0].uri);
      setMediaType(type);
    }
  };

  const uploadPhotoToImgBB = async (uri) => {
    const formData = new FormData();
    formData.append('image', { uri: uri, type: 'image/jpeg', name: 'upload.jpg' });

    const response = await fetch(`https://api.imgbb.com/1/upload?key=${IMGBB_API_KEY}`, {
      method: 'POST',
      body: formData,
      headers: { 'Accept': 'application/json', 'Content-Type': 'multipart/form-data' },
    });
    const data = await response.json();
    return data.data.url; 
  };

  const uploadVideoToCloudinary = async (uri) => {
    const formData = new FormData();
    formData.append('file', { uri: uri, type: 'video/mp4', name: 'upload.mp4' });
    formData.append('upload_preset', CLOUDINARY_UPLOAD_PRESET);

    const response = await fetch(`https://api.cloudinary.com/v1_1/${CLOUDINARY_CLOUD_NAME}/video/upload`, {
      method: 'POST',
      body: formData,
      headers: { 'Accept': 'application/json', 'Content-Type': 'multipart/form-data' },
    });
    const data = await response.json();
    return data.secure_url; 
  };

  const handleShare = async () => {
    if (!caption.trim() && !mediaUri) {
      return Alert.alert('Hold on', 'Please add a photo, video, or caption to post.');
    }

    setIsUploading(true);
    
    try {
      let liveMediaUrl = null;

      // 1. Upload Media if it exists
      if (mediaUri) {
        if (mediaType === 'image') {
          liveMediaUrl = await uploadPhotoToImgBB(mediaUri);
        } else if (mediaType === 'video') {
          liveMediaUrl = await uploadVideoToCloudinary(mediaUri);
        }
      }

      // 2. Fetch current user's profile info
      const userDoc = await getDoc(doc(db, 'users', auth.currentUser.uid));
      const userData = userDoc.data();

      // 3. Save to Firebase Firestore
      await addDoc(collection(db, 'posts'), {
        userId: auth.currentUser.uid,
        user: {
          username: userData.username,
          name: userData.name,
          avatar: userData.avatar || 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png'
        },
        caption: caption.trim(),
        imageUrl: liveMediaUrl, 
        mediaType: mediaType || 'text',
        likesCount: 0,
        commentsCount: 0,
        createdAt: new Date().toISOString()
      });

      // 4. Success! Go back to the feed.
      setIsUploading(false);
      navigation.navigate('Home');

    } catch (error) {
      console.error(error);
      Alert.alert('Upload Failed', error.message);
      setIsUploading(false);
    }
  };

  return (
    <View style={styles.container}>
      <View style={[styles.header, { paddingTop: insets.top + 10 }]}>
        <TouchableOpacity onPress={() => navigation.goBack()} disabled={isUploading}>
          <Text style={styles.cancelText}>Cancel</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>New Post</Text>
        <TouchableOpacity onPress={handleShare} disabled={isUploading}>
          {isUploading ? (
            <ActivityIndicator size="small" color="#007AFF" />
          ) : (
            <Text style={styles.shareText}>Share</Text>
          )}
        </TouchableOpacity>
      </View>

      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={{ flex: 1 }}>
        <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
          
          <TextInput
            style={styles.captionInput}
            placeholder="Write a caption..."
            placeholderTextColor="#999"
            multiline
            maxLength={2200}
            value={caption}
            onChangeText={setCaption}
            autoFocus
          />

          {mediaUri && (
            <View style={styles.previewContainer}>
              <Image source={{ uri: mediaUri }} style={styles.mediaPreview} />
              <TouchableOpacity style={styles.removeMediaBtn} onPress={() => { setMediaUri(null); setMediaType(null); }}>
                <Feather name="x" size={16} color="#FFF" />
              </TouchableOpacity>
              {mediaType === 'video' && (
                <View style={styles.videoBadge}>
                  <Feather name="video" size={14} color="#FFF" />
                </View>
              )}
            </View>
          )}

        </ScrollView>
      </KeyboardAvoidingView>

      <View style={[styles.attachmentBar, { paddingBottom: insets.bottom || 20 }]}>
        <TouchableOpacity style={styles.attachBtn} onPress={() => pickMedia('image')}>
          <Feather name="image" size={24} color="#000" />
          <Text style={styles.attachText}>Photo</Text>
        </TouchableOpacity>
        
        <TouchableOpacity style={styles.attachBtn} onPress={() => pickMedia('video')}>
          <Feather name="video" size={24} color="#000" />
          <Text style={styles.attachText}>Video</Text>
        </TouchableOpacity>
      </View>

    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FFF' },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 16, paddingBottom: 16, borderBottomWidth: 1, borderBottomColor: '#F0F0F0' },
  cancelText: { fontSize: 16, color: '#000' },
  headerTitle: { fontSize: 18, fontWeight: '800', color: '#000' },
  shareText: { fontSize: 16, fontWeight: '700', color: '#007AFF' },
  content: { padding: 16 },
  captionInput: { fontSize: 16, color: '#000', minHeight: 80, textAlignVertical: 'top' },
  previewContainer: { marginTop: 20, position: 'relative', alignSelf: 'flex-start' },
  mediaPreview: { width: 200, height: 250, borderRadius: 12, backgroundColor: '#F0F0F0' },
  removeMediaBtn: { position: 'absolute', top: 8, right: 8, backgroundColor: 'rgba(0,0,0,0.6)', borderRadius: 12, padding: 4 },
  videoBadge: { position: 'absolute', bottom: 8, left: 8, backgroundColor: 'rgba(0,0,0,0.6)', borderRadius: 12, padding: 6 },
  attachmentBar: { flexDirection: 'row', borderTopWidth: 1, borderTopColor: '#F0F0F0', paddingVertical: 12, paddingHorizontal: 16, backgroundColor: '#FFF' },
  attachBtn: { flexDirection: 'row', alignItems: 'center', marginRight: 24 },
  attachText: { fontSize: 15, fontWeight: '600', color: '#000', marginLeft: 8 },
});
