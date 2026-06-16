import os
import requests
from datetime import datetime
from dateutil.relativedelta import relativedelta
import time
import json
from dotenv import load_dotenv


load_dotenv()

GITHUB_TOKEN = os.getenv("GITHUB_TOKEN")

REPOS = [
    "tensorflow/tensorflow", "pytorch/pytorch", "torvalds/linux", "python/cpython",
    "rust-lang/rust", "microsoft/TypeScript", "microsoft/vscode", "golang/go",
    "numpy/numpy", "scikit-learn/scikit-learn", "openbsd/src", "freebsd/freebsd-src",
    "pandas-dev/pandas", "scipy/scipy", "tidyverse/ggplot2", "kubernetes/kubernetes",
    "postgres/postgres", "nodejs/node", "facebook/react", "angular/angular",
    "matplotlib/matplotlib", "apache/httpd", "nginx/nginx", "opencv/opencv",
    "ipython/ipython", "rstudio/rstudio", "jupyterlab/jupyterlab", "gcc-mirror/gcc",
    "apple/swift", "denoland/deno", "apache/spark", "llvm/llvm-project",
    "chromium/chromium", "v8/v8"
]

TWO_YEARS_AGO = (datetime.now() - relativedelta(years=2)).isoformat() + "Z"

HEADERS = {
    "Accept": "application/vnd.github.v3+json",
    "Authorization": f"token {GITHUB_TOKEN}" if GITHUB_TOKEN else ""
}

if not GITHUB_TOKEN:
    print("⚠️ WARNING: GITHUB_TOKEN not found in .env file. Rate limiting will block you almost immediately!")

def get_repo_topics(owner, repo):
    url = f"https://api.github.com/repos/{owner}/{repo}"
    response = requests.get(url, headers=HEADERS)
    if response.status_code == 200:
        return response.json().get("topics", [])
    return []

def get_pr_commits(owner, repo, pr_number):
    url = f"https://api.github.com/repos/{owner}/{repo}/pulls/{pr_number}/commits"
    response = requests.get(url, headers=HEADERS, params={"per_page": 100})

    commits_list = []
    if response.status_code == 200:
        for c in response.json():
            author_info = c.get("author") or {}
            commit_data = {
                "hash": c.get("sha"),
                "message": c.get("commit", {}).get("message"),
                "date": c.get("commit", {}).get("author", {}).get("date"),
                "authorUsername": author_info.get("login", "unknown_user")
            }
            commits_list.append(commit_data)
    return commits_list

def get_pr_comments(owner, repo, pr_number):
    url = f"https://api.github.com/repos/{owner}/{repo}/issues/{pr_number}/comments"
    response = requests.get(url, headers=HEADERS, params={"per_page": 100})

    comments_list = []
    if response.status_code == 200:
        for c in response.json():
            user_info = c.get("user") or {}
            comment_data = {
                "commentId_github": c.get("id"),
                "content": c.get("body") or "",
                "authorUsername": user_info.get("login", "unknown_user"),
                "createdAt": c.get("created_at")
            }
            comments_list.append(comment_data)
    return comments_list

def extract_project_data(repo_fullname):
    owner, repo = repo_fullname.split("/")
    print(f"\n🚀 Starting extraction for repository: {repo_fullname}")

    topics = get_repo_topics(owner, repo)

    project_data = {
        "projectName": repo,
        "owner": owner,
        "tags": topics,
        "tasks": []
    }

    url = f"https://api.github.com/repos/{owner}/{repo}/pulls"
    params = {
        "state": "closed",
        "sort": "updated",
        "direction": "desc",
        "per_page": 30
    }

    response = requests.get(url, headers=HEADERS, params=params)
    if response.status_code != 200:
        print(f"❌ ERROR fetching PRs for {repo_fullname}: {response.status_code}")
        return None

    pull_requests = response.json()

    for pr in pull_requests:
        closed_at = pr.get("closed_at")

        if not closed_at or closed_at < TWO_YEARS_AGO:
            continue

        pr_number = pr.get("number")
        user_info = pr.get("user") or {}

        print(f"  -> Processing PR #{pr_number}: {pr.get('title')[:50]}...")

        commits = get_pr_commits(owner, repo, pr_number)

        comments = get_pr_comments(owner, repo, pr_number)

        contributors_set = set([c["authorUsername"] for c in commits if c["authorUsername"] != "unknown_user"])
        for comment in comments:
            if comment["authorUsername"] != "unknown_user":
                contributors_set.add(comment["authorUsername"])
        if user_info.get("login"):
            contributors_set.add(user_info.get("login"))

        task_data = {
            "githubId": pr.get("id"),
            "title": pr.get("title"),
            "description": pr.get("body") or "",
            "status": "COMPLETED" if pr.get("merged_at") else "CLOSED",
            "createdAt": pr.get("created_at"),
            "updatedAt": closed_at,
            "contributors": list(contributors_set),
            "commits": commits,
            "comments": comments
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

    output_filename = "dev2enterprise_dump.json"
    with open(output_filename, "w", encoding="utf-8") as f:
        json.dump(all_extracted_data, f, indent=4, ensure_ascii=False)

    print(f"\n🎉 DATA EXTRACTION COMPLETED successfully! Output saved to: {output_filename}")