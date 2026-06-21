import os
import requests
import time
from dotenv import load_dotenv

# ==========================================
# CONFIGURAZIONE AMBIENTE
# ==========================================
load_dotenv()

BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080")
ADMIN_EMAIL = os.getenv("ADMIN_EMAIL")
ADMIN_PASSWORD = os.getenv("ADMIN_PASSWORD")
GITHUB_TOKEN = os.getenv("GITHUB_TOKEN")

if not ADMIN_EMAIL or not ADMIN_PASSWORD:
    print("❌ ERRORE: ADMIN_EMAIL o ADMIN_PASSWORD non trovati nel file .env.")
    exit(1)

TOKEN_CACHE = {}

# ==========================================
# FUNZIONI DI SUPPORTO
# ==========================================

def get_auth_headers(email, password="Password123!"):
    """Effettua il login sul backend e restituisce gli header con il token JWT."""
    if email in TOKEN_CACHE:
        return {"Content-Type": "application/json", "Authorization": f"Bearer {TOKEN_CACHE[email]}"}

    login_url = f"{BACKEND_URL}/api/auth/login"
    try:
        response = requests.post(login_url, json={"email": email, "password": password})

        if response.status_code == 200:
            # FIX: Gestisce sia il caso in cui il backend restituisca JSON, sia Testo Puro
            try:
                data = response.json()
                token = data.get("token") or data.get("accessToken")
            except ValueError:
                token = response.text.strip()

            TOKEN_CACHE[email] = token
            return {"Content-Type": "application/json", "Authorization": f"Bearer {token}"}
        else:
            print(f"  ❌ Login fallito per {email}. Status: {response.status_code}")
    except Exception as e:
        print(f"  ❌ Errore di login per {email}: {e}")

    return None

def get_github_skills(username):
    """Chiama le API di GitHub per estrarre i linguaggi usati dall'utente nei suoi repo."""
    headers = {"Accept": "application/vnd.github.v3+json"}
    if GITHUB_TOKEN:
        headers["Authorization"] = f"token {GITHUB_TOKEN}"

    # Recupera i repository pubblici dell'utente
    url = f"https://api.github.com/users/{username}/repos?per_page=100&type=owner"

    try:
        response = requests.get(url, headers=headers)

        if response.status_code == 200:
            repos = response.json()
            # Estrae il linguaggio principale di ogni repo, ignorando i valori None
            languages = set(repo.get("language") for repo in repos if repo.get("language"))
            return list(languages)

        elif response.status_code == 403:
            print(f"    ⚠️ GitHub Rate Limit superato! Aggiungi un GITHUB_TOKEN al file .env")
        elif response.status_code == 404:
            print(f"    ⚠️ Utente GitHub '{username}' non trovato.")
        else:
            print(f"    ❌ Errore GitHub per {username}: {response.status_code}")

    except Exception as e:
        print(f"    ❌ Errore di connessione a GitHub per {username}: {e}")

    return []

# ==========================================
# LOGICA PRINCIPALE
# ==========================================

def sync_all_user_skills():
    print("🚀 Inizio sincronizzazione Skill da GitHub...")

    # 1. Login come Admin per poter leggere la lista di tutti gli utenti
    admin_headers = get_auth_headers(ADMIN_EMAIL, ADMIN_PASSWORD)
    if not admin_headers:
        print("❌ Autenticazione Admin fallita. Controlla le credenziali o se il backend è acceso.")
        return

    # 2. Recupera tutti gli utenti dal sistema
    print("📥 Recupero lista utenti dal database...")
    users_url = f"{BACKEND_URL}/api/users"
    try:
        users_response = requests.get(users_url, headers=admin_headers)
        if users_response.status_code != 200:
            print(f"❌ Impossibile recuperare gli utenti: {users_response.status_code}")
            return

        users_list = users_response.json()
        print(f"👥 Trovati {len(users_list)} utenti nel sistema.\n")
    except Exception as e:
        print(f"❌ Errore API: {e}")
        return

    # 3. Itera su ogni utente, estrai skill da GitHub e aggiorna il backend
    for index, user in enumerate(users_list, start=1):
        username = user.get("username")
        email = user.get("email")

        if not username or not email:
            continue

        print(f"[{index}/{len(users_list)}] Analisi utente: {username}...")

        # Estrai i linguaggi da GitHub
        skills = get_github_skills(username)

        if not skills:
            print("    ⏭️ Nessuna skill rilevata o utente senza repo. Salto.")
            continue

        print(f"    ⭐ Skill trovate: {', '.join(skills)}")

        # Autenticazione specifica come l'utente corrente per poter aggiornare il suo profilo
        user_headers = get_auth_headers(email, "Password123!")

        if not user_headers:
            print(f"    ⚠️ Impossibile autenticarsi come {email}. Salto l'aggiornamento.")
            continue

        # Chiama l'endpoint per aggiornare le skill
        update_url = f"{BACKEND_URL}/api/users/skills"

        try:
            # Assicurati di passare l'array direttamente, come abbiamo visto prima!
            update_response = requests.put(update_url, json=skills, headers=user_headers)

            if update_response.status_code in [200, 201]:
                print(f"    ✅ Skill aggiornate con successo nel database!")
            else:
                print(f"    ❌ Errore aggiornamento database ({update_response.status_code}): {update_response.text}")
        except Exception as e:
            print(f"    ❌ Errore di rete durante l'aggiornamento: {e}")

        # Piccola pausa per non stressare né il tuo backend né GitHub
        time.sleep(0.5)

    print("\n🎉 SINCRONIZZAZIONE SKILL COMPLETATA!")

if __name__ == "__main__":
    sync_all_user_skills()