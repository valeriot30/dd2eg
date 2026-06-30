import os
import requests
import time
from dotenv import load_dotenv
import re

# Dizionario delle Skill con le relative espressioni regolari (keywords)
SKILL_KEYWORDS = {
    # ==========================
    # LINGUAGGI DI PROGRAMMAZIONE
    # ==========================
    "Python": [r"\bpython\b", r"\bpip\b", r"\bdjango\b", r"\bflask\b", r"\bpytest\b"],
    "JavaScript": [r"\bjavascript\b", r"\bjs\b", r"\bes6\b", r"\bnode\.?js\b", r"\bnpm\b"],
    "TypeScript": [r"\btypescript\b", r"\bts\b"],
    "Java": [r"\bjava\b", r"\bspring\b", r"\bmaven\b", r"\bgradle\b"],
    "C++": [r"\bc\+\+\b", r"\bcpp\b", r"\bgcc\b", r"\bcmake\b"],
    "C": [r"\bc\b", r"\bclang\b", r"\bmalloc\b", r"\bpointers\b"],
    "C#": [r"\bc#\b", r"\bcsharp\b", r"\b\.net\b", r"\bdotnet\b"],
    "Go": [r"\bgo\b", r"\bgolang\b", r"\bgoroutine\b"],
    "Rust": [r"\brust\b", r"\bcargo\b", r"\bcrate\b"],
    "Ruby": [r"\bruby\b", r"\bgem\b", r"\brails\b"],
    "PHP": [r"\bphp\b", r"\blaravel\b", r"\bcomposer\b", r"\bsymfony\b"],
    "Swift": [r"\bswift\b", r"\bxcrun\b", r"\bios\b", r"\bxcode\b"],
    "Kotlin": [r"\bkotlin\b", r"\bcoroutines\b"],

    # ==========================
    # FRONTEND & WEB
    # ==========================
    "React": [r"\breact\b", r"\bjsx\b", r"\breactnative\b", r"\bnext\.?js\b"],
    "Vue.js": [r"\bvue\b", r"\bvuejs\b", r"\bnuxt\b"],
    "Angular": [r"\bangular\b", r"\brxjs\b"],
    "Svelte": [r"\bsvelte\b"],
    "HTML/CSS": [r"\bhtml\b", r"\bhtml5\b", r"\bcss\b", r"\bcss3\b", r"\bsass\b", r"\bscss\b", r"\btailwind\b", r"\bbootstrap\b"],
    "WebAssembly": [r"\bwasm\b", r"\bwebassembly\b"],

    # ==========================
    # DATABASE & DATI
    # ==========================
    "SQL": [r"\bsql\b", r"\bquery\b", r"\bmysql\b", r"\bpostgres\b", r"\bpostgresql\b", r"\bmariadb\b"],
    "NoSQL": [r"\bnosql\b", r"\bmongo\b", r"\bmongodb\b", r"\bcassandra\b", r"\bcouchdb\b"],
    "Redis": [r"\bredis\b", r"\bcaching\b", r"\bmemcached\b"],
    "Data Engineering": [r"\bkafka\b", r"\bspark\b", r"\bhadoop\b", r"\belasticsearch\b"],

    # ==========================
    # INTELLIGENZA ARTIFICIALE & ML
    # ==========================
    "Machine Learning": [r"\bmachine learning\b", r"\bml\b", r"\bscikit-learn\b", r"\bsklearn\b"],
    "Deep Learning": [r"\bdeep learning\b", r"\bpvtorch\b", r"\bpytorch\b", r"\btensorflow\b", r"\bkeras\b", r"\bneural network\b"],
    "Data Science": [r"\bdata science\b", r"\bpandas\b", r"\bnumpy\b", r"\bmatplotlib\b", r"\bdata analysis\b"],

    # ==========================
    # DEVOPS & INFRASTRUTTURA
    # ==========================
    "Docker": [r"\bdocker\b", r"\bdockerfile\b", r"\bcontainer\b"],
    "Kubernetes": [r"\bkubernetes\b", r"\bk8s\b", r"\bhelm\b", r"\bpod\b"],
    "CI/CD": [r"\bci/cd\b", r"\bgithub actions\b", r"\bjenkins\b", r"\bgitlab ci\b", r"\btravis\b", r"\bpipeline\b"],
    "Cloud Computing": [r"\baws\b", r"\bazure\b", r"\bgcp\b", r"\bgoogle cloud\b", r"\bcloud\b"],
    "Infrastructure as Code": [r"\bterraform\b", r"\bansible\b", r"\biac\b"],

    # ==========================
    # HARD & SOFT SKILLS GENERICHE
    # ==========================
    "Software Testing": [r"\btest\b", r"\btesting\b", r"\bunit test\b", r"\bmock\b", r"\bjest\b", r"\bcypress\b", r"\bqa\b"],
    "Cybersecurity": [r"\bsecurity\b", r"\bvulnerability\b", r"\bauth\b", r"\boauth\b", r"\bcwe\b", r"\bcve\b", r"\bcrypto\b", r"\bxss\b"],
    "Performance Optimization": [r"\bperformance\b", r"\boptimization\b", r"\bmemory leak\b", r"\bprofiling\b"],
    "Technical Writing": [r"\bdocumentation\b", r"\bdocs\b", r"\breadme\b", r"\bmarkdown\b", r"\btypo\b"],
    "UI/UX Design": [r"\bui\b", r"\bux\b", r"\bdesign\b", r"\baccessibility\b", r"\ba11y\b", r"\bcolor\b", r"\blayout\b"]
}

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

        print(f"    🔄 Analisi testo e aggiornamento di {len(project_tasks)} task in corso...")

        # 7. Aggiorniamo i task uno a uno
        for task in project_tasks:
            task_id = task.get("id")
            
            # 1. Uniamo Titolo e Descrizione e convertiamo in minuscolo
            titolo = task.get("title") or ""
            descrizione = task.get("description") or ""
            testo_completo = f"{titolo} {descrizione}".lower()
            
            # 2. Troviamo le skill con le Regex
            skills_trovate = set()
            for skill, keywords in SKILL_KEYWORDS.items():
                for keyword in keywords:
                    if re.search(keyword, testo_completo):
                        skills_trovate.add(skill)
                        break

            if not skills_trovate:
                # Se non trova niente salta
                continue

            update_url = f"{BACKEND_URL}/api/tasks/{task_id}/update"

            try:
                # Strutturiamo il JSON esattamente come si aspetta l'UpdateTaskDTO
                payload = {"skills": list(skills_trovate)}
                update_response = requests.put(update_url, json=payload, headers=admin_headers)

                if update_response.status_code in [200, 201]:
                    print(f"      ✅ Task {task_id} aggiornato con: {list(skills_trovate)}")
                else:
                    print(f"      ❌ Errore aggiornamento task {task_id}: {update_response.status_code}")
            except Exception as e:
                print(f"      ❌ Errore di rete su task {task_id}: {e}")

    print("\n🎉 SINCRONIZZAZIONE COMPLETATA CON SUCCESSO!")

if __name__ == "__main__":
    sync_all_task_skills()