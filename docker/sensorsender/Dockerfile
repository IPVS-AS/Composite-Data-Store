FROM python:2.7-alpine
WORKDIR /code
RUN pip install pika
RUN pip install redis
RUN pip install names
COPY . .
ENTRYPOINT ["python", "./sensorsender.py", "docker"]