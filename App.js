import React from 'react';
import { View, Text } from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { createMaterialTopTabNavigator } from '@react-navigation/material-top-tabs'; 
import { useSafeAreaInsets, SafeAreaProvider } from 'react-native-safe-area-context';
import { StatusBar } from 'expo-status-bar';
import { Feather } from '@expo/vector-icons';

import LoginScreen from './src/screens/LoginScreen';
import HomeScreen from './src/screens/HomeScreen';
import ProfileScreen from './src/screens/ProfileScreen';
import SettingsScreen from './src/screens/SettingsScreen';
import EditProfileScreen from './src/screens/EditProfileScreen'; 

const ExploreScreen = () => <View style={{flex: 1, backgroundColor: '#F8FAFC', justifyContent: 'center', alignItems: 'center'}}><Text style={{fontSize: 20, fontWeight: 'bold'}}>Explore</Text></View>;
const VideoScreen = () => <View style={{flex: 1, backgroundColor: '#F8FAFC', justifyContent: 'center', alignItems: 'center'}}><Text style={{fontSize: 20, fontWeight: 'bold'}}>Video</Text></View>;

const Stack = createNativeStackNavigator();
const Tab = createMaterialTopTabNavigator();

function MainTabs() {
  const insets = useSafeAreaInsets();
  
  return (
    <Tab.Navigator
      tabBarPosition="bottom" 
      screenOptions={({ route }) => ({
        tabBarShowLabel: true,
        tabBarLabelStyle: { 
          fontSize: 11, 
          fontWeight: '700', 
          textTransform: 'capitalize',
          marginTop: 2
        },
        tabBarActiveTintColor: '#007AFF', // Telegram Blue text/icon
        tabBarInactiveTintColor: '#8E8E93', // Gray text/icon
        tabBarIndicatorStyle: { 
          backgroundColor: '#E5F0FF', // The light blue background bubble
          height: '80%', 
          top: '10%', 
          borderRadius: 100, // Makes it a perfect pill
        },
        tabBarStyle: { 
          position: 'absolute',
          bottom: insets.bottom > 0 ? insets.bottom : 16,
          left: 16,
          right: 16,
          backgroundColor: '#FFFFFF', 
          borderRadius: 40, // Floating pill shape
          height: 65,
          shadowColor: '#000',
          shadowOffset: { width: 0, height: 8 },
          shadowOpacity: 0.1,
          shadowRadius: 16,
          elevation: 10,
        },
        tabBarIcon: ({ color, focused }) => {
          let iconName;
          if (route.name === 'HomeTab') iconName = 'message-circle';
          else if (route.name === 'ExploreTab') iconName = 'users';
          else if (route.name === 'VideoTab') iconName = 'settings';
          else if (route.name === 'ProfileTab') iconName = 'user';
          
          return <Feather name={iconName} size={22} color={color} style={{ transform: [{ scale: focused ? 1.1 : 1 }] }} />;
        },
      })}
    >
      <Tab.Screen name="HomeTab" component={HomeScreen} options={{ title: 'Chats' }} />
      <Tab.Screen name="ExploreTab" component={ExploreScreen} options={{ title: 'Contacts' }} />
      <Tab.Screen name="VideoTab" component={VideoScreen} options={{ title: 'Settings' }} />
      <Tab.Screen name="ProfileTab" component={ProfileScreen} options={{ title: 'Profile' }} />
    </Tab.Navigator>
  );
}

export default function App() {
  return (
    <SafeAreaProvider>
      <NavigationContainer>
        <Stack.Navigator screenOptions={{ headerShown: false }}>
          <Stack.Screen name="Login" component={LoginScreen} />
          <Stack.Screen name="Home" component={MainTabs} options={{ animation: 'fade' }} /> 
          <Stack.Screen name="Settings" component={SettingsScreen} options={{ animation: 'slide_from_right' }} />
          <Stack.Screen name="EditProfile" component={EditProfileScreen} options={{ animation: 'slide_from_bottom' }} />
        </Stack.Navigator>
        <StatusBar style="auto" />
      </NavigationContainer>
    </SafeAreaProvider>
  );
}
