import os
import json
import requests
import time
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
# AUTHENTICATION & TOKEN CACHE
# ==========================================
TOKEN_CACHE = {}

def get_auth_headers(email, password):
    """Logs in and returns the headers with the JWT token. Uses cache."""
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
            return None
    except requests.exceptions.RequestException:
        return None

# ==========================================
# CORE IMPORT LOGIC
# ==========================================

def import_system_data():
    if not os.path.exists(INPUT_FILE):
        print(f"❌ ERROR: Input file '{INPUT_FILE}' not found.")
        return

    admin_headers = get_auth_headers(ADMIN_EMAIL, ADMIN_PASSWORD)
    if not admin_headers:
        print("❌ ERROR: Admin authentication failed. Check credentials and server status.")
        exit(1)

    print(f"📂 Loading data from {INPUT_FILE}...")
    with open(INPUT_FILE, "r", encoding="utf-8") as f:
        projects_batch = json.load(f)

    # ==========================================
    # 1. EXTRACT ALL USERS
    # ==========================================
    print("🔍 Scanning for all unique users (Owners, Contributors, Commenters, Committers)...")
    unique_users = set()

    for project in projects_batch:
        if project.get("owner"): unique_users.add(project.get("owner"))

        for task in project.get("tasks", []):
            for contributor in task.get("contributors", []):
                if contributor and contributor != "unknown_user":
                    unique_users.add(contributor)

            for comment in task.get("comments", []):
                if comment.get("authorUsername"): unique_users.add(comment.get("authorUsername"))

            # NUOVO: Estrazione autori dei commit
            for commit in task.get("commits", []):
                if commit.get("authorUsername"): unique_users.add(commit.get("authorUsername"))

    print(f"👥 Found {len(unique_users)} unique users. Syncing with backend...")

    user_url = f"{BACKEND_URL}/api/users"

    for username in unique_users:
        user_payload = {
            "username": username,
            "name": username,
            "email": f"{username}@github.dev",
            "password": "Password123!",
            "userType": "DEVELOPER"
        }

        try:
            res = requests.post(user_url, json=user_payload, headers=admin_headers)
            if res.status_code in [200, 201]:
                print(f"  👤 Created user: {username}")
        except Exception:
            pass

    print("\n🚀 Starting transaction pipeline for Projects, Tasks, Comments and Commits...\n")

    # ==========================================
    # 2. PIPELINE
    # ==========================================
    for project in projects_batch:
        owner_username = project.get("owner", "unknown")
        owner_headers = get_auth_headers(f"{owner_username}@github.dev", "Password123!") or admin_headers

        # CREATE PROJECT
        project_payload = {
            "name": project.get("projectName", "Unknown Project"),
            "description": f"Open Source Project imported from GitHub ({owner_username}/{project.get('projectName', '')}).",
            "tags": project.get("tags", [])
        }

        print(f"📦 Importing Project: {project_payload['name']}...")
        project_url = f"{BACKEND_URL}/api/projects"

        try:
            project_response = requests.post(project_url, json=project_payload, headers=owner_headers)
            if project_response.status_code not in [200, 201]: continue
            project_id = project_response.json().get("id")
            if not project_id: continue
        except Exception:
            continue

        # CREATE TASKS
        for task in project.get("tasks", []):
            task_desc = task.get("description") or "No description provided."
            commits_list = task.get("commits", [])

            # Pre-allochiamo almeno 10 slot, o quanti sono i commit effettivi nel JSON
            max_commits = max(10, len(commits_list))

            task_payload = {
                "projectId": project_id,
                "title": task.get("title", "Untitled Task"),
                "description": task_desc[:500],
                "body": task_desc,
                "priority": "MEDIUM",
                "numMaxCommits": max_commits,
                "skills": []
            }

            print(f"    📝 Creating Task: {task_payload['title'][:40]}...")
            task_url = f"{BACKEND_URL}/api/tasks/add"

            try:
                task_response = requests.post(task_url, json=task_payload, headers=owner_headers)
                if task_response.status_code not in [200, 201]: continue
                task_id = task_response.json().get("id")
            except Exception:
                continue

            # ADD COMMENTS
            for comment in task.get("comments", []):
                if not comment.get("content"): continue

                commenter_email = f"{comment.get('authorUsername')}@github.dev"
                commenter_headers = get_auth_headers(commenter_email, "Password123!") or admin_headers

                comment_url = f"{BACKEND_URL}/api/tasks/{task_id}/comments/add"
                try:
                    requests.post(comment_url, json={"content": comment.get("content")}, headers=commenter_headers)
                except Exception: pass

            # NUOVO: ADD COMMITS
            for commit in commits_list:
                committer_author = commit.get("authorUsername", "unknown")
                committer_email = f"{committer_author}@github.dev"
                committer_headers = get_auth_headers(committer_email, "Password123!") or admin_headers

                # Mappa i campi secondo il tuo CreateCommitDTO Java
                commit_payload = {
                    "hash": commit.get("hash", f"mock-{int(time.time()*1000)}"), # Fallback se manca
                    "comment": commit.get("message", "Commit message")[:200],    # Usa 'message' o adatta alla tua JSON structure
                    "numLines": commit.get("numLines", 10)                       # Fallback se manca
                }

                commit_url = f"{BACKEND_URL}/api/tasks/{task_id}/commits/add"
                try:
                    commit_response = requests.post(commit_url, json=commit_payload, headers=committer_headers)
                    if commit_response.status_code not in [200, 201]:
                        print(f"        ⚠️ Warning: Failed to add commit {commit_payload['hash'][:7]}")
                except Exception as e:
                    print(f"        ⚠️ Connection error on commit insertion: {str(e)}")

        time.sleep(0.5)

    print("\n🎉 IMPORT PROCESS COMPLETED! Projects, Comments, and Commits are now safely stored in MongoDB.")

if __name__ == "__main__":
    start_time = time.time()
    import_system_data()
    print(f"⏱️ Total execution time: {round(time.time() - start_time, 2)} seconds.")