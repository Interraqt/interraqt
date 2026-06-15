import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { StatusBar } from 'expo-status-bar';
import { Feather } from '@expo/vector-icons';
import { View, Text } from 'react-native';

import LoginScreen from './src/screens/LoginScreen';
import HomeScreen from './src/screens/HomeScreen';
import ProfileScreen from './src/screens/ProfileScreen';
import SettingsScreen from './src/screens/SettingsScreen'; 

const ExploreScreen = () => <View style={{flex: 1, justifyContent: 'center', alignItems: 'center'}}><Text style={{fontSize: 24, fontWeight: 'bold'}}>Explore</Text></View>;
const VideoScreen = () => <View style={{flex: 1, justifyContent: 'center', alignItems: 'center'}}><Text style={{fontSize: 24, fontWeight: 'bold'}}>Video</Text></View>;

const Stack = createNativeStackNavigator();
const Tab = createBottomTabNavigator();

function MainTabs() {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarShowLabel: false,
        tabBarActiveTintColor: '#000',
        tabBarInactiveTintColor: '#999',
        tabBarStyle: { backgroundColor: '#FFF', borderTopWidth: 1, borderColor: '#EAEAEA' },
        tabBarIcon: ({ color, size }) => {
          let iconName;
          if (route.name === 'HomeTab') iconName = 'home';
          else if (route.name === 'ExploreTab') iconName = 'search';
          else if (route.name === 'VideoTab') iconName = 'play-circle';
          else if (route.name === 'ProfileTab') iconName = 'user';
          return <Feather name={iconName} size={size} color={color} />;
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
    <NavigationContainer>
      <Stack.Navigator 
        screenOptions={{ 
          headerShown: false,
          animation: 'slide_from_right' // Smooth WhatsApp style animation
        }}
      >
        <Stack.Screen name="Login" component={LoginScreen} />
        <Stack.Screen name="Home" component={MainTabs} /> 
        <Stack.Screen name="Settings" component={SettingsScreen} />
      </Stack.Navigator>
      <StatusBar style="dark" />
    </NavigationContainer>
  );
}
