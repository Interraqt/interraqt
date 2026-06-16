import React, { useState, useEffect } from 'react';
import { TouchableOpacity, StyleSheet, Alert } from 'react-native';
import { Feather } from '@expo/vector-icons';
import { auth, db } from '../config/firebase';
import { doc, setDoc, getDoc, arrayUnion, arrayRemove } from 'firebase/firestore';

export default function SaveButton({ post, isLight = false }) {
  const currentUserId = auth.currentUser?.uid;
  const [saved, setSaved] = useState(false);

  // Check initial save status when component loads
  useEffect(() => {
    const checkSavedStatus = async () => {
      if (!currentUserId) return;
      const userRef = doc(db, 'users', currentUserId);
      const userSnap = await getDoc(userRef);
      if (userSnap.exists()) {
        const userData = userSnap.data();
        if (userData.savedPosts && userData.savedPosts.includes(post.id)) {
          setSaved(true);
        }
      }
    };
    checkSavedStatus();
  }, [currentUserId, post.id]);

  const toggleSave = async () => {
    if (!currentUserId) return;
    
    // Optimistic UI Update
    setSaved(!saved);

    const userRef = doc(db, 'users', currentUserId);
    try {
      // Using setDoc with merge: true prevents errors if the user doc is missing fields
      await setDoc(userRef, {
        savedPosts: saved ? arrayRemove(post.id) : arrayUnion(post.id)
      }, { merge: true });
    } catch (error) {
      console.error("Error saving post", error);
      setSaved(saved); // Revert UI if Firebase fails
      Alert.alert("Error", "Could not save post.");
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
  filledBookmark: { fill: '#000' } // Fake fill using standard color
});
