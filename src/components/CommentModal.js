import React, { useState, useEffect } from 'react';
import { StyleSheet, Text, View, TextInput, TouchableOpacity, FlatList, ActivityIndicator, Image } from 'react-native';
import Modal from 'react-native-modal';
import { Feather } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { auth, db } from '../config/firebase';
import { collection, query, orderBy, onSnapshot, addDoc, doc, updateDoc, increment, getDoc } from 'firebase/firestore';

export default function CommentModal({ isVisible, onClose, postId }) {
  const insets = useSafeAreaInsets();
  
  const [comments, setComments] = useState([]);
  const [newComment, setNewComment] = useState('');
  const [isPosting, setIsPosting] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  // REAL-TIME LISTENER FOR COMMENTS
  useEffect(() => {
    if (!postId || !isVisible) return; // Only listen if the modal is open
    
    setIsLoading(true);
    const commentsRef = collection(db, 'posts', postId, 'comments');
    const q = query(commentsRef, orderBy('createdAt', 'desc'));
    
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const fetchedComments = snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      }));
      setComments(fetchedComments);
      setIsLoading(false);
    });

    return () => unsubscribe();
  }, [postId, isVisible]);

  const handlePostComment = async () => {
    if (!newComment.trim()) return;
    setIsPosting(true);

    try {
      const userDoc = await getDoc(doc(db, 'users', auth.currentUser.uid));
      const userData = userDoc.data();

      await addDoc(collection(db, 'posts', postId, 'comments'), {
        userId: auth.currentUser.uid,
        user: {
          username: userData.username,
          avatar: userData.avatar || 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png'
        },
        text: newComment.trim(),
        createdAt: new Date().toISOString()
      });

      await updateDoc(doc(db, 'posts', postId), {
        commentsCount: increment(1)
      });

      setNewComment(''); 
    } catch (error) {
      console.error("Error posting comment: ", error);
    } finally {
      setIsPosting(false);
    }
  };

  const renderComment = ({ item }) => (
    <View style={styles.commentRow}>
      <Image source={{ uri: item.user?.avatar }} style={styles.avatar} />
      <View style={styles.commentContent}>
        <Text style={styles.commentText}>
          <Text style={styles.username}>{item.user?.username} </Text>
          {item.text}
        </Text>
      </View>
    </View>
  );

  return (
    <Modal
      isVisible={isVisible}
      onBackdropPress={onClose}
      onSwipeComplete={onClose}
      swipeDirection={['down']}
      propagateSwipe={true} // Allows the FlatList to scroll without instantly closing the modal
      avoidKeyboard={true}  // Automatically pushes the sheet up when typing
      backdropOpacity={0.5}
      style={styles.modalStyle}
    >
      <View style={[styles.modalContent, { paddingBottom: insets.bottom || 20 }]}>
        
        {/* DRAG HANDLE & TITLE */}
        <View style={styles.dragHandle} />
        <Text style={styles.headerTitle}>Comments</Text>
        <View style={styles.divider} />

        {/* COMMENTS LIST */}
        {isLoading ? (
          <View style={styles.loadingContainer}><ActivityIndicator size="small" color="#000" /></View>
        ) : (
          <FlatList
            data={comments}
            keyExtractor={(item) => item.id}
            renderItem={renderComment}
            contentContainerStyle={styles.listContent}
            inverted // Places the newest comments at the bottom near the input box
            showsVerticalScrollIndicator={false}
            ListEmptyComponent={
              <Text style={styles.emptyText}>No comments yet. Start the conversation!</Text>
            }
          />
        )}

        {/* INPUT AREA */}
        <View style={styles.inputContainer}>
          <TextInput
            style={styles.textInput}
            placeholder="Add a comment..."
            placeholderTextColor="#999"
            value={newComment}
            onChangeText={setNewComment}
            multiline
            maxLength={500}
          />
          <TouchableOpacity onPress={handlePostComment} disabled={isPosting || !newComment.trim()}>
            {isPosting ? (
              <ActivityIndicator size="small" color="#007AFF" style={{ marginBottom: 10 }} />
            ) : (
              <Text style={[styles.postButtonText, { color: newComment.trim() ? '#007AFF' : '#A0A0A0' }]}>Post</Text>
            )}
          </TouchableOpacity>
        </View>

      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  modalStyle: { justifyContent: 'flex-end', margin: 0 },
  modalContent: { backgroundColor: '#FFFFFF', borderTopLeftRadius: 24, borderTopRightRadius: 24, paddingTop: 12, height: '75%' }, // 75% height for the bottom sheet look
  dragHandle: { width: 40, height: 4, backgroundColor: '#CCC', borderRadius: 2, alignSelf: 'center', marginBottom: 16 },
  headerTitle: { fontSize: 18, fontWeight: '800', color: '#000', textAlign: 'center', marginBottom: 12 },
  divider: { height: 1, backgroundColor: '#EFEFEF', width: '100%', marginBottom: 8 },
  
  loadingContainer: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  
  listContent: { paddingHorizontal: 16, paddingBottom: 20 },
  commentRow: { flexDirection: 'row', marginBottom: 16 },
  avatar: { width: 32, height: 32, borderRadius: 16, backgroundColor: '#F0F0F0', marginRight: 12 },
  commentContent: { flex: 1, justifyContent: 'center' },
  username: { fontWeight: '700', color: '#000' },
  commentText: { fontSize: 14, color: '#000', lineHeight: 20 },
  emptyText: { textAlign: 'center', color: '#999', marginTop: 40 },

  inputContainer: { flexDirection: 'row', alignItems: 'flex-end', paddingHorizontal: 16, paddingTop: 12, borderTopWidth: 0.5, borderTopColor: '#EFEFEF', backgroundColor: '#FFF' },
  textInput: { flex: 1, backgroundColor: '#F9F9F9', borderRadius: 20, paddingHorizontal: 16, paddingTop: 10, paddingBottom: 10, minHeight: 40, maxHeight: 100, fontSize: 14, color: '#000', marginRight: 12 },
  postButtonText: { fontSize: 16, fontWeight: '700', marginBottom: 10 },
});
