import os
import json
import requests
import time
import random
from dotenv import load_dotenv

# ==========================================
# ENVIRONMENT CONFIGURATION
# ==========================================
load_dotenv()

BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080")
ADMIN_EMAIL = os.getenv("ADMIN_EMAIL")
ADMIN_PASSWORD = os.getenv("ADMIN_PASSWORD")
INPUT_FILE = "dev2enterprise_dump.json"

if not ADMIN_EMAIL or not ADMIN_PASSWORD:
    print("❌ ERROR: ADMIN_EMAIL or ADMIN_PASSWORD not found in .env file.")
    exit(1)

# ==========================================
# AUTHENTICATION, CACHE & AUTO-CREATION
# ==========================================
TOKEN_CACHE = {}

def create_missing_user(username, role="DEVELOPER"):
    """Crea un utente al volo assegnandogli il ruolo specificato (DEVELOPER o ENTERPRISE)"""
    user_url = f"{BACKEND_URL}/api/users"
    user_payload = {
        "username": username,
        "name": username,
        "email": f"{username}@github.dev",
        "password": "Password123!",
        "userType": role
    }

    try:
        # Usa temporaneamente le credenziali Admin per creare l'utente
        admin_login = requests.post(f"{BACKEND_URL}/api/auth/login", json={"email": ADMIN_EMAIL, "password": ADMIN_PASSWORD})
        admin_token = admin_login.json().get("token") or admin_login.json().get("accessToken") if admin_login.status_code == 200 else ""
        headers = {"Content-Type": "application/json", "Authorization": f"Bearer {admin_token}"} if admin_token else {}

        res = requests.post(user_url, json=user_payload, headers=headers)
        if res.status_code in [200, 201]:
            print(f"  🌟 Auto-created missing user: {username} (Role: {role})")
            return True
        else:
            print(f"  ❌ Auto-create failed for {username}: {res.text}")
            return False
    except Exception as e:
        print(f"  ❌ Error creating user {username}: {e}")
        return False

def get_auth_headers(username, role="DEVELOPER", attempt=1):
    """Esegue il login. Se fallisce, crea l'utente col ruolo specificato e riprova in automatico."""
    email = f"{username}@github.dev"
    password = "Password123!"

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
            # SE IL LOGIN FALLISCE AL PRIMO TENTATIVO -> CREA L'UTENTE E RIPROVA
            if attempt == 1:
                if create_missing_user(username, role):
                    return get_auth_headers(username, role, attempt=2)

            return None # Se fallisce anche al secondo tentativo, rinuncia.

    except requests.exceptions.RequestException:
        return None

# ==========================================
# CORE IMPORT LOGIC
# ==========================================

def import_system_data():
    if not os.path.exists(INPUT_FILE):
        print(f"❌ ERROR: Input file '{INPUT_FILE}' not found.")
        return

    print(f"📂 Loading data from {INPUT_FILE}...")
    with open(INPUT_FILE, "r", encoding="utf-8") as f:
        projects_batch = json.load(f)

    print("\n🚀 Starting transaction pipeline for Projects, Tasks, Comments and Commits...\n")

    for project in projects_batch:
        owner_username = project.get("owner", "unknown")

        # L'owner del progetto viene creato come ENTERPRISE
        owner_headers = get_auth_headers(owner_username, role="ENTERPRISE")

        if not owner_headers:
            print(f"    ⚠️ Skipping project '{project.get('projectName')}': Cannot create or authenticate owner {owner_username}")
            continue

        # CREATE PROJECT (con descrizione reale fixata)
        real_description = project.get("description")
        if not real_description:
            real_description = f"Open Source Project imported from GitHub ({owner_username}/{project.get('projectName', 'Unknown')})."

        project_payload = {
            "name": project.get("projectName", "Unknown Project"),
            "description": real_description,
            "tags": project.get("tags", [])
        }

        print(f"📦 Importing Project: {project_payload['name']}...")
        project_url = f"{BACKEND_URL}/api/projects"

        try:
            project_response = requests.post(project_url, json=project_payload, headers=owner_headers)
            if project_response.status_code not in [200, 201]:
                print(f"    ❌ Failed to create project: {project_response.status_code} - {project_response.text}")
                continue

            project_id = project_response.json().get("id")
            if not project_id: continue
        except Exception as e:
            print(f"    ❌ Exception creating project: {str(e)}")
            continue

        # CREATE TASKS
        for task in project.get("tasks", []):
            task_desc = task.get("description") or "No description provided."
            commits_list = task.get("commits", [])

            # Calcolo numMaxCommits: numero di commit reali + un offset random da 0 a 5
            real_commits_count = len(commits_list)
            max_commits = max(1, real_commits_count + random.randint(0, 5))

            # Generazione budget casuale per il task (tra 50 e 1000)
            random_budget = random.randint(50, 1000)

            task_payload = {
                "projectId": project_id,
                "title": task.get("title", "Untitled Task"),
                "description": task_desc[:500],
                "body": task_desc,
                "priority": "MEDIUM",
                "numMaxCommits": max_commits,
                "budget": random_budget,
                "skills": []
            }

            print(f"    📝 Creating Task: {task_payload['title'][:40]} (Max Commits: {max_commits}, Budget: {random_budget})...")
            task_url = f"{BACKEND_URL}/api/tasks/add"

            try:
                task_response = requests.post(task_url, json=task_payload, headers=owner_headers)
                if task_response.status_code not in [200, 201]:
                    print(f"      ❌ Failed to create task: {task_response.status_code} - {task_response.text}")
                    continue
                task_id = task_response.json().get("id")
            except Exception as e:
                print(f"      ❌ Exception creating task: {str(e)}")
                continue

            # ADD COMMENTS
            for comment in task.get("comments", []):
                if not comment.get("content"): continue

                commenter_username = comment.get("authorUsername", "unknown")
                # I commentatori vengono creati come DEVELOPER
                commenter_headers = get_auth_headers(commenter_username, role="DEVELOPER")

                if not commenter_headers:
                    continue

                comment_url = f"{BACKEND_URL}/api/tasks/{task_id}/comments/add"
                try:
                    requests.post(comment_url, json={"content": comment.get("content")}, headers=commenter_headers)
                except Exception: pass

            # ADD COMMITS
            for commit in commits_list:
                committer_username = commit.get("authorUsername", "unknown")
                # I committer vengono creati come DEVELOPER
                committer_headers = get_auth_headers(committer_username, role="DEVELOPER")

                if not committer_headers:
                    print(f"        ⚠️ Skipping commit {commit.get('hash', '')[:7]} - Cannot authenticate {committer_username}")
                    continue

                commit_payload = {
                    "hash": commit.get("hash", f"mock-{int(time.time()*1000)}"),
                    "comment": commit.get("message", "Commit message")[:200],
                    "numLines": commit.get("numLines", 10)
                }

                commit_url = f"{BACKEND_URL}/api/tasks/{task_id}/commits/add"
                try:
                    commit_response = requests.post(commit_url, json=commit_payload, headers=committer_headers)
                    if commit_response.status_code not in [200, 201]:
                        print(f"        ❌ Backend Error {commit_response.status_code}: {commit_response.text}")
                except Exception as e:
                    print(f"        ⚠️ Connection error on commit insertion: {str(e)}")

        time.sleep(0.5)

    print("\n🎉 IMPORT PROCESS COMPLETED! Projects, Tasks, Comments, and Commits are now safely stored in MongoDB.")

if __name__ == "__main__":
    start_time = time.time()
    import_system_data()
    print(f"⏱️ Total execution time: {round(time.time() - start_time, 2)} seconds.")