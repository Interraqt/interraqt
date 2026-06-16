import React, { useState } from 'react';
import { StyleSheet, Text, View, FlatList, Image, TouchableOpacity, Modal, Dimensions, TouchableWithoutFeedback } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Feather } from '@expo/vector-icons';
import { useFonts, TaiHeritagePro_700Bold } from '@expo-google-fonts/tai-heritage-pro';

const { width } = Dimensions.get('window');

// Premium High-Res Mock Data
const MOCK_POSTS = [
  {
    id: '1',
    user: { name: 'Hardik Kalal', username: 'hardikkalal', avatar: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=400&q=80' },
    image: 'https://images.unsplash.com/photo-1682687220742-aba13b6e50ba?q=80&w=1000',
    caption: 'Exploring the new fluid design systems. The combination of stark white and deep blacks creates such a powerful contrast. 🖤✨',
    likes: '1,294',
    comments: '48',
    time: '2 hours ago'
  },
  {
    id: '2',
    user: { name: 'Tech Insider', username: 'techinsider', avatar: 'https://images.unsplash.com/photo-1531427186611-ecfd6d936c79?w=400&q=80' },
    image: 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?q=80&w=1000',
    caption: 'Abstract forms and digital brutalism. What are your thoughts on the current state of mobile UI?',
    likes: '8,432',
    comments: '192',
    time: '5 hours ago'
  },
  {
    id: '3',
    user: { name: 'Design Daily', username: 'designdaily', avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400&q=80' },
    image: 'https://images.unsplash.com/photo-1550684848-fac1c5b4e853?q=80&w=1000',
    caption: 'Less is always more. Stripping away the noise to focus purely on the content and typography.',
    likes: '4,011',
    comments: '88',
    time: '12 hours ago'
  }
];

export default function HomeScreen() {
  const insets = useSafeAreaInsets();
  const [modalVisible, setModalVisible] = useState(false);
  const [selectedPost, setSelectedPost] = useState(null);

  // Load the premium logo font
  let [fontsLoaded] = useFonts({
    TaiHeritagePro_700Bold,
  });

  const openMenu = (post) => {
    setSelectedPost(post);
    setModalVisible(true);
  };

  const renderPost = ({ item }) => (
    <View style={styles.postContainer}>
      
      {/* POST HEADER */}
      <View style={styles.postHeader}>
        <View style={styles.userInfoContainer}>
          <Image source={{ uri: item.user.avatar }} style={styles.avatar} />
          <View style={styles.userTextContainer}>
            <Text style={styles.username}>{item.user.username}</Text>
            <Text style={styles.name}>{item.user.name}</Text>
          </View>
        </View>
        <TouchableOpacity style={styles.moreButton} onPress={() => openMenu(item)} hitSlop={{top: 10, bottom: 10, left: 10, right: 10}}>
          <Feather name="more-horizontal" size={20} color="#000" />
        </TouchableOpacity>
      </View>

      {/* FULL SCREEN IMAGE (4:5 Aspect Ratio) */}
      <Image source={{ uri: item.image }} style={styles.postImage} />

      {/* ACTION BAR */}
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

      {/* LIKES & CAPTION */}
      <View style={styles.contentArea}>
        <Text style={styles.likesText}>{item.likes} likes</Text>
        <Text style={styles.captionText}>
          <Text style={styles.captionUsername}>{item.user.username} </Text>
          {item.caption}
        </Text>
        <TouchableOpacity>
          <Text style={styles.viewCommentsText}>View all {item.comments} comments</Text>
        </TouchableOpacity>
        <Text style={styles.timeText}>{item.time}</Text>
      </View>

    </View>
  );

  if (!fontsLoaded) return null;

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      
      {/* TOP NAVIGATION HEADER */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Interraqt</Text>
      </View>

      {/* MAIN FEED */}
      <FlatList
        data={MOCK_POSTS}
        keyExtractor={(item) => item.id}
        renderItem={renderPost}
        showsVerticalScrollIndicator={false}
        contentContainerStyle={{ paddingBottom: 120 }} // Space for bottom tab bar
      />

      {/* BOTTOM SHEET MODAL (Threads Style) */}
      <Modal visible={modalVisible} transparent={true} animationType="fade">
        <TouchableWithoutFeedback onPress={() => setModalVisible(false)}>
          <View style={styles.modalOverlay}>
            <TouchableWithoutFeedback>
              <View style={[styles.modalContent, { paddingBottom: insets.bottom || 20 }]}>
                
                {/* Drag Handle */}
                <View style={styles.dragHandle} />

                {/* Menu Options */}
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
  
  // Header
  header: { height: 50, justifyContent: 'center', alignItems: 'center', borderBottomWidth: 0.5, borderBottomColor: '#EFEFEF', backgroundColor: '#FFF' },
  headerTitle: { fontFamily: 'TaiHeritagePro_700Bold', fontSize: 28, color: '#000', marginTop: -4 },
  
  // Post Container
  postContainer: { marginBottom: 20 },
  
  // Post Header
  postHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 16, paddingVertical: 12 },
  userInfoContainer: { flexDirection: 'row', alignItems: 'center' },
  avatar: { width: 36, height: 36, borderRadius: 18, backgroundColor: '#EFEFEF', marginRight: 10 },
  userTextContainer: { justifyContent: 'center' },
  username: { fontSize: 14, fontWeight: '700', color: '#000' },
  name: { fontSize: 12, color: '#666', marginTop: 1 },
  moreButton: { padding: 4 },
  
  // Post Image (4:5 Aspect Ratio for professional feel)
  postImage: { width: width, height: width * 1.25, backgroundColor: '#FAFAFA', resizeMode: 'cover' },
  
  // Action Bar (Likes, Comments, Share)
  actionBar: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 16, paddingVertical: 12 },
  actionLeft: { flexDirection: 'row', alignItems: 'center', gap: 16 },
  actionIcon: { paddingRight: 4 },
  actionRight: { paddingLeft: 4 },
  
  // Content Area (Captions, likes count)
  contentArea: { paddingHorizontal: 16 },
  likesText: { fontSize: 14, fontWeight: '700', color: '#000', marginBottom: 6 },
  captionText: { fontSize: 14, color: '#000', lineHeight: 20, marginBottom: 6 },
  captionUsername: { fontWeight: '700' },
  viewCommentsText: { fontSize: 14, color: '#666', marginBottom: 6 },
  timeText: { fontSize: 12, color: '#999', marginTop: 2 },

  // --- MODAL / BOTTOM SHEET STYLES ---
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0, 0, 0, 0.4)', justifyContent: 'flex-end' },
  modalContent: { backgroundColor: '#FFFFFF', borderTopLeftRadius: 24, borderTopRightRadius: 24, paddingTop: 12, paddingHorizontal: 16, paddingBottom: 40 },
  dragHandle: { width: 40, height: 4, backgroundColor: '#E0E0E0', borderRadius: 2, alignSelf: 'center', marginBottom: 20 },
  
  menuGroup: { backgroundColor: '#F9F9F9', borderRadius: 16, marginBottom: 16, overflow: 'hidden' },
  menuItem: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: 16, paddingHorizontal: 20, borderBottomWidth: 0.5, borderBottomColor: '#EAEAEA' },
  menuText: { fontSize: 16, fontWeight: '600', color: '#000' }
});
