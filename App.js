import React from 'react';
import { View, Text } from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { createMaterialTopTabNavigator } from '@react-navigation/material-top-tabs'; // Enables Swiping!
import { useSafeAreaInsets, SafeAreaProvider } from 'react-native-safe-area-context';
import { StatusBar } from 'expo-status-bar';
import { Feather } from '@expo/vector-icons';

import LoginScreen from './src/screens/LoginScreen';
import HomeScreen from './src/screens/HomeScreen';
import ProfileScreen from './src/screens/ProfileScreen';
import SettingsScreen from './src/screens/SettingsScreen';
import EditProfileScreen from './src/screens/EditProfileScreen'; // New screen we will create

const ExploreScreen = () => <View style={{flex: 1, backgroundColor: '#FFF', justifyContent: 'center', alignItems: 'center'}}><Text style={{fontSize: 20, fontWeight: 'bold'}}>Explore</Text></View>;
const VideoScreen = () => <View style={{flex: 1, backgroundColor: '#FFF', justifyContent: 'center', alignItems: 'center'}}><Text style={{fontSize: 20, fontWeight: 'bold'}}>Video</Text></View>;

const Stack = createNativeStackNavigator();
const Tab = createMaterialTopTabNavigator();

function MainTabs() {
  const insets = useSafeAreaInsets();
  
  return (
    <Tab.Navigator
      tabBarPosition="bottom" // Forces top tabs to the bottom!
      screenOptions={({ route }) => ({
        tabBarShowLabel: false,
        tabBarActiveTintColor: '#000',
        tabBarInactiveTintColor: '#A0A0A0',
        tabBarIndicatorStyle: { 
          backgroundColor: '#000', 
          height: 3, 
          top: 0, // Puts the animated highlight line at the top of the tab bar
          borderRadius: 3
        },
        tabBarStyle: { 
          backgroundColor: '#FFF', 
          borderTopWidth: 1, 
          borderColor: '#F0F0F0',
          paddingBottom: insets.bottom, // Safe area for gestures
          height: 60 + insets.bottom
        },
        tabBarIcon: ({ color, focused }) => {
          let iconName;
          if (route.name === 'HomeTab') iconName = 'home';
          else if (route.name === 'ExploreTab') iconName = 'search';
          else if (route.name === 'VideoTab') iconName = 'play-circle';
          else if (route.name === 'ProfileTab') iconName = 'user';
          // Make the icon slightly larger when focused for animation effect
          return <Feather name={iconName} size={focused ? 26 : 24} color={color} style={{ transform: [{ scale: focused ? 1.1 : 1 }] }} />;
        },
      })}
    >
      <Tab.Screen name="HomeTab" component={HomeScreen} />
      <Tab.Screen name="ExploreTab" component={ExploreScreen} />
      <Tab.Screen name="VideoTab" component={VideoScreen} />
      <Tab.Screen name="ProfileTab" component={ProfileScreen} />
    </Tab.Navigator>
  );
}

export default function App() {
  return (
    <SafeAreaProvider>
      <NavigationContainer>
        <Stack.Navigator 
          screenOptions={{ headerShown: false }}
        >
          <Stack.Screen name="Login" component={LoginScreen} />
          {/* FADE animation stops the aggressive blinking when logging in */}
          <Stack.Screen name="Home" component={MainTabs} options={{ animation: 'fade' }} /> 
          <Stack.Screen name="Settings" component={SettingsScreen} options={{ animation: 'slide_from_right' }} />
          <Stack.Screen name="EditProfile" component={EditProfileScreen} options={{ animation: 'slide_from_bottom' }} />
        </Stack.Navigator>
        <StatusBar style="dark" />
      </NavigationContainer>
    </SafeAreaProvider>
  );
}
