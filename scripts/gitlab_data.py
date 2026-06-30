import os
import requests
from datetime import datetime
from dateutil.relativedelta import relativedelta
import time
import json
import urllib.parse
from dotenv import load_dotenv

load_dotenv()

GITLAB_TOKEN = os.getenv("GITLAB_TOKEN")
GITLAB_API_URL = os.getenv("GITLAB_API_URL", "https://gitlab.com/api/v4")

REPOS = [
    "gitlab-org/gitlab",
    "gitlab-org/gitlab-runner",
    "gitlab-org/gitaly",
    "gitlab-org/gitlab-shell",
    "gitlab-org/omnibus-gitlab",
    "inkscape/inkscape",
    "fdroid/fdroidclient",
    "fdroid/fdroidserver",
    "videolan/vlc",
    "gnome/gimp"
]

TWO_YEARS_AGO = (datetime.now() - relativedelta(years=2)).isoformat() + "Z"

HEADERS = {
    "PRIVATE-TOKEN": GITLAB_TOKEN
} if GITLAB_TOKEN else {}

if not GITLAB_TOKEN:
    print("⚠️ WARNING: GITLAB_TOKEN not found in .env file. GitLab API may restrict access or trigger rate limits!")

def clean_username(name, email):
    if not name and not email:
        return "unknown_user"
    if email:
        username = email.split("@")[0]
        # remove unwanted characters
        username = "".join(c for c in username if c.isalnum() or c in "._-")
        if username:
            return username
    # fallback to clean name
    username = "".join(c for c in name if c.isalnum() or c in "._-")
    return username.lower() if username else "unknown_user"

def get_repo_details(encoded_path):
    url = f"{GITLAB_API_URL}/projects/{encoded_path}"
    response = requests.get(url, headers=HEADERS)
    if response.status_code == 200:
        data = response.json()
        topics = data.get("topics", [])
        if not topics:
            topics = data.get("tag_list", [])
        return topics
    return []

def get_mr_commits(encoded_path, mr_iid):
    url = f"{GITLAB_API_URL}/projects/{encoded_path}/merge_requests/{mr_iid}/commits"
    response = requests.get(url, headers=HEADERS, params={"per_page": 100})
    
    commits_list = []
    if response.status_code == 200:
        for c in response.json():
            author_name = c.get("author_name") or ""
            author_email = c.get("author_email") or ""
            commit_data = {
                "hash": c.get("id"),
                "message": c.get("message") or c.get("title"),
                "date": c.get("created_at"),
                "authorUsername": clean_username(author_name, author_email)
            }
            commits_list.append(commit_data)
    return commits_list

def get_mr_comments(encoded_path, mr_iid):
    url = f"{GITLAB_API_URL}/projects/{encoded_path}/merge_requests/{mr_iid}/notes"
    response = requests.get(url, headers=HEADERS, params={"per_page": 100})
    
    comments_list = []
    if response.status_code == 200:
        for c in response.json():
            # Exclude system notes (like status changes, auto-actions)
            if c.get("system", False):
                continue
            author_info = c.get("author") or {}
            comment_data = {
                "commentId_github": c.get("id"), # Map to commentId_github for compatibility
                "content": c.get("body") or "",
                "authorUsername": author_info.get("username", "unknown_user"),
                "createdAt": c.get("created_at")
            }
            comments_list.append(comment_data)
    return comments_list

def extract_project_data(repo_fullname):
    encoded_path = urllib.parse.quote_plus(repo_fullname)
    print(f"\n🚀 Starting extraction for GitLab repository: {repo_fullname}")
    
    topics = get_repo_details(encoded_path)
    
    parts = repo_fullname.split("/")
    owner = parts[0]
    repo = parts[1] if len(parts) > 1 else repo_fullname
    
    project_data = {
        "projectName": repo,
        "owner": owner,
        "tags": topics,
        "tasks": []
    }
    
    # Fetch Merge Requests
    url = f"{GITLAB_API_URL}/projects/{encoded_path}/merge_requests"
    params = {
        "state": "all", # We will filter merged/closed MRs in Python
        "order_by": "updated_at",
        "sort": "desc",
        "per_page": 30
    }
    
    response = requests.get(url, headers=HEADERS, params=params)
    if response.status_code != 200:
        print(f"❌ ERROR fetching MRs for {repo_fullname}: {response.status_code}")
        return None
        
    merge_requests = response.json()
    
    for mr in merge_requests:
        state = mr.get("state")
        if state not in ["merged", "closed"]:
            continue
            
        updated_at = mr.get("updated_at")
        if not updated_at or updated_at < TWO_YEARS_AGO:
            continue
            
        mr_iid = mr.get("iid")
        author_info = mr.get("author") or {}
        
        print(f"  -> Processing MR #{mr_iid}: {mr.get('title')[:50]}...")
        
        commits = get_mr_commits(encoded_path, mr_iid)
        comments = get_mr_comments(encoded_path, mr_iid)
        
        contributors_set = set([c["authorUsername"] for c in commits if c["authorUsername"] != "unknown_user"])
        for comment in comments:
            if comment["authorUsername"] != "unknown_user":
                contributors_set.add(comment["authorUsername"])
        if author_info.get("username"):
            contributors_set.add(author_info.get("username"))
            
        # For compatibility with importV2.py which looks for task.get("merged_at")
        merged_at = mr.get("merged_at") if state == "merged" else None
        
        task_data = {
            "githubId": mr.get("id"), # Map to githubId for compatibility
            "title": mr.get("title"),
            "description": mr.get("description") or "",
            "status": "COMPLETED" if state == "merged" else "CLOSED",
            "createdAt": mr.get("created_at"),
            "updatedAt": updated_at,
            "contributors": list(contributors_set),
            "commits": commits,
            "comments": comments,
            "merged_at": merged_at # Ensure merged_at is populated if merged
        }
        
        project_data["tasks"].append(task_data)
        time.sleep(0.3)
        
    return project_data

if __name__ == "__main__":
    all_extracted_data = []
    
    for repo in REPOS:
        data = extract_project_data(repo)
        if data and data["tasks"]:
            all_extracted_data.append(data)
            
        print(f"Finished processing {repo}. Throttling for 2 seconds...")
        time.sleep(2)
        
    output_filename = "dev2enterprise_dump_gitlab.json"
    with open(output_filename, "w", encoding="utf-8") as f:
        json.dump(all_extracted_data, f, indent=4, ensure_ascii=False)
        
    print(f"\n🎉 DATA EXTRACTION COMPLETED successfully! Output saved to: {output_filename}")
