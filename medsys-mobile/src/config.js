// ─────────────────────────────────────────────────────────────────────────────
//  Configuration des URLs API — medsys-mobile
//
//  En développement, créer un fichier .env à la racine du projet mobile :
//
//    EXPO_PUBLIC_API_HOST=192.168.1.42   ← votre IP locale (sans http://)
//
//  Comment trouver votre IP locale :
//    Windows → cmd → ipconfig          → "Adresse IPv4"
//    Linux   → terminal → hostname -I
//    Mac     → terminal → ifconfig en0 → "inet"
//
//  ⚠️  NE PAS utiliser "localhost" depuis un téléphone physique.
//      Expo Go sur émulateur Android peut utiliser "10.0.2.2" pour localhost.
// ─────────────────────────────────────────────────────────────────────────────

const API_HOST = process.env.EXPO_PUBLIC_API_HOST || '10.0.2.2'; // fallback émulateur Android

// En production, toutes les requêtes passent par l'api-gateway (port 8080)
const GATEWAY_URL = process.env.EXPO_PUBLIC_GATEWAY_URL || `http://${API_HOST}:8080`;

// URLs directes (dev uniquement, si pas de gateway)
export const AUTH_BASE_URL    = process.env.EXPO_PUBLIC_AUTH_URL    || `http://${API_HOST}:8082/api/v1`;
export const PATIENT_BASE_URL = process.env.EXPO_PUBLIC_PATIENT_URL || `http://${API_HOST}:8081/api/v1`;

export default { API_HOST, GATEWAY_URL, AUTH_BASE_URL, PATIENT_BASE_URL };
