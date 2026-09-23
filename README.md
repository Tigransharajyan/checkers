# Checkers Online

Online multiplayer checkers (Russian draughts) web application built with Spring Boot, Thymeleaf, WebSocket/STOMP.

## Tech Stack

- **Backend:** Java 17, Spring Boot 3.3.4
- **Auth:** JWT (stateless, access + refresh tokens)
- **DB (dev):** H2 in-memory (PostgreSQL-compatible mode)
- **DB (prod):** PostgreSQL
- **Realtime:** WebSocket/STOMP + SockJS
- **UI:** Thymeleaf + vanilla JS
- **i18n:** English / Russian / Armenian

---

## Local Development

### Requirements

- Java 17+
- Maven 3.9+ (or use the included `./mvnw` wrapper — no installation needed)
- No PostgreSQL needed locally (H2 in-memory is used automatically)

### Run

```bash
# Using Maven wrapper (recommended)
./mvnw spring-boot:run

# Or with system Maven
mvn spring-boot:run
```

App starts at **http://localhost:8080** with H2 dev profile.  
H2 console: **http://localhost:8080/h2-console** (JDBC URL: `jdbc:h2:mem:checkers`)

### Run Tests

```bash
./mvnw test
```

### Build JAR

```bash
./mvnw clean package -DskipTests
java -jar target/checkers-online-*.jar
```

---

## Docker

### Build

```bash
docker build -t checkers-online .
```

### Run (dev mode, H2)

```bash
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  checkers-online
```

### Run (prod mode, PostgreSQL)

```bash
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/checkers \
  -e JWT_SECRET=$(openssl rand -base64 64) \
  -e SITE_BASE_URL=http://localhost:8080 \
  checkers-online
```

### Test prod profile locally (without real PostgreSQL)

```bash
# Verify app starts without secrets leaking into wrong profile
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

---

## Deploy to Render.com

### Requirements

- GitHub account with this repo pushed
- Render.com account (free tier works)

### Environment Variables

| Variable | Required | Purpose | Secret |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | ✅ | Must be `prod` on Render | No |
| `SPRING_DATASOURCE_URL` | ✅ | PostgreSQL JDBC URL (injected by Render Blueprint) | Yes |
| `JWT_SECRET` | ✅ | JWT signing key, min 32 chars | Yes — auto-generated |
| `SITE_BASE_URL` | ✅ | Your Render app URL (e.g. `https://checkers.onrender.com`) | No |
| `ASSISTANT_LLM_API_KEY` | ❌ | Only if `app.assistant.provider=llm` | Yes |
| `PORT` | — | Injected by Render automatically, do not set manually | — |

### First Deploy — Step by Step

1. **Push to GitHub:**
   ```bash
   git init  # if not already a git repo
   git add .
   git commit -m "Prepare Render deployment"
   git remote add origin https://github.com/YOUR_USERNAME/checkers-online.git
   git push -u origin main
   ```

2. **Connect to Render using Blueprint:**
   - Go to [Render Dashboard](https://dashboard.render.com)
   - Click **New** → **Blueprint**
   - Select your GitHub repo
   - Render detects `render.yaml` automatically
   - Click **Apply**

   Render will create:
   - Web Service `checkers-online` (Docker, free plan)
   - PostgreSQL database `checkers-db` (free plan)
   - `SPRING_DATASOURCE_URL` linked automatically
   - `JWT_SECRET` generated randomly

3. **After first deploy — set SITE_BASE_URL:**
   - Go to your Web Service → **Environment**
   - Find `SITE_BASE_URL` (marked as `sync: false`)
   - Set it to your Render URL: `https://checkers-online-xxxx.onrender.com`
   - Click **Save Changes** → service redeploys automatically

4. **Optional — set LLM key:**
   - If you want the AI assistant, set `ASSISTANT_LLM_API_KEY`
   - Also change `app.assistant.provider=llm` in `application.yml`

### Subsequent Deploys

Every `git push` to `main` triggers automatic redeploy:

```bash
git add .
git commit -m "Your changes"
git push
```

### View Logs

```bash
# In Render Dashboard → your service → Logs tab
# Or use Render CLI:
render logs --service checkers-online --tail
```

### Health Check

The app uses the root path `/` as health check endpoint.
Render considers the service healthy when it returns HTTP 200.

To verify manually:
```bash
curl -I https://your-app.onrender.com/
# Expect: HTTP/2 200
```

### PostgreSQL Connection Info

- In Render Dashboard → `checkers-db` → **Info** tab
- Internal URL: used by the web service automatically
- External URL: for connecting with psql or a DB GUI

```bash
# Connect with psql (use External URL from Render)
psql "postgres://checkers:PASSWORD@HOST/checkers?sslmode=require"
```

### Database Schema

**No Flyway/Liquibase migrations.** The schema is managed by Hibernate:

| Profile | `ddl-auto` | Behavior |
|---|---|---|
| `dev` | `update` | Creates/updates schema automatically (H2) |
| `prod` | `update` | Creates/updates schema automatically (PostgreSQL) |

> ⚠️ **Note:** `ddl-auto=update` is safe for initial deploys and development but has limitations for complex migrations in production. If you add a column rename or complex schema change in the future, use Flyway migrations instead.

---

## Architecture Limitations

1. **Single instance only:** Matchmaking and presence tracking use in-memory state (`ConcurrentLinkedQueue`, `ConcurrentHashMap`). Multi-instance deployments require Redis for shared state.

2. **No file uploads:** The app has no filesystem persistence — ephemeral Render containers are fine.

3. **Free tier cold starts:** Render free tier spins down after 15 minutes of inactivity. First request after spin-down takes ~30 seconds. Upgrade to a paid plan to avoid this.

4. **WebSocket behind proxy:** Render's reverse proxy supports WebSocket upgrade. SockJS is used as fallback transport.

---

## Secrets Reference

Secrets that must **never** be committed to Git:

| Secret | How to generate |
|---|---|
| `JWT_SECRET` | `openssl rand -base64 64` (auto-generated by Render Blueprint) |
| `SPRING_DATASOURCE_URL` | Provided by Render PostgreSQL (auto-linked) |
| `ASSISTANT_LLM_API_KEY` | From your LLM provider dashboard |
