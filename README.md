# PENITENT

> *Un penitente camina hacia su condena. Solo el dolor lo purifica.*

Penitente es un juego de acción y plataformas 2D de desplazamiento lateral ambientado en un mundo gótico-medieval oscuro, inspirado en **Blasphemous**. Explora mazmorras, catacumbas y osarios repletos de enemigos mientras buscas la redención a golpe de espada.

---

## Características

### Mundo y zonas

El juego se divide en **tres zonas interconectadas**, cada una con su propia atmósfera, enemigos y música:

| Zona | Descripción                                                                                    |
|------|------------------------------------------------------------------------------------------------|
| **Las Entrañas** | Las entrañas de una inmensa biblioteca. Patrullas de enemigos y plataformas peligrosas.        |
| **El Osario** | Un cementerio de libros custodiado por esqueletos encadenados.                                 |
| **Las Catacumbas** | Cavernas sin luz pobladas por murciélagos que cazan en la oscuridad y un enemigo muy poderoso. |

### Combate

- **Ataque básico** con espada — pulsa J para atacar.
- **Sistema de combo** — pulsa J de nuevo durante el primer golpe para encadenar un segundo ataque.
- **Dash** — esquiva con un impulso rápido hacia adelante pulsando K.
- Ventana de daño precisa por enemigo: cada tipo tiene sus propios fotogramas de ataque activo.

### Enemigos

| Enemigo | Comportamiento |
|---------|---------------|
| **Guardia** | Patrulla entre límites. Te persigue al acercarte, te ataca en rango. 2 golpes para derrotarlo. |
| **Esqueleto** | IA más agresiva con ataque de cadena en ventana de fotogramas 10–14. Resiste 4 golpes y causa 2 corazones de daño. |
| **Murciélago** | Enemigo volador. Duerme hasta que te acercas y entonces te persigue en todas direcciones. |

### Sistema de guardado

- **3 ranuras de guardado** independientes.
- Los **altares de penitencia** (checkpoints) guardan la partida, curan al jugador por completo y reinician los enemigos de las demás zonas.
- Los datos guardados incluyen zona, posición, objetos, salud y marca de tiempo.

### Inventario y objetos

Cinco objetos coleccionables ocultos por el mundo, cada uno con nombre y lore propios:

- Azoque
- Cáliz de los Versos Invertidos
- Matraz Biliar Vacío
- Nudos de Cordón de Rosario
- Velo Negro de Luto

### Audio

Música ambiental distinta para cada zona y efectos de sonido para todas las acciones del jugador y los enemigos (pasos, golpes, muertes, dash, salto, etc.).

---

## Controles

| Tecla | Acción |
|-------|--------|
| `A` / `D` | Moverse izquierda / derecha |
| `Espacio` | Saltar |
| `J` | Atacar (pulsar de nuevo durante el primer ataque para el combo) |
| `K` | Dash |
| `B` | Interactuar con altar de penitencia |
| `ESC` | Pausa |

---

## Tecnología

- **Motor:** [libGDX](https://libgdx.com/) 1.14
- **Plataforma de escritorio:** LWJGL3
- **Lenguaje:** Java
- **Mapas:** Tiled (`.tmx`), resolución lógica 608×320 px, tiles de 32×32 px
- **Fuentes:** P052 (Palatino) generada en tiempo real con FreeType
- **Build:** Gradle multi-módulo (`core` + `lwjgl3`)

---

## Construcción y ejecución

**Requisitos:** JDK 11 o superior, Gradle (wrapper incluido).

```bash
# Ejecutar el juego (escritorio)
./gradlew lwjgl3:run

# Generar JAR ejecutable
./gradlew lwjgl3:jar
# Salida: lwjgl3/build/libs/Penitent-1.0.0.jar

# Compilar sin ejecutar
./gradlew build

# Limpiar
./gradlew clean
```

El directorio de trabajo en ejecución es `assets/`. Todos los recursos se referencian relativos a esa carpeta.

---

## Créditos

| Rol | Autor / Fuente |
|-----|---------------|
| Desarrollo | Borja Zorrilla Gracia |
| Arte | [itch.io](https://itch.io) |
| Música | Carlos Viola |
| Sonidos | [freesound.org](https://freesound.org) |
| Motor | libGDX |

---

*Versión 1.0.0*
