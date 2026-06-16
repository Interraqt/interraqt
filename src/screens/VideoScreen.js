import React, { useState, useEffect, useRef } from 'react';
import { StyleSheet, Text, View, FlatList, Dimensions, TouchableOpacity, Image, ActivityIndicator, Share } from 'react-native';
import { Video, ResizeMode } from 'expo-av';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useIsFocused } from '@react-navigation/native';
import { Feather } from '@expo/vector-icons';
import Modal from 'react-native-modal';
import { db } from '../config/firebase';
import { collection, query, orderBy, onSnapshot } from 'firebase/firestore';

import LikeButton from '../components/LikeButton';
import SaveButton from '../components/SaveButton';
import CommentModal from '../components/CommentModal';

const { height, width } = Dimensions.get('window'); // EXACT full screen height for perfect snapping

const ReelItem = ({ item, index, activeVideoIndex, isFocused, isCommentOpen, onOpenComments, onOpenOptions }) => {
  const insets = useSafeAreaInsets();
  const [isManualPause, setIsManualPause] = useState(false);
  
  const isPlaying = isFocused && index === activeVideoIndex && !isManualPause;

  const handleShare = async () => {
    try { await Share.share({ message: `Watch this reel on Interraqt!`, url: item.imageUrl }); } 
    catch (error) { console.log(error); }
  };

  return (
    <View style={[styles.videoContainer, { height: height }]}>
      
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
        {isManualPause && (
          <View style={styles.pauseOverlay}>
            <Feather name="play" size={60} color="rgba(255,255,255,0.8)" />
          </View>
        )}
      </TouchableOpacity>

      {!isCommentOpen && (
        <>
          {/* Pushed up by 110 to avoid the custom floating tab bar */}
          <View style={[styles.bottomInfo, { paddingBottom: insets.bottom + 110 }]}>
            <View style={styles.userInfo}>
              <Image source={{ uri: item.user?.avatar || 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png' }} style={styles.avatar} />
              <Text style={styles.username}>{item.user?.username}</Text>
            </View>
            <Text style={styles.caption} numberOfLines={2}>{item.caption}</Text>
          </View>

          {/* Vertical Actions: Like, Comment, Share, Save, Options */}
          <View style={[styles.rightActions, { paddingBottom: insets.bottom + 110 }]}>
            <LikeButton post={item} isLight={true} />
            
            <TouchableOpacity style={styles.actionIconVertical} onPress={onOpenComments}>
              <Feather name="message-circle" size={32} color="#FFF" />
              {item.commentsCount > 0 && <Text style={styles.actionText}>{item.commentsCount}</Text>}
            </TouchableOpacity>
            
            <TouchableOpacity style={styles.actionIconVertical} onPress={handleShare}>
              <Feather name="send" size={30} color="#FFF" />
            </TouchableOpacity>
            
            <SaveButton post={item} isLight={true} />

            <TouchableOpacity style={styles.actionIconVertical} onPress={onOpenOptions}>
              <Feather name="more-vertical" size={28} color="#FFF" />
            </TouchableOpacity>
          </View>
        </>
      )}
    </View>
  );
};

export default function VideoScreen() {
  const isFocused = useIsFocused(); 
  const insets = useSafeAreaInsets();
  
  const [videos, setVideos] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [activeVideoIndex, setActiveVideoIndex] = useState(0);

  // Modals State
  const [commentModalVisible, setCommentModalVisible] = useState(false);
  const [activeCommentPostId, setActiveCommentPostId] = useState(null);
  
  const [optionsModalVisible, setOptionsModalVisible] = useState(false);

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
        pagingEnabled // Snaps one at a time
        showsVerticalScrollIndicator={false}
        snapToInterval={height} // Exact window height
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
            onOpenOptions={() => setOptionsModalVisible(true)}
          />
        )}
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <Feather name="video-off" size={48} color="#666" />
            <Text style={styles.emptyText}>No videos yet.</Text>
          </View>
        }
      />

      {/* COMMENTS MODAL (Passes isFromVideo to stay transparent) */}
      <CommentModal 
        isVisible={commentModalVisible} 
        onClose={() => setCommentModalVisible(false)} 
        postId={activeCommentPostId} 
        isFromVideo={true} 
      />

      {/* VIDEO OPTIONS MODAL */}
      <Modal
        isVisible={optionsModalVisible}
        onBackdropPress={() => setOptionsModalVisible(false)} 
        onSwipeComplete={() => setOptionsModalVisible(false)} 
        swipeDirection={['down']}
        backdropOpacity={0.5} 
        style={styles.optionsModalStyle}
      >
        <View style={[styles.optionsModalContent, { paddingBottom: insets.bottom || 20 }]}>
          <View style={styles.dragHandleOptions} />
          
          <View style={styles.menuGroup}>
            <TouchableOpacity style={styles.menuItem}>
              <Feather name="link" size={20} color="#000" style={styles.menuIcon} />
              <Text style={styles.menuText}>Copy link</Text>
            </TouchableOpacity>
            
            <TouchableOpacity style={styles.menuItem}>
              <Feather name="send" size={20} color="#000" style={styles.menuIcon} />
              <Text style={styles.menuText}>Share</Text>
            </TouchableOpacity>

            <TouchableOpacity style={styles.menuItem}>
              <Feather name="eye" size={20} color="#000" style={styles.menuIcon} />
              <Text style={styles.menuText}>Interested</Text>
            </TouchableOpacity>

            <TouchableOpacity style={styles.menuItem}>
              <Feather name="eye-off" size={20} color="#000" style={styles.menuIcon} />
              <Text style={styles.menuText}>Not interested</Text>
            </TouchableOpacity>

            <TouchableOpacity style={styles.menuItem}>
              <Feather name="info" size={20} color="#000" style={styles.menuIcon} />
              <Text style={styles.menuText}>Account info</Text>
            </TouchableOpacity>

            <TouchableOpacity style={[styles.menuItem, { borderBottomWidth: 0 }]}>
              <Feather name="alert-triangle" size={20} color="#FF3B30" style={styles.menuIcon} />
              <Text style={[styles.menuText, { color: '#FF3B30' }]}>Report</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>

    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#000' },
  loadingContainer: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#000' },
  emptyContainer: { flex: 1, height: height, justifyContent: 'center', alignItems: 'center' },
  emptyText: { color: '#666', fontSize: 16, marginTop: 12, fontWeight: '600' },
  
  videoContainer: { width: width, backgroundColor: '#000', position: 'relative' },
  videoWrapper: { flex: 1, backgroundColor: '#000' },
  videoMini: { transform: [{ scale: 0.8 }, { translateY: -120 }], borderRadius: 20, overflow: 'hidden' }, 
  pauseOverlay: { ...StyleSheet.absoluteFillObject, justifyContent: 'center', alignItems: 'center', backgroundColor: 'rgba(0,0,0,0.2)' },
  
  bottomInfo: { position: 'absolute', bottom: 0, left: 16, width: '75%', zIndex: 10 },
  userInfo: { flexDirection: 'row', alignItems: 'center', marginBottom: 12 },
  avatar: { width: 40, height: 40, borderRadius: 20, borderWidth: 1, borderColor: '#FFF', marginRight: 10 },
  username: { color: '#FFF', fontSize: 16, fontWeight: '800', textShadowColor: 'rgba(0,0,0,0.8)', textShadowOffset: { width: 0, height: 1 }, textShadowRadius: 4 },
  caption: { color: '#FFF', fontSize: 14, fontWeight: '500', lineHeight: 20, textShadowColor: 'rgba(0,0,0,0.8)', textShadowOffset: { width: 0, height: 1 }, textShadowRadius: 4 },

  rightActions: { position: 'absolute', bottom: 0, right: 16, alignItems: 'center', zIndex: 10 },
  actionIconVertical: { alignItems: 'center', marginBottom: 20 },
  actionText: { color: '#FFF', fontSize: 14, fontWeight: '700', marginTop: 4, textShadowColor: 'rgba(0,0,0,0.8)', textShadowOffset: { width: 0, height: 1 }, textShadowRadius: 4 },

  // Options Modal Styles
  optionsModalStyle: { justifyContent: 'flex-end', margin: 0 },
  optionsModalContent: { backgroundColor: '#F9F9F9', borderTopLeftRadius: 24, borderTopRightRadius: 24, paddingTop: 12, paddingHorizontal: 16 },
  dragHandleOptions: { width: 40, height: 4, backgroundColor: '#CCC', borderRadius: 2, alignSelf: 'center', marginBottom: 20 },
  menuGroup: { backgroundColor: '#FFF', borderRadius: 16, marginBottom: 16, overflow: 'hidden' },
  menuItem: { flexDirection: 'row', alignItems: 'center', paddingVertical: 16, paddingHorizontal: 20, borderBottomWidth: 0.5, borderBottomColor: '#EAEAEA' },
  menuIcon: { marginRight: 16 },
  menuText: { fontSize: 16, fontWeight: '600', color: '#000' }
});
