# Photo Bogotá - Backend API

## Descripción del Proyecto
Photo Bogotá es una solución tecnológica diseñada para mitigar el desconocimiento de ubicaciones puntuales de interés fotográfico en la ciudad. El sistema facilita el descubrimiento y aprovechamiento de espacios visualmente atractivos, permitiendo a los usuarios capturar momentos únicos en sitios que suelen permanecer ocultos.

La plataforma se enfoca en la identificación, clasificación y recomendación de "spots" basados en intereses específicos como gastronomía, arte urbano y naturaleza.

## Objetivos del Sistema
* **Descubrimiento Inteligente**: Brindar información precisa sobre la ubicación de lugares atractivos en Bogotá.
* **Interfaz Inclusiva**: Desarrollo de una navegación intuitiva para jóvenes y adultos con distintos niveles de experiencia tecnológica.
* **Comunidad Activa**: Implementar un sistema de valoraciones, reacciones y comentarios para enriquecer la experiencia colectiva.
* **Integridad de Datos**: Permitir el reporte de inconsistencias como "sitio cerrado" o "dirección incorrecta" para mantener la información confiable.

## Arquitectura Técnica
El backend está construido sobre **Spring Boot 3.x**, siguiendo una estructura de capas para garantizar escalabilidad y mantenimiento:

* **Lenguaje**: Java 21+ (Maven).
* **Base de Datos**: MongoDB - Base de datos no relacional para almacenamiento flexible de documentos.
* **Seguridad**: Spring Security con **BCrypt** para la protección de credenciales y gestión de acceso.
* **Mapeo**: **MapStruct** para la transformación eficiente entre entidades de persistencia y DTOs.
* **Validación**: Lógica de negocio para restringir imágenes a una resolución máxima de 720p.

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
El sistema organiza los puntos de interés en categorías temáticas para personalizar la búsqueda:
* **Gastronómicos**: Sitios con estética visual y oferta culinaria.
* **Urbanos**: Arte callejero, arquitectura, skateparks y murales.
* **Naturaleza/Extremos**: Montañas, cascadas y miradores.
* **Culturales**: Plazas, iglesias y mercados tradicionales.
* **Geográficos**: Filtrado específico por localidades de Bogotá.

## Limitaciones y Alcance
* **Geografía**: El proyecto se limita exclusivamente al área metropolitana de Bogotá D.C.
* **Técnico**: Capacidad de almacenamiento sujeta a recursos del servidor y resolución máxima de 720p por imagen.
* **Social**: La aplicación no funciona como red social; no incluye funciones de seguimiento ("follow").

## Requisitos de Ejecución
1. Configurar MongoDB en el entorno local (base de datos no relacional).
2. Ejecutar `mvn clean install` para la generación de código de MapStruct.
3. Iniciar la aplicación con `mvn spring-boot:run`.
