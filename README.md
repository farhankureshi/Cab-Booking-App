# 🚖 CabGo – Cab Booking Application (Java / Spring Boot)

Full-stack ride-hailing app following the "Cab Booking Application – Java Tech Stack" document.

## Run it (2 minutes)
Requirements: **Java 17+** and **Maven 3.9+**

```bash
mvn spring-boot:run
```
Open **http://localhost:8080**. Uses an embedded H2 file database, so nothing else is needed.

Demo accounts (created on first start):

| Role   | Email           | Password  |
|--------|-----------------|-----------|
| Rider  | rider@cab.com   | rider123  |
| Driver | driver@cab.com  | driver123 |
| Admin  | admin@cab.com   | admin123  |

Promo codes: `WELCOME20` (20% off), `SAVE10` (10% off).

### Try the full flow (use two browser windows: one normal, one incognito)
1. **Driver** window: log in → click **Offline** to go **Online** (tap the map to set your position).
2. **Rider** window: log in → tap map for pickup, tap again for drop → see fare/ETA → **Request ride**.
3. Driver sees the request live (WebSocket) → **Accept** → **Simulate driving** (rider sees the car move live) → *I've arrived* → *Start ride* → *Simulate driving* → *Complete ride*.
4. Rider → **Pay** (demo mode) → rate the driver → download the **PDF receipt**.
5. **Admin** window: stats, revenue, driver ranking, live SOS alerts, support tickets.

## What maps to the PDF
| PDF section | Implementation |
|---|---|
| Spring Boot, Java 17, Maven | `pom.xml` |
| Spring Security + JWT, BCrypt, role-based access | `config/SecurityConfig`, `JwtFilter`, `service/JwtService`, `@PreAuthorize` |
| Rider / driver registration | `web/AuthController` |
| Ride booking + fare calculation | `service/RideService`, `FareService` (base + per km, min fare, promo codes) |
| Nearby drivers notified | `/topic/drivers` broadcast + 15 km filter in `RideService.open` |
| Driver dashboard (requests, accept/reject, earnings, history, ratings) | `web/DriverController`, frontend driver view |
| Maps: location select, route plot, ETA, live tracking | Leaflet + OpenStreetMap + OSRM routing; haversine distance for fare/ETA; live driver marker |
| WebSockets (`@MessageMapping`, `@SendTo`) | `web/RealtimeController` (location + SOS), `config/WebSocketConfig`, `SimpMessagingTemplate` status pushes |
| Stripe Java SDK: intent → confirm → webhook → receipt | `service/PaymentService`, `web/PaymentController` |
| Ratings, average, driver ranking | `RideService.rate`, admin "Driver ranking" |
| Ride history + PDF invoice | `service/ReceiptService` (OpenPDF, the maintained iText fork) |
| DB design: users, drivers, rides, payments, ratings, promo_codes, support_tickets | `model/*` (JPA/Hibernate) |
| PostgreSQL | `application-postgres.yml` |
| Docker, Nginx, CI/CD (GitHub Actions) | `Dockerfile`, `docker-compose.yml`, `nginx.conf`, `.github/workflows/ci.yml` |

## Stripe (optional)
Without keys the app runs payments in **demo mode**. For real card payments:
```bash
export STRIPE_SECRET_KEY=sk_test_...
export STRIPE_PUBLISHABLE_KEY=pk_test_...
export STRIPE_WEBHOOK_SECRET=whsec_...      # stripe listen --forward-to localhost:8080/api/payments/webhook
mvn spring-boot:run
```
Payment is verified server-side (webhook and PaymentIntent retrieve) before a ride is marked paid.

## PostgreSQL + Docker
```bash
docker compose up --build     # app + postgres + nginx on http://localhost
```
Or run locally against Postgres: `SPRING_PROFILES_ACTIVE=postgres DB_URL=... mvn spring-boot:run`.

## REST API
| Method | Path | Role |
|---|---|---|
| POST | `/api/auth/register`, `/api/auth/login` | public |
| POST | `/api/rides/estimate` | any |
| POST | `/api/rides` | RIDER |
| GET  | `/api/rides/active`, `/api/rides/my` | any |
| GET  | `/api/rides/open` | DRIVER |
| POST | `/api/rides/{id}/accept`, `/api/rides/{id}/status?to=ARRIVED\|STARTED\|COMPLETED` | DRIVER |
| POST | `/api/rides/{id}/cancel`, `/api/rides/{id}/rating` | RIDER |
| GET  | `/api/rides/{id}/receipt` (PDF) | ride participants / ADMIN |
| POST | `/api/payments/{rideId}/intent`, `/api/payments/{rideId}/confirm` | RIDER |
| POST | `/api/payments/webhook` | Stripe |
| PUT  | `/api/driver/status`, `/api/driver/location`; GET `/api/driver/dashboard` | DRIVER |
| GET/POST | `/api/support` | any |
| GET  | `/api/admin/stats` | ADMIN |

WebSocket: STOMP at `/ws`. Topics `/topic/ride/{id}`, `/topic/drivers`, `/topic/sos`; send to `/app/ride/{id}/location`, `/app/sos`.

## Not included (listed in the PDF as optional/extra)
Google OAuth2 social login, Redis caching, Kafka, e-mailing receipts (JavaMailSender), React build. The frontend is a single static page served by Spring Boot so there is no Node build step.

## Production notes
Change `JWT_SECRET`; restrict WebSocket origins (`WebSocketConfig`); add STOMP channel authentication before real use.
