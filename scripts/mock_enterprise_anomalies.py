import os
import requests
import random
import time
from dotenv import load_dotenv

# ==========================================
# CONFIGURATION
# ==========================================
load_dotenv()

BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080")
ADMIN_EMAIL = os.getenv("ADMIN_EMAIL")
ADMIN_PASSWORD = os.getenv("ADMIN_PASSWORD")

if not ADMIN_EMAIL or not ADMIN_PASSWORD:
    print("❌ ERROR: ADMIN_EMAIL or ADMIN_PASSWORD not found in .env file.")
    exit(1)

TOKEN_CACHE = {}

def get_auth_headers(email, password="Password123!"):
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
    except Exception as e:
        print(f"  ❌ Login error for {email}: {e}")
    return None

def get_all_users(headers):
    response = requests.get(f"{BACKEND_URL}/api/users", headers=headers)
    if response.status_code == 200:
        return response.json()
    return []

def get_all_projects(headers):
    response = requests.get(f"{BACKEND_URL}/api/projects", headers=headers)
    if response.status_code == 200:
        return response.json()
    return []

def get_project_tasks(project_id, headers):
    response = requests.get(f"{BACKEND_URL}/api/tasks/{project_id}", headers=headers)
    if response.status_code == 200:
        return response.json()
    return []

def fund_task(enterprise_email, task_id, amount):
    headers = get_auth_headers(enterprise_email)
    if not headers:
        return False
    
    url = f"{BACKEND_URL}/api/tasks/{task_id}/fund"
    try:
        response = requests.post(url, json={"taskId": task_id, "amount": amount}, headers=headers)
        if response.status_code in [200, 201]:
            return True
        else:
            print(f"    ⚠️ Failed to fund task {task_id} as {enterprise_email}: {response.text}")
    except Exception as e:
        pass
    return False

# ==========================================
# MAIN SCRIPT
# ==========================================
def main():
    print("🚀 Starting Enterprise Funding Mocking (Anomaly Injection)...")
    
    admin_headers = get_auth_headers(ADMIN_EMAIL, ADMIN_PASSWORD)
    if not admin_headers:
        print("🛑 Admin authentication failed.")
        return

    users = get_all_users(admin_headers)
    projects = get_all_projects(admin_headers)

    enterprises = [u for u in users if u.get("userType") == "ENTERPRISE"]
    developers = [u for u in users if u.get("userType") == "DEVELOPER"]

    if len(enterprises) < 2:
        print("🛑 Not enough enterprises to create a cross-enterprise anomaly. Need at least 2.")
        return
    
    print(f"👥 Found {len(enterprises)} Enterprises and {len(developers)} Developers.")
    print(f"📁 Found {len(projects)} Projects.")

    # 1. CROSS-ENTERPRISE ANOMALY INJECTION
    print("\n💉 Injecting Cross-Enterprise Anomaly (Money Laundering pattern)...")
    ent_a = enterprises[0]
    ent_b = enterprises[1]
    
    # Find a project created by Enterprise A
    proj_a = next((p for p in projects if p.get("creatorId") == ent_a.get("id")), None)
    # Find a project created by Enterprise B
    proj_b = next((p for p in projects if p.get("creatorId") == ent_b.get("id")), None)

    if proj_a and proj_b:
        tasks_a = get_project_tasks(proj_a.get("id"), admin_headers)
        tasks_b = get_project_tasks(proj_b.get("id"), admin_headers)

        if tasks_a and tasks_b:
            task_a = tasks_a[0].get("id")
            task_b = tasks_b[0].get("id")
            
            # Enterprise A funds Task B (Project B)
            print(f"  💸 Enterprise A ({ent_a['username']}) funding Task {task_b} from Project B...")
            fund_task(ent_a["email"], task_b, 5000)
            
            # Enterprise B funds Task A (Project A)
            print(f"  💸 Enterprise B ({ent_b['username']}) funding Task {task_a} from Project A...")
            fund_task(ent_b["email"], task_a, 5000)
            print("  ✅ Cross-Enterprise Anomaly injected successfully!")
        else:
            print("  ⚠️ Projects don't have tasks to fund. Skipping anomaly.")
    else:
        print("  ⚠️ Could not find projects created by the selected enterprises. Skipping anomaly.")


    # 2. SHELL PROJECT ANOMALY INJECTION
    print("\n💉 Injecting Shell Project Anomaly (Dev-Enterprise fraud pattern)...")
    ent_c = enterprises[0]
    
    # Find a project created by a developer
    proj_dev = next((p for p in projects if next((d for d in developers if d.get("id") == p.get("creatorId")), None)), None)
    
    if proj_dev:
        dev_id = proj_dev.get("creatorId")
        dev = next((d for d in developers if d.get("id") == dev_id), None)
        
        if dev:
            tasks_dev = get_project_tasks(proj_dev.get("id"), admin_headers)
            if tasks_dev:
                task_dev = tasks_dev[0].get("id")
                # Enterprise funds the task. Since the Dev (owner) already accepted the task in import.py (which creates WORK_ON),
                # this funding completes the Shell Project pattern: Dev created project -> Dev works on task -> Ent funds task
                print(f"  💸 Enterprise C ({ent_c['username']}) funding Task {task_dev} worked by {dev['username']}...")
                fund_task(ent_c["email"], task_dev, 10000)
                print("  ✅ Shell Project Anomaly injected successfully!")
            else:
                print("  ⚠️ Dev project has no tasks.")
    else:
        print("  ⚠️ Could not find a project created by a Developer.")


    # 3. RANDOM FUNDING TO POPULATE GRAPH
    print("\n🎲 Adding random valid funding for other tasks...")
    funding_count = 0
    for ent in enterprises:
        ent_email = ent["email"]
        for p in random.sample(projects, min(3, len(projects))):
            # Don't fund own projects to avoid noise
            if p.get("creatorId") == ent.get("id"):
                continue
                
            tasks = get_project_tasks(p.get("id"), admin_headers)
            if tasks:
                t_id = random.choice(tasks).get("id")
                amount = random.randint(100, 2000)
                if fund_task(ent_email, t_id, amount):
                    funding_count += 1
            time.sleep(0.1)

    print(f"  ✅ Added {funding_count} random funding relations.")
    print("\n🎉 MOCKING COMPLETED!")

if __name__ == "__main__":
    main()
