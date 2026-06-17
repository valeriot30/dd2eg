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
    print("❌ ERROR: ADMIN_EMAIL or ADMIN_PASSWORD not found in .env file. Cannot authenticate.")
    exit(1)

# ==========================================
# CORE IMPORT LOGIC
# ==========================================

def authenticate():
    """Authenticates with the Spring Boot backend and returns the JWT token."""
    print("🔐 Attempting to log in to the backend...")

    login_url = f"{BACKEND_URL}/api/auth/login"

    payload = {
        "email": ADMIN_EMAIL,
        "password": ADMIN_PASSWORD
    }

    try:
        response = requests.post(login_url, json=payload)

        if response.status_code == 200:
            try:
                response_data = response.json()
                token = response_data.get("token") or response_data.get("accessToken")
            except ValueError:
                raw_token = response.text.strip()
                if raw_token.startswith("eyJ"):
                    print("  ✅ Successfully extracted raw JWT token!\n")
                    return raw_token
                else:
                    print("❌ ERROR: The backend returned 200 OK, but the body is neither JSON nor a valid JWT token.")
                    exit(1)

            if not token:
                print("❌ ERROR: Login successful, but no token field found.")
                exit(1)

            print("  ✅ Login successful! JWT Token acquired.\n")
            return token

        else:
            print(f"❌ ERROR: Authentication failed. Status {response.status_code}")
            exit(1)

    except requests.exceptions.RequestException as e:
        print(f"❌ ERROR: Network connection failed. Is Spring Boot running?")
        exit(1)


def import_system_data():
    if not os.path.exists(INPUT_FILE):
        print(f"❌ ERROR: Input file '{INPUT_FILE}' not found. Run the GitHub extractor script first.")
        return

    # 1. AUTHENTICATE AND GET HEADERS
    token = authenticate()
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {token}"
    }

    print(f"📂 Loading data from {INPUT_FILE}...")
    with open(INPUT_FILE, "r", encoding="utf-8") as f:
        projects_batch = json.load(f)

    # ==========================================
    # NEW STEP: EXTRACT AND CREATE USERS FIRST
    # ==========================================
    print("🔍 Scanning for unique contributors...")
    unique_users = set()
    
    for project in projects_batch:
        for task in project.get("tasks", []):
            for contributor in task.get("contributors", []):
                if contributor and contributor != "unknown_user":
                    unique_users.add(contributor)
                    
    print(f"👥 Found {len(unique_users)} unique users. Syncing with backend...")
    
    # Assumiamo che l'endpoint per registrare/creare un utente sia POST /api/users
    # Cambia user_url se il tuo endpoint è /api/auth/register
    user_url = f"{BACKEND_URL}/api/users" 
    
    for username in unique_users:
        user_payload = {
            "username": username,
            "email": f"{username}@github.dev", # Generiamo un'email univoca basata sull'username
            "password": "Password123!",        # Password fittizia standard
            "userType": "DEVELOPER"            # Assegniamo il ruolo base
        }
        
        try:
            res = requests.post(user_url, json=user_payload, headers=headers)
            if res.status_code in [200, 201]:
                print(f"  👤 Created user: {username}")
            # Se restituisce 400 o 409, probabile che l'utente esista già (es. per l'email univoca)
            elif res.status_code in [400, 409] or "duplicate" in res.text.lower() or "exists" in res.text.lower():
                pass # Utente già esistente, procediamo silenziosamente
            else:
                print(f"  ⚠️ Warning creating user {username}: {res.status_code} - {res.text}")
        except Exception as e:
            print(f"  ❌ Connection error creating user {username}: {str(e)}")

    print("\n🚀 Starting transaction pipeline for Projects and Tasks...\n")

    # ==========================================
    # PIPELINE: PROJECTS, TASKS AND COMMENTS
    # ==========================================
    for project in projects_batch:
        
        # CREATE PROJECT
        project_payload = {
            "name": project["projectName"],
            "description": f"Open Source Project imported from GitHub ({project['owner']}/{project['projectName']}).",
            "tags": project["tags"]
        }

        print(f"📦 Importing Project: {project['projectName']}...")
        project_url = f"{BACKEND_URL}/api/projects"

        try:
            project_response = requests.post(project_url, json=project_payload, headers=headers)
            if project_response.status_code not in [200, 201]:
                print(f"  ❌ Failed to create project '{project['projectName']}': {project_response.status_code} - {project_response.text}")
                continue

            saved_project = project_response.json()
            project_id = saved_project.get("id")
            
            if not project_id:
                print(f"  ❌ Project created but 'id' field is missing from response. Ensure your DTO returns 'id'.")
                continue

        except Exception as e:
            print(f"  ❌ Connection error while creating project: {str(e)}")
            continue

        # CREATE TASKS FOR THIS PROJECT
        for task in project.get("tasks", []):
            task_payload = {
                "projectId": project_id,
                "title": task["title"],
                "description": task["description"][:500] if task["description"] else "No description provided.",
                "body": task["description"],
                "priority": "MEDIUM", # Assicurati che nel Java DTO questo campo sia una Stringa o un Enum
                "numMaxCommits": 10
            }

            print(f"    📝 Creating Task: {task['title'][:40]}...")
            # Corretto: rimosso /add
            task_url = f"{BACKEND_URL}/api/tasks/add"

            try:
                task_response = requests.post(task_url, json=task_payload, headers=headers)
                if task_response.status_code not in [200, 201]:
                    print(f"      ❌ Failed to create task: {task_response.status_code} - {task_response.text}")
                    continue

                saved_task = task_response.json()
                task_id = saved_task.get("id")

            except Exception as e:
                print(f"      ❌ Connection error while creating task: {str(e)}")
                continue

            # ADD COMMENTS TO THIS TASK
            for comment in task.get("comments", []):
                comment_payload = {
                    "content": comment["content"]
                }

                # Corretto: rimosso /add
                comment_url = f"{BACKEND_URL}/api/tasks/{task_id}/comments/add"

                try:
                    comment_response = requests.post(comment_url, json=comment_payload, headers=headers)
                    if comment_response.status_code not in [200, 201]:
                        print(f"        ⚠️ Warning: Could not append comment by {comment['authorUsername']}")
                except Exception as e:
                    print(f"        ⚠️ Connection error on comment insertion: {str(e)}")

        time.sleep(0.5)

    print("\n🎉 IMPORT PROCESS COMPLETED! All valid data has been synced to MongoDB.")

if __name__ == "__main__":
    start_time = time.time()
    import_system_data()
    print(f"⏱️ Total execution time: {round(time.time() - start_time, 2)} seconds.")