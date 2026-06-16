import React, { useState } from 'react';
import { TouchableOpacity, Text, StyleSheet } from 'react-native';
import { Feather } from '@expo/vector-icons';
import { auth, db } from '../config/firebase';
import { doc, updateDoc, arrayUnion, arrayRemove } from 'firebase/firestore';

export default function LikeButton({ post, isLight = false }) {
  const currentUserId = auth.currentUser?.uid;
  const initialLikes = post.likes || [];
  
  const [liked, setLiked] = useState(initialLikes.includes(currentUserId));
  const [likeCount, setLikeCount] = useState(initialLikes.length);

  const toggleLike = async () => {
    if (!currentUserId) return;

    setLiked(!liked);
    setLikeCount(liked ? likeCount - 1 : likeCount + 1);

    const postRef = doc(db, 'posts', post.id);
    try {
      await updateDoc(postRef, {
        likes: liked ? arrayRemove(currentUserId) : arrayUnion(currentUserId)
      });
    } catch (error) {
      console.error("Error updating like", error);
      setLiked(liked);
      setLikeCount(liked ? likeCount + 1 : likeCount - 1);
    }
  };

  const iconColor = liked ? "#FF3B30" : (isLight ? "#FFFFFF" : "#000000");
  const textColor = isLight ? "#FFFFFF" : "#000000";

  return (
    <TouchableOpacity onPress={toggleLike} style={[styles.actionIcon, isLight && styles.verticalIcon]}>
      <Feather name="heart" size={isLight ? 32 : 24} color={iconColor} style={liked && styles.filledHeart} />
      {likeCount > 0 && <Text style={[styles.countText, { color: textColor, marginTop: isLight ? 4 : 0 }]}>{likeCount}</Text>}
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  actionIcon: { flexDirection: 'row', alignItems: 'center', marginRight: 16 },
  verticalIcon: { flexDirection: 'column', marginRight: 0, marginBottom: 20 },
  countText: { marginLeft: 6, fontSize: 14, fontWeight: '600' },
  filledHeart: { color: '#FF3B30', fill: '#FF3B30' } 
});
