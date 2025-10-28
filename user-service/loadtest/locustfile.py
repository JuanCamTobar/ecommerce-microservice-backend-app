from locust import HttpUser, task, between, events
import random
import string
import json
import time


def rand_username():
    return "user_" + ''.join(random.choices(string.ascii_lowercase + string.digits, k=8))

def rand_name():
    return ''.join(random.choices(string.ascii_letters, k=6))


class UserBehavior(HttpUser):
    wait_time = between(0.5, 2.0)

    def on_start(self):
        self.my_users = []

    @task(15)
    def list_users(self):
        with self.client.get('/api/users', catch_response=True, name="GET /api/users") as resp:
            if resp.status_code == 200:
                try:
                    data = resp.json()
                    # expects the API to return a DTO collection with 'collection' property
                    if isinstance(data, dict) and 'collection' in data:
                        pass
                except Exception:
                    pass

    @task(25)
    def create_user(self):
        username = rand_username()
        payload = {
            "firstName": rand_name(),
            "lastName": rand_name(),
            "email": f"{username}@example.com",
            "phone": "+1-555-0123",
            "imageUrl": "https://example.com/avatar.png",
            "credential": {
                "username": username,
                "password": "LocustPass!23"
            }
        }
        with self.client.post('/api/users', json=payload, catch_response=True, name="POST /api/users") as resp:
            if resp.status_code == 200 or resp.status_code == 201:
                try:
                    u = resp.json()
                    if u and 'userId' in u:
                        self.my_users.append(u['userId'])
                except Exception:
                    pass
            else:
                resp.failure(f"Unexpected status: {resp.status_code}")

    @task(20)
    def get_user_by_id(self):
        if not self.my_users:
            r = self.client.get('/api/users')
            try:
                data = r.json()
                coll = data.get('collection', []) if isinstance(data, dict) else []
                if coll:
                    uid = coll[0].get('userId')
                    if uid:
                        self.client.get(f'/api/users/{uid}', name="GET /api/users/:id")
            except Exception:
                pass
            return

        user_id = random.choice(self.my_users)
        self.client.get(f'/api/users/{user_id}', name="GET /api/users/:id")

    @task(10)
    def find_by_username(self):
        if not self.my_users:
            return
        r = self.client.get('/api/users')
        try:
            data = r.json()
            coll = data.get('collection', []) if isinstance(data, dict) else []
            if coll:
                candidate = random.choice(coll)
                cred = candidate.get('credential') or candidate.get('credentialDto')
                if cred and 'username' in cred:
                    self.client.get(f"/api/users/username/{cred['username']}", name="GET /api/users/username/:username")
        except Exception:
            pass

    @task(10)
    def update_user(self):
        if not self.my_users:
            return
        user_id = random.choice(self.my_users)
        payload = {
            "userId": user_id,
            "firstName": "Updated_" + rand_name(),
            "lastName": rand_name(),
            "email": f"updated_{user_id}@example.com",
            "phone": "+1-999-9999",
        }
        with self.client.put(f'/api/users/{user_id}', json=payload, catch_response=True, name="PUT /api/users/:id") as resp:
            if not (200 <= resp.status_code < 300):
                resp.failure(f"Unexpected status: {resp.status_code}")

    @task(5)
    def delete_user(self):
        if not self.my_users:
            return
        user_id = self.my_users.pop(0)
        with self.client.delete(f'/api/users/{user_id}', catch_response=True, name="DELETE /api/users/:id") as resp:
            if not (200 <= resp.status_code < 300):
                resp.failure(f"Unexpected status: {resp.status_code}")


@events.quitting.add_listener
def _(environment, **kw):
    print("Quitting. Summary:")
    if environment.stats.total:
        print(environment.stats.total)
