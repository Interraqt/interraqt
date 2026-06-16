import React, { useState, useEffect, useRef } from 'react';
import { StyleSheet, Text, View, FlatList, Dimensions, TouchableOpacity, Image, ActivityIndicator, Share, Animated } from 'react-native';
import { Video, ResizeMode } from 'expo-av';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useIsFocused } from '@react-navigation/native'; // <-- PULLS FOCUS STATE
import { Feather } from '@expo/vector-icons';
import { db } from '../config/firebase';
import { collection, query, orderBy, onSnapshot } from 'firebase/firestore';

import LikeButton from '../components/LikeButton';
import SaveButton from '../components/SaveButton';
import CommentModal from '../components/CommentModal';

const { height, width } = Dimensions.get('window');

// INDIVIDUAL VIDEO COMPONENT (Manages its own pause/play state)
const ReelItem = ({ item, index, activeVideoIndex, isFocused, isCommentOpen, onOpenComments }) => {
  const insets = useSafeAreaInsets();
  const [isManualPause, setIsManualPause] = useState(false);
  
  // Exact math to fix the "2 posts on one screen" bug
  const TAB_BAR_HEIGHT = (insets.bottom > 0 ? insets.bottom : 20) + 70; 
  const VIDEO_HEIGHT = height - TAB_BAR_HEIGHT;

  // Determines if video should actually play
  const isPlaying = isFocused && index === activeVideoIndex && !isManualPause;

  const handleShare = async () => {
    try { await Share.share({ message: `Watch this reel on Interraqt!`, url: item.imageUrl }); } 
    catch (error) { console.log(error); }
  };

  return (
    <View style={[styles.videoContainer, { height: VIDEO_HEIGHT }]}>
      
      {/* WRAPPER FOR MINI-PLAYER EFFECT & TAP TO PAUSE */}
      <TouchableOpacity 
        activeOpacity={1} 
        onPress={() => setIsManualPause(!isManualPause)} 
        style={[styles.videoWrapper, isCommentOpen && styles.videoMini]}
      >
        <Video
          source={{ uri: item.imageUrl }}
          style={StyleSheet.absoluteFillObject}
          resizeMode={ResizeMode.COVER}
          isLooping
          shouldPlay={isPlaying}
        />
        
        {/* BIG PLAY ICON WHEN PAUSED */}
        {isManualPause && (
          <View style={styles.pauseOverlay}>
            <Feather name="play" size={60} color="rgba(255,255,255,0.8)" />
          </View>
        )}
      </TouchableOpacity>

      {/* OVERLAYS (Hide them if comments are open so it looks clean) */}
      {!isCommentOpen && (
        <>
          <View style={[styles.bottomInfo, { paddingBottom: 20 }]}>
            <View style={styles.userInfo}>
              <Image source={{ uri: item.user?.avatar || 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png' }} style={styles.avatar} />
              <Text style={styles.username}>{item.user?.username}</Text>
            </View>
            <Text style={styles.caption} numberOfLines={2}>{item.caption}</Text>
          </View>

          <View style={[styles.rightActions, { paddingBottom: 20 }]}>
            <LikeButton post={item} isLight={true} />
            <TouchableOpacity style={styles.actionIconVertical} onPress={onOpenComments}>
              <Feather name="message-circle" size={32} color="#FFF" />
              {item.commentsCount > 0 && <Text style={styles.actionText}>{item.commentsCount}</Text>}
            </TouchableOpacity>
            <TouchableOpacity style={styles.actionIconVertical} onPress={handleShare}>
              <Feather name="send" size={32} color="#FFF" />
            </TouchableOpacity>
            <SaveButton post={item} isLight={true} />
          </View>
        </>
      )}
    </View>
  );
};

export default function VideoScreen() {
  const insets = useSafeAreaInsets();
  const isFocused = useIsFocused(); // Stops video when navigating away!
  
  const [videos, setVideos] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [activeVideoIndex, setActiveVideoIndex] = useState(0);

  const [commentModalVisible, setCommentModalVisible] = useState(false);
  const [activeCommentPostId, setActiveCommentPostId] = useState(null);

  const TAB_BAR_HEIGHT = (insets.bottom > 0 ? insets.bottom : 20) + 70; 
  const VIDEO_HEIGHT = height - TAB_BAR_HEIGHT;

  useEffect(() => {
    const q = query(collection(db, 'posts'), orderBy('createdAt', 'desc'));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const fetchedPosts = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
      const videoPosts = fetchedPosts.filter(post => post.mediaType === 'video');
      setVideos(videoPosts);
      setIsLoading(false);
    });
    return () => unsubscribe();
  }, []);

  const viewabilityConfig = useRef({ itemVisiblePercentThreshold: 60 }).current;
  const onViewableItemsChanged = useRef(({ viewableItems }) => {
    if (viewableItems.length > 0) setActiveVideoIndex(viewableItems[0].index);
  }).current;

  if (isLoading) return <View style={styles.loadingContainer}><ActivityIndicator size="large" color="#FFF" /></View>;

  return (
    <View style={styles.container}>
      <FlatList
        data={videos}
        keyExtractor={(item) => item.id}
        pagingEnabled
        showsVerticalScrollIndicator={false}
        snapToInterval={VIDEO_HEIGHT} // CRITICAL: Snaps perfectly to 1 post
        snapToAlignment="start"
        decelerationRate="fast"
        onViewableItemsChanged={onViewableItemsChanged}
        viewabilityConfig={viewabilityConfig}
        renderItem={({ item, index }) => (
          <ReelItem 
            item={item} 
            index={index} 
            activeVideoIndex={activeVideoIndex} 
            isFocused={isFocused} 
            isCommentOpen={commentModalVisible && activeCommentPostId === item.id}
            onOpenComments={() => {
              setActiveCommentPostId(item.id);
              setCommentModalVisible(true);
            }}
          />
        )}
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <Feather name="video-off" size={48} color="#666" />
            <Text style={styles.emptyText}>No videos yet.</Text>
          </View>
        }
      />

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
  emptyText: { color: '#666', fontSize: 16, marginTop: 12, fontWeight: '600' },
  
  videoContainer: { width: width, backgroundColor: '#000', position: 'relative' },
  
  // Normal state vs Mini state for comments
  videoWrapper: { flex: 1, backgroundColor: '#000' },
  videoMini: { transform: [{ scale: 0.8 }, { translateY: -120 }], borderRadius: 20, overflow: 'hidden' }, // Instagram Mini Player Effect
  
  pauseOverlay: { ...StyleSheet.absoluteFillObject, justifyContent: 'center', alignItems: 'center', backgroundColor: 'rgba(0,0,0,0.1)' },
  
  bottomInfo: { position: 'absolute', bottom: 0, left: 16, width: '75%', zIndex: 10 },
  userInfo: { flexDirection: 'row', alignItems: 'center', marginBottom: 12 },
  avatar: { width: 40, height: 40, borderRadius: 20, borderWidth: 1, borderColor: '#FFF', marginRight: 10 },
  username: { color: '#FFF', fontSize: 16, fontWeight: '800', textShadowColor: 'rgba(0,0,0,0.6)', textShadowOffset: { width: 0, height: 1 }, textShadowRadius: 4 },
  caption: { color: '#FFF', fontSize: 14, fontWeight: '500', lineHeight: 20, textShadowColor: 'rgba(0,0,0,0.6)', textShadowOffset: { width: 0, height: 1 }, textShadowRadius: 4 },

  rightActions: { position: 'absolute', bottom: 0, right: 16, alignItems: 'center', zIndex: 10 },
  actionIconVertical: { alignItems: 'center', marginBottom: 20 },
  actionText: { color: '#FFF', fontSize: 14, fontWeight: '700', marginTop: 4, textShadowColor: 'rgba(0,0,0,0.6)', textShadowOffset: { width: 0, height: 1 }, textShadowRadius: 4 }
});
