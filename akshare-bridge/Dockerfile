FROM python:3.11-slim

WORKDIR /app
COPY requirements.txt /app/requirements.txt
RUN pip install --no-cache-dir --default-timeout=180 -i https://pypi.tuna.tsinghua.edu.cn/simple -r /app/requirements.txt
COPY app.py /app/app.py

EXPOSE 5005
CMD ["python", "/app/app.py"]
