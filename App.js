import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { StatusBar } from 'expo-status-bar';
import { Ionicons } from '@expo/vector-icons';
import { View, Text } from 'react-native';

// Import Screens
import LoginScreen from './src/screens/LoginScreen';
import HomeScreen from './src/screens/HomeScreen';
import ProfileScreen from './src/screens/ProfileScreen'; // We will build this next!

// Placeholder screens for Explore and Video
const ExploreScreen = () => <View style={{flex: 1, justifyContent: 'center', alignItems: 'center'}}><Text style={{fontSize: 24, fontWeight: 'bold'}}>Explore</Text></View>;
const VideoScreen = () => <View style={{flex: 1, justifyContent: 'center', alignItems: 'center'}}><Text style={{fontSize: 24, fontWeight: 'bold'}}>Video</Text></View>;

const Stack = createNativeStackNavigator();
const Tab = createBottomTabNavigator();

// The 4-Tab Bottom Navigation Bar
function MainTabs() {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarShowLabel: false, // Hides the text under icons like Instagram
        tabBarActiveTintColor: '#000000',
        tabBarInactiveTintColor: '#888888',
        tabBarIcon: ({ focused, color, size }) => {
          let iconName;
          if (route.name === 'HomeTab') iconName = focused ? 'home' : 'home-outline';
          else if (route.name === 'ExploreTab') iconName = focused ? 'search' : 'search-outline';
          else if (route.name === 'VideoTab') iconName = focused ? 'play-circle' : 'play-circle-outline';
          else if (route.name === 'ProfileTab') iconName = focused ? 'person-circle' : 'person-circle-outline';
          return <Ionicons name={iconName} size={size + 4} color={color} />;
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
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        <Stack.Screen name="Login" component={LoginScreen} />
        {/* After login, redirect to the Tabs instead of a single screen */}
        <Stack.Screen name="Home" component={MainTabs} /> 
      </Stack.Navigator>
      <StatusBar style="auto" />
    </NavigationContainer>
  );
}
