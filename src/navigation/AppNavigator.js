import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { createMaterialTopTabNavigator } from '@react-navigation/material-top-tabs'; 
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Feather } from '@expo/vector-icons';
import { View, Text } from 'react-native';

// Import Screens
import LoginScreen from '../screens/LoginScreen';
import HomeScreen from '../screens/HomeScreen';
import ProfileScreen from '../screens/ProfileScreen';
import SettingsScreen from '../screens/SettingsScreen';
import EditProfileScreen from '../screens/EditProfileScreen';

const ExploreScreen = () => <View style={{flex: 1, backgroundColor: '#FAFAFA', justifyContent: 'center', alignItems: 'center'}}><Text style={{fontSize: 20, fontWeight: 'bold'}}>Explore</Text></View>;
const VideoScreen = () => <View style={{flex: 1, backgroundColor: '#FAFAFA', justifyContent: 'center', alignItems: 'center'}}><Text style={{fontSize: 20, fontWeight: 'bold'}}>Video</Text></View>;

const Stack = createNativeStackNavigator();
const Tab = createMaterialTopTabNavigator();

function MainTabs() {
  const insets = useSafeAreaInsets();
  
  return (
    <Tab.Navigator
      tabBarPosition="bottom" 
      screenOptions={({ route }) => ({
        tabBarShowLabel: true,
        tabBarLabelStyle: { fontSize: 11, fontWeight: '700', textTransform: 'capitalize', marginTop: 2 },
        tabBarActiveTintColor: '#000', 
        tabBarInactiveTintColor: '#8E8E93', 
        tabBarIndicatorStyle: { backgroundColor: '#EAEAEA', height: '80%', top: '10%', borderRadius: 100 },
        tabBarStyle: { 
          position: 'absolute', bottom: insets.bottom > 0 ? insets.bottom : 16,
          left: 16, right: 16, backgroundColor: '#FFF', borderRadius: 40, height: 65,
          shadowColor: '#000', shadowOffset: { width: 0, height: 8 }, shadowOpacity: 0.05, shadowRadius: 16, elevation: 10,
        },
        tabBarIcon: ({ color, focused }) => {
          let iconName = route.name === 'HomeTab' ? 'home' : route.name === 'ExploreTab' ? 'search' : route.name === 'VideoTab' ? 'video' : 'user';
          return <Feather name={iconName} size={22} color={color} style={{ transform: [{ scale: focused ? 1.1 : 1 }] }} />;
        },
      })}
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
        <Stack.Screen name="Home" component={MainTabs} options={{ animation: 'fade' }} /> 
        <Stack.Screen name="Settings" component={SettingsScreen} options={{ animation: 'slide_from_right' }} />
        <Stack.Screen name="EditProfile" component={EditProfileScreen} options={{ animation: 'slide_from_bottom' }} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
