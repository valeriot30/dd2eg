import os
import requests
import time
from dotenv import load_dotenv

# ==========================================
# CONFIGURAZIONE AMBIENTE
# ==========================================
# Carica le variabili dal file .env
load_dotenv()

BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080")
ADMIN_EMAIL = os.getenv("ADMIN_EMAIL")
ADMIN_PASSWORD = os.getenv("ADMIN_PASSWORD")
GITHUB_TOKEN = os.getenv("GITHUB_TOKEN") # Fortemente consigliato per evitare limiti di GitHub

if not ADMIN_EMAIL or not ADMIN_PASSWORD:
    print("❌ ERRORE: ADMIN_EMAIL o ADMIN_PASSWORD non trovati nel file .env.")
    exit(1)

# Cache per non richiedere il token JWT a ogni ciclo
TOKEN_CACHE = {}

# ==========================================
# FUNZIONI DI SUPPORTO API
# ==========================================

def get_auth_headers(email, password="Password123!"):
    """Effettua il login sul backend e restituisce gli header con il token JWT."""
    if email in TOKEN_CACHE:
        return {"Content-Type": "application/json", "Authorization": f"Bearer {TOKEN_CACHE[email]}"}

    login_url = f"{BACKEND_URL}/api/auth/login"
    try:
        response = requests.post(login_url, json={"email": email, "password": password})
        if response.status_code == 200:
            try:
                data = response.json()
                token = data.get("token") or data.get("accessToken")
            except ValueError:
                token = response.text.strip()
            TOKEN_CACHE[email] = token
            return {"Content-Type": "application/json", "Authorization": f"Bearer {token}"}
        else:
            print(f"❌ Login fallito per {email}: {response.status_code}")
    except Exception as e:
        print(f"❌ Errore di connessione al backend: {e}")
    return None


def get_github_username(user_id, headers):
    """Chiama il backend per ottenere il vero username a partire dall'ID di MongoDB."""
    url = f"{BACKEND_URL}/api/users/{user_id}"
    try:
        response = requests.get(url, headers=headers)
        if response.status_code == 200:
            user_data = response.json()
            # Cerca il campo username
            return user_data.get("username") or user_data.get("githubUsername")
        else:
            print(f"    ❌ Backend: Utente ID {user_id} non trovato ({response.status_code}).")
    except Exception as e:
        print(f"    ❌ Errore API Utente: {e}")
    return None


def get_project_tasks(project_id, headers):
    """Recupera la lista dei task associati a un progetto specifico dal backend."""
    url = f"{BACKEND_URL}/api/tasks/{project_id}"
    try:
        response = requests.get(url, headers=headers)
        if response.status_code == 200:
            return response.json()
        elif response.status_code == 404:
            # Gestito silenziosamente, significa solo che il progetto non ha task
            return []
        else:
            print(f"    ❌ Errore API Task ({response.status_code}).")
    except Exception as e:
        print(f"    ❌ Errore di rete API Task: {e}")
    return []


def get_github_project_languages(owner, repo):
    """Chiama le API di GitHub per estrarre i linguaggi usati in un repository specifico."""
    headers = {"Accept": "application/vnd.github.v3+json"}
    if GITHUB_TOKEN:
        headers["Authorization"] = f"token {GITHUB_TOKEN}"

    url = f"https://api.github.com/repos/{owner}/{repo}/languages"

    try:
        response = requests.get(url, headers=headers)
        if response.status_code == 200:
            languages_dict = response.json()
            return list(languages_dict.keys())
        elif response.status_code == 403:
            print("    ⚠️ GitHub Rate Limit superato! Aggiungi GITHUB_TOKEN nel file .env")
        elif response.status_code == 404:
            print(f"    ⚠️ Repo GitHub non trovato: {owner}/{repo}")
        else:
            print(f"    ❌ Errore API GitHub ({response.status_code}) per {owner}/{repo}")
    except Exception as e:
        # Se cade internet, catturiamo l'errore senza far crashare tutto lo script
        print(f"    ❌ Errore di connessione a GitHub: {e}")

    return []

# ==========================================
# LOGICA PRINCIPALE
# ==========================================

def sync_all_task_skills():
    print("🚀 Inizio sincronizzazione Skill per i Task...\n")

    # 1. Login iniziale come Amministratore
    admin_headers = get_auth_headers(ADMIN_EMAIL, ADMIN_PASSWORD)
    if not admin_headers:
        print("🛑 Impossibile procedere senza autenticazione.")
        return

    # 2. Recupera tutti i progetti
    print("📥 Recupero lista progetti dal database...")
    try:
        projects_response = requests.get(f"{BACKEND_URL}/api/projects", headers=admin_headers)
        if projects_response.status_code != 200:
            print(f"❌ Impossibile recuperare i progetti ({projects_response.status_code})")
            return
        projects_list = projects_response.json()
        print(f"📁 Trovati {len(projects_list)} progetti da analizzare.\n")
    except Exception as e:
        print(f"❌ Errore critico API Progetti: {e}")
        return

    # 3. Itera su ogni progetto
    for index, project in enumerate(projects_list, start=1):
        project_id = project.get("id")
        owner_id = project.get("owner") or project.get("creatorId")
        repo = project.get("name")

        if not owner_id or not repo or not project_id:
            continue

        print(f"[{index}/{len(projects_list)}] Analisi progetto ID: {project_id}...")

        # 4. Traduciamo l'ID del creatore nel vero username di GitHub
        github_username = get_github_username(owner_id, admin_headers)

        if not github_username:
            print(f"    ⚠️ Username mancante per il creatore ID {owner_id}. Salto {repo}.")
            continue

        print(f"    👤 Repository target: {github_username}/{repo}")

        # 5. Chiediamo i task del progetto
        project_tasks = get_project_tasks(project_id, admin_headers)

        if not project_tasks:
            print("    ⏭️ Nessun task trovato per questo progetto. Salto.")
            continue

        # 6. Scarichiamo le skill da GitHub
        skills = get_github_project_languages(github_username, repo)

        if not skills:
            print("    ⏭️ Nessun linguaggio rilevato su GitHub. Salto l'aggiornamento.")
            continue

        print(f"    ⭐ Skill trovate: {', '.join(skills)}")
        print(f"    🔄 Aggiornamento di {len(project_tasks)} task in corso...")

        # 7. Aggiorniamo i task uno a uno
        for task in project_tasks:
            task_id = task.get("id")
            update_url = f"{BACKEND_URL}/api/tasks/{task_id}/update"

            try:
                # Strutturiamo il JSON esattamente come si aspetta l'UpdateTaskDTO
                payload = {"skills": skills}
                update_response = requests.put(update_url, json=payload, headers=admin_headers)

                if update_response.status_code in [200, 201]:
                    print(f"      ✅ Task {task_id} aggiornato!")
                else:
                    print(f"      ❌ Errore aggiornamento task {task_id}: {update_response.status_code}")
            except Exception as e:
                print(f"      ❌ Errore di rete su task {task_id}: {e}")

        # Piccola pausa di cortesia per evitare che GitHub blocchi il tuo IP
        time.sleep(0.5)

    print("\n🎉 SINCRONIZZAZIONE COMPLETATA CON SUCCESSO!")

if __name__ == "__main__":
    sync_all_task_skills()