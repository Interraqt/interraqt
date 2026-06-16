import React, { useState } from 'react';
import { TouchableOpacity, StyleSheet } from 'react-native';
import { Feather } from '@expo/vector-icons';
import { auth, db } from '../config/firebase';
import { doc, updateDoc, arrayUnion, arrayRemove } from 'firebase/firestore';

export default function SaveButton({ post, isLight = false }) {
  const currentUserId = auth.currentUser?.uid;
  const [saved, setSaved] = useState(false);

  const toggleSave = async () => {
    if (!currentUserId) return;
    setSaved(!saved);

    const userRef = doc(db, 'users', currentUserId);
    try {
      await updateDoc(userRef, {
        savedPosts: saved ? arrayRemove(post.id) : arrayUnion(post.id)
      });
    } catch (error) {
      console.error("Error saving post", error);
      setSaved(saved); 
    }
  };

  const iconColor = isLight ? "#FFFFFF" : "#000000";

  return (
    <TouchableOpacity onPress={toggleSave} style={[styles.actionRight, isLight && styles.verticalIcon]}>
      <Feather name="bookmark" size={isLight ? 32 : 24} color={iconColor} style={saved && styles.filledBookmark} />
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  actionRight: { paddingLeft: 4 },
  verticalIcon: { paddingLeft: 0, marginBottom: 20 },
  filledBookmark: { fill: '#000' }
});
