# Photo Bogotá - Backend API

## Descripción del Proyecto
[cite_start]Photo Bogotá es una solución tecnológica diseñada para mitigar el desconocimiento de ubicaciones puntuales de interés fotográfico en la ciudad[cite: 3]. [cite_start]El sistema facilita el descubrimiento y aprovechamiento de espacios visualmente atractivos, permitiendo a los usuarios capturar momentos únicos en sitios que suelen permanecer ocultos[cite: 3, 5].

[cite_start]La plataforma se enfoca en la identificación, clasificación y recomendación de "spots" basados en intereses específicos como gastronomía, arte urbano y naturaleza[cite: 27].

## Objetivos del Sistema
* [cite_start]**Descubrimiento Inteligente**: Brindar información precisa sobre la ubicación de lugares atractivos en Bogotá[cite: 5].
* [cite_start]**Interfaz Inclusiva**: Desarrollo de una navegación intuitiva para jóvenes y adultos con distintos niveles de experiencia tecnológica[cite: 7].
* [cite_start]**Comunidad Activa**: Implementar un sistema de valoraciones, reacciones y comentarios para enriquecer la experiencia colectiva[cite: 10].
* [cite_start]**Integridad de Datos**: Permitir el reporte de inconsistencias como "sitio cerrado" o "dirección incorrecta" para mantener la información confiable[cite: 11, 13, 14].

## Arquitectura Técnica
El backend está construido sobre **Spring Boot 3.x**, siguiendo una estructura de capas para garantizar escalabilidad y mantenimiento:

* [cite_start]**Lenguaje**: Java 21+ (Maven). [cite: 32]
* **Seguridad**: Spring Security con **BCrypt** para la protección de credenciales y gestión de acceso.
* **Mapeo**: **MapStruct** para la transformación eficiente entre entidades de persistencia y DTOs.
* [cite_start]**Validación**: Lógica de negocio para restringir imágenes a una resolución máxima de 720p[cite: 32].

## Estructura de Paquetes (Package Naming)
Se utiliza la convención de dominio inverso para la organización del código: `com.photobogota.api`.

* **`config`**: Configuración de seguridad (SecurityChain), encriptación y beans globales.
* **`controller`**: Endpoints REST para la gestión de spots, categorías y reportes.
* **`dto`**: Objetos de transferencia de datos para desacoplar la API de la base de datos.
* **`exception`**: Manejo global de excepciones para respuestas estandarizadas.
* **`mapper`**: Interfaces de MapStruct para la conversión automática de objetos.
* **`model`**: Entidades JPA que representan la estructura de datos en este caso MONGO DB.
* **`repository`**: Capa de acceso a datos mediante Spring Data JPA.
* **`service`**: Capa de servicios con la lógica de negocio y filtros especializados.

## Categorías y Filtros
[cite_start]El sistema organiza los puntos de interés en categorías temáticas para personalizar la búsqueda[cite: 16]:
* [cite_start]**Gastronómicos**: Sitios con estética visual y oferta culinaria[cite: 17].
* [cite_start]**Urbanos**: Arte callejero, arquitectura, skateparks y murales[cite: 18].
* [cite_start]**Naturaleza/Extremos**: Montañas, cascadas y miradores[cite: 19].
* [cite_start]**Culturales**: Plazas, iglesias y mercados tradicionales[cite: 20].
* [cite_start]**Geográficos**: Filtrado específico por localidades de Bogotá[cite: 22].

## Limitaciones y Alcance
* [cite_start]**Geografía**: El proyecto se limita exclusivamente al área metropolitana de Bogotá D.C.[cite: 31].
* [cite_start]**Técnico**: Capacidad de almacenamiento sujeta a recursos del servidor y resolución máxima de 720p por imagen[cite: 32].
* [cite_start]**Social**: La aplicación no funciona como red social; no incluye funciones de seguimiento ("follow")[cite: 34].

## Requisitos de Ejecución
1. Configurar el entorno de base de datos relacional.
2. Ejecutar `mvn clean install` para la generación de código de MapStruct.
3. Iniciar la aplicación con `mvn spring-boot:run`.
