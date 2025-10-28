from locust import HttpUser, task, between
import random
import datetime
import json

DATE_FMT = "%d-%m-%Y__%H:%M:%S:%f"


class FavouriteUser(HttpUser):
    """Simple Locust user that exercises the Favourite service.

    - GET /api/favourites (list)
    - POST /api/favourites (create)
    """

    wait_time = between(1, 3)

    @task(7)
    def list_favourites(self):
        self.client.get("/api/favourites", name="GET /api/favourites")

    @task(3)
    def create_favourite(self):
        user_id = random.randint(1, 100)
        product_id = random.randint(1, 100)
        like_date = datetime.datetime.utcnow().strftime(DATE_FMT)
        payload = {
            "userId": user_id,
            "productId": product_id,
            "likeDate": like_date
        }
        headers = {"Content-Type": "application/json"}
        self.client.post(
            "/api/favourites",
            data=json.dumps(payload),
            headers=headers,
            name="POST /api/favourites",
        )
