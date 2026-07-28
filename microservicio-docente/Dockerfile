FROM python:3.12-slim
WORKDIR /app
ENV PYTHONUNBUFFERED=1
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
EXPOSE 8000 9091
CMD ["sh", "-c", "python manage.py runserver 0.0.0.0:8000 & python docentes/grpc_services/server.py"]
