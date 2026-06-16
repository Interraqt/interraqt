import React, { useState } from 'react';
import { StyleSheet, Text, View, TextInput, TouchableOpacity, Image, KeyboardAvoidingView, Platform, ScrollView, ActivityIndicator } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Feather } from '@expo/vector-icons';
import * as ImagePicker from 'expo-image-picker';

export default function CreatePostScreen({ navigation }) {
  const insets = useSafeAreaInsets();
  const [caption, setCaption] = useState('');
  const [mediaUri, setMediaUri] = useState(null);
  const [mediaType, setMediaType] = useState(null); // 'image' or 'video'
  const [isUploading, setIsUploading] = useState(false);

  const pickMedia = async (type) => {
    // Ask for permission
    const permissionResult = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (permissionResult.granted === false) {
      alert("You've refused to allow this app to access your photos!");
      return;
    }

    // Open picker
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

  const handleShare = async () => {
    if (!caption.trim() && !mediaUri) {
      alert('Please add a photo, video, or caption to post.');
      return;
    }

    setIsUploading(true);
    
    // TODO: We will implement ImgBB and Cloudinary uploads here
    console.log("Ready to upload:", { mediaUri, mediaType, caption });
    
    // Simulate upload delay for now
    setTimeout(() => {
      setIsUploading(false);
      navigation.goBack();
    }, 2000);
  };

  return (
    <View style={styles.container}>
      {/* HEADER */}
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
          
          {/* CAPTION INPUT */}
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

          {/* MEDIA PREVIEW */}
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

      {/* BOTTOM ATTACHMENT BAR */}
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
