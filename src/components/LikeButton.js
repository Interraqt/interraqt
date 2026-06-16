import React, { useState, useEffect } from 'react';
import { TouchableOpacity, Text, StyleSheet } from 'react-native';
import { Feather } from '@expo/vector-icons';
import { auth, db } from '../config/firebase';
import { doc, updateDoc, arrayUnion, arrayRemove, getDoc } from 'firebase/firestore';

export default function LikeButton({ post }) {
  // Check if current user's ID is in the likes array (default to empty array if undefined)
  const currentUserId = auth.currentUser?.uid;
  const initialLikes = post.likes || [];
  
  const [liked, setLiked] = useState(initialLikes.includes(currentUserId));
  const [likeCount, setLikeCount] = useState(initialLikes.length);

  const toggleLike = async () => {
    if (!currentUserId) return;

    // 1. Instant UI Update (Optimistic rendering)
    setLiked(!liked);
    setLikeCount(liked ? likeCount - 1 : likeCount + 1);

    // 2. Background Firebase Update
    const postRef = doc(db, 'posts', post.id);
    try {
      await updateDoc(postRef, {
        likes: liked ? arrayRemove(currentUserId) : arrayUnion(currentUserId)
      });
    } catch (error) {
      console.error("Error updating like", error);
      // Revert if it fails
      setLiked(liked);
      setLikeCount(liked ? likeCount + 1 : likeCount - 1);
    }
  };

  return (
    <TouchableOpacity onPress={toggleLike} style={styles.actionIcon}>
      <Feather name="heart" size={24} color={liked ? "#FF3B30" : "#000"} style={liked && styles.filledHeart} />
      <Text style={styles.countText}>{likeCount > 0 ? likeCount : ''}</Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  actionIcon: { flexDirection: 'row', alignItems: 'center', marginRight: 16 },
  countText: { marginLeft: 6, fontSize: 14, fontWeight: '600', color: '#000' },
  filledHeart: { color: '#FF3B30', fill: '#FF3B30' } // Fake fill by matching color
});
