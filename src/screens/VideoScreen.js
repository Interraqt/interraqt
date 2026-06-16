import React, { useState, useEffect, useRef } from 'react';
import { StyleSheet, Text, View, FlatList, Dimensions, TouchableOpacity, Image, ActivityIndicator, Share } from 'react-native';
import { Video, ResizeMode } from 'expo-av';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Feather } from '@expo/vector-icons';
import { db } from '../config/firebase';
import { collection, query, orderBy, onSnapshot } from 'firebase/firestore';

import LikeButton from '../components/LikeButton';
import SaveButton from '../components/SaveButton';
import CommentModal from '../components/CommentModal';

const { height, width } = Dimensions.get('window');

export default function VideoScreen() {
  const insets = useSafeAreaInsets();
  const [videos, setVideos] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [activeVideoIndex, setActiveVideoIndex] = useState(0);

  // Comment Modal State
  const [commentModalVisible, setCommentModalVisible] = useState(false);
  const [activeCommentPostId, setActiveCommentPostId] = useState(null);

  // Fetch only posts where mediaType === 'video'
  useEffect(() => {
    const q = query(collection(db, 'posts'), orderBy('createdAt', 'desc'));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const fetchedPosts = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
      // Filter videos in memory to avoid Firebase index requirements for now
      const videoPosts = fetchedPosts.filter(post => post.mediaType === 'video');
      setVideos(videoPosts);
      setIsLoading(false);
    });
    return () => unsubscribe();
  }, []);

  // Performance optimized viewability tracking (plays only the video on screen)
  const viewabilityConfig = useRef({ itemVisiblePercentThreshold: 50 }).current;
  const onViewableItemsChanged = useRef(({ viewableItems }) => {
    if (viewableItems.length > 0) {
      setActiveVideoIndex(viewableItems[0].index);
    }
  }).current;

  const handleShare = async (post) => {
    try {
      await Share.share({
        message: `Watch this reel by ${post.user?.username} on Interraqt!`,
        url: post.imageUrl 
      });
    } catch (error) {
      console.log(error.message);
    }
  };

  const renderVideo = ({ item, index }) => (
    <View style={styles.videoContainer}>
      
      {/* THE VIDEO PLAYER */}
      <Video
        source={{ uri: item.imageUrl }}
        style={styles.video}
        resizeMode={ResizeMode.COVER}
        isLooping
        shouldPlay={index === activeVideoIndex && !commentModalVisible} // Pauses if you open comments!
      />

      {/* OVERLAY: BOTTOM LEFT (User Info & Caption) */}
      <View style={[styles.bottomInfo, { paddingBottom: insets.bottom + 90 }]}>
        <View style={styles.userInfo}>
          <Image source={{ uri: item.user?.avatar || 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png' }} style={styles.avatar} />
          <Text style={styles.username}>{item.user?.username}</Text>
        </View>
        <Text style={styles.caption} numberOfLines={2}>{item.caption}</Text>
      </View>

      {/* OVERLAY: BOTTOM RIGHT (Actions Stack) */}
      <View style={[styles.rightActions, { paddingBottom: insets.bottom + 90 }]}>
        
        <LikeButton post={item} isLight={true} />
        
        <TouchableOpacity style={styles.actionIconVertical} onPress={() => { setActiveCommentPostId(item.id); setCommentModalVisible(true); }}>
          <Feather name="message-circle" size={32} color="#FFF" />
          {item.commentsCount > 0 && <Text style={styles.actionText}>{item.commentsCount}</Text>}
        </TouchableOpacity>

        <TouchableOpacity style={styles.actionIconVertical} onPress={() => handleShare(item)}>
          <Feather name="send" size={32} color="#FFF" />
        </TouchableOpacity>

        <SaveButton post={item} isLight={true} />
      </View>

    </View>
  );

  if (isLoading) {
    return <View style={styles.loadingContainer}><ActivityIndicator size="large" color="#000" /></View>;
  }

  return (
    <View style={styles.container}>
      
      {/* FULL SCREEN PAGING LIST */}
      <FlatList
        data={videos}
        keyExtractor={(item) => item.id}
        renderItem={renderVideo}
        pagingEnabled // SNAPS to exact height
        showsVerticalScrollIndicator={false}
        snapToInterval={height}
        snapToAlignment="start"
        decelerationRate="fast"
        onViewableItemsChanged={onViewableItemsChanged}
        viewabilityConfig={viewabilityConfig}
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <Feather name="video-off" size={48} color="#999" />
            <Text style={styles.emptyText}>No videos yet.</Text>
          </View>
        }
      />

      {/* REUSABLE COMMENTS MODAL */}
      <CommentModal 
        isVisible={commentModalVisible} 
        onClose={() => setCommentModalVisible(false)} 
        postId={activeCommentPostId} 
      />

    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#000' },
  loadingContainer: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#000' },
  emptyContainer: { flex: 1, height: height, justifyContent: 'center', alignItems: 'center' },
  emptyText: { color: '#999', fontSize: 16, marginTop: 12, fontWeight: '600' },
  
  videoContainer: { width: width, height: height, backgroundColor: '#000', position: 'relative' },
  video: { ...StyleSheet.absoluteFillObject },
  
  bottomInfo: { position: 'absolute', bottom: 0, left: 16, width: '75%', zIndex: 10 },
  userInfo: { flexDirection: 'row', alignItems: 'center', marginBottom: 12 },
  avatar: { width: 40, height: 40, borderRadius: 20, borderWidth: 1, borderColor: '#FFF', marginRight: 10 },
  username: { color: '#FFF', fontSize: 16, fontWeight: '800', textShadowColor: 'rgba(0,0,0,0.5)', textShadowOffset: { width: 0, height: 1 }, textShadowRadius: 3 },
  caption: { color: '#FFF', fontSize: 14, fontWeight: '500', lineHeight: 20, textShadowColor: 'rgba(0,0,0,0.5)', textShadowOffset: { width: 0, height: 1 }, textShadowRadius: 3 },

  rightActions: { position: 'absolute', bottom: 0, right: 16, alignItems: 'center', zIndex: 10 },
  actionIconVertical: { alignItems: 'center', marginBottom: 20 },
  actionText: { color: '#FFF', fontSize: 14, fontWeight: '700', marginTop: 4, textShadowColor: 'rgba(0,0,0,0.5)', textShadowOffset: { width: 0, height: 1 }, textShadowRadius: 3 }
});
