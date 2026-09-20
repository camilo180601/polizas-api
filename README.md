# API de pólizas

Implementación del Módulo 2 de la prueba técnica. Proyecto Java 21, Spring Boot 4.1.1, Maven 3.9.16 (fijado por Maven Wrapper), Spring MVC, JPA y H2 en memoria. La versión de Spring Boot es [estable y compatible con Java 21](https://docs.spring.io/spring-boot/system-requirements.html).

## Ejecutar

```bash
java -version
./mvnw clean verify
./mvnw spring-boot:run
# Alternativa después de compilar:
java -jar target/polizas-api.jar
```

En Windows: `mvnw.cmd clean verify` y `mvnw.cmd spring-boot:run`. La primera ejecución descarga Maven y dependencias. Puerto predeterminado: 8080. Si se cambia el puerto, definir también `CORE_BASE_URL` con la URL local nueva. `APP_API_KEY` reemplaza la clave de demostración `123456`. Ejemplo: `CORE_BASE_URL=http://localhost:9090` al usar el puerto 9090.

H2 usa `jdbc:h2:mem:polizas;DB_CLOSE_DELAY=-1`, usuario `sa`, contraseña vacía y esquema `create-drop`. La consola H2 está deshabilitada. **Todos los datos se pierden al apagar la aplicación.** El perfil `demo` es el predeterminado y carga seed solo cuando la tabla está vacía. Las pruebas usan el perfil `test` y sus propios fixtures.

## Contrato

Cada ruta requiere `api-key: 123456` o `x-api-key: 123456`. Si se envían ambos, ambos deben coincidir. Se rechazan encabezados repetidos y claves incorrectas con `401`. Las respuestas son JSON, salvo `204` sin cuerpo. Los arrays se ordenan por id ascendente.

| Método y ruta | Respuesta correcta | Descripción |
| --- | --- | --- |
| `GET /polizas?tipo=&estado=` | `200` | Filtros opcionales `INDIVIDUAL`/`COLECTIVA` y `ACTIVA`/`RENOVADA`/`CANCELADA`, combinados con AND. |
| `GET /polizas/{id}/riesgos` | `200` | Incluye riesgos cancelados. |
| `POST /polizas/{id}/renovar` | `200` | Cuerpo `{"ipcPorcentaje":5.00}`. |
| `POST /polizas/{id}/cancelar` | `200` | Cancela lógicamente póliza y riesgos. |
| `POST /polizas/{id}/riesgos` | `201` | Cuerpo `{"descripcion":"Apartamento 301"}`; solo colectiva vigente. |
| `POST /riesgos/{id}/cancelar` | `200` | Cancela lógicamente el riesgo. |
| `POST /core-mock/evento` | `204` | Cuerpo `{"evento":"ACTUALIZACION","polizaId":555}`; solo registra el evento. |

Los DTOs de respuesta exponen id, estado, fechas, canon y prima de póliza, o id, póliza, descripción y estado de riesgo. Los errores usan `{timestamp,status,code,message,path,fieldErrors}`. Códigos principales: `400 VALIDATION_ERROR`, `404 NOT_FOUND`, `409 BUSINESS_RULE_VIOLATION` o `CONCURRENT_OPERATION`, `502 CORE_UNAVAILABLE`, `500 INTERNAL_ERROR`.

## Seed comprobado

| ID | Tipo | Estado | Canon | Meses | Prima | Riesgos |
| --- | --- | --- | ---: | ---: | ---: | --- |
| 1 | INDIVIDUAL | ACTIVA | 1,000,000.00 | 12 | 12,000,000.00 | 1 activo |
| 2 | COLECTIVA | ACTIVA | 2,000,000.00 | 12 | 24,000,000.00 | 2 activos |
| 3 | INDIVIDUAL | CANCELADA | 800,000.00 | 6 | 4,800,000.00 | 1 cancelado |
| 4 | COLECTIVA | RENOVADA | 1,500,000.00 | 12 | 18,000,000.00 | 1 activo, 1 cancelado |

## Ejemplos

```bash
curl -i 'http://localhost:8080/polizas?tipo=COLECTIVA&estado=ACTIVA' -H 'api-key: 123456'
curl -i http://localhost:8080/polizas/1/riesgos -H 'api-key: 123456'
curl -i -X POST http://localhost:8080/polizas/1/renovar -H 'api-key: 123456' -H 'Content-Type: application/json' -d '{"ipcPorcentaje":5.00}'
curl -i -X POST http://localhost:8080/polizas/2/riesgos -H 'api-key: 123456' -H 'Content-Type: application/json' -d '{"descripcion":"Apartamento 301"}'
curl -i -X POST http://localhost:8080/riesgos/1/cancelar -H 'api-key: 123456'
curl -i -X POST http://localhost:8080/polizas/2/cancelar -H 'api-key: 123456'
curl -i -X POST http://localhost:8080/core-mock/evento -H 'api-key: 123456' -H 'Content-Type: application/json' -d '{"evento":"ACTUALIZACION","polizaId":555}'
curl -i http://localhost:8080/polizas -H 'x-api-key: 123456'
```

Estos comandos modifican el seed. Reiniciar la aplicación restablece los datos. Fallos reproducibles:

```bash
curl -i http://localhost:8080/polizas                           # 401
curl -i -X POST http://localhost:8080/polizas/3/renovar -H 'api-key: 123456' -H 'Content-Type: application/json' -d '{"ipcPorcentaje":5}'  # 409
curl -i -X POST http://localhost:8080/polizas/1/riesgos -H 'api-key: 123456' -H 'Content-Type: application/json' -d '{"descripcion":"Otro"}' # 409
```

## Decisiones de implementación

- El IPC es porcentaje en unidades: `5` significa 5 %. Debe ser no negativo y tener máximo cuatro decimales. No se consulta el IPC oficial; el 5 % es ilustrativo. El canon se multiplica por `1 + IPC/100` y se redondea a dos decimales con `HALF_UP`; la prima es canon por meses iniciales. Los resultados fuera de `DECIMAL(19,2)` se rechazan con `400`.
- Las fechas usan fin exclusivo. La renovación empieza en el fin anterior, añade los meses de vigencia inicial con `plusMonths`, y es repetible: cada POST válido abre otro período y aplica nuevamente el porcentaje. No hay reintentos automáticos.
- `ACTIVA` es el estado inicial. Un riesgo tiene descripción de hasta 200 caracteres y cancelación lógica. Una individual no admite riesgos adicionales. Se permite cancelar el último riesgo de una colectiva. Cancelar de nuevo retorna el estado actual sin nuevo evento.
- Las mutaciones de una póliza toman un bloqueo pesimista sobre ella. Esto serializa operaciones de la misma póliza y evita agregar riesgos activos durante su cancelación. H2 sirve para demostrar la lógica local, no el comportamiento de un motor productivo.
- Después de guardar y hacer flush de una mutación real, el cliente envía por HTTP `ACTUALIZACION` al mock con el id de póliza. La cancelación de varios riesgos produce un solo evento. El mock no modifica la base y acepta ids inexistentes como 555. Hay 1 segundo para conectar y 2 segundos de espera de respuesta; no hay reintentos.
- Un fallo HTTP o timeout CORE causa `502` y rollback local. HTTP y la transacción H2 **no son atómicos**: el mock puede recibir un evento aunque la transacción se revierta. Una solución real requeriría outbox y deduplicación.
- Se registran intento, éxito o fallo del envío y recepción en el mock con ids técnicos y correlación, sin registrar la clave. El valor de la clave es solo de demostración.

## Verificación realizada

`./mvnw clean verify` pasó con 11 pruebas: rutas, filtros, reglas, concurrencia acotada, rollback, cliente HTTP con servidor stub y flujo HTTP completo contra el mock real. También se inició `target/polizas-api.jar` y se comprobó `GET /polizas` (cuatro registros del seed), `x-api-key` y renovación del id 1 (`1,050,000.00` de canon, `12,600,000.00` de prima). La ejecución local usó Java 25 para compilar con `release 21`; el proyecto requiere Java 21 o posterior compatible con Spring Boot 4.1.1.

La URL pública de este repositorio debe incluirse en el PDF de entrega de la prueba.
