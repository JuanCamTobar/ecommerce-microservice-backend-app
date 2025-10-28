FROM python:3.11-slim
WORKDIR /loadtest

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY locustfile.py ./

ENV LOCUSTFILE=locustfile.py
EXPOSE 8089

ENTRYPOINT ["locust"]
CMD ["-f", "locustfile.py", "--host", "http://api-gateway-container:8080", "--web-host", "0.0.0.0", "--web-port", "8089"]
