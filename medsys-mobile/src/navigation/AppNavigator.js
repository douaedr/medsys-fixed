import { NavigationContainer } from '@react-navigation/native'
import { createNativeStackNavigator } from '@react-navigation/native-stack'
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs'
import { Text } from 'react-native'
import { useAuth } from '../context/AuthContext'
import { colors } from '../theme'

import LoginScreen from '../screens/LoginScreen'
import RegisterScreen from '../screens/RegisterScreen'
import ForgotPasswordScreen from '../screens/ForgotPasswordScreen'
import HomeScreen from '../screens/HomeScreen'
import ProfileScreen from '../screens/ProfileScreen'
import DossierScreen from '../screens/DossierScreen'
import RendezVousScreen from '../screens/RendezVousScreen'
import MessagerieScreen from '../screens/MessagerieScreen'
import DocumentsScreen from '../screens/DocumentsScreen'

const Stack = createNativeStackNavigator()
const Tab = createBottomTabNavigator()

const HEADER_OPTS = {
  headerStyle: { backgroundColor: '#1e3a8a' },
  headerTintColor: 'white',
  headerTitleStyle: { fontWeight: '800', fontSize: 16 },
}

function MainTabs() {
  return (
    <Tab.Navigator
      screenOptions={{
        ...HEADER_OPTS,
        tabBarActiveTintColor: colors.primary,
        tabBarInactiveTintColor: colors.gray,
        tabBarStyle: { borderTopWidth: 1, borderTopColor: colors.border, paddingBottom: 4, height: 58 },
        tabBarLabelStyle: { fontSize: 10, fontWeight: '600' },
      }}
    >
      <Tab.Screen
        name="Accueil"
        component={HomeScreen}
        options={{
          tabBarIcon: ({ color }) => <Text style={{ fontSize: 20 }}>🏠</Text>,
          headerTitle: '🏥 MedSys',
        }}
      />
      <Tab.Screen
        name="Dossier"
        component={DossierScreen}
        options={{
          tabBarIcon: ({ color }) => <Text style={{ fontSize: 20 }}>📁</Text>,
          headerTitle: '📁 Dossier médical',
        }}
      />
      <Tab.Screen
        name="RendezVous"
        component={RendezVousScreen}
        options={{
          tabBarLabel: 'RDV',
          tabBarIcon: ({ color }) => <Text style={{ fontSize: 20 }}>📅</Text>,
          headerTitle: '📅 Mes rendez-vous',
        }}
      />
      <Tab.Screen
        name="Messagerie"
        component={MessagerieScreen}
        options={{
          tabBarIcon: ({ color }) => <Text style={{ fontSize: 20 }}>💬</Text>,
          headerTitle: '💬 Messagerie',
        }}
      />
      <Tab.Screen
        name="Documents"
        component={DocumentsScreen}
        options={{
          tabBarIcon: ({ color }) => <Text style={{ fontSize: 20 }}>📂</Text>,
          headerTitle: '📂 Mes documents',
        }}
      />
      <Tab.Screen
        name="Profil"
        component={ProfileScreen}
        options={{
          tabBarIcon: ({ color }) => <Text style={{ fontSize: 20 }}>👤</Text>,
          headerTitle: '👤 Mon profil',
        }}
      />
    </Tab.Navigator>
  )
}

function AuthStack() {
  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      <Stack.Screen name="Login" component={LoginScreen} />
      <Stack.Screen name="Register" component={RegisterScreen} />
      <Stack.Screen name="ForgotPassword" component={ForgotPasswordScreen} />
    </Stack.Navigator>
  )
}

export default function AppNavigator() {
  const { isAuthenticated, loading } = useAuth()

  if (loading) return null

  return (
    <NavigationContainer>
      {isAuthenticated ? <MainTabs /> : <AuthStack />}
    </NavigationContainer>
  )
}
