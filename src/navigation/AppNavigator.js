import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Animated, useWindowDimensions } from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { createMaterialTopTabNavigator } from '@react-navigation/material-top-tabs'; 
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Feather } from '@expo/vector-icons';
import { BlurView } from 'expo-blur';

// Import Screens
import LoginScreen from '../screens/LoginScreen';
import SignupScreen from '../screens/SignupScreen'; 
import HomeScreen from '../screens/HomeScreen';
import ProfileScreen from '../screens/ProfileScreen';
import SettingsScreen from '../screens/SettingsScreen';
import EditProfileScreen from '../screens/EditProfileScreen';
import CreatePostScreen from '../screens/CreatePostScreen'; // <-- Added Create Post Screen

const ExploreScreen = () => <View style={{flex: 1, backgroundColor: '#FAFAFA'}}><Text style={{marginTop: 100, textAlign: 'center', fontSize: 20}}>Explore</Text></View>;
const VideoScreen = () => <View style={{flex: 1, backgroundColor: '#FAFAFA'}}><Text style={{marginTop: 100, textAlign: 'center', fontSize: 20}}>Video</Text></View>;

const Stack = createNativeStackNavigator();
const Tab = createMaterialTopTabNavigator();

// --- CUSTOM LIQUID GLASS TAB BAR ---
function GlassTabBar({ state, descriptors, navigation, position }) {
  const insets = useSafeAreaInsets();
  const { width } = useWindowDimensions();
  
  // Math for the sliding bubble
  const TAB_BAR_WIDTH = width - 32; // 16px margin on left and right
  const TAB_WIDTH = TAB_BAR_WIDTH / 4; // 4 tabs

  // Animates the bubble exactly to the current tab
  const translateX = position.interpolate({
    inputRange: [0, 1, 2, 3],
    outputRange: [0, TAB_WIDTH, TAB_WIDTH * 2, TAB_WIDTH * 3],
  });

  return (
    <View style={[styles.tabBarContainer, { bottom: insets.bottom > 0 ? insets.bottom : 20 }]}>
      <BlurView intensity={80} tint="light" style={styles.glassBackground}>
        
        {/* THE SLIDING HIGHLIGHT BUBBLE */}
        <Animated.View style={[styles.slidingBubble, { transform: [{ translateX }] }]}>
          {/* This inner view creates the padding so it NEVER touches the edges! */}
          <View style={styles.bubbleInner} /> 
        </Animated.View>

        {/* THE ICONS AND TEXT */}
        {state.routes.map((route, index) => {
          const { options } = descriptors[route.key];
          const label = options.title !== undefined ? options.title : route.name;
          const isFocused = state.index === index;

          const onPress = () => {
            const event = navigation.emit({ type: 'tabPress', target: route.key, canPreventDefault: true });
            if (!isFocused && !event.defaultPrevented) {
              navigation.navigate(route.name);
            }
          };

          let iconName = 'home';
          if (route.name === 'ExploreTab') iconName = 'search';
          if (route.name === 'VideoTab') iconName = 'video';
          if (route.name === 'ProfileTab') iconName = 'user';

          return (
            <TouchableOpacity key={index} onPress={onPress} style={styles.tabItem}>
              <Feather name={iconName} size={22} color={isFocused ? '#000' : '#8E8E93'} style={{ marginBottom: 4 }} />
              <Text style={[styles.tabLabel, { color: isFocused ? '#000' : '#8E8E93' }]}>{label}</Text>
            </TouchableOpacity>
          );
        })}

      </BlurView>
    </View>
  );
}

function MainTabs() {
  return (
    <Tab.Navigator
      tabBarPosition="bottom" 
      swipeEnabled={true} 
      tabBar={(props) => <GlassTabBar {...props} />} 
    >
      <Tab.Screen name="HomeTab" component={HomeScreen} options={{ title: 'Home' }} />
      <Tab.Screen name="ExploreTab" component={ExploreScreen} options={{ title: 'Explore' }} />
      <Tab.Screen name="VideoTab" component={VideoScreen} options={{ title: 'Video' }} />
      <Tab.Screen name="ProfileTab" component={ProfileScreen} options={{ title: 'Profile' }} />
    </Tab.Navigator>
  );
}

export default function AppNavigator() {
  return (
    <NavigationContainer>
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        <Stack.Screen name="Login" component={LoginScreen} />
        <Stack.Screen name="Signup" component={SignupScreen} options={{ animation: 'slide_from_right' }} /> 
        <Stack.Screen name="Home" component={MainTabs} options={{ animation: 'fade' }} /> 
        <Stack.Screen name="Settings" component={SettingsScreen} options={{ animation: 'slide_from_right' }} />
        <Stack.Screen name="EditProfile" component={EditProfileScreen} options={{ animation: 'slide_from_bottom' }} />
        
        {/* <-- ADDED: Create Post Screen with slide up animation --> */}
        <Stack.Screen name="CreatePost" component={CreatePostScreen} options={{ animation: 'slide_from_bottom' }} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}

const styles = StyleSheet.create({
  tabBarContainer: {
    position: 'absolute',
    left: 16,
    right: 16,
    height: 70,
    borderRadius: 35,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 10 },
    shadowOpacity: 0.1,
    shadowRadius: 20,
    elevation: 10,
  },
  glassBackground: {
    flex: 1,
    flexDirection: 'row',
    backgroundColor: 'rgba(255, 255, 255, 0.65)', 
    borderRadius: 35,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.8)',
    overflow: 'hidden',
  },
  slidingBubble: {
    position: 'absolute',
    width: '25%', 
    height: '100%',
    paddingHorizontal: 8, 
    paddingVertical: 10,  
  },
  bubbleInner: {
    flex: 1,
    backgroundColor: '#FFFFFF',
    borderRadius: 100, 
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.08,
    shadowRadius: 8,
    elevation: 2,
  },
  tabItem: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    zIndex: 1, 
  },
  tabLabel: {
    fontSize: 11,
    fontWeight: '700',
  }
});
