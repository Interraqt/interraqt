import React, { useState, useEffect } from 'react';
import { StyleSheet, Text, View, FlatList, Image, TouchableOpacity, Modal, Dimensions, TouchableWithoutFeedback, ActivityIndicator } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Feather } from '@expo/vector-icons';
import { useFonts, TaiHeritagePro_700Bold } from '@expo-google-fonts/tai-heritage-pro';
import { db } from '../config/firebase';
import { collection, query, orderBy, onSnapshot } from 'firebase/firestore';

const { width } = Dimensions.get('window');

export default function HomeScreen() {
  const insets = useSafeAreaInsets();
  
  // State for real data and UI interaction
  const [posts, setPosts] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [modalVisible, setModalVisible] = useState(false);
  const [selectedPost, setSelectedPost] = useState(null);

  let [fontsLoaded] = useFonts({
    TaiHeritagePro_700Bold,
  });

  // REAL-TIME FIREBASE LISTENER
  useEffect(() => {
    const q = query(collection(db, 'posts'), orderBy('createdAt', 'desc'));
    
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const fetchedPosts = snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      }));
      setPosts(fetchedPosts);
      setIsLoading(false);
    }, (error) => {
      console.log("Error fetching posts:", error);
      setIsLoading(false);
    });

    return () => unsubscribe(); // Cleanup listener on unmount
  }, []);

  const openMenu = (post) => {
    setSelectedPost(post);
    setModalVisible(true);
  };

  const renderPost = ({ item }) => (
    <View style={styles.postContainer}>
      <View style={styles.postHeader}>
        <View style={styles.userInfoContainer}>
          {/* Fallback to a default avatar if none exists */}
          <Image source={{ uri: item.user?.avatar || 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png' }} style={styles.avatar} />
          <View style={styles.userTextContainer}>
            <Text style={styles.username}>{item.user?.username}</Text>
            <Text style={styles.name}>{item.user?.name}</Text>
          </View>
        </View>
        <TouchableOpacity style={styles.moreButton} onPress={() => openMenu(item)} hitSlop={{top: 10, bottom: 10, left: 10, right: 10}}>
          <Feather name="more-horizontal" size={20} color="#000" />
        </TouchableOpacity>
      </View>

      {/* Conditionally render image if it's a photo post */}
      {item.imageUrl && (
        <Image source={{ uri: item.imageUrl }} style={styles.postImage} />
      )}

      <View style={styles.actionBar}>
        <View style={styles.actionLeft}>
          <TouchableOpacity style={styles.actionIcon}>
            <Feather name="heart" size={24} color="#000" />
          </TouchableOpacity>
          <TouchableOpacity style={styles.actionIcon}>
            <Feather name="message-circle" size={24} color="#000" />
          </TouchableOpacity>
          <TouchableOpacity style={styles.actionIcon}>
            <Feather name="send" size={24} color="#000" />
          </TouchableOpacity>
        </View>
        <TouchableOpacity style={styles.actionRight}>
          <Feather name="bookmark" size={24} color="#000" />
        </TouchableOpacity>
      </View>

      <View style={styles.contentArea}>
        <Text style={styles.likesText}>{item.likesCount || 0} likes</Text>
        <Text style={styles.captionText}>
          <Text style={styles.captionUsername}>{item.user?.username} </Text>
          {item.caption}
        </Text>
        {item.commentsCount > 0 && (
          <TouchableOpacity>
            <Text style={styles.viewCommentsText}>View all {item.commentsCount} comments</Text>
          </TouchableOpacity>
        )}
      </View>
    </View>
  );

  // Empty State Component for when there are no posts yet
  const renderEmptyState = () => (
    <View style={styles.emptyContainer}>
      <Feather name="camera" size={48} color="#E0E0E0" style={{ marginBottom: 16 }} />
      <Text style={styles.emptyTitle}>No Posts Yet</Text>
      <Text style={styles.emptySubtitle}>When people post, they will appear here.</Text>
    </View>
  );

  if (!fontsLoaded) return null;

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Interraqt</Text>
      </View>

      {isLoading ? (
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color="#000" />
        </View>
      ) : (
        <FlatList
          data={posts}
          keyExtractor={(item) => item.id}
          renderItem={renderPost}
          showsVerticalScrollIndicator={false}
          contentContainerStyle={posts.length === 0 ? { flex: 1 } : { paddingBottom: 120 }}
          ListEmptyComponent={renderEmptyState}
        />
      )}

      {/* BOTTOM SHEET MODAL */}
      <Modal visible={modalVisible} transparent={true} animationType="fade">
        <TouchableWithoutFeedback onPress={() => setModalVisible(false)}>
          <View style={styles.modalOverlay}>
            <TouchableWithoutFeedback>
              <View style={[styles.modalContent, { paddingBottom: insets.bottom || 20 }]}>
                
                <View style={styles.dragHandle} />

                <View style={styles.menuGroup}>
                  <TouchableOpacity style={styles.menuItem}>
                    <Text style={styles.menuText}>Copy link</Text>
                    <Feather name="link" size={20} color="#000" />
                  </TouchableOpacity>
                  <TouchableOpacity style={styles.menuItem}>
                    <Text style={styles.menuText}>Save</Text>
                    <Feather name="bookmark" size={20} color="#000" />
                  </TouchableOpacity>
                  <TouchableOpacity style={styles.menuItem}>
                    <Text style={styles.menuText}>Not interested</Text>
                    <Feather name="eye-off" size={20} color="#000" />
                  </TouchableOpacity>
                </View>

                <View style={styles.menuGroup}>
                  <TouchableOpacity style={styles.menuItem}>
                    <Text style={styles.menuText}>Account Info</Text>
                    <Feather name="info" size={20} color="#000" />
                  </TouchableOpacity>
                  <TouchableOpacity style={styles.menuItem}>
                    <Text style={styles.menuText}>Mute</Text>
                    <Feather name="user-x" size={20} color="#000" />
                  </TouchableOpacity>
                  <TouchableOpacity style={styles.menuItem}>
                    <Text style={styles.menuText}>Restrict</Text>
                    <Feather name="user-minus" size={20} color="#000" />
                  </TouchableOpacity>
                  <TouchableOpacity style={styles.menuItem}>
                    <Text style={styles.menuText}>Block</Text>
                    <Feather name="slash" size={20} color="#000" />
                  </TouchableOpacity>
                  <TouchableOpacity style={styles.menuItem}>
                    <Text style={[styles.menuText, { color: '#FF3B30' }]}>Report</Text>
                    <Feather name="alert-circle" size={20} color="#FF3B30" />
                  </TouchableOpacity>
                </View>

              </View>
            </TouchableWithoutFeedback>
          </View>
        </TouchableWithoutFeedback>
      </Modal>

    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FFFFFF' },
  
  header: { height: 50, justifyContent: 'center', alignItems: 'center', borderBottomWidth: 0.5, borderBottomColor: '#EFEFEF', backgroundColor: '#FFF' },
  headerTitle: { fontFamily: 'TaiHeritagePro_700Bold', fontSize: 28, color: '#000', marginTop: -4 },
  
  loadingContainer: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  
  emptyContainer: { flex: 1, justifyContent: 'center', alignItems: 'center', paddingHorizontal: 40 },
  emptyTitle: { fontSize: 20, fontWeight: '800', color: '#000', marginBottom: 8 },
  emptySubtitle: { fontSize: 15, color: '#666', textAlign: 'center' },
  
  postContainer: { marginBottom: 20 },
  
  postHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 16, paddingVertical: 12 },
  userInfoContainer: { flexDirection: 'row', alignItems: 'center' },
  avatar: { width: 36, height: 36, borderRadius: 18, backgroundColor: '#EFEFEF', marginRight: 10 },
  userTextContainer: { justifyContent: 'center' },
  username: { fontSize: 14, fontWeight: '700', color: '#000' },
  name: { fontSize: 12, color: '#666', marginTop: 1 },
  moreButton: { padding: 4 },
  
  postImage: { width: width, height: width * 1.25, backgroundColor: '#FAFAFA', resizeMode: 'cover' },
  
  actionBar: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 16, paddingVertical: 12 },
  actionLeft: { flexDirection: 'row', alignItems: 'center', gap: 16 },
  actionIcon: { paddingRight: 4 },
  actionRight: { paddingLeft: 4 },
  
  contentArea: { paddingHorizontal: 16 },
  likesText: { fontSize: 14, fontWeight: '700', color: '#000', marginBottom: 6 },
  captionText: { fontSize: 14, color: '#000', lineHeight: 20, marginBottom: 6 },
  captionUsername: { fontWeight: '700' },
  viewCommentsText: { fontSize: 14, color: '#666', marginBottom: 6 },

  modalOverlay: { flex: 1, backgroundColor: 'rgba(0, 0, 0, 0.4)', justifyContent: 'flex-end' },
  modalContent: { backgroundColor: '#FFFFFF', borderTopLeftRadius: 24, borderTopRightRadius: 24, paddingTop: 12, paddingHorizontal: 16, paddingBottom: 40 },
  dragHandle: { width: 40, height: 4, backgroundColor: '#E0E0E0', borderRadius: 2, alignSelf: 'center', marginBottom: 20 },
  
  menuGroup: { backgroundColor: '#F9F9F9', borderRadius: 16, marginBottom: 16, overflow: 'hidden' },
  menuItem: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: 16, paddingHorizontal: 20, borderBottomWidth: 0.5, borderBottomColor: '#EAEAEA' },
  menuText: { fontSize: 16, fontWeight: '600', color: '#000' }
});
