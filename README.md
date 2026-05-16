# Online Course Store (MVC Project)

Spring Boot приложение для управления курсами с PostgreSQL.

## Технологии

- Java 17
- Spring Boot
- PostgreSQL
- Docker
- OpenShift

## Локальный запуск

```bash
git clone https://github.com/sorokin1989/diplom-project.git
cd diplom-project
mvn clean package
mvn spring-boot:run

Деплой в OpenShift
1. Сборка образа
docker login
docker build -t sorokin1989/diplom-project:latest .
docker push sorokin1989/diplom-project:latest
2. Развертывание
oc login --token=ВАШ_ТОКЕН --server=https://api.rm2.thpm.p1.openshiftapps.com:6443
oc project sorokin1989-dev
oc import-image diplom-project:latest --from=docker.io/sorokin1989/diplom-project:latest --confirm
oc new-app --name=diplom-project --image-stream=diplom-project:latest
oc set env deployment/diplom-project \
  SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-diplom:5432/diplom_db \
  SPRING_DATASOURCE_USERNAME=diplom_user \
  SPRING_DATASOURCE_PASSWORD=diplom_password \
  SPRING_JPA_HIBERNATE_DDL_AUTO=update
oc set triggers deployment/diplom-project --from-image=diplom-project:latest --containers=diplom-project
oc expose service diplom-project
oc get pods
oc get route
3. База данных
oc new-app --name=postgres-diplom \
  -e POSTGRESQL_USER=diplom_user \
  -e POSTGRESQL_PASSWORD=diplom_password \
  -e POSTGRESQL_DATABASE=diplom_db \
  postgresql:latest

Обновление приложения
docker build -t sorokin1989/diplom-project:latest .
docker push sorokin1989/diplom-project:latest
oc import-image diplom-project:latest --confirm
Как предотвратить засыпание приложения
В бесплатной версии OpenShift приложение засыпает через 24 часа без запросов.

Решение: UptimeRobot
Зарегистрируйтесь на https://uptimerobot.com

Add New Monitor → HTTP(S)

URL = ваш адрес (команда ниже)

Интервал = 5 минут
oc get route diplom-project -o jsonpath='{.spec.host}'
Если приложение уснуло
curl -k https://$(oc get route diplom-project -o jsonpath='{.spec.host}')
Команды для проверки
oc get pods
oc logs deployment/diplom-project --tail=50
oc rollout restart deployment/diplom-project
oc exec -it deployment/postgres-diplom -- psql -U diplom_user -d diplom_db
Таблица courses(создать отдельно в OpenShift)
CREATE TABLE IF NOT EXISTS courses (
    id BIGSERIAL PRIMARY KEY,
    author VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    description TEXT,
    is_active BOOLEAN,
    materials_path VARCHAR(255),
    price NUMERIC(10,2),
    review_count INTEGER,
    title VARCHAR(255) NOT NULL,
    category_id BIGINT
);

## Что нужно заменить перед сохранением:

| Найти | Заменить на |
|-------|-------------|
| `ВАШ_ЛОГИН` | Ваш логин на Docker Hub |
| `ВАШ_РЕПОЗИТОРИЙ` | Название репозитория |
| `ВАШ_ТОКЕН` | Ваш токен OpenShift |
| `ВАШ_КЛАСТЕР` | Адрес кластера |

## Как сохранить на GitHub:

1. Открыть репозиторий
2. Нажать `Add file` → `Create new file`
3. В поле имени ввести `README.md`
4. **Скопировать ВЕСЬ текст выше** и вставить
5. Нажать `Commit changes` (зелёная кнопка внизу)
