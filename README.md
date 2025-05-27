# **Tilli Backend Server**

[![Coverage Status](https://coveralls.io/repos/github/tillioss/tilli-backend-server/badge.svg?branch=master)](https://coveralls.io/github/tillioss/tilli-backend-server?branch=master)

Tilli is an open-source web-based application focused on game-based social emotional learning experiences for kids. The backend is built with Scala, uses Truffle for compilation, and Redis for database management.

## Table of Contents

- [Community](#joining-the-tilli-community)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Running Locally](#running-locally)
- [Contributing](#contribution-guidelines)
- [Support](#support)

## Prerequisites

Before you begin, ensure you have the following installed:

- Redis (Version 8.x recommended)
- SBT (Scala Build Tool)
- Java Development Kit (JDK)

## Installation

### Redis Setup

1. **Install Redis Version 8.x**

   - For macOS: `brew install redis@8`
   - For Linux: Follow [Redis installation guide](https://redis.io/docs/getting-started/installation/)

2. **Stop existing Redis server (if running)**

   ```bash
   # On Linux
   sudo /etc/init.d/redis-server stop

   # On macOS
   redis-cli shutdown
   ```

3. **Backup existing data (optional)**

   ```bash
   cd /var/lib/redis/
   cp dump.rdb dump_backup.rdb
   ```

4. **Replace Redis data**

   ```bash
   cp /path/to/tilli-backend-server/data/dump.rdb /var/lib/redis/
   ```

5. **Start Redis server**

   ```bash
   # On Linux
   sudo /etc/init.d/redis-server start

   # On macOS
   brew services start redis
   ```

## Running Locally

Run the server using SBT with the following command structure:

```bash
sbt "runMain com.teqbahn.bootstrap.StarterMain local 2553 8093 <ServerIP> <RedisIP>:<RedisPort> <MailId> <MailPassword> <filepath>"
```

### Parameters Explained:

- `local`: Server running environment
- `2553`: Pekko Port
- `8093`: HTTP Port
- `<ServerIP>`: Your system IP (e.g., 192.0.0.1)
- `<RedisIP>`: Redis IP (default: 127.0.0.1)
- `<RedisPort>`: Redis Port (default: 6379)
- `<MailId>`: Email for notifications
- `<MailPassword>`: Email password
- `<filepath>`: System filepath for storage

Example:

```bash
sbt "runMain com.teqbahn.bootstrap.StarterMain local 2553 8093 192.1.0.1 127.0.0.1:6379 your.email@example.com yourpassword /path/to/storage"
```

## Joining the Tilli Community

We are a vibrant community of learning designers, game designers, developers, and educators passionate about creating innovative social emotional learning experiences for kids. Our goal is to develop inclusive, accountable, and accessible technology services for education.

## Contribution Guidelines

We welcome contributions! Please read our [Contribution Guidelines](https://tillioss.github.io/DEPRECATED-tilli-docs/docs/Contribution-Guidelines) before getting started.

## Code of Conduct

Our community is governed by our [Code of Conduct](https://tillioss.github.io/DEPRECATED-tilli-docs/docs/code-of-conduct). Please review it before participating.

## Support

- **Documentation**: Check our [User and Developer Documentation](https://tillioss.github.io/DEPRECATED-tilli-docs/docs/getting-started-developer#installing-tilli-server)
- **Video Tutorial**: Watch our [setup guide](https://drive.google.com/file/d/1DftDb_z109lvuRV8l0URmPbI6XOAVwEt/view?usp=sharing)
- **Community**: Join our [Slack Community](https://join.slack.com/t/tilliopensour-wyp9205/shared_invite/zt-206f4f11s-HoII8Kob45f6WK3GPIIi6g) for support and collaboration

## License

This project is open source. Please see the LICENSE file for details.
