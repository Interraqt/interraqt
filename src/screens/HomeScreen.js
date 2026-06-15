import React from 'react';
import { StyleSheet, Text, View, SafeAreaView } from 'react-native';

export default function HomeScreen() {
  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.content}>
        <Text style={styles.logoText}>Interraqt</Text>
        <Text style={styles.subtitle}>Coming Soon 🚀</Text>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#ffffff' },
  content: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  logoText: { fontSize: 42, fontWeight: '900', color: '#000000', letterSpacing: -1 },
  subtitle: { fontSize: 18, color: '#666666', marginTop: 12 },
});
