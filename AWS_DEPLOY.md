# Guía de Deployment en AWS — MusicMatch Backend

## Prerequisitos
- AWS CLI instalado y configurado (`aws configure`)
- Docker Desktop corriendo
- Cuenta AWS con permisos en ECS, ECR, RDS, VPC

---

## Paso 1 — Crear la base de datos en RDS

```bash
aws rds create-db-instance \
  --db-instance-identifier musicmatch-db \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 15 \
  --master-username musicmatch \
  --master-user-password TuPasswordSeguro123 \
  --allocated-storage 20 \
  --publicly-accessible \
  --db-name musicmatch \
  --region us-east-1
```

Espera ~5 min. Obtén el endpoint:
```bash
aws rds describe-db-instances \
  --db-instance-identifier musicmatch-db \
  --query 'DBInstances[0].Endpoint.Address' --output text
```

---

## Paso 2 — Crear repositorio en ECR y subir imagen

```bash
# Crear repositorio
aws ecr create-repository --repository-name musicmatch --region us-east-1

# Login a ECR (reemplaza 123456789 con tu AWS Account ID)
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS \
  --password-stdin 123456789.dkr.ecr.us-east-1.amazonaws.com

# Build y push
docker build -t musicmatch .
docker tag musicmatch:latest 123456789.dkr.ecr.us-east-1.amazonaws.com/musicmatch:latest
docker push 123456789.dkr.ecr.us-east-1.amazonaws.com/musicmatch:latest
```

---

## Paso 3 — Crear cluster ECS y task definition

```bash
# Crear cluster
aws ecs create-cluster --cluster-name musicmatch-cluster --region us-east-1
```

En la consola AWS → ECS → Task Definitions → Create new:
- Launch type: FARGATE
- CPU: 0.5 vCPU, Memory: 1 GB
- Container: imagen del ECR
- Port: 8080
- Variables de entorno (desde AWS Secrets Manager o directamente):
  - `DB_USERNAME` = musicmatch
  - `DB_PASSWORD` = TuPasswordSeguro123
  - `SPRING_DATASOURCE_URL` = jdbc:postgresql://[RDS_ENDPOINT]:5432/musicmatch
  - `JWT_SECRET` = tu-secret-key
  - `SPOTIFY_CLIENT_ID` = tu-client-id
  - `SPOTIFY_CLIENT_SECRET` = tu-client-secret
  - `MAIL_PASSWORD` = tu-resend-key
  - `CORS_ORIGINS` = http://tu-frontend.com

---

## Paso 4 — Crear servicio ECS

En la consola AWS → ECS → tu cluster → Create Service:
- Launch type: FARGATE
- Task definition: la que creaste
- Service name: musicmatch-service
- Desired tasks: 1
- VPC: default, subnets públicas
- Security group: abre el puerto 8080 al mundo (0.0.0.0/0)
- Load balancer: opcional para producción real

---

## Paso 5 — Verificar

```bash
# Obtener IP pública de la tarea corriendo
aws ecs list-tasks --cluster musicmatch-cluster
aws ecs describe-tasks --cluster musicmatch-cluster --tasks [TASK_ARN] \
  --query 'tasks[0].attachments[0].details'

# Probar
curl http://[IP_PUBLICA]:8080/actuator/health
curl -X POST http://[IP_PUBLICA]:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","email":"test@test.com","password":"Password1"}'
```

---

## Security Groups requeridos

| Tipo | Protocolo | Puerto | Origen |
|------|-----------|--------|--------|
| Custom TCP | TCP | 8080 | 0.0.0.0/0 |
| PostgreSQL | TCP | 5432 | Security group del ECS |

---

## Costo estimado
- RDS db.t3.micro: ~$15/mes
- ECS Fargate (0.5 vCPU / 1GB): ~$15/mes
- ECR storage: <$1/mes
- **Total: ~$30/mes** (o gratis con Free Tier en el primer año)
