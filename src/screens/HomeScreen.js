import React, { useState, useEffect } from 'react';
import { StyleSheet, Text, View, FlatList, Image, TouchableOpacity, Dimensions, ActivityIndicator, Share } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Feather } from '@expo/vector-icons';
import { useFonts, TaiHeritagePro_700Bold } from '@expo-google-fonts/tai-heritage-pro';
import { db } from '../config/firebase';
import { collection, query, orderBy, onSnapshot } from 'firebase/firestore';

// NEW IMPORTS
import { Video, ResizeMode } from 'expo-av';
import Modal from 'react-native-modal'; 
import LikeButton from '../components/LikeButton';
import SaveButton from '../components/SaveButton';

const { width } = Dimensions.get('window');

export default function HomeScreen({ navigation }) {
  const insets = useSafeAreaInsets();
  const [posts, setPosts] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  
  // Modal State
  const [modalVisible, setModalVisible] = useState(false);
  const [selectedPost, setSelectedPost] = useState(null);

  let [fontsLoaded] = useFonts({ TaiHeritagePro_700Bold });

  useEffect(() => {
    const q = query(collection(db, 'posts'), orderBy('createdAt', 'desc'));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const fetchedPosts = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
      setPosts(fetchedPosts);
      setIsLoading(false);
    });
    return () => unsubscribe();
  }, []);

  const openMenu = (post) => {
    setSelectedPost(post);
    setModalVisible(true);
  };

  // Functional Native Share
  const handleShare = async (post) => {
    try {
      await Share.share({
        message: `Check out this post by ${post.user?.username} on Interraqt: ${post.caption}`,
        url: post.imageUrl // iOS uses this natively
      });
    } catch (error) {
      console.log(error.message);
    }
  };

  const renderPost = ({ item }) => (
    <View style={styles.postContainer}>
      {/* HEADER */}
      <View style={styles.postHeader}>
        <View style={styles.userInfoContainer}>
          <Image source={{ uri: item.user?.avatar || 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png' }} style={styles.avatar} />
          <View style={styles.userTextContainer}>
            <Text style={styles.username}>{item.user?.username}</Text>
            <Text style={styles.name}>{item.user?.name}</Text>
          </View>
        </View>
        <TouchableOpacity style={styles.moreButton} onPress={() => openMenu(item)}>
          <Feather name="more-horizontal" size={20} color="#000" />
        </TouchableOpacity>
      </View>

      {/* MEDIA HANDLER: IMAGE vs VIDEO */}
      {item.imageUrl && (
        item.mediaType === 'video' ? (
          <Video
            source={{ uri: item.imageUrl }}
            style={styles.postMedia}
            useNativeControls
            resizeMode={ResizeMode.COVER}
            isLooping
            shouldPlay={false} // Prevents auto-playing all videos at once
          />
        ) : (
          <Image source={{ uri: item.imageUrl }} style={styles.postMedia} />
        )
      )}

      {/* ACTION BAR */}
      <View style={styles.actionBar}>
        <View style={styles.actionLeft}>
          <LikeButton post={item} />
          
          <TouchableOpacity style={styles.actionIcon} onPress={() => console.log('Navigate to Comments Screen')}>
            <Feather name="message-circle" size={24} color="#000" />
            <Text style={styles.countText}>{item.commentsCount > 0 ? item.commentsCount : ''}</Text>
          </TouchableOpacity>
          
          <TouchableOpacity style={styles.actionIcon} onPress={() => handleShare(item)}>
            <Feather name="send" size={24} color="#000" />
          </TouchableOpacity>
        </View>
        
        <SaveButton post={item} />
      </View>

      {/* CAPTION */}
      <View style={styles.contentArea}>
        <Text style={styles.captionText}>
          <Text style={styles.captionUsername}>{item.user?.username} </Text>
          {item.caption}
        </Text>
      </View>
    </View>
  );

  if (!fontsLoaded) return null;

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Interraqt</Text>
      </View>

      {isLoading ? (
        <View style={styles.loadingContainer}><ActivityIndicator size="large" color="#000" /></View>
      ) : (
        <FlatList
          data={posts}
          keyExtractor={(item) => item.id}
          renderItem={renderPost}
          showsVerticalScrollIndicator={false}
          contentContainerStyle={{ paddingBottom: 120 }}
        />
      )}

      {/* NEW: PREMIUM DRAGGABLE BOTTOM SHEET */}
      <Modal
        isVisible={modalVisible}
        onBackdropPress={() => setModalVisible(false)} // Tap outside to close
        onSwipeComplete={() => setModalVisible(false)} // Drag down to close
        swipeDirection={['down']}
        backdropOpacity={0.5} // Dims the background perfectly
        style={styles.modalStyle}
      >
        <View style={[styles.modalContent, { paddingBottom: insets.bottom || 20 }]}>
          
          <View style={styles.dragHandle} />

          {/* Cleaned Menu: Icon First, Aligned */}
          <View style={styles.menuGroup}>
            <TouchableOpacity style={styles.menuItem}>
              <Feather name="link" size={20} color="#000" style={styles.menuIcon} />
              <Text style={styles.menuText}>Copy link</Text>
            </TouchableOpacity>
            
            <TouchableOpacity style={styles.menuItem}>
              <Feather name="eye-off" size={20} color="#000" style={styles.menuIcon} />
              <Text style={styles.menuText}>Not interested</Text>
            </TouchableOpacity>

            <TouchableOpacity style={styles.menuItem}>
              <Feather name="eye" size={20} color="#000" style={styles.menuIcon} />
              <Text style={styles.menuText}>Interested</Text>
            </TouchableOpacity>

            <TouchableOpacity style={[styles.menuItem, { borderBottomWidth: 0 }]}>
              <Feather name="info" size={20} color="#000" style={styles.menuIcon} />
              <Text style={styles.menuText}>About account</Text>
            </TouchableOpacity>
          </View>

        </View>
      </Modal>

    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FFFFFF' },
  header: { height: 50, justifyContent: 'center', alignItems: 'center', borderBottomWidth: 0.5, borderBottomColor: '#EFEFEF' },
  headerTitle: { fontFamily: 'TaiHeritagePro_700Bold', fontSize: 28, color: '#000', marginTop: -4 },
  loadingContainer: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  
  postContainer: { marginBottom: 20 },
  postHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 16, paddingVertical: 12 },
  userInfoContainer: { flexDirection: 'row', alignItems: 'center' },
  avatar: { width: 36, height: 36, borderRadius: 18, backgroundColor: '#EFEFEF', marginRight: 10 },
  userTextContainer: { justifyContent: 'center' },
  username: { fontSize: 14, fontWeight: '700', color: '#000' },
  name: { fontSize: 12, color: '#666', marginTop: 1 },
  moreButton: { padding: 4 },
  
  // Handles both Images and Videos
  postMedia: { width: width, height: width * 1.25, backgroundColor: '#FAFAFA' },
  
  actionBar: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 16, paddingVertical: 12 },
  actionLeft: { flexDirection: 'row', alignItems: 'center' },
  actionIcon: { flexDirection: 'row', alignItems: 'center', marginRight: 16 },
  countText: { marginLeft: 6, fontSize: 14, fontWeight: '600', color: '#000' },
  
  contentArea: { paddingHorizontal: 16 },
  captionText: { fontSize: 14, color: '#000', lineHeight: 20 },
  captionUsername: { fontWeight: '700' },

  // Draggable Modal Styles
  modalStyle: { justifyContent: 'flex-end', margin: 0 },
  modalContent: { backgroundColor: '#F9F9F9', borderTopLeftRadius: 24, borderTopRightRadius: 24, paddingTop: 12, paddingHorizontal: 16 },
  dragHandle: { width: 40, height: 4, backgroundColor: '#CCC', borderRadius: 2, alignSelf: 'center', marginBottom: 20 },
  
  menuGroup: { backgroundColor: '#FFF', borderRadius: 16, marginBottom: 16, overflow: 'hidden' },
  menuItem: { flexDirection: 'row', alignItems: 'center', paddingVertical: 16, paddingHorizontal: 20, borderBottomWidth: 0.5, borderBottomColor: '#EAEAEA' },
  menuIcon: { marginRight: 16 },
  menuText: { fontSize: 16, fontWeight: '600', color: '#000' }
});
