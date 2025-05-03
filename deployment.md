 # Deployment Guide for GCP VM Instance

 This guide describes how to deploy the **Tilli Backend Server** on a Google Cloud Platform (GCP) Compute Engine virtual machine (VM) instance.

 ## Prerequisites

 - A GCP account with billing enabled.
 - [Google Cloud SDK (gcloud CLI)](https://cloud.google.com/sdk/docs/install) installed and authenticated.
 - An SSH key associated with your GCP project for VM access.
 - **Redis**: either install on the VM or use [Cloud Memorystore for Redis].

 ## 1. Provision a VM Instance

 You can create a VM using the `gcloud` CLI or the GCP Console. Example CLI command:
 ```bash
 gcloud compute instances create tilli-server-vm \
   --zone=us-central1-a \
   --machine-type=e2-medium \
   --image-family=ubuntu-22-04 \
   --image-project=ubuntu-os-cloud \
   --boot-disk-size=50GB \
   --tags=http-server,https-server \
   --metadata=startup-script='#! /bin/bash
     apt-get update
     apt-get install -y openjdk-8-jdk git unzip'
 ```

 ## 2. Configure Firewall Rules

 Allow traffic on the application ports (default: 8093 for HTTP, 2553 for Pekko clustering):
 ```bash
 gcloud compute firewall-rules create allow-tilli-ports \
   --allow tcp:8093,tcp:2553 \
   --target-tags=http-server,https-server \
   --description="Allow Tilli application ports"
 ```

 ## 3. SSH into the VM

 ```bash
 gcloud compute ssh tilli-server-vm --zone us-central1-a
 ```

 ## 4. Install Dependencies

 ```bash
 sudo apt update && sudo apt upgrade -y
 sudo apt install -y openjdk-8-jdk git sbt redis-server
 ```

 > **Note:** If using Cloud Memorystore, skip the `redis-server` installation and use the Memorystore IP and network.

 ## 5. Clone the Repository and Build

 ```bash
 cd ~
 git clone https://github.com/tillioss/tilli-backend-server.git
 cd tilli-backend-server
 sbt assembly
 ```

 The assembly (fat JAR) will be located in:
 ```text
 target/scala-2.12/tilli-backend-server-*-assembly.jar
 ```

 ## 6. Configure and Start as a Service

 Create a systemd service file `/etc/systemd/system/tilli.service` with the following content:
 ```ini
 [Unit]
 Description=Tilli Backend Server
 After=network.target

 [Service]
 # Replace <USERNAME> with the VM user account (e.g., ubuntu)
 User=<USERNAME>
 WorkingDirectory=/home/<USERNAME>/tilli-backend-server
 ExecStart=/usr/bin/java -Xms512M -Xmx1024M -jar \
   /home/<USERNAME>/tilli-backend-server/target/scala-2.12/tilli-backend-server-*-assembly.jar \
   local 2553 8093 <SERVER_IP> <REDIS_HOST:PORT> <MAIL_ID> <MAIL_PASSWORD> <FILE_PATH>
 Restart=always
 SuccessExitStatus=143
 StandardOutput=syslog
 StandardError=syslog
 SyslogIdentifier=tilli

 [Install]
 WantedBy=multi-user.target
 ```

 Adjust the placeholders:
 - `<SERVER_IP>`: IP address of this VM (or `0.0.0.0` to bind all interfaces).
 - `<REDIS_HOST:PORT>`: `localhost:6379` (if local) or Memorystore endpoint.
 - `<MAIL_ID>`, `<MAIL_PASSWORD>`: SMTP credentials or dummy values.
 - `<FILE_PATH>`: path for file operations (e.g., `/var/www/html/tilli`).

 Enable and start the service:
 ```bash
 sudo systemctl daemon-reload
 sudo systemctl start tilli
 sudo systemctl enable tilli
 ```

 ## 7. Verify and Access

 Follow logs:
 ```bash
 sudo journalctl -u tilli -f
 ```

 Open your browser to `http://<EXTERNAL_VM_IP>:8093` to confirm the application is running.

 ## 8. (Optional) Domain and SSL

 To use a custom domain and HTTPS, consider:
 - Setting up a DNS A record pointing to `<EXTERNAL_VM_IP>`.
 - Installing Certbot and obtaining a Let's Encrypt TLS certificate.
 - Configuring a reverse proxy (e.g., Nginx) to handle SSL termination.

 ## References
 - GCP Compute Engine: https://cloud.google.com/compute/docs
 - Cloud Memorystore for Redis: https://cloud.google.com/memorystore/docs/redis
 - sbt assembly plugin: https://github.com/sbt/sbt-assembly