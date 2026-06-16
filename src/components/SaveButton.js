import React, { useState } from 'react';
import { TouchableOpacity, StyleSheet } from 'react-native';
import { Feather } from '@expo/vector-icons';
import { auth, db } from '../config/firebase';
import { doc, updateDoc, arrayUnion, arrayRemove } from 'firebase/firestore';

export default function SaveButton({ post }) {
  const currentUserId = auth.currentUser?.uid;
  // We track saved posts inside the User's document
  const [saved, setSaved] = useState(false); // For now, starts false. We can load real state later.

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
      setSaved(saved); // Revert
    }
  };

  return (
    <TouchableOpacity onPress={toggleSave} style={styles.actionRight}>
      <Feather name="bookmark" size={24} color={saved ? "#000" : "#000"} style={saved && styles.filledBookmark} />
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  actionRight: { paddingLeft: 4 },
  filledBookmark: { fill: '#000' }
});
